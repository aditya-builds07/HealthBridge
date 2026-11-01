package com.example.healthbridge.api;

import com.example.healthbridge.models.MedicalHistory;
import com.example.healthbridge.models.MedicalRecord;
import com.example.healthbridge.models.Medication;
import com.example.healthbridge.models.Profile;

import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Retrofit interface for Supabase PostgREST API endpoints.
 * Each table is accessed via /rest/v1/<table_name>.
 */
public interface SupabaseApiService {

    // ── Profiles ──────────────────────────────────────────────
    @GET("profiles")
    Call<List<Profile>> getProfiles(@Query("select") String select);

    @GET("profiles")
    Call<List<Profile>> getProfileById(
            @Query("select") String select,
            @Query("id") String idFilter // e.g. "eq.1"
    );

    @GET("profiles")
    Call<List<Profile>> getProfileByUserId(
            @Query("select") String select,
            @Query("user_id") String userIdFilter // e.g. "eq.105e12f7-..."
    );

    // ── Medical Records ───────────────────────────────────────
    @GET("medical_records")
    Call<List<MedicalRecord>> getMedicalRecords(
            @Query("select") String select,
            @Query("profile_id") String profileIdFilter // e.g. "eq.1"
    );

    @POST("medical_records")
    Call<List<MedicalRecord>> insertMedicalRecord(@Body MedicalRecord record);

    // ── Medications ───────────────────────────────────────────
    @GET("medications")
    Call<List<Medication>> getMedications(
            @Query("select") String select,
            @Query("profile_id") String profileIdFilter
    );

    @POST("medications")
    Call<List<Medication>> insertMedication(@Body Medication medication);

    // ── Medical History ───────────────────────────────────────
    @GET("medical_history")
    Call<List<MedicalHistory>> getMedicalHistory(
            @Query("select") String select,
            @Query("profile_id") String profileIdFilter
    );

    @POST("medical_history")
    Call<List<MedicalHistory>> insertMedicalHistory(@Body MedicalHistory history);

    // ── Storage upload (dynamic URL) ──────────────────────────
    @PUT
    Call<Void> uploadFile(
            @Url String url,
            @Header("Content-Type") String contentType,
            @Body RequestBody fileBody
    );
}
