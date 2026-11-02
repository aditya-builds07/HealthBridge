package com.example.healthbridge.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
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
     * Displays the Add Medication dialog.
     *
     * @param context  Calling Activity or Context
     * @param listener Optional callback invoked upon successful insertion
     */
    public static void showAddMedicationDialog(final Context context, final OnMedicationAddedListener listener) {
        if (context == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_medication, null);

        final TextInputEditText editName = dialogView.findViewById(R.id.editMedicationName);
        final TextInputEditText editDosage = dialogView.findViewById(R.id.editMedicationDosage);
        final TextInputEditText editStartDate = dialogView.findViewById(R.id.editMedicationStartDate);
        final TextInputEditText editStatus = dialogView.findViewById(R.id.editMedicationStatus);
        final ProgressBar progressMedication = dialogView.findViewById(R.id.progressMedication);
        final MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelMedication);
        final MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveMedication);

        // Pre-fill today's date in YYYY-MM-DD format
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        editStartDate.setText(dateFormat.format(calendar.getTime()));

        // Date picker dialog on clicking the start date field
        editStartDate.setFocusable(false);
        editStartDate.setClickable(true);
        editStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(
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
                ).show();
            }
        });

        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                String dosage = editDosage.getText() != null ? editDosage.getText().toString().trim() : "";
                String startDate = editStartDate.getText() != null ? editStartDate.getText().toString().trim() : "";
                String status = editStatus.getText() != null ? editStatus.getText().toString().trim() : "";

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
                if (startDate.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.error_medication_start_date_required), Toast.LENGTH_SHORT).show();
                    editStartDate.requestFocus();
                    return;
                }
                if (status.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.error_medication_status_required), Toast.LENGTH_SHORT).show();
                    editStatus.requestFocus();
                    return;
                }

                // Prevent duplicate submissions & show progress
                btnSave.setEnabled(false);
                btnCancel.setEnabled(false);
                progressMedication.setVisibility(View.VISIBLE);
                dialog.setCancelable(false);

                // Retrieve authenticated user's profile_id (fallback to 1 for demo persona if profileId not set)
                int profileId = SessionManager.getInstance(context).getProfileId();
                if (profileId <= 0) {
                    profileId = 1;
                }

                final Medication newMedication = new Medication(profileId, name, dosage, "Daily", startDate, status);

                SupabaseApiService apiService = SupabaseClient.getApiService();
                apiService.insertMedication(newMedication).enqueue(new Callback<List<Medication>>() {
                    @Override
                    public void onResponse(Call<List<Medication>> call, Response<List<Medication>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, context.getString(R.string.success_medication_added), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
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
            }
        });

        dialog.show();
    }
}

