package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.CartItemAdapter;
import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShowCartActivity extends AppCompatActivity implements OrderItemManager.OnCartUpdateListener {

    // UI Components
    private RecyclerView cartItemsRecyclerView;
    private TextView emptyCartText;
    private TextView subtotalTextView;
    private Button checkoutButton;
    private Button clearCartButton;
    private Toolbar toolbar;
    private ImageButton backButton;

    // Data
    private OrderItemManager orderItemManager;
    private CartItemAdapter restaurantCartAdapter;
    private NumberFormat currencyFormat;
    DatabaseReference reference;
    StorageReference storageReference;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize currency format
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize CartManager
        orderItemManager = OrderItemManager.getInstance(this);
        orderItemManager.setOnCartUpdateListener(this);

        // Initialize UI components
        initViews();
        setupRecyclerView();
        setupClickListeners();

        // Update cart items
        updateCartItems();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setTitle("Giỏ hàng");
        }

        backButton = findViewById(R.id.backButton);
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);
        emptyCartText = findViewById(R.id.emptyCartText);
        subtotalTextView = findViewById(R.id.subtotalTextView);
        checkoutButton = findViewById(R.id.checkoutButton);
        clearCartButton = findViewById(R.id.clearCartButton);
    }

    private void setupRecyclerView() {
        List<CartItem> restaurantCarts = groupItemsByRestaurant(orderItemManager.getCartItems());
        restaurantCartAdapter = new CartItemAdapter( restaurantCarts);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(restaurantCartAdapter);
    }

    private List<CartItem> groupItemsByRestaurant(List<OrderItem> items) {
        Map<String, List<OrderItem>> restaurantItemsMap = new HashMap<>();
        Map<String, String> restaurantNames = new HashMap<>();
        Map<String, String> restaurantImages = new HashMap<>();

        // Group items by restaurant
        for (OrderItem item : items) {
            String restaurantId = item.getRestaurantId();
            if (!restaurantItemsMap.containsKey(restaurantId)) {
                restaurantItemsMap.put(restaurantId, new ArrayList<>());
                restaurantNames.put(restaurantId, item.getRestaurantName());
            }
            restaurantItemsMap.get(restaurantId).add(item);
        }

        // Convert to CartItem list
        List<CartItem> restaurantCarts = new ArrayList<>();
        for (String restaurantId : restaurantItemsMap.keySet()) {
            CartItem cartItem = new CartItem(
                restaurantId,
                restaurantNames.get(restaurantId),
                restaurantImages.get(restaurantId),
                restaurantItemsMap.get(restaurantId)
            );
            restaurantCarts.add(cartItem);
        }

        return restaurantCarts;
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Checkout button
        checkoutButton.setOnClickListener(v -> {
            if (!orderItemManager.isEmpty()) {
                Intent intent = new Intent(ShowCartActivity.this, OrderActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            }
        });

        // Clear cart button
        clearCartButton.setOnClickListener(v -> {
            if (!orderItemManager.isEmpty()) {
                orderItemManager.clearCart();
                Toast.makeText(this, "Đã xóa giỏ hàng", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Giỏ hàng đã trống", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    public void loadFromFirebase() {
//        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//        reference.child("carts").child(currentUser.getUid()).child(res)
//
//    }
    private void updateCartItems() {
        List<OrderItem> cartItems = orderItemManager.getCartItems();
        if (cartItems.isEmpty()) {
            emptyCartText.setVisibility(View.VISIBLE);
            cartItemsRecyclerView.setVisibility(View.GONE);
            checkoutButton.setEnabled(false);
            clearCartButton.setEnabled(false);
        } else {
            emptyCartText.setVisibility(View.GONE);
            cartItemsRecyclerView.setVisibility(View.VISIBLE);
            checkoutButton.setEnabled(true);
            clearCartButton.setEnabled(true);
            List<CartItem> restaurantCarts = groupItemsByRestaurant(cartItems);
            restaurantCartAdapter.setCartItems(restaurantCarts);
        }

        // Update subtotal
        double subtotal = orderItemManager.getCartTotal();
        String formattedSubtotal = currencyFormat.format(subtotal).replace("₫", "đ");
        subtotalTextView.setText("Tổng tiền: " + formattedSubtotal);
    }

    @Override
    public void onCartUpdated(List<OrderItem> cartItems, double total) {
        updateCartItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh cart when returning to this activity
        updateCartItems();
    }


} 