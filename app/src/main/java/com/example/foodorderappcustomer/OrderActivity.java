package com.example.foodorderappcustomer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.OrderItemAdapter;
import com.example.foodorderappcustomer.Adapter.ReviewImageAdapter;
import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.Models.Promotion;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.example.foodorderappcustomer.util.PaymentService;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

public class OrderActivity extends AppCompatActivity implements OrderItemAdapter.OrderItemListener, OrderItemManager.OnCartUpdateListener {

    private static final String TAG = "OrderActivity";
    private static final double DEFAULT_DELIVERY_FEE = 15000;
    private static final int MAX_IMAGES = 5;
    private static final int LOCATION_REQUEST_CODE = 1001;

    private FirebaseAuth firebaseAuth;

    // UI Components
    private RecyclerView cartItemsRecyclerView;
    private TextView emptyCartText;
    private CardView deliveryCard, paymentCard, orderSummaryCard, promotionCard;
    private TextView subtotalTextView, deliveryFeeTextView, totalTextView, discountTextView;
    private EditText  noteEditText, promotionCodeEditText;
    private TextView addressEditText;
    private Button applyPromotionButton;
    private RadioGroup paymentMethodRadioGroup;
    private ExtendedFloatingActionButton checkoutButton;
    private Toolbar toolbar;
    private ImageButton backButton;

    // Data
    private OrderItemManager orderItemManager;
    private OrderItemAdapter orderItemAdapter;
    private NumberFormat currencyFormat;
    private double subtotal;
    private double deliveryFee = DEFAULT_DELIVERY_FEE;
    private double total;
    private double discount = 0;
    private Promotion appliedPromotion;
    private String restaurantId;
    private String restaurantName;
    private DatabaseReference databaseReference;

    private ReviewImageAdapter reviewImageAdapter;
    private ActivityResultLauncher<Intent> locationActivityLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private List<Uri> selectedImages = new ArrayList<>();
    LinearLayout discountLayer;
    TextView discountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize currency format
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize Cart
        orderItemManager = OrderItemManager.getInstance(this);
        orderItemManager.setOnCartUpdateListener(this);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Get restaurant info from intent (if available)
        if (getIntent().hasExtra("RESTAURANT_ID")) {
            restaurantId = getIntent().getStringExtra("RESTAURANT_ID");
        }
        if (getIntent().hasExtra("RESTAURANT_NAME")) {
            restaurantName = getIntent().getStringExtra("RESTAURANT_NAME");
        }


        // Initialize UI components
        initViews();
        setupRecyclerView();
        setupClickListeners();

        // Load user's saved address
        loadUserAddress();

        // Update cart items
        updateCartItems();

        // Initialize image picker launchers
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        Toast.makeText(this, "Cần quyền truy cập ảnh để thêm hình", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && selectedImages.size() < MAX_IMAGES) {
                        selectedImages.add(uri);
                        reviewImageAdapter.addImage(uri);
                    } else if (selectedImages.size() >= MAX_IMAGES) {
                        Toast.makeText(this, "Chỉ có thể thêm tối đa " + MAX_IMAGES + " hình", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Initialize location activity launcher
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

    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        backButton = findViewById(R.id.backButton);
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);
        emptyCartText = findViewById(R.id.emptyCartText);
        deliveryCard = findViewById(R.id.deliveryCard);
        paymentCard = findViewById(R.id.paymentCard);
        orderSummaryCard = findViewById(R.id.orderSummaryCard);
        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryFeeTextView = findViewById(R.id.deliveryFeeTextView);
        totalTextView = findViewById(R.id.totalTextView);
        addressEditText = findViewById(R.id.addressEditText);
        noteEditText = findViewById(R.id.noteEditText);
        paymentMethodRadioGroup = findViewById(R.id.paymentMethodRadioGroup);
        checkoutButton = findViewById(R.id.checkoutButton);
        promotionCard = findViewById(R.id.promotionCard);
        discountTextView = findViewById(R.id.discountTextView);
        promotionCodeEditText = findViewById(R.id.promotionCodeEditText);
        applyPromotionButton = findViewById(R.id.applyPromotionButton);
        discountLayer = findViewById(R.id.discountLayout);
        discountText = findViewById(R.id.discountTextView);

        // Setup promotion button
        applyPromotionButton.setOnClickListener(v -> applyPromotionCode());
    }

