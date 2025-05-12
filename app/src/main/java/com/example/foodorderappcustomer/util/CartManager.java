package com.example.foodorderappcustomer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.foodorderappcustomer.Models.CartItem;
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
public class CartManager {
    private static final String TAG = "CartManager";
    private static final String PREF_NAME = "cart_preferences";
    private static final String CART_ITEMS_KEY = "cart_items";
    
    private static CartManager instance;
    private final Context context;
    private final List<CartItem> cartItems;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth firebaseAuth;
    private OnCartUpdateListener onCartUpdateListener;
    
    // Interface for cart update callbacks
    public interface OnCartUpdateListener {
        void onCartUpdated(List<CartItem> cartItems, double total);
    }
    
    private CartManager(Context context) {
        this.context = context.getApplicationContext();
        this.cartItems = new ArrayList<>();
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        this.firebaseAuth = FirebaseAuth.getInstance();
        loadCartFromLocal();
        syncWithFirebase();
    }
    
    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }
    
    public void setOnCartUpdateListener(OnCartUpdateListener listener) {
        this.onCartUpdateListener = listener;
    }
    
    // Add item to cart
    public void addItem(CartItem cartItem) {
        // Check if item already exists in cart
        boolean itemExists = false;
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem existingItem = cartItems.get(i);
            if (existingItem.getItemId().equals(cartItem.getItemId())) {
                // Check if toppings are the same
                if (haveSameToppings(existingItem.getToppings(), cartItem.getToppings())) {
                    // Update quantity
                    existingItem.setQuantity(existingItem.getQuantity() + cartItem.getQuantity());
                    itemExists = true;
                    break;
                }
            }
        }
        
        // If item doesn't exist, add it
        if (!itemExists) {
            cartItems.add(cartItem);
        }
        
        saveCart();
        notifyCartUpdated();
        Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
    }
    
    // Remove item from cart
    public void removeItem(CartItem cartItem) {
        cartItems.remove(cartItem);
        saveCart();
        notifyCartUpdated();
    }
    
    // Remove item from cart by index
    public void removeItem(int position) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.remove(position);
            saveCart();
            notifyCartUpdated();
        }
    }
    
    // Update item quantity
    public void updateItemQuantity(CartItem cartItem, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(cartItem);
        } else {
            for (CartItem item : cartItems) {
                if (item.getItemId().equals(cartItem.getItemId()) && 
                    haveSameToppings(item.getToppings(), cartItem.getToppings())) {
                    item.setQuantity(newQuantity);
                    break;
                }
            }
            saveCart();
            notifyCartUpdated();
        }
    }
    
    // Update cart item by index
    public void updateItem(int position, CartItem cartItem) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.set(position, cartItem);
            saveCart();
            notifyCartUpdated();
        }
    }
    
    // Clear cart
    public void clearCart() {
        cartItems.clear();
        saveCart();
        notifyCartUpdated();
    }
    
    // Get all items in cart
    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }
    
    // Get total price of items in cart
    public double getCartTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }
    
    // Get number of items in cart
    public int getItemCount() {
        return cartItems.size();
    }
    
    // Check if cart is empty
    public boolean isEmpty() {
        return cartItems.isEmpty();
    }
    
    // Check if two topping lists are the same
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
    
    // Notify listeners that cart has been updated
    private void notifyCartUpdated() {
        if (onCartUpdateListener != null) {
            onCartUpdateListener.onCartUpdated(cartItems, getCartTotal());
        }
    }
    
    // Save cart to SharedPreferences and Firebase
    private void saveCart() {
        saveCartToLocal();
        saveCartToFirebase();
    }
    
    // Save cart to SharedPreferences
    private void saveCartToLocal() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        Gson gson = new Gson();
        String json = gson.toJson(cartItems);
        
        editor.putString(CART_ITEMS_KEY, json);
        editor.apply();
    }
    
    // Load cart from SharedPreferences
    private void loadCartFromLocal() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(CART_ITEMS_KEY, null);
        
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<CartItem>>(){}.getType();
            List<CartItem> loadedItems = gson.fromJson(json, type);
            
            if (loadedItems != null) {
                cartItems.clear();
                cartItems.addAll(loadedItems);
            }
        }
    }
    
    // Save cart to Firebase
    private void saveCartToFirebase() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference.child("carts").child(userId).setValue(cartItems)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cart saved to Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving cart to Firebase", e));
        }
    }
    
    // Sync cart with Firebase
    private void syncWithFirebase() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference.child("carts").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Only load from Firebase if local cart is empty
                    if (cartItems.isEmpty() && dataSnapshot.exists()) {
                        List<CartItem> firebaseCartItems = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            CartItem item = snapshot.getValue(CartItem.class);
                            if (item != null) {
                                firebaseCartItems.add(item);
                            }
                        }
                        
                        cartItems.clear();
                        cartItems.addAll(firebaseCartItems);
                        saveCartToLocal();
                        notifyCartUpdated();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error syncing cart with Firebase", databaseError.toException());
                }
            });
        }
    }
    
    // Create order from cart
    public Order createOrder(String restaurantId, String restaurantName, double deliveryFee, String address, String paymentMethod) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be logged in to create an order");
        }
        
        String userId = currentUser.getUid();
        
        return new Order(
            userId,
            restaurantId,
            restaurantName,
            new ArrayList<>(cartItems), // Use cart items directly
            getCartTotal(),
            deliveryFee,
            address,
            paymentMethod
        );
    }
    
    // Submit order to Firebase
    public void submitOrder(Order order, OnOrderSubmitListener listener) {
        if (firebaseAuth.getCurrentUser() == null) {
            if (listener != null) {
                listener.onFailure("User must be logged in to submit an order");
            }
            return;
        }
        
        // Generate a new order ID
        String orderId = databaseReference.child("orders").push().getKey();
        if (orderId == null) {
            if (listener != null) {
                listener.onFailure("Failed to generate order ID");
            }
            return;
        }
        
        order.setId(orderId);
        
        // Save order to Firebase
        databaseReference.child("orders").child(orderId).setValue(order)
            .addOnSuccessListener(aVoid -> {
                clearCart();
                if (listener != null) {
                    listener.onSuccess(order);
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onFailure(e.getMessage());
                }
            });
    }
    
    // Interface for order submission callbacks
    public interface OnOrderSubmitListener {
        void onSuccess(Order order);
        void onFailure(String errorMessage);
    }
} 