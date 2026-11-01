package com.example.healthbridge.utils;

import java.util.Calendar;

/**
 * Pure utility class for health-related calculations and formatting.
 * No Android Context dependencies — keeps methods testable and clean.
 *
 * Owner: Sonali
 */
public final class HealthUtils {

    private HealthUtils() {
        // Prevent instantiation
    }

    // ──────────────────────────────────────────────
    // BMI Calculation
    // ──────────────────────────────────────────────

    /**
     * Calculates BMI from weight (kg) and height (cm).
     * Formula: BMI = weight / (height_m²)
     *
     * Rules:
     *  - Rejects Double.NaN and positive/negative Infinity for either input
     *  - Rejects zero or negative height or weight (<= 0)
     *  - Returns -1.0 for any invalid input or calculation
     *  - Returns valid BMI rounded to 1 decimal place
     *
     * @param weightKg  Weight in kilograms
     * @param heightCm  Height in centimetres
     * @return BMI rounded to 1 decimal place, or -1.0 if inputs are invalid
     */
    public static double calculateBmi(double weightKg, double heightCm) {
        if (Double.isNaN(weightKg) || Double.isInfinite(weightKg) ||
            Double.isNaN(heightCm) || Double.isInfinite(heightCm) ||
            weightKg <= 0 || heightCm <= 0) {
            return -1.0;
        }

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);

        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            return -1.0;
        }

        return Math.round(bmi * 10.0) / 10.0; // round to 1 decimal place
    }

    /**
     * Returns the WHO category string for a given BMI value.
     *
     * | BMI Range                 | Category      |
     * |---------------------------|---------------|
     * | <= 0 / NaN / ±Infinity    | "N/A"         |
     * | < 18.5                    | "Underweight" |
     * | 18.5 – 24.9               | "Normal"      |
     * | 25.0 – 29.9               | "Overweight"  |
     * | ≥ 30.0                    | "Obese"       |
     *
     * @param bmi  The BMI value to evaluate
     * @return WHO category name or "N/A" if invalid
     */
    public static String getBmiCategory(double bmi) {
        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            return "N/A";
        }
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    /**
     * Returns a color int (packed ARGB) for the BMI badge background.
     *
     * | Category    | Color   | Hex     |
     * |-------------|---------|---------|
     * | Invalid/N/A | Grey    | #9E9E9E |
     * | Underweight | Blue    | #1E88E5 |
     * | Normal      | Green   | #43A047 |
     * | Overweight  | Orange  | #FB8C00 |
     * | Obese       | Red     | #E53935 |
     *
     * @param bmi  The BMI value to evaluate
     * @return Packed ARGB color int
     */
    public static int getBmiColor(double bmi) {
        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            return 0xFF9E9E9E; // Grey (#9E9E9E)
        }
        if (bmi < 18.5) return 0xFF1E88E5; // Blue (#1E88E5)
        if (bmi < 25.0) return 0xFF43A047; // Green (#43A047)
        if (bmi < 30.0) return 0xFFFB8C00; // Orange (#FB8C00)
        return 0xFFE53935;                  // Red (#E53935)
    }

    // ──────────────────────────────────────────────
    // Greeting
    // ──────────────────────────────────────────────

    /**
     * Returns a time-of-day greeting string.
     *   Before 12:00 → "Good Morning,"
     *   12:00–17:00  → "Good Afternoon,"
     *   After 17:00  → "Good Evening,"
     */
    public static String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning,";
        if (hour < 17) return "Good Afternoon,";
        return "Good Evening,";
    }

    // ──────────────────────────────────────────────
    // Display helpers
    // ──────────────────────────────────────────────

    /**
     * Returns the trimmed value if present, otherwise "Not provided".
     * Explicitly filters out null and literal "null" strings to satisfy PRD rule F1.4.
     */
    public static String safeDisplay(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return "Not provided";
        }
        return value.trim();
    }
}
