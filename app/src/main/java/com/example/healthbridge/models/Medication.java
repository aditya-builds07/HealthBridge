package com.example.healthbridge.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model for the 'medications' table in Supabase.
 */
public class Medication {

    @SerializedName("id")
    private int id;

    @SerializedName("profile_id")
    private int profileId;

    @SerializedName("name")
    private String name;

    @SerializedName("dosage")
    private String dosage;

    @SerializedName("frequency")
    private String frequency;

    public Medication() {}

    public Medication(int profileId, String name, String dosage, String frequency) {
        this.profileId = profileId;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
    }

    // Getters
    public int getId() { return id; }
    public int getProfileId() { return profileId; }
    public String getName() { return name; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setProfileId(int profileId) { this.profileId = profileId; }
    public void setName(String name) { this.name = name; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
}
