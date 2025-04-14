package com.example.foodorderappcustomer.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.foodorderappcustomer.Adapters.CategoryAdapter;
import com.example.foodorderappcustomer.Adapters.RestaurantAdapter;
import com.example.foodorderappcustomer.Models.Category;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private RecyclerView categoryRecyclerView;
    private RecyclerView restaurantRecyclerView;
    private CategoryAdapter categoryAdapter;
    private RestaurantAdapter restaurantAdapter;
    private EditText searchEditText;
    private List<Category> allCategories;
    private List<Restaurant> allRestaurants;
    private TextView addressTextView;

    // Firebase references
    private DatabaseReference databaseReference;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        searchEditText = view.findViewById(R.id.searchEditText);
        categoryRecyclerView = view.findViewById(R.id.categoryView);
        restaurantRecyclerView = view.findViewById(R.id.restaurantView);
//        addressTextView = view.findViewById(R.id.address);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize lists
        allCategories = new ArrayList<>();
        allRestaurants = new ArrayList<>();

        // Setup adapters
        categoryAdapter = new CategoryAdapter(allCategories);
        restaurantAdapter = new RestaurantAdapter(allRestaurants);

        // Setup RecyclerViews
        setupCategoryRecyclerView();
        setupRestaurantRecyclerView();

        // Load data from Firebase
        loadDataFromFirebase();

        // Setup search functionality
        setupSearch();
    }

    private void setupCategoryRecyclerView() {
        // Use GridLayoutManager with 4 columns for the categories
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 4);
        categoryRecyclerView.setLayoutManager(gridLayoutManager);
        categoryRecyclerView.setAdapter(categoryAdapter);
    }

    private void setupRestaurantRecyclerView() {
        restaurantRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        restaurantRecyclerView.setAdapter(restaurantAdapter);
    }

    private void loadDataFromFirebase() {
        // Load categories from menu items
        databaseReference.child("menu_items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> uniqueCategories = new HashSet<>();

                // Extract unique categories from menu items
                for (DataSnapshot menuItemSnapshot : dataSnapshot.getChildren()) {
                    String category = menuItemSnapshot.child("category").getValue(String.class);
                    if (category != null) {
                        uniqueCategories.add(category);
                    }
                }

                // Clear existing categories
                allCategories.clear();

                // Add categories with appropriate icons
                for (String categoryName : uniqueCategories) {
                    int iconResource = getCategoryIcon(categoryName);
                    allCategories.add(new Category(categoryName, iconResource));
                }

                // Notify adapter
                categoryAdapter.updateData(allCategories);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load categories: " + databaseError.getMessage());
            }
        });

        // Load restaurants
        databaseReference.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allRestaurants.clear();

                for (DataSnapshot restaurantSnapshot : dataSnapshot.getChildren()) {
                    String restaurantId = restaurantSnapshot.getKey();
                    String id = restaurantSnapshot.child("id").getValue().toString();
                    if (id == null) id = restaurantId; // Use key if id is not available

                    String name = restaurantSnapshot.child("restaurant_name").getValue(String.class);
                    String address = restaurantSnapshot.child("address").getValue(String.class);

                    // Default rating and image if not available in Firebase
                    float rating = 4.5f;
                    int imageResource = R.drawable.nemnuong; // Default image

                    // Create restaurant object and add to list
                    Restaurant restaurant = new Restaurant(id, name, address, rating, imageResource);
                    allRestaurants.add(restaurant);
                }

                // Notify adapter
                restaurantAdapter.updateData(allRestaurants);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load restaurants: " + databaseError.getMessage());
            }
        });
    }

    private int getCategoryIcon(String categoryName) {
        // Map category names to icon resources
        switch (categoryName.toLowerCase()) {
            case "cơm":
                return R.drawable.icons_rice;
            case "phở, bún":
                return R.drawable.icons_pho;
            case "đồ uống":
                return R.drawable.icons_drink;
            case "bánh mỳ":
                return R.drawable.icons8_bread;
            default:
                return R.drawable.icons_pho; // Default icon
        }
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterData(String query) {
        // Filter categories
        List<Category> filteredCategories = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredCategories.add(category);
            }
        }
        categoryAdapter.updateData(filteredCategories);

        // Filter restaurants
        List<Restaurant> filteredRestaurants = new ArrayList<>();
        for (Restaurant restaurant : allRestaurants) {
            if (restaurant.getName().toLowerCase().contains(query.toLowerCase()) ||
                    restaurant.getAddress().toLowerCase().contains(query.toLowerCase())) {
                filteredRestaurants.add(restaurant);
            }
        }
        restaurantAdapter.updateData(filteredRestaurants);
    }
}