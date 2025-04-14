package com.example.foodorderappcustomer.Models;

public class MenuItem {
    private String id;
    private String name;
    private double price;
    private String category;
    private float rating;
    private int imageResource;

    public MenuItem(String id, String name, double price, String category, float rating, int imageResource) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.rating = rating;
        this.imageResource = imageResource;
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

    public int getImageResource() {
        return imageResource;
    }
}