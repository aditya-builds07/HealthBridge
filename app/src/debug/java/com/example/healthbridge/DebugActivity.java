package com.example.healthbridge;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * DEBUG-ONLY diagnostic activity for Health Bridge.
 * Troubleshoots network, Supabase config, GoTrue Auth, PostgREST tables, storage, and sessions.
 * Never ships in release builds (resides in src/debug).
 */
public class DebugActivity extends AppCompatActivity {

    private EditText editDebugEmail;
    private EditText editDebugPassword;
    private ScrollView scrollLog;
    private TextView tvLog;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    // In-memory test state from successful "Test Login"
    private String lastAccessToken = null;
    private String lastUserId = null;
    private String lastUserEmail = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // Initialize SupabaseClient with context for any static helpers
        SupabaseClient.init(this);

        editDebugEmail = findViewById(R.id.editDebugEmail);
        editDebugPassword = findViewById(R.id.editDebugPassword);
        scrollLog = findViewById(R.id.scrollLog);
        tvLog = findViewById(R.id.tvLog);

        Button btnConfig = findViewById(R.id.btnConfig);
        Button btnInternet = findViewById(R.id.btnInternet);
        Button btnTestLogin = findViewById(R.id.btnTestLogin);
        Button btnTestProfile = findViewById(R.id.btnTestProfile);
        Button btnTestTables = findViewById(R.id.btnTestTables);
        Button btnTestStorage = findViewById(R.id.btnTestStorage);
        Button btnSession = findViewById(R.id.btnSession);
        Button btnClearSession = findViewById(R.id.btnClearSession);
        Button btnCopyLog = findViewById(R.id.btnCopyLog);
        Button btnRunAll = findViewById(R.id.btnRunAll);
        Button btnClearLog = findViewById(R.id.btnClearLog);

