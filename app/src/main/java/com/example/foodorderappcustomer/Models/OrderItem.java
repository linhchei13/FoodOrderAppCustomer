package com.example.foodorderappcustomer.Models;

import java.util.List;
import java.util.Map;

public class OrderItem {
    private String menuItemId;
    private String name;
    private double price;
    private int quantity;
    private List<Map<String, Object>> options;

    public OrderItem() {
        // Required empty constructor for Firebase
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<Map<String, Object>> getOptions() {
        return options;
    }

    public void setOptions(List<Map<String, Object>> options) {
        this.options = options;
    }

    // Helper method to get formatted price
    public String getFormattedPrice() {
        return String.format("%,.0f đ", price);
    }

    // Helper method to get total price
    public double getTotalPrice() {
        return price * quantity;
    }

    // Helper method to get formatted total price
    public String getFormattedTotalPrice() {
        return String.format("%,.0f đ", getTotalPrice());
    }
}
