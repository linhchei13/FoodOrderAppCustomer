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
    private static final String PREF_NAME = "cart" + FirebaseAuth.getInstance().getUid();
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

        // Validate item ID
        if (newItem.getItemId() == null || newItem.getItemId().isEmpty()) {
            showToast("Lỗi: Không tìm thấy thông tin món ăn");
            Log.e(TAG, "addItem: Item ID is null or empty");
            return;
        }

        // Find existing item with same ID, restaurant, and toppings
        OrderItem existingItem = findExistingItem(newItem);

        if (existingItem != null) {
            // Update quantity of existing item - ADD to existing quantity
            existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());

            // Recalculate total price
            double basePrice = existingItem.getItemPrice();
            double toppingsPrice = 0;
            if (existingItem.getToppings() != null) {
                for (Option topping : existingItem.getToppings()) {
                    toppingsPrice += topping.getPrice();
                }
            }
            existingItem.setTotalPrice((basePrice + toppingsPrice) * existingItem.getQuantity());

            Log.d(TAG, "addItem: Updated quantity for existing item: " + existingItem.getItemName() + ", new quantity: " + existingItem.getQuantity());
        } else {
            // Calculate total price for new item
            double basePrice = newItem.getItemPrice();
            double toppingsPrice = 0;
            if (newItem.getToppings() != null) {
                for (Option topping : newItem.getToppings()) {
                    toppingsPrice += topping.getPrice();
                }
            }
            newItem.setTotalPrice((basePrice + toppingsPrice) * newItem.getQuantity());

            // Add new item
            cartItems.add(newItem);
            Log.d(TAG, "addItem: Added new item: " + newItem.getItemName() + " from restaurant: " + newItem.getRestaurantId());
        }

        saveCart();
        notifyCartUpdated();
        showToast("Đã thêm vào giỏ hàng");
    }

    // Update item quantity - SET exact quantity (not add)
    public void updateItemQuantity(OrderItem item, int newQuantity) {
        OrderItem existingItem = findExistingItem(item);
        if (existingItem != null) {
            if (newQuantity <= 0) {
                removeItem(existingItem);
                Log.d(TAG, "updateItemQuantity: Removed item due to zero quantity: " + existingItem.getItemName());
            } else {
                // SET the exact quantity (not add)
                existingItem.setQuantity(newQuantity);

                // Recalculate total price
                double basePrice = existingItem.getItemPrice();
                double toppingsPrice = 0;
                if (existingItem.getToppings() != null) {
                    for (Option topping : existingItem.getToppings()) {
                        toppingsPrice += topping.getPrice();
                    }
                }
                existingItem.setTotalPrice((basePrice + toppingsPrice) * newQuantity);

                saveCart();
                notifyCartUpdated();
                Log.d(TAG, "updateItemQuantity: Set quantity for item: " + existingItem.getItemName() + " to " + newQuantity);
            }
        } else {
            Log.w(TAG, "updateItemQuantity: Item not found in cart: " + item.getItemName());
        }
    }

    // Set item quantity to exact value (for updating from detail activity)
    public void setItemQuantity(OrderItem item, int exactQuantity) {
        OrderItem existingItem = findExistingItem(item);
        if (existingItem != null) {
            if (exactQuantity <= 0) {
                removeItem(existingItem);
            } else {
                existingItem.setQuantity(exactQuantity);

                // Recalculate total price
                double basePrice = existingItem.getItemPrice();
                double toppingsPrice = 0;
                if (existingItem.getToppings() != null) {
                    for (Option topping : existingItem.getToppings()) {
                        toppingsPrice += topping.getPrice();
                    }
                }
                existingItem.setTotalPrice((basePrice + toppingsPrice) * exactQuantity);

                saveCart();
                notifyCartUpdated();
            }
        } else {
            // If item doesn't exist and quantity > 0, add it
            if (exactQuantity > 0) {
                item.setQuantity(exactQuantity);
                addItem(item);
            }
        }
    }

    // Remove item from cart
    public void removeItem(OrderItem item) {
        boolean removed = false;

        // Try to find and remove by exact object reference first
        if (cartItems.remove(item)) {
            removed = true;
        } else {
            // If not found by reference, find by ID and restaurant
            OrderItem toRemove = null;
            for (OrderItem cartItem : cartItems) {
                if (cartItem.getItemId().equals(item.getItemId()) &&
                        cartItem.getRestaurantId().equals(item.getRestaurantId()) &&
                        haveSameToppings(cartItem.getToppings(), item.getToppings())) {
                    toRemove = cartItem;
                    break;
                }
            }
            if (toRemove != null) {
                cartItems.remove(toRemove);
                removed = true;
            }
        }

        if (removed) {
            Log.d(TAG, "removeItem: Removed item: " + item.getItemName());
            saveCart();
            notifyCartUpdated();
        } else {
            Log.w(TAG, "removeItem: Item not found in cart: " + item.getItemName());
        }
    }

    // Clear entire cart
    public void clearCart() {
        cartItems.clear();
        saveCart();
        notifyCartUpdated();
        Log.d(TAG, "clearCart: Cart cleared");
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
        double total = 0;
        for (OrderItem item : cartItems) {
            if (item.getRestaurantId().equals(restaurantId)) {
                total += item.getTotalPrice();
            }
        }
        return total;
    }

    // Get total quantity for a specific restaurant
    public int getRestaurantQuantity(String restaurantId) {
        int total = 0;
        for (OrderItem item : cartItems) {
            if (item.getRestaurantId().equals(restaurantId)) {
                total += item.getQuantity();
            }
        }
        return total;
    }

    // Remove all items from a specific restaurant
    public void removeRestaurantItems(String restaurantId) {
        cartItems.removeIf(item -> item.getRestaurantId().equals(restaurantId));
        saveCart();
        notifyCartUpdated();
        Log.d(TAG, "removeRestaurantItems: Removed all items from restaurant: " + restaurantId);
    }

    // Get total price of all items in cart
    public double getCartTotal() {
        double total = 0;
        for (OrderItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    // Calculate total amount (alias for getCartTotal for compatibility)
    public double calculateTotalAmount() {
        return getCartTotal();
    }

    // Get total number of items in cart
    public int getItemCount() {
        return cartItems.size();
    }

    // Get total quantity of all items in cart
    public int getTotalQuantity() {
        int total = 0;
        for (OrderItem item : cartItems) {
            total += item.getQuantity();
        }
        return total;
    }

    // Check if cart is empty
    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    // Check if cart has items from specific restaurant
    public boolean hasItemsFromRestaurant(String restaurantId) {
        for (OrderItem item : cartItems) {
            if (item.getRestaurantId().equals(restaurantId)) {
                return true;
            }
        }
        return false;
    }

    // Get quantity of specific item in cart (considering toppings)
    public int getItemQuantity(String itemId, String restaurantId, List<Option> toppings) {
        for (OrderItem item : cartItems) {
            if (item.getItemId().equals(itemId) &&
                    item.getRestaurantId().equals(restaurantId) &&
                    haveSameToppings(item.getToppings(), toppings)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    // Get quantity of specific item in cart (without considering toppings)
    public int getItemQuantity(String itemId) {
        int totalQuantity = 0;
        for (OrderItem item : cartItems) {
            if (item.getItemId().equals(itemId)) {
                totalQuantity += item.getQuantity();
            }
        }
        return totalQuantity;
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
            if (Math.abs(toppingMap1.get(option2.getId()).getPrice() - option2.getPrice()) > 0.01) {
                return false;
            }
        }

        return true;
    }

    private void notifyCartUpdated() {
        if (onCartUpdateListener != null) {
            Log.d(TAG, "notifyCartUpdated: Listener is not null, calling onCartUpdated.");
            onCartUpdateListener.onCartUpdated(new ArrayList<>(cartItems), getCartTotal());
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

        Log.d(TAG, "saveCartToLocal: Cart saved with " + cartItems.size() + " items");
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
                Log.d(TAG, "loadCartFromLocal: Loaded " + cartItems.size() + " items from local storage");
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
