package com.example.foodorderappcustomer.util;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SalesManager {
    private static final String TAG = "SalesManager";
    private static SalesManager instance;
    private final DatabaseReference databaseReference;

    private SalesManager() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized SalesManager getInstance() {
        if (instance == null) {
            instance = new SalesManager();
        }
        return instance;
    }

    public void updateSalesForOrder(String orderId) {
        DatabaseReference orderRef = databaseReference.child("orders").child(orderId);
        
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String status = snapshot.child("status").getValue(String.class);
                if (!"completed".equals(status)) return;

                // Get all items in the order
                DataSnapshot itemsSnapshot = snapshot.child("items");
                Map<String, Integer> itemQuantities = new HashMap<>();

                // Count quantities for each item
                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                    String itemId = itemSnapshot.child("itemId").getValue(String.class);
                    Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                    
                    if (itemId != null && quantity != null) {
                        itemQuantities.put(itemId, quantity);
                    }
                }

                // Update sales for each item
                updateSalesForItems(itemQuantities);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error updating sales: " + error.getMessage());
            }
        });
    }

    private void updateSalesForItems(Map<String, Integer> itemQuantities) {
        DatabaseReference menuItemsRef = databaseReference.child("menuItems");

        for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
            String itemId = entry.getKey();
            int quantity = entry.getValue();

            menuItemsRef.child(itemId).child("sales")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        int currentSales = 0;
                        if (snapshot.exists()) {
                            currentSales = snapshot.getValue(Integer.class);
                        }
                        
                        // Update with new total
                        menuItemsRef.child(itemId).child("sales")
                                .setValue(currentSales + quantity)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Sales updated for item: " + itemId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating sales for item " + itemId + ": " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting current sales for item " + itemId + ": " + e.getMessage());
                    });
        }
    }

    public void recalculateAllSales() {
        DatabaseReference ordersRef = databaseReference.child("orders");
        DatabaseReference menuItemsRef = databaseReference.child("menuItems");

        // First, reset all sales to 0
        menuItemsRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                menuItemsRef.child(itemSnapshot.getKey()).child("sales").setValue(0);
            }

            // Then calculate sales from completed orders
            ordersRef.orderByChild("status").equalTo("completed")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Map<String, Integer> totalSales = new HashMap<>();

                            for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                                DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                    String itemId = itemSnapshot.child("itemId").getValue(String.class);
                                    Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);

                                    if (itemId != null && quantity != null) {
                                        totalSales.put(itemId, 
                                            totalSales.getOrDefault(itemId, 0) + quantity);
                                    }
                                }
                            }

                            // Update all sales counts
                            for (Map.Entry<String, Integer> entry : totalSales.entrySet()) {
                                menuItemsRef.child(entry.getKey()).child("sales")
                                        .setValue(entry.getValue());
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Error recalculating sales: " + error.getMessage());
                        }
                    });
        });
    }
} 