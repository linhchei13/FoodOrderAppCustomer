package com.example.foodorderappcustomer.Models;

public class OpeningHour {
    private String open;
    private String close;

    public OpeningHour() {}

    public OpeningHour(String open, String close) {
        this.open = open;
        this.close = close;
    }

    // Getters and setters
    public String getOpen() { return open; }
    public void setOpen(String open) { this.open = open; }

    public String getClose() { return close; }
    public void setClose(String close) { this.close = close; }
}
