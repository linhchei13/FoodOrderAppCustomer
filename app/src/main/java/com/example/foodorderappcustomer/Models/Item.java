package com.example.foodorderappcustomer.Models;

public class Item {
    private String id;
    private String name;

    private String restaurantId;

    private double price;

    private String imageUrl;

    public Item() {
        // Default constructor required for Firebase
    }
    public Item(String id, String name, String restaurantId, double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.restaurantId = restaurantId;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String formatPrice(double p) {
        return String.format("%.2f", p);
    }
}
