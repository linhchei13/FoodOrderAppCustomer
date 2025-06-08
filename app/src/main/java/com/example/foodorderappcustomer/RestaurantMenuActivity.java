package com.example.foodorderappcustomer;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Adapter.MenuItemAdapter;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.example.foodorderappcustomer.util.FoodCustomizationDialog;
import com.example.foodorderappcustomer.util.SalesManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

public class RestaurantMenuActivity extends AppCompatActivity implements OrderItemManager.OnCartUpdateListener {
    private static final String TAG = "RestaurantDetailActivity";
    private static final int REQUEST_FOOD_DETAIL = 1;

    // UI Components
    private Toolbar toolbar;
    private ImageButton backButton;
    private CollapsingToolbarLayout collapsingToolbar;
    private RecyclerView menuItemsRecyclerView;
    private ImageView restImageView;

    private TextView restaurantNameTextView;
    private TextView restaurantDescriptionText;
    private TextView addressText;
    private TextView deliveryFeeText;
    private TextView deliveryTimeText, categoryText;
    private RatingBar restaurantRating;
    private TextView ratingValueText;
    private TextView reviewCountText;
    private TabLayout menuTabLayout;
    private Button viewCartButton;
    private LinearLayout cartButtonLayout;
    private TextView cartQuantityTV;
    private ImageButton favoriteButton;
    private ImageButton showInfoButton;
    private ProgressBar loadingProgressBar;
    private NestedScrollView contentContainer;

    // Data
    private String restaurantId;
    private String restaurantName;
    private String restaurantDescription;
    private String restaurantAddress;
    private double restaurantRatingValue;
    private String restaurantImageUrl;
    private List<String> cuisineTypes;
    private double deliveryFee;
    private int deliveryTime;
    private List<MenuItem> menuItems;
    private List<MenuItem> filteredMenuItems;
    private Map<String, List<MenuItem>> menuItemsByCategory;
    private MenuItemAdapter menuItemAdapter;

    private Restaurant currentRestaurant;

    // Firebase
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    // Formatting
    private NumberFormat currencyFormat;

    // Add CartManager field
    private OrderItemManager orderItemManager;

    private SalesManager salesManager;

    private boolean isFavorite = false;
    private FirebaseAuth mAuth;
    private DatabaseReference favoritesRef;
    private DatabaseReference restaurantRef;

    private boolean isRestaurantDataLoaded = false;
    private boolean isMenuDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize currency formatter
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize CartManager
        orderItemManager = OrderItemManager.getInstance(this);
        orderItemManager.setOnCartUpdateListener(this); // Register as listener
        salesManager = SalesManager.getInstance();
        salesManager.recalculateAllSales();

        // Get restaurant ID from intent
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");
        restaurantName = getIntent().getStringExtra("RESTAURANT_NAME");

