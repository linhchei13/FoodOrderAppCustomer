package com.example.foodorderappcustomer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.OrderAdapter;
import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.OrderInformationActivity;
import com.example.foodorderappcustomer.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OrderListFragment extends Fragment implements OrderAdapter.OnOrderClickListener {
    private static final String ARG_STATUS = "status";
    private String orderStatus;
    private RecyclerView recyclerViewOrders;
    private OrderAdapter adapter;
    private ProgressBar progressBar;
    private TextView textError;
    private View emptyStateLayout;

    // Firebase
    private DatabaseReference ordersRef;
    private FirebaseAuth auth;

    public static OrderListFragment newInstance(String status) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderStatus = getArguments().getString(ARG_STATUS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);

        // Initialize Firebase
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        auth = FirebaseAuth.getInstance();

        // Initialize views
        recyclerViewOrders = view.findViewById(R.id.recyclerOrders);
        progressBar = view.findViewById(R.id.progressBar);
        textError = view.findViewById(R.id.textError);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        // Setup RecyclerView
        setupRecyclerView();

        // Load orders
        loadOrders();

        return view;
    }

    private void setupRecyclerView() {
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(new ArrayList<>(), this);
        recyclerViewOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        showLoading(true);

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            showError("Vui lòng đăng nhập để xem đơn hàng");
            showLoading(false);
            return;
        }

        ordersRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Order> orders = new ArrayList<>();

                        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);
                                if (order != null) {
                                    // Filter orders based on status
                                    if (orderStatus.equals("all") || order.getStatus().equals(orderStatus)) {
                                        orders.add(order);
                                    }
                                }
                            } catch (Exception e) {
                                // Skip this order if there's an error
                            }
                        }

                        // Sort orders by time in descending order (newest first)
                        orders.sort((o1, o2) -> o2.getOrderTime().compareTo(o1.getOrderTime()));

                        // Update UI
                        showLoading(false);
                        if (orders.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                            adapter.updateData(orders);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        showLoading(false);
                        showError("Không thể tải dữ liệu: " + databaseError.getMessage());
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String errorMessage) {
        if (textError != null) {
            textError.setText(errorMessage);
            textError.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(getActivity(), OrderInformationActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }
} 