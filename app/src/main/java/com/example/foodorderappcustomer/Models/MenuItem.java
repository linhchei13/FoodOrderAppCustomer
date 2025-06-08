package com.example.foodorderappcustomer.Models;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    private String id;
    private String name;
    private double price;

    private int  preparationTime;
    private String category;
    private float rating;
    private String imageUrl;
    private String description;
    private String restaurantId;
    private int sales;
    private int likes;
    private List<OptionGroup> availableOptions;

    public MenuItem() {

    }

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
        this.likes = 0;
        this.availableOptions = new ArrayList<>();
    }
    
    public MenuItem(String id, String name, double price,
                   String category, float rating, String description, String imageUrl, int sales, int likes) {
        this(id, name, price, category, rating, description, imageUrl, sales);
        this.likes = likes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    }    public void setDescription(String description) {
        this.description = description;
    }

    public int getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime (int preparationTime) {
        this.preparationTime = preparationTime;
    }
    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getStats() {
        return sales + "k đã bán - " + likes + " lượt thích";
    }

    public List<OptionGroup> getAvailableOptions() {
        return availableOptions;
    }

    public void setAvailableOptions(List<OptionGroup> availableOptions) {
        this.availableOptions = availableOptions;
    }


}