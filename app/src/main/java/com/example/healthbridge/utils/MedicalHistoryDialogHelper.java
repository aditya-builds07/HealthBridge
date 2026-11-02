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
import com.example.healthbridge.models.MedicalHistory;
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
 * Reusable helper class for creating and displaying the "Add Medical History" dialog.
 * Handles user input validation, Retrofit POST integration with Supabase,
 * loading state feedback, and duplicate submission prevention.
 */
public class MedicalHistoryDialogHelper {

    /**
     * Callback interface to notify when a medical history entry is successfully added.
     */
    public interface OnMedicalHistoryAddedListener {
        void onMedicalHistoryAdded(MedicalHistory history);
    }

    /**
     * Displays the Add Medical History dialog.
     *
     * @param context  Calling Activity or Context
     * @param listener Optional callback invoked upon successful insertion
     */
    public static void showAddMedicalHistoryDialog(final Context context, final OnMedicalHistoryAddedListener listener) {
        if (context == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_medical_history, null);

        final TextInputEditText editCondition = dialogView.findViewById(R.id.editHistoryCondition);
        final TextInputEditText editDate = dialogView.findViewById(R.id.editHistoryDate);
        final TextInputEditText editNotes = dialogView.findViewById(R.id.editHistoryNotes);
        final ProgressBar progressHistory = dialogView.findViewById(R.id.progressHistory);
        final MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelHistory);
        final MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveHistory);

        // Pre-fill today's date in YYYY-MM-DD format
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        editDate.setText(dateFormat.format(calendar.getTime()));

        // Date picker dialog on clicking the date field
        editDate.setFocusable(false);
        editDate.setClickable(true);
        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        context,
                        (view, year, month, dayOfMonth) -> {
                            calendar.set(Calendar.YEAR, year);
                            calendar.set(Calendar.MONTH, month);
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            editDate.setText(dateFormat.format(calendar.getTime()));
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
                String condition = editCondition.getText() != null ? editCondition.getText().toString().trim() : "";
                String diagnosedOn = editDate.getText() != null ? editDate.getText().toString().trim() : "";
                String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : "";

                // Field validations
                if (condition.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.error_history_condition_required), Toast.LENGTH_SHORT).show();
                    editCondition.requestFocus();
                    return;
                }
                if (diagnosedOn.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.error_history_date_required), Toast.LENGTH_SHORT).show();
                    editDate.requestFocus();
                    return;
                }
                if (notes.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.error_history_notes_required), Toast.LENGTH_SHORT).show();
                    editNotes.requestFocus();
                    return;
                }

                // Prevent duplicate submissions & show progress
                btnSave.setEnabled(false);
                btnCancel.setEnabled(false);
                progressHistory.setVisibility(View.VISIBLE);
                dialog.setCancelable(false);

                // Retrieve authenticated user's profile_id (fallback to 1 for demo persona if profileId not set)
                int profileId = SessionManager.getInstance(context).getProfileId();
                if (profileId <= 0) {
                    profileId = 1;
                }

                final MedicalHistory newHistory = new MedicalHistory(profileId, condition, notes, diagnosedOn);

                SupabaseApiService apiService = SupabaseClient.getApiService();
                apiService.insertMedicalHistory(newHistory).enqueue(new Callback<List<MedicalHistory>>() {
                    @Override
                    public void onResponse(Call<List<MedicalHistory>> call, Response<List<MedicalHistory>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, context.getString(R.string.success_history_added), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            if (listener != null) {
                                MedicalHistory created = (response.body() != null && !response.body().isEmpty())
                                        ? response.body().get(0)
                                        : newHistory;
                                listener.onMedicalHistoryAdded(created);
                            }
                        } else {
                            btnSave.setEnabled(true);
                            btnCancel.setEnabled(true);
                            progressHistory.setVisibility(View.GONE);
                            dialog.setCancelable(true);
                            Toast.makeText(context, context.getString(R.string.error_history_add_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<MedicalHistory>> call, Throwable t) {
                        btnSave.setEnabled(true);
                        btnCancel.setEnabled(true);
                        progressHistory.setVisibility(View.GONE);
                        dialog.setCancelable(true);
                        Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.show();
    }
}

