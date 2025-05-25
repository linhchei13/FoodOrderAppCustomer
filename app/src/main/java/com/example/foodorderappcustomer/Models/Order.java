package com.example.foodorderappcustomer.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Order {
    private String id;
    private String userId;
    private String restaurantId;
    private String restaurantName;
    private List<FoodItem> items;
    private double subtotal;
    private double deliveryFee;
    private double total;
    private double discount;
    private String promotionId;
    private String status;
    private String address;
    private String paymentMethod;
    private Date orderTime;
    private Date deliveryTime;
    private String note;

    public Order() {
        this.items = new ArrayList<>();
        this.status = "pending"; // Mặc định là "đang chờ xử lý"
        this.orderTime = new Date();
    }

    public Order(String userId, String restaurantId, String restaurantName, List<FoodItem> items,
                double subtotal, double deliveryFee, String address, String paymentMethod) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.items = items != null ? items : new ArrayList<>();
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.total = subtotal + deliveryFee;
        this.status = "pending";
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.orderTime = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public List<FoodItem> getItems() { return items; }
    public void setItems(List<FoodItem> items) { this.items = items; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Date getOrderTime() { return orderTime; }
    public void setOrderTime(Date orderTime) { this.orderTime = orderTime; }

    public Date getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(Date deliveryTime) { this.deliveryTime = deliveryTime; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    // Add an item to the order
    public void addItem(FoodItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        recalculateTotal();
    }

    // Remove an item from the order
    public void removeItem(FoodItem item) {
        if (items != null) {
            items.remove(item);
            recalculateTotal();
        }
    }

    // Recalculate total price
    private void recalculateTotal() {
        this.subtotal = 0;
        if (items != null) {
            for (FoodItem item : items) {
                this.subtotal += item.getTotalPrice();
            }
        }
        this.total = this.subtotal + this.deliveryFee - this.discount;
    }
} 