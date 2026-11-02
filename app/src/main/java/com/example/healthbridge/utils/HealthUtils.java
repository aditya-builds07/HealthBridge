package com.example.healthbridge.utils;

import androidx.annotation.ColorRes;

import com.example.healthbridge.R;

import java.util.Locale;

/**
 * Pure static health calculation and formatting utilities.
 * Null-safe, thread-safe, contains no Android UI code except resource IDs.
 */
public final class HealthUtils {

    public static final String CATEGORY_UNDERWEIGHT = "Underweight";
    public static final String CATEGORY_NORMAL = "Normal";
    public static final String CATEGORY_OVERWEIGHT = "Overweight";
    public static final String CATEGORY_OBESE = "Obese";
    public static final String STATE_NA = "N/A";

    private HealthUtils() {
        // Prevent instantiation
    }

    /**
     * Calculates BMI = weight (kg) / (height in meters)^2, rounded to 1 decimal place.
     * Returns "N/A" if weight or height is missing, zero, negative, or invalid.
     * Never crashes or divides by zero.
     *
     * @param weightKg patient weight in kilograms
     * @param heightCm patient height in centimeters
     * @return formatted 1-decimal BMI string (e.g. "22.4") or "N/A"
     */
    public static String calculateBmi(double weightKg, double heightCm) {
        if (weightKg <= 0 || heightCm <= 0
                || Double.isNaN(weightKg) || Double.isNaN(heightCm)
                || Double.isInfinite(weightKg) || Double.isInfinite(heightCm)) {
            return STATE_NA;
        }

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);

        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            return STATE_NA;
        }

        return String.format(Locale.US, "%.1f", bmi);
    }

    /**
     * Computes the numeric BMI value.
     * Returns -1.0 if inputs are missing, zero, or invalid.
     *
     * @param weightKg patient weight in kilograms
     * @param heightCm patient height in centimeters
     * @return rounded 1-decimal numeric BMI or -1.0 if invalid
     */
    public static double calculateBmiValue(double weightKg, double heightCm) {
        String bmiStr = calculateBmi(weightKg, heightCm);
        if (STATE_NA.equals(bmiStr)) {
            return -1.0;
        }
        try {
            return Double.parseDouble(bmiStr);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    /**
     * Maps a numeric BMI to its standard WHO category.
     * Underweight (&lt; 18.5), Normal (18.5 to 24.9), Overweight (25 to 29.9), Obese (&gt;= 30.0), or N/A.
     *
     * @param bmi numeric BMI value
     * @return category string ("Underweight", "Normal", "Overweight", "Obese", or "N/A")
     */
    public static String getBmiCategory(double bmi) {
        if (bmi <= 0 || Double.isNaN(bmi) || Double.isInfinite(bmi)) {
            return STATE_NA;
        }
        if (bmi < 18.5) {
            return CATEGORY_UNDERWEIGHT;
        } else if (bmi < 25.0) {
            return CATEGORY_NORMAL;
        } else if (bmi < 30.0) {
            return CATEGORY_OVERWEIGHT;
        } else {
            return CATEGORY_OBESE;
        }
    }

    /**
     * Overload to map a BMI string (e.g. "22.4" or "N/A") to its category.
     *
     * @param bmiStr BMI as a formatted string
     * @return category string or "N/A"
     */
    public static String getBmiCategory(String bmiStr) {
        if (bmiStr == null || bmiStr.trim().isEmpty() || STATE_NA.equalsIgnoreCase(bmiStr.trim())) {
            return STATE_NA;
        }
        try {
            double bmi = Double.parseDouble(bmiStr.trim());
            return getBmiCategory(bmi);
        } catch (NumberFormatException e) {
            return STATE_NA;
        }
    }

    /**
     * Returns the color resource ID for the given BMI category badge.
     * Underweight: @color/hb_bmi_underweight (#1E88E5)
     * Normal:      @color/hb_bmi_normal      (#43A047)
     * Overweight:  @color/hb_bmi_overweight  (#FB8C00)
     * Obese:       @color/hb_bmi_obese       (#E53935)
     * N/A:         @color/hb_bmi_na          (#9E9E9E)
     *
     * @param category category name
     * @return color resource id
     */
    @ColorRes
    public static int getBmiColor(String category) {
        return getBmiColorRes(category);
    }

    @ColorRes
    public static int getBmiColorRes(String category) {
        if (category == null) {
            return R.color.hb_bmi_na;
        }
        switch (category.trim()) {
            case CATEGORY_UNDERWEIGHT:
                return R.color.hb_bmi_underweight;
            case CATEGORY_NORMAL:
                return R.color.hb_bmi_normal;
            case CATEGORY_OVERWEIGHT:
                return R.color.hb_bmi_overweight;
            case CATEGORY_OBESE:
                return R.color.hb_bmi_obese;
            case STATE_NA:
            default:
                return R.color.hb_bmi_na;
        }
    }

    /**
     * Formats the full BMI badge label from raw measurements.
     * Example: "22.4 · Normal", or "N/A" for the N/A state.
     *
     * @param weightKg patient weight in kilograms
     * @param heightCm patient height in centimeters
     * @return formatted label string
     */
    public static String formatBmiLabel(double weightKg, double heightCm) {
        String bmi = calculateBmi(weightKg, heightCm);
        if (STATE_NA.equals(bmi)) {
            return STATE_NA;
        }
        String category = getBmiCategory(bmi);
        if (STATE_NA.equals(category)) {
            return STATE_NA;
        }
        return bmi + " \u00B7 " + category;
    }

    /**
     * Formats the full BMI badge label from pre-computed BMI and category strings.
     * Example: "22.4 · Normal", or "N/A" for the N/A state.
     *
     * @param bmi formatted BMI string
     * @param category category string
     * @return formatted label string
     */
    public static String formatBmiLabel(String bmi, String category) {
        if (bmi == null || STATE_NA.equalsIgnoreCase(bmi.trim())
                || category == null || STATE_NA.equalsIgnoreCase(category.trim())) {
            return STATE_NA;
        }
        return bmi.trim() + " \u00B7 " + category.trim();
    }

    /**
     * Formats the full BMI badge label from numeric BMI value.
     * Example: "22.4 · Normal", or "N/A" for the N/A state.
     *
     * @param bmi numeric BMI value
     * @return formatted label string
     */
    public static String formatBmiLabel(double bmi) {
        if (bmi <= 0 || Double.isNaN(bmi) || Double.isInfinite(bmi)) {
            return STATE_NA;
        }
        String bmiStr = String.format(Locale.US, "%.1f", bmi);
        String category = getBmiCategory(bmi);
        if (STATE_NA.equals(category)) {
            return STATE_NA;
        }
        return bmiStr + " \u00B7 " + category;
    }
}

