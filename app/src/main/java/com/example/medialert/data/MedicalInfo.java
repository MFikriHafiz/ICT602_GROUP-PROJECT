package com.example.medialert.data;

import java.io.Serializable;
import java.util.List;

public class MedicalInfo implements Serializable {
    private String userId; // To link this medical info to a specific user
    private String bloodType;
    private List<String> medicalConditions; // e.g., "Diabetes", "Hypertension"
    private List<String> allergies;         // e.g., "Penicillin", "Peanuts"
    private List<String> medications;       // e.g., "Insulin", "Lisinopril"
    private String emergencyNotes;          // Free-form text for critical info (e.g., "Pacemaker", "Deaf")
    private Boolean organDonor;             // Optional: true/false for organ donor status

    public MedicalInfo() {
        // Default constructor required for Firestore
    }

    public MedicalInfo(String userId, String bloodType, List<String> medicalConditions,
                       List<String> allergies, List<String> medications, String emergencyNotes,
                       Boolean organDonor) {
        this.userId = userId;
        this.bloodType = bloodType;
        this.medicalConditions = medicalConditions;
        this.allergies = allergies;
        this.medications = medications;
        this.emergencyNotes = emergencyNotes;
        this.organDonor = organDonor;
    }

    // --- Getters ---
    public String getUserId() {
        return userId;
    }

    public String getBloodType() {
        return bloodType;
    }

    public List<String> getMedicalConditions() {
        return medicalConditions;
    }

    public List<String> getAllergies() {
        return allergies;
    }

    public List<String> getMedications() {
        return medications;
    }

    public String getEmergencyNotes() {
        return emergencyNotes;
    }

    public Boolean getOrganDonor() {
        return organDonor;
    }

    // --- Setters ---
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public void setMedicalConditions(List<String> medicalConditions) {
        this.medicalConditions = medicalConditions;
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies;
    }

    public void setMedications(List<String> medications) {
        this.medications = medications;
    }

    public void setEmergencyNotes(String emergencyNotes) {
        this.emergencyNotes = emergencyNotes;
    }

    public void setOrganDonor(Boolean organDonor) {
        this.organDonor = organDonor;
    }
}