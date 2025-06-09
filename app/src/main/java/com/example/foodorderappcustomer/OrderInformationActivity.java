package com.example.foodorderappcustomer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.OrderItemAdapter;
import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.util.SalesManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;

public class OrderInformationActivity extends AppCompatActivity {
    private TextView textOrderId, textOrderStatus, textOrderTime;
    private TextView textRestaurantName;
    private TextView textDeliveryAddress, textDeliveryNote;
    private TextView textPaymentMethod;
    private TextView textSubtotal, textDeliveryFee, textTotal;
    private RecyclerView orderItemsRecyclerView;
    private View buttonCancelOrder;
    private MaterialButton buttonMarkAsCompleted;
    private MaterialButton buttonReview;
    private ImageButton backButton;

    private Order currentOrder;
    private DatabaseReference databaseReference;
    private SimpleDateFormat dateFormat;
    private ActivityResultLauncher<Intent> reviewLauncher;

    private static final int REQUEST_REVIEW = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        // Initialize ActivityResultLauncher
        reviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK || result.getResultCode() == REQUEST_REVIEW) {
                    // Review was submitted successfully
                    buttonReview.setEnabled(false);
                    buttonReview.setText("Đã đánh giá");
                }
            }
        );

        // Initialize views
        initViews();
        setupClickListeners();

        // Get order data from intent
        String orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId != null) {
            loadOrderData(orderId);
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        backButton = findViewById(R.id.backButton);
        textOrderId = findViewById(R.id.textOrderId);
        textOrderStatus = findViewById(R.id.textOrderStatus);
        textOrderTime = findViewById(R.id.textOrderTime);
        textRestaurantName = findViewById(R.id.textRestaurantName);
        textDeliveryAddress = findViewById(R.id.textDeliveryAddress);
        textDeliveryNote = findViewById(R.id.textDeliveryNote);
        textPaymentMethod = findViewById(R.id.textPaymentMethod);
        textSubtotal = findViewById(R.id.textSubtotal);
        textDeliveryFee = findViewById(R.id.textDeliveryFee);
        textTotal = findViewById(R.id.textTotal);
        orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView);
        buttonCancelOrder = findViewById(R.id.buttonCancelOrder);
        buttonMarkAsCompleted = findViewById(R.id.buttonMarkAsCompleted);
        buttonReview = findViewById(R.id.buttonReview);

        // Setup RecyclerView
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

