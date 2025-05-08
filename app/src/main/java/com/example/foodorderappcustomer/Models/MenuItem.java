package com.example.foodorderappcustomer.Models;

public class MenuItem {
    private String id;
    private String name;
    private double price;
    private String category;
    private float rating;
    private int imageResource;
    private String imageUrl;
    private String description;
    private String restaurantId;

    public MenuItem(String id, String name, double price, String category, float rating, int imageResource) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.rating = rating;
        this.imageResource = imageResource;
        this.description = ""; // Default empty description
        this.imageUrl = null;
    }

    public MenuItem(String id, String name, double price, String category, float rating, int imageResource, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.rating = rating;
        this.imageResource = imageResource;
        this.description = description;
        this.imageUrl = null;
    }
    
    public MenuItem(String id, String name, double price, String category, float rating, int imageResource, String description, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.rating = rating;
        this.imageResource = imageResource;
        this.description = description;
        this.imageUrl = imageUrl;
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

    public int getImageResource() {
        return imageResource;
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