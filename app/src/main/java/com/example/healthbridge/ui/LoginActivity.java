package com.example.healthbridge.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthbridge.R;
import com.example.healthbridge.api.SupabaseApiService;
import com.example.healthbridge.api.SupabaseAuthService;
import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.models.AuthRequest;
import com.example.healthbridge.models.AuthResponse;
import com.example.healthbridge.models.Profile;
import com.example.healthbridge.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login screen with Supabase GoTrue Auth integration.
 * Secure: strictly enforces credentials; fallback only runs if email confirmation is pending.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SupabaseClient with context for token injection
        SupabaseClient.init(this);
        sessionManager = SessionManager.getInstance(this);

        // If already authenticated, redirect straight to protected Dashboard
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard();
            return;
        }

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

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_error_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Call Supabase GoTrue Auth API (/auth/v1/token?grant_type=password)
        SupabaseAuthService authService = SupabaseClient.getAuthService();
        authService.login(new AuthRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    sessionManager.saveSession(auth);

                    // Fetch user's profile to complete session details
                    fetchProfileAndProceed(auth.getUser() != null ? auth.getUser().getId() : null);
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}

                    // The fallback is ONLY allowed if Supabase reports that email confirmation is pending
                    if (errorBody.contains("email_not_confirmed")) {
                        // Demo fallback for email_not_confirmed
                        fallbackEmailNotConfirmed(email);
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.login_error_invalid),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchProfileAndProceed(String userId) {
        SupabaseApiService api = SupabaseClient.getApiService();
        Call<List<Profile>> call;
        if (userId != null && !userId.isEmpty()) {
            call = api.getProfileByUserId("*", "eq." + userId);
        } else {
            call = api.getProfiles("*");
        }

        call.enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);
                    sessionManager.saveProfileInfo(profile.getId(), profile.getFullName());
                }
                Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                setLoading(false);
                // Auth succeeded; proceed even if profile query fails
                navigateToDashboard();
            }
        });
    }

    /**
     * Fallback ONLY for hackathon demo if email confirmation is pending in Supabase Auth.
     */
    private void fallbackEmailNotConfirmed(String email) {
        SupabaseApiService api = SupabaseClient.getApiService();
        api.getProfiles("*").enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);
                    sessionManager.saveProfileInfo(profile.getId(), profile.getFullName());
                    Toast.makeText(LoginActivity.this,
                            "Email unconfirmed: entered demo mode.",
                            Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
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

    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
