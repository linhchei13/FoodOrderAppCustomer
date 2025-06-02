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
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.MenuItemAdapter;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.util.CartManager;
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

public class RestaurantMenuActivity extends AppCompatActivity {
    private static final String TAG = "RestaurantDetailActivity";
    
    // UI Components
    private Toolbar toolbar;
    private ImageButton backButton;
    private CollapsingToolbarLayout collapsingToolbar;
    private RecyclerView menuItemsRecyclerView;
    private ImageView restImageView;
    private TextView restaurantDescriptionText;
    private TextView addressText;
    private TextView cuisineText;
    private TextView deliveryFeeText;
    private TextView deliveryTimeText;
    private RatingBar restaurantRating;
    private TextView ratingValueText;
    private TabLayout menuTabLayout;
    private Button viewCartButton;
    
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
    
    // Firebase
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    
    // Formatting
    private NumberFormat currencyFormat;
    
    // Add CartManager field
    private CartManager cartManager;

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
        cartManager = CartManager.getInstance(this);

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
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        backButton = findViewById(R.id.backButton);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        menuItemsRecyclerView = findViewById(R.id.menuItemsRecyclerView);
        restImageView = findViewById(R.id.restImageView);
        restaurantDescriptionText = findViewById(R.id.restaurantDescriptionText);
        addressText = findViewById(R.id.addressText);
        cuisineText = findViewById(R.id.cuisineText);
        deliveryFeeText = findViewById(R.id.deliveryFeeText);
        deliveryTimeText = findViewById(R.id.deliveryTimeText);
        restaurantRating = findViewById(R.id.restaurantRating);
        ratingValueText = findViewById(R.id.ratingValueText);
        menuTabLayout = findViewById(R.id.menuTabLayout);
        viewCartButton = findViewById(R.id.viewOrderButton);
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
        menuItemAdapter = new MenuItemAdapter(new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        menuItemsRecyclerView.setLayoutManager(layoutManager);
        menuItemsRecyclerView.setAdapter(menuItemAdapter);

        // Set click listeners for menu items
        menuItemAdapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MenuItem menuItem) {
                // Launch FoodDetailActivity when a menu item is clicked
                Intent intent = new Intent(RestaurantMenuActivity.this, FoodDetailActivity.class);
                intent.putExtra("FOOD_ID", menuItem.getId());
                startActivity(intent);
            }
//
            @Override
            public void onAddClick(MenuItem menuItem, View view) {
                Intent intent = new Intent(RestaurantMenuActivity.this, FoodDetailActivity.class);
                intent.putExtra("FOOD_ID", menuItem.getId());
                startActivity(intent);
            }
        });
    }
    
    private void loadRestaurantDetails() {
        databaseReference.child("restaurants").child(restaurantId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get basic restaurant details
                    if (dataSnapshot.hasChild("name")) {
                        restaurantName = dataSnapshot.child("name").getValue(String.class);
                        collapsingToolbar.setTitle(restaurantName);
                    }
                    
                    if (dataSnapshot.hasChild("description")) {
                        restaurantDescription = dataSnapshot.child("description").getValue(String.class);
                    } else {
                        restaurantDescription = generateDefaultDescription(restaurantName, null);
                    }
                    
                    if (dataSnapshot.hasChild("address")) {
                        restaurantAddress = dataSnapshot.child("address").getValue(String.class);
                    } else {
                        restaurantAddress = "Chưa có thông tin địa chỉ";
                    }
                    
                    // Get restaurant rating
                    if (dataSnapshot.hasChild("rating")) {
                        restaurantRatingValue = dataSnapshot.child("rating").getValue(Double.class);
                    } else {
                        restaurantRatingValue = 0;
                    }
                    
                    // Get delivery info
                    if (dataSnapshot.hasChild("delivery_fee")) {
                        deliveryFee = dataSnapshot.child("delivery_fee").getValue(Double.class);
                    } else {
                        deliveryFee = 15000; // Default delivery fee
                    }
                    
                    if (dataSnapshot.hasChild("delivery_time")) {
                        deliveryTime = dataSnapshot.child("delivery_time").getValue(Integer.class);
                    } else {
                        deliveryTime = 30; // Default delivery time (minutes)
                    }
                    
                    // Get cuisine types
                    if (dataSnapshot.hasChild("cuisines")) {
                        for (DataSnapshot cuisineSnapshot : dataSnapshot.child("cuisines").getChildren()) {
                            cuisineTypes.add(cuisineSnapshot.getValue(String.class));
                        }
                    }
                    
                    // Get image URL
                    if (dataSnapshot.hasChild("image_url")) {
                        restaurantImageUrl = dataSnapshot.child("image_url").getValue(String.class);
                    } else if (dataSnapshot.hasChild("imageUrl")) {
                        restaurantImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                    } else if (dataSnapshot.hasChild("image")) {
                        restaurantImageUrl = dataSnapshot.child("image").getValue(String.class);
                    }
                    
                    // Update UI with restaurant details
                    updateRestaurantUI();
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
    
    private void updateRestaurantUI() {
        // Set restaurant description
        restaurantDescriptionText.setText(restaurantDescription);
        
        // Set address
        addressText.setText(restaurantAddress);
        
        // Set rating
        restaurantRating.setRating((float) restaurantRatingValue);
        ratingValueText.setText(String.format("%.1f", restaurantRatingValue));
        
        // Set cuisine types
        if (cuisineTypes != null && !cuisineTypes.isEmpty()) {
            cuisineText.setText(String.join(", ", cuisineTypes));
        } else {
            cuisineText.setText("Chưa phân loại");
        }
        
        // Set delivery info
        String formattedFee = currencyFormat.format(deliveryFee).replace("₫", "đ");
        deliveryFeeText.setText(formattedFee);
        deliveryTimeText.setText(deliveryTime + " phút");
        
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
                    if (snapshot.hasChild("image_url")) {
                        imageUrl = snapshot.child("image_url").getValue(String.class);
                    } else if (snapshot.hasChild("imageUrl")) {
                        imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    } else if (snapshot.hasChild("image")) {
                        imageUrl = snapshot.child("image").getValue(String.class);
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

    // Method to show quantity picker dialog at the position of the clicked view
    public void showQuantityPickerDialog(MenuItem menuItem, View anchorView) {
        // Create dialog with custom style
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_quantity_picker);
        
        // Find views in dialog
        ImageButton decreaseBtn = dialog.findViewById(R.id.decreaseButtonDialog);
        ImageButton increaseBtn = dialog.findViewById(R.id.increaseButtonDialog);
        TextView quantityTv = dialog.findViewById(R.id.quantityTextViewDialog);
        Button cancelBtn = dialog.findViewById(R.id.cancelButton);
        Button confirmBtn = dialog.findViewById(R.id.confirmButton);
        TextView dialogTitleTv = dialog.findViewById(R.id.dialogTitleTextView);
        
        // Set dialog title to include menu item name
        dialogTitleTv.setText("Số lượng: " + menuItem.getName());
        
        // Set initial quantity to 1
        int quantity = 1;
        quantityTv.setText(String.valueOf(quantity));
        
        // Decrease button
        decreaseBtn.setOnClickListener(v -> {
            int currentQty = Integer.parseInt(quantityTv.getText().toString());
            if (currentQty > 1) {
                quantityTv.setText(String.valueOf(currentQty - 1));
            }
        });
        
        // Increase button
        increaseBtn.setOnClickListener(v -> {
            int currentQty = Integer.parseInt(quantityTv.getText().toString());
            quantityTv.setText(String.valueOf(currentQty + 1));
        });
        
        // Make the quantity text clickable to manually enter quantity
        quantityTv.setOnClickListener(v -> {
            // Create an AlertDialog with an EditText for manual entry
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Nhập số lượng");
            
            // Set up the input
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setText(quantityTv.getText().toString());
            builder.setView(input);
            
            // Set up the buttons
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                String inputText = input.getText().toString();
                if (!inputText.isEmpty()) {
                    int inputQty = Integer.parseInt(inputText);
                    // Ensure minimum quantity is 1
                    if (inputQty < 1) inputQty = 1;
                    quantityTv.setText(String.valueOf(inputQty));
                }
            });
            builder.setNegativeButton("Hủy", (dialogInterface, i) -> dialogInterface.cancel());
            
            builder.show();
        });
        
        // Cancel button
        cancelBtn.setOnClickListener(v -> {
            dialog.dismiss();
        });
        
        // Confirm button
        confirmBtn.setOnClickListener(v -> {
            // Get the quantity from dialog
            int selectedQuantity = Integer.parseInt(quantityTv.getText().toString());
            
            // Add to cart
            addToCart(menuItem, selectedQuantity);
            
            dialog.dismiss();
        });
        
        // Show dialog
        dialog.show();
        
        // Position dialog near the anchor view if provided
        if (anchorView != null) {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                // Get screen dimensions
                android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenWidth = displayMetrics.widthPixels;
                int screenHeight = displayMetrics.heightPixels;
                
                // Get location of anchor view on screen
                int[] location = new int[2];
                anchorView.getLocationOnScreen(location);
                
                // Get dialog dimensions
                int dialogWidth = window.getDecorView().getWidth();
                int dialogHeight = window.getDecorView().getHeight();
                
                // If dialog width/height is zero (not yet measured), use estimated values
                if (dialogWidth == 0) dialogWidth = (int)(screenWidth * 0.8);
                if (dialogHeight == 0) dialogHeight = 400; // Estimated height
                
                // Calculate position
                int x = location[0] + (anchorView.getWidth() / 2) - (dialogWidth / 2);
                int y = location[1] - dialogHeight;
                
                // Ensure dialog stays within screen bounds
                if (x < 0) x = 0;
                if (x + dialogWidth > screenWidth) x = screenWidth - dialogWidth;
                if (y < 0) y = 0;
                
                // Set position
                android.view.WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                params.x = x;
                params.y = y;
                
                window.setAttributes(params);
            }
        }
    }
    
    // Method to add a menu item to the cart
    private void addToCart(MenuItem menuItem, int quantity) {
        // Create a cart item
        OrderItem cartItem = new OrderItem(
            menuItem.getId(),
            restaurantId,
            menuItem.getName(),
            menuItem.getPrice(),
            quantity,
            menuItem.getCategory(),
            new ArrayList<Option>(),  // No toppings when adding directly from restaurant menu
            menuItem.getImageUrl()
        );
        
        // Add to cart using CartManager
        cartManager.addItem(cartItem);
        
        // Show success message
        Toast.makeText(this, quantity + " × " + menuItem.getName() + " đã được thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}