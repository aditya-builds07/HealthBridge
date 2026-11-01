package com.example.healthbridge.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthbridge.R;
import com.example.healthbridge.utils.HealthUtils;

import java.util.Locale;

// ── INTEGRATION PLACEHOLDERS ────────────────────────────────────────────────
// The imports below will resolve once the corresponding files are merged:
//
// import com.example.healthbridge.api.SupabaseApiService;   // Aditya
// import com.example.healthbridge.api.SupabaseClient;       // Aditya
// import com.example.healthbridge.models.Profile;            // Aditya
// import com.example.healthbridge.utils.HealthBridgePdfGenerator; // Aditya
// import com.example.healthbridge.utils.QrCodeHelper;        // Aditya
// import com.example.healthbridge.ui.VaultActivity;          // Atharv
//
// SessionManager (Priti) — used to retrieve user_id from SharedPreferences
// import com.example.healthbridge.SessionManager;            // Priti
// ────────────────────────────────────────────────────────────────────────────

/**
 * Main landing screen after login.
 * Displays emergency info, vitals with BMI, and quick-action tiles.
 *
 * Owner: Sonali
 *
 * INTEGRATION NOTES — items that need other team members' code:
 *  1. Retrofit profile fetch      → needs SupabaseClient + SupabaseApiService (Aditya)
 *  2. Profile model class         → needs models/Profile.java (Aditya)
 *  3. SessionManager.getUserId()  → needs SessionManager.java (Priti)
 *  4. Doctor Summary button       → needs HealthBridgePdfGenerator (Aditya)
 *  5. QR Code button              → needs QrCodeHelper (Aditya)
 *  6. Vault button                → needs VaultActivity (Atharv)
 *  7. Drawable resources          → bg_gradient, bg_glass_card, bg_emergency_card,
 *                                    bg_button_primary (Aditya)
 *  8. Color resources             → colors.xml entries (Aditya)
 */
public class DashboardActivity extends AppCompatActivity {

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

    // ── Data from profile (populated after API call) ────────────────────
    private String emergencyPhone = null;

