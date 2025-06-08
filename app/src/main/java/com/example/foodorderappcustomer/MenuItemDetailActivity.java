package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Adapter.OptionAdapter;
import com.example.foodorderappcustomer.Adapter.OptionGroupAdapter;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.OptionGroup;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MenuItemDetailActivity extends AppCompatActivity implements OptionAdapter.OnToppingSelectedListener, OrderItemManager.OnCartUpdateListener {
    private static final String TAG = "FoodDetailActivity";

    // UI components
    private ImageButton backButton;
    private ImageView foodImageView;
    private TextView foodNameTextView;
    private TextView foodPriceTextView;
    private TextView foodDescriptionTextView;
    private RecyclerView toppingsRecyclerView;
    private ImageButton decreaseButton;
    private ImageButton increaseButton;
    private TextView quantityTextView;
    private TextView totalPriceTextView;
    private MaterialButton addToCartButton;
    private TextView foodCategoryText;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageButton favoriteButton;

    // Data
    private String foodId, restaurantId;
    private String foodName;
    private double foodPrice;
    private String foodDescription;
    private String foodImageUrl;
    private String foodCategory;
    private float foodRating;
    private List<String> foodIngredients;
    private int quantity = 1;
    private double totalPrice;
    private List<OptionGroup> optionGroups = new ArrayList<>();
    private Map<String, Option> selectedToppings = new HashMap<>();
    private String restaurantName;

    // Formatting
    private NumberFormat currencyFormat;

    // Firebase
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    // Add CartManager field
    private OrderItemManager orderItemManager;

    // Favorite functionality
    private boolean isFavorite = false;
    private FirebaseAuth mAuth;
    private DatabaseReference userFavoritesRef;
    private DatabaseReference menuItemRef;
    private DatabaseReference ordersRef;

    // Track if item is already in cart
    private boolean isItemInCart = false;
    private OrderItem existingCartItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Initialize currency format
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize CartManager
        orderItemManager = OrderItemManager.getInstance(this);
        orderItemManager.setOnCartUpdateListener(this); // Register as listener

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        userFavoritesRef = databaseReference.child("users");
        menuItemRef = databaseReference.child("menuItems");
        ordersRef = databaseReference.child("orders");

        // Initialize UI components
        initializeViews();

        // Get food ID and restaurant info from intent
        foodId = getIntent().getStringExtra("FOOD_ID");
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");
        restaurantName = getIntent().getStringExtra("RESTAURANT_NAME");

        // Get current quantity from intent if available
        int currentQuantity = getIntent().getIntExtra("CURRENT_QUANTITY", 1);
        quantity = Math.max(currentQuantity, 1); // Ensure minimum quantity is 1

        if (foodId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if item is already in cart
        checkItemInCart();

        // Load food details
        loadFoodDetails();

        // Set up click listeners
        setupClickListeners();

        // Update quantity display
        updateQuantityDisplay();

        // Check favorite status
        checkFavoriteStatus();

        // Load sales data
        loadSalesData();
    }

    private void checkItemInCart() {
        List<OrderItem> cartItems = orderItemManager.getCartItems();
        for (OrderItem item : cartItems) {
            if (item.getItemId().equals(foodId)) {
                isItemInCart = true;
                existingCartItem = item;
                quantity = item.getQuantity(); // Set quantity to current cart quantity
                break;
            }
        }
        updateAddToCartButton();
    }

    private void updateAddToCartButton() {
        if (isItemInCart) {
            addToCartButton.setText("Cập nhật giỏ hàng");
            addToCartButton.setIcon(getDrawable(R.drawable.ic_shopping_cart));
        } else {
            addToCartButton.setText("Thêm vào giỏ hàng");
            addToCartButton.setIcon(getDrawable(R.drawable.ic_shopping_cart));
        }
    }

    private void initializeViews() {
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        backButton = findViewById(R.id.backButton);
        foodImageView = findViewById(R.id.foodImageView);
        foodNameTextView = findViewById(R.id.foodName);
        foodPriceTextView = findViewById(R.id.foodPriceTextView);
        foodDescriptionTextView = findViewById(R.id.foodDescriptionTextView);

        toppingsRecyclerView = findViewById(R.id.toppingsRecyclerView);
        decreaseButton = findViewById(R.id.decreaseButton);
        increaseButton = findViewById(R.id.increaseButton);
        quantityTextView = findViewById(R.id.quantityTextView);
        totalPriceTextView = findViewById(R.id.totalPriceTextView);
        addToCartButton = findViewById(R.id.addToCartButton);
        favoriteButton = findViewById(R.id.favoriteButton);

        // Set up RecyclerView
        toppingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    private void updateFoodDetails() {
        // Kiểm tra xem Activity có còn tồn tại không trước khi cập nhật UI
        if (isFinishing() || isDestroyed()) {
            Log.d(TAG, "updateFoodDetails: Activity is finishing or destroyed, skipping UI update");
            return;
        }

        foodNameTextView.setText(foodName);
        String formattedPrice = currencyFormat.format(foodPrice).replace("₫", "đ");
        foodPriceTextView.setText(formattedPrice);

        // Set food description
        foodDescriptionTextView.setText(foodDescription);

        // Load food image based on image URL from Firebase or fallback to category image
        try {
            // Kiểm tra context trước khi sử dụng Glide
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this)
                        .load(foodImageUrl)
                        .placeholder(R.drawable.loading_img)
                        .error(R.drawable.logo2)
                        .into(foodImageView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image with Glide: " + e.getMessage());
            // Fallback nếu Glide gặp lỗi
            foodImageView.setImageResource(R.drawable.logo2);
        }

        // Calculate and update total price
        updateTotalPrice();
    }

// Cập nhật phương thức loadFoodDetails() để xử lý callback sau khi Activity bị hủy

    private void loadFoodDetails() {
        // Lưu trữ reference để có thể hủy listener khi cần
        ValueEventListener foodDetailsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Kiểm tra xem Activity có còn tồn tại không
                if (isFinishing() || isDestroyed()) {
                    Log.d(TAG, "onDataChange: Activity is finishing or destroyed, skipping update");
                    return;
                }

                if (dataSnapshot.exists()) {
                    // Get basic food details
                    foodName = dataSnapshot.child("name").getValue(String.class);
                    if (dataSnapshot.hasChild("price")) {
                        foodPrice = dataSnapshot.child("price").getValue(Double.class);
                    }
                    foodCategory = dataSnapshot.child("category").getValue(String.class);

                    // Get rating if available
                    if (dataSnapshot.hasChild("rating")) {
                        foodRating = dataSnapshot.child("rating").getValue(Float.class);
                    } else {
                        foodRating = 0f;
                    }

                    // Get description if available, otherwise use a default
                    if (dataSnapshot.hasChild("description")) {
                        foodDescription = dataSnapshot.child("description").getValue(String.class);
                    }

                    // Get ingredients if available
                    if (dataSnapshot.hasChild("ingredients")) {
                        foodIngredients = new ArrayList<>();
                        for (DataSnapshot ingredientSnapshot : dataSnapshot.child("ingredients").getChildren()) {
                            foodIngredients.add(ingredientSnapshot.getValue(String.class));
                        }
                    }

                    // Get image URL if available - Check different possible field names
                    if (dataSnapshot.hasChild("image_url")) {
                        foodImageUrl = dataSnapshot.child("image_url").getValue(String.class);
                    } else if (dataSnapshot.hasChild("imageUrl")) {
                        foodImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                    } else if (dataSnapshot.hasChild("image")) {
                        foodImageUrl = dataSnapshot.child("image").getValue(String.class);
                    }

                    // Update UI with food details
                    updateFoodDetails();

                    // Load toppings (if available)
                    loadToppings();
                } else {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(MenuItemDetailActivity.this, "Không tìm thấy thông tin món ăn", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load food details: " + databaseError.getMessage());
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(MenuItemDetailActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        };

        // Lưu trữ reference để có thể hủy listener trong onDestroy()
        DatabaseReference foodRef = databaseReference.child("menuItems").child(foodId);
        foodRef.addValueEventListener(foodDetailsListener);

        // Lưu reference để hủy listener khi cần
        foodDetailsValueEventListener = foodDetailsListener;
        foodDetailsReference = foodRef;
    }

    // Thêm biến để lưu trữ reference
    private ValueEventListener foodDetailsValueEventListener;
    private DatabaseReference foodDetailsReference;

    // Thêm phương thức onDestroy() để hủy tất cả các listeners
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Hủy các listeners để tránh memory leak và callback sau khi Activity bị hủy
        if (foodDetailsReference != null && foodDetailsValueEventListener != null) {
            foodDetailsReference.removeEventListener(foodDetailsValueEventListener);
        }

        Log.d(TAG, "onDestroy: All listeners removed");
    }

    private void loadToppings() {
        // Check if there are options in Firebase
        databaseReference.child("menuItems").child(foodId).child("options").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                optionGroups.clear(); // Clear existing groups

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                        String groupName = groupSnapshot.child("name").getValue(String.class);
                        Boolean requiredBoolean = groupSnapshot.child("require").getValue(Boolean.class);
                        boolean groupRequired = requiredBoolean != null ? requiredBoolean : false;

                        // For maxSelections, if 'require' is true, and it's a single choice type (like Size), assume 1.
                        // Otherwise, assume 0 for multiple selections (or no limit).
                        // If your database has a 'maxSelections' field, you should retrieve it here.
                        int maxSelections = 0;
                        if (groupRequired && groupName != null && groupName.equalsIgnoreCase("size")) { // Example inference for single selection
                            maxSelections = 1;
                        }

                        OptionGroup optionGroup = new OptionGroup(groupSnapshot.getKey(), groupName, maxSelections, groupRequired);

                        DataSnapshot choicesSnapshot = groupSnapshot.child("choices");
                        List<Option> optionsForGroup = new ArrayList<>();
                        for (DataSnapshot choiceSnapshot : choicesSnapshot.getChildren()) {
                            String optionName = choiceSnapshot.child("name").getValue(String.class);
                            double optionPrice = 0;
                            if (choiceSnapshot.hasChild("price")) {
                                optionPrice = choiceSnapshot.child("price").getValue(Double.class);
                            }
                            String optionId = choiceSnapshot.getKey();
                            optionsForGroup.add(new Option(optionId, optionName, optionPrice));
                        }
                        optionGroup.setOptions(optionsForGroup);
                        optionGroups.add(optionGroup);
                    }
                }

                // Update toppings RecyclerView with OptionGroupAdapter
                OptionGroupAdapter adapter = new OptionGroupAdapter(MenuItemDetailActivity.this, optionGroups, MenuItemDetailActivity.this);
                toppingsRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load options: " + databaseError.getMessage());
                // If loading fails, still set an empty adapter
                OptionGroupAdapter adapter = new OptionGroupAdapter(MenuItemDetailActivity.this, new ArrayList<>(), MenuItemDetailActivity.this);
                toppingsRecyclerView.setAdapter(adapter);
            }
        });
    }

    private void updateQuantityDisplay() {
        quantityTextView.setText(String.valueOf(quantity));
        updateTotalPrice();
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Decrease quantity button
        decreaseButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityWithAnimation(quantityTextView, String.valueOf(quantity));
                updateTotalPrice();
            }
        });

        // Increase quantity button
        increaseButton.setOnClickListener(v -> {
            quantity++;
            updateQuantityWithAnimation(quantityTextView, String.valueOf(quantity));
            updateTotalPrice();
        });

        // Quantity text view (allow manual entry)
        quantityTextView.setOnClickListener(v -> {
            showQuantityInputDialog();
        });

        // Add to cart button
        addToCartButton.setOnClickListener(v -> addToCart());

        // Favorite button click listener
        favoriteButton.setOnClickListener(v -> toggleFavorite());
    }

    // Helper method to animate quantity changes
    private void updateQuantityWithAnimation(TextView textView, String newValue) {
        // Create a scale animation
        android.view.animation.ScaleAnimation scaleAnimation = new android.view.animation.ScaleAnimation(
                1.0f, 1.3f, // Start and end X scale
                1.0f, 1.3f, // Start and end Y scale
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y
        );
        scaleAnimation.setDuration(150);

        // Create a scale down animation
        android.view.animation.ScaleAnimation scaleDownAnimation = new android.view.animation.ScaleAnimation(
                1.3f, 1.0f, // Start and end X scale
                1.3f, 1.0f, // Start and end Y scale
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y
        );
        scaleDownAnimation.setDuration(150);

        // Set what happens when animation ends
        scaleAnimation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {}

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                // Update the text and start the scale down animation
                textView.setText(newValue);
                textView.startAnimation(scaleDownAnimation);
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });

        // Start the animation
        textView.startAnimation(scaleAnimation);
    }

    private void updateTotalPrice() {
        // Calculate base price
        totalPrice = foodPrice * quantity;

        // Add topping prices from all selected options in option groups
        for (com.example.foodorderappcustomer.Models.OptionGroup group : optionGroups) {
            for (com.example.foodorderappcustomer.Models.Option option : group.getSelectedOptions()) {
                totalPrice += option.getPrice() * quantity;
            }
        }

        // Format and display total price
        String formattedPrice = currencyFormat.format(totalPrice).replace("₫", "đ");
        totalPriceTextView.setText(formattedPrice);
    }

    private void addToCart() {
        if (foodId == null || foodName == null ) {
            Toast.makeText(this, "Lỗi: Thông tin món ăn không đầy đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create order item
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(foodId);
        orderItem.setItemName(foodName);
        orderItem.setItemPrice(foodPrice);
        orderItem.setQuantity(quantity);
        orderItem.setRestaurantId(restaurantId);

        // Add selected toppings
        if (selectedToppings != null && !selectedToppings.isEmpty()) {
            orderItem.setToppings(new ArrayList<>(selectedToppings.values()));
        }

        // Calculate total price including toppings
        double totalPrice = foodPrice * quantity;
        if (selectedToppings != null) {
            for (Option topping : selectedToppings.values()) {
                totalPrice += topping.getPrice() * quantity;
            }
        }
        orderItem.setTotalPrice(totalPrice);

        // Add to cart (now supports multiple restaurants)
        orderItemManager.addItem(orderItem, this);

        // Update button text
        updateAddToCartButton();
    }

    private void addNewItemToCart() {
        // Tạo danh sách topping đã chọn
        List<Option> selectedToppingsList = new ArrayList<>(selectedToppings.values());

        // Tạo món ăn mới trong giỏ hàng
        OrderItem cartItem = new OrderItem(
                foodId,
                restaurantId,
                foodName,
                foodPrice,
                quantity,
                foodCategory,
                selectedToppingsList,
                foodImageUrl
        );

        // Thêm vào giỏ hàng
        orderItemManager.addItem(cartItem);
        Toast.makeText(this, foodName + " đã được thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCartUpdated(List<OrderItem> cartItems, double total) {
        // Update cart status when cart changes
        checkItemInCart();
    }

    @Override
    public void onToppingSelected(Option option, boolean isSelected) {
        if (isSelected) {
            selectedToppings.put(option.getId(), option);
        } else {
            selectedToppings.remove(option.getId());
        }
        updateTotalPrice();
    }

    // Show dialog for manual quantity input
    private void showQuantityInputDialog() {
        // Create an AlertDialog with an EditText for manual entry
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Nhập số lượng");

        // Set up the input
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(quantity));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String inputText = input.getText().toString();
            if (!inputText.isEmpty()) {
                int newQuantity = Integer.parseInt(inputText);
                // Ensure minimum quantity is 1
                if (newQuantity < 1) newQuantity = 1;
                quantity = newQuantity;
                quantityTextView.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void checkFavoriteStatus() {
        if (mAuth.getCurrentUser() == null) {
            updateFavoriteButton(false);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        userFavoritesRef.child(userId).child("favorites_menu").child(foodId)
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
        DatabaseReference userFavoriteRef = userFavoritesRef.child(userId).child("favorites_menu").child(foodId);
        DatabaseReference menuItemFavoriteCountRef = menuItemRef.child(foodId).child("favorites");

        if (isFavorite) {
            // Remove from favorites
            userFavoriteRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Decrease favorite count
                        menuItemFavoriteCountRef.get().addOnSuccessListener(snapshot -> {
                            long currentCount = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                            if (currentCount > 0) {
                                menuItemFavoriteCountRef.setValue(currentCount - 1);
                            }
                        });
                        Toast.makeText(this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Có lỗi xảy ra khi xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Create favorite data with timestamp and menu item info
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("timestamp", System.currentTimeMillis());
            favoriteData.put("menuItemId", foodId);
            favoriteData.put("restaurantId", restaurantId);
            favoriteData.put("name", foodName);
            favoriteData.put("price", foodPrice);
            favoriteData.put("imageUrl", foodImageUrl);
            favoriteData.put("category", foodCategory);

            // Add to favorites
            userFavoriteRef.setValue(favoriteData)
                    .addOnSuccessListener(aVoid -> {
                        // Increase favorite count
                        menuItemFavoriteCountRef.get().addOnSuccessListener(snapshot -> {
                            long currentCount = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                            menuItemFavoriteCountRef.setValue(currentCount + 1);
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

    private void loadSalesData() {
        // Query orders where status is "completed" and contains this menu item
        ordersRef.orderByChild("status").equalTo("completed")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int totalSales = 0;

                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            // Get order items
                            DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                            for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                String itemId = itemSnapshot.child("itemId").getValue(String.class);
                                if (itemId != null && itemId.equals(foodId)) {
                                    // Get quantity of this item in the order
                                    Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                    if (quantity != null) {
                                        totalSales += quantity;
                                    }
                                }
                            }
                        }

                        // Update sales count in database
                        menuItemRef.child(foodId).child("sales")
                                .setValue(totalSales)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Sales count updated successfully");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating sales count: " + e.getMessage());
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading sales data: " + error.getMessage());
                    }
                });
    }
    private void finishWithResult() {
        // Trở về màn hình menu nhà hàng với kết quả
        Intent resultIntent = new Intent();
        resultIntent.putExtra("RESTAURANT_ID", restaurantId);
        resultIntent.putExtra("RESTAURANT_NAME", restaurantName);
        resultIntent.putExtra("ITEM_UPDATED", true); // Flag to indicate item was updated
        resultIntent.putExtra("FOOD_ID", foodId); // Include food ID for specific updates
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // Also override onBackPressed to ensure result is sent
    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("RESTAURANT_ID", restaurantId);
        resultIntent.putExtra("RESTAURANT_NAME", restaurantName);
        resultIntent.putExtra("ITEM_UPDATED", false);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }
}