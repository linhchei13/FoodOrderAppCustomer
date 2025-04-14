package com.example.foodorderappcustomer.Models;

public class Restaurant {
    private String id;
    private String name;
    private String address;
    private float rating;
    private int imageResource;

    public Restaurant(String id, String name, String address, float rating, int imageResource) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.imageResource = imageResource;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public float getRating() {
        return rating;
    }

    public int getImageResource() {
        return imageResource;
    }
}