package com.example.foodorderappcustomer.Models;

public class SavedAddress {
    private String id;
    private String label;
    private String address;
    private String placeId;
    private long lastUsed;

    public SavedAddress() {
        // Required empty constructor for Firebase
    }

    public SavedAddress(String id, String label, String address, String placeId) {
        this.id = id;
        this.label = label;
        this.address = address;
        this.placeId = placeId;
        this.lastUsed = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
