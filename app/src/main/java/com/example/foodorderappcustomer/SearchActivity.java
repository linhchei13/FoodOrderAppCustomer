package com.example.foodorderappcustomer;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private RadioGroup sortGroup;
    private RadioButton rbDefault, rbCheapest, rbBestSeller, rbNearest, rbBestRating;
    private Button btn35Rating, btn40Rating, btn45Rating;
    private Button btnPriceRange1, btnPriceRange2;
    private EditText etMinPrice, etMaxPrice;
    private Button btnReset, btnConfirm;
    private EditText etSearchQuery;
    private ImageButton btnBack, btnClearSearch, btnFilter;
    private LinearLayout filterSection;
    private boolean isFilterVisible = false;

    // Results components
    private RecyclerView recyclerView;
    private RestaurantAdapter adapter;
    private List<Restaurant> restaurantList;
    private List<Restaurant> filteredList;

    // Filter parameters
    private double minRating = 0;
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_new);

        initViews();
        setupData();
        setupListeners();
        setupRecyclerView();
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
        btnPriceRange1 = findViewById(R.id.btnPriceRange1);
        btnPriceRange2 = findViewById(R.id.btnPriceRange2);
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
        isFilterVisible = !isFilterVisible;
        filterSection.setVisibility(isFilterVisible ? View.VISIBLE : View.GONE);
    }

    private void setupData() {
        restaurantList = new ArrayList<>();
        filteredList = new ArrayList<>();
        
        // Load data from Firebase
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                restaurantList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Restaurant restaurant = snapshot.getValue(Restaurant.class);
                    restaurant.setId(snapshot.getKey());
                    restaurantList.add(restaurant);
                }
                
                // Initialize filtered list with all restaurants
                filteredList.clear();
                filteredList.addAll(restaurantList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SearchActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new RestaurantAdapter(filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Search functionality
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                performSearch();
            }
        });
        
        // Clear search field
        btnClearSearch.setOnClickListener(v -> {
            etSearchQuery.setText("");
            performSearch();
        });
        
        // Back button
        btnBack.setOnClickListener(v -> finish());
        
        // Rating filter buttons
        btn35Rating.setOnClickListener(v -> {
            minRating = 3.5;
            resetRatingButtonsStyle();
            btn35Rating.setBackground(getResources().getDrawable(R.drawable.button_selected));
            applyFilters();
        });
        
        btn40Rating.setOnClickListener(v -> {
            minRating = 4.0;
            resetRatingButtonsStyle();
            btn40Rating.setBackground(getResources().getDrawable(R.drawable.button_selected));
            applyFilters();
        });
        
        btn45Rating.setOnClickListener(v -> {
            minRating = 4.5;
            resetRatingButtonsStyle();
            btn45Rating.setBackground(getResources().getDrawable(R.drawable.button_selected));
            applyFilters();
        });
        
        // Price range buttons
        btnPriceRange1.setOnClickListener(v -> {
            minPrice = 0;
            maxPrice = 30000;
            etMinPrice.setText(String.valueOf(minPrice));
            etMaxPrice.setText(String.valueOf(maxPrice));
            resetPriceButtonsStyle();
            btnPriceRange1.setBackground(getResources().getDrawable(R.drawable.button_selected));
            applyFilters();
        });
        
        btnPriceRange2.setOnClickListener(v -> {
            minPrice = 30000;
            maxPrice = 50000;
            etMinPrice.setText(String.valueOf(minPrice));
            etMaxPrice.setText(String.valueOf(maxPrice));
            resetPriceButtonsStyle();
            btnPriceRange2.setBackground(getResources().getDrawable(R.drawable.button_selected));
            applyFilters();
        });
        
        // Reset button
        btnReset.setOnClickListener(v -> resetFilters());
        
        // Confirm button
        btnConfirm.setOnClickListener(v -> applyFilters());
        
        // Sort group
        sortGroup.setOnCheckedChangeListener((group, checkedId) -> applySort());
        
        // Filter button
        btnFilter.setOnClickListener(v -> toggleFilterSection());
    }

    private void resetRatingButtonsStyle() {
        btn35Rating.setBackground(getResources().getDrawable(R.drawable.button_outline));
        btn40Rating.setBackground(getResources().getDrawable(R.drawable.button_outline));
        btn45Rating.setBackground(getResources().getDrawable(R.drawable.button_outline));
    }

    private void resetPriceButtonsStyle() {
        btnPriceRange1.setBackground(getResources().getDrawable(R.drawable.button_outline));
        btnPriceRange2.setBackground(getResources().getDrawable(R.drawable.button_outline));
    }

    private void performSearch() {
        String query = etSearchQuery.getText().toString().toLowerCase().trim();
        
        // Apply search query
        filteredList.clear();
        
        if (query.isEmpty()) {
            filteredList.addAll(restaurantList);
        } else {
            for (Restaurant restaurant : restaurantList) {
                if (restaurant.getName().toLowerCase().contains(query) || 
                    (restaurant.getDescription() != null && restaurant.getDescription().toLowerCase().contains(query))) {
                    filteredList.add(restaurant);
                }
            }
        }
        
        // Apply any active filters
        applyFilters();
    }

    private void resetFilters() {
        // Reset sort options
        rbDefault.setChecked(true);
        
        // Reset rating
        minRating = 0;
        resetRatingButtonsStyle();
        
        // Reset price range
        minPrice = 0;
        maxPrice = Double.MAX_VALUE;
        etMinPrice.setText("");
        etMaxPrice.setText("");
        resetPriceButtonsStyle();
        
        // Clear search and reset list
        etSearchQuery.setText("");
        filteredList.clear();
        filteredList.addAll(restaurantList);
        adapter.notifyDataSetChanged();
    }

    private void applyFilters() {
        // Get values from min/max price fields if provided
        try {
            String minPriceText = etMinPrice.getText().toString();
            String maxPriceText = etMaxPrice.getText().toString();
            
            if (!minPriceText.isEmpty()) {
                minPrice = Double.parseDouble(minPriceText);
            }
            
            if (!maxPriceText.isEmpty()) {
                maxPrice = Double.parseDouble(maxPriceText);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Vui lòng nhập giá hợp lệ", Toast.LENGTH_SHORT).show();
        }
        
        // Apply search first
        String query = etSearchQuery.getText().toString().toLowerCase().trim();
        List<Restaurant> searchResults = new ArrayList<>();
        
        if (query.isEmpty()) {
            searchResults.addAll(restaurantList);
        } else {
            for (Restaurant restaurant : restaurantList) {
                if (restaurant.getName().toLowerCase().contains(query) || 
                    (restaurant.getDescription() != null && restaurant.getDescription().toLowerCase().contains(query))) {
                    searchResults.add(restaurant);
                }
            }
        }
        
        // Apply filters to search results
        filteredList.clear();
        
        for (Restaurant restaurant : searchResults) {
            // Apply rating filter
            if (restaurant.getRating() >= minRating) {
                // Apply price filter
                String priceStr = restaurant.getAveragePrice();
                double price = Double.valueOf(priceStr) * 1000;
                if (price >= minPrice && price <= maxPrice) {
                    filteredList.add(restaurant);
                }
            }
        }
        
        // Apply sorting
        applySort();
        
        adapter.notifyDataSetChanged();
    }

    private void applySort() {
        int checkedId = sortGroup.getCheckedRadioButtonId();
        
        if (checkedId == R.id.rbCheapest) {
            Collections.sort(filteredList, Comparator.comparing(Restaurant::getAveragePrice));
        } else if (checkedId == R.id.rbBestSeller) {

        } else if (checkedId == R.id.rbBestRating) {
            Collections.sort(filteredList, (r1, r2) -> Double.compare(r2.getRating(), r1.getRating()));
        }
        
        adapter.notifyDataSetChanged();
    }
}