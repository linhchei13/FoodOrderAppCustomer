package com.example.foodorderappcustomer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.CheckOutItemAdapter;
import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CheckOutActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private ImageButton backButton;
    private TextView toolbarTitle;
    private RecyclerView cartItemsRecyclerView;
    private TextView addressEditText;
    private TextInputEditText noteEditText;
    private RadioGroup paymentMethodRadioGroup;
    private RadioButton cashOnDeliveryRadioButton;
    private RadioButton bankTransferRadioButton;
    private RadioButton eWalletRadioButton;
    private TextView promotionCodeEditText;
    private ImageView applyPromotionButton;
    private TextView subtotalTextView;
    private TextView deliveryFeeTextView;
    private TextView discountTextView;
    private TextView totalTextView;
    private LinearLayout discountLayout;
    private ExtendedFloatingActionButton checkoutButton;

    // Data and Managers
    private CheckOutItemAdapter checkOutItemAdapter;
    private OrderItemManager orderItemManager;
    private DatabaseReference databaseReference;
    private NumberFormat currencyFormat;

    private ActivityResultLauncher<Intent> locationActivityLauncher;

    private String restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    private List<OrderItem> restaurantItems;
    private double subtotal;
    private double deliveryFee = 15000; // Default delivery fee
    private double discount = 0;
    private double total;
    private FirebaseAuth firebaseAuth;

    // Thêm vào phần khai báo biến trong CheckOutActivity
    private ActivityResultLauncher<Intent> promotionActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String promoCode = result.getData().getStringExtra("PROMO_CODE");
                    String promoId = result.getData().getStringExtra("PROMO_ID");
                    double discountAmount = result.getData().getDoubleExtra("DISCOUNT_AMOUNT", 0);

                    // Cập nhật UI và dữ liệu
                    promotionCodeEditText.setText(promoCode + ": Giảm " + (int) discountAmount /1000 + "K");
                    discount = discountAmount;
                    discountLayout.setVisibility(View.VISIBLE);
                    calculateTotals();

                    Toast.makeText(CheckOutActivity.this, "Áp dụng mã ưu đãi thành công!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_out);
        // Get current user
        firebaseAuth = FirebaseAuth.getInstance();
        // Get restaurant info from intent
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");
        restaurantName = getIntent().getStringExtra("RESTAURANT_NAME");

        if (restaurantId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin nhà hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        locationActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String selectedAddress = result.getData().getStringExtra("selected_address");
                        if (selectedAddress != null && !selectedAddress.isEmpty()) {
                            // Update the address text
                            addressEditText.setText(selectedAddress);

                            // Update SharedPreferences with the new selected address
                            SharedPreferences prefs = getSharedPreferences(firebaseAuth.getCurrentUser().getUid(), MODE_PRIVATE);
                            prefs.edit()
                                    .putString("current_address", selectedAddress)
                                    .putBoolean("has_selected_address", true)
                                    .apply();
                        }
                    }
                }
        );

        // Initialize managers and formatters
        orderItemManager = OrderItemManager.getInstance(this);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize views
        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupRecyclerView();

        // Load restaurant-specific data
        loadRestaurantItems();
        loadRestaurantInfo();
        loadUserAddress();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        backButton = findViewById(R.id.backButton);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);
        addressEditText = findViewById(R.id.addressEditText);
        noteEditText = findViewById(R.id.noteEditText);
        paymentMethodRadioGroup = findViewById(R.id.paymentMethodRadioGroup);
        cashOnDeliveryRadioButton = findViewById(R.id.cashOnDeliveryRadioButton);
        bankTransferRadioButton = findViewById(R.id.bankTransferRadioButton);
        eWalletRadioButton = findViewById(R.id.eWalletRadioButton);
        promotionCodeEditText = findViewById(R.id.promotionCodeEditText);
        applyPromotionButton = findViewById(R.id.applyPromotionButton);
        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryFeeTextView = findViewById(R.id.deliveryFeeTextView);
        discountTextView = findViewById(R.id.discountTextView);
        totalTextView = findViewById(R.id.totalTextView);
        discountLayout = findViewById(R.id.discountLayout);
        checkoutButton = findViewById(R.id.checkoutButton);

        // Set default values
        toolbarTitle.setText("Đơn hàng của bạn");
        cashOnDeliveryRadioButton.setChecked(true);
        // Thêm vào setupClickListeners() trong CheckOutActivity
        promotionCodeEditText.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOutActivity.this, PromotionActivity.class);
            intent.putExtra("RESTAURANT_ID", restaurantId);
            intent.putExtra("ORDER_TOTAL", subtotal);
            promotionActivityLauncher.launch(intent);
        });
        applyPromotionButton.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOutActivity.this, PromotionActivity.class);
            intent.putExtra("RESTAURANT_ID", restaurantId);
            intent.putExtra("ORDER_TOTAL", subtotal);
            promotionActivityLauncher.launch(intent);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        checkoutButton.setOnClickListener(v -> placeOrder());

    }

    private void setupRecyclerView() {
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadRestaurantItems() {
        // Get only items from the selected restaurant
        restaurantItems = orderItemManager.getRestaurantItems(restaurantId);

        if (restaurantItems.isEmpty()) {
            Toast.makeText(this, "Không có món nào từ nhà hàng này trong giỏ hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up adapter with restaurant-specific items
        checkOutItemAdapter = new CheckOutItemAdapter(restaurantItems);
        cartItemsRecyclerView.setAdapter(checkOutItemAdapter);

        // Calculate subtotal for this restaurant only
        calculateTotals();
    }

    private void loadRestaurantInfo() {
        if (restaurantId == null) return;

        databaseReference.child("restaurants").child(restaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            restaurantName = dataSnapshot.child("name").getValue(String.class);
                            restaurantAddress = dataSnapshot.child("address").getValue(String.class);

                            // Update delivery fee if available
                            Double restaurantDeliveryFee = dataSnapshot.child("deliveryFee").getValue(Double.class);
                            if (restaurantDeliveryFee != null) {
                                deliveryFee = restaurantDeliveryFee;
                                calculateTotals();
                            }

                            // Update toolbar title with restaurant name
                            if (restaurantName != null) {
                                toolbarTitle.setText("Đơn hàng từ " + restaurantName);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Use default values
                    }
                });
    }

    private void loadUserAddress() {
        // Make address text clickable to open location selection
        addressEditText.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOutActivity.this, LocationActivity.class);
            locationActivityLauncher.launch(intent);
        });

        // First check if user has manually selected an address
        SharedPreferences prefs = getSharedPreferences(firebaseAuth.getUid(), MODE_PRIVATE);
        boolean hasSelectedAddress = prefs.getBoolean("has_selected_address", false);
        String currentAddress = prefs.getString("current_address", null);

        if (hasSelectedAddress && currentAddress != null && !currentAddress.isEmpty()) {
            addressEditText.setText(currentAddress);
        } else {
            // If no manually selected address, try to get the most recent address from Firebase
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                databaseReference.child("users")
                        .child(currentUser.getUid())
                        .child("recentAddresses")
                        .orderByChild("lastUsed")
                        .limitToLast(1)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        String recentAddress = snapshot.child("formattedAddress").getValue(String.class);
                                        if (recentAddress != null && !recentAddress.isEmpty()) {
                                            addressEditText.setText(recentAddress);
                                            // Update SharedPreferences but don't set has_selected_address flag
                                            prefs.edit()
                                                    .putString("current_address", recentAddress)
                                                    .putBoolean("has_selected_address", false)
                                                    .apply();
                                            return;
                                        }
                                    }
                                }
                                // If no recent address found, show default text
                                addressEditText.setText("Chọn địa chỉ giao hàng");
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(CheckOutActivity.this,
                                        "Lỗi khi tải địa chỉ: " + databaseError.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void calculateTotals() {
        // Calculate subtotal for this restaurant only
        subtotal = orderItemManager.getRestaurantTotal(restaurantId);
        total = subtotal + deliveryFee - discount;

        // Ensure total is not negative
        if (total < 0) {
            total = 0;
        }

        // Update UI
        updateTotalDisplay();
    }

    private void updateTotalDisplay() {
        String formattedSubtotal = currencyFormat.format(subtotal).replace("₫", "đ");
        String formattedDeliveryFee = currencyFormat.format(deliveryFee).replace("₫", "đ");
        String formattedDiscount = currencyFormat.format(discount).replace("₫", "đ");
        String formattedTotal = currencyFormat.format(total).replace("₫", "đ");

        subtotalTextView.setText(formattedSubtotal);
        deliveryFeeTextView.setText(formattedDeliveryFee);
        discountTextView.setText("-" + formattedDiscount);
        totalTextView.setText(formattedTotal);

        // Update checkout button text
        checkoutButton.setText("Đặt hàng • " + formattedTotal);
    }

    private void placeOrder() {
        // Validate items
        if (restaurantItems == null || restaurantItems.isEmpty()) {
            Toast.makeText(this, "Không có món nào để đặt hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get payment method
        String paymentMethod;
        int selectedPaymentId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        if (selectedPaymentId == R.id.cashOnDeliveryRadioButton) {
            paymentMethod = "Thanh toán khi nhận hàng";
        } else if (selectedPaymentId == R.id.bankTransferRadioButton) {
            paymentMethod = "Chuyển khoản ngân hàng";
        } else if (selectedPaymentId == R.id.eWalletRadioButton) {
            paymentMethod = "Ví điện tử";
        } else {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get delivery address
        String deliveryAddress = addressEditText.getText().toString().trim();
        if (deliveryAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get note
        String note = noteEditText.getText().toString().trim();

        try {
            // Create order for this restaurant only
            Order order = orderItemManager.createRestaurantOrder(
                    restaurantId,
                    restaurantName != null ? restaurantName : "Nhà hàng",
                    deliveryFee,
                    deliveryAddress,
                    paymentMethod
            );

            // Add note if provided
            if (!note.isEmpty()) {
                order.setNote(note);
            }

            // Add discount if applied
            if (discount > 0) {
                order.setDiscount(discount);
                order.setPromotionId(promotionCodeEditText.getText().toString().trim());
            }

            // Update total with discount
            order.setTotal(total);

            // Submit order
            checkoutButton.setEnabled(false);
            checkoutButton.setText("Đang đặt hàng...");

            orderItemManager.submitOrder(order, new OrderItemManager.OnOrderSubmitListener() {
                @Override
                public void onSuccess(Order submittedOrder) {
                    runOnUiThread(() -> {
                        Toast.makeText(CheckOutActivity.this,
                                "Đặt hàng thành công! Mã đơn hàng: " + submittedOrder.getId(),
                                Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(CheckOutActivity.this, OrderInformationActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(CheckOutActivity.this,
                                "Lỗi đặt hàng: " + errorMessage,
                                Toast.LENGTH_LONG).show();

                        checkoutButton.setEnabled(true);
                        updateTotalDisplay(); // Restore button text
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            checkoutButton.setEnabled(true);
            updateTotalDisplay(); // Restore button text
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh restaurant items in case cart was updated
        loadRestaurantItems();
    }
}