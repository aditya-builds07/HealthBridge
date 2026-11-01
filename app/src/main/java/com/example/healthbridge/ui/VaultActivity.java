package com.example.healthbridge.ui;

import android.os.Bundle;

/**
 * Document Vault screen (Protected Route).
 * Extends BaseProtectedActivity so unauthenticated users cannot access this screen.
 */
public class VaultActivity extends BaseProtectedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Atharv — set content view to activity_vault.xml and implement logic
        // setContentView(R.layout.activity_vault);
    }
}
