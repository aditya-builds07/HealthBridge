package com.example.healthbridge.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import com.example.healthbridge.models.MedicalHistory;
import com.example.healthbridge.models.Medication;
import com.example.healthbridge.models.Profile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generates a 1-page Doctor Summary PDF using Android native PdfDocument.
 * Layout: Header → Patient details → Emergency box → Vitals/BMI →
 *         Current Medications (max 6) → Medical History (max 6) → Footer.
 *
 * PRD Feature F6 — owned by Aditya.
 */
public class HealthBridgePdfGenerator {

    private static final String TAG = "HealthBridgePdfGen";

    // A4 page dimensions in points (1/72 inch)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    // Design colors
    private static final int COLOR_TEAL = Color.parseColor("#00897B");
    private static final int COLOR_TEAL_DARK = Color.parseColor("#00695C");
    private static final int COLOR_CRIMSON = Color.parseColor("#D32F2F");
    private static final int COLOR_WHITE = Color.WHITE;
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#1F2D2B");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#5C6F6C");
    private static final int COLOR_LIGHT_BG = Color.parseColor("#F5F5F5");
    private static final int COLOR_TABLE_BORDER = Color.parseColor("#E0E0E0");

    // Max rows shown before "and N more"
    private static final int MAX_TABLE_ROWS = 6;

    // Footer text — always at the bottom
    private static final String FOOTER_TEXT = "Prototype with fictional demo data. Not for real medical use.";
    private static final int FOOTER_HEIGHT = 30;
    private static final int FOOTER_Y = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT;

    /**
     * Generates the doctor summary PDF.
     *
     * @param ctx     Android context (for cache directory)
     * @param p       Patient profile
     * @param meds    List of current medications
     * @param history List of medical history entries
     * @return The generated PDF File, or null on failure
     */
    public static File generate(Context ctx, Profile p, List<Medication> meds, List<MedicalHistory> history) {
        if (ctx == null || p == null) {
            Log.e(TAG, "Context or Profile is null");
            return null;
        }

        if (meds == null) meds = new ArrayList<>();
        if (history == null) history = new ArrayList<>();

        PdfDocument document = new PdfDocument();

        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            float y = MARGIN;

            // ── 1. TEAL HEADER BAND ──────────────────────────────────
            y = drawHeader(canvas, y);

            // ── 2. PATIENT DETAILS ───────────────────────────────────
            y = drawPatientDetails(canvas, y, p);

            // ── 3. EMERGENCY BOX ─────────────────────────────────────
            y = drawEmergencyBox(canvas, y, p);

            // ── 4. VITALS & BMI ──────────────────────────────────────
            y = drawVitals(canvas, y, p);

            // ── 5. CURRENT MEDICATIONS TABLE ─────────────────────────
            if (y < FOOTER_Y - 40) {
                y = drawMedicationsTable(canvas, y, meds);
            }

            // ── 6. MEDICAL HISTORY TABLE ─────────────────────────────
            if (y < FOOTER_Y - 40) {
                y = drawMedicalHistoryTable(canvas, y, history);
            }

            // ── 7. FOOTER ────────────────────────────────────────────
            drawFooter(canvas);

            document.finishPage(page);

            // Save to cache directory
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File outputFile = new File(ctx.getCacheDir(), "healthbridge_summary_" + timestamp + ".pdf");
            FileOutputStream fos = new FileOutputStream(outputFile);
            document.writeTo(fos);
            fos.close();
            document.close();

            Log.d(TAG, "PDF generated: " + outputFile.getAbsolutePath());
            return outputFile;

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate PDF", e);
            document.close();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION DRAWING METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Draws the teal header band with title, tagline, and date.
     */
    private static float drawHeader(Canvas canvas, float y) {
        float bandHeight = 70;
        Paint bandPaint = newFillPaint(COLOR_TEAL);
        canvas.drawRect(0, y, PAGE_WIDTH, y + bandHeight, bandPaint);

        Paint titlePaint = newTextPaint(COLOR_WHITE, 20, true);
        canvas.drawText("Health Bridge", MARGIN + 10, y + 28, titlePaint);

        Paint taglinePaint = newTextPaint(COLOR_WHITE, 9, false);
        canvas.drawText("Bridging Patient History with Better Doctor Decisions", MARGIN + 10, y + 44, taglinePaint);

        Paint datePaint = newTextPaint(COLOR_WHITE, 8, false);
        String dateStr = "Generated: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new Date());
        float dateWidth = datePaint.measureText(dateStr);
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - 10 - dateWidth, y + 28, datePaint);

        return y + bandHeight + 16;
    }

    /**
     * Draws patient details: name, age, gender.
     */
    private static float drawPatientDetails(Canvas canvas, float y, Profile p) {
        Paint sectionTitle = newTextPaint(COLOR_TEAL_DARK, 13, true);
        canvas.drawText("Patient Details", MARGIN, y + 13, sectionTitle);
        y += 22;

        Paint labelPaint = newTextPaint(COLOR_TEXT_SECONDARY, 10, true);
        Paint valuePaint = newTextPaint(COLOR_TEXT_PRIMARY, 10, false);

        // Row 1: Name
        y = drawLabelValue(canvas, y, "Name:", safe(p.getFullName()), labelPaint, valuePaint);
        // Row 2: Age and Gender side by side
        String ageStr = p.getAge() > 0 ? String.valueOf(p.getAge()) + " years" : "Not provided";
        canvas.drawText("Age:", MARGIN, y + 10, labelPaint);
        canvas.drawText(ageStr, MARGIN + 55, y + 10, valuePaint);

        canvas.drawText("Gender:", MARGIN + 200, y + 10, labelPaint);
        canvas.drawText(safe(p.getGender()), MARGIN + 260, y + 10, valuePaint);
        y += 18;

        return y + 6;
    }

    /**
     * Draws the emergency box with crimson title bar.
     */
    private static float drawEmergencyBox(Canvas canvas, float y, Profile p) {
        float boxStartY = y;

        // Crimson title bar
        float titleBarHeight = 22;
        Paint titleBarPaint = newFillPaint(COLOR_CRIMSON);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + titleBarHeight, titleBarPaint);

        Paint titlePaint = newTextPaint(COLOR_WHITE, 11, true);
        canvas.drawText("Emergency Information", MARGIN + 8, y + 15, titlePaint);
        y += titleBarHeight;

        // Light background for box content
        float contentHeight = 56;
        Paint bgPaint = newFillPaint(Color.parseColor("#FFF3F3"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + contentHeight, bgPaint);

        // Border around the whole box
        Paint borderPaint = newStrokePaint(Color.parseColor("#FFCDD2"), 1f);
        canvas.drawRect(MARGIN, boxStartY, PAGE_WIDTH - MARGIN, y + contentHeight, borderPaint);

        Paint labelPaint = newTextPaint(COLOR_TEXT_SECONDARY, 9, true);
        Paint valuePaint = newTextPaint(COLOR_TEXT_PRIMARY, 9, false);

        float innerY = y + 14;

        // Row 1: Blood Group and Allergies
        canvas.drawText("Blood Group:", MARGIN + 8, innerY, labelPaint);
        canvas.drawText(safe(p.getBloodGroup()), MARGIN + 80, innerY, valuePaint);

        canvas.drawText("Allergies:", MARGIN + 200, innerY, labelPaint);
        String allergies = safe(p.getAllergies());
        drawClippedText(canvas, allergies, MARGIN + 260, innerY, PAGE_WIDTH - MARGIN - 8, valuePaint);
        innerY += 18;

        // Row 2: Emergency Contact Name and Phone
        canvas.drawText("Contact:", MARGIN + 8, innerY, labelPaint);
        canvas.drawText(safe(p.getEmergencyContactName()), MARGIN + 60, innerY, valuePaint);

        canvas.drawText("Phone:", MARGIN + 260, innerY, labelPaint);
        canvas.drawText(safe(p.getEmergencyContactPhone()), MARGIN + 305, innerY, valuePaint);

        y += contentHeight + 10;
        return y;
    }

    /**
     * Draws vitals: height, weight, BMI value and category.
     * BMI is calculated privately here — does NOT call any external utility.
     */
    private static float drawVitals(Canvas canvas, float y, Profile p) {
        Paint sectionTitle = newTextPaint(COLOR_TEAL_DARK, 13, true);
        canvas.drawText("Vitals", MARGIN, y + 13, sectionTitle);
        y += 22;

        Paint labelPaint = newTextPaint(COLOR_TEXT_SECONDARY, 10, true);
        Paint valuePaint = newTextPaint(COLOR_TEXT_PRIMARY, 10, false);

        double heightCm = p.getHeightCm();
        double weightKg = p.getWeightKg();

        String heightStr = heightCm > 0 ? String.format(Locale.US, "%.1f cm", heightCm) : "Not provided";
        String weightStr = weightKg > 0 ? String.format(Locale.US, "%.1f kg", weightKg) : "Not provided";

        // Height and Weight on one line
        canvas.drawText("Height:", MARGIN, y + 10, labelPaint);
        canvas.drawText(heightStr, MARGIN + 55, y + 10, valuePaint);

        canvas.drawText("Weight:", MARGIN + 200, y + 10, labelPaint);
        canvas.drawText(weightStr, MARGIN + 260, y + 10, valuePaint);
        y += 18;

        // BMI calculation (private, no external utility)
        String bmiStr;
        String bmiCategory;
        int bmiColor;

        if (heightCm > 0 && weightKg > 0) {
            double heightM = heightCm / 100.0;
            double bmi = weightKg / (heightM * heightM);
            bmiStr = String.format(Locale.US, "%.1f", bmi);

            if (bmi < 18.5) {
                bmiCategory = "Underweight";
                bmiColor = Color.parseColor("#1E88E5");
            } else if (bmi <= 24.9) {
                bmiCategory = "Normal";
                bmiColor = Color.parseColor("#43A047");
            } else if (bmi <= 29.9) {
                bmiCategory = "Overweight";
                bmiColor = Color.parseColor("#FB8C00");
            } else {
                bmiCategory = "Obese";
                bmiColor = Color.parseColor("#E53935");
            }
        } else {
            bmiStr = "N/A";
            bmiCategory = "";
            bmiColor = Color.parseColor("#9E9E9E");
        }

        canvas.drawText("BMI:", MARGIN, y + 10, labelPaint);
        Paint bmiValuePaint = newTextPaint(bmiColor, 10, true);
        canvas.drawText(bmiStr, MARGIN + 55, y + 10, bmiValuePaint);

        if (!bmiCategory.isEmpty()) {
            Paint bmiCatPaint = newTextPaint(bmiColor, 10, false);
            canvas.drawText("(" + bmiCategory + ")", MARGIN + 55 + bmiValuePaint.measureText(bmiStr) + 6, y + 10, bmiCatPaint);
        }
        y += 24;

        return y;
    }

    /**
     * Draws the Current Medications table.
     * Shows at most MAX_TABLE_ROWS rows, then "and N more".
     * Columns: Name, Dosage, Frequency, Status.
     */
    private static float drawMedicationsTable(Canvas canvas, float y, List<Medication> meds) {
        Paint sectionTitle = newTextPaint(COLOR_TEAL_DARK, 13, true);
        canvas.drawText("Current Medications", MARGIN, y + 13, sectionTitle);
        y += 20;

        if (meds.isEmpty()) {
            Paint emptyPaint = newTextPaint(COLOR_TEXT_SECONDARY, 9, false);
            canvas.drawText("No medications recorded.", MARGIN, y + 10, emptyPaint);
            return y + 20;
        }

        // Column widths for the 4 columns
        float[] colWidths = {140, 120, 130, CONTENT_WIDTH - 390};
        String[] headers = {"Name", "Dosage", "Frequency", "Status"};

        y = drawTableHeader(canvas, y, headers, colWidths);

        int count = Math.min(meds.size(), MAX_TABLE_ROWS);
        Paint cellPaint = newTextPaint(COLOR_TEXT_PRIMARY, 8, false);

        for (int i = 0; i < count; i++) {
            if (y + 16 > FOOTER_Y - 10) break; // Stop before footer

            Medication m = meds.get(i);
            float cellY = y + 11;
            float x = MARGIN + 4;

            drawClippedText(canvas, safe(m.getName()), x, cellY, x + colWidths[0] - 8, cellPaint);
            x += colWidths[0];
            drawClippedText(canvas, safe(m.getDosage()), x, cellY, x + colWidths[1] - 8, cellPaint);
            x += colWidths[1];
            drawClippedText(canvas, safe(m.getFrequency()), x, cellY, x + colWidths[2] - 8, cellPaint);
            x += colWidths[2];
            drawClippedText(canvas, safe(m.getStatus()), x, cellY, x + colWidths[3] - 8, cellPaint);

            y += 16;

            // Light row separator
            Paint sepPaint = newStrokePaint(COLOR_TABLE_BORDER, 0.5f);
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, sepPaint);
        }

        if (meds.size() > MAX_TABLE_ROWS) {
            int remaining = meds.size() - MAX_TABLE_ROWS;
            Paint morePaint = newTextPaint(COLOR_TEXT_SECONDARY, 8, true);
            canvas.drawText("... and " + remaining + " more", MARGIN + 4, y + 12, morePaint);
            y += 16;
        }

        return y + 10;
    }

    /**
     * Draws the Medical History table.
     * Shows at most MAX_TABLE_ROWS rows, then "and N more".
     * Columns: Condition, Diagnosed On, Notes.
     */
    private static float drawMedicalHistoryTable(Canvas canvas, float y, List<MedicalHistory> history) {
        Paint sectionTitle = newTextPaint(COLOR_TEAL_DARK, 13, true);
        canvas.drawText("Medical History", MARGIN, y + 13, sectionTitle);
        y += 20;

        if (history.isEmpty()) {
            Paint emptyPaint = newTextPaint(COLOR_TEXT_SECONDARY, 9, false);
            canvas.drawText("No medical history recorded.", MARGIN, y + 10, emptyPaint);
            return y + 20;
        }

        // Column widths for the 3 columns
        float[] colWidths = {150, 100, CONTENT_WIDTH - 250};
        String[] headers = {"Condition", "Diagnosed On", "Notes"};

        y = drawTableHeader(canvas, y, headers, colWidths);

        int count = Math.min(history.size(), MAX_TABLE_ROWS);
        Paint cellPaint = newTextPaint(COLOR_TEXT_PRIMARY, 8, false);

        for (int i = 0; i < count; i++) {
            if (y + 16 > FOOTER_Y - 10) break; // Stop before footer

            MedicalHistory h = history.get(i);
            float cellY = y + 11;
            float x = MARGIN + 4;

            drawClippedText(canvas, safe(h.getCondition()), x, cellY, x + colWidths[0] - 8, cellPaint);
            x += colWidths[0];
            drawClippedText(canvas, safe(h.getDiagnosedOn()), x, cellY, x + colWidths[1] - 8, cellPaint);
            x += colWidths[1];
            drawClippedText(canvas, safe(h.getNotes()), x, cellY, x + colWidths[2] - 8, cellPaint);

            y += 16;

            // Light row separator
            Paint sepPaint = newStrokePaint(COLOR_TABLE_BORDER, 0.5f);
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, sepPaint);
        }

        if (history.size() > MAX_TABLE_ROWS) {
            int remaining = history.size() - MAX_TABLE_ROWS;
            Paint morePaint = newTextPaint(COLOR_TEXT_SECONDARY, 8, true);
            canvas.drawText("... and " + remaining + " more", MARGIN + 4, y + 12, morePaint);
            y += 16;
        }

        return y + 10;
    }

    /**
     * Draws the footer text at the fixed bottom position.
     */
    private static void drawFooter(Canvas canvas) {
        Paint linePaint = newStrokePaint(COLOR_TABLE_BORDER, 1f);
        canvas.drawLine(MARGIN, FOOTER_Y, PAGE_WIDTH - MARGIN, FOOTER_Y, linePaint);

        Paint footerPaint = newTextPaint(COLOR_TEXT_SECONDARY, 8, false);
        float textWidth = footerPaint.measureText(FOOTER_TEXT);
        float x = (PAGE_WIDTH - textWidth) / 2f;
        canvas.drawText(FOOTER_TEXT, x, FOOTER_Y + 16, footerPaint);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Draws a table header row with a light background and bold text.
     */
    private static float drawTableHeader(Canvas canvas, float y, String[] headers, float[] colWidths) {
        float headerHeight = 16;

        // Header background
        Paint hdrBgPaint = newFillPaint(COLOR_TEAL);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerHeight, hdrBgPaint);

        Paint hdrTextPaint = newTextPaint(COLOR_WHITE, 8, true);
        float x = MARGIN + 4;
        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], x, y + 11, hdrTextPaint);
            x += colWidths[i];
        }

        return y + headerHeight;
    }

    /**
     * Draws a label: value pair on one line.
     */
    private static float drawLabelValue(Canvas canvas, float y, String label, String value,
                                         Paint labelPaint, Paint valuePaint) {
        canvas.drawText(label, MARGIN, y + 10, labelPaint);
        drawClippedText(canvas, value, MARGIN + 55, y + 10, PAGE_WIDTH - MARGIN, valuePaint);
        return y + 18;
    }

    /**
     * Draws text clipped/ellipsized to fit within maxX.
     * Uses Paint.measureText to calculate whether the text fits.
     */
    private static void drawClippedText(Canvas canvas, String text, float x, float y, float maxX, Paint paint) {
        if (text == null || text.isEmpty()) {
            canvas.drawText("Not provided", x, y, paint);
            return;
        }

        float availableWidth = maxX - x;
        if (availableWidth <= 0) return;

        float textWidth = paint.measureText(text);
        if (textWidth <= availableWidth) {
            canvas.drawText(text, x, y, paint);
        } else {
            // Ellipsize: find how many characters fit with "..."
            String ellipsis = "...";
            float ellipsisWidth = paint.measureText(ellipsis);
            float targetWidth = availableWidth - ellipsisWidth;
            if (targetWidth <= 0) {
                canvas.drawText(ellipsis, x, y, paint);
                return;
            }

            int charCount = paint.breakText(text, true, targetWidth, null);
            String truncated = text.substring(0, charCount) + ellipsis;
            canvas.drawText(truncated, x, y, paint);
        }
    }

    /**
     * Returns "Not provided" for null or empty strings. Never returns "null".
     */
    private static String safe(String value) {
        return (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value))
                ? "Not provided" : value;
    }

    /**
     * Creates a new Paint for filled rectangles.
     */
    private static Paint newFillPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        return paint;
    }

    /**
     * Creates a new Paint for text rendering.
     */
    private static Paint newTextPaint(int color, float textSizeSp, boolean bold) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(textSizeSp);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(bold);
        return paint;
    }

    /**
     * Creates a new Paint for stroke/border lines.
     */
    private static Paint newStrokePaint(int color, float strokeWidth) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);
        return paint;
    }
}
