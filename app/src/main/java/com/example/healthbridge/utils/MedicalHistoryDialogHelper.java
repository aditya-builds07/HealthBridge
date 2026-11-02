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
     * Shows medical history dialog adhering to PRD requirements.
     *
     * @param ctx       Activity or Context
     * @param profileId Profile ID to associate medical history with
     * @param onSaved   Callback invoked when saved successfully
     */
    public static void show(final Context ctx, final int profileId, final Runnable onSaved) {
        showDialogInternal(ctx, profileId, onSaved, null);
    }

    /**
     * Displays the Add Medical History dialog with custom listener.
     */
    public static void showAddMedicalHistoryDialog(final Context context, final OnMedicalHistoryAddedListener listener) {
        int profileId = SessionManager.getInstance(context).getProfileId();
        if (profileId <= 0) {
            profileId = 1;
        }
        showDialogInternal(context, profileId, null, listener);
    }

    private static void showDialogInternal(final Context context, final int profileId, final Runnable onSaved, final OnMedicalHistoryAddedListener listener) {
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
        editDate.setOnClickListener(v -> new DatePickerDialog(
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
        ).show());

        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String condition = editCondition.getText() != null ? editCondition.getText().toString().trim() : "";
            String diagnosedOn = editDate.getText() != null ? editDate.getText().toString().trim() : "";
            String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : "";

            // Field validations: condition and diagnosis date are required; notes is optional
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

            // Prevent duplicate submissions & show progress
            btnSave.setEnabled(false);
            btnCancel.setEnabled(false);
            progressHistory.setVisibility(View.VISIBLE);
            dialog.setCancelable(false);

            int finalProfileId = profileId > 0 ? profileId : 1;
            final MedicalHistory newHistory = new MedicalHistory(finalProfileId, condition, notes, diagnosedOn);

            SupabaseApiService apiService = SupabaseClient.getApiService();
            apiService.insertMedicalHistory(newHistory).enqueue(new Callback<List<MedicalHistory>>() {
                @Override
                public void onResponse(Call<List<MedicalHistory>> call, Response<List<MedicalHistory>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(context, context.getString(R.string.success_history_added), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        if (onSaved != null) {
                            onSaved.run();
                        }
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
        });

        dialog.show();
    }
}
