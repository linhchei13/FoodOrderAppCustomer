package com.example.foodorderappcustomer.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.CategoryAdapter;
import com.example.foodorderappcustomer.Adapter.PromotionAdapter;
import com.example.foodorderappcustomer.Adapter.RestaurantAdapter;
import com.example.foodorderappcustomer.CartActivity;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.SavedAddressesActivity;
import com.example.foodorderappcustomer.LocationActivity;
import com.example.foodorderappcustomer.Models.Category;
import com.example.foodorderappcustomer.Models.Promotion;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.SearchActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.foodorderappcustomer.util.OrderItemManager;

public class HomeFragment extends Fragment implements PromotionAdapter.OnPromotionClickListener {

    private RecyclerView categoryRecyclerView;
    private RecyclerView restaurantRecyclerView;
    private RecyclerView promotionsRecyclerView;
    private CategoryAdapter categoryAdapter;
    private RestaurantAdapter restaurantAdapter;
    private PromotionAdapter promotionAdapter;
    private List<Category> categoryList;
    private List<Restaurant> restaurantList;
    private List<Restaurant> nearbyRestaurantList;
    private List<Restaurant> filteredRestaurantList;
    private List<Promotion> promotionList;

    List<MenuItem> menuItems = new ArrayList<>();
    private FloatingActionButton floatingActionButton;
    private TextView searchEditText;
    private TextView welcomeTextView;
    private TextView addressTextView;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private ActivityResultLauncher<Intent> locationActivityLauncher;

    private OrderItemManager orderItemManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Initialize OrderItemManager
        orderItemManager = OrderItemManager.getInstance(requireContext());

        // Initialize views
        categoryRecyclerView = view.findViewById(R.id.categoryView);
        restaurantRecyclerView = view.findViewById(R.id.restaurantView);
        promotionsRecyclerView = view.findViewById(R.id.promotionsRecyclerView);
//        nearbyRestaurantsRecyclerView = view.findViewById(R.id.nearbyRestaurantsRecyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        welcomeTextView = view.findViewById(R.id.textView);
        addressTextView = view.findViewById(R.id.addressTextView);
        floatingActionButton = view.findViewById(R.id.floatingActionButton);

        // Initialize lists
        categoryList = new ArrayList<>();
        restaurantList = new ArrayList<>();
        nearbyRestaurantList = new ArrayList<>();
        filteredRestaurantList = new ArrayList<>();
        promotionList = new ArrayList<>();

        // Set up RecyclerViews
        setupCategoryRecyclerView();
        setupRestaurantRecyclerView();
        setupPromotionsRecyclerView();

        // Set up search functionality
        // Set up click listeners
        setupClickListeners();

        // Load data
        loadUserData();
        loadCategories();
        loadRestaurants();
        loadPromotions();

        // Update cart button visibility
        updateCartButton();

        // Initialize location activity launcher
        locationActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String selectedAddress = result.getData().getStringExtra("selected_address");
                    if (selectedAddress != null && !selectedAddress.isEmpty()) {
                        // Directly update the UI with selected address
                        addressTextView.setText(selectedAddress);
                        
                        // Store the address in SharedPreferences
                        SharedPreferences prefs = getActivity().getSharedPreferences(firebaseAuth.getUid(), Context.MODE_PRIVATE);
                        prefs.edit()
                            .putString("current_address", selectedAddress)
                            .putBoolean("has_selected_address", true)
                            .apply();
                    }
                }
            }
        );

        return view;
    }

    private void setupClickListeners() {
        floatingActionButton.setOnClickListener(v -> {
            if (!orderItemManager.isEmpty()) {
                startActivity(new Intent(getContext(), CartActivity.class));
            } else {
                Toast.makeText(getContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            }
        });
        searchEditText.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SearchActivity.class));
        });
        addressTextView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LocationActivity.class);
            locationActivityLauncher.launch(intent);
        });
