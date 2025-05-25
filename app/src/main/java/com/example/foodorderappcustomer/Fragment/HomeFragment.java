package com.example.foodorderappcustomer.Fragment;

import android.content.Intent;
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

public class HomeFragment extends Fragment implements PromotionAdapter.OnPromotionClickListener {

    private RecyclerView categoryRecyclerView;
    private RecyclerView restaurantRecyclerView;
    private RecyclerView promotionsRecyclerView;
    private RecyclerView nearbyRestaurantsRecyclerView;
    private CategoryAdapter categoryAdapter;
    private RestaurantAdapter restaurantAdapter;
    private RestaurantAdapter nearbyRestaurantAdapter;
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
    private TextView viewAllCategories;
    private TextView viewAllRestaurants;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

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
        nearbyRestaurantsRecyclerView = view.findViewById(R.id.nearbyRestaurantsRecyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        welcomeTextView = view.findViewById(R.id.textView);
        addressTextView = view.findViewById(R.id.addressTextView);
        viewAllCategories = view.findViewById(R.id.viewAllCategories);
        viewAllRestaurants = view.findViewById(R.id.viewAllRestaurants);
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
        setupNearbyRestaurantsRecyclerView();

        // Set up search functionality
        setupSearch();
        setupLocation();

        // Set up click listeners
        setupClickListeners();

        // Load data
        loadUserData();
        loadCategories();
        loadRestaurants();
        loadPromotions();

        return view;
    }

    private void setupClickListeners() {
        viewAllCategories.setOnClickListener(v -> {
            // Handle view all categories click
            Toast.makeText(getContext(), "View all categories clicked", Toast.LENGTH_SHORT).show();
            // TODO: Launch categories screen
        });

        viewAllRestaurants.setOnClickListener(v -> {
            // Handle view all restaurants click
            Toast.makeText(getContext(), "View all restaurants clicked", Toast.LENGTH_SHORT).show();
            // TODO: Launch restaurants screen
        });
        floatingActionButton.setOnClickListener(v -> {
            // Handle floating action button click
            Toast.makeText(getContext(), "Floating action button clicked", Toast.LENGTH_SHORT).show();
            // TODO: Launch cart screen
            startActivity(new Intent(getContext(), CartActivity.class));
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

    private void setupNearbyRestaurantsRecyclerView() {
        nearbyRestaurantAdapter = new RestaurantAdapter(nearbyRestaurantList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        nearbyRestaurantsRecyclerView.setLayoutManager(layoutManager);
        nearbyRestaurantsRecyclerView.setAdapter(nearbyRestaurantAdapter);
    }

    private void setupSearch() {
        searchEditText.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SearchActivity.class));
        });
    }

    private void setupLocation() {
        addressTextView.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), LocationActivity.class));
        });
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
            databaseReference.child("users").child(currentUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String displayName = dataSnapshot.child("displayName").getValue(String.class);
                                if (displayName != null && !displayName.isEmpty()) {
                                    welcomeTextView.setText("Xin chào, " + displayName + "!");
                                }

                                // Get user address
                                if (dataSnapshot.child("address").exists()) {
                                    DataSnapshot addressSnapshot = dataSnapshot.child("address");
                                    String street = addressSnapshot.child("street").getValue(String.class);
                                    String city = addressSnapshot.child("city").getValue(String.class);
                                    String state = addressSnapshot.child("state").getValue(String.class);

                                    StringBuilder fullAddress = new StringBuilder();
                                    if (street != null && !street.isEmpty()) {
                                        fullAddress.append(street);
                                    }
                                    if (city != null && !city.isEmpty()) {
                                        if (fullAddress.length() > 0) fullAddress.append(", ");
                                        fullAddress.append(city);
                                    }
                                    if (state != null && !state.isEmpty()) {
                                        if (fullAddress.length() > 0) fullAddress.append(", ");
                                        fullAddress.append(state);
                                    }

                                    if (fullAddress.length() > 0) {
                                        addressTextView.setText(fullAddress.toString());
                                        // Make address clickable to update location
                                        addressTextView.setOnClickListener(v -> {
                                            startActivity(new Intent(getContext(), LocationActivity.class));
                                        });
                                    } else {
                                        addressTextView.setText("Thêm địa chỉ của bạn");
                                        addressTextView.setOnClickListener(v -> {
                                            startActivity(new Intent(getContext(), LocationActivity.class));
                                        });
                                    }
                                } else {
                                    addressTextView.setText("Thêm địa chỉ của bạn");
                                    addressTextView.setOnClickListener(v -> {
                                        startActivity(new Intent(getContext(), LocationActivity.class));
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(getContext(), "Lỗi khi tải dữ liệu người dùng", Toast.LENGTH_SHORT).show();
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

                    // Set cuisine types if available
                    if (snapshot.hasChild("cuisine")) {
                        List<String> cuisineTypes = new ArrayList<>();
                        for (DataSnapshot cuisineSnapshot : snapshot.child("cuisine").getChildren()) {
                            String cuisine = cuisineSnapshot.getValue(String.class);
                            if (cuisine != null) {
                                cuisineTypes.add(cuisine);
                            }
                        }
                        restaurant.setCuisineTypes(cuisineTypes);
                    }

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
                nearbyRestaurantAdapter.updateData(nearbyRestaurantList);
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
                    String code = snapshot.child("code").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);
                    String startDateStr = snapshot.child("startDate").getValue(String.class);
                    String endDateStr = snapshot.child("endDate").getValue(String.class);
                    Double discountValue = snapshot.child("discountValue").getValue(Double.class);
                    String discountType = snapshot.child("discountType").getValue(String.class);
                    Boolean isActive = snapshot.child("isActive").getValue(Boolean.class);

                    // Convert date strings to Date objects
                    Date startDate = null;
                    Date endDate = null;
                    try {
                        if (startDateStr != null) {
                            startDate = dateFormat.parse(startDateStr);
                        }
                        if (endDateStr != null) {
                            endDate = dateFormat.parse(endDateStr);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    // Only add active promotions
                    if (isActive != null && isActive) {
                        Promotion promotion = new Promotion(id, code, description, discountType, discountValue);
                        promotion.setStartDate(startDate);
                        promotion.setEndDate(endDate);
                        promotionList.add(promotion);
                    }
                }

                promotionAdapter.updateData(promotionList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Lỗi khi tải khuyến mãi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getCategoryImageResource(String categoryName) {
        // Map category names to drawable resources
        // You should replace these with your actual drawable resources

        String lowerCaseName = categoryName.toLowerCase();

        if (lowerCaseName.contains("pizza")) {
            return R.drawable.icons_pizza;
        } else if (lowerCaseName.contains("pasta")) {
            return R.drawable.icons_pho;
        } else if (lowerCaseName.contains("burger") || lowerCaseName.contains("hamburger")) {
            return R.drawable.icons8_bread;
        } else {
            return R.drawable.logo2;
        }
    }

    @Override
    public void onPromotionClick(Promotion promotion) {
        // Handle promotion click
        Toast.makeText(getContext(), "Promotion code: " + promotion.getCode(), Toast.LENGTH_SHORT).show();
        // TODO: Show promotion details or apply to cart
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