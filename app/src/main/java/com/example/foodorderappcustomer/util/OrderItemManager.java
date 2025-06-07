package com.example.foodorderappcustomer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.Models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A singleton class to manage the shopping cart
 */
public class OrderItemManager {
    private static final String TAG = "OrderItemManager";
    private static final String PREF_NAME = "cart_preferences";
    private static final String CART_ITEMS_KEY = "cart_items";
    
    private static OrderItemManager instance;
    private final Context context;
    private final List<OrderItem> cartItems;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth firebaseAuth;
    private OnCartUpdateListener onCartUpdateListener;
    
    // Interface for cart update callbacks
    public interface OnCartUpdateListener {
        void onCartUpdated(List<OrderItem> cartItems, double total);
    }
    
    private OrderItemManager(Context context) {
        this.context = context.getApplicationContext();
        this.cartItems = new ArrayList<>();
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        this.firebaseAuth = FirebaseAuth.getInstance();
        loadCartFromLocal();
    }
    
    public static synchronized OrderItemManager getInstance(Context context) {
        if (instance == null) {
            instance = new OrderItemManager(context);
        }
        return instance;
    }
    
    public void setOnCartUpdateListener(OnCartUpdateListener listener) {
        this.onCartUpdateListener = listener;
    }
    
    // Add item to cart
    public void addItem(OrderItem newItem) {
        // Validate restaurant ID
        if (newItem.getRestaurantId() == null || newItem.getRestaurantId().isEmpty()) {
            showToast("Lỗi: Không tìm thấy thông tin nhà hàng");
            return;
        }

        // Find existing item with same ID and toppings
        OrderItem existingItem = findExistingItem(newItem);
        
        if (existingItem != null) {
            // Update quantity of existing item
            existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
        } else {
            // Add new item
            cartItems.add(newItem);
        }
        
        saveCart();
        notifyCartUpdated();
        showToast("Đã thêm vào giỏ hàng");
    }
    
    // Remove item from cart
    public void removeItem(OrderItem item) {
        if (cartItems.remove(item)) {
            saveCart();
            notifyCartUpdated();
        }
    }
    
    // Update item quantity
    public void updateItemQuantity(OrderItem item, int newQuantity) {
        OrderItem existingItem = findExistingItem(item);
        if (existingItem != null) {
            if (newQuantity <= 0) {
                removeItem(existingItem);
            } else {
                existingItem.setQuantity(newQuantity);
                saveCart();
                notifyCartUpdated();
            }
        }
    }
    
    // Clear cart
    public void clearCart() {
        cartItems.clear();
        saveCart();
        notifyCartUpdated();
    }
    
    // Get all items in cart
    public List<OrderItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }
    
    // Get total price of items in cart
    public double getCartTotal() {
        return cartItems.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();
    }
    
    // Get number of items in cart
    public int getItemCount() {
        return cartItems.size();
    }
    
    // Check if cart is empty
    public boolean isEmpty() {
        return cartItems.isEmpty();
    }
    
    // Get items for a specific restaurant
    public List<OrderItem> getRestaurantItems(String restaurantId) {
        List<OrderItem> restaurantItems = new ArrayList<>();
        for (OrderItem item : cartItems) {
            if (item.getRestaurantId().equals(restaurantId)) {
                restaurantItems.add(item);
            }
        }
        return restaurantItems;
    }
    
    // Get total price for a specific restaurant
    public double getRestaurantTotal(String restaurantId) {
        return cartItems.stream()
                .filter(item -> item.getRestaurantId().equals(restaurantId))
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();
    }
    
    // Remove all items from a specific restaurant
    public void removeRestaurantItems(String restaurantId) {
        cartItems.removeIf(item -> item.getRestaurantId().equals(restaurantId));
        saveCart();
        notifyCartUpdated();
    }
    
    // Create order for a specific restaurant
    public Order createRestaurantOrder(String restaurantId, String restaurantName, double deliveryFee, String address, String paymentMethod) {
        if (firebaseAuth.getCurrentUser() == null) {
            throw new IllegalStateException("User must be logged in to create an order");
        }
        
        List<OrderItem> restaurantItems = getRestaurantItems(restaurantId);
        if (restaurantItems.isEmpty()) {
            throw new IllegalStateException("No items found for restaurant");
        }
        
        // Validate all items are from the same restaurant
        for (OrderItem item : restaurantItems) {
            if (!item.getRestaurantId().equals(restaurantId)) {
                throw new IllegalStateException("Invalid items in cart: items from different restaurants");
            }
        }
        
        return new Order(
            firebaseAuth.getCurrentUser().getUid(),
            restaurantId,
            restaurantName,
            new ArrayList<>(restaurantItems),
            getRestaurantTotal(restaurantId),
            deliveryFee,
            address,
            paymentMethod
        );
    }
    
    // Submit order to Firebase
    public void submitOrder(Order order, OnOrderSubmitListener listener) {
        if (firebaseAuth.getCurrentUser() == null) {
            notifyFailure(listener, "User must be logged in to submit an order");
            return;
        }
        
        // Validate all items in order are from the same restaurant
        String restaurantId = order.getRestaurantId();
        for (OrderItem item : order.getItems()) {
            if (!item.getRestaurantId().equals(restaurantId)) {
                notifyFailure(listener, "Invalid order: items from different restaurants");
                return;
            }
        }
        
        String orderId = databaseReference.child("orders").push().getKey();
        if (orderId == null) {
            notifyFailure(listener, "Failed to generate order ID");
            return;
        }
        
        order.setId(orderId);
        
        databaseReference.child("orders").child(orderId).setValue(order)
            .addOnSuccessListener(aVoid -> {
                // Remove only the items from this restaurant after successful order
                removeRestaurantItems(restaurantId);
                if (listener != null) {
                    listener.onSuccess(order);
                }
            })
            .addOnFailureListener(e -> notifyFailure(listener, e.getMessage()));
    }
    
    // Interface for order submission callbacks
    public interface OnOrderSubmitListener {
        void onSuccess(Order order);
        void onFailure(String errorMessage);
    }

    // Helper Methods
    private OrderItem findExistingItem(OrderItem item) {
        for (OrderItem existingItem : cartItems) {
            if (existingItem.getItemId().equals(item.getItemId()) && 
                existingItem.getRestaurantId().equals(item.getRestaurantId()) &&
                haveSameToppings(existingItem.getToppings(), item.getToppings())) {
                return existingItem;
            }
        }
        return null;
    }

    private boolean haveSameToppings(List<Option> toppings1, List<Option> toppings2) {
        if (toppings1 == null && toppings2 == null) return true;
        if (toppings1 == null || toppings2 == null) return false;
        if (toppings1.size() != toppings2.size()) return false;

        Map<String, Option> toppingMap = new HashMap<>();
        for (Option option : toppings1) {
            toppingMap.put(option.getId(), option);
        }
        
        for (Option option : toppings2) {
            if (!toppingMap.containsKey(option.getId())) {
                return false;
            }
        }
        
        return true;
    }
    
    private void notifyCartUpdated() {
        if (onCartUpdateListener != null) {
            onCartUpdateListener.onCartUpdated(cartItems, getCartTotal());
        }
    }
    
    private void saveCart() {
        saveCartToLocal();
    }
    
    private void saveCartToLocal() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        editor.putString(CART_ITEMS_KEY, gson.toJson(cartItems));
        editor.apply();
    }
    
    private void loadCartFromLocal() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(CART_ITEMS_KEY, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<OrderItem>>(){}.getType();
            List<OrderItem> loadedItems = gson.fromJson(json, type);

            if (loadedItems != null) {
                cartItems.clear();
                cartItems.addAll(loadedItems);
            }
        }
    }
    
    private void notifyFailure(OnOrderSubmitListener listener, String message) {
        if (listener != null) {
            listener.onFailure(message);
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
} 