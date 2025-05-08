package com.example.foodorderappcustomer.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CartItem implements Serializable {
    private String itemId;
    private String restaurantId;
    private String itemName;
    private double itemPrice;
    private int quantity;
    private String category;
    private List<Option> toppings;
    private String imageUrl;
    private int imageResource;

    // Constructors
    public CartItem() {
        this.toppings = new ArrayList<>();
        this.quantity = 1;
    }

    public CartItem(String itemId, String restaurantId, String itemName, double itemPrice, String category, 
                   String imageUrl, int imageResource) {
        this.itemId = itemId;
        this.restaurantId = restaurantId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = 1;
        this.category = category;
        this.toppings = new ArrayList<>();
        this.imageUrl = imageUrl;
        this.imageResource = imageResource;
    }

    public CartItem(String itemId, String restaurantId, String itemName, double itemPrice, int quantity, 
                   String category, List<Option> toppings, String imageUrl, int imageResource) {
        this.itemId = itemId;
        this.restaurantId = restaurantId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
        this.category = category;
        this.toppings = toppings != null ? toppings : new ArrayList<>();
        this.imageUrl = imageUrl;
        this.imageResource = imageResource;
    }

    // Getters and setters
    public String getItemId() { 
        return itemId; 
    }
    
    public void setItemId(String itemId) { 
        this.itemId = itemId; 
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getItemName() { 
        return itemName; 
    }
    
    public void setItemName(String itemName) { 
        this.itemName = itemName; 
    }

    public double getItemPrice() { 
        return itemPrice; 
    }
    
    public void setItemPrice(double itemPrice) { 
        this.itemPrice = itemPrice; 
    }

    public int getQuantity() { 
        return quantity; 
    }
    
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public void decrementQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    public String getCategory() { 
        return category; 
    }
    
    public void setCategory(String category) { 
        this.category = category; 
    }

    public List<Option> getToppings() { 
        return toppings; 
    }
    
    public void setToppings(List<Option> toppings) { 
        this.toppings = toppings; 
    }

    public void addTopping(Option topping) {
        if (this.toppings == null) {
            this.toppings = new ArrayList<>();
        }
        this.toppings.add(topping);
    }

    public String getImageUrl() { 
        return imageUrl; 
    }
    
    public void setImageUrl(String imageUrl) { 
        this.imageUrl = imageUrl; 
    }

    public int getImageResource() { 
        return imageResource; 
    }
    
    public void setImageResource(int imageResource) { 
        this.imageResource = imageResource; 
    }

    // Calculate total price for this cart item
    public double getTotalPrice() {
        double toppingPrice = 0;
        if (toppings != null) {
            for (Option option : toppings) {
                toppingPrice += option.getPrice();
            }
        }
        return (itemPrice + toppingPrice) * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        
        // Basic item equality check
        boolean basicEqual = Objects.equals(itemId, cartItem.itemId);
        
        // If toppings are null/empty in both objects, or identical lists
        if ((toppings == null || toppings.isEmpty()) && 
            (cartItem.toppings == null || cartItem.toppings.isEmpty())) {
            return basicEqual;
        }
        
        // If one has toppings and other doesn't
        if ((toppings == null || toppings.isEmpty()) != 
            (cartItem.toppings == null || cartItem.toppings.isEmpty())) {
            return false;
        }
        
        // Compare toppings if both have them
        if (toppings != null && cartItem.toppings != null) {
            if (toppings.size() != cartItem.toppings.size()) {
                return false;
            }
            
            // Check if all toppings are the same (ignoring order)
            for (Option option : toppings) {
                boolean found = false;
                for (Option otherOption : cartItem.toppings) {
                    if (Objects.equals(option.getId(), otherOption.getId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }
        
        return basicEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
}