// Topping.java
package com.example.foodorderappcustomer.Models;

public class Option {
    private String id;
    private String name;
    private double price;
    private boolean selected;

    public Option(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.selected = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}