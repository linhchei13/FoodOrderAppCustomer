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

import com.example.foodorderappcustomer.API.GeoCodingApi;
import com.example.foodorderappcustomer.API.GeoResponse;
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
import com.example.foodorderappcustomer.API.DistanceAPI;
import com.example.foodorderappcustomer.API.DistanceResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private static final String GOONG_API_KEY = "YOUR_GOONG_API_KEY"; // Replace with your actual API key
    private String userLatitude;
    private String userLongitude;

    // Cache for geocoding results
    private static class GeocodingCache {
        private final String address;
        private final String latitude;
        private final String longitude;
        private final long timestamp;

        public GeocodingCache(String address, String latitude, String longitude) {
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            // Cache valid for 24 hours
            return System.currentTimeMillis() - timestamp < TimeUnit.HOURS.toMillis(24);
        }
    }

    // Cache for distance results
    private static class DistanceCache {
        private final String originLat;
        private final String originLng;
        private final String destLat;
        private final String destLng;
        private final double distance;
        private final long timestamp;

        public DistanceCache(String originLat, String originLng, String destLat, String destLng, double distance) {
            this.originLat = originLat;
            this.originLng = originLng;
            this.destLat = destLat;
            this.destLng = destLng;
            this.distance = distance;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            // Cache valid for 1 hour
            return System.currentTimeMillis() - timestamp < TimeUnit.HOURS.toMillis(1);
        }

        public boolean matches(String originLat, String originLng, String destLat, String destLng) {
            return this.originLat.equals(originLat) && 
                   this.originLng.equals(originLng) && 
                   this.destLat.equals(destLat) && 
                   this.destLng.equals(destLng);
        }
    }

    private Map<String, GeocodingCache> geocodingCache = new HashMap<>();
    private List<DistanceCache> distanceCache = new ArrayList<>();

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
        if (!isAdded() || getActivity() == null) {
            return;
        }

        databaseReference.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                restaurantList.clear();
                nearbyRestaurantList.clear();

                try {
                    loadRestaurantsWithoutDistance(dataSnapshot);
                    // Get user's current address from SharedPreferences
                    SharedPreferences prefs = getActivity().getSharedPreferences(firebaseAuth.getUid(), Context.MODE_PRIVATE);
                    String currentAddress = prefs.getString("current_address", null);

                    if (currentAddress != null && !currentAddress.isEmpty()) {
                         // Geocode the current address to get coordinates
                        String apiKey = getContext().getString(R.string.goong_api_key);
                        GeoCodingApi.apiInterface.getGeo(apiKey, currentAddress)
                            .enqueue(new Callback<GeoResponse>() {
                                @Override
                                public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {
                                    if (!isAdded() || getActivity() == null) {
                                        return;
                                    }

                                    if (response.isSuccessful() && response.body() != null &&
                                        response.body().getResults() != null && !response.body().getResults().isEmpty()) {

                                        GeoResponse.GeocoderResult result = response.body().getResults().get(0);
                                        userLatitude = String.valueOf(result.getGeometry().getLocation().getLat());
                                        userLongitude = String.valueOf(result.getGeometry().getLocation().getLng());

                                        try {
                                            // Store coordinates in SharedPreferences
                                            prefs.edit()
                                                .putString("current_latitude", userLatitude)
                                                .putString("current_longitude", userLongitude)
                                                .apply();

                                            // Now load restaurants with the coordinates
                                            loadRestaurantsWithCoordinates(dataSnapshot);
                                        } catch (Exception e) {
                                            Log.e("HomeFragment", "Error saving coordinates", e);
                                            loadRestaurantsWithoutDistance(dataSnapshot);
                                        }
                                    } else {
                                        loadRestaurantsWithoutDistance(dataSnapshot);
                                    }
                                }

                                @Override
                                public void onFailure(Call<GeoResponse> call, Throwable t) {
                                    Log.e("GeoCodingAPI", "Error getting user coordinates", t);
                                    if (isAdded()) {
                                        loadRestaurantsWithoutDistance(dataSnapshot);
                                    }
                                }
                            });
                    } else {
                        loadRestaurantsWithoutDistance(dataSnapshot);
                    }
                } catch (Exception e) {
                    Log.e("HomeFragment", "Error accessing SharedPreferences", e);
                    loadRestaurantsWithoutDistance(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải nhà hàng", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadRestaurantsWithCoordinates(DataSnapshot dataSnapshot) {
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        restaurantList.clear();
        Map<String, Restaurant> restaurantMap = new HashMap<>();

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            String id = snapshot.getKey();
            loadRestaurantReviews(id);
            loadItems(id);

            Restaurant restaurant = snapshot.getValue(Restaurant.class);
            restaurant.setId(snapshot.getKey());
            if (snapshot.hasChild("category")) {
                String category = snapshot.child("category").getValue(String.class);
                restaurant.setCategory(category);
            } else {
                restaurant.setCategory("Chưa có danh mục");
            }

            restaurantMap.put(id, restaurant);
            restaurantList.add(restaurant);

            if (restaurant.getAddress() != null && !restaurant.getAddress().isEmpty()) {
                calculateDistanceWithCache(restaurant, restaurantMap);
            }
        }

        if (isAdded() && restaurantAdapter != null) {
            restaurantAdapter.updateData(restaurantList);
        }
    }

    private void calculateDistanceWithCache(Restaurant restaurant, Map<String, Restaurant> restaurantMap) {
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        // Check if we have cached geocoding for restaurant
        GeocodingCache restaurantLocationCache = geocodingCache.get(restaurant.getAddress());
        if (restaurantLocationCache != null && restaurantLocationCache.isValid()) {
            // Use cached restaurant coordinates
            calculateDistanceWithCoordinates(
                restaurant, 
                restaurantMap, 
                restaurantLocationCache.latitude, 
                restaurantLocationCache.longitude
            );
        } else {
            // Need to geocode restaurant address
            String apiKey = getContext().getString(R.string.goong_api_key);
            GeoCodingApi.apiInterface.getGeo(apiKey, restaurant.getAddress())
                .enqueue(new Callback<GeoResponse>() {
                    @Override
                    public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {
                        // Check if fragment is still attached
                        if (!isAdded() || getActivity() == null) {
                            return;
                        }

                        if (response.isSuccessful() && response.body() != null && 
                            response.body().getResults() != null && !response.body().getResults().isEmpty()) {
                            
                            GeoResponse.GeocoderResult result = response.body().getResults().get(0);
                            String restaurantLat = String.valueOf(result.getGeometry().getLocation().getLat());
                            String restaurantLng = String.valueOf(result.getGeometry().getLocation().getLng());

                            // Cache the geocoding result
                            geocodingCache.put(restaurant.getAddress(), 
                                new GeocodingCache(restaurant.getAddress(), restaurantLat, restaurantLng));

                            calculateDistanceWithCoordinates(restaurant, restaurantMap, restaurantLat, restaurantLng);
                        }
                    }

                    @Override
                    public void onFailure(Call<GeoResponse> call, Throwable t) {
                        Log.e("GeoCodingAPI", "Error getting restaurant coordinates", t);
                    }
                });
        }
    }

    private void calculateDistanceWithCoordinates(Restaurant restaurant, Map<String, Restaurant> restaurantMap, 
                                                String restaurantLat, String restaurantLng) {
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        // Check if we have cached distance
        for (DistanceCache cache : distanceCache) {
            if (cache.isValid() && cache.matches(userLatitude, userLongitude, restaurantLat, restaurantLng)) {
                // Use cached distance
                updateRestaurantDistance(restaurant, restaurantMap, cache.distance);
                return;
            }
        }

        // Need to calculate distance
        String apiKey = getContext().getString(R.string.goong_api_key);
        String origins = userLatitude + "," + userLongitude;
        String destinations = restaurantLat + "," + restaurantLng;

        DistanceAPI.apiInterface.getDistance(apiKey, origins, destinations, "car")
            .enqueue(new Callback<DistanceResult>() {
                @Override
                public void onResponse(Call<DistanceResult> call, Response<DistanceResult> response) {
                    // Check if fragment is still attached
                    if (!isAdded() || getActivity() == null) {
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null && 
                        response.body().getRows() != null && !response.body().getRows().isEmpty()) {
                        
                        DistanceResult.Rows row = response.body().getRows().get(0);
                        if (row.getElements() != null && !row.getElements().isEmpty()) {
                            DistanceResult.Elements element = row.getElements().get(0);
                            if (element.getStatus().equals("OK")) {
                                double distanceInKm = Double.parseDouble(element.getDistance().getValue()) / 1000.0;
                                
                                // Cache the distance result
                                distanceCache.add(new DistanceCache(userLatitude, userLongitude, 
                                    restaurantLat, restaurantLng, distanceInKm));

                                // Update restaurant distance
                                updateRestaurantDistance(restaurant, restaurantMap, distanceInKm);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<DistanceResult> call, Throwable t) {
                    Log.e("DistanceAPI", "Error calculating distance", t);
                }
            });
    }

    private void updateRestaurantDistance(Restaurant restaurant, Map<String, Restaurant> restaurantMap, double distanceInKm) {
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        // Update restaurant distance in Firebase
        databaseReference.child("restaurants")
            .child(restaurant.getId())
            .child("distance")
            .setValue(distanceInKm);
        
        // Update the restaurant object in the map
        Restaurant updatedRestaurant = restaurantMap.get(restaurant.getId());
        if (updatedRestaurant != null) {
            updatedRestaurant.setDistance(distanceInKm);
            // Update the list with the modified restaurant
            int position = restaurantList.indexOf(updatedRestaurant);
            if (position != -1 && restaurantAdapter != null) {
                restaurantList.set(position, updatedRestaurant);
                restaurantAdapter.notifyItemChanged(position);
            }
        }
    }

    private void loadRestaurantsWithoutDistance(DataSnapshot dataSnapshot) {
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        restaurantList.clear();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            String id = snapshot.getKey();
            loadRestaurantReviews(id);
            loadItems(id);

            Restaurant restaurant = snapshot.getValue(Restaurant.class);
            restaurant.setId(snapshot.getKey());
            if (snapshot.hasChild("category")) {
                String category = snapshot.child("category").getValue(String.class);
                restaurant.setCategory(category);
            } else {
                restaurant.setCategory("Chưa có danh mục");
            }

            restaurantList.add(restaurant);
        }

        if (isAdded() && restaurantAdapter != null) {
            restaurantAdapter.updateData(restaurantList);
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clear old cache entries
        geocodingCache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        distanceCache.removeIf(cache -> !cache.isValid());
    }
}