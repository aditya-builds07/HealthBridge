package com.example.healthbridge.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model for the 'profiles' table in Supabase.
 */
public class Profile {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("age")
    private int age;

    @SerializedName("gender")
    private String gender;

    @SerializedName("blood_group")
    private String bloodGroup;

    @SerializedName("height_cm")
    private double heightCm;

    @SerializedName("weight_kg")
    private double weightKg;

    @SerializedName("allergies")
    private String allergies;

    @SerializedName("emergency_contact_name")
    private String emergencyContactName;

    @SerializedName("emergency_contact_phone")
    private String emergencyContactPhone;

    public Profile() {}

    // Getters
    public int getId() { return id; }
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getBloodGroup() { return bloodGroup; }
    public double getHeightCm() { return heightCm; }
    public double getWeightKg() { return weightKg; }
    public String getAllergies() { return allergies; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    /**
     * Returns the display string for a field, showing "Not provided" if null or empty.
     */
    public static String safeDisplay(String value) {
        return (value == null || value.trim().isEmpty()) ? "Not provided" : value;
    }
}