        btnConfig.setOnClickListener(v -> testConfig());
        btnInternet.setOnClickListener(v -> testInternet(null));
        btnTestLogin.setOnClickListener(v -> testLogin(null));
        btnTestProfile.setOnClickListener(v -> testProfile(null));
        btnTestTables.setOnClickListener(v -> testTables(null));
        btnTestStorage.setOnClickListener(v -> testStorage(null));
        btnSession.setOnClickListener(v -> testSession());
        btnClearSession.setOnClickListener(v -> clearSession());
        btnCopyLog.setOnClickListener(v -> copyLog());
        btnRunAll.setOnClickListener(v -> runAllDiagnostics());
        btnClearLog.setOnClickListener(v -> tvLog.setText(""));
    }

    // ── 1. CONFIG ──────────────────────────────────────────────────
    private boolean testConfig() {
        logHeader("1. CHECK CONFIG");
        String url = BuildConfig.SUPABASE_URL;
        String key = BuildConfig.SUPABASE_KEY;

        boolean urlOk = url != null && !url.trim().isEmpty();
        boolean keyOk = key != null && !key.trim().isEmpty();

        log("BuildConfig.SUPABASE_URL: " + (urlOk ? url : "[EMPTY]"));
        log("BuildConfig.SUPABASE_KEY: " + maskKey(key));

        if (!urlOk || !keyOk) {
            log("[FAIL] Config is missing values!");
            if (!urlOk) log("-> LIKELY CAUSE: 'supabase.url' missing in local.properties");
            if (!keyOk) log("-> LIKELY CAUSE: 'supabase.key' missing in local.properties");
            return false;
        }

        if (!url.startsWith("https://")) {
            log("[FAIL] SUPABASE_URL must begin with https://");
            log("-> LIKELY CAUSE: Invalid URL scheme in local.properties");
            return false;
        }

        log("[PASS] Config values loaded from BuildConfig / local.properties.");
        return true;
    }

    // ── 2. INTERNET / AUTH HEALTH ──────────────────────────────────
    private void testInternet(Runnable onDone) {
        logHeader("2. INTERNET & AUTH HEALTH");
        String healthUrl = BuildConfig.SUPABASE_URL + "/auth/v1/health";
        log("GET " + healthUrl);

        Request request = new Request.Builder()
                .url(healthUrl)
                .header("apikey", BuildConfig.SUPABASE_KEY)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    log("[FAIL] Network call failed: " + e.getMessage());
                    log("-> LIKELY CAUSE: No internet connection or Supabase host is unreachable.");
                    if (onDone != null) onDone.run();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int code = response.code();
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    log("HTTP Status: " + code);
                    if (code == 200) {
                        log("[PASS] Supabase GoTrue Auth service is healthy: " + body);
                    } else {
                        log("[FAIL] Auth health returned HTTP " + code + ": " + body);
                        log("-> LIKELY CAUSE: Supabase project paused or invalid SUPABASE_URL.");
                    }
                    if (onDone != null) onDone.run();
                });
            }
        });
    }

    // ── 3. TEST LOGIN ──────────────────────────────────────────────
    private void testLogin(Runnable onDone) {
        logHeader("3. TEST LOGIN (POST /auth/v1/token?grant_type=password)");

        String email = editDebugEmail.getText() != null ? editDebugEmail.getText().toString().trim() : "";
        String password = editDebugPassword.getText() != null ? editDebugPassword.getText().toString() : "";

        if (email.isEmpty() || password.isEmpty()) {
            log("[FAIL] Email and password cannot be empty.");
            if (onDone != null) onDone.run();
            return;
        }

        String loginUrl = BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";
        log("Attempting direct OkHttp login for: " + email);

        try {
            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(mediaType, json.toString());

            Request request = new Request.Builder()
                    .url(loginUrl)
                    .header("apikey", BuildConfig.SUPABASE_KEY)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        log("[FAIL] Login network error: " + e.getMessage());
                        log("-> LIKELY CAUSE: Network failure or SSL handshake error.");
                        if (onDone != null) onDone.run();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    int code = response.code();
                    String resBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        log("HTTP Status: " + code);
                        if (code == 200) {
                            try {
                                JSONObject resJson = new JSONObject(resBody);
                                lastAccessToken = resJson.optString("access_token", null);
                                long expiresIn = resJson.optLong("expires_in", 0);
                                JSONObject userObj = resJson.optJSONObject("user");
                                lastUserId = userObj != null ? userObj.optString("id", null) : null;
                                lastUserEmail = userObj != null ? userObj.optString("email", email) : email;

                                log("[PASS] Login Successful!");
                                log("User ID: " + lastUserId);
                                log("User Email: " + lastUserEmail);
                                log("Token Expiry: " + expiresIn + " seconds (~" + (expiresIn / 60) + " mins)");
                                log("Access Token: RECEIVED successfully (length: "
                                        + (lastAccessToken != null ? lastAccessToken.length() : 0) + ", not printed)");
                            } catch (Exception ex) {
                                log("[WARN] Succeeded but failed parsing JSON: " + ex.getMessage());
                            }
                        } else {
                            log("[FAIL] Login failed! Raw error response:\n" + resBody);
                            interpretLoginFailure(code, resBody);
                        }
                        if (onDone != null) onDone.run();
                    });
                }
            });

        } catch (Exception e) {
            log("[FAIL] Exception building login request: " + e.getMessage());
            if (onDone != null) onDone.run();
        }
    }

    private void interpretLoginFailure(int code, String errorBody) {
        if (errorBody.contains("email_not_confirmed") || errorBody.toLowerCase().contains("email not confirmed")) {
            log("-> LIKELY CAUSE: email_not_confirmed.");
            log("   Rahul's email confirmation is required by Supabase.");
            log("   FIX: Run SQL in Supabase Dashboard:");
            log("   UPDATE auth.users SET email_confirmed_at = NOW() WHERE email = 'rahul@healthbridge.com';");
        } else if (errorBody.contains("invalid_credentials") || errorBody.contains("invalid_grant") || errorBody.toLowerCase().contains("invalid login credentials")) {
            log("-> LIKELY CAUSE: wrong password or user does not exist in auth.users.");
            log("   Verify that rahul@healthbridge.com is signed up with password 123456.");
        } else if (code == 401) {
            log("-> LIKELY CAUSE: Invalid Supabase API Key (SUPABASE_KEY). Verify local.properties.");
        } else {
            log("-> LIKELY CAUSE: HTTP " + code + " error. Check error message details above.");
        }
    }

    // ── 4. TEST PROFILE ────────────────────────────────────────────
    private void testProfile(Runnable onDone) {
        logHeader("4. TEST PROFILE QUERY (GET /rest/v1/profiles)");

        String token = lastAccessToken;
        String uid = lastUserId;

        // Fall back to SessionManager if testLogin was not run in this session
        if (token == null) {
            token = SessionManager.getInstance(this).getAccessToken();
        }
        if (uid == null) {
            uid = SessionManager.getInstance(this).getUserId();
        }

        String bearer = (token != null && !token.trim().isEmpty()) ? token : BuildConfig.SUPABASE_KEY;
        String authType = (token != null && !token.trim().isEmpty()) ? "User JWT" : "Anon Key";

        log("Using Auth: " + authType + ", Target user_id: " + (uid != null ? uid : "[ANY]"));

        String profileUrl;
        if (uid != null && !uid.trim().isEmpty()) {
            profileUrl = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?user_id=eq." + uid + "&select=*";
        } else {
            profileUrl = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?select=*&limit=5";
        }

        Request request = new Request.Builder()
                .url(profileUrl)
                .header("apikey", BuildConfig.SUPABASE_KEY)
                .header("Authorization", "Bearer " + bearer)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    log("[FAIL] Profile query network error: " + e.getMessage());
                    if (onDone != null) onDone.run();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int code = response.code();
                String resBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    log("HTTP Status: " + code);
                    if (code == 200) {
                        try {
                            JSONArray arr = new JSONArray(resBody);
                            int count = arr.length();
                            log("Profiles returned: " + count + " row(s)");

                            if (count > 0) {
                                JSONObject p = arr.getJSONObject(0);
                                log("[PASS] Profile Row Found!");
                                log("  id: " + p.optInt("id", -1));
                                log("  full_name: " + p.optString("full_name", "N/A"));
                                log("  user_id: " + p.optString("user_id", "N/A"));
                                log("  blood_group: " + p.optString("blood_group", "N/A"));
                            } else {
                                log("[FAIL] Query succeeded with HTTP 200 but returned 0 rows!");
                                log("-> LIKELY CAUSE: Profile row exists but user_id is NULL or not linked.");
                                log("   OR Row Level Security (RLS) is filtering rows for this role.");
                                log("   FIX: Link Rahul's user_id or disable RLS via database/cleanup.sql.");
                            }
                        } catch (Exception ex) {
                            log("[FAIL] JSON parsing error: " + ex.getMessage() + "\nRaw: " + resBody);
                        }
                    } else if (code == 401 || code == 403) {
                        log("[FAIL] Profile query forbidden/unauthorized (HTTP " + code + ")!");
                        log("Raw body: " + resBody);
                        log("-> LIKELY CAUSE: Row Level Security (RLS) is ENABLED on 'profiles' and blocking reads.");
                        log("   FIX: Run 'ALTER TABLE profiles DISABLE ROW LEVEL SECURITY;' or database/cleanup.sql.");
                    } else {
                        log("[FAIL] Profile query returned HTTP " + code + ": " + resBody);
                    }
                    if (onDone != null) onDone.run();
                });
            }
        });
    }

    // ── 5. TEST TABLES ─────────────────────────────────────────────
    private void testTables(Runnable onDone) {
        logHeader("5. TEST TABLES (medications, medical_history, medical_records)");

        String[] tables = new String[]{"medications", "medical_history", "medical_records"};
        testTableRecursive(tables, 0, onDone);
    }

    private void testTableRecursive(String[] tables, int index, Runnable onDone) {
        if (index >= tables.length) {
            log("[PASS] Completed table checks.");
            if (onDone != null) onDone.run();
            return;
        }

        String table = tables[index];
        String token = lastAccessToken != null ? lastAccessToken : BuildConfig.SUPABASE_KEY;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/" + table + "?select=*&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_KEY)
                .header("Authorization", "Bearer " + token)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    log("[" + table + "] FAIL network error: " + e.getMessage());
                    testTableRecursive(tables, index + 1, onDone);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int code = response.code();
                String body = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    if (code == 200) {
                        try {
                            JSONArray arr = new JSONArray(body);
                            log("[" + table + "] PASS (HTTP 200) - Rows returned: " + arr.length());
                        } catch (Exception e) {
                            log("[" + table + "] PASS (HTTP 200) - " + body);
                        }
                    } else if (code == 401 || code == 403) {
                        log("[" + table + "] FAIL (HTTP " + code + ") - RLS blocking reads!");
                        log("-> LIKELY CAUSE: Table '" + table + "' has RLS enabled.");
                    } else {
                        log("[" + table + "] FAIL (HTTP " + code + "): " + body);
                    }
                    testTableRecursive(tables, index + 1, onDone);
                });
            }
        });
    }

    // ── 6. TEST STORAGE ────────────────────────────────────────────
    private void testStorage(Runnable onDone) {
        logHeader("6. TEST STORAGE (healthbridge-files bucket)");

        // Test listing files via Storage API (accessible to anon/authenticated with public bucket policies)
        String listUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/list/" + SupabaseClient.BUCKET_NAME;
        log("POST " + listUrl);

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("prefix", "");
            bodyJson.put("limit", 10);
            bodyJson.put("offset", 0);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(mediaType, bodyJson.toString());

            String token = lastAccessToken != null ? lastAccessToken : BuildConfig.SUPABASE_KEY;

            Request request = new Request.Builder()
                    .url(listUrl)
                    .header("apikey", BuildConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        log("[FAIL] Storage check network error: " + e.getMessage());
                        if (onDone != null) onDone.run();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    int code = response.code();
                    String resBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        log("Storage API HTTP Status: " + code);
                        if (code == 200) {
                            try {
                                JSONArray arr = new JSONArray(resBody);
                                log("[PASS] Storage bucket '" + SupabaseClient.BUCKET_NAME
                                        + "' is active, public, and accessible! (Files count: " + arr.length() + ")");
                            } catch (Exception e) {
                                log("[PASS] Storage bucket '" + SupabaseClient.BUCKET_NAME + "' accessible: " + resBody);
                            }
                        } else if (code == 404) {
                            log("[FAIL] Bucket '" + SupabaseClient.BUCKET_NAME + "' NOT FOUND (HTTP 404)!");
                            log("-> LIKELY CAUSE: Bucket has not been created yet in Supabase Storage.");
                        } else {
                            log("[WARN] Bucket returned HTTP " + code + ": " + resBody);
                        }
                        if (onDone != null) onDone.run();
                    });
                }
            });
        } catch (Exception e) {
            log("[FAIL] Error preparing storage request: " + e.getMessage());
            if (onDone != null) onDone.run();
        }
    }

    // ── 7. SESSION ─────────────────────────────────────────────────
    private void testSession() {
        logHeader("7. CURRENT SESSION STATE");

        SessionManager sm = SessionManager.getInstance(this);
        boolean loggedIn = sm.isLoggedIn();
        String userId = sm.getUserId();
        String userEmail = sm.getUserEmail();
        int profileId = sm.getProfileId();
        String profileName = sm.getProfileName();

        SharedPreferences prefs = getSharedPreferences("HealthBridgeSession", Context.MODE_PRIVATE);
        long expiresAt = prefs.getLong("expires_at", 0);
        String token = sm.getAccessToken();

        log("isLoggedIn(): " + loggedIn);
        log("User ID: " + (userId != null ? userId : "(null)"));
        log("User Email: " + (userEmail != null ? userEmail : "(null)"));
        log("Profile ID: " + profileId);
        log("Profile Name: " + (profileName != null ? profileName : "(null)"));
        log("Token Saved: " + (token != null && !token.isEmpty() ? "YES (masked for security)" : "NO"));

        if (expiresAt > 0) {
            Date expDate = new Date(expiresAt);
            long diffMs = expiresAt - System.currentTimeMillis();
            String diffStr = diffMs > 0 ? (diffMs / 1000 / 60) + " mins remaining" : "EXPIRED";
            log("Expires At: " + expDate + " (" + diffStr + ")");
        } else {
            log("Expires At: Not recorded (0)");
        }

        if (loggedIn) {
            log("[PASS] Valid active session stored in SharedPreferences.");
        } else {
            log("[INFO] No active session currently stored.");
        }
    }

    // ── 8. CLEAR SESSION ───────────────────────────────────────────
    private void clearSession() {
        logHeader("8. CLEAR SESSION");
        SessionManager.getInstance(this).clearSession();
        lastAccessToken = null;
        lastUserId = null;
        lastUserEmail = null;
        log("[PASS] SharedPreferences session and in-memory tokens cleared.");
        Toast.makeText(this, "Session cleared", Toast.LENGTH_SHORT).show();
    }

    // ── 9. COPY LOG ────────────────────────────────────────────────
    private void copyLog() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            ClipData clip = ClipData.newPlainText("HealthBridge Diagnostic Log", tvLog.getText().toString());
            cm.setPrimaryClip(clip);
            Toast.makeText(this, "Diagnostic log copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    // ── RUN ALL DIAGNOSTICS SEQUENTIALLY ───────────────────────────
    private void runAllDiagnostics() {
        tvLog.setText("");
        log("=== STARTING FULL HEALTH BRIDGE DIAGNOSTICS ===");

        boolean configOk = testConfig();
        if (!configOk) {
            log("\n[ABORTED] Fix config in local.properties before continuing.");
            return;
        }

        testInternet(() -> {
            testLogin(() -> {
                testProfile(() -> {
                    testTables(() -> {
                        testStorage(() -> {
                            testSession();
                            logSummaryInterpretation();
                        });
                    });
                });
            });
        });
    }

    private void logSummaryInterpretation() {
        logHeader("DIAGNOSTIC SUMMARY & LIKELY CAUSE");
        if (lastAccessToken == null) {
            log("-> PRIMARY ISSUE: Authentication failed. See Section 3 for exact error from Supabase.");
            log("   Common causes: email_not_confirmed (run cleanup.sql) or invalid credentials.");
        } else {
            log("[ALL CHECKS FINISHED] If login works here but fails in LoginActivity, verify that");
            log("SessionManager and local.properties credentials match, and check Logcat for Activity exceptions.");
        }
    }

    // ── HELPERS ────────────────────────────────────────────────────
    private void logHeader(String header) {
        log("\n────────────────────────────────────────");
        log("▶ " + header);
        log("────────────────────────────────────────");
    }

    private void log(String message) {
        String timestamp = timeFormat.format(new Date());
        String line = "[" + timestamp + "] " + message + "\n";
        tvLog.append(line);

        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    private String maskKey(String key) {
        if (key == null || key.trim().isEmpty()) return "(empty)";
        if (key.length() <= 10) return "****";
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }
}