    private void setupRecyclerView() {
        // Get only items for this restaurant
        List<OrderItem> restaurantItems = orderItemManager.getRestaurantItems(restaurantId);
        orderItemAdapter = new OrderItemAdapter(restaurantItems);
        orderItemAdapter.setListener(this);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(orderItemAdapter);
        
        // Update totals
        updateTotals();
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Checkout button
        checkoutButton.setOnClickListener(v -> checkout());
    }

    private void updateCartItems() {
        List<OrderItem> cartItems = orderItemManager.getCartItems();
        emptyCartText.setVisibility(View.GONE);
        cartItemsRecyclerView.setVisibility(View.VISIBLE);
        deliveryCard.setVisibility(View.VISIBLE);
        paymentCard.setVisibility(View.VISIBLE);
        orderSummaryCard.setVisibility(View.VISIBLE);
        checkoutButton.setVisibility(View.VISIBLE);
        orderItemAdapter.setOrderItems(cartItems);

        // Update totals
        updateTotals();
    }

    private void updateTotals() {
        // Calculate totals for this restaurant only
        subtotal = orderItemManager.getRestaurantTotal(restaurantId);
        total = subtotal + deliveryFee - discount;

        // Update UI
        String formattedSubtotal = currencyFormat.format(subtotal).replace("₫", "đ");
        String formattedDeliveryFee = currencyFormat.format(deliveryFee).replace("₫", "đ");
        String formattedTotal = currencyFormat.format(total).replace("₫", "đ");
        String formattedDiscount = currencyFormat.format(discount).replace("₫", "đ");

        subtotalTextView.setText(formattedSubtotal);
        deliveryFeeTextView.setText(formattedDeliveryFee);
        totalTextView.setText(formattedTotal);
        
        if (discount > 0) {
            discountLayer.setVisibility(View.VISIBLE);
            discountText.setText("-" + formattedDiscount);
        } else {
            discountLayer.setVisibility(View.GONE);
        }

        // Update checkout button state
        boolean hasItems = !orderItemManager.getRestaurantItems(restaurantId).isEmpty();
        checkoutButton.setEnabled(hasItems);
        emptyCartText.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        cartItemsRecyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);
    }

    private void checkout() {
        if (orderItemManager.getRestaurantItems(restaurantId).isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate address
        String address = addressEditText.getText().toString().trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get payment method
        int selectedPaymentId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        if (selectedPaymentId == -1) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        String paymentMethod = ((RadioButton) findViewById(selectedPaymentId)).getText().toString();

        // Get note
        String note = noteEditText.getText().toString().trim();

        try {
            // Create order for this restaurant only
            Order order = orderItemManager.createRestaurantOrder(
                restaurantId,
                restaurantName,
                deliveryFee,
                address,
                paymentMethod
            );
            order.setNote(note);
            if (appliedPromotion != null) {
                order.setPromotionId(appliedPromotion.getId());
                order.setDiscount(discount);
            }

            // Submit order
            orderItemManager.submitOrder(order, new OrderItemManager.OnOrderSubmitListener() {
                @Override
                public void onSuccess(Order order) {
                    Toast.makeText(OrderActivity.this, "Đặt hàng thành công", Toast.LENGTH_SHORT).show();
                    // Navigate to order information
                    Intent intent = new Intent(OrderActivity.this, OrderInformationActivity.class);
                    intent.putExtra("ORDER_ID", order.getId());
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(OrderActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoginPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Cần đăng nhập")
                .setMessage("Bạn cần đăng nhập để đặt hàng.")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    Intent intent = new Intent(OrderActivity.this, LoginActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyPromotionCode() {
        String code = promotionCodeEditText.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Vui lòng nhập mã ưu đãi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable UI elements while processing
        applyPromotionButton.setEnabled(false);
        promotionCodeEditText.setEnabled(false);

        // Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Đang kiểm tra mã")
                .setMessage("Vui lòng đợi...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // Move Firebase operation to background thread
        new Thread(() -> {
            try {
                databaseReference.child("promotions")
                        .orderByChild("promoCode")
                        .equalTo(code)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            runOnUiThread(() -> {
                                loadingDialog.dismiss();
                                if (!snapshot.exists()) {
                                    showPromotionError("Mã ưu đãi không tồn tại");
                                    return;
                                }

                                // Get the first matching promotion
                                Promotion promotion = null;
                                for (var child : snapshot.getChildren()) {
                                    promotion = child.getValue(Promotion.class);
                                    if (promotion != null) {
                                        promotion.setId(child.getKey());
                                        break;
                                    }
                                }

                                if (promotion == null) {
                                    showPromotionError("Mã ưu đãi không hợp lệ");
                                    return;
                                }

                                // Validate promotion
//                                if (!promotion.isValid()) {
//                                    showPromotionError("Mã ưu đãi đã hết hạn hoặc không còn hiệu lực");
//                                    return;
//                                }

                                // Check restaurant restriction
                                if (promotion.getRestaurantId() != null &&
                                        !promotion.getRestaurantId().equals(restaurantId)) {
                                    showPromotionError("Mã ưu đãi không áp dụng cho nhà hàng này");
                                    return;
                                }

                                // Check minimum order amount
                                if (subtotal < Double.valueOf(promotion.getMinimumOrder())) {
                                    showPromotionError(String.format(
                                            "Đơn hàng tối thiểu %.0fđ để áp dụng mã này",
                                            promotion.getMinimumOrder()));
                                    return;
                                }

                                // Apply promotion
                                appliedPromotion = promotion;

                                calculateDiscount();
                                discountText.setText(String.format("%.0fđ", discount));
                                discountLayer.setVisibility(View.VISIBLE);
                                updateTotals();
                                Toast.makeText(OrderActivity.this,
                                        "Áp dụng mã ưu đãi thành công!",
                                        Toast.LENGTH_SHORT).show();

                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                loadingDialog.dismiss();
                                showPromotionError("Lỗi: " + e.getMessage());
                            });
                        });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    showPromotionError("Lỗi không xác định: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showPromotionError(String message) {
        // Re-enable UI elements
        applyPromotionButton.setEnabled(true);
        promotionCodeEditText.setEnabled(true);

        // Show error message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void calculateDiscount() {
        if (appliedPromotion == null) {
            discount = 0;
            return;
        }

        try {
            if (appliedPromotion.getDiscountType().equals("percentage")) {
                discount = subtotal * (appliedPromotion.getDiscountAmount() / 100);
                // Apply max discount if set
                if (appliedPromotion.getMaxDiscountAmount() > 0) {
                    discount = Math.min(discount, appliedPromotion.getMaxDiscountAmount());
                }
            } else {
                discount = appliedPromotion.getDiscountAmount();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating discount", e);
            discount = 0;
        }
    }

    @Override
    public void onCartUpdated(List<OrderItem> cartItems, double total) {
        // Filter items for this restaurant only
        List<OrderItem> restaurantItems = orderItemManager.getRestaurantItems(restaurantId);
        orderItemAdapter.setOrderItems(restaurantItems);
        updateTotals();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any pending operations
//        if (databaseReference != null) {
//            databaseReference.removeEventListener(null);
//        }
    }

    private void loadUserAddress() {
        // Make address text clickable to open location selection
        addressEditText.setOnClickListener(v -> {
            Intent intent = new Intent(OrderActivity.this, LocationActivity.class);
            locationActivityLauncher.launch(intent);
        });

        // First check if user has manually selected an address
        SharedPreferences prefs = getSharedPreferences(firebaseAuth.getUid(), MODE_PRIVATE);
        boolean hasSelectedAddress = prefs.getBoolean("has_selected_address", false);
        String currentAddress = prefs.getString("selected_address", null);

        if (hasSelectedAddress && currentAddress != null && !currentAddress.isEmpty()) {
            // Use the manually selected address
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
                                Toast.makeText(OrderActivity.this, 
                                    "Lỗi khi tải địa chỉ: " + databaseError.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {

    }

    @Override
    public void onRemoveItem(CartItem item) {

    }
}