package com.example.healthbridge.utils;

import android.content.Context;

/**
 * Dialog wrapper for adding Medical History (PRD P4).
 */
public class HistoryDialog {

    public static void show(Context ctx, int profileId, Runnable onSaved) {
        MedicalHistoryDialogHelper.show(ctx, profileId, onSaved);
    }
}

