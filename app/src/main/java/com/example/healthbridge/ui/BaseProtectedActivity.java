package com.example.healthbridge.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthbridge.utils.SessionManager;

/**
 * Base Activity for all protected routes (DashboardActivity, VaultActivity, etc.).
 * Automatically checks for an active Supabase session.
 * If the user is unauthenticated or the session has expired, they are redirected
 * to LoginActivity and the back-stack is cleared so they cannot navigate back.
 */
public abstract class BaseProtectedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSession();
    }

    private void checkSession() {
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            redirectToLogin();
        }
    }

    /**
     * Redirects the user to the LoginActivity and clears the navigation task stack.
     */
    protected void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Logs the current user out, clears session tokens, and navigates to Login.
     */
    protected void performLogout() {
        SessionManager.getInstance(this).clearSession();
        redirectToLogin();
    }
}

