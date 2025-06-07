package com.example.foodorderappcustomer.Models;

import java.util.List;

public class CartItem {
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private List<OrderItem> items;
    private double totalPrice;
    private int totalQuantity;

    public CartItem(String restaurantId, String restaurantName, String restaurantImage, List<OrderItem> items) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantImage = restaurantImage;
        this.items = items;
        calculateTotals();
    }

    public void calculateTotals() {
        this.totalPrice = 0;
        this.totalQuantity = 0;
        
        if (items != null) {
            for (OrderItem item : items) {
                this.totalPrice += item.getTotalPrice();
                this.totalQuantity += item.getQuantity();
            }
        }
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
        calculateTotals();
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void addItem(OrderItem item) {
        if (items != null) {
            items.add(item);
            calculateTotals();
        }
    }

    public void removeItem(OrderItem item) {
        if (items != null) {
            items.remove(item);
            calculateTotals();
        }
    }

    public void updateItemQuantity(OrderItem item, int newQuantity) {
        if (items != null) {
            for (OrderItem cartItem : items) {
                if (cartItem.getItemId().equals(item.getItemId())) {
                    cartItem.setQuantity(newQuantity);
                    break;
                }
            }
            calculateTotals();
        }
    }
}
