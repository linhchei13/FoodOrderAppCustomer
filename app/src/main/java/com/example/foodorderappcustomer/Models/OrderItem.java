package com.example.foodorderappcustomer.Models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderItem {
    private String itemId;
    private String restaurantId;
    private String itemName;
    private double itemPrice;
    private int quantity;
    private String category;
    private List<Option> toppings;
    private List<Option> selectedOptions;
    private List<String> specialInstructions;
    private String imageUrl;
    private String description;
    private String restaurantName;
    private double totalPrice;

    private String note;


    // Constructors
    public OrderItem() {
        this.toppings = new ArrayList<>();
        this.selectedOptions = new ArrayList<>();
        this.specialInstructions = new ArrayList<>();
        this.quantity = 1;
    }

    public OrderItem(String itemId, String restaurantId, String itemName, double itemPrice, String category,
                     String imageUrl) {
        this.itemId = itemId;
        this.restaurantId = restaurantId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = 1;
        this.category = category;
        this.toppings = new ArrayList<>();
        this.selectedOptions = new ArrayList<>();
        this.specialInstructions = new ArrayList<>();
        this.imageUrl = imageUrl;
    }

    public OrderItem(String itemId, String itemName, double itemPrice, int quantity,
                     String imageUrl, String description, String restaurantId, String restaurantName) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.description = description;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.toppings = new ArrayList<>();
        this.selectedOptions = new ArrayList<>();
        this.specialInstructions = new ArrayList<>();
        this.totalPrice = itemPrice * quantity;
    }

    public OrderItem(String itemId, String restaurantId, String itemName, double itemPrice, int quantity,
                     String category, List<Option> toppings, String imageUrl) {
        this.itemId = itemId;
        this.restaurantId = restaurantId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
        this.category = category;
        this.toppings = toppings != null ? toppings : new ArrayList<>();
        this.selectedOptions = new ArrayList<>();
        this.specialInstructions = new ArrayList<>();
        this.imageUrl = imageUrl;
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


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public List<Option> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<Option> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }

    public void addSelectedOption(Option option) {
        if (this.selectedOptions == null) {
            this.selectedOptions = new ArrayList<>();
        }
        this.selectedOptions.add(option);
    }

    public List<String> getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(List<String> specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public void addSpecialInstruction(String instruction) {
        if (this.specialInstructions == null) {
            this.specialInstructions = new ArrayList<>();
        }
        this.specialInstructions.add(instruction);
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    // Calculate total price for this cart item
    public double getTotalPrice() {
        if (totalPrice > 0) {
            return totalPrice;
        }
        
        double calculatedPrice = itemPrice;
        
        // Add topping prices
        if (toppings != null) {
            for (Option option : toppings) {
                calculatedPrice += option.getPrice();
            }
        }
        
        // Add selected option prices
        if (selectedOptions != null) {
            for (Option option : selectedOptions) {
                if (option.isSelected()) {
                    calculatedPrice += option.getPrice();
                }
            }
        }
        
        // Check if upsize is included in special instructions
        if (specialInstructions != null) {
            for (String instruction : specialInstructions) {
                if (instruction.toLowerCase().contains("upsize")) {
                    calculatedPrice += 6000; // Add upsize fee
                    break;
                }
            }
        }
        
        return calculatedPrice * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem cartItem = (OrderItem) o;
        
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