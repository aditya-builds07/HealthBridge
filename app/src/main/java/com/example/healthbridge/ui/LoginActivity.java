package com.example.healthbridge.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthbridge.R;
import com.example.healthbridge.api.SupabaseApiService;
import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.models.Profile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Demo login screen with pre-filled credentials.
 * Validates against the profiles table and navigates to DashboardActivity on success.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "HealthBridgePrefs";
    private static final String KEY_USER_ID = "user_id";

    // Demo credentials
    private static final String DEMO_EMAIL = "rahul@healthbridge.com";
    private static final String DEMO_PASSWORD = "123456";

    private TextInputEditText editEmail, editPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    private void attemptLogin() {
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";

        // Validate empty fields
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_error_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate demo credentials
        if (!email.equals(DEMO_EMAIL) || !password.equals(DEMO_PASSWORD)) {
            Toast.makeText(this, getString(R.string.login_error_invalid), Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress, disable button
        setLoading(true);

        // Verify against Supabase — fetch the demo profile
        SupabaseApiService api = SupabaseClient.getApiService();
        api.getProfiles("*").enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);

                    // Save user_id to SharedPreferences
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit()
                            .putInt(KEY_USER_ID, profile.getId())
                            .putString("user_name", profile.getFullName())
                            .apply();

                    Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();

                    // Navigate to Dashboard
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_error_invalid), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}

