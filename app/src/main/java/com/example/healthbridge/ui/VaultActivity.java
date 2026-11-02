package com.example.healthbridge.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.healthbridge.R;
import com.example.healthbridge.adapters.RecordsAdapter;
import com.example.healthbridge.api.SupabaseApiService;
import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.models.MedicalRecord;
import com.example.healthbridge.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Document Vault Activity.
 * Extends BaseProtectedActivity so unauthenticated users cannot access this screen.
 */
public class VaultActivity extends BaseProtectedActivity {

    private static final String[] CATEGORIES = {
            "All", "Prescription", "Lab Report", "X-Ray", "Summary"
    };

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private ImageButton btnBack;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupCategories;
    private RecyclerView rvRecords;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutUploading;
    private FloatingActionButton fabUpload;

    private AlertDialog detailsDialog;

    private RecordsAdapter recordsAdapter;
    private SupabaseApiService apiService;

    private String currentQuery = "";
    private String currentCategory = "All";

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private PendingFile pendingFile;

    private static class PendingFile {
        final String displayName;
        final String mimeType;
        final byte[] data;

        PendingFile(String displayName, String mimeType, byte[] data) {
            this.displayName = displayName;
            this.mimeType = mimeType;
            this.data = data;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        initViews();
        initBackend();
        setupRecyclerView();
        setupNavigation();
        setupSearch();
        setupCategoryChips();
        setupFilePicker();

        loadRecords();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detailsDialog != null && detailsDialog.isShowing()) {
            detailsDialog.dismiss();
            detailsDialog = null;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        pendingFile = null;
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        rvRecords = findViewById(R.id.rvRecords);
        progressLoading = findViewById(R.id.progressLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutUploading = findViewById(R.id.layoutUploading);
        fabUpload = findViewById(R.id.fabUpload);
    }

    private void initBackend() {
        SupabaseClient.init(getApplicationContext());
        apiService = SupabaseClient.getApiService();
    }

    private void setupRecyclerView() {
        recordsAdapter = new RecordsAdapter();
        recordsAdapter.setOnRecordClickListener(this::handleRecordClick);
        if (rvRecords != null) {
            rvRecords.setLayoutManager(new LinearLayoutManager(this));
            rvRecords.setAdapter(recordsAdapter);
        }
    }

    private void setupNavigation() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        if (uri != null) {
                            validateAndProcessFile(uri);
                        }
                    }
                }
        );

        if (fabUpload != null) {
            fabUpload.setOnClickListener(v -> launchFilePicker());
        }
    }

    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "application/pdf"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            if (filePickerLauncher != null) {
                filePickerLauncher.launch(Intent.createChooser(intent, "Select Document"));
            }
        } catch (ActivityNotFoundException e) {
            showSnackbar("No file picker found on this phone.");
        } catch (Exception e) {
            showSnackbar("Could not open file picker.");
        }
    }

    private void validateAndProcessFile(Uri uri) {
        if (fabUpload != null) {
            fabUpload.setEnabled(false);
        }
        if (layoutUploading != null) {
            layoutUploading.setVisibility(View.VISIBLE);
        }

        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null || mimeType.trim().isEmpty() || mimeType.equals("application/octet-stream")) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (extension != null && !extension.trim().isEmpty()) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
            }
        }

        if (mimeType != null && mimeType.equalsIgnoreCase("image/jpg")) {
            mimeType = "image/jpeg";
        }

        if (!isAllowedMimeType(mimeType)) {
            if (layoutUploading != null) layoutUploading.setVisibility(View.GONE);
            if (fabUpload != null) fabUpload.setEnabled(true);
            showSnackbar("Only PDF, JPG and PNG files are allowed.");
            return;
        }

        long fileSize = -1;
        String displayName = "document";

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.trim().isEmpty()) {
                        displayName = name.trim();
                    }
                }

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    fileSize = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
        }

        if (fileSize > MAX_FILE_SIZE) {
            if (layoutUploading != null) layoutUploading.setVisibility(View.GONE);
            if (fabUpload != null) fabUpload.setEnabled(true);
            showSnackbar("File is too large. Maximum size is 10 MB.");
            return;
        }

        final String finalMimeType = mimeType;
        final String finalDisplayName = displayName;
        final long initialSize = fileSize;

        executorService.execute(() -> {
            try {
                byte[] data = readBytes(uri);

                if (initialSize <= 0 && data.length > MAX_FILE_SIZE) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (layoutUploading != null) layoutUploading.setVisibility(View.GONE);
                        if (fabUpload != null) fabUpload.setEnabled(true);
                        showSnackbar("File is too large. Maximum size is 10 MB.");
                    });
                    return;
                }

                PendingFile result = new PendingFile(finalDisplayName, finalMimeType, data);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (layoutUploading != null) layoutUploading.setVisibility(View.GONE);
                    if (fabUpload != null) fabUpload.setEnabled(true);
                    pendingFile = result;
                    showUploadConfirmationDialog();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (layoutUploading != null) layoutUploading.setVisibility(View.GONE);
                    if (fabUpload != null) fabUpload.setEnabled(true);
                    showSnackbar("Could not read this file. Try another one.");
                });
            }
        });
    }

    private void showUploadConfirmationDialog() {
        if (pendingFile == null) return;

        float density = getResources().getDisplayMetrics().density;
        int paddingPx = (int) (20f * density);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(paddingPx, (int) (12f * density), paddingPx, 0);

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setBoxCornerRadii(12f * density, 12f * density, 12f * density, 12f * density);
        inputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.hb_teal_primary));
        inputLayout.setHint("Document Title");

        TextInputEditText etTitle = new TextInputEditText(this);
        etTitle.setText(removeFileExtension(pendingFile.displayName));
        etTitle.setTextColor(ContextCompat.getColor(this, R.color.hb_text_primary));
        etTitle.setTextSize(14f);
        etTitle.setSingleLine(true);
        inputLayout.addView(etTitle);
        container.addView(inputLayout);

        TextView tvCategoryLabel = new TextView(this);
        tvCategoryLabel.setText("Category");
        tvCategoryLabel.setTextSize(12f);
        tvCategoryLabel.setTextColor(ContextCompat.getColor(this, R.color.hb_text_secondary));
        tvCategoryLabel.setPadding(0, (int) (16f * density), 0, (int) (8f * density));
        container.addView(tvCategoryLabel);

        List<String> uploadCategories = new ArrayList<>();
        for (String cat : CATEGORIES) {
            if (!"All".equalsIgnoreCase(cat)) {
                uploadCategories.add(cat);
            }
        }
        String[] categoryArray = uploadCategories.toArray(new String[0]);

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        ColorStateList radioColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        ContextCompat.getColor(this, R.color.hb_teal_primary),
                        ContextCompat.getColor(this, R.color.hb_text_secondary)
                }
        );

        for (int i = 0; i < categoryArray.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(categoryArray[i]);
            rb.setTextColor(ContextCompat.getColor(this, R.color.hb_text_primary));
            rb.setTextSize(14f);
            rb.setButtonTintList(radioColorStateList);
            rb.setId(View.generateViewId());
            radioGroup.addView(rb);

            if (i == 0) {
                rb.setChecked(true); // first one selected by default
            }
        }
        container.addView(radioGroup);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Upload Document")
                .setView(container)
                .setPositiveButton("Upload", null)
                .setNegativeButton("Cancel", (d, which) -> {
                    pendingFile = null;
                })
                .create();

        dialog.setOnShowListener(d -> {
            Button uploadBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            int tealColor = ContextCompat.getColor(this, R.color.hb_teal_primary);
            if (uploadBtn != null) uploadBtn.setTextColor(tealColor);
            if (cancelBtn != null) cancelBtn.setTextColor(tealColor);

            if (uploadBtn != null) {
                uploadBtn.setOnClickListener(v -> {
                    String title = (etTitle.getText() != null) ? etTitle.getText().toString().trim() : "";
                    if (title.isEmpty()) {
                        inputLayout.setError("Title cannot be empty");
                        return;
                    }
                    inputLayout.setError(null);

                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    RadioButton checkedRb = radioGroup.findViewById(checkedId);
                    String selectedCategory = (checkedRb != null) ? checkedRb.getText().toString() : categoryArray[0];

                    dialog.dismiss();
                    performUpload(title, selectedCategory);
                });
            }
        });

        dialog.show();
    }

    private void performUpload(String title, String category) {
        if (pendingFile == null || pendingFile.data == null) {
            handleUploadFailure();
            return;
        }

        if (layoutUploading != null) {
            layoutUploading.setVisibility(View.VISIBLE);
        }
        if (fabUpload != null) {
            fabUpload.setEnabled(false);
        }

        int sessionProfileId = SessionManager.getInstance(this).getProfileId();
        final int profileId = sessionProfileId > 0 ? sessionProfileId : 1;

        String sanitized = sanitizeFileName(pendingFile.displayName);
        String storagePath = profileId + "/" + System.currentTimeMillis() + "_" + sanitized;

        final String finalMimeType = pendingFile.mimeType;
        final byte[] uploadData = pendingFile.data;

        Call<Void> uploadCall = callUploadStorage(storagePath, finalMimeType, uploadData);
        uploadCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (isFinishing() || isDestroyed()) {
                    pendingFile = null;
                    return;
                }

                if (!response.isSuccessful()) {
                    handleUploadFailure();
                    return;
                }

                String publicUrl = getStoragePublicUrl(storagePath);
                MedicalRecord newRecord = new MedicalRecord(profileId, title, category, publicUrl, finalMimeType);

                Call<List<MedicalRecord>> insertCall = callInsertMedicalRecord(newRecord);
                insertCall.enqueue(new Callback<List<MedicalRecord>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<MedicalRecord>> call, @NonNull Response<List<MedicalRecord>> insertResponse) {
                        if (isFinishing() || isDestroyed()) {
                            pendingFile = null;
                            return;
                        }

                        if (insertResponse.isSuccessful() && insertResponse.body() != null) {
                            if (layoutUploading != null) layoutUploading.setVisibility(View.GONE);
                            if (fabUpload != null) fabUpload.setEnabled(true);
                            pendingFile = null;

                            showSnackbar("Document uploaded.");
                            loadRecords();
                        } else {
                            handleUploadFailure();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<MedicalRecord>> call, @NonNull Throwable t) {
                        if (isFinishing() || isDestroyed()) {
                            pendingFile = null;
                            return;
                        }
                        handleUploadFailure();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    pendingFile = null;
                    return;
                }
                handleUploadFailure();
            }
        });
    }

    private void handleUploadFailure() {
        if (layoutUploading != null) {
            layoutUploading.setVisibility(View.GONE);
        }
        if (fabUpload != null) {
            fabUpload.setEnabled(true);
        }
        pendingFile = null;
        showSnackbar("Upload failed. Check your internet and try again.");
    }

    private Call<Void> callUploadStorage(String storagePath, String mimeType, byte[] data) {
        String uploadUrl = SupabaseClient.STORAGE_URL + SupabaseClient.BUCKET_NAME + "/" + storagePath;
        RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), data);
        return apiService.uploadFile(uploadUrl, mimeType, requestBody);
    }

    private Call<List<MedicalRecord>> callInsertMedicalRecord(MedicalRecord record) {
        return apiService.insertMedicalRecord(record);
    }

    private String getStoragePublicUrl(String storagePath) {
        return SupabaseClient.STORAGE_PUBLIC_URL + SupabaseClient.BUCKET_NAME + "/" + storagePath;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "document";
        }
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        return sanitized.isEmpty() ? "document" : sanitized;
    }

    private String removeFileExtension(String fileName) {
        if (fileName == null) return "document";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    private boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) return false;
        String lower = mimeType.trim().toLowerCase(Locale.ROOT);
        return lower.equals("application/pdf")
                || lower.equals("image/jpeg")
                || lower.equals("image/png");
    }

    private byte[] readBytes(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("Unable to open input stream for URI");
            }
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, bytesRead);
            }
            return byteBuffer.toByteArray();
        }
    }

    private void setupSearch() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                currentQuery = (s != null) ? s.toString().trim() : "";
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v);
                return true;
            }
            return false;
        });
    }

    private void setupCategoryChips() {
        if (chipGroupCategories == null) return;

        chipGroupCategories.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int cornerRadiusPx = (int) (20f * density);
        int strokeWidthPx = (int) (1f * density);
        int minHeightPx = (int) (48f * density);

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };

        int[] bgColors = new int[]{
                ContextCompat.getColor(this, R.color.hb_teal_primary),
                ContextCompat.getColor(this, R.color.hb_glass_fill)
        };
        ColorStateList bgStateList = new ColorStateList(states, bgColors);

        int[] textColors = new int[]{
                ContextCompat.getColor(this, R.color.hb_text_on_dark),
                ContextCompat.getColor(this, R.color.hb_teal_dark)
        };
        ColorStateList textStateList = new ColorStateList(states, textColors);

        int[] strokeColors = new int[]{
                ContextCompat.getColor(this, R.color.hb_teal_primary),
                ContextCompat.getColor(this, R.color.hb_glass_stroke)
        };
        ColorStateList strokeStateList = new ColorStateList(states, strokeColors);

        for (String category : CATEGORIES) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipBackgroundColor(bgStateList);
            chip.setTextColor(textStateList);
            chip.setChipStrokeColor(strokeStateList);
            chip.setChipStrokeWidth(strokeWidthPx);
            chip.setChipCornerRadius(cornerRadiusPx);
            chip.setMinHeight(minHeightPx);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setCheckedIconVisible(false);

            if ("All".equals(category)) {
                chip.setChecked(true);
            }

            chipGroupCategories.addView(chip);
        }

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds != null && !checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                Chip selectedChip = group.findViewById(checkedId);
                if (selectedChip != null && selectedChip.getText() != null) {
                    currentCategory = selectedChip.getText().toString();
                } else {
                    currentCategory = "All";
                }
            } else {
                currentCategory = "All";
            }
            applyFilter();
        });
    }

    private void applyFilter() {
        if (recordsAdapter == null) return;

        recordsAdapter.applyFilter(currentQuery, currentCategory);

        int displayedCount = recordsAdapter.getDisplayedCount();
        int totalCount = recordsAdapter.getTotalCount();

        if (displayedCount == 0) {
            if (rvRecords != null) rvRecords.setVisibility(View.GONE);
            if (layoutEmpty != null) {
                layoutEmpty.setVisibility(View.VISIBLE);
                updateEmptyStateMessage(totalCount == 0);
            }
        } else {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (rvRecords != null) rvRecords.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyStateMessage(boolean isTotalEmpty) {
        if (layoutEmpty == null) return;

        TextView tvEmpty = null;
        for (int i = 0; i < layoutEmpty.getChildCount(); i++) {
            View child = layoutEmpty.getChildAt(i);
            if (child instanceof TextView) {
                tvEmpty = (TextView) child;
                break;
            }
        }

        if (tvEmpty != null) {
            if (isTotalEmpty) {
                tvEmpty.setText("No documents yet. Tap + to upload.");
            } else {
                tvEmpty.setText("No documents found.");
            }
        }
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        }
    }

    private void showSnackbar(String message) {
        View root = findViewById(android.R.id.content);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void loadRecords() {
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        int profileId = SessionManager.getInstance(this).getProfileId();
        String profileFilter = "eq." + (profileId > 0 ? profileId : 1);

        Call<List<MedicalRecord>> call = apiService.getMedicalRecords("*", profileFilter);

        call.enqueue(new Callback<List<MedicalRecord>>() {
            @Override
            public void onResponse(@NonNull Call<List<MedicalRecord>> call, @NonNull Response<List<MedicalRecord>> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (progressLoading != null) {
                    progressLoading.setVisibility(View.GONE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<MedicalRecord> records = new ArrayList<>(response.body());
                    records.sort((r1, r2) -> {
                        String d1 = r1 != null ? r1.getCreatedAt() : null;
                        String d2 = r2 != null ? r2.getCreatedAt() : null;
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1; // nulls last
                        if (d2 == null) return -1;
                        return d2.compareTo(d1); // descending newest first
                    });
                    recordsAdapter.setRecords(records);
                    applyFilter();
                } else {
                    handleLoadFailure();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MedicalRecord>> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (progressLoading != null) {
                    progressLoading.setVisibility(View.GONE);
                }

                handleLoadFailure();
            }
        });
    }

    private void handleLoadFailure() {
        if (recordsAdapter != null && recordsAdapter.getItemCount() == 0) {
            if (layoutEmpty != null) {
                layoutEmpty.setVisibility(View.VISIBLE);
                updateEmptyStateMessage(true);
            }
            if (rvRecords != null) rvRecords.setVisibility(View.GONE);
        }

        Snackbar.make(rvRecords, "Could not load documents. Check your internet and try again.", Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> loadRecords())
                .show();
    }

    private void handleRecordClick(MedicalRecord record) {
        showDetailsDialog(record);
    }

    private void showDetailsDialog(MedicalRecord record) {
        if (record == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_record_details, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        detailsDialog = dialog;

        ImageView ivPreview = dialogView.findViewById(R.id.ivPreview);
        TextView tvDetailTitle = dialogView.findViewById(R.id.tvDetailTitle);
        TextView tvDetailCategory = dialogView.findViewById(R.id.tvDetailCategory);
        TextView tvFileName = dialogView.findViewById(R.id.tvFileName);
        TextView tvFileType = dialogView.findViewById(R.id.tvFileType);
        TextView tvDetailCategoryValue = dialogView.findViewById(R.id.tvDetailCategoryValue);
        TextView tvUploadedOn = dialogView.findViewById(R.id.tvUploadedOn);
        Button btnView = dialogView.findViewById(R.id.btnView);

        String title = record.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            tvDetailTitle.setText(title.trim());
        } else {
            tvDetailTitle.setText("Untitled document");
        }

        String category = record.getCategory();
        if (category == null || category.trim().isEmpty()) {
            category = "General";
        }
        tvDetailCategory.setText(category);
        tvDetailCategoryValue.setText(category);

        int[] catColors = getCategoryColorsStatic(category, this);
        tvDetailCategory.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_tag));
        tvDetailCategory.setBackgroundTintList(ColorStateList.valueOf(catColors[0]));
        tvDetailCategory.setTextColor(catColors[1]);

        String fileUrl = record.getFileUrl();
        String extractedFileName = extractFileNameDecoded(fileUrl, title);
        tvFileName.setText(extractedFileName);

        String mimeType = record.getMimeType();
        String displayFileType = formatFileType(mimeType);
        tvFileType.setText(displayFileType);

        String uploadedOn = formatDisplayDate(record.getCreatedAt());
        tvUploadedOn.setText(uploadedOn);

        boolean isPdf = mimeType != null && mimeType.equalsIgnoreCase("application/pdf");
        boolean isImage = mimeType != null && mimeType.startsWith("image/");

        if (isImage && fileUrl != null && !fileUrl.trim().isEmpty()) {
            ivPreview.setImageTintList(null);
            Glide.with(this)
                    .load(fileUrl.trim())
                    .centerCrop()
                    .placeholder(R.drawable.ic_description)
                    .error(R.drawable.ic_description)
                    .into(ivPreview);
        } else {
            ivPreview.setImageResource(R.drawable.ic_description);
            ivPreview.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.hb_cat_pdf_text)));
        }

        btnView.setOnClickListener(v -> {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                showSnackbar("File link not available.");
                return;
            }
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                if (mimeType != null && !mimeType.trim().isEmpty()) {
                    viewIntent.setDataAndType(Uri.parse(fileUrl.trim()), mimeType.trim());
                } else {
                    viewIntent.setData(Uri.parse(fileUrl.trim()));
                }
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(viewIntent);
            } catch (ActivityNotFoundException e) {
                try {
                    Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl.trim()));
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fallback);
                } catch (Exception ex) {
                    showSnackbar("No app found to open this file.");
                }
            } catch (Exception e) {
                showSnackbar("No app found to open this file.");
            }
        });

        dialog.show();
    }

    private String extractFileNameDecoded(String fileUrl, String fallbackTitle) {
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            try {
                String clean = fileUrl.trim();
                int q = clean.indexOf('?');
                if (q != -1) {
                    clean = clean.substring(0, q);
                }
                int slash = clean.lastIndexOf('/');
                if (slash != -1 && slash < clean.length() - 1) {
                    String rawName = clean.substring(slash + 1);
                    return URLDecoder.decode(rawName, "UTF-8");
                }
            } catch (Exception ignored) {
            }
        }
        return fallbackTitle != null && !fallbackTitle.trim().isEmpty() ? fallbackTitle.trim() : "document";
    }

    private String formatFileType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return "Unknown";
        }
        String lower = mimeType.trim().toLowerCase(Locale.ROOT);
        if (lower.equals("application/pdf")) return "PDF";
        if (lower.equals("image/jpeg") || lower.equals("image/jpg")) return "JPEG";
        if (lower.equals("image/png")) return "PNG";
        return mimeType.trim();
    }

    private String formatDisplayDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Not provided";
        }
        try {
            String trimmed = rawDate.trim();
            SimpleDateFormat outFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            if (trimmed.contains("T")) {
                String datePart = trimmed.split("T")[0];
                SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsed = inFormat.parse(datePart);
                if (parsed != null) {
                    return outFormat.format(parsed);
                }
            } else if (trimmed.length() >= 10) {
                SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsed = inFormat.parse(trimmed.substring(0, 10));
                if (parsed != null) {
                    return outFormat.format(parsed);
                }
            }
        } catch (Exception ignored) {
        }
        return "Not provided";
    }

    private int[] getCategoryColorsStatic(String category, Context context) {
        int bg;
        int text;
        String cat = (category != null) ? category.trim() : "";

        if (cat.equalsIgnoreCase("Prescriptions") || cat.equalsIgnoreCase("Prescription")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_prescription_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_prescription_text);
        } else if (cat.equalsIgnoreCase("Lab Reports") || cat.equalsIgnoreCase("Lab Report")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_lab_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_lab_text);
        } else if (cat.equalsIgnoreCase("X-Rays") || cat.equalsIgnoreCase("X-Ray")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_xray_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_xray_text);
        } else if (cat.equalsIgnoreCase("Medical History")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_history_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_history_text);
        } else {
            bg = ContextCompat.getColor(context, R.color.hb_glass_fill);
            text = ContextCompat.getColor(context, R.color.hb_text_secondary);
        }
        return new int[]{bg, text};
    }
}
