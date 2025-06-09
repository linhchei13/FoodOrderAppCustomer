package com.example.foodorderappcustomer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.example.foodorderappcustomer.Adapter.RestaurantSearchAdapter;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.foodorderappcustomer.API.GeoCodingApi;
import com.example.foodorderappcustomer.API.GeoResponse;
import com.example.foodorderappcustomer.API.DistanceAPI;
import com.example.foodorderappcustomer.API.DistanceResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {
    private static final int REQUEST_FOOD_DETAIL = 1;
    private RadioGroup sortGroup;
    private RadioButton rbDefault, rbCheapest, rbBestSeller, rbNearest, rbBestRating;
    private Button btn35Rating, btn40Rating, btn45Rating;
    private EditText etMinPrice, etMaxPrice;
    private Button btnReset, btnConfirm;
    private EditText etSearchQuery;
    private ImageButton btnBack, btnClearSearch, btnFilter;
    private LinearLayout filterSection;
    private boolean isFilterVisible = false;

    // Results components
    private RecyclerView recyclerView;
    private RestaurantSearchAdapter adapter;
    private List<Restaurant> restaurantList;
    private List<Restaurant> filteredList;
    private Map<String, List<MenuItem>> restaurantMenuItems;

    // Filter parameters
    private double minRating = 0;
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;

    private String userLatitude;
    private String userLongitude;
    private FirebaseAuth firebaseAuth;
    private Map<String, Restaurant> restaurantMap = new HashMap<>();
    private boolean isCalculatingDistances = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_enhanced);

        firebaseAuth = FirebaseAuth.getInstance();
        initViews();
        setupData();
        setupListeners();
        setupRecyclerView();
        loadUserLocation();
    }

    private void initViews() {
        // Sort group
        sortGroup = findViewById(R.id.sortGroup);
        rbDefault = findViewById(R.id.rbDefault);
        rbCheapest = findViewById(R.id.rbCheapest);
        rbBestSeller = findViewById(R.id.rbBestSeller);
        rbNearest = findViewById(R.id.rbNearest);
        rbBestRating = findViewById(R.id.rbBestRating);

        // Filter button and section
        btnFilter = findViewById(R.id.btnFilter);
        filterSection = findViewById(R.id.filterSection);

        // Rating buttons
        btn35Rating = findViewById(R.id.btn35Rating);
        btn40Rating = findViewById(R.id.btn40Rating);
        btn45Rating = findViewById(R.id.btn45Rating);

        // Price range
        etMinPrice = findViewById(R.id.etMinPrice);
        etMaxPrice = findViewById(R.id.etMaxPrice);

        // Action buttons
        btnReset = findViewById(R.id.btnReset);
        btnConfirm = findViewById(R.id.btnConfirm);

        // Search
        etSearchQuery = findViewById(R.id.etSearchQuery);
        btnBack = findViewById(R.id.btnBack);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        // Results view
        recyclerView = findViewById(R.id.recyclerView);
    }

    private void toggleFilterSection() {
        btnFilter.setBackgroundResource(isFilterVisible ? R.drawable.button_outline : R.drawable.button_selected);
        isFilterVisible = !isFilterVisible;
        filterSection.setVisibility(isFilterVisible ? View.VISIBLE : View.GONE);

        // Add animation
        if (isFilterVisible) {
            filterSection.setAlpha(0f);
            filterSection.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    private void setupData() {
        restaurantList = new ArrayList<>();
        filteredList = new ArrayList<>();
        restaurantMenuItems = new HashMap<>();

        // Show loading indicator
        recyclerView.setVisibility(View.GONE);
        // Load restaurants from Firebase
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("SearchActivity", "Loading restaurants data");
                restaurantList.clear();
                restaurantMap.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Restaurant restaurant = snapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        restaurant.setId(snapshot.getKey());
                        restaurantList.add(restaurant);
                        restaurantMap.put(restaurant.getId(), restaurant);
                        Log.d("SearchActivity", "Loaded restaurant: " + restaurant.getName());
                    }
                }

                // Initialize filtered list with all restaurants
                filteredList.clear();
                filteredList.addAll(restaurantList);
                
                // Load menu items for each restaurant
                for (Restaurant restaurant : restaurantList) {
                    loadMenuItemsForRestaurant(restaurant.getId());
                }

                // Calculate distances if we have user location
                if (userLatitude != null && userLongitude != null) {
                    calculateDistancesForRestaurants();
                }

                // Show results
                recyclerView.setVisibility(View.VISIBLE);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SearchActivity", "Error loading restaurants: " + databaseError.getMessage());
                Toast.makeText(SearchActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMenuItemsForRestaurant(String restaurantId) {
        Log.d("SearchActivity", "Loading menu items for restaurant: " + restaurantId);
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference()
                .child("menuItems");

        menuRef.orderByChild("restaurantId").equalTo(restaurantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<MenuItem> menuItems = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            MenuItem menuItem = snapshot.getValue(MenuItem.class);
                            if (menuItem != null) {
                                menuItem.setId(snapshot.getKey());
                                menuItems.add(menuItem);
                                Log.d("SearchActivity", "Loaded menu item: " + menuItem.getName() + " for restaurant: " + restaurantId);
                            }
                        }

                        restaurantMenuItems.put(restaurantId, menuItems);
                        Log.d("SearchActivity", "Total menu items loaded for restaurant " + restaurantId + ": " + menuItems.size());

                        // Update adapter with menu items
                        if (adapter != null) {
                            adapter.updateMenuItems(restaurantMenuItems);
                            // If there's an active search, perform it again with the new menu items
                            String currentQuery = etSearchQuery.getText().toString().trim();
                            if (!currentQuery.isEmpty()) {
                                performSearch();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("SearchActivity", "Error loading menu items for restaurant " + restaurantId + ": " + databaseError.getMessage());
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new RestaurantSearchAdapter(filteredList, restaurantMenuItems, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Add item decoration for spacing
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        
        // Initially hide the RecyclerView until data is loaded
        recyclerView.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Search functionality with debounce
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            private android.os.Handler handler = new android.os.Handler();
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button based on text
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> performSearch();
                handler.postDelayed(searchRunnable, 300); // 300ms debounce
            }
        });

        // Clear search field
        btnClearSearch.setOnClickListener(v -> {
            etSearchQuery.setText("");
            performSearch();
            btnClearSearch.setVisibility(View.GONE);
        });

        // Handle search action from keyboard
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchQuery.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch();
                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSearchQuery.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Rating filter buttons
        btn35Rating.setOnClickListener(v -> {
            minRating = 3.5;
            resetRatingButtonsStyle();
            btn35Rating.setBackgroundResource(R.drawable.button_selected);
            applyFilters();
        });

        btn40Rating.setOnClickListener(v -> {
            minRating = 4.0;
            resetRatingButtonsStyle();
            btn40Rating.setBackgroundResource(R.drawable.button_selected);
            applyFilters();
        });

        btn45Rating.setOnClickListener(v -> {
            minRating = 4.5;
            resetRatingButtonsStyle();
            btn45Rating.setBackgroundResource(R.drawable.button_selected);
            applyFilters();
        });

        // Reset button
        btnReset.setOnClickListener(v -> resetFilters());

        // Confirm button
        btnConfirm.setOnClickListener(v -> {
            applyFilters();
            toggleFilterSection(); // Hide filter section after applying
        });

        // Sort group
        sortGroup.setOnCheckedChangeListener((group, checkedId) -> applySort());

        // Filter button
        btnFilter.setOnClickListener(v -> toggleFilterSection());
    }

    private void resetRatingButtonsStyle() {
        if (btn35Rating != null) btn35Rating.setBackgroundResource(R.drawable.button_outline_purple);
        if (btn40Rating != null) btn40Rating.setBackgroundResource(R.drawable.button_outline_purple);
        if (btn45Rating != null) btn45Rating.setBackgroundResource(R.drawable.button_outline_purple);
    }

    private void performSearch() {
        String query = etSearchQuery.getText().toString().toLowerCase().trim();
        Log.d("SearchActivity", "Performing search with query: " + query);
        
        filteredList.clear();

        if (query.isEmpty()) {
            Log.d("SearchActivity", "Empty query, showing all restaurants");
            filteredList.addAll(restaurantList);
        } else {
            // Use a Map to store restaurants and their matching menu items
            Map<String, Restaurant> matchedRestaurants = new HashMap<>();
            Map<String, List<MenuItem>> matchingMenuItems = new HashMap<>();

            for (Restaurant restaurant : restaurantList) {
                boolean restaurantMatch = false;
                List<MenuItem> restaurantMatchingItems = new ArrayList<>();

                // Search in restaurant name and description
                if (restaurant.getName().toLowerCase().contains(query)) {
                    restaurantMatch = true;
                    Log.d("SearchActivity", "Restaurant match found: " + restaurant.getName());
                }

                // Search in menu items
                if (restaurantMenuItems.containsKey(restaurant.getId())) {
                    List<MenuItem> menuItems = restaurantMenuItems.get(restaurant.getId());
                    for (MenuItem item : menuItems) {
                        if (item.getName().toLowerCase().contains(query)) {
                            restaurantMatch = true;
                            restaurantMatchingItems.add(item);
                            Log.d("SearchActivity", "Menu item match found: " + item.getName() + " in restaurant: " + restaurant.getName());
                        }
                    }
                }

                // If restaurant matches or has matching menu items, add it to results
                if (restaurantMatch || !restaurantMatchingItems.isEmpty()) {
                    matchedRestaurants.put(restaurant.getId(), restaurant);
                    if (!restaurantMatchingItems.isEmpty()) {
                        matchingMenuItems.put(restaurant.getId(), restaurantMatchingItems);
                    }
                }
            }

            Log.d("SearchActivity", "Search results - Restaurants found: " + matchedRestaurants.size() + 
                    ", Restaurants with matching menu items: " + matchingMenuItems.size());

            // Add all matched restaurants to filteredList
            filteredList.addAll(matchedRestaurants.values());

            // Update adapter with matching menu items
            if (adapter != null) {
                adapter.updateMatchingMenuItems(matchingMenuItems);
            }
        }

        // Apply any active filters
        applyFilters();
        
        // Update UI
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        // Show message if no results
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy kết quả phù hợp", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetFilters() {
        // Reset sort options
        if (rbDefault != null) rbDefault.setChecked(true);

        // Reset rating
        minRating = 0;
        resetRatingButtonsStyle();

        // Reset price range
        minPrice = 0;
        maxPrice = Double.MAX_VALUE;
        if (etMinPrice != null) etMinPrice.setText("");
        if (etMaxPrice != null) etMaxPrice.setText("");

        // Clear search and reset list
        if (etSearchQuery != null) etSearchQuery.setText("");
        filteredList.clear();
        filteredList.addAll(restaurantList);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void applyFilters() {
        // Get values from min/max price fields if provided
        try {
            String minPriceText = etMinPrice != null ? etMinPrice.getText().toString() : "";
            String maxPriceText = etMaxPrice != null ? etMaxPrice.getText().toString() : "";

            if (!minPriceText.isEmpty()) {
                minPrice = Double.parseDouble(minPriceText);
            }

            if (!maxPriceText.isEmpty()) {
                maxPrice = Double.parseDouble(maxPriceText);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Vui lòng nhập giá hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Apply search first
        String query = etSearchQuery != null ? etSearchQuery.getText().toString().toLowerCase().trim() : "";
        Map<String, Restaurant> searchResults = new HashMap<>();
        Map<String, List<MenuItem>> matchingMenuItems = new HashMap<>();

        if (query.isEmpty()) {
            for (Restaurant restaurant : restaurantList) {
                searchResults.put(restaurant.getId(), restaurant);
            }
        } else {
            for (Restaurant restaurant : restaurantList) {
                boolean restaurantMatch = false;
                List<MenuItem> restaurantMatchingItems = new ArrayList<>();

                // Search in restaurant name and description
                if (restaurant.getName().toLowerCase().contains(query)) {
                    restaurantMatch = true;
                }

                // Search in menu items
                if (restaurantMenuItems.containsKey(restaurant.getId())) {
                    List<MenuItem> menuItems = restaurantMenuItems.get(restaurant.getId());
                    for (MenuItem item : menuItems) {
                        if (item.getName().toLowerCase().contains(query)) {
                            restaurantMatch = true;
                            restaurantMatchingItems.add(item);
                        }
                    }
                }

                // If restaurant matches or has matching menu items, add it to results
                if (restaurantMatch || !restaurantMatchingItems.isEmpty()) {
                    searchResults.put(restaurant.getId(), restaurant);
                    if (!restaurantMatchingItems.isEmpty()) {
                        matchingMenuItems.put(restaurant.getId(), restaurantMatchingItems);
                    }
                }
            }
        }

        // Apply filters to search results
        filteredList.clear();

        for (Restaurant restaurant : searchResults.values()) {
            // Apply rating filter
            if (restaurant.getRating() >= minRating) {
                // Apply price filter
                boolean priceMatch = true;
                if (matchingMenuItems.containsKey(restaurant.getId())) {
                    // Check if any matching menu item is within price range
                    priceMatch = false;
                    for (MenuItem item : matchingMenuItems.get(restaurant.getId())) {
                        try {
                            double price = item.getPrice();
                            if (price >= minPrice && price <= maxPrice) {
                                priceMatch = true;
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid prices
                        }
                    }
                } else {
                    // If no matching menu items, check restaurant's average price
                    try {
                        String priceStr = restaurant.getAveragePrice();
                        if (priceStr != null && !priceStr.isEmpty()) {
                            double price = Double.valueOf(priceStr) * 1000;
                            priceMatch = price >= minPrice && price <= maxPrice;
                        }
                    } catch (NumberFormatException e) {
                        // If price parsing fails, include in results
                        priceMatch = true;
                    }
                }

                if (priceMatch) {
                    filteredList.add(restaurant);
                }
            }
        }

        // Update adapter with matching menu items
        if (adapter != null) {
            adapter.updateMatchingMenuItems(matchingMenuItems);
        }

        // Apply sorting
        applySort();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void applySort() {
        if (sortGroup == null) return;

        int checkedId = sortGroup.getCheckedRadioButtonId();

        if (checkedId == R.id.rbNearest) {
            // Sort by distance
            Collections.sort(filteredList, (r1, r2) -> {
                double dist1 = r1.getDistance();
                double dist2 = r2.getDistance();
                // If distance is not calculated (0), put it at the end
                if (dist1 == 0) return 1;
                if (dist2 == 0) return -1;
                return Double.compare(dist1, dist2);
            });
        } else if (checkedId == R.id.rbCheapest) {
            Collections.sort(filteredList, (r1, r2) -> {
                try {
                    String price1 = r1.getAveragePrice();
                    String price2 = r2.getAveragePrice();
                    if (price1 == null) price1 = "0";
                    if (price2 == null) price2 = "0";
                    return Double.compare(Double.valueOf(price1), Double.valueOf(price2));
                } catch (NumberFormatException e) {
                    return 0;
                }
            });
        } else if (checkedId == R.id.rbBestRating) {
            Collections.sort(filteredList, (r1, r2) -> Double.compare(r2.getRating(), r1.getRating()));
        } else if (checkedId == R.id.rbBestSeller) {
            // Sort by total ratings (popularity)
            Collections.sort(filteredList, (r1, r2) -> Integer.compare(r2.getTotalRatings(), r1.getTotalRatings()));
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadUserLocation() {
        SharedPreferences prefs = getSharedPreferences(firebaseAuth.getUid(), Context.MODE_PRIVATE);
        String currentAddress = prefs.getString("current_address", null);
        
        if (currentAddress != null && !currentAddress.isEmpty()) {
            // Try to get cached coordinates first
            userLatitude = prefs.getString("current_latitude", null);
            userLongitude = prefs.getString("current_longitude", null);

            if (userLatitude == null || userLongitude == null) {
                // If no cached coordinates, geocode the address
                String apiKey = getString(R.string.goong_api_key);
                GeoCodingApi.apiInterface.getGeo(apiKey, currentAddress)
                    .enqueue(new Callback<GeoResponse>() {
                        @Override
                        public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {
                            if (response.isSuccessful() && response.body() != null && 
                                response.body().getResults() != null && !response.body().getResults().isEmpty()) {
                                
                                GeoResponse.GeocoderResult result = response.body().getResults().get(0);
                                userLatitude = String.valueOf(result.getGeometry().getLocation().getLat());
                                userLongitude = String.valueOf(result.getGeometry().getLocation().getLng());

                                // Cache the coordinates
                                prefs.edit()
                                    .putString("current_latitude", userLatitude)
                                    .putString("current_longitude", userLongitude)
                                    .apply();

                                // Calculate distances for all restaurants
                                calculateDistancesForRestaurants();
                            }
                        }

                        @Override
                        public void onFailure(Call<GeoResponse> call, Throwable t) {
                            Log.e("SearchActivity", "Error getting user coordinates", t);
                        }
                    });
            } else {
                // We have cached coordinates, calculate distances
                calculateDistancesForRestaurants();
            }
        }
    }

    private void calculateDistancesForRestaurants() {
        if (isCalculatingDistances || userLatitude == null || userLongitude == null) {
            return;
        }

        isCalculatingDistances = true;
        String apiKey = getString(R.string.goong_api_key);

        for (Restaurant restaurant : restaurantList) {
            if (restaurant.getAddress() == null || restaurant.getAddress().isEmpty()) {
                continue;
            }

            // First get restaurant coordinates
            GeoCodingApi.apiInterface.getGeo(apiKey, restaurant.getAddress())
                .enqueue(new Callback<GeoResponse>() {
                    @Override
                    public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {
                        if (response.isSuccessful() && response.body() != null && 
                            response.body().getResults() != null && !response.body().getResults().isEmpty()) {
                            
                            GeoResponse.GeocoderResult result = response.body().getResults().get(0);
                            String restaurantLat = String.valueOf(result.getGeometry().getLocation().getLat());
                            String restaurantLng = String.valueOf(result.getGeometry().getLocation().getLng());

                            // Calculate distance
                            String origins = userLatitude + "," + userLongitude;
                            String destinations = restaurantLat + "," + restaurantLng;

                            DistanceAPI.apiInterface.getDistance(apiKey, origins, destinations, "car")
                                .enqueue(new Callback<DistanceResult>() {
                                    @Override
                                    public void onResponse(Call<DistanceResult> call, Response<DistanceResult> response) {
                                        if (response.isSuccessful() && response.body() != null && 
                                            response.body().getRows() != null && !response.body().getRows().isEmpty()) {
                                            
                                            DistanceResult.Rows row = response.body().getRows().get(0);
                                            if (row.getElements() != null && !row.getElements().isEmpty()) {
                                                DistanceResult.Elements element = row.getElements().get(0);
                                                if (element.getStatus().equals("OK")) {
                                                    double distanceInKm = Double.parseDouble(element.getDistance().getValue()) / 1000.0;
                                                    
                                                    // Update restaurant distance
                                                    restaurant.setDistance(distanceInKm);
                                                    
                                                    // If we're currently sorting by distance, update the list
                                                    if (rbNearest != null && rbNearest.isChecked()) {
                                                        applySort();
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<DistanceResult> call, Throwable t) {
                                        Log.e("SearchActivity", "Error calculating distance", t);
                                    }
                                });
                        }
                    }

                    @Override
                    public void onFailure(Call<GeoResponse> call, Throwable t) {
                        Log.e("SearchActivity", "Error getting restaurant coordinates", t);
                    }
                });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOOD_DETAIL && resultCode == RESULT_OK && data != null) {
            String foodId = data.getStringExtra("FOOD_ID");
            Log.d("SearchActivity", "Received result for food item: " + foodId);

            if (foodId != null && adapter != null) {
                // Refresh the specific item in the adapter
                adapter.refreshMenuItem(foodId);
                Log.d("SearchActivity", "Refreshing menu item: " + foodId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh all items when activity resumes
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.d("SearchActivity", "Refreshed all items on resume");
        }
    }
}
