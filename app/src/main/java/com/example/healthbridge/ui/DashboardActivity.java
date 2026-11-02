package com.example.healthbridge.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.healthbridge.R;
import com.example.healthbridge.api.SupabaseApiService;
import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.models.MedicalHistory;
import com.example.healthbridge.models.Medication;
import com.example.healthbridge.models.Profile;
import com.example.healthbridge.utils.HealthBridgePdfGenerator;
import com.example.healthbridge.utils.SummaryPdfUploader;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.example.healthbridge.utils.MedicationDialog;
import com.example.healthbridge.utils.HistoryDialog;
import com.example.healthbridge.utils.ProfileSetupDialogHelper;
import android.widget.Toast;
import com.example.healthbridge.utils.HealthUtils;
import com.example.healthbridge.utils.QrCodeHelper;
import com.example.healthbridge.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
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
 * Dashboard screen for Health Bridge.
 * Displays patient greeting, Emergency Red-Alert Card, Vitals glass strip with BMI,
 * quick action tiles (SOS, Nearby Hospitals, Doctor Summary, QR Code), and Medical Vault entry.
 */
public class DashboardActivity extends BaseProtectedActivity {

    private ScrollView dashboardScrollView;
    private LinearLayout dashboardContentLayout;
    private ProgressBar progressBar;

    private TextView tvGreeting;
    private TextView tvPatientName;

    // Emergency Red-Alert Card views
    private TextView tvBloodGroup;
    private TextView tvAllergies;
    private TextView tvEmergencyContact;

    // Vitals views
    private TextView tvBmiBadge;
    private TextView tvHeight;
    private TextView tvWeight;

    // Quick Actions & Vault views
    private View btnSos;
    private View btnHospitals;
    private View btnDoctorSummary;
    private View btnQrCode;
    private View cardVault;
    private View btnAddMedication;
    private View btnAddHistory;
    private View btnLogout;
    private View imgAvatar;
    private View cardEmergency;
    private View cardCompleteProfileBanner;

    private SessionManager sessionManager;
    private Profile currentProfile;
    private String lastSummaryUrl = null;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce light theme so UI displays correctly regardless of system dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        // Ensure SupabaseClient is initialized with context for token injection
        SupabaseClient.init(this);
        sessionManager = SessionManager.getInstance(this);

        // Verify active session. If missing, redirect to LoginActivity and finish
        String userId = sessionManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupClickListeners();

        // Initial default greeting
        tvGreeting.setText(getTimeOfDayGreeting());
        String cachedName = sessionManager.getProfileName();
        if (cachedName != null && !cachedName.trim().isEmpty()) {
            tvPatientName.setText(cachedName);
        } else {
            tvPatientName.setText(getString(R.string.default_patient_name));
        }

        // Fetch live profile data asynchronously via Retrofit
        loadDashboardData();
    }

    private void initViews() {
        dashboardScrollView = findViewById(R.id.dashboardScrollView);
        dashboardContentLayout = findViewById(R.id.dashboardContentLayout);
        progressBar = findViewById(R.id.progressBar);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvPatientName = findViewById(R.id.tvPatientName);

        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvAllergies = findViewById(R.id.tvAllergies);
        tvEmergencyContact = findViewById(R.id.tvEmergencyContact);

        tvBmiBadge = findViewById(R.id.tvBmiBadge);
        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);

        btnSos = findViewById(R.id.btnSos);
        btnHospitals = findViewById(R.id.btnHospitals);
        btnDoctorSummary = findViewById(R.id.btnDoctorSummary);
        btnQrCode = findViewById(R.id.btnQrCode);
        cardVault = findViewById(R.id.cardVault);
        btnAddMedication = findViewById(R.id.btnAddMedication);
        btnAddHistory = findViewById(R.id.btnAddHistory);
        btnLogout = findViewById(R.id.btnLogout);
        imgAvatar = findViewById(R.id.imgAvatar);
        cardEmergency = findViewById(R.id.cardEmergency);
        cardCompleteProfileBanner = findViewById(R.id.cardCompleteProfileBanner);
    }

    private void setupClickListeners() {
        btnSos.setOnClickListener(v -> handleSosCall());
        btnHospitals.setOnClickListener(v -> handleNearbyHospitals());
        btnDoctorSummary.setOnClickListener(v -> handleDoctorSummary());
        btnQrCode.setOnClickListener(v -> handleQrCode());
        cardVault.setOnClickListener(v -> handleOpenVault());
        if (btnAddMedication != null) {
            btnAddMedication.setOnClickListener(v -> {
                int profileId = currentProfile != null ? currentProfile.getId() : sessionManager.getProfileId();
                MedicationDialog.show(DashboardActivity.this, profileId, this::loadDashboardData);
            });
        }
        if (btnAddHistory != null) {
            btnAddHistory.setOnClickListener(v -> {
                int profileId = currentProfile != null ? currentProfile.getId() : sessionManager.getProfileId();
                HistoryDialog.show(DashboardActivity.this, profileId, this::loadDashboardData);
            });
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> showProfileSetupDialog());
        }
        if (cardEmergency != null) {
            cardEmergency.setOnClickListener(v -> showProfileSetupDialog());
        }
        if (cardCompleteProfileBanner != null) {
            cardCompleteProfileBanner.setOnClickListener(v -> showProfileSetupDialog());
        }
    }

    /**
     * Asynchronously loads patient profile from Supabase PostgREST API.
     */
    private void loadDashboardData() {
        setLoading(true);

        String userId = sessionManager.getUserId();
        SupabaseApiService apiService = SupabaseClient.getApiService();
        Call<List<Profile>> call;

        if (userId != null && !userId.trim().isEmpty()) {
            call = apiService.getProfileByUserId("*", "eq." + userId);
        } else {
            call = apiService.getProfiles("*");
        }

        call.enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (isFinishing() || isDestroyed()) return;

                setLoading(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentProfile = response.body().get(0);
                    sessionManager.saveProfileInfo(currentProfile.getId(), currentProfile.getFullName());
                    bindProfileData(currentProfile);
                    checkProfileSetupPrompt(currentProfile);
                } else {
                    showErrorSnackbar();
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;

                setLoading(false);
                showErrorSnackbar();
            }
        });
    }

    /**
     * Binds patient profile attributes to the dashboard layout.
     */
    private void bindProfileData(Profile profile) {
        if (profile == null) {
            return;
        }

        // Header greeting & display name
        tvGreeting.setText(getTimeOfDayGreeting());
        tvPatientName.setText(profile.getFullName() != null ? profile.getFullName() : getString(R.string.default_patient_name));

        // Emergency Red-Alert Card
        tvBloodGroup.setText(Profile.safeDisplay(profile.getBloodGroup()));
        tvAllergies.setText(Profile.safeDisplay(profile.getAllergies()));

        String contactName = profile.getEmergencyContactName();
        String contactPhone = profile.getEmergencyContactPhone();
        String contactDisplay;
        if (contactName != null && !contactName.trim().isEmpty() && contactPhone != null && !contactPhone.trim().isEmpty()) {
            contactDisplay = contactName.trim() + " (" + contactPhone.trim() + ")";
        } else if (contactPhone != null && !contactPhone.trim().isEmpty()) {
            contactDisplay = contactPhone.trim();
        } else if (contactName != null && !contactName.trim().isEmpty()) {
            contactDisplay = contactName.trim();
        } else {
            contactDisplay = getString(R.string.not_provided);
        }
        tvEmergencyContact.setText(contactDisplay);

        // Vitals: Height and Weight
        if (profile.getHeightCm() > 0) {
            tvHeight.setText(String.format(Locale.US, "%.0f cm", profile.getHeightCm()));
        } else {
            tvHeight.setText(getString(R.string.not_provided));
        }

        if (profile.getWeightKg() > 0) {
            tvWeight.setText(String.format(Locale.US, "%.1f kg", profile.getWeightKg()));
        } else {
            tvWeight.setText(getString(R.string.not_provided));
        }



        // BMI badge computation via HealthUtils
        String bmi = HealthUtils.calculateBmi(profile.getWeightKg(), profile.getHeightCm());
        String category = HealthUtils.getBmiCategory(bmi);
        String bmiLabel = HealthUtils.formatBmiLabel(bmi, category);
        tvBmiBadge.setText(bmiLabel);

        int colorRes = HealthUtils.getBmiColorRes(category);
        int badgeColor = ContextCompat.getColor(this, colorRes);

        GradientDrawable pillDrawable = new GradientDrawable();
        pillDrawable.setShape(GradientDrawable.RECTANGLE);
        pillDrawable.setCornerRadius(dpToPx(12));
        pillDrawable.setColor(badgeColor);
        tvBmiBadge.setBackground(pillDrawable);
        tvBmiBadge.setTextColor(ContextCompat.getColor(this, R.color.hb_text_on_dark));

        boolean isComplete = ProfileSetupDialogHelper.isProfileComplete(profile);
        if (cardCompleteProfileBanner != null) {
            cardCompleteProfileBanner.setVisibility(isComplete ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Handles emergency SOS dial action. Uses Intent.ACTION_DIAL (no CALL_PHONE permission required).
     * Falls back to universal emergency number "112" if emergency contact is unlisted.
     */
    private void handleSosCall() {
        String phone = (currentProfile != null
                && currentProfile.getEmergencyContactPhone() != null
                && !currentProfile.getEmergencyContactPhone().trim().isEmpty())
                ? currentProfile.getEmergencyContactPhone().trim()
                : "";

        if (phone.isEmpty()) {
            Toast.makeText(this, R.string.sos_no_number, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        try {
            startActivity(dialIntent);
        } catch (ActivityNotFoundException | SecurityException e) {
            showPlainSnackbar(getString(R.string.error_dialer));
        }
    }

    /**
     * Launches Nearby Hospitals query using Google Maps or browser fallback.
     */
    private void handleNearbyHospitals() {
        Uri mapUri = Uri.parse("geo:0,0?q=hospitals near me");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException | SecurityException e) {
            // Fall back to web search if Google Maps is not installed
            try {
                Uri webUri = Uri.parse("https://www.google.com/maps/search/hospitals+near+me");
                Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                startActivity(webIntent);
            } catch (ActivityNotFoundException | SecurityException ex) {
                showPlainSnackbar(getString(R.string.error_maps));
            }
        }
    }

    /**
     * Unified generation and upload pipeline for the Doctor Summary PDF.
     * Fetches medications and medical_history via Retrofit, generates the 1-page PDF on a background thread,
     * and uploads it to Supabase Storage as doctor_summary_<profileId>.pdf.
     * Invokes onSuccess only after upload succeeds.
     *
     * @param triggeringView the tile view tapped by the user, disabled during operation
     * @param onSuccess callback executed on UI thread after successful upload
     */
    public interface SummaryResultCallback {
        void onResult(String cloudUrl, File localFile);
    }

    private void ensureSummaryUploaded(View triggeringView, SummaryResultCallback onSuccess, SummaryResultCallback onFailure) {
        if (currentProfile == null) {
            showPlainSnackbar(getString(R.string.error_create_summary));
            return;
        }

        if (triggeringView != null) {
            triggeringView.setEnabled(false);
            triggeringView.setAlpha(0.4f);
        }
        progressBar.setVisibility(View.VISIBLE);

        SupabaseApiService apiService = SupabaseClient.getApiService();
        String profileIdFilter = "eq." + currentProfile.getId();

        // Step 1: Fetch medications asynchronously via Retrofit
        apiService.getMedications("*", profileIdFilter).enqueue(new Callback<List<Medication>>() {
            @Override
            public void onResponse(Call<List<Medication>> call, Response<List<Medication>> medResponse) {
                if (isFinishing() || isDestroyed()) return;

                List<Medication> medications = (medResponse.isSuccessful() && medResponse.body() != null)
                        ? medResponse.body() : new ArrayList<>();

                // Step 2: Fetch medical history asynchronously via Retrofit
                apiService.getMedicalHistory("*", profileIdFilter).enqueue(new Callback<List<MedicalHistory>>() {
                    @Override
                    public void onResponse(Call<List<MedicalHistory>> call, Response<List<MedicalHistory>> histResponse) {
                        if (isFinishing() || isDestroyed()) return;

                        List<MedicalHistory> history = (histResponse.isSuccessful() && histResponse.body() != null)
                                ? histResponse.body() : new ArrayList<>();

                        generateAndUploadPdf(triggeringView, medications, history, onSuccess, onFailure);
                    }

                    @Override
                    public void onFailure(Call<List<MedicalHistory>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        generateAndUploadPdf(triggeringView, medications, new ArrayList<>(), onSuccess, onFailure);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Medication>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;

                apiService.getMedicalHistory("*", profileIdFilter).enqueue(new Callback<List<MedicalHistory>>() {
                    @Override
                    public void onResponse(Call<List<MedicalHistory>> call, Response<List<MedicalHistory>> histResponse) {
                        if (isFinishing() || isDestroyed()) return;

                        List<MedicalHistory> history = (histResponse.isSuccessful() && histResponse.body() != null)
                                ? histResponse.body() : new ArrayList<>();

                        generateAndUploadPdf(triggeringView, new ArrayList<>(), history, onSuccess, onFailure);
                    }

                    @Override
                    public void onFailure(Call<List<MedicalHistory>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        generateAndUploadPdf(triggeringView, new ArrayList<>(), new ArrayList<>(), onSuccess, onFailure);
                    }
                });
            }
        });
    }

    private void generateAndUploadPdf(View triggeringView, List<Medication> medications, List<MedicalHistory> history,
                                      SummaryResultCallback onSuccess, SummaryResultCallback onFailure) {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.execute(() -> {
            File pdfFile = null;
            try {
                pdfFile = HealthBridgePdfGenerator.generate(DashboardActivity.this, currentProfile, medications, history);
            } catch (Exception ignored) {
                pdfFile = null;
            }

            final File generatedFile = pdfFile;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (generatedFile == null || !generatedFile.exists()) {
                    resetActionState(triggeringView);
                    showPlainSnackbar(getString(R.string.error_create_summary));
                    if (onFailure != null) {
                        onFailure.onResult(null, null);
                    }
                    return;
                }

                String summaryTitle = "Doctor Summary - " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

                // Upload with SummaryPdfUploader (inserts medical_records row under category 'Summary')
                SummaryPdfUploader.upload(generatedFile, currentProfile.getId(), summaryTitle, new SummaryPdfUploader.Callback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        if (isFinishing() || isDestroyed()) return;
                        lastSummaryUrl = publicUrl;
                        resetActionState(triggeringView);
                        showPlainSnackbar(getString(R.string.success_summary_created));
                        if (onSuccess != null) {
                            onSuccess.onResult(publicUrl, generatedFile);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        resetActionState(triggeringView);
                        if (onFailure != null) {
                            onFailure.onResult(null, generatedFile);
                        } else {
                            showPlainSnackbar(getString(R.string.error_create_summary));
                        }
                    }
                });
            });
        });
    }

    /**
     * Doctor Summary click handler: ensures summary is generated and uploaded, then opens the PDF via ACTION_VIEW.
     */
    private void handleDoctorSummary() {
        if (currentProfile == null) {
            showPlainSnackbar(getString(R.string.error_create_summary));
            return;
        }

        ensureSummaryUploaded(
            btnDoctorSummary,
            (cloudUrl, localFile) -> openDoctorSummaryPdf(cloudUrl, localFile),
            (cloudUrl, localFile) -> {
                if (localFile != null && localFile.exists()) {
                    openDoctorSummaryPdf(null, localFile);
                } else {
                    showPlainSnackbar(getString(R.string.error_create_summary));
                }
            }
        );
    }

    /**
     * Opens the Doctor Summary PDF with ACTION_VIEW through the FileProvider with a browser fallback.
     */
    private void openDoctorSummaryPdf(String cloudUrl, File localFile) {
        if (localFile != null && localFile.exists()) {
            try {
                Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        localFile
                );
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException ignored) {
            } catch (Exception ignored) {
            }
        }

        String fallbackUrl = (cloudUrl != null && !cloudUrl.trim().isEmpty())
                ? cloudUrl
                : (currentProfile != null ? SupabaseClient.STORAGE_PUBLIC_URL + SupabaseClient.BUCKET_NAME + "/doctor_summary_" + currentProfile.getId() + ".pdf" : null);

        if (fallbackUrl != null) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(browserIntent);
            } catch (Exception ex) {
                showPlainSnackbar(getString(R.string.error_create_summary));
            }
        } else {
            showPlainSnackbar(getString(R.string.error_create_summary));
        }
    }

    /**
     * QR Code click handler: if summary URL exists, shows it directly.
     * Otherwise generates and uploads first. If offline/failure, shows Emergency Card QR.
     */
    private void handleQrCode() {
        if (currentProfile == null) {
            showPlainSnackbar(getString(R.string.error_qr));
            return;
        }

        if (lastSummaryUrl != null && !lastSummaryUrl.trim().isEmpty()) {
            QrCodeHelper.showQrDialog(this, lastSummaryUrl, getString(R.string.qr_dialog_title));
            return;
        }

        ensureSummaryUploaded(
            btnQrCode,
            (cloudUrl, localFile) -> {
                try {
                    QrCodeHelper.showQrDialog(DashboardActivity.this, cloudUrl, getString(R.string.qr_dialog_title));
                } catch (Exception e) {
                    showPlainSnackbar(getString(R.string.error_qr));
                }
            },
            (cloudUrl, localFile) -> {
                try {
                    String emergencyText = QrCodeHelper.buildEmergencyText(currentProfile);
                    QrCodeHelper.showQrDialog(DashboardActivity.this, emergencyText, "Emergency Card");
                } catch (Exception e) {
                    showPlainSnackbar(getString(R.string.error_qr));
                }
            }
        );
    }

    private void handleOpenVault() {
        Intent intent = new Intent(DashboardActivity.this, VaultActivity.class);
        startActivity(intent);
    }

    private void resetActionState(View triggeringView) {
        if (triggeringView != null) {
            triggeringView.setEnabled(true);
            triggeringView.setAlpha(1.0f);
        }
        progressBar.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        dashboardContentLayout.setAlpha(loading ? 0.35f : 1.0f);
    }

    private void showErrorSnackbar() {
        Snackbar.make(dashboardScrollView,
                getString(R.string.error_load_data),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, v -> loadDashboardData())
                .setActionTextColor(ContextCompat.getColor(this, R.color.hb_teal_primary))
                .show();
    }

    private void showPlainSnackbar(String message) {
        if (message == null || message.trim().isEmpty()) return;
        Snackbar.make(dashboardScrollView, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Computes time-of-day greeting (morning, afternoon, evening).
     */
    private String getTimeOfDayGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return getString(R.string.greeting_morning);
        } else if (hour >= 12 && hour < 17) {
            return getString(R.string.greeting_afternoon);
        } else {
            return getString(R.string.greeting_evening);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void checkProfileSetupPrompt(Profile profile) {
        if (profile == null) return;
        if (ProfileSetupDialogHelper.isProfileComplete(profile)) {
            return;
        }
        String userId = sessionManager.getUserId();
        if (sessionManager.isProfileSetupSkipped(userId)) {
            return;
        }
        showProfileSetupDialog();
    }

    private void showProfileSetupDialog() {
        ProfileSetupDialogHelper.show(this, currentProfile, updatedProfile -> {
            if (updatedProfile != null) {
                currentProfile = updatedProfile;
                sessionManager.saveProfileInfo(currentProfile.getId(), currentProfile.getFullName());
                bindProfileData(currentProfile);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
