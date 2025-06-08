package com.example.foodorderappcustomer.Models;

import java.util.List;

public class CartItem {
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private String address;
    private String category;
    private boolean isOpen;
    private String openingHours;
    private double distance;
    private List<OrderItem> items;
    private double totalPrice;
    private int totalQuantity;

    public CartItem() {
    }

    public CartItem(String restaurantId, String restaurantName, String restaurantImage, List<OrderItem> items) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantImage = restaurantImage;
        this.items = items;
        this.isOpen = true; // Default to open
        this.distance = 0.0; // Default distance
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
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

    // Helper method to get formatted distance
    public String getFormattedDistance() {
        if (distance < 1.0) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.1f km", distance);
        }
    }

    // Helper method to get status text
    public String getStatusText() {
        if (isOpen) {
            return "Đóng cửa • Mở cửa vào " + (openingHours != null ? openingHours : "08:00");
        } else {
            return "Đóng cửa • Mở cửa vào " + (openingHours != null ? openingHours : "08:00");
        }
    }
}