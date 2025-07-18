package com.example.medialert.data;

import java.io.Serializable;

public class EmergencyContact implements Serializable {
    private String id; // Unique ID for each contact
    private String name;
    private String relationship;
    private String phoneNumber;
    private String imageUrl; // Store image URI as a String
    private boolean isPrimary;

    public EmergencyContact() {
        // Default constructor required for Firebase or other deserialization
    }

    public EmergencyContact(String id, String name, String relationship, String phoneNumber, String imageUrl, boolean isPrimary) {
        this.id = id;
        this.name = name;
        this.relationship = relationship;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRelationship() {
        return relationship;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    // Setters (for Firebase deserialization and internal modification)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}