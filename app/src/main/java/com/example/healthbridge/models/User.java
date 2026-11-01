package com.example.healthbridge.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model representing a Supabase Auth user from auth.users.
 */
public class User {

    @SerializedName("id")
    private String id; // UUID in auth.users

    @SerializedName("email")
    private String email;

    @SerializedName("created_at")
    private String createdAt;

    public User() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

