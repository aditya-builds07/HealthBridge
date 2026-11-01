package com.example.healthbridge.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.healthbridge.models.AuthResponse;

/**
 * Manages Supabase Auth user sessions and JWT access tokens using SharedPreferences.
 */
public class SessionManager {

    private static final String PREF_NAME = "HealthBridgeSession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_PROFILE_ID = "profile_id";
    private static final String KEY_PROFILE_NAME = "profile_name";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Persist tokens and user details from a successful Supabase Auth response.
     */
    public void saveSession(AuthResponse authResponse) {
        if (authResponse == null) return;

        long expiresAt = System.currentTimeMillis() + (authResponse.getExpiresIn() * 1000L);
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_ACCESS_TOKEN, authResponse.getAccessToken())
                .putString(KEY_REFRESH_TOKEN, authResponse.getRefreshToken())
                .putLong(KEY_EXPIRES_AT, expiresAt);

        if (authResponse.getUser() != null) {
            editor.putString(KEY_USER_ID, authResponse.getUser().getId())
                  .putString(KEY_USER_EMAIL, authResponse.getUser().getEmail());
        }

        editor.apply();
    }

    /**
     * Save the linked local profile ID and name.
     */
    public void saveProfileInfo(int profileId, String profileName) {
        prefs.edit()
                .putInt(KEY_PROFILE_ID, profileId)
                .putString(KEY_PROFILE_NAME, profileName)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public int getProfileId() {
        return prefs.getInt(KEY_PROFILE_ID, -1);
    }

    public String getProfileName() {
        return prefs.getString(KEY_PROFILE_NAME, null);
    }

    /**
     * Check if a valid session exists.
     */
    public boolean isLoggedIn() {
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
        // If expiry is stored, ensure current time is before expiry
        return expiresAt == 0 || System.currentTimeMillis() < expiresAt;
    }

    /**
     * Clear all stored session credentials (logout).
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}