//        setupReviewImageClickListeners();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        buttonCancelOrder.setOnClickListener(v -> showCancelConfirmationDialog());
        
        buttonMarkAsCompleted.setOnClickListener(v -> showMarkAsCompletedConfirmationDialog());

        buttonReview.setOnClickListener(v -> {
            Intent intent = ReviewOrderActivity.newIntent(this, currentOrder.getId(), currentOrder.getRestaurantId());
            reviewLauncher.launch(intent);
        });
    }

    private void loadOrderData(String orderId) {
        databaseReference.child("orders").child(orderId).get()
            .addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    currentOrder = dataSnapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        displayOrderData();
                    }
                } else {
                    Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void displayOrderData() {
        // Display basic order info
        textOrderId.setText("Đơn hàng #" + currentOrder.getId());
        textOrderTime.setText(dateFormat.format(currentOrder.getOrderTime()));
        textRestaurantName.setText(currentOrder.getRestaurantName());
        textDeliveryAddress.setText(currentOrder.getAddress());
        textDeliveryNote.setText("Ghi chú: " + (currentOrder.getNote() != null && !currentOrder.getNote().isEmpty() 
            ? currentOrder.getNote() : "Không có"));
        textPaymentMethod.setText(currentOrder.getPaymentMethod());

        // Display prices
        textSubtotal.setText(String.format("%,.0f đ", currentOrder.getSubtotal()));
        textDeliveryFee.setText(String.format("%,.0f đ", currentOrder.getDeliveryFee()));
        textTotal.setText(String.format("%,.0f đ", currentOrder.getTotal()));

        // Display status and handle buttons visibility
        String statusText;
        int statusColor;
        switch (currentOrder.getStatus()) {
            case "pending":
                statusText = "Chờ xác nhận";
                statusColor = getResources().getColor(R.color.status_pending);
                buttonCancelOrder.setVisibility(View.VISIBLE);
                buttonMarkAsCompleted.setVisibility(View.GONE);
                buttonReview.setVisibility(View.GONE);
                break;
            case "contacted":
                statusText = "Đang giao";
                statusColor = getResources().getColor(R.color.status_processing);
                buttonCancelOrder.setVisibility(View.GONE);
                buttonMarkAsCompleted.setVisibility(View.VISIBLE);
                buttonReview.setVisibility(View.GONE);
                break;
            case "verified":
                statusText = "Đang giao";
                statusColor = getResources().getColor(R.color.status_processing);
                buttonCancelOrder.setVisibility(View.GONE);
                buttonMarkAsCompleted.setVisibility(View.VISIBLE);
                buttonReview.setVisibility(View.GONE);
                break;
            case "completed":
                statusText = "Hoàn thành";
                statusColor = getResources().getColor(R.color.status_completed);
                buttonCancelOrder.setVisibility(View.GONE);
                buttonMarkAsCompleted.setVisibility(View.GONE);
                buttonReview.setVisibility(View.VISIBLE);
                checkExistingReview();
                break;
            case "cancelled":
                statusText = "Đã hủy";
                statusColor = getResources().getColor(R.color.status_cancelled);
                buttonCancelOrder.setVisibility(View.GONE);
                buttonMarkAsCompleted.setVisibility(View.GONE);
                buttonReview.setVisibility(View.GONE);
                break;
            case "canceled":
                statusText = "Đã hủy";
                statusColor = getResources().getColor(R.color.status_cancelled);
                buttonCancelOrder.setVisibility(View.GONE);
                buttonMarkAsCompleted.setVisibility(View.GONE);
                buttonReview.setVisibility(View.GONE);
                break;
            default:
                statusText = currentOrder.getStatus();
                statusColor = getResources().getColor(R.color.status_default);
                buttonCancelOrder.setVisibility(View.GONE);
                buttonMarkAsCompleted.setVisibility(View.GONE);
                buttonReview.setVisibility(View.GONE);
        }
        textOrderStatus.setText(statusText);
        textOrderStatus.setTextColor(statusColor);

        // Display order items
        List<OrderItem> orderItems = currentOrder.getItems();
        if (orderItems != null && !orderItems.isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(orderItems);
            orderItemsRecyclerView.setAdapter(adapter);
        }
    }

    private void checkExistingReview() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference.child("reviews")
            .child(currentOrder.getId())
            .get()
            .addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    // Review exists, disable review button
                    buttonReview.setEnabled(false);
                    buttonReview.setText("Đã đánh giá");
                } else {
                    buttonReview.setEnabled(true);
                    buttonReview.setText("Đánh giá đơn hàng");
                }
            });
    }
    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
            .setPositiveButton("Hủy đơn", (dialog, which) -> cancelOrder())
            .setNegativeButton("Không", null)
            .show();
    }

    private void cancelOrder() {
        if (currentOrder != null) {
            // Show loading dialog
            AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Đang xử lý")
                .setMessage("Vui lòng đợi...")
                .setCancelable(false)
                .create();
            loadingDialog.show();

            // Update order status in Firebase
            databaseReference.child("orders").child(currentOrder.getId())
                .child("status").setValue("cancelled")
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void showMarkAsCompletedConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Xác nhận đã nhận hàng")
            .setMessage("Bạn có chắc chắn đã nhận được hàng?")
            .setPositiveButton("Xác nhận", (dialog, which) -> markOrderAsCompleted())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void markOrderAsCompleted() {
        if (currentOrder != null) {
            // Show loading dialog
            AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Đang xử lý")
                .setMessage("Vui lòng đợi...")
                .setCancelable(false)
                .create();
            loadingDialog.show();
            Date dateCompleted = new Date(System.currentTimeMillis());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String formattedDate = dateFormat.format(dateCompleted);

            // Update order status in Firebase
            databaseReference.child("orders").child(currentOrder.getId())
                .child("status").setValue("completed")
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Đã cập nhật trạng thái đơn hàng", Toast.LENGTH_SHORT).show();
                    // Refresh the activity to show review section
                    recreate();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            SalesManager salesManager = SalesManager.getInstance();
            salesManager.updateSalesForOrder(currentOrder.getId());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_REVIEW && resultCode == RESULT_OK) {
            // Review was submitted successfully
            buttonReview.setEnabled(false);
            buttonReview.setText("Đã đánh giá");
        }
    }
}