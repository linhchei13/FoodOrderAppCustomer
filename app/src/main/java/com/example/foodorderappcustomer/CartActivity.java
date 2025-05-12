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
import com.example.foodorderappcustomer.util.CartManager;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartManager.OnCartUpdateListener, CartItemAdapter.CartItemListener {

    // UI Components
    private RecyclerView cartItemsRecyclerView;
    private TextView emptyCartText;
    private TextView subtotalTextView;
    private Button checkoutButton;
    private Button clearCartButton;
    private Toolbar toolbar;
    private ImageButton backButton;

    // Data
    private CartManager cartManager;
    private CartItemAdapter cartItemAdapter;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize currency format
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize CartManager
        cartManager = CartManager.getInstance(this);
        cartManager.setOnCartUpdateListener(this);

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
        List<CartItem> items = cartManager.getCartItems();
        cartItemAdapter = new CartItemAdapter(items);
        cartItemAdapter.setListener(this);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(cartItemAdapter);
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Checkout button
        checkoutButton.setOnClickListener(v -> {
            if (!cartManager.isEmpty()) {
                Intent intent = new Intent(CartActivity.this, OrderActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            }
        });

        // Clear cart button
        clearCartButton.setOnClickListener(v -> {
            if (!cartManager.isEmpty()) {
                cartManager.clearCart();
                Toast.makeText(this, "Đã xóa giỏ hàng", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Giỏ hàng đã trống", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCartItems() {
        List<CartItem> cartItems = cartManager.getCartItems();
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
            cartItemAdapter.setCartItems(cartItems);
        }

        // Update subtotal
        double subtotal = cartManager.getCartTotal();
        String formattedSubtotal = currencyFormat.format(subtotal).replace("₫", "đ");
        subtotalTextView.setText("Tổng tiền: " + formattedSubtotal);
    }

    @Override
    public void onCartUpdated(List<CartItem> cartItems, double total) {
        updateCartItems();
    }

    @Override
    public void onQuantityChanged(CartItem cartItem, int newQuantity) {
        cartManager.updateItemQuantity(cartItem, newQuantity);
    }

    @Override
    public void onRemoveItem(CartItem cartItem) {
        cartManager.removeItem(cartItem);
        Toast.makeText(this, "Đã xóa món khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh cart when returning to this activity
        updateCartItems();
    }
} 