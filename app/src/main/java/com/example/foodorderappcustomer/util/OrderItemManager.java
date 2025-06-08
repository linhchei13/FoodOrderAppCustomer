package com.example.foodorderappcustomer.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A singleton class to manage the shopping cart
 * Supports multiple restaurants with separate orders for each
 */
public class OrderItemManager {
    private static final String TAG = "OrderItemManager";
    private static final String PREF_NAME = "cart_preferences";
    private static final String CART_ITEMS_KEY = "cart_items";

    private static OrderItemManager instance;
    private final Context context;
    private final List<OrderItem> cartItems; // Contains items from multiple restaurants
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

    // Add item to cart - allows multiple restaurants
    public void addItem(OrderItem newItem) {
        addItem(newItem, null);
    }

    // Add item to cart with activity context
    public void addItem(OrderItem newItem, Activity activityContext) {
        // Validate restaurant ID
        if (newItem.getRestaurantId() == null || newItem.getRestaurantId().isEmpty()) {
            showToast("Lỗi: Không tìm thấy thông tin nhà hàng");
            Log.e(TAG, "addItem: Restaurant ID is null or empty");
            return;
        }

        // Find existing item with same ID, restaurant, and toppings
        OrderItem existingItem = findExistingItem(newItem);

        if (existingItem != null) {
            // Update quantity of existing item
            existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
            Log.d(TAG, "addItem: Updated quantity for existing item: " + existingItem.getItemName() + ", new quantity: " + existingItem.getQuantity());
        } else {
            // Add new item
            cartItems.add(newItem);
            Log.d(TAG, "addItem: Added new item: " + newItem.getItemName() + " from restaurant: " + newItem.getRestaurantId());
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

    // Clear entire cart
    public void clearCart() {
        cartItems.clear();
        saveCart();
        notifyCartUpdated();
    }

    // Get all items in cart
    public List<OrderItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    // Get all unique restaurant IDs in cart
    public List<String> getRestaurantIds() {
        List<String> restaurantIds = new ArrayList<>();
        for (OrderItem item : cartItems) {
            if (!restaurantIds.contains(item.getRestaurantId())) {
                restaurantIds.add(item.getRestaurantId());
            }
        }
        return restaurantIds;
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

    // Get total quantity for a specific restaurant
    public int getRestaurantQuantity(String restaurantId) {
        return cartItems.stream()
                .filter(item -> item.getRestaurantId().equals(restaurantId))
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    // Remove all items from a specific restaurant
    public void removeRestaurantItems(String restaurantId) {
        cartItems.removeIf(item -> item.getRestaurantId().equals(restaurantId));
        saveCart();
        notifyCartUpdated();
    }

    // Get total price of all items in cart
    public double getCartTotal() {
        return cartItems.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();
    }

    // Get total number of items in cart
    public int getItemCount() {
        return cartItems.size();
    }

    // Get total quantity of all items in cart
    public int getTotalQuantity() {
        return cartItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    // Check if cart is empty
    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    // Check if cart has items from specific restaurant
    public boolean hasItemsFromRestaurant(String restaurantId) {
        return cartItems.stream()
                .anyMatch(item -> item.getRestaurantId().equals(restaurantId));
    }

    // Get quantity of specific item in cart
    public int getItemQuantity(String itemId) {
        return cartItems.stream()
                .filter(item -> item.getItemId().equals(itemId))
                .mapToInt(OrderItem::getQuantity)
                .sum();
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
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            notifyFailure(listener, "User must be logged in to submit an order");
            Log.e(TAG, "submitOrder: User not logged in.");
            return;
        }

        // Validate order items and restaurant ID
        if (order.getRestaurantId() == null || order.getRestaurantId().isEmpty()) {
            notifyFailure(listener, "Lỗi: Không tìm thấy thông tin nhà hàng cho đơn hàng");
            Log.e(TAG, "submitOrder: Restaurant ID is null or empty in order.");
            return;
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            notifyFailure(listener, "Lỗi: Đơn hàng không có món ăn");
            Log.e(TAG, "submitOrder: Order items list is null or empty.");
            return;
        }

        // Ensure all items in order are from the same restaurant
        String orderRestaurantId = order.getRestaurantId();
        for (OrderItem item : order.getItems()) {
            if (!item.getRestaurantId().equals(orderRestaurantId)) {
                notifyFailure(listener, "Lỗi: Món ăn trong đơn hàng từ các nhà hàng khác nhau");
                Log.e(TAG, "submitOrder: Items from different restaurants found in order.");
                return;
            }
        }

        // Generate order ID
        String orderId = generateOrderId();
        if (orderId == null) {
            notifyFailure(listener, "Lỗi: Không thể tạo ID đơn hàng");
            Log.e(TAG, "submitOrder: Failed to generate order ID.");
            return;
        }
        order.setId(orderId);
        order.setUserId(currentUser.getUid());

        databaseReference.child("orders").child(orderId).setValue(order)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "submitOrder: Order submitted successfully. Order ID: " + orderId);
                    // Remove only the items from this restaurant after successful order
                    removeRestaurantItems(orderRestaurantId);
                    if (listener != null) {
                        listener.onSuccess(order);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "submitOrder: Failed to submit order. Error: " + e.getMessage());
                    notifyFailure(listener, e.getMessage());
                });
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

        // Create a map for quick lookup of toppings1 by ID
        Map<String, Option> toppingMap1 = new HashMap<>();
        for (Option option : toppings1) {
            toppingMap1.put(option.getId(), option);
        }

        // Check if all toppings2 are present in toppings1 and have same price
        for (Option option2 : toppings2) {
            if (!toppingMap1.containsKey(option2.getId())) {
                return false;
            }
            if (toppingMap1.get(option2.getId()).getPrice() != option2.getPrice()) {
                return false;
            }
        }

        return true;
    }

    private void notifyCartUpdated() {
        if (onCartUpdateListener != null) {
            Log.d(TAG, "notifyCartUpdated: Listener is not null, calling onCartUpdated.");
            onCartUpdateListener.onCartUpdated(cartItems, getCartTotal());
        } else {
            Log.w(TAG, "notifyCartUpdated: Listener is null. Cart update not propagated.");
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

    private String generateOrderId() {
        Date currentTime = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");
        String dateString = dateFormat.format(currentTime);
        int random = new Random().nextInt(10000); // 0-9999
        String randomNum = String.format("%04d", random);
        return dateString + randomNum;
    }
}