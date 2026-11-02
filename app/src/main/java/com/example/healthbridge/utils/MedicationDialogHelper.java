package com.example.healthbridge.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.healthbridge.R;
import com.example.healthbridge.api.SupabaseApiService;
import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.models.Medication;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reusable helper class for creating and displaying the "Add Medication" dialog.
 * Handles user input validation, Retrofit POST integration with Supabase,
 * loading state feedback, and duplicate submission prevention.
 */
public class MedicationDialogHelper {

    /**
     * Callback interface to notify when a medication is successfully added.
     */
    public interface OnMedicationAddedListener {
        void onMedicationAdded(Medication medication);
    }

    /**
     * Shows medication dialog adhering to PRD requirements.
     *
     * @param ctx       Activity or Context
     * @param profileId Profile ID to associate medication with
     * @param onSaved   Callback invoked when saved successfully
     */
    public static void show(final Context ctx, final int profileId, final Runnable onSaved) {
        showDialogInternal(ctx, profileId, onSaved, null);
    }

    /**
     * Displays the Add Medication dialog with custom listener.
     */
    public static void showAddMedicationDialog(final Context context, final OnMedicationAddedListener listener) {
        int profileId = SessionManager.getInstance(context).getProfileId();
        if (profileId <= 0) {
            profileId = 1;
        }
        showDialogInternal(context, profileId, null, listener);
    }

    private static void showDialogInternal(final Context context, final int profileId, final Runnable onSaved, final OnMedicationAddedListener listener) {
        if (context == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_medication, null);

        final TextInputEditText editName = dialogView.findViewById(R.id.editMedicationName);
        final TextInputEditText editDosage = dialogView.findViewById(R.id.editMedicationDosage);
        final TextInputEditText editFrequency = dialogView.findViewById(R.id.editMedicationFrequency);
        final TextInputEditText editStartDate = dialogView.findViewById(R.id.editMedicationStartDate);
        final Spinner spinnerStatus = dialogView.findViewById(R.id.spinnerMedicationStatus);
        final ProgressBar progressMedication = dialogView.findViewById(R.id.progressMedication);
        final MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelMedication);
        final MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveMedication);

        // Setup status spinner with "Active" and "Completed"
        String[] statusOptions = new String[]{"Active", "Completed"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, statusOptions);
        if (spinnerStatus != null) {
            spinnerStatus.setAdapter(statusAdapter);
            spinnerStatus.setSelection(0);
        }

        // Pre-fill today's date in YYYY-MM-DD format
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        editStartDate.setText(dateFormat.format(calendar.getTime()));

        // Date picker dialog on clicking the start date field
        editStartDate.setFocusable(false);
        editStartDate.setClickable(true);
        editStartDate.setOnClickListener(v -> new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    editStartDate.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show());

        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = editName.getText() != null ? editName.getText().toString().trim() : "";
            String dosage = editDosage.getText() != null ? editDosage.getText().toString().trim() : "";
            String frequency = editFrequency != null && editFrequency.getText() != null ? editFrequency.getText().toString().trim() : "";
            String startDate = editStartDate.getText() != null ? editStartDate.getText().toString().trim() : "";
            String status = spinnerStatus != null && spinnerStatus.getSelectedItem() != null
                    ? spinnerStatus.getSelectedItem().toString()
                    : "Active";

            // Field validations
            if (name.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.error_medication_name_required), Toast.LENGTH_SHORT).show();
                editName.requestFocus();
                return;
            }
            if (dosage.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.error_medication_dosage_required), Toast.LENGTH_SHORT).show();
                editDosage.requestFocus();
                return;
            }
            if (frequency.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.error_medication_frequency_required), Toast.LENGTH_SHORT).show();
                if (editFrequency != null) editFrequency.requestFocus();
                return;
            }
            if (startDate.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.error_medication_start_date_required), Toast.LENGTH_SHORT).show();
                editStartDate.requestFocus();
                return;
            }

            // Prevent duplicate submissions & show progress
            btnSave.setEnabled(false);
            btnCancel.setEnabled(false);
            progressMedication.setVisibility(View.VISIBLE);
            dialog.setCancelable(false);

            int finalProfileId = profileId > 0 ? profileId : 1;
            final Medication newMedication = new Medication(finalProfileId, name, dosage, frequency, startDate, status);

            SupabaseApiService apiService = SupabaseClient.getApiService();
            apiService.insertMedication(newMedication).enqueue(new Callback<List<Medication>>() {
                @Override
                public void onResponse(Call<List<Medication>> call, Response<List<Medication>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(context, context.getString(R.string.success_medication_added), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        if (onSaved != null) {
                            onSaved.run();
                        }
                        if (listener != null) {
                            Medication created = (response.body() != null && !response.body().isEmpty())
                                    ? response.body().get(0)
                                    : newMedication;
                            listener.onMedicationAdded(created);
                        }
                    } else {
                        btnSave.setEnabled(true);
                        btnCancel.setEnabled(true);
                        progressMedication.setVisibility(View.GONE);
                        dialog.setCancelable(true);
                        Toast.makeText(context, context.getString(R.string.error_medication_add_failed), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<Medication>> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnCancel.setEnabled(true);
                    progressMedication.setVisibility(View.GONE);
                    dialog.setCancelable(true);
                    Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }
}
