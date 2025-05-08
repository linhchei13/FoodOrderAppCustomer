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
import com.example.foodorderappcustomer.OrderDetailActivity;
import com.example.foodorderappcustomer.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment implements OrderAdapter.OnOrderClickListener {
    private TabLayout tabLayout;
    private RecyclerView recyclerViewOrders;
    private OrderAdapter adapter;
    private ProgressBar progressBar;
    private TextView textError;
    private View emptyStateLayout;

    // Firebase
    private FirebaseDatabase database;
    private DatabaseReference ordersRef;
    private FirebaseAuth auth;

    public HistoryFragment() {
        // Required empty public constructor
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
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
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerViewOrders = view.findViewById(R.id.recyclerHistory);
        progressBar = view.findViewById(R.id.progressBar);
        textError = view.findViewById(R.id.textError);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        // Set up TabLayout
        setupTabLayout();

        // Set up RecyclerView
        setupRecyclerView();

        // Load orders from Firebase
        loadOrdersFromFirebase();
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Handle tab selection
                String status = "";
                switch (tab.getPosition()) {
                    case 0: // Deal đã mua
                        status = "completed";
                        break;
                    case 1: // Lịch sử
                        status = "all";
                        break;
                    case 2: // Đánh giá
                        status = "rated";
                        break;
                    case 3: // Đơn nhập
                        status = "processing";
                        break;
                }
                loadOrdersFromFirebase(status);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Handle tab unselection
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Handle tab reselection
            }
        });
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
                                    if (status.equals("all") || order.getStatus().equals(status)) {
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
        Intent intent = new Intent(getActivity(), OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }
}
