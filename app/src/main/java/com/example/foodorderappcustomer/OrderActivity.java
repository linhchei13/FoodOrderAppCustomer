package com.example.foodorderappcustomer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.CartItemAdapter;
import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.util.CartManager;
import com.example.foodorderappcustomer.util.PaymentService;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class OrderActivity extends AppCompatActivity implements CartItemAdapter.CartItemListener, CartManager.OnCartUpdateListener {

    private static final double DEFAULT_DELIVERY_FEE = 15000;

    // UI Components
    private RecyclerView cartItemsRecyclerView;
    private TextView emptyCartText;
    private CardView deliveryCard, paymentCard, orderSummaryCard;
    private TextView subtotalTextView, deliveryFeeTextView, totalTextView;
    private EditText addressEditText, noteEditText;
    private RadioGroup paymentMethodRadioGroup;
    private ExtendedFloatingActionButton checkoutButton;
    private Toolbar toolbar;
    private ImageButton backButton;

    // Data
    private CartManager cartManager;
    private CartItemAdapter cartItemAdapter;
    private NumberFormat currencyFormat;
    private double subtotal;
    private double deliveryFee = DEFAULT_DELIVERY_FEE;
    private double total;
    private String restaurantId;
    private String restaurantName;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Initialize currency format
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize Cart
        cartManager = CartManager.getInstance(this);
        cartManager.setOnCartUpdateListener(this);

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

        // Update cart items
        updateCartItems();
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
        checkoutButton.setOnClickListener(v -> placeOrder());
    }

    private void updateCartItems() {
        List<CartItem> cartItems = cartManager.getCartItems();
        emptyCartText.setVisibility(View.GONE);
        cartItemsRecyclerView.setVisibility(View.VISIBLE);
        deliveryCard.setVisibility(View.VISIBLE);
        paymentCard.setVisibility(View.VISIBLE);
        orderSummaryCard.setVisibility(View.VISIBLE);
        checkoutButton.setVisibility(View.VISIBLE);
        cartItemAdapter.setCartItems(cartItems);

        // Update totals
        updateTotals();
    }

    private void updateTotals() {
        subtotal = cartManager.getCartTotal();
        total = subtotal + deliveryFee;

        // Format and display amounts
        String formattedSubtotal = currencyFormat.format(subtotal).replace("₫", "đ");
        String formattedDeliveryFee = currencyFormat.format(deliveryFee).replace("₫", "đ");
        String formattedTotal = currencyFormat.format(total).replace("₫", "đ");

        subtotalTextView.setText(formattedSubtotal);
        deliveryFeeTextView.setText(formattedDeliveryFee);
        totalTextView.setText(formattedTotal);
    }

    private void placeOrder() {
        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showLoginPrompt();
            return;
        }

        // Validate address
        String address = addressEditText.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            addressEditText.setError("Vui lòng nhập địa chỉ giao hàng");
            return;
        }

        // Get selected payment method
        int selectedId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selectedId);
        String paymentMethod = radioButton.getText().toString();

        // Get note
        String note = noteEditText.getText().toString().trim();

        // Create order
        try {
            // If restaurant info is not available, try to get it from the first cart item
            if (TextUtils.isEmpty(restaurantId) && !cartManager.getCartItems().isEmpty()) {
                CartItem firstItem = cartManager.getCartItems().get(0);
                restaurantId = firstItem.getRestaurantId();
            }

            // Default restaurant name if not available
            if (TextUtils.isEmpty(restaurantName)) {
                restaurantName = "Nhà hàng";
            }

            // Create order
            Order order = new Order();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String randomNum = String.format("%04d", (int)(Math.random() * 10000));
            order.setId(timestamp.substring(timestamp.length() - 6) + randomNum);
            order.setUserId(currentUser.getUid());
            order.setRestaurantId(restaurantId);
            order.setRestaurantName(restaurantName);
            order.setAddress(address);
            order.setNote(note);
            order.setPaymentMethod(paymentMethod);
            order.setSubtotal(subtotal);
            order.setDeliveryFee(deliveryFee);
            order.setTotal(total);
            order.setStatus("pending");
            order.setOrderTime(new Date());
            
            // Add items to order
            List<CartItem> cartItems = cartManager.getCartItems();
            for (CartItem item : cartItems) {
                order.addItem(item);
            }

            // Show loading dialog
            AlertDialog loadingDialog = new AlertDialog.Builder(this)
                    .setTitle("Đang xử lý đơn hàng")
                    .setMessage("Vui lòng đợi...")
                    .setCancelable(false)
                    .create();
            loadingDialog.show();

            // Save order to Firebase
            databaseReference.child("orders").child(order.getId()).setValue(order)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    // Process payment based on selected method
                    processPayment(order, paymentMethod);
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processPayment(Order order, String paymentMethod) {
        switch (paymentMethod) {
            case "Ví điện tử":
                processVNPayPayment(order);
                break;
            case "Thẻ quốc tế":
                showCardPaymentDialog(order);
                break;
            case "Thanh toán khi nhận hàng":
                // For cash payment, just show success dialog
                cartManager.clearCart();
                showOrderSuccessDialog(order);
                break;
            default:
                Toast.makeText(this, "Phương thức thanh toán không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private void processVNPayPayment(Order order) {
        PaymentService.processVNPayPayment(this, order, new PaymentService.PaymentCallback() {
            @Override
            public void onPaymentSuccess(String transactionId) {
                // Clear cart and show success dialog
                cartManager.clearCart();
                showOrderSuccessDialog(order);
            }

            @Override
            public void onPaymentFailure(String errorMessage) {
                Toast.makeText(OrderActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCardPaymentDialog(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_card_payment, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Get references to dialog views
        EditText cardNumberInput = dialogView.findViewById(R.id.cardNumberInput);
        EditText expiryDateInput = dialogView.findViewById(R.id.expiryDateInput);
        EditText cvvInput = dialogView.findViewById(R.id.cvvInput);
        EditText cardholderNameInput = dialogView.findViewById(R.id.cardholderNameInput);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        // Set up click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            String cardNumber = cardNumberInput.getText().toString().trim();
            String expiryDate = expiryDateInput.getText().toString().trim();
            String cvv = cvvInput.getText().toString().trim();
            String cardholderName = cardholderNameInput.getText().toString().trim();

            // Validate inputs
            if (cardNumber.isEmpty() || expiryDate.isEmpty() || cvv.isEmpty() || cardholderName.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Process card payment
            PaymentService.processCardPayment(this, order, cardNumber, expiryDate, cvv, 
                new PaymentService.PaymentCallback() {
                    @Override
                    public void onPaymentSuccess(String transactionId) {
                        dialog.dismiss();
                        cartManager.clearCart();
                        showOrderSuccessDialog(order);
                    }

                    @Override
                    public void onPaymentFailure(String errorMessage) {
                        Toast.makeText(OrderActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
        });

        dialog.show();
    }

    private void showOrderSuccessDialog(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công!")
                .setMessage("Đơn hàng của bạn đã được đặt thành công. Mã đơn hàng: " + order.getId())
                .setPositiveButton("OK", (dialog, which) -> {
                    // Navigate to home screen
                    Intent intent = new Intent(OrderActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
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

    @Override
    public void onQuantityChanged(CartItem cartItem, int newQuantity) {
        // Find the corresponding CartItem and update its quantity
        List<CartItem> cartItems = cartManager.getCartItems();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            if (item.getItemId().equals(cartItem.getItemId())) {
                item.setQuantity(newQuantity);
                cartManager.updateItem(i, item);
                break;
            }
        }
    }

    @Override
    public void onRemoveItem(CartItem cartItem) {
        // Find the corresponding CartItem and remove it
        List<CartItem> cartItems = cartManager.getCartItems();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            if (item.getItemId().equals(cartItem.getItemId())) {
                cartManager.removeItem(i);
                Toast.makeText(this, "Đã xóa món khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    @Override
    public void onCartUpdated(List<CartItem> cartItems, double total) {
        // Update the UI with new cart data
        cartItemAdapter.setCartItems(cartItems);
        updateTotals();
        
        // Update visibility of UI elements based on whether cart is empty
        if (cartItems.isEmpty()) {
            emptyCartText.setVisibility(View.VISIBLE);
            cartItemsRecyclerView.setVisibility(View.GONE);
            deliveryCard.setVisibility(View.GONE);
            paymentCard.setVisibility(View.GONE);
            orderSummaryCard.setVisibility(View.GONE);
            checkoutButton.setVisibility(View.GONE);
        } else {
            emptyCartText.setVisibility(View.GONE);
            cartItemsRecyclerView.setVisibility(View.VISIBLE);
            deliveryCard.setVisibility(View.VISIBLE);
            paymentCard.setVisibility(View.VISIBLE);
            orderSummaryCard.setVisibility(View.VISIBLE);
            checkoutButton.setVisibility(View.VISIBLE);
        }
    }
} 