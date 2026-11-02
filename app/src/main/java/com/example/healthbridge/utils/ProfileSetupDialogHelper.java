package com.example.healthbridge.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.healthbridge.models.Profile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper class for displaying and managing the Personal Health Profile Setup popup.
 * Collects age/DOB, blood group, height, weight, allergies, and emergency contact.
 * Persists updates to the Supabase profiles table.
 */
public class ProfileSetupDialogHelper {

    public interface OnProfileUpdatedListener {
        void onProfileUpdated(Profile updatedProfile);
    }

    /**
     * Checks if a patient profile has already completed basic health details.
     */
    public static boolean isProfileComplete(Profile p) {
        if (p == null) return false;
        String bg = p.getBloodGroup();
        boolean hasBloodGroup = bg != null && !bg.trim().isEmpty()
                && !bg.equalsIgnoreCase("Unknown")
                && !bg.equalsIgnoreCase("Not provided")
                && !bg.equalsIgnoreCase("Select Blood Group");
        boolean hasVitals = p.getHeightCm() > 0 && p.getWeightKg() > 0;
        String phone = p.getEmergencyContactPhone();
        boolean hasEmergency = phone != null && !phone.trim().isEmpty()
                && !phone.equalsIgnoreCase("Not provided");
        return hasBloodGroup && (hasVitals || hasEmergency);
    }

    /**
     * Displays the Health Profile Setup popup dialog.
     */
    public static void show(final Context context, final Profile profile, final OnProfileUpdatedListener listener) {
        if (context == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_profile_setup, null);

        final TextInputLayout layoutAge = dialogView.findViewById(R.id.layoutAge);
        final TextInputLayout layoutDob = dialogView.findViewById(R.id.layoutDob);
        final TextInputLayout layoutHeight = dialogView.findViewById(R.id.layoutHeight);
        final TextInputLayout layoutWeight = dialogView.findViewById(R.id.layoutWeight);
        final TextInputLayout layoutAllergies = dialogView.findViewById(R.id.layoutAllergies);
        final TextInputLayout layoutEmergencyName = dialogView.findViewById(R.id.layoutEmergencyName);
        final TextInputLayout layoutEmergencyRelation = dialogView.findViewById(R.id.layoutEmergencyRelation);
        final TextInputLayout layoutEmergencyPhone = dialogView.findViewById(R.id.layoutEmergencyPhone);

        final TextInputEditText editAge = dialogView.findViewById(R.id.editAge);
        final TextInputEditText editDob = dialogView.findViewById(R.id.editDob);
        final TextInputEditText editHeight = dialogView.findViewById(R.id.editHeight);
        final TextInputEditText editWeight = dialogView.findViewById(R.id.editWeight);
        final TextInputEditText editAllergies = dialogView.findViewById(R.id.editAllergies);
        final TextInputEditText editEmergencyName = dialogView.findViewById(R.id.editEmergencyName);
        final TextInputEditText editEmergencyRelation = dialogView.findViewById(R.id.editEmergencyRelation);
        final TextInputEditText editEmergencyPhone = dialogView.findViewById(R.id.editEmergencyPhone);

        final Spinner spinnerBloodGroup = dialogView.findViewById(R.id.spinnerBloodGroup);
        final ProgressBar progressProfileSetup = dialogView.findViewById(R.id.progressProfileSetup);
        final MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveProfileSetup);
        final MaterialButton btnSkip = dialogView.findViewById(R.id.btnSkipProfileSetup);

        // Allergy suggestion chips
        final Chip chipNoAllergies = dialogView.findViewById(R.id.chipNoAllergies);
        final Chip chipPenicillin = dialogView.findViewById(R.id.chipPenicillin);
        final Chip chipPeanuts = dialogView.findViewById(R.id.chipPeanuts);
        final Chip chipSulfa = dialogView.findViewById(R.id.chipSulfa);
        final Chip chipLatex = dialogView.findViewById(R.id.chipLatex);
        final Chip chipAspirin = dialogView.findViewById(R.id.chipAspirin);

        // Blood Group Spinner Setup
        final String[] bloodGroupOptions = new String[]{
                "Select Blood Group",
                "A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−", "Unknown"
        };
        ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, bloodGroupOptions);
        spinnerBloodGroup.setAdapter(bgAdapter);

        // Pre-fill existing data if profile already contains some information
        if (profile != null) {
            if (profile.getAge() > 0) {
                editAge.setText(String.valueOf(profile.getAge()));
            }
            if (profile.getHeightCm() > 0) {
                editHeight.setText(String.format(Locale.US, "%.1f", profile.getHeightCm()));
            }
            if (profile.getWeightKg() > 0) {
                editWeight.setText(String.format(Locale.US, "%.1f", profile.getWeightKg()));
            }
            if (profile.getAllergies() != null && !profile.getAllergies().equalsIgnoreCase("Not provided")) {
                editAllergies.setText(profile.getAllergies());
            }

            int bgIndex = findBloodGroupIndex(bloodGroupOptions, profile.getBloodGroup());
            spinnerBloodGroup.setSelection(bgIndex);

            String contactName = profile.getEmergencyContactName();
            if (contactName != null && !contactName.trim().isEmpty() && !contactName.equalsIgnoreCase("Not provided")) {
                int openParen = contactName.indexOf('(');
                int closeParen = contactName.indexOf(')');
                if (openParen > 0 && closeParen > openParen) {
                    editEmergencyName.setText(contactName.substring(0, openParen).trim());
                    editEmergencyRelation.setText(contactName.substring(openParen + 1, closeParen).trim());
                } else {
                    editEmergencyName.setText(contactName.trim());
                }
            }

            String phone = profile.getEmergencyContactPhone();
            if (phone != null && !phone.trim().isEmpty() && !phone.equalsIgnoreCase("Not provided")) {
                editEmergencyPhone.setText(phone.trim());
            }
        }

        // Setup Date of Birth Picker with automatic age calculation
        final Calendar cal = Calendar.getInstance();
        editDob.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, month);
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        editDob.setText(sdf.format(cal.getTime()));

                        Calendar today = Calendar.getInstance();
                        int calculatedAge = today.get(Calendar.YEAR) - year;
                        if (today.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) {
                            calculatedAge--;
                        }
                        if (calculatedAge >= 0 && calculatedAge <= 125) {
                            editAge.setText(String.valueOf(calculatedAge));
                            layoutAge.setError(null);
                        }
                    },
                    cal.get(Calendar.YEAR) - 25,
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePicker.show();
        });

        // Setup allergy suggestion chips
        if (chipNoAllergies != null) {
            chipNoAllergies.setOnClickListener(v -> editAllergies.setText(context.getString(R.string.chip_no_allergies)));
        }
        setupAllergyChip(chipPenicillin, editAllergies);
        setupAllergyChip(chipPeanuts, editAllergies);
        setupAllergyChip(chipSulfa, editAllergies);
        setupAllergyChip(chipLatex, editAllergies);
        setupAllergyChip(chipAspirin, editAllergies);

        // Real-time error clearance
        clearErrorOnTextChange(editAge, layoutAge);
        clearErrorOnTextChange(editHeight, layoutHeight);
        clearErrorOnTextChange(editWeight, layoutWeight);
        clearErrorOnTextChange(editEmergencyName, layoutEmergencyName);
        clearErrorOnTextChange(editEmergencyPhone, layoutEmergencyPhone);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Skip for Now button listener
        btnSkip.setOnClickListener(v -> {
            String userId = profile != null && profile.getUserId() != null
                    ? profile.getUserId()
                    : SessionManager.getInstance(context).getUserId();
            SessionManager.getInstance(context).setProfileSetupSkipped(userId, true);
            dialog.dismiss();
            Toast.makeText(context, R.string.profile_setup_skipped_hint, Toast.LENGTH_LONG).show();
        });

        // Save & Continue button listener
        btnSave.setOnClickListener(v -> {
            boolean isValid = true;

            // 1. Validate Age
            String ageStr = editAge.getText() != null ? editAge.getText().toString().trim() : "";
            int age = 0;
            if (ageStr.isEmpty()) {
                layoutAge.setError(context.getString(R.string.error_age_required));
                isValid = false;
            } else {
                try {
                    age = Integer.parseInt(ageStr);
                    if (age <= 0 || age > 120) {
                        layoutAge.setError(context.getString(R.string.error_age_invalid));
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    layoutAge.setError(context.getString(R.string.error_age_invalid));
                    isValid = false;
                }
            }

            // 2. Validate Blood Group
            int selectedBgPos = spinnerBloodGroup.getSelectedItemPosition();
            if (selectedBgPos <= 0) {
                Toast.makeText(context, R.string.error_blood_group_required, Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            String selectedBloodGroup = selectedBgPos > 0 ? bloodGroupOptions[selectedBgPos] : "Unknown";

            // 3. Validate Height
            String heightStr = editHeight.getText() != null ? editHeight.getText().toString().trim() : "";
            double height = 0;
            if (heightStr.isEmpty()) {
                layoutHeight.setError(context.getString(R.string.error_height_required));
                isValid = false;
            } else {
                try {
                    height = Double.parseDouble(heightStr);
                    if (height < 30 || height > 260) {
                        layoutHeight.setError(context.getString(R.string.error_height_invalid));
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    layoutHeight.setError(context.getString(R.string.error_height_invalid));
                    isValid = false;
                }
            }

            // 4. Validate Weight
            String weightStr = editWeight.getText() != null ? editWeight.getText().toString().trim() : "";
            double weight = 0;
            if (weightStr.isEmpty()) {
                layoutWeight.setError(context.getString(R.string.error_weight_required));
                isValid = false;
            } else {
                try {
                    weight = Double.parseDouble(weightStr);
                    if (weight < 2 || weight > 350) {
                        layoutWeight.setError(context.getString(R.string.error_weight_invalid));
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    layoutWeight.setError(context.getString(R.string.error_weight_invalid));
                    isValid = false;
                }
            }

            // 5. Allergies
            String allergies = editAllergies.getText() != null ? editAllergies.getText().toString().trim() : "";
            if (allergies.isEmpty()) {
                allergies = "No known allergies";
            }

            // 6. Validate Emergency Contact
            String emergencyName = editEmergencyName.getText() != null ? editEmergencyName.getText().toString().trim() : "";
            if (emergencyName.isEmpty()) {
                layoutEmergencyName.setError(context.getString(R.string.error_emergency_name_required));
                isValid = false;
            }

            String emergencyPhone = editEmergencyPhone.getText() != null ? editEmergencyPhone.getText().toString().trim() : "";
            if (emergencyPhone.isEmpty()) {
                layoutEmergencyPhone.setError(context.getString(R.string.error_emergency_phone_required));
                isValid = false;
            } else if (emergencyPhone.replaceAll("[^0-9]", "").length() < 7) {
                layoutEmergencyPhone.setError(context.getString(R.string.error_emergency_phone_invalid));
                isValid = false;
            }

            if (!isValid) {
                return;
            }

            // Relationship
            String relation = editEmergencyRelation.getText() != null ? editEmergencyRelation.getText().toString().trim() : "";
            String fullEmergencyName = relation.isEmpty() ? emergencyName : emergencyName + " (" + relation + ")";

            // Prepare Supabase JSON payload
            Map<String, Object> updates = new HashMap<>();
            updates.put("age", age);
            updates.put("blood_group", selectedBloodGroup);
            updates.put("height_cm", height);
            updates.put("weight_kg", weight);
            updates.put("allergies", allergies);
            updates.put("emergency_contact_name", fullEmergencyName);
            updates.put("emergency_contact_phone", emergencyPhone);

            // Update UI to loading state
            btnSave.setEnabled(false);
            btnSkip.setEnabled(false);
            progressProfileSetup.setVisibility(View.VISIBLE);

            final int finalAge = age;
            final double finalHeight = height;
            final double finalWeight = weight;
            final String finalBloodGroup = selectedBloodGroup;
            final String finalAllergies = allergies;
            final String finalContactName = fullEmergencyName;
            final String finalContactPhone = emergencyPhone;

            int profileId = profile != null ? profile.getId() : SessionManager.getInstance(context).getProfileId();
            String userId = profile != null && profile.getUserId() != null
                    ? profile.getUserId()
                    : SessionManager.getInstance(context).getUserId();

            SupabaseApiService apiService = SupabaseClient.getApiService();
            Call<List<Profile>> call;
            if (profileId > 0) {
                call = apiService.updateProfile("eq." + profileId, updates);
            } else {
                call = apiService.updateProfileByUserId("eq." + userId, updates);
            }

            call.enqueue(new Callback<List<Profile>>() {
                @Override
                public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                    if (response.isSuccessful()) {
                        Profile updated = profile;
                        if (response.body() != null && !response.body().isEmpty()) {
                            updated = response.body().get(0);
                        } else if (updated != null) {
                            updated.setAge(finalAge);
                            updated.setBloodGroup(finalBloodGroup);
                            updated.setHeightCm(finalHeight);
                            updated.setWeightKg(finalWeight);
                            updated.setAllergies(finalAllergies);
                            updated.setEmergencyContactName(finalContactName);
                            updated.setEmergencyContactPhone(finalContactPhone);
                        }

                        // Mark setup completed for this user
                        SessionManager.getInstance(context).setProfileSetupSkipped(userId, false);

                        Toast.makeText(context, R.string.profile_setup_success, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        if (listener != null) {
                            listener.onProfileUpdated(updated);
                        }
                    } else {
                        btnSave.setEnabled(true);
                        btnSkip.setEnabled(true);
                        progressProfileSetup.setVisibility(View.GONE);
                        Toast.makeText(context, R.string.profile_setup_failed, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<Profile>> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnSkip.setEnabled(true);
                    progressProfileSetup.setVisibility(View.GONE);
                    Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private static void setupAllergyChip(final Chip chip, final TextInputEditText editText) {
        if (chip == null || editText == null) return;
        chip.setOnClickListener(v -> {
            String chipText = chip.getText() != null ? chip.getText().toString().trim() : "";
            String current = editText.getText() != null ? editText.getText().toString().trim() : "";

            if (current.isEmpty() || current.equalsIgnoreCase("No known allergies") || current.equalsIgnoreCase("None")) {
                editText.setText(chipText);
            } else if (!current.toLowerCase().contains(chipText.toLowerCase())) {
                editText.setText(current + ", " + chipText);
            }
        });
    }

    private static void clearErrorOnTextChange(TextInputEditText editText, TextInputLayout layout) {
        if (editText == null || layout == null) return;
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private static int findBloodGroupIndex(String[] options, String bloodGroup) {
        if (bloodGroup == null || bloodGroup.trim().isEmpty()) return 0;
        String clean = bloodGroup.replace("−", "-").trim();
        for (int i = 0; i < options.length; i++) {
            String optClean = options[i].replace("−", "-").trim();
            if (optClean.equalsIgnoreCase(clean)) {
                return i;
            }
        }
        return 0;
    }
}

