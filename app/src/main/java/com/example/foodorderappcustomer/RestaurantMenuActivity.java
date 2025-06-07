package com.example.foodorderappcustomer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.MenuItemAdapter;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.example.foodorderappcustomer.util.FoodCustomizationDialog;
import com.example.foodorderappcustomer.util.ImageUtils;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private TextView deliveryTimeText;
    private RatingBar restaurantRating;
    private TextView ratingValueText;
    private TextView reviewCountText;
    private TabLayout menuTabLayout;
    private Button viewCartButton;
    private LinearLayout cartButtonLayout;
    private TextView priceTV;
    
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

        // Get restaurant ID from intent
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");
        restaurantName = getIntent().getStringExtra("RESTAURANT_NAME");
        
        if (restaurantId == null) {
            Toast.makeText(this, "Restaurant ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
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
        
        // Load restaurant details
        loadRestaurantDetails();
        
        // Load menu items for this restaurant
        loadMenuItems();
        
        // Setup click listeners
        setupClickListeners();

        // Initial update of cart button
        updateCartButton();
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
        priceTV = findViewById(R.id.priceTV);
    }
    
    private void setupClickListeners() {
        // Back button click listener
        backButton.setOnClickListener(v -> finish());
        
        // View cart button click listener
        viewCartButton.setOnClickListener(v -> {
            // Open OrderActivity with restaurant info
            Intent intent = new Intent(RestaurantMenuActivity.this,OrderActivity.class);
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
                int currentQuantity = 0;
                for (OrderItem cartItem : orderItemManager.getCartItems()) {
                    if (cartItem.getItemId().equals(menuItem.getId())) {
                        currentQuantity = cartItem.getQuantity();
                        break;
                    }
                }

                // Launch FoodDetailActivity when a menu item is clicked
                Intent intent = new Intent(RestaurantMenuActivity.this, FoodDetailActivity.class);
                intent.putExtra("FOOD_ID", menuItem.getId());
                intent.putExtra("RESTAURANT_ID", restaurantId);
                intent.putExtra("RESTAURANT_NAME", restaurantName);
                intent.putExtra("CURRENT_QUANTITY", currentQuantity);
                startActivityForResult(intent, REQUEST_FOOD_DETAIL);
            }

            @Override
            public void onAddClick(MenuItem menuItem, View view) {
                // Check if menu item has options
                if (menuItem.getAvailableOptions() != null && !menuItem.getAvailableOptions().isEmpty()) {
                    // Show customization dialog if item has options
                    FoodCustomizationDialog dialog = new FoodCustomizationDialog(RestaurantMenuActivity.this, menuItem, (customizedMenuItem, quantity, options, note, totalPrice) -> {
                        // Create a cart item with the customized options
                        OrderItem cartItem = new OrderItem(
                            customizedMenuItem.getId(),
                            restaurantId,
                            customizedMenuItem.getName(),
                            totalPrice / quantity, // Price per item
                            quantity,
                            customizedMenuItem.getCategory(),
                            options,
                            customizedMenuItem.getImageUrl()
                        );
                        
                        // Add to cart using OrderItemManager
                        orderItemManager.addItem(cartItem);
                    });
                    dialog.show();
                } else {
                    // Check if item is already in cart
                    OrderItem existingItem = null;
                    for (OrderItem cartItem : orderItemManager.getCartItems()) {
                        if (cartItem.getItemId().equals(menuItem.getId())) {
                            existingItem = cartItem;
                            break;
                        }
                    }

                    if (existingItem != null) {
                        // Update quantity if item exists
                        orderItemManager.updateItemQuantity(existingItem, existingItem.getQuantity() + 1);
                    } else {
                        // Add new item to cart
                    OrderItem cartItem = new OrderItem(
                        menuItem.getId(),
                        restaurantId,
                        menuItem.getName(),
                        menuItem.getPrice(),
                        1, // Default quantity
                        menuItem.getCategory(),
                        new ArrayList<>(), // No options
                        menuItem.getImageUrl()
                    );
                    orderItemManager.addItem(cartItem);
                    }
                }
            }
        });
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
                    
                    // Load restaurant reviews to calculate rating
                    loadRestaurantReviews();
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
                    
                    // Update UI with new rating and review count
                    updateRestaurantUI(reviewCount);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading restaurant reviews: " + error.getMessage());
                }
            });
    }
    
    private void updateRestaurantUI(int reviewCount) {
        //Set restaurant name
        restaurantNameTextView.setText(restaurantName);

        // Set restaurant description
        restaurantDescriptionText.setText(restaurantDescription);
        
        // Set address
        addressText.setText(restaurantAddress);
        
        // Set rating and review count
        restaurantRating.setRating((float) restaurantRatingValue);
        ratingValueText.setText(String.format("%.1f", restaurantRatingValue));
        reviewCountText.setText(String.format("(%d đánh giá)", reviewCount));
        
        // Set delivery info
        String formattedFee = currencyFormat.format(deliveryFee).replace("₫", "đ");
        deliveryFeeText.setText(formattedFee);
        deliveryTimeText.setText(deliveryTime + " phút");
        deliveryFeeText.setText(currentRestaurant.getAveragePrice());
        
        // Load restaurant image
        loadRestaurantImage();
    }
    
    private void loadRestaurantImage() {
        // Use the ImageUtils class to load the image
        ImageUtils.loadImage(
            restaurantImageUrl, 
            restImageView, 
            R.drawable.bg,
            R.drawable.logo2
        );
    }

    private void loadMenuItems() {
        databaseReference.child("menuItems").orderByChild("restaurantId").equalTo(restaurantId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                menuItems.clear();
                menuItemsByCategory.clear();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String id = snapshot.getKey();
                    String name = snapshot.child("name").getValue(String.class);
                    double price = 0;
                    if (snapshot.hasChild("price")) {
                        price = snapshot.child("price").getValue(Double.class);
                    }
                    
                    String category = snapshot.child("category").getValue(String.class);
                    if (category == null) {
                        category = "Khác";
                    }
                    
                    // Get rating if available
                    float rating = 0f;
                    if (snapshot.hasChild("rating")) {
                        rating = snapshot.child("rating").getValue(Float.class);
                    }
                    
                    // Get description if available
                    String description = "";
                    if (snapshot.hasChild("description")) {
                        description = snapshot.child("description").getValue(String.class);
                    } else {
                        description = generateDefaultDescription(name, category);
                    }
                    
                    // Get image URL if available
                    String imageUrl = null;
                    if (snapshot.hasChild("imageUrl")) {
                        imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    }

                    int sales= 0;
                    if (snapshot.hasChild("sales")) {
                        sales = snapshot.child("sales").getValue(int.class);
                    }

                    // Create menu item
                    MenuItem menuItem = new MenuItem(id, name, price, category, rating, description, imageUrl, sales);
                    menuItem.setRestaurantId(restaurantId);
                    
                    // Add to the list
                    menuItems.add(menuItem);
                    
                    // Add to category map
                    if (!menuItemsByCategory.containsKey(category)) {
                        menuItemsByCategory.put(category, new ArrayList<>());
                    }
                    menuItemsByCategory.get(category).add(menuItem);
                }
                
                // Update menu tab layout
                updateMenuTabs();
                
                // Update menu items adapter
                menuItemAdapter.updateData(menuItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load menu items: " + databaseError.getMessage());
                Toast.makeText(RestaurantMenuActivity.this, "Error loading menu items", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void filterMenuItemsByCategory(String category) {
        filteredMenuItems.clear();
        
        if (category.equals("All")) {
            filteredMenuItems.addAll(menuItems);
        } else if (menuItemsByCategory.containsKey(category)) {
            filteredMenuItems.addAll(menuItemsByCategory.get(category));
        }
        
        menuItemAdapter.updateData(filteredMenuItems);
    }

    private String generateDefaultDescription(String name, String category) {
        // Generate a default description based on the food name and category
        if (category != null) {
            switch (category.toLowerCase()) {
                case "phở, bún":
                    return name + " được chế biến từ nguyên liệu tươi ngon, nước dùng đậm đà, thơm ngon.";
                case "cơm":
                    return name + " được nấu từ gạo thơm ngon, kèm theo các món ăn phụ đặc sắc.";
                case "đồ uống":
                    return name + " thơm ngon, giải khát, được pha chế từ nguyên liệu tươi mới.";
                case "bánh mỳ":
                    return name + " giòn tan, nhân đầy đặn với các nguyên liệu tươi ngon.";
                default:
                    return name + " là món ăn đặc trưng của nhà hàng, được chế biến từ nguyên liệu tươi ngon.";
            }
        }
        return name + " là món ăn đặc trưng của nhà hàng, được chế biến từ nguyên liệu tươi ngon.";
    }

    private void updateMenuTabs() {
        menuTabLayout.removeAllTabs();
        for (String category : menuItemsByCategory.keySet()) {
            menuTabLayout.addTab(menuTabLayout.newTab().setText(category));
        }
        if (menuTabLayout.getTabCount() > 0) {
            menuTabLayout.addTab(menuTabLayout.newTab().setText("All"), 0);
            menuTabLayout.selectTab(menuTabLayout.getTabAt(0));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOOD_DETAIL && resultCode == RESULT_OK && data != null) {
            // Refresh the menu items list if needed
            String returnedRestaurantId = data.getStringExtra("RESTAURANT_ID");
            if (returnedRestaurantId != null && returnedRestaurantId.equals(restaurantId)) {
                // We no longer call updateCartButton here directly as it's handled by the listener
                // updateCartButton(); 
            }
        }
    }

    private void updateCartButton() {
        Log.d(TAG, "updateCartButton: Called");
        Log.d(TAG, "updateCartButton: Restaurant ID: " + restaurantId);

        // Check if there are items from this restaurant in the cart
        List<OrderItem> restaurantItems = orderItemManager.getRestaurantItems(restaurantId);
        if (!restaurantItems.isEmpty()) {
            Log.d(TAG, "updateCartButton: Restaurant items found, size: " + restaurantItems.size());
            cartButtonLayout.setVisibility(View.VISIBLE);
            Log.d(TAG, "updateCartButton: Setting cartButtonLayout to VISIBLE");
            double total = orderItemManager.getRestaurantTotal(restaurantId);
            String formattedTotal = currencyFormat.format(total).replace("₫", "đ");
            priceTV.setText(formattedTotal);
        } else {
            Log.d(TAG, "updateCartButton: No restaurant items found");
            cartButtonLayout.setVisibility(View.GONE);
            Log.d(TAG, "updateCartButton: Setting cartButtonLayout to GONE");
        }
    }

    @Override
    public void onCartUpdated(List<OrderItem> cartItems, double total) {
        // This callback is triggered whenever the cart is updated in OrderItemManager
        updateCartButton();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}