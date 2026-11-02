package com.example.healthbridge.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthbridge.BuildConfig;
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
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Sign-Up Activity using Supabase GoTrue Auth and PostgREST.
 * Allows patients to create an account, links/creates their profile row,
 * and seamlessly transitions into DashboardActivity.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout layoutFullName, layoutEmail, layoutPassword, layoutConfirmPassword;
    private TextInputEditText editFullName, editEmail, editPassword, editConfirmPassword;
    private MaterialButton btnSignUp;
    private TextView textBackToLogin;
    private ProgressBar progressSignUp;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Ensure Supabase client is initialized with context
        SupabaseClient.init(this);
        sessionManager = SessionManager.getInstance(this);

        layoutFullName = findViewById(R.id.layoutFullName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);

        editFullName = findViewById(R.id.editFullName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        btnSignUp = findViewById(R.id.btnSignUp);
        textBackToLogin = findViewById(R.id.textBackToLogin);
        progressSignUp = findViewById(R.id.progressSignUp);

        btnSignUp.setOnClickListener(v -> attemptSignUp());
        textBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptSignUp() {
        String fullName = editFullName.getText() != null ? editFullName.getText().toString().trim() : "";
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString() : "";
        String confirmPassword = editConfirmPassword.getText() != null ? editConfirmPassword.getText().toString() : "";

        // Reset errors
        layoutFullName.setError(null);
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutConfirmPassword.setError(null);

        boolean isValid = true;

        if (fullName.isEmpty()) {
            layoutFullName.setError(getString(R.string.signup_error_name_empty));
            isValid = false;
        }

        if (email.isEmpty()) {
            layoutEmail.setError(getString(R.string.signup_error_email_empty));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.signup_error_email_invalid));
            isValid = false;
        }

        if (password.length() < 6) {
            layoutPassword.setError(getString(R.string.signup_error_password_length));
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError(getString(R.string.signup_error_password_mismatch));
            isValid = false;
        }

        if (!isValid) return;

        setLoading(true);

        SupabaseAuthService authService = SupabaseClient.getAuthService();
        authService.signUp(new AuthRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();

                    if (auth.getAccessToken() != null && !auth.getAccessToken().trim().isEmpty()) {
                        // Access token returned -> Save session and ensure profile
                        sessionManager.saveSession(auth);
                        String userId = auth.getUser() != null ? auth.getUser().getId() : null;
                        ensureProfile(userId, email, fullName);
                    } else {
                        // Supabase email confirmation is enabled (no session issued yet)
                        setLoading(false);
                        Toast.makeText(SignUpActivity.this,
                                getString(R.string.signup_email_confirmation),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    setLoading(false);
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}

                    String lowerError = errorBody.toLowerCase();
                    if (lowerError.contains("already registered") || lowerError.contains("already exists") || lowerError.contains("user_already_exists")) {
                        layoutEmail.setError(getString(R.string.signup_email_exists));
                        Toast.makeText(SignUpActivity.this,
                                getString(R.string.signup_email_exists),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                getString(R.string.signup_failed),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(SignUpActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Checks if a profile row exists for this user_id. If none exists, creates one.
     */
    private void ensureProfile(String userId, String email, String fullName) {
        if (userId == null || userId.isEmpty()) {
            setLoading(false);
            navigateToDashboard();
            return;
        }

        SupabaseApiService api = SupabaseClient.getApiService();
        api.getProfileByUserId("*", "eq." + userId).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(@NonNull Call<List<Profile>> call, @NonNull Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);
                    sessionManager.saveProfileInfo(profile.getId(),
                            profile.getFullName() != null ? profile.getFullName() : fullName);
                    setLoading(false);
                    Toast.makeText(SignUpActivity.this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                } else {
                    // Profile row does not exist yet -> Insert new profile row
                    insertNewProfile(userId, email, fullName);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Profile>> call, @NonNull Throwable t) {
                // If query fails, attempt insertion
                insertNewProfile(userId, email, fullName);
            }
        });
    }

    /**
     * Inserts a minimal profile row: user_id, email, full_name.
     */
    private void insertNewProfile(String userId, String email, String fullName) {
        try {
            JSONObject profileJson = new JSONObject();
            profileJson.put("user_id", userId);
            profileJson.put("email", email);
            profileJson.put("full_name", fullName);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(mediaType, profileJson.toString());

            String postUrl = BuildConfig.SUPABASE_URL + "/rest/v1/profiles";
            Request request = new Request.Builder()
                    .url(postUrl)
                    .post(body)
                    .build();

            SupabaseClient.getHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(SignUpActivity.this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    });
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        try {
                            JSONArray arr = new JSONArray(resBody);
                            if (arr.length() > 0) {
                                JSONObject created = arr.getJSONObject(0);
                                int profileId = created.optInt("id", -1);
                                if (profileId != -1) {
                                    sessionManager.saveProfileInfo(profileId, fullName);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(SignUpActivity.this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    });
                }
            });
        } catch (Exception e) {
            setLoading(false);
            navigateToDashboard();
        }
    }

    private void setLoading(boolean loading) {
        progressSignUp.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignUp.setEnabled(!loading);
        textBackToLogin.setEnabled(!loading);
        editFullName.setEnabled(!loading);
        editEmail.setEnabled(!loading);
        editPassword.setEnabled(!loading);
        editConfirmPassword.setEnabled(!loading);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

