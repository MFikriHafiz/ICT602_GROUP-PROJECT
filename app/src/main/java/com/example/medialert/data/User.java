package com.example.medialert.data; // Adjust package name as needed

// This class represents a user document in Firestore
public class User {
    private String uid; // Firebase User ID
    private String name;
    private String phone;
    private String email;
    private String profileImageUriString; // URI of the profile image (e.g., from Firebase Storage or local)

    // Public no-argument constructor needed for Firestore automatic data mapping
    public User() {
    }

    public User(String uid, String name, String phone, String email, String profileImageUriString) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.profileImageUriString = profileImageUriString;
    }

    // Getters and Setters for all fields
    // Firestore uses getters to serialize data and setters to deserialize data

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUriString() {
        return profileImageUriString;
    }

    public void setProfileImageUriString(String profileImageUriString) {
        this.profileImageUriString = profileImageUriString;
    }
}