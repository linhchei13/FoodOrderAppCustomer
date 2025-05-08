package com.example.foodorderappcustomer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.OrderItemAdapter;
import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.Models.CartItem;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {
    private TextView textOrderId, textOrderStatus, textOrderTime;
    private TextView textRestaurantName;
    private TextView textDeliveryAddress, textDeliveryNote;
    private TextView textPaymentMethod;
    private TextView textSubtotal, textDeliveryFee, textTotal;
    private RecyclerView orderItemsRecyclerView;
    private View buttonCancelOrder;
    private ImageButton backButton;

    private Order currentOrder;
    private DatabaseReference databaseReference;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

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

        // Setup RecyclerView
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        buttonCancelOrder.setOnClickListener(v -> showCancelConfirmationDialog());
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
        textOrderId.setText("Đơn hàng #" + currentOrder.getId().substring(0, 8));
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

        // Display status
        String statusText;
        int statusColor;
        switch (currentOrder.getStatus()) {
            case "pending":
                statusText = "Đang chờ xác nhận";
                statusColor = getResources().getColor(R.color.status_pending);
                buttonCancelOrder.setVisibility(View.VISIBLE);
                break;
            case "processing":
                statusText = "Đang xử lý";
                statusColor = getResources().getColor(R.color.status_processing);
                buttonCancelOrder.setVisibility(View.GONE);
                break;
            case "completed":
                statusText = "Hoàn thành";
                statusColor = getResources().getColor(R.color.status_completed);
                buttonCancelOrder.setVisibility(View.GONE);
                break;
            case "cancelled":
                statusText = "Đã hủy";
                statusColor = getResources().getColor(R.color.status_cancelled);
                buttonCancelOrder.setVisibility(View.GONE);
                break;
            default:
                statusText = currentOrder.getStatus();
                statusColor = getResources().getColor(R.color.status_default);
                buttonCancelOrder.setVisibility(View.GONE);
        }
        textOrderStatus.setText(statusText);
        textOrderStatus.setTextColor(statusColor);

        // Display order items
        List<CartItem> orderItems = currentOrder.getItems();
        if (orderItems != null && !orderItems.isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(orderItems);
            orderItemsRecyclerView.setAdapter(adapter);
        }
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
}