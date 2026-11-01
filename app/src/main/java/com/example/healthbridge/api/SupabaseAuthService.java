package com.example.healthbridge.api;

import com.example.healthbridge.models.AuthRequest;
import com.example.healthbridge.models.AuthResponse;
import com.example.healthbridge.models.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit interface for Supabase GoTrue Auth API endpoints (/auth/v1/).
 */
public interface SupabaseAuthService {

    /**
     * Sign in with email and password.
     * POST /auth/v1/token?grant_type=password
     */
    @POST("token?grant_type=password")
    Call<AuthResponse> login(@Body AuthRequest request);

    /**
     * Sign up a new user with email and password.
     * POST /auth/v1/signup
     */
    @POST("signup")
    Call<AuthResponse> signUp(@Body AuthRequest request);

    /**
     * Refresh an existing access token using a refresh token.
     * POST /auth/v1/token?grant_type=refresh_token
     */
    @POST("token?grant_type=refresh_token")
    Call<AuthResponse> refreshToken(@Body Map<String, String> body);

    /**
     * Invalidate session / logout.
     * POST /auth/v1/logout
     */
    @POST("logout")
    Call<Void> logout(@Header("Authorization") String bearerToken);

    /**
     * Get the authenticated user's details.
     * GET /auth/v1/user
     */
    @GET("user")
    Call<User> getCurrentUser(@Header("Authorization") String bearerToken);
}

