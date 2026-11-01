package com.example.healthbridge.ui;

import android.os.Bundle;

/**
 * Dashboard screen (Protected Route).
 * Extends BaseProtectedActivity so unauthenticated users cannot access this screen.
 */
public class DashboardActivity extends BaseProtectedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Sonali — set content view to activity_dashboard.xml and implement logic
        // setContentView(R.layout.activity_dashboard);
    }
}