        if (restaurantId == null) {
            Toast.makeText(this, "Restaurant ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        favoritesRef = databaseReference.child("users").child(mAuth.getCurrentUser().getUid()).child("favorites");
        restaurantRef = databaseReference.child("restaurants");

        // Initialize views
        initializeViews();

        // Setup toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        // Set restaurant name in toolbar
        collapsingToolbar.setTitle(restaurantName);

        // Set up RecyclerView for menu items
        setupMenuItemsRecyclerView();

        // Initialize data structures
        menuItems = new ArrayList<>();
        filteredMenuItems = new ArrayList<>();
        menuItemsByCategory = new HashMap<>();
        cuisineTypes = new ArrayList<>();

        // Show loading indicator and hide content initially
        loadingProgressBar.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        cartButtonLayout.setVisibility(View.GONE);

        // Load data
        loadRestaurantDetails();
        loadMenuItems();

        // Setup click listeners
        setupClickListeners();

        // Initial update of cart button
        updateCartButton();

        // Check if restaurant is in favorites
        checkFavoriteStatus();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        backButton = findViewById(R.id.backButton);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        menuItemsRecyclerView = findViewById(R.id.menuItemsRecyclerView);
        restImageView = findViewById(R.id.restImageView);
        restaurantDescriptionText = findViewById(R.id.restaurantDescriptionText);
        addressText = findViewById(R.id.addressText);
        deliveryFeeText = findViewById(R.id.deliveryFeeText);
        deliveryTimeText = findViewById(R.id.deliveryTimeText);
        restaurantRating = findViewById(R.id.restaurantRating);
        ratingValueText = findViewById(R.id.ratingValueText);
        reviewCountText = findViewById(R.id.reviewCountText);
        menuTabLayout = findViewById(R.id.menuTabLayout);
        viewCartButton = findViewById(R.id.viewOrderButton);
        restaurantNameTextView = findViewById(R.id.restaurantNameTextView);
        cartButtonLayout = findViewById(R.id.cartButtonLayout);
        cartQuantityTV = findViewById(R.id.cartQuantityTV);
        favoriteButton = findViewById(R.id.favoriteButton);
        categoryText = findViewById(R.id.categoryText);
        showInfoButton = findViewById(R.id.showInformation);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        contentContainer = findViewById(R.id.contentContainer);
    }

    private void setupClickListeners() {
        // Back button click listener
        backButton.setOnClickListener(v -> finish());

        // View cart button click listener
        viewCartButton.setOnClickListener(v -> {
            // Open OrderActivity with restaurant info
            Intent intent = new Intent(RestaurantMenuActivity.this, CheckOutActivity.class);
            intent.putExtra("RESTAURANT_ID", restaurantId);
            intent.putExtra("RESTAURANT_NAME", restaurantName);
            startActivity(intent);
        });

        // Menu tab selection listener
        menuTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterMenuItemsByCategory(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });

        // Favorite button click listener
        favoriteButton.setOnClickListener(v -> toggleFavorite());

        // Show Info button click listener
        showInfoButton.setOnClickListener(v -> {
            Intent intent = new Intent(RestaurantMenuActivity.this, RestaurantInformationActivity.class);
            intent.putExtra("RESTAURANT_ID", restaurantId);
            intent.putExtra("RESTAURANT_NAME", restaurantName);
            startActivity(intent);
        });
    }

