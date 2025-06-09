package com.example.foodorderappcustomer.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Restaurant implements Serializable {
    private String id;
    private String name;
    private String description;
    private String address;
    private double rating;

    private int totalRatings;
    private String imageUrl;
    private double deliveryFee;
    private List<String> cuisineTypes;
    private double distance;
    private String averagePrice;

    private String category;

    private List<MenuItem> menuItems;

    private int imageResource;


//    private Map<String, OpeningHour> openingHours;

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

    public void setImageResource(int imageResource) {
        this.imageResource = imageResource;
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

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(String averagePrice) {
        this.averagePrice = averagePrice;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getCuisineTypes() {
        cuisineTypes = new ArrayList<>();
        cuisineTypes.add(category);
        return cuisineTypes;
    }

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public void setCuisineTypes(List<String> cuisineTypes) {
        this.cuisineTypes = cuisineTypes;
    }

//    public Map<String, OpeningHour> getOpeningHours() {
//        return openingHours;
//    }
//
//    public void setOpeningHours(Map<String, OpeningHour> openingHours) {
//        this.openingHours = openingHours;
//    }
}