package com.example.foodorderappcustomer.Fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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

public class PendingFragment extends Fragment implements  OrderAdapter.OnOrderClickListener{
    private RecyclerView recyclerViewOrders;
    private OrderAdapter adapter;
    private ProgressBar progressBar;
    private TextView textError;
    private View emptyStateLayout;

    // Firebase
    private FirebaseDatabase database;
    private DatabaseReference ordersRef;
    private FirebaseAuth auth;

    public PendingFragment() {
        // Required empty public constructor
    }

    public static PendingFragment newInstance() {
        return new PendingFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("orders");
        auth = FirebaseAuth.getInstance();

        // Initialize views
        recyclerViewOrders = view.findViewById(R.id.recyclerHistory);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        // Set up RecyclerView
        setupRecyclerView();

        // Load orders from Firebase
        loadOrdersFromFirebase();
    }
    private void setupRecyclerView() {
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(new ArrayList<>(), this);
        recyclerViewOrders.setAdapter(adapter);
    }

    private void loadOrdersFromFirebase() {
        loadOrdersFromFirebase("all");
    }

    private void loadOrdersFromFirebase(String status) {
        showLoading(true);

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            showError("Vui lòng đăng nhập để xem lịch sử đơn hàng");
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
                                    if (status.equals("pending")) {
                                        orders.add(order);
                                    }
                                }
                            } catch (Exception e) {
                                // Skip this order if there's an error
                            }
                        }

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
        // Start OrderDetailActivity with the order ID
        Intent intent = new Intent(getActivity(), OrderInformationActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }
}