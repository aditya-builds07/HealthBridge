package com.example.healthbridge.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model for the 'medical_records' table in Supabase.
 */
public class MedicalRecord {

    @SerializedName("id")
    private int id;

    @SerializedName("profile_id")
    private int profileId;

    @SerializedName("title")
    private String title;

    @SerializedName("category")
    private String category;

    @SerializedName("file_url")
    private String fileUrl;

    @SerializedName("mime_type")
    private String mimeType;

    @SerializedName("created_at")
    private String createdAt;

    public MedicalRecord() {}

    public MedicalRecord(int profileId, String title, String category, String fileUrl, String mimeType) {
        this.profileId = profileId;
        this.title = title;
        this.category = category;
        this.fileUrl = fileUrl;
        this.mimeType = mimeType;
    }

    // Getters
    public int getId() { return id; }
    public int getProfileId() { return profileId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getFileUrl() { return fileUrl; }
    public String getMimeType() { return mimeType; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setProfileId(int profileId) { this.profileId = profileId; }
    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
