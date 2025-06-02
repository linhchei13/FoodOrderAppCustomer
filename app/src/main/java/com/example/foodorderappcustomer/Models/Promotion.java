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
    private String id; // sẽ gán thủ công từ push key
    private String promoCode;
    private String discountType;
    private double discountAmount;
    private String startDate;
    private String endDate;
    private int totalUsage;
    private int usagePerUser;
    private String minimumOrder;
    private String restaurantId;
    private double maxDiscountAmount;
    private boolean expired = false;

    public Promotion() {
        // Firebase cần constructor rỗng
    }

    public Promotion(String promoCode, String discountType, double discountAmount, String startDate,
                     String endDate,String minimumOrder, double maxDiscountAmount) {
        this.promoCode = promoCode;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalUsage = totalUsage;
        this.usagePerUser = usagePerUser;
        this.minimumOrder = minimumOrder;
        this.restaurantId = restaurantId;
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public String getDescription() {
        if (discountType.equals("percentage")) {
            return "Giảm " + (int)discountAmount + "% cho đơn hàng từ " +
                    minimumOrder.substring(0, minimumOrder.length() - 3) + "K";
        }
        else {
            return "Giảm " + (int)(discountAmount/1000) + "K cho đơn hàng từ " +
                    minimumOrder.substring(0, minimumOrder.length() - 3) + "K";
        }
    }
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getTotalUsage() { return totalUsage; }
    public void setTotalUsage(int totalUsage) { this.totalUsage = totalUsage; }

    public int getUsagePerUser() { return usagePerUser; }
    public void setUsagePerUser(int usagePerUser) { this.usagePerUser = usagePerUser; }

    public String getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(String minimumOrder) { this.minimumOrder = minimumOrder; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }


}