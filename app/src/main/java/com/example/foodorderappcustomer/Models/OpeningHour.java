package com.example.foodorderappcustomer.Models;

public class OpeningHour {
    private String day;
    private String openTime;
    private String closeTime;

    public OpeningHour() {
        // Default constructor for Firebase
    }

    public OpeningHour(String day, String openTime, String closeTime) {
        this.day = day;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    // Getters and Setters
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

}
