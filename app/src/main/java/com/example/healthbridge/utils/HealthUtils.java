package com.example.healthbridge.utils;

import java.util.Calendar;

/**
 * Pure utility class for health-related calculations and formatting.
 * No Android Context dependencies.
 */
public final class HealthUtils {

    private HealthUtils() {
        // Prevent instantiation
    }

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

        return Math.round(bmi * 10.0) / 10.0;
    }

    public static String getBmiCategory(double bmi) {
        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            return "N/A";
        }
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    public static int getBmiColor(double bmi) {
        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            return 0xFF9E9E9E; // Grey (#9E9E9E)
        }
        if (bmi < 18.5) return 0xFF1E88E5; // Blue (#1E88E5)
        if (bmi < 25.0) return 0xFF43A047; // Green (#43A047)
        if (bmi < 30.0) return 0xFFFB8C00; // Orange (#FB8C00)
        return 0xFFE53935;                  // Red (#E53935)
    }

    public static String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning,";
        if (hour < 17) return "Good Afternoon,";
        return "Good Evening,";
    }

    public static String safeDisplay(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return "Not provided";
        }
        return value.trim();
    }
}
