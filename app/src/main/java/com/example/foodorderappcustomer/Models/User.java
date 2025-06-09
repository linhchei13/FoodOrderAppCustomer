package com.example.foodorderappcustomer.Models;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private Map<String, Object> address;

    private long createdAt;

    // Default constructor required for Firebase
    public User() {

    }

    public User(String userId, String firstName, String lastName, String email) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = "";
        this.profileImageUrl = "";
        this.address = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public User(String userId, String firstName, String lastName, String email, String phone) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = "";
        this.address = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public User(String userId, String firstName, String lastName, String email, String phone, String profileImageUrl) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.address = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Map<String, Object> getAddress() {
        return address;
    }

    public void setAddress(Map<String, Object> address) {
        this.address = address;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Helper method to get formatted address
    @com.google.firebase.database.Exclude
    public String getFormattedAddress() {
        if (address != null && address.containsKey("formattedAddress")) {
            Object addr = address.get("formattedAddress");
            return addr != null ? addr.toString() : "";
        }
        return "";
    }

    // Helper method to set formatted address
    @com.google.firebase.database.Exclude
    public void setFormattedAddress(String formattedAddress) {
        if (address == null) {
            address = new HashMap<>();
        }
        address.put("formattedAddress", formattedAddress);
        address.put("lastUsed", System.currentTimeMillis());
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("firstName", firstName);
        result.put("lastName", lastName);
        result.put("email", email);
        result.put("phone", phone);
        result.put("profileImageUrl", profileImageUrl);
        result.put("address", address);
        result.put("createdAt", createdAt);

        return result;
    }
}