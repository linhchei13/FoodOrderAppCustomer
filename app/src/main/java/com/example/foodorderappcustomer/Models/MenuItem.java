package com.example.foodorderappcustomer.Models;

public class MenuItem {
    private String id;
    private String name;
    private double price;
    private String category;
    private float rating;
    private String imageUrl;
    private String description;
    private String restaurantId;

    private int sales;
    
    public MenuItem(String id, String name, double price,
                    String category, float rating, String description, String imageUrl, int sales) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.rating = rating;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sales = sales;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public float getRating() {
        return rating;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}