package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.CartItemAdapter;
import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity implements CartItemAdapter.OnCartItemClickListener {
    private ImageButton backButton;
    private TextView manageButton;
    private RecyclerView cartItemsRecyclerView;
    private TextView emptyCartText;
    private TextView subtotalTextView;
    private Button clearCartButton;

    private CartItemAdapter cartItemAdapter;
    private OrderItemManager orderItemManager;
    private DatabaseReference databaseReference;
    private NumberFormat currencyFormat;

    private Map<String, CartItem> restaurantCarts;
    private List<CartItem> cartItemsList;
    private double totalAmount;
    private int loadingCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize managers and formatters
        orderItemManager = OrderItemManager.getInstance(this);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize data structures
        restaurantCarts = new HashMap<>();
        cartItemsList = new ArrayList<>();

        // Initialize views
        initializeViews();
        setupClickListeners();
        setupRecyclerView();

        // Load cart items
        loadCartItems();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        manageButton = findViewById(R.id.manageButton);
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);
        emptyCartText = findViewById(R.id.emptyCartText);
        subtotalTextView = findViewById(R.id.subtotalTextView);
        clearCartButton = findViewById(R.id.clearCartButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        manageButton.setOnClickListener(v -> {
            // TODO: Implement manage cart functionality
            Toast.makeText(this, "Quản lý giỏ hàng", Toast.LENGTH_SHORT).show();
        });

        clearCartButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xóa giỏ hàng")
                    .setMessage("Bạn có chắc muốn xóa tất cả các món trong giỏ hàng?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        orderItemManager.clearCart();
                        updateUI();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void setupRecyclerView() {
        cartItemAdapter = new CartItemAdapter(cartItemsList);
        cartItemAdapter.setOnCartItemClickListener(this);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(cartItemAdapter);
    }

    private void loadCartItems() {
        List<OrderItem> cartItems = orderItemManager.getCartItems();
        if (cartItems.isEmpty()) {
            updateUI();
            return;
        }

        // Get all unique restaurant IDs
        List<String> restaurantIds = orderItemManager.getRestaurantIds();

        // Clear previous data
        restaurantCarts.clear();
        loadingCount = restaurantIds.size();

        // Load restaurant details for each restaurant
        for (String restaurantId : restaurantIds) {
            List<OrderItem> restaurantItems = orderItemManager.getRestaurantItems(restaurantId);
            loadRestaurantDetails(restaurantId, restaurantItems);
        }
    }

    private void loadRestaurantDetails(String restaurantId, List<OrderItem> items) {
        databaseReference.child("restaurants").child(restaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String restaurantName = dataSnapshot.child("name").getValue(String.class);
                            String restaurantImage = dataSnapshot.child("imageUrl").getValue(String.class);
                            String address = dataSnapshot.child("address").getValue(String.class);
                            String category = dataSnapshot.child("category").getValue(String.class);
                            Double distance = dataSnapshot.child("distance").getValue(Double.class);

                            // Create cart item for this restaurant
                            CartItem cartItem = new CartItem(
                                    restaurantId,
                                    restaurantName != null ? restaurantName : "Nhà hàng",
                                    restaurantImage,
                                    items
                            );

                            // Set additional restaurant info
                            cartItem.setAddress(address);
                            cartItem.setCategory(category);
                            cartItem.setDistance(distance != null ? distance : 0.0);

                            restaurantCarts.put(restaurantId, cartItem);

                            // Check if all restaurants are loaded
                            loadingCount--;
                            if (loadingCount <= 0) {
                                updateCartItemsList();
                            }
                        } else {
                            loadingCount--;
                            if (loadingCount <= 0) {
                                updateCartItemsList();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(CartActivity.this,
                                "Lỗi khi tải thông tin nhà hàng",
                                Toast.LENGTH_SHORT).show();
                        loadingCount--;
                        if (loadingCount <= 0) {
                            updateCartItemsList();
                        }
                    }
                });
    }

    private void updateCartItemsList() {
        cartItemsList.clear();
        cartItemsList.addAll(restaurantCarts.values());
        cartItemAdapter.setCartItems(cartItemsList);

        // Calculate total amount across all restaurants
        totalAmount = 0;
        for (CartItem cartItem : cartItemsList) {
            totalAmount += cartItem.getTotalPrice();
        }

        updateUI();
    }

    private void updateUI() {
        boolean isEmpty = cartItemsList.isEmpty();

        // Update visibility
        emptyCartText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cartItemsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        clearCartButton.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // Update total amount
        if (!isEmpty) {
            String formattedTotal = currencyFormat.format(totalAmount).replace("₫", "đ");
            subtotalTextView.setText("Tổng tiền: " + formattedTotal);
        } else {
            subtotalTextView.setText("Tổng tiền: 0đ");
        }

        // Update title with restaurant count
        if (!isEmpty) {
            setTitle("Giỏ hàng (" + cartItemsList.size() + " nhà hàng)");
        } else {
            setTitle("Giỏ hàng");
        }
    }

    @Override
    public void onCartItemClick(CartItem cartItem) {
        // Open CheckOutActivity for the selected restaurant
        Intent intent = new Intent(this, CheckOutActivity.class);
        intent.putExtra("RESTAURANT_ID", cartItem.getRestaurantId());
        intent.putExtra("RESTAURANT_NAME", cartItem.getRestaurantName());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems(); // Refresh cart when returning to this activity
    }
}