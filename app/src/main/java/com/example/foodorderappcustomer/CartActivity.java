package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.CartItemAdapter;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity implements CartItemAdapter.OnCartItemListener {

    // UI Components
    private ImageButton backButton;
    private TextView titleTextView;
    private TextView manageButton;
    private TextView cancelButton;
    private RecyclerView cartItemsRecyclerView;
    private LinearLayout emptyCartLayout;
    private LinearLayout manageModeBottomPanel;
    private CheckBox selectAllCheckBox;
    private Button deleteButton;

    // Data
    private CartItemAdapter cartItemAdapter;
    private OrderItemManager orderItemManager;
    private NumberFormat currencyFormat;
    private boolean isManageMode = false;
    private List<String> selectedRestaurantIds = new ArrayList<>();
    private Map<String, List<OrderItem>> restaurantItemsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize managers and formatters
        orderItemManager = OrderItemManager.getInstance(this);
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize views
        initializeViews();
        setupClickListeners();
        setupRecyclerView();

        // Load cart items
        loadCartItems();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        titleTextView = findViewById(R.id.titleTextView);
        manageButton = findViewById(R.id.manageButton);
        cancelButton = findViewById(R.id.cancelButton);
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);

        emptyCartLayout = findViewById(R.id.emptyCartLayout);

        manageModeBottomPanel = findViewById(R.id.manageModeBottomPanel);
        selectAllCheckBox = findViewById(R.id.selectAllCheckBox);
        deleteButton = findViewById(R.id.deleteButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        manageButton.setOnClickListener(v -> {
            // Switch to manage mode
            setManageMode(true);
        });

        cancelButton.setOnClickListener(v -> {
            // Switch back to normal mode
            setManageMode(false);
        });


        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectAllRestaurants();
            } else {
                deselectAllRestaurants();
            }
        });

        deleteButton.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void setupRecyclerView() {
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemAdapter = new CartItemAdapter(this, restaurantItemsMap, selectedRestaurantIds);
        cartItemAdapter.setOnCartItemListener(this);
        cartItemsRecyclerView.setAdapter(cartItemAdapter);
    }

    private void loadCartItems() {
        // Get all cart items
        List<OrderItem> allItems = orderItemManager.getCartItems();

        // Group items by restaurant
        restaurantItemsMap.clear();
        for (OrderItem item : allItems) {
            String restaurantId = item.getRestaurantId();
            if (!restaurantItemsMap.containsKey(restaurantId)) {
                restaurantItemsMap.put(restaurantId, new ArrayList<>());
            }
            restaurantItemsMap.get(restaurantId).add(item);
        }

        // Update adapter
        cartItemAdapter.setRestaurantItemsMap(restaurantItemsMap);

        // Update UI based on cart state
        updateUI();
    }

    private void updateUI() {
        if (restaurantItemsMap.isEmpty()) {
            // Empty cart
            emptyCartLayout.setVisibility(View.VISIBLE);
            cartItemsRecyclerView.setVisibility(View.GONE);
            manageButton.setVisibility(View.GONE);

            // Always show normal mode for empty cart
            setManageMode(false);
        } else {
            // Has items
            emptyCartLayout.setVisibility(View.GONE);
            cartItemsRecyclerView.setVisibility(View.VISIBLE);

            // Only show manage button in normal mode
            if (!isManageMode) {
                manageButton.setVisibility(View.VISIBLE);
            }
            updateDeleteButton();
        }
    }

    private void setManageMode(boolean manageMode) {
        isManageMode = manageMode;

        if (manageMode) {
            manageButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.VISIBLE);
            manageModeBottomPanel.setVisibility(View.VISIBLE);

            // Clear selections
            selectedRestaurantIds.clear();
            selectAllCheckBox.setChecked(false);
        } else {
            // Switch to normal mode
            titleTextView.setText("Giỏ hàng");
            manageButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.GONE);
            manageModeBottomPanel.setVisibility(View.GONE);

            // Clear selections
            selectedRestaurantIds.clear();
        }

        // Update adapter to reflect mode change
        cartItemAdapter.setManageMode(manageMode);
        cartItemAdapter.notifyDataSetChanged();

        // Update delete button
        updateDeleteButton();
    }

    private void selectAllRestaurants() {
        selectedRestaurantIds.clear();
        selectedRestaurantIds.addAll(restaurantItemsMap.keySet());
        cartItemAdapter.notifyDataSetChanged();
        updateDeleteButton();
    }

    private void deselectAllRestaurants() {
        selectedRestaurantIds.clear();
        cartItemAdapter.notifyDataSetChanged();
        updateDeleteButton();
    }

    private void updateDeleteButton() {
        boolean hasSelection = !selectedRestaurantIds.isEmpty();
        deleteButton.setEnabled(hasSelection);
        deleteButton.setAlpha(hasSelection ? 1.0f : 0.5f);

        if (hasSelection) {
            deleteButton.setText("Xóa (" + selectedRestaurantIds.size() + ")");
        } else {
            deleteButton.setText("Xóa");
        }
    }

    private void showDeleteConfirmation() {
        if (selectedRestaurantIds.isEmpty()) {
            return;
        }

        String message;
        if (selectedRestaurantIds.size() == 1) {
            message = "Bạn có chắc muốn xóa nhà hàng này khỏi giỏ hàng?";
        } else {
            message = "Bạn có chắc muốn xóa " + selectedRestaurantIds.size() + " nhà hàng khỏi giỏ hàng?";
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage(message)
                .setPositiveButton("Xóa", (dialog, which) -> deleteSelectedRestaurants())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSelectedRestaurants() {
        // Remove items from selected restaurants
        for (String restaurantId : selectedRestaurantIds) {
            orderItemManager.removeRestaurantItems(restaurantId);
        }

        // Clear selection
        selectedRestaurantIds.clear();

        // Reload cart
        loadCartItems();

        Toast.makeText(this, "Đã xóa khỏi giỏ hàng", Toast.LENGTH_SHORT).show();

        // If cart is now empty, switch back to normal mode
        if (restaurantItemsMap.isEmpty()) {
            setManageMode(false);
        }
    }

    @Override
    public void onRestaurantSelectionChanged(String restaurantId, boolean isSelected) {
        if (isSelected) {
            if (!selectedRestaurantIds.contains(restaurantId)) {
                selectedRestaurantIds.add(restaurantId);
            }
        } else {
            selectedRestaurantIds.remove(restaurantId);
        }

        updateDeleteButton();

        // Update select all checkbox
        selectAllCheckBox.setOnCheckedChangeListener(null);
        selectAllCheckBox.setChecked(selectedRestaurantIds.size() == restaurantItemsMap.size() && !restaurantItemsMap.isEmpty());
        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectAllRestaurants();
            } else {
                deselectAllRestaurants();
            }
        });
    }

    @Override
    public void onRestaurantClicked(String restaurantId) {
        if (!isManageMode) {
            // In normal mode, navigate to restaurant detail or checkout
            Toast.makeText(this, "Chuyển đến thanh toán cho nhà hàng: " + restaurantId, Toast.LENGTH_SHORT).show();

            // Example navigation to checkout
            Intent intent = new Intent(this, CheckOutActivity.class);
            intent.putExtra("RESTAURANT_ID", restaurantId);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems(); // Refresh cart when returning to this activity
    }

    @Override
    public void onBackPressed() {
        if (isManageMode) {
            // If in manage mode, switch back to normal mode
            setManageMode(false);
        } else {
            // Otherwise, normal back behavior
            super.onBackPressed();
        }
    }
}
