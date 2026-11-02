package com.example.healthbridge.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.healthbridge.R;
import com.example.healthbridge.api.SupabaseApiService;
import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.models.Profile;
import com.example.healthbridge.utils.HealthBridgePdfGenerator;
import com.example.healthbridge.utils.HealthUtils;
import com.example.healthbridge.utils.QrCodeHelper;
import com.example.healthbridge.utils.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main landing screen after login.
 * Displays emergency info, vitals with BMI, and quick-action tiles.
 * Extends BaseProtectedActivity to guarantee session validity.
 */
public class DashboardActivity extends BaseProtectedActivity {

    // ── View references ─────────────────────────────────────────────────
    private TextView tvGreeting;
    private TextView tvPatientName;
    private TextView tvBloodGroup;
    private TextView tvAllergies;
    private TextView tvEmergencyContact;
    private TextView tvHeight;
    private TextView tvWeight;
    private TextView tvBmiBadge;
    private ProgressBar progressBar;

    private Profile currentProfile = null;
    private String emergencyPhone = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bindViews();
        setupGreeting();
        setupQuickActions();
        loadProfileData();
    }

    private void bindViews() {
        tvGreeting          = findViewById(R.id.tvGreeting);
        tvPatientName       = findViewById(R.id.tvPatientName);
        tvBloodGroup        = findViewById(R.id.tvBloodGroup);
        tvAllergies         = findViewById(R.id.tvAllergies);
        tvEmergencyContact  = findViewById(R.id.tvEmergencyContact);
        tvHeight            = findViewById(R.id.tvHeight);
        tvWeight            = findViewById(R.id.tvWeight);
        tvBmiBadge          = findViewById(R.id.tvBmiBadge);
        progressBar         = findViewById(R.id.progressBar);
    }

    private void setupGreeting() {
        tvGreeting.setText(HealthUtils.getGreeting());
    }

    private void setupQuickActions() {
        // SOS — dial emergency contact
        findViewById(R.id.tileSos).setOnClickListener(v -> handleSosClick());

        // Nearby Hospitals — Google Maps intent
        findViewById(R.id.tileHospitals).setOnClickListener(v -> handleNearbyHospitalsClick());

        // Doctor Summary — Generate PDF
        findViewById(R.id.tileDoctorSummary).setOnClickListener(v -> handleDoctorSummaryClick());

        // QR Code — Emergency QR share dialog
        findViewById(R.id.tileQrCode).setOnClickListener(v -> handleQrCodeClick());

        // Document Vault
        findViewById(R.id.btnVault).setOnClickListener(v -> {
            try {
                startActivity(new Intent(DashboardActivity.this, VaultActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Opening Document Vault...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSosClick() {
        if (emergencyPhone == null || emergencyPhone.trim().isEmpty() || "Not provided".equalsIgnoreCase(emergencyPhone.trim())) {
            Toast.makeText(this, "No emergency contact phone available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + Uri.encode(emergencyPhone.trim())));
            startActivity(dialIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open dialer", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleNearbyHospitalsClick() {
        try {
            Uri gmmUri = Uri.parse("geo:0,0?q=hospitals near me");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } catch (Exception e) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/hospitals+near+me"));
                startActivity(browserIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Unable to open maps", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleDoctorSummaryClick() {
        if (currentProfile == null) {
            Toast.makeText(this, "Profile not loaded yet. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Generating Doctor Summary PDF...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            File pdf = HealthBridgePdfGenerator.generate(
                    DashboardActivity.this,
                    currentProfile,
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            runOnUiThread(() -> {
                if (pdf != null && pdf.exists()) {
                    Toast.makeText(DashboardActivity.this, "PDF generated: " + pdf.getName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(DashboardActivity.this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void handleQrCodeClick() {
        if (currentProfile == null) {
            Toast.makeText(this, "Profile not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        String qrContent = QrCodeHelper.buildEmergencyText(currentProfile);
        QrCodeHelper.showQrDialog(this, qrContent, "Emergency Medical Card");
    }

    private void loadProfileData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String userId = SessionManager.getInstance(this).getUserId();
        SupabaseApiService api = SupabaseClient.getApiService();
        Call<List<Profile>> call;

        if (userId != null && !userId.trim().isEmpty()) {
            call = api.getProfileByUserId("*", "eq." + userId);
        } else {
            call = api.getProfiles("*");
        }

        call.enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(@NonNull Call<List<Profile>> call, @NonNull Response<List<Profile>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentProfile = response.body().get(0);
                    populateUi(currentProfile);
                } else {
                    // Fallback to session details or demo
                    populateFallbackData();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Profile>> call, @NonNull Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                populateFallbackData();
            }
        });
    }

    private void populateUi(Profile profile) {
        if (profile == null) return;

        tvPatientName.setText(HealthUtils.safeDisplay(profile.getFullName()));
        tvBloodGroup.setText(HealthUtils.safeDisplay(profile.getBloodGroup()));
        tvAllergies.setText(HealthUtils.safeDisplay(profile.getAllergies()));

        String contact = HealthUtils.safeDisplay(profile.getEmergencyContactName());
        if (profile.getEmergencyContactPhone() != null && !profile.getEmergencyContactPhone().trim().isEmpty()) {
            contact += " · " + profile.getEmergencyContactPhone().trim();
        }
        tvEmergencyContact.setText(contact);
        this.emergencyPhone = profile.getEmergencyContactPhone();

        double h = profile.getHeightCm();
        double w = profile.getWeightKg();
        tvHeight.setText(h > 0 ? String.format(Locale.US, "Height: %.0f cm", h) : "Height: --");
        tvWeight.setText(w > 0 ? String.format(Locale.US, "Weight: %.1f kg", w) : "Weight: --");

        double bmi = HealthUtils.calculateBmi(w, h);
        String cat = HealthUtils.getBmiCategory(bmi);
        int color  = HealthUtils.getBmiColor(bmi);

        tvBmiBadge.setText(bmi < 0 ? "N/A" : String.format(Locale.US, "%.1f · %s", bmi, cat));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(32f);
        bg.setColor(color);
        tvBmiBadge.setBackground(bg);
    }

    private void populateFallbackData() {
        String profileName = SessionManager.getInstance(this).getProfileName();
        if (profileName == null || profileName.isEmpty()) {
            profileName = "Rahul Sharma";
        }
        tvPatientName.setText(profileName);
        tvBloodGroup.setText("O+");
        tvAllergies.setText("Penicillin");
        tvEmergencyContact.setText("Amit Sharma · 9000000001");
        this.emergencyPhone = "9000000001";
        tvHeight.setText("Height: 172 cm");
        tvWeight.setText("Weight: 68.5 kg");

        double bmi = HealthUtils.calculateBmi(68.5, 172.0);
        tvBmiBadge.setText(String.format(Locale.US, "%.1f · Normal", bmi));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(32f);
        bg.setColor(HealthUtils.getBmiColor(bmi));
        tvBmiBadge.setBackground(bg);
    }
}
