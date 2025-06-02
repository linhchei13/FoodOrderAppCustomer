package com.example.foodorderappcustomer.Models;

import java.util.List;

public class CartItem {
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private List<OrderItem> items;
    private double totalPrice;

    private int quantity;


    public CartItem(String restaurantId, String restaurantName, String restaurantImage, List<OrderItem> items) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantImage = restaurantImage;
        this.items = items;
        this.quantity = 0;
        this.totalPrice = 0;
        calculateTotalPrice();
    }

    private void calculateTotalPrice() {
        this.totalPrice = 0;
        if (items != null) {
            for (OrderItem item : items) {
                this.totalPrice += item.getItemPrice() * item.getQuantity();
            }
        }
    }

    public int getQuantity() {
        if (items != null) {
            for (OrderItem item : items) {
                this.quantity += item.getQuantity();
            }
        }
        return quantity;
    }

    // Getters and Setters
    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getRestaurantImage() {
        return restaurantImage;
    }

    public void setRestaurantImage(String restaurantImage) {
        this.restaurantImage = restaurantImage;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        calculateTotalPrice();
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}
