package com.example.foodorderappcustomer.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.CategoryAdapter;
import com.example.foodorderappcustomer.Adapter.PromotionAdapter;
import com.example.foodorderappcustomer.Adapter.RestaurantAdapter;
import com.example.foodorderappcustomer.CartActivity;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import static android.app.Activity.RESULT_OK;

import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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
    private FloatingActionButton floatingActionButton;
    private TextView searchEditText;
    private TextView welcomeTextView;
    private TextView addressTextView;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private static final int LOCATION_REQUEST_CODE = 1001;
    private ActivityResultLauncher<Intent> locationActivityLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

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
                        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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
            // Handle floating action button click
            Toast.makeText(getContext(), "Floating action button clicked", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getContext(), CartActivity.class));
        });
        searchEditText.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SearchActivity.class));
        });
        addressTextView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LocationActivity.class);
            locationActivityLauncher.launch(intent);
        });
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

    private void setupSearch() {

    }

    private void setupLocation() {

    }

    private void filterRestaurants(String query) {
        filteredRestaurantList.clear();

        if (query.isEmpty()) {
            filteredRestaurantList.addAll(restaurantList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Restaurant restaurant : restaurantList) {
                if (restaurant.getName().toLowerCase().contains(lowerCaseQuery) ||
                        (restaurant.getAddress() != null && restaurant.getAddress().toLowerCase().contains(lowerCaseQuery))) {
                    filteredRestaurantList.add(restaurant);
                }
            }
        }

        restaurantAdapter.updateData(filteredRestaurantList);
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // First check if user has manually selected an address
            SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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
        // Clear existing categories
        categoryList.clear();

        // Add predefined categories
        Category cafeTraSua = new Category("1", "Cà phê, Trà sữa", "Các loại cà phê và trà sữa");
        cafeTraSua.setImageResource(R.drawable.icons_drink);

        Category com = new Category("2", "Cơm", "Các món cơm");
        com.setImageResource(R.drawable.icons_rice);

        Category bunPho = new Category("3", "Bún, Phở", "Các món bún và phở");
        bunPho.setImageResource(R.drawable.icons_pho);

        Category anVat = new Category("4", "Ăn vặt", "Các món ăn vặt");
        anVat.setImageResource(R.drawable.icons_pizza);

        Category trangMieng = new Category("5", "Tráng miệng", "Các món tráng miệng");
        trangMieng.setImageResource(R.drawable.icons_dessert);

        // Add categories to the list
        categoryList.add(cafeTraSua);
        categoryList.add(com);
        categoryList.add(bunPho);
        categoryList.add(anVat);
        categoryList.add(trangMieng);

        // Update adapter
        categoryAdapter.updateData(categoryList);
    }

    private void loadRestaurants() {
        databaseReference.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                restaurantList.clear();
                nearbyRestaurantList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String id = snapshot.getKey();
                    String name = snapshot.child("name").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);

                    // Get address
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
                        } else {
                            address = addressSnapshot.getValue(String.class);
                        }
                    }

                    // Get rating
                    double rating = 0.0;
                    if (snapshot.hasChild("rating")) {
                        Double ratingValue = snapshot.child("rating").getValue(Double.class);
                        if (ratingValue != null) {
                            rating = ratingValue;
                        }
                    }

                    // Get delivery fee
                    double deliveryFee = 0.0;
                    if (snapshot.hasChild("deliveryFee")) {
                        Double deliveryFeeValue = snapshot.child("deliveryFee").getValue(Double.class);
                        if (deliveryFeeValue != null) {
                            deliveryFee = deliveryFeeValue;
                        }
                    }

                    // Get delivery time
                    int deliveryTime = 0;
                    if (snapshot.hasChild("averageDeliveryTime")) {
                        Integer deliveryTimeValue = snapshot.child("averageDeliveryTime").getValue(Integer.class);
                        if (deliveryTimeValue != null) {
                            deliveryTime = deliveryTimeValue;
                        }
                    }

                    // Create a Restaurant object with default image resource
                    Restaurant restaurant = new Restaurant(id, name, description, address, rating);
                    restaurant.setDeliveryFee(deliveryFee);
                    restaurant.setAverageDeliveryTime(deliveryTime);

                    // Set a default image resource
                    // This is a temporary solution until you implement image loading from URLs
                    if (snapshot.hasChild("imageUrl")) {
                        String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                        restaurant.setImageUrl(imageUrl);
                    } else {
                        restaurant.setImageResource(R.drawable.logo2);
                    }
                    restaurantList.add(restaurant);

                    // For demo purposes, we'll consider the first 5 restaurants as "nearby"
                    if (nearbyRestaurantList.size() < 5) {
                        nearbyRestaurantList.add(restaurant);
                    }
                }

                // Also update the filtered list
                filteredRestaurantList.clear();
                filteredRestaurantList.addAll(restaurantList);

                restaurantAdapter.updateData(filteredRestaurantList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Lỗi khi tải nhà hàng", Toast.LENGTH_SHORT).show();
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
                    String code = snapshot.child("promoCode").getValue(String.class);
                    String startDateStr = snapshot.child("startDate").getValue(String.class);
                    String endDateStr = snapshot.child("endDate").getValue(String.class);
                    
                    // Safely get discountAmount with null check
                    Double discountValue = snapshot.child("discountAmount").getValue(Double.class);
                    if (discountValue == null) {
                        discountValue = 0.0;
                    }
                    
                    String discountType = snapshot.child("discountType").getValue(String.class);
                    
                    // Safely get maxDiscountAmount with null check
                    Double maxDiscount = snapshot.child("maxDiscountAmount").getValue(Double.class);
                    if (maxDiscount == null) {
                        maxDiscount = 0.0;
                    }
                    
                    String minimumOrder = snapshot.child("minimumOrder").getValue(String.class);
                    
                    Promotion promotion = new Promotion(code, discountType, discountValue, startDateStr, endDateStr, minimumOrder, maxDiscount);
                    promotion.setDiscountAmount(discountValue);
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
    }
}