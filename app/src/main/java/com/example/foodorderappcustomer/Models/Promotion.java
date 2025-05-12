package com.example.foodorderappcustomer.Models;

import java.io.Serializable;
import java.util.Date;

public class Promotion implements Serializable {
    private String id;
    private String code;
    private String description;
    private String discountType; // "percentage" or "fixed"
    private double discountValue;
    private Date startDate;
    private Date endDate;
    private double minOrderAmount;
    private double maxDiscount;
    private String restaurantId; // null means applicable to all restaurants
    private boolean isActive;
    private int usageLimit;
    private int usageCount;
    
    // Constructor
    public Promotion() {
    }
    
    public Promotion(String id, String code, String description, String discountType, double discountValue) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.isActive = true;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDiscountType() {
        return discountType;
    }
    
    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }
    
    public double getDiscountValue() {
        return discountValue;
    }
    
    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }
    
    public Date getStartDate() {
        return startDate;
    }
    
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
    
    public Date getEndDate() {
        return endDate;
    }
    
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    
    public double getMinOrderAmount() {
        return minOrderAmount;
    }
    
    public void setMinOrderAmount(double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }
    
    public double getMaxDiscount() {
        return maxDiscount;
    }
    
    public void setMaxDiscount(double maxDiscount) {
        this.maxDiscount = maxDiscount;
    }
    
    public String getRestaurantId() {
        return restaurantId;
    }
    
    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public int getUsageLimit() {
        return usageLimit;
    }
    
    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    // Format the discount value for display
    public String getFormattedDiscount() {
        if (discountType.equals("percentage")) {
            return (int) discountValue + "%";
        } else {
            return String.format("%.0f₫", discountValue);
        }
    }
    
    // Check if promotion is still valid
    public boolean isValid() {
        Date now = new Date();
        
        // Check if active
        if (!isActive) {
            return false;
        }
        
        // Check dates
        if (startDate != null && now.before(startDate)) {
            return false;
        }
        
        if (endDate != null && now.after(endDate)) {
            return false;
        }
        
        // Check usage limit
        if (usageLimit > 0 && usageCount >= usageLimit) {
            return false;
        }
        
        return true;
    }
}