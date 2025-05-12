package com.example.foodorderappcustomer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.RestaurantAdapter;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText searchEditText;
    private ImageButton filterButton;
    private LinearLayout filterLayout;
    private RecyclerView searchResultsRecyclerView;
    private RadioGroup priceRangeGroup;
    private RadioGroup deliveryTimeGroup;
    private RadioGroup distanceGroup;
    private Button applyFilterButton;

    private RestaurantAdapter restaurantAdapter;
    private List<Restaurant> allRestaurants;
    private List<Restaurant> filteredRestaurants;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        filterLayout = findViewById(R.id.filterLayout);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        priceRangeGroup = findViewById(R.id.priceRangeGroup);
        deliveryTimeGroup = findViewById(R.id.deliveryTimeGroup);
        distanceGroup = findViewById(R.id.distanceGroup);
        applyFilterButton = findViewById(R.id.applyFilterButton);

        // Initialize lists
        allRestaurants = new ArrayList<>();
        filteredRestaurants = new ArrayList<>();

        // Setup RecyclerView
        restaurantAdapter = new RestaurantAdapter(filteredRestaurants);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(restaurantAdapter);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Setup click listeners
        setupClickListeners();

        // Setup search functionality
        setupSearch();

        // Load restaurants
        loadRestaurants();
    }

    private void setupClickListeners() {
        filterButton.setOnClickListener(v -> {
            if (filterLayout.getVisibility() == View.VISIBLE) {
                filterLayout.setVisibility(View.GONE);
            } else {
                filterLayout.setVisibility(View.VISIBLE);
            }
        });

        applyFilterButton.setOnClickListener(v -> {
            applyFilters();
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRestaurants(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadRestaurants() {
        databaseReference.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allRestaurants.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String id = snapshot.getKey();
                    String name = snapshot.child("name").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);
                    String address = "";
                    if (snapshot.hasChild("address")) {
                        DataSnapshot addressSnapshot = snapshot.child("address");
                        String street = addressSnapshot.child("street").getValue(String.class);
                        String city = addressSnapshot.child("city").getValue(String.class);
                        String state = addressSnapshot.child("state").getValue(String.class);

                        if (street != null && city != null) {
                            address = street + ", " + city;
                            if (state != null) {
                                address += ", " + state;
                            }
                        }
                    }

                    double rating = 0.0;
                    if (snapshot.hasChild("rating")) {
                        Double ratingValue = snapshot.child("rating").getValue(Double.class);
                        if (ratingValue != null) {
                            rating = ratingValue;
                        }
                    }

                    double deliveryFee = 0.0;
                    if (snapshot.hasChild("deliveryFee")) {
                        Double deliveryFeeValue = snapshot.child("deliveryFee").getValue(Double.class);
                        if (deliveryFeeValue != null) {
                            deliveryFee = deliveryFeeValue;
                        }
                    }

                    int deliveryTime = 0;
                    if (snapshot.hasChild("averageDeliveryTime")) {
                        Integer deliveryTimeValue = snapshot.child("averageDeliveryTime").getValue(Integer.class);
                        if (deliveryTimeValue != null) {
                            deliveryTime = deliveryTimeValue;
                        }
                    }

                    Restaurant restaurant = new Restaurant(id, name, description, address, rating);
                    restaurant.setDeliveryFee(deliveryFee);
                    restaurant.setAverageDeliveryTime(deliveryTime);

                    if (snapshot.hasChild("imageUrl")) {
                        String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                        restaurant.setImageUrl(imageUrl);
                    } else {
                        restaurant.setImageResource(R.drawable.logo2);
                    }

                    allRestaurants.add(restaurant);
                }
                filteredRestaurants.clear();
                filteredRestaurants.addAll(allRestaurants);
                restaurantAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SearchActivity.this, "Lỗi khi tải nhà hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterRestaurants(String query) {
        filteredRestaurants.clear();
        if (query.isEmpty()) {
            filteredRestaurants.addAll(allRestaurants);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Restaurant restaurant : allRestaurants) {
                if (restaurant.getName().toLowerCase().contains(lowerCaseQuery) ||
                    (restaurant.getAddress() != null && restaurant.getAddress().toLowerCase().contains(lowerCaseQuery))) {
                    filteredRestaurants.add(restaurant);
                }
            }
        }
        restaurantAdapter.notifyDataSetChanged();
    }

    private void applyFilters() {
        filteredRestaurants.clear();
        List<Restaurant> tempList = new ArrayList<>(allRestaurants);

        // Apply price filter
        int selectedPriceId = priceRangeGroup.getCheckedRadioButtonId();
        if (selectedPriceId != -1) {
            if (selectedPriceId == R.id.priceRange1) {
                tempList.removeIf(restaurant -> restaurant.getDeliveryFee() >= 50000);
            } else if (selectedPriceId == R.id.priceRange2) {
                tempList.removeIf(restaurant -> 
                    restaurant.getDeliveryFee() < 50000 || restaurant.getDeliveryFee() > 100000);
            } else if (selectedPriceId == R.id.priceRange3) {
                tempList.removeIf(restaurant -> restaurant.getDeliveryFee() <= 100000);
            }
        }

        // Apply delivery time filter
        int selectedTimeId = deliveryTimeGroup.getCheckedRadioButtonId();
        if (selectedTimeId != -1) {
            if (selectedTimeId == R.id.timeRange1) {
                tempList.removeIf(restaurant -> restaurant.getAverageDeliveryTime() >= 30);
            } else if (selectedTimeId == R.id.timeRange2) {
                tempList.removeIf(restaurant -> 
                    restaurant.getAverageDeliveryTime() < 30 || restaurant.getAverageDeliveryTime() > 60);
            } else if (selectedTimeId == R.id.timeRange3) {
                tempList.removeIf(restaurant -> restaurant.getAverageDeliveryTime() <= 60);
            }
        }

        // Apply distance filter
        int selectedDistanceId = distanceGroup.getCheckedRadioButtonId();
        if (selectedDistanceId != -1) {
            // Note: This is a placeholder. You'll need to implement actual distance calculation
            // based on user's location and restaurant's location
            if (selectedDistanceId == R.id.distanceRange1) {
                // Filter for restaurants within 1km
                tempList.removeIf(restaurant -> restaurant.getDistance() > 1.0);
            } else if (selectedDistanceId == R.id.distanceRange2) {
                // Filter for restaurants between 1km and 3km
                tempList.removeIf(restaurant -> 
                    restaurant.getDistance() <= 1.0 || restaurant.getDistance() > 3.0);
            } else if (selectedDistanceId == R.id.distanceRange3) {
                // Filter for restaurants beyond 3km
                tempList.removeIf(restaurant -> restaurant.getDistance() <= 3.0);
            }
        }

        filteredRestaurants.addAll(tempList);
        restaurantAdapter.notifyDataSetChanged();
        filterLayout.setVisibility(View.GONE);
    }
} 