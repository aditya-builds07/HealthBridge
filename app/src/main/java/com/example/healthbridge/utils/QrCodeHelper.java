package com.example.healthbridge.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.healthbridge.models.Profile;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/**
 * Helper for generating and displaying ZXing QR codes in an AlertDialog.
 * PRD Feature F7 — owned by Aditya.
 *
 * Uses ZXing core (com.google.zxing:core) QRCodeWriter to generate the QR bitmap.
 * Dialog follows design.md §7.11: rounded white dialog, QR on white square ~240dp,
 * caption underneath, teal Close button.
 */
public class QrCodeHelper {

    private static final int QR_SIZE_PX = 512;
    private static final int QR_MARGIN = 2;
    private static final int MAX_CONTENT_LENGTH = 1500;

    // Design tokens from design.md
    private static final int COLOR_TEAL = Color.parseColor("#00897B");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#1F2D2B");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#5C6F6C");

    /**
     * Generates a QR code bitmap from the given content and displays it in a
     * styled AlertDialog.
     *
     * @param ctx     Activity or application context
     * @param content The text/URL to encode in the QR code
     * @param title   Dialog title text
     */
    public static void showQrDialog(Context ctx, String content, String title) {
        if (ctx == null) return;

        // Guard: empty or null content
        if (content == null || content.trim().isEmpty()) {
            Toast.makeText(ctx, "Nothing to encode in QR code.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guard: content too long for a reliable QR scan
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH);
        }

        // Generate QR bitmap
        Bitmap qrBitmap = generateQrBitmap(content);
        if (qrBitmap == null) {
            Toast.makeText(ctx, "Failed to generate QR code.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Build the dialog view programmatically ───────────────
        int dp8 = dpToPx(ctx, 8);
        int dp16 = dpToPx(ctx, 16);
        int dp24 = dpToPx(ctx, 24);
        int dp240 = dpToPx(ctx, 240);

        // Root layout
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp24, dp24, dp24, dp16);

        // Rounded white background for the root
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE);
        rootBg.setCornerRadius(dpToPx(ctx, 20));
        root.setBackground(rootBg);

        // Title
        if (title != null && !title.trim().isEmpty()) {
            TextView titleView = new TextView(ctx);
            titleView.setText(title);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            titleView.setTextColor(COLOR_TEXT_PRIMARY);
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(0, 0, 0, dp16);
            // Bold
            titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
            root.addView(titleView);
        }

        // White square container for the QR (ensures scanners read it easily)
        LinearLayout qrContainer = new LinearLayout(ctx);
        qrContainer.setGravity(Gravity.CENTER);
        GradientDrawable qrBg = new GradientDrawable();
        qrBg.setColor(Color.WHITE);
        qrBg.setStroke(dpToPx(ctx, 1), Color.parseColor("#E0E0E0"));
        qrBg.setCornerRadius(dpToPx(ctx, 12));
        qrContainer.setBackground(qrBg);
        int qrPad = dp16;
        qrContainer.setPadding(qrPad, qrPad, qrPad, qrPad);

        // QR ImageView
        ImageView qrImage = new ImageView(ctx);
        qrImage.setImageBitmap(qrBitmap);
        qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dp240, dp240);
        qrImage.setLayoutParams(imgParams);
        qrImage.setContentDescription("QR Code");
        qrContainer.addView(qrImage);

        root.addView(qrContainer);

        // Caption below QR
        TextView caption = new TextView(ctx);
        caption.setText("Scan to view patient information");
        caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        caption.setTextColor(COLOR_TEXT_SECONDARY);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, dp8, 0, dp8);
        root.addView(caption);

        // Build and show AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(root)
                .setCancelable(true)
                .create();

        // Teal "Close" button
        TextView closeBtn = new TextView(ctx);
        closeBtn.setText("Close");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setTypeface(closeBtn.getTypeface(), android.graphics.Typeface.BOLD);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(COLOR_TEAL);
        btnBg.setCornerRadius(dpToPx(ctx, 16));
        closeBtn.setBackground(btnBg);

        int btnPadV = dpToPx(ctx, 12);
        int btnPadH = dpToPx(ctx, 32);
        closeBtn.setPadding(btnPadH, btnPadV, btnPadH, btnPadV);
        closeBtn.setMinWidth(dpToPx(ctx, 120));
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = dp8;
        closeBtn.setLayoutParams(btnParams);
        root.addView(closeBtn);

        // Transparent dialog window background so our rounded root shows properly
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    /**
     * Builds a plain-text emergency card string from the patient's profile.
     * Suitable for encoding in an offline QR code.
     *
     * @param p Patient profile
     * @return Formatted emergency text, never null
     */
    public static String buildEmergencyText(Profile p) {
        if (p == null) {
            return "HEALTH BRIDGE EMERGENCY CARD\n"
                    + "Name: Not provided\n"
                    + "Blood Group: Not provided\n"
                    + "Allergies: Not provided\n"
                    + "Emergency Contact: Not provided";
        }

        return "HEALTH BRIDGE EMERGENCY CARD\n"
                + "Name: " + safe(p.getFullName()) + "\n"
                + "Blood Group: " + safe(p.getBloodGroup()) + "\n"
                + "Allergies: " + safe(p.getAllergies()) + "\n"
                + "Emergency Contact: " + safe(p.getEmergencyContactName())
                + ", " + safe(p.getEmergencyContactPhone());
    }

    // ═══════════════════════════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generates a 512x512 QR code bitmap using ZXing QRCodeWriter.
     * Black modules on white background, error correction level M, margin 2.
     *
     * @param content Text to encode
     * @return Bitmap, or null on failure
     */
    private static Bitmap generateQrBitmap(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, QR_MARGIN);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX, hints);

            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;

        } catch (WriterException e) {
            android.util.Log.e("QrCodeHelper", "QR generation failed", e);
            return null;
        }
    }

    /**
     * Returns "Not provided" for null or empty strings.
     */
    private static String safe(String value) {
        return (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value))
                ? "Not provided" : value;
    }

    /**
     * Converts dp to pixels.
     */
    private static int dpToPx(Context ctx, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics()));
    }
}
