package com.example.foodorderappcustomer.Models;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartItem {
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private List<OrderItem> items;
    private String address;
    private Map<String, OpeningHour> openingHours;
    private double distance;

    public CartItem(String restaurantId, String restaurantName, String restaurantImage, List<OrderItem> items) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantImage = restaurantImage;
        this.items = items;
    }

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
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Map<String, OpeningHour> getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(Map<String, OpeningHour> openingHours) {
        this.openingHours = openingHours;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * Kiểm tra nhà hàng có đang mở cửa không dựa trên thời gian hiện tại
     */
    public double getTotalPrice() {
        double total = 0;
        for (OrderItem item : items) {
            total += item.getItemPrice() * item.getQuantity();
        }
        return total;
    }

    public int getTotalQuantity() {
        int total = 0;
        for (OrderItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    public String getFormattedDistance() {
        return String.format(Locale.getDefault(), "%.2f km", distance);
    }

}
