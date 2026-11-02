package com.example.healthbridge.utils;

import android.content.Context;

/**
 * Dialog wrapper for adding Medications (PRD P3).
 */
public class MedicationDialog {

    public static void show(Context ctx, int profileId, Runnable onSaved) {
        MedicationDialogHelper.show(ctx, profileId, onSaved);
    }
}

