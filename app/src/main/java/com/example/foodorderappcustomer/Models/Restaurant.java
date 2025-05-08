package com.example.foodorderappcustomer.Models;

import java.io.Serializable;
import java.util.List;

public class Restaurant implements Serializable {
    private String id;
    private String name;
    private String description;
    private String address;
    private double rating;
    private int imageResource;
    private String imageUrl;
    private double deliveryFee;
    private int averageDeliveryTime;
    private List<String> cuisineTypes;
    private double distance;

    // Default constructor for Firebase
    public Restaurant() {
    }

    public Restaurant(String id, String name, String description, String address, double rating) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.rating = rating;
    }


    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }


    public double getDistance() {
        return distance;
    }

    public void setImageResource(int imageResource) {
        this.imageResource = imageResource;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public int getAverageDeliveryTime() {
        return averageDeliveryTime;
    }

    public void setAverageDeliveryTime(int averageDeliveryTime) {
        this.averageDeliveryTime = averageDeliveryTime;
    }

    public List<String> getCuisineTypes() {
        return cuisineTypes;
    }

    public void setCuisineTypes(List<String> cuisineTypes) {
        this.cuisineTypes = cuisineTypes;
    }
}