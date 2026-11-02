package com.example.healthbridge.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.healthbridge.BuildConfig;
import com.example.healthbridge.api.SupabaseClient;
import com.example.healthbridge.models.MedicalRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Uploads a generated PDF to Supabase Storage (healthbridge-files bucket)
 * and inserts a medical_records row with category "Summary".
 *
 * PRD Feature F6 — owned by Aditya.
 */
public class SummaryPdfUploader {

    private static final String TAG = "SummaryPdfUploader";

    /**
     * Callback for upload result. Both methods are invoked on the main thread.
     */
    public interface Callback {
        /**
         * Called when the PDF is uploaded and the medical_records row is inserted.
         *
         * @param publicUrl The public URL of the uploaded PDF in Supabase Storage
         */
        void onSuccess(String publicUrl);

        /**
         * Called when the upload or record insertion fails.
         *
         * @param message Human-readable error description
         */
        void onError(String message);
    }

    /**
     * Uploads the given PDF file to Supabase Storage and inserts a medical_records row.
     * Runs the network calls on a background thread; callbacks fire on the main thread.
     *
     * @param pdfFile   The PDF file to upload (from HealthBridgePdfGenerator)
     * @param profileId The profile ID to associate the record with
     * @param title     Title for the medical record (e.g. "Doctor Summary - 01 Nov 2026")
     * @param callback  Result callback
     */
    public static void upload(final File pdfFile, final int profileId,
                              final String title, final Callback callback) {
        if (pdfFile == null || !pdfFile.exists()) {
            postError(callback, "PDF file is null or does not exist.");
            return;
        }
        if (profileId <= 0) {
            postError(callback, "Invalid profile ID.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // ── Step 1: Read the PDF file into a byte array ──────
                    byte[] fileBytes = readFileBytes(pdfFile);
                    if (fileBytes == null || fileBytes.length == 0) {
                        postError(callback, "Failed to read PDF file.");
                        return;
                    }

                    // ── Step 2: Build the storage upload URL ─────────────
                    // Path: healthbridge-files/summaries/<filename>
                    String storagePath = "summaries/" + pdfFile.getName();
                    String uploadUrl = SupabaseClient.STORAGE_URL
                            + SupabaseClient.BUCKET_NAME + "/" + storagePath;

                    // ── Step 3: Upload to Supabase Storage ───────────────
                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/pdf"), fileBytes);

                    Call<Void> uploadCall = SupabaseClient.getApiService()
                            .uploadFile(uploadUrl, "application/pdf", body);

                    Response<Void> uploadResponse = uploadCall.execute();

                    if (!uploadResponse.isSuccessful()) {
                        String errorBody = "";
                        if (uploadResponse.errorBody() != null) {
                            errorBody = uploadResponse.errorBody().string();
                        }
                        postError(callback, "Storage upload failed ("
                                + uploadResponse.code() + "): " + errorBody);
                        return;
                    }

                    Log.d(TAG, "PDF uploaded to storage: " + storagePath);

                    // ── Step 4: Build the public URL ─────────────────────
                    String publicUrl = SupabaseClient.STORAGE_PUBLIC_URL
                            + SupabaseClient.BUCKET_NAME + "/" + storagePath;

                    // ── Step 5: Insert medical_records row ───────────────
                    MedicalRecord record = new MedicalRecord(
                            profileId,
                            title,
                            "Summary",       // category
                            publicUrl,       // file_url
                            "application/pdf" // mime_type
                    );

                    Call<List<MedicalRecord>> insertCall =
                            SupabaseClient.getApiService().insertMedicalRecord(record);

                    Response<List<MedicalRecord>> insertResponse = insertCall.execute();

                    if (!insertResponse.isSuccessful()) {
                        String errorBody = "";
                        if (insertResponse.errorBody() != null) {
                            errorBody = insertResponse.errorBody().string();
                        }
                        // Upload succeeded but record insert failed — log it
                        Log.w(TAG, "Record insert failed but file is at: " + publicUrl);
                        postError(callback, "Record insert failed ("
                                + insertResponse.code() + "): " + errorBody);
                        return;
                    }

                    Log.d(TAG, "Medical record inserted. Public URL: " + publicUrl);

                    // ── Step 6: Success callback on main thread ──────────
                    postSuccess(callback, publicUrl);

                } catch (Exception e) {
                    Log.e(TAG, "Upload failed", e);
                    postError(callback, "Upload exception: " + e.getMessage());
                }
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  INTERNAL HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reads all bytes from a file.
     */
    private static byte[] readFileBytes(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            int bytesRead = fis.read(bytes);
            fis.close();
            if (bytesRead != bytes.length) {
                Log.w(TAG, "Partial read: " + bytesRead + " of " + bytes.length);
            }
            return bytes;
        } catch (IOException e) {
            Log.e(TAG, "Error reading file", e);
            return null;
        }
    }

    /**
     * Posts a success callback on the main thread.
     */
    private static void postSuccess(final Callback callback, final String publicUrl) {
        if (callback == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(publicUrl);
            }
        });
    }

    /**
     * Posts an error callback on the main thread.
     */
    private static void postError(final Callback callback, final String message) {
        if (callback == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }
}