    private void setupMenuItemsRecyclerView() {
        menuItemAdapter = new MenuItemAdapter(this, new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        menuItemsRecyclerView.setLayoutManager(layoutManager);
        menuItemsRecyclerView.setAdapter(menuItemAdapter);

        // Set click listeners for menu items
        menuItemAdapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MenuItem menuItem) {
                // Check if item is in cart
                int currentQuantity = orderItemManager.getItemQuantity(menuItem.getId());

                // Launch FoodDetailActivity when a menu item is clicked
                Intent intent = new Intent(RestaurantMenuActivity.this, MenuItemDetailActivity.class);
                intent.putExtra("FOOD_ID", menuItem.getId());
                intent.putExtra("RESTAURANT_ID", restaurantId);
                intent.putExtra("RESTAURANT_NAME", restaurantName);
                intent.putExtra("CURRENT_QUANTITY", currentQuantity);
                startActivityForResult(intent, REQUEST_FOOD_DETAIL);
            }

            @Override
            public void onAddClick(MenuItem menuItem, View view) {
                Log.d(TAG, "onAddClick: Attempting to add item: " + menuItem.getName());

                // Check if menu item has options
                if (menuItem.getAvailableOptions() != null && !menuItem.getAvailableOptions().isEmpty()) {
                    // Show customization dialog if item has options
                    FoodCustomizationDialog dialog = new FoodCustomizationDialog(RestaurantMenuActivity.this, menuItem, (customizedMenuItem, quantity, options, note, totalPrice) -> {
                        // Create a cart item with the customized options
                        OrderItem cartItem = new OrderItem(
                                customizedMenuItem.getId(),
                                restaurantId,
                                customizedMenuItem.getName(),
                                customizedMenuItem.getPrice(), // Base price per item
                                quantity,
                                customizedMenuItem.getCategory(),
                                options,
                                customizedMenuItem.getImageUrl()
                        );

                        // Add to cart using OrderItemManager
                        orderItemManager.addItem(cartItem);

                        // Update specific item in adapter
                        menuItemAdapter.refreshItem(menuItem.getId());

                        // Force update cart button immediately
                        runOnUiThread(() -> updateCartButton());

                        Log.d(TAG, "onAddClick: Item with options added via dialog: " + customizedMenuItem.getName());
                    });
                    dialog.show();
                } else {
                    // Simple item without options - just add 1 to cart
                    OrderItem cartItem = new OrderItem(
                            menuItem.getId(),
                            restaurantId,
                            menuItem.getName(),
                            menuItem.getPrice(),
                            1, // Add 1 quantity
                            menuItem.getCategory(),
                            new ArrayList<>(), // No options
                            menuItem.getImageUrl()
                    );

                    orderItemManager.addItem(cartItem);
                    Log.d(TAG, "onAddClick: New item added directly: " + menuItem.getName());

                    // Update specific item in adapter
                    menuItemAdapter.refreshItem(menuItem.getId());

                    // Force update cart button immediately after adding item
                    runOnUiThread(() -> updateCartButton());
                }
            }

            @Override
            public void onDecreaseClick(MenuItem menuItem, View view) {
                Log.d(TAG, "onDecreaseClick: Attempting to decrease item: " + menuItem.getName());

                // Find the existing item in cart (first match for this menu item)
                OrderItem existingItem = null;
                for (OrderItem cartItem : orderItemManager.getCartItems()) {
                    if (cartItem.getItemId().equals(menuItem.getId()) &&
                            cartItem.getRestaurantId().equals(restaurantId)) {
                        existingItem = cartItem;
                        break;
                    }
                }

                if (existingItem != null) {
                    int newQuantity = existingItem.getQuantity() - 1;

                    if (newQuantity <= 0) {
                        // Remove item completely if quantity becomes 0 or less
                        orderItemManager.removeItem(existingItem);
                        Log.d(TAG, "onDecreaseClick: Item removed from cart: " + menuItem.getName());
                    } else {
                        // Update quantity using setItemQuantity to set exact value
                        orderItemManager.setItemQuantity(existingItem, newQuantity);
                        Log.d(TAG, "onDecreaseClick: Quantity decreased for item: " + menuItem.getName() + " to " + newQuantity);
                    }

                    // Update specific item in adapter
                    menuItemAdapter.refreshItem(menuItem.getId());

                    // Force update cart button immediately after decreasing item
                    runOnUiThread(() -> updateCartButton());
                } else {
                    Log.w(TAG, "onDecreaseClick: No item found in cart for: " + menuItem.getName());
                }
            }
        });
    }

    private void checkAndShowContent() {
        if (isRestaurantDataLoaded && isMenuDataLoaded) {
            // All data is loaded, show content
            loadingProgressBar.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
            
            // Show cart button if there are items
            if (!orderItemManager.getRestaurantItems(restaurantId).isEmpty()) {
                cartButtonLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadRestaurantDetails() {
        databaseReference.child("restaurants").child(restaurantId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get basic restaurant details
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    currentRestaurant = restaurant;
                    restaurant.setId(dataSnapshot.getKey());

                    restaurantName = restaurant.getName();
                    restaurantDescription = restaurant.getDescription();
                    restaurantAddress = restaurant.getAddress();
                    restaurantImageUrl = restaurant.getImageUrl();
                    deliveryFee = restaurant.getDeliveryFee();
                    //Set restaurant name
                    restaurantNameTextView.setText(restaurantName);

                    // Set restaurant description
                    restaurantDescriptionText.setText(restaurantDescription);

//                    // Set address
//                    addressText.setText(restaurantAddress);

                    categoryText.setText(currentRestaurant.getCategory());

                    // Set rating and review count
                    restaurantRating.setRating((float) currentRestaurant.getRating());
                    ratingValueText.setText(String.format("%.1f", currentRestaurant.getRating()));
                    reviewCountText.setText(String.format("(%d đánh giá)", currentRestaurant.getTotalRatings()));

//                    ImageUtils.loadImage( currentRestaurant.getImageUrl(), restImageView,
//                            R.drawable.loading_img, R.drawable.logo2);
                    Glide.with(getApplicationContext())
                            .load(currentRestaurant.getImageUrl())
                            .placeholder(R.drawable.loading_img)
                            .error(R.drawable.logo2)
                            .into(restImageView);

                    isRestaurantDataLoaded = true;
                    checkAndShowContent();
                } else {
                    Toast.makeText(RestaurantMenuActivity.this, "Restaurant not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading restaurant details: " + databaseError.getMessage());
                Toast.makeText(RestaurantMenuActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadRestaurantReviews() {
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

                        // Calculate average rating
                        if (reviewCount > 0) {
                            restaurantRatingValue = totalRating / reviewCount;
                        } else {
                            restaurantRatingValue = 0;
                        }
                        updateRestaurantUI(reviewCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading restaurant reviews: " + error.getMessage());
                    }
                });
    }

    private void updateRestaurantUI(int reviewCount) {

        reviewCountText.setText(String.format("(%d đánh giá)", reviewCount));

        // Set delivery info
        String formattedFee = currencyFormat.format(deliveryFee).replace("₫", "đ");
        deliveryFeeText.setText(formattedFee);
        deliveryTimeText.setText(deliveryTime + " phút");
        deliveryFeeText.setText(currentRestaurant.getAveragePrice());

        // Load restaurant image
    }


    private void loadMenuItems() {
        databaseReference.child("menuItems").orderByChild("restaurantId").equalTo(restaurantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        menuItems.clear();
                        menuItemsByCategory.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            MenuItem menuItem = snapshot.getValue(MenuItem.class);
                            if (menuItem != null) {
                                menuItem.setRestaurantId(restaurantId);
                                menuItem.setId(snapshot.getKey());

                                // Add to the list
                                menuItems.add(menuItem);
                                String category = menuItem.getCategory();
                                if (category == null || category.trim().isEmpty()) {
                                    category = "Khác";
                                }

                                // Add to category map
                                if (!menuItemsByCategory.containsKey(category)) {
                                    menuItemsByCategory.put(category, new ArrayList<>());
                                }
                                menuItemsByCategory.get(category).add(menuItem);
                            }
                        }

                        // Update menu tab layout
                        updateMenuTabs();

                        // Update menu items adapter with all items initially
                        menuItemAdapter.updateData(menuItems);
                        
                        // Mark menu data as loaded
                        isMenuDataLoaded = true;
                        checkAndShowContent();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to load menu items: " + databaseError.getMessage());
                        Toast.makeText(RestaurantMenuActivity.this, "Error loading menu items", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateMenuTabs() {
        menuTabLayout.removeAllTabs();
        
        // Add "All" tab first
        TabLayout.Tab allTab = menuTabLayout.newTab().setText("All");
        menuTabLayout.addTab(allTab);
        
        // Add other category tabs
        for (String category : menuItemsByCategory.keySet()) {
            menuTabLayout.addTab(menuTabLayout.newTab().setText(category));
        }

        // Select "All" tab by default
        if (menuTabLayout.getTabCount() > 0) {
            menuTabLayout.selectTab(menuTabLayout.getTabAt(0));
            // Show all menu items initially
            filterMenuItemsByCategory("All");
        }
    }

    private void filterMenuItemsByCategory(String category) {
        filteredMenuItems.clear();

        if (category.equals("All")) {
            // Show all menu items
            filteredMenuItems.addAll(menuItems);
            // Sort items by category for better organization
            Collections.sort(filteredMenuItems, (item1, item2) -> {
                // First sort by category
                int categoryCompare = item1.getCategory().compareTo(item2.getCategory());
                if (categoryCompare != 0) {
                    return categoryCompare;
                }
                // Then sort by name within same category
                return item1.getName().compareTo(item2.getName());
            });
        } else if (menuItemsByCategory.containsKey(category)) {
            // Show items from specific category
            filteredMenuItems.addAll(menuItemsByCategory.get(category));
            // Sort items by name within category
            Collections.sort(filteredMenuItems, (item1, item2) -> 
                item1.getName().compareTo(item2.getName()));
        }

        menuItemAdapter.updateData(filteredMenuItems);
    }

    // Update the onActivityResult method in RestaurantMenuActivity.java

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOOD_DETAIL && resultCode == RESULT_OK && data != null) {
            // Refresh the menu items list when returning from detail activity
            String returnedRestaurantId = data.getStringExtra("RESTAURANT_ID");
            if (returnedRestaurantId != null && returnedRestaurantId.equals(restaurantId)) {
                // Update cart button
                updateCartButton();

                // Refresh all items in the adapter to show updated quantities
                if (menuItemAdapter != null) {
                    runOnUiThread(() -> {
                        menuItemAdapter.refreshAllItems();
                        Log.d(TAG, "onActivityResult: Refreshed adapter after returning from detail activity");
                    });
                }
            }
        }
    }

    // Also update the onResume method to ensure quantities are always up to date
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the cart button state is updated when the activity resumes
        updateCartButton();

        // Refresh adapter to show current cart quantities
        if (menuItemAdapter != null) {
            menuItemAdapter.refreshAllItems();
            Log.d(TAG, "onResume: Refreshed adapter to show current quantities");
        }
    }

    // Update the onCartUpdated method for better handling
    @Override
    public void onCartUpdated(List<OrderItem> cartItems, double total) {
        Log.d(TAG, "onCartUpdated: Callback received. Current cart size: " + cartItems.size() + ", Total: " + total);

        // Update the cart button
        updateCartButton();

        // Refresh the adapter on the main thread
        runOnUiThread(() -> {
            if (menuItemAdapter != null) {
                menuItemAdapter.refreshAllItems();
                Log.d(TAG, "onCartUpdated: Refreshed adapter with new quantities");
            }
        });
    }

    private void updateCartButton() {
        Log.d(TAG, "updateCartButton: Called");

        // Check if there are items from this restaurant in the cart
        List<OrderItem> restaurantItems = orderItemManager.getRestaurantItems(restaurantId);

        if (!restaurantItems.isEmpty()) {
            // Calculate total quantity and price
            int totalQuantity = 0;
            double total = 0;
            for (OrderItem item : restaurantItems) {
                totalQuantity += item.getQuantity();
                total += item.getItemPrice() * item.getQuantity();
            }

            // Update cart quantity display
            cartQuantityTV.setText(String.valueOf(totalQuantity));

            // Update button text with price
            String formattedTotal = currencyFormat.format(total).replace("₫", "đ");
            viewCartButton.setText("Xem đơn hàng • " + formattedTotal);

            // Only show cart button if content is visible
            if (contentContainer.getVisibility() == View.VISIBLE) {
                cartButtonLayout.setVisibility(View.VISIBLE);
            }
        } else {
            cartButtonLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void checkFavoriteStatus() {
        if (mAuth.getCurrentUser() == null) {
            updateFavoriteButton(false);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        favoritesRef.child(restaurantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isFavorite = snapshot.exists();
                        updateFavoriteButton(isFavorite);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking favorite status: " + error.getMessage());
                    }
                });
    }

    private void toggleFavorite() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userFavoriteRef = favoritesRef.child(restaurantId);
        DatabaseReference restaurantFavoriteCountRef = restaurantRef.child(restaurantId).child("favoriteCount");

        if (isFavorite) {
            // Remove from favorites
            userFavoriteRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Decrease favorite count
                        restaurantFavoriteCountRef.get().addOnSuccessListener(snapshot -> {
                            long currentCount = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                            if (currentCount > 0) {
                                restaurantFavoriteCountRef.setValue(currentCount - 1);
                            }
                        });
                        Toast.makeText(this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Có lỗi xảy ra khi xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add to favorites
            userFavoriteRef.setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        // Increase favorite count
                        restaurantFavoriteCountRef.get().addOnSuccessListener(snapshot -> {
                            long currentCount = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                            restaurantFavoriteCountRef.setValue(currentCount + 1);
                        });
                        Toast.makeText(this, "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Có lỗi xảy ra khi thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateFavoriteButton(boolean isFavorite) {
        this.isFavorite = isFavorite;
        favoriteButton.setImageResource(isFavorite ?
                R.drawable.baseline_favorite_24 :
                R.drawable.baseline_favorite_border_24);
        favoriteButton.setColorFilter(getResources().getColor(
                isFavorite ? R.color.rebecca_purple : android.R.color.darker_gray,
                null));
    }
}
