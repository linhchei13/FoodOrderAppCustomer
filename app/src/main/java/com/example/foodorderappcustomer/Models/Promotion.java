package com.example.foodorderappcustomer.Models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Promotion {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    
    private String id;
    private String code;
    private String description;
    private String discountType; // "percentage" or "fixed"
    private double discountValue;
    
    @PropertyName("startDate")
    private String startDateStr;
    
    @PropertyName("endDate")
    private String endDateStr;
    
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
    
    // Getters and setters for Date fields
    @Exclude
    public Date getStartDate() {
        try {
            return startDateStr != null ? dateFormat.parse(startDateStr) : null;
        } catch (ParseException e) {
            return null;
        }
    }
    
    @Exclude
    public void setStartDate(Date startDate) {
        this.startDateStr = startDate != null ? dateFormat.format(startDate) : null;
    }
    
    @Exclude
    public Date getEndDate() {
        try {
            return endDateStr != null ? dateFormat.parse(endDateStr) : null;
        } catch (ParseException e) {
            return null;
        }
    }
    
    @Exclude
    public void setEndDate(Date endDate) {
        this.endDateStr = endDate != null ? dateFormat.format(endDate) : null;
    }
    
    // Firebase getters and setters for date strings
    public String getStartDateStr() {
        return startDateStr;
    }
    
    public void setStartDateStr(String startDateStr) {
        this.startDateStr = startDateStr;
    }
    
    public String getEndDateStr() {
        return endDateStr;
    }
    
    public void setEndDateStr(String endDateStr) {
        this.endDateStr = endDateStr;
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
        
        // Check dates using string comparison
        try {
            if (startDateStr != null) {
                Date startDate = dateFormat.parse(startDateStr);
                if (now.before(startDate)) {
                    return false;
                }
            }
            
            if (endDateStr != null) {
                Date endDate = dateFormat.parse(endDateStr);
                if (now.after(endDate)) {
                    return false;
                }
            }
        } catch (ParseException e) {
            return false;
        }
        
        // Check usage limit
        if (usageLimit > 0 && usageCount >= usageLimit) {
            return false;
        }
        
        return true;
    }

    // Helper method to convert to Map for Firebase
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("code", code);
        map.put("description", description);
        map.put("discountType", discountType);
        map.put("discountValue", discountValue);
        map.put("startDateStr", startDateStr);
        map.put("endDateStr", endDateStr);
        map.put("minOrderAmount", minOrderAmount);
        map.put("maxDiscount", maxDiscount);
        map.put("restaurantId", restaurantId);
        map.put("isActive", isActive);
        map.put("usageLimit", usageLimit);
        map.put("usageCount", usageCount);
        return map;
    }

    // Helper method to format date for display
    @Exclude
    public String getFormattedStartDate() {
        try {
            Date date = dateFormat.parse(startDateStr);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return startDateStr;
        }
    }

    @Exclude
    public String getFormattedEndDate() {
        try {
            Date date = dateFormat.parse(endDateStr);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return endDateStr;
        }
    }
}