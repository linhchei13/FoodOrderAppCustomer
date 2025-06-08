package com.example.foodorderappcustomer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.RestaurantAdapter;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.RestaurantMenuActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {
    private static final String TAG = "FavoriteFragment";

    private RecyclerView favoriteRecyclerView;
    private TextView emptyStateTextView;
    private ProgressBar progressBar;
    private RestaurantAdapter restaurantAdapter;
    private List<Restaurant> favoriteRestaurants;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        favoriteRestaurants = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);
        
        // Initialize views
        favoriteRecyclerView = view.findViewById(R.id.favoriteRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        progressBar = view.findViewById(R.id.progressBar);

        // Setup RecyclerView
        setupRecyclerView();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadFavoriteRestaurants();
    }

    private void setupRecyclerView() {
        restaurantAdapter = new RestaurantAdapter(favoriteRestaurants);
        favoriteRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        favoriteRecyclerView.setAdapter(restaurantAdapter);
    }

    private void loadFavoriteRestaurants() {
        if (mAuth.getCurrentUser() == null) {
            showEmptyState("Vui lòng đăng nhập để xem nhà hàng yêu thích");
            return;
        }

        showLoading(true);
        String userId = mAuth.getCurrentUser().getUid();

        databaseReference.child("users").child(userId).child("favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favoriteRestaurants.clear();
                        
                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            showEmptyState("Bạn chưa có nhà hàng yêu thích nào");
                            showLoading(false);
                            return;
                        }

                        // Get all favorite restaurant IDs
                        List<String> favoriteIds = new ArrayList<>();
                        for (DataSnapshot favoriteSnapshot : snapshot.getChildren()) {
                            String restaurantId = favoriteSnapshot.getKey();
                            if (restaurantId != null) {
                                favoriteIds.add(restaurantId);
                            }
                        }

                        if (favoriteIds.isEmpty()) {
                            showEmptyState("Bạn chưa có nhà hàng yêu thích nào");
                            showLoading(false);
                            return;
                        }

                        // Load restaurant details for each favorite
                        loadRestaurantDetails(favoriteIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading favorites: " + error.getMessage());
                        showEmptyState("Có lỗi xảy ra khi tải danh sách yêu thích");
                        showLoading(false);
                    }
                });
    }

    private void loadRestaurantDetails(List<String> restaurantIds) {
        int totalRestaurants = restaurantIds.size();
        final int[] loadedCount = {0};

        for (String restaurantId : restaurantIds) {
            databaseReference.child("restaurants").child(restaurantId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Restaurant restaurant = snapshot.getValue(Restaurant.class);
                                if (restaurant != null) {
                                    restaurant.setId(snapshot.getKey());
                                    favoriteRestaurants.add(restaurant);
                                }
                            }

                            loadedCount[0]++;
                            if (loadedCount[0] == totalRestaurants) {
                                // All restaurants loaded
                                showLoading(false);
                                if (favoriteRestaurants.isEmpty()) {
                                    showEmptyState("Bạn chưa có nhà hàng yêu thích nào");
                                } else {
                                    restaurantAdapter.notifyDataSetChanged();
                                    emptyStateTextView.setVisibility(View.GONE);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error loading restaurant details: " + error.getMessage());
                            loadedCount[0]++;
                            if (loadedCount[0] == totalRestaurants) {
                                showLoading(false);
                                if (favoriteRestaurants.isEmpty()) {
                                    showEmptyState("Có lỗi xảy ra khi tải thông tin nhà hàng");
                                } else {
                                    restaurantAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        favoriteRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(String message) {
        emptyStateTextView.setText(message);
        emptyStateTextView.setVisibility(View.VISIBLE);
        favoriteRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }
} 