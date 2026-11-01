package com.example.healthbridge.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model for the 'medical_history' table in Supabase.
 */
public class MedicalHistory {

    @SerializedName("id")
    private int id;

    @SerializedName("profile_id")
    private int profileId;

    @SerializedName("condition")
    private String condition;

    @SerializedName("notes")
    private String notes;

    @SerializedName("diagnosed_on")
    private String diagnosedOn;

    public MedicalHistory() {}

    public MedicalHistory(int profileId, String condition, String notes, String diagnosedOn) {
        this.profileId = profileId;
        this.condition = condition;
        this.notes = notes;
        this.diagnosedOn = diagnosedOn;
    }

    // Getters
    public int getId() { return id; }
    public int getProfileId() { return profileId; }
    public String getCondition() { return condition; }
    public String getNotes() { return notes; }
    public String getDiagnosedOn() { return diagnosedOn; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setProfileId(int profileId) { this.profileId = profileId; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setDiagnosedOn(String diagnosedOn) { this.diagnosedOn = diagnosedOn; }
}