    // ====================================================================
    // Lifecycle
    // ====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bindViews();
        setupGreeting();
        setupQuickActions();
        loadProfileData();
    }

    // ====================================================================
    // View binding
    // ====================================================================

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

    // ====================================================================
    // Greeting (time-of-day)
    // ====================================================================

    private void setupGreeting() {
        tvGreeting.setText(HealthUtils.getGreeting());
    }

    // ====================================================================
    // Quick-action tile click handlers
    // ====================================================================

    private void setupQuickActions() {

        // SOS — dial emergency contact
        findViewById(R.id.tileSos).setOnClickListener(v -> handleSosClick());

        // Nearby Hospitals — Google Maps intent
        findViewById(R.id.tileHospitals).setOnClickListener(v -> handleNearbyHospitalsClick());

        // Doctor Summary — PDF generation
        // TODO [INTEGRATION - BLOCKED] Wire to Aditya's HealthBridgePdfGenerator
        findViewById(R.id.tileDoctorSummary).setOnClickListener(v ->
                Toast.makeText(this,
                        "Doctor Summary — awaiting PDF generator integration",
                        Toast.LENGTH_SHORT).show());

        // QR Code — share popup
        // TODO [INTEGRATION - BLOCKED] Wire to Aditya's QrCodeHelper
        findViewById(R.id.tileQrCode).setOnClickListener(v ->
                Toast.makeText(this,
                        "QR Code — awaiting QR helper integration",
                        Toast.LENGTH_SHORT).show());

        // Medical Vault
        // TODO [INTEGRATION - BLOCKED] Wire to Atharv's VaultActivity
        findViewById(R.id.btnVault).setOnClickListener(v -> {
            try {
                // Uncomment when VaultActivity is available:
                // startActivity(new Intent(DashboardActivity.this, VaultActivity.class));
                Toast.makeText(this,
                        "Medical Vault — awaiting VaultActivity integration",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open vault", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────
    // SOS — Intent.ACTION_DIAL (PRD F2)
    // Uses ACTION_DIAL, NOT ACTION_CALL → no CALL_PHONE permission needed.
    // ────────────────────────────────────────────────────────────────────

    private void handleSosClick() {
        if (emergencyPhone == null || emergencyPhone.trim().isEmpty() || "Not provided".equalsIgnoreCase(emergencyPhone.trim())) {
            Toast.makeText(this,
                    "No emergency contact available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + Uri.encode(emergencyPhone.trim())));
            startActivity(dialIntent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Unable to open dialer", Toast.LENGTH_SHORT).show();
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Nearby Hospitals — Google Maps intent with browser fallback (PRD F3)
    // ────────────────────────────────────────────────────────────────────

    private void handleNearbyHospitalsClick() {
        try {
            Uri gmmUri = Uri.parse("geo:0,0?q=hospitals near me");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } catch (Exception e) {
            // Fallback: open in browser
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/hospitals+near+me"));
                startActivity(browserIntent);
            } catch (Exception ex) {
                Toast.makeText(this,
                        "Unable to open maps", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ====================================================================
    // Profile data loading
    // ====================================================================

    /**
     * Fetches the user profile from Supabase via Retrofit and populates the UI.
     *
     * TODO [INTEGRATION - BLOCKED] Replace the simulated loader below with the real
     * Retrofit call once Aditya's SupabaseClient, SupabaseApiService, Profile model,
     * and Priti's SessionManager are committed to main.
     *
     * The call will be:
     *   String userId = SessionManager.getUserId(this);            // Priti
     *   SupabaseApiService api = SupabaseClient.getService();      // Aditya
     *   Call<List<Profile>> call = api.getProfile("eq." + userId); // Aditya
     *   call.enqueue(new Callback<List<Profile>>() { ... });
     */
    private void loadProfileData() {
        progressBar.setVisibility(View.VISIBLE);

        // ── PLACEHOLDER: Simulated profile for UI verification ──────────
        // Remove this block and uncomment the Retrofit call below after integration.
        simulateProfileLoad(
                "Rahul Sharma",
                "B+",
                "Penicillin, Dust",
                "Priya Sharma",
                "+91-9876543210",
                170.0,  // height cm
                72.0    // weight kg
        );
    }

    /**
     * Temporary method that populates the UI with hardcoded demo data
     * so the layout can be verified before the API layer is ready.
     * DELETE this method after real Retrofit integration.
     */
    private void simulateProfileLoad(String name, String bloodGroup, String allergies,
                                     String contactName, String contactPhone,
                                     double heightCm, double weightKg) {

        progressBar.setVisibility(View.GONE);

        // Patient name
        tvPatientName.setText(HealthUtils.safeDisplay(name));

        // Emergency card
        tvBloodGroup.setText(HealthUtils.safeDisplay(bloodGroup));
        tvAllergies.setText(HealthUtils.safeDisplay(allergies));

        String contactDisplay = HealthUtils.safeDisplay(contactName);
        if (contactPhone != null && !contactPhone.trim().isEmpty() && !"null".equalsIgnoreCase(contactPhone.trim())) {
            contactDisplay += " · " + contactPhone.trim();
        }
        tvEmergencyContact.setText(contactDisplay);

        // Store phone for SOS
        this.emergencyPhone = contactPhone;

        // Vitals
        tvHeight.setText(heightCm > 0 ? String.format(Locale.US, "Height: %.0f cm", heightCm) : "Height: --");
        tvWeight.setText(weightKg > 0 ? String.format(Locale.US, "Weight: %.1f kg", weightKg) : "Weight: --");

        // BMI
        double bmi = HealthUtils.calculateBmi(weightKg, heightCm);
        String bmiCategory = HealthUtils.getBmiCategory(bmi);
        int bmiColor = HealthUtils.getBmiColor(bmi);

        if (bmi < 0) {
            tvBmiBadge.setText("N/A");
        } else {
            tvBmiBadge.setText(String.format(Locale.US, "%.1f · %s", bmi, bmiCategory));
        }

        // Apply BMI badge background color as a rounded pill
        GradientDrawable bmiBg = new GradientDrawable();
        bmiBg.setShape(GradientDrawable.RECTANGLE);
        bmiBg.setCornerRadius(32f);  // pill shape
        bmiBg.setColor(bmiColor);
        tvBmiBadge.setBackground(bmiBg);
    }

    // ====================================================================
    // Retrofit integration template (uncomment when dependencies are ready)
    // ====================================================================
    /*
    private void loadProfileFromApi() {
        progressBar.setVisibility(View.VISIBLE);

        // Get user_id from session
        String userId = SessionManager.getUserId(this);
        if (userId == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        SupabaseApiService api = SupabaseClient.getService();
        Call<List<Profile>> call = api.getProfile("eq." + userId);

        call.enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);
                    populateUi(profile);
                } else {
                    Toast.makeText(DashboardActivity.this,
                            "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DashboardActivity.this,
                        "Network error. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUi(Profile profile) {
        tvPatientName.setText(HealthUtils.safeDisplay(profile.getFullName()));
        tvBloodGroup.setText(HealthUtils.safeDisplay(profile.getBloodGroup()));
        tvAllergies.setText(HealthUtils.safeDisplay(profile.getAllergies()));

        String contact = HealthUtils.safeDisplay(profile.getEmergencyContactName());
        if (profile.getEmergencyContactPhone() != null) {
            contact += " · " + profile.getEmergencyContactPhone();
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
    */
}