//        addressTextView.setOnClickListener(v -> {
//            Intent intent = new Intent(getContext(), SavedAddressesActivity.class);
//            startActivity(intent);
//                }
//                );
    }

    private void setupCategoryRecyclerView() {
        categoryAdapter = new CategoryAdapter(categoryList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        categoryRecyclerView.setLayoutManager(layoutManager);
        categoryRecyclerView.setAdapter(categoryAdapter);
    }

    private void setupRestaurantRecyclerView() {
        restaurantAdapter = new RestaurantAdapter(restaurantList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        restaurantRecyclerView.setLayoutManager(layoutManager);
        restaurantRecyclerView.setAdapter(restaurantAdapter);
    }

    private void setupPromotionsRecyclerView() {
        promotionAdapter = new PromotionAdapter(promotionList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        promotionsRecyclerView.setLayoutManager(layoutManager);
        promotionsRecyclerView.setAdapter(promotionAdapter);
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // First check if user has manually selected an address
            SharedPreferences prefs = getActivity().getSharedPreferences(firebaseAuth.getUid(), Context.MODE_PRIVATE);
            boolean hasSelectedAddress = prefs.getBoolean("has_selected_address", false);
            String currentAddress = prefs.getString("current_address", null);

            if (hasSelectedAddress && currentAddress != null && !currentAddress.isEmpty()) {
                // Use the manually selected address
                addressTextView.setText(currentAddress);
            } else {
                // If no manually selected address, try to get the most recent address from Firebase
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
                                            addressTextView.setText(recentAddress);
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
                                addressTextView.setText("Thêm địa chỉ của bạn");
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(getContext(), "Lỗi khi tải địa chỉ gần đây", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            // Load user display name
            databaseReference.child("users")
                    .child(currentUser.getUid())
                    .child("firstName")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String displayName = dataSnapshot.getValue(String.class);
                                if (displayName != null && !displayName.isEmpty()) {
                                    welcomeTextView.setText("Xin chào, " + displayName + "!");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle error
                        }
                    });
        }
    }

    private void loadCategories() {
        databaseReference.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String id = categorySnapshot.child("id").getValue(String.class);
                    String name = categorySnapshot.child("name").getValue(String.class);
                    String url = categorySnapshot.child("imageUrl").getValue(String.class);
                    String description = categorySnapshot.child("description").getValue(String.class);

                    Category category = new Category(id, name, description, url);
                    categoryList.add(category);
                }
                // Notify adapter of data change
                if (categoryAdapter != null) {
                    categoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CategoryFragment", "Error loading categories", error.toException());
            }
        });
    }

    private void loadRestaurants() {
        databaseReference.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                restaurantList.clear();
                nearbyRestaurantList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String id = snapshot.getKey();
                    Log.d("RestaurantFragment", "Loaded restaurant ID: " + snapshot);
                    loadRestaurantReviews(id);
                    loadItems(id);

                    Restaurant restaurant = snapshot.getValue(Restaurant.class);
                    restaurant.setId(snapshot.getKey());

                    restaurantList.add(restaurant);
                }

                restaurantAdapter.updateData(restaurantList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Lỗi khi tải nhà hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadItems(String restaurantId) {

        databaseReference.child("menuItems").orderByChild("restaurantId").equalTo(restaurantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int aveprice = 0;
                        double total = 0;
                        int count = 0;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            MenuItem  menuItem = snapshot.getValue(MenuItem.class);
                            if (menuItem != null) {
                                total += menuItem.getPrice();
                                count++;
                            }
                        }
                        aveprice = (int) Math.ceil(total/count) / 1000;
                        String price = String.valueOf(aveprice);
                        databaseReference.child("restaurants").child(restaurantId)
                                .child("averagePrice").setValue(price);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Load menu", "Failed to load menu items: " + error.getMessage());
                    }
                });
    }


    private void loadRestaurantReviews(String restaurantId) {
        databaseReference.child("reviews")
                .orderByChild("restaurantId")
                .equalTo(restaurantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalRating = 0;
                        int reviewCount = 0;

                        for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                            Review review = reviewSnapshot.getValue(Review.class);
                            if (review != null) {
                                totalRating += review.getRating();
                                reviewCount++;
                            }
                        }
                        double restaurantRatingValue;

                        // Calculate average rating
                        if (reviewCount > 0) {
                            restaurantRatingValue = totalRating / reviewCount;
                        } else {
                            restaurantRatingValue = 0;
                        }
                        databaseReference.child("restaurants").child(restaurantId).child("rating").setValue(restaurantRatingValue);
                        databaseReference.child("restaurants").child(restaurantId).child("totalRatings").setValue(reviewCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void loadPromotions() {
        databaseReference.child("promotions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                promotionList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String id = snapshot.getKey();

                    Promotion promotion = snapshot.getValue(Promotion.class);
                    promotion.setId(id);
                    promotionList.add(promotion);
                }

                promotionAdapter.updateData(promotionList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Lỗi khi tải khuyến mãi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCartButton() {
        if (orderItemManager != null && !orderItemManager.isEmpty()) {
            floatingActionButton.setVisibility(View.VISIBLE);
        } else {
            floatingActionButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPromotionClick(Promotion promotion) {
        // Handle promotion click
        Toast.makeText(getContext(), "Promotion code: " + promotion.getPromoCode(), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        loadUserData();
        loadCategories();
        loadRestaurants();
        loadPromotions();
        // Update cart button visibility when returning to this fragment
        updateCartButton();
    }
}