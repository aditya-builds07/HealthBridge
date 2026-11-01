package com.example.healthbridge.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for Supabase Auth endpoints (/auth/v1/token, /auth/v1/signup).
 */
public class AuthRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

