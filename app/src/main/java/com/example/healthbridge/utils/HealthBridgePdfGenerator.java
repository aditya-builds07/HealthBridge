package com.example.healthbridge.utils;

import android.content.Context;

import com.example.healthbridge.models.MedicalHistory;
import com.example.healthbridge.models.Medication;
import com.example.healthbridge.models.Profile;

import java.io.File;
import java.util.List;

/**
 * Utility for generating the 1-page Doctor Summary PDF using Android native PdfDocument.
 */
public class HealthBridgePdfGenerator {

    /**
     * Stub method for generating the doctor summary PDF.
     * To be implemented by Aditya.
     */
    public static File generate(Context ctx, Profile p, List<Medication> meds, List<MedicalHistory> history) {
        return null; // stub
    }
}

