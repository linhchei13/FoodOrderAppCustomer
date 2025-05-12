package com.example.foodorderappcustomer;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.OptionAdapter;
import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.util.CartManager;
import com.example.foodorderappcustomer.util.ImageUtils;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
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

public class FoodDetailActivity extends AppCompatActivity implements OptionAdapter.OnToppingSelectedListener {
    private static final String TAG = "FoodDetailActivity";

    // UI components
    private ImageButton backButton;
    private ImageView foodImageView;
    private TextView foodNameTextView;
    private TextView foodPriceTextView;
    private TextView foodDescriptionTextView;
    private TextView ingredientsTextView;
    private RecyclerView toppingsRecyclerView;
    private ImageButton decreaseButton;
    private ImageButton increaseButton;
    private TextView quantityTextView;
    private TextView totalPriceTextView;
    private MaterialButton addToCartButton;
    private RatingBar foodRatingBar;
    private TextView foodRatingText;
    private TextView foodCategoryText;
    private CollapsingToolbarLayout collapsingToolbar;

    // Data
    private String foodId;
    private String foodName;
    private double foodPrice;
    private String foodDescription;
    private String foodImageUrl;
    private String foodCategory;
    private float foodRating;
    private List<String> foodIngredients;
    private int quantity = 1;
    private double totalPrice;
    private List<Option> options = new ArrayList<>();
    private Map<String, Option> selectedToppings = new HashMap<>();

    // Formatting
    private NumberFormat currencyFormat;

    // Firebase
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    // Add CartManager field
    private CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        // Initialize currency format
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize CartManager
        cartManager = CartManager.getInstance(this);

        // Initialize UI components
        initializeViews();

        // Get food ID from intent
        foodId = getIntent().getStringExtra("FOOD_ID");
        if (foodId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load food details
        loadFoodDetails();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        backButton = findViewById(R.id.backButton);
        foodImageView = findViewById(R.id.foodImageView);
        foodNameTextView = findViewById(R.id.foodNameTextView);
        foodPriceTextView = findViewById(R.id.foodPriceTextView);
        foodDescriptionTextView = findViewById(R.id.foodDescriptionTextView);
        foodRatingBar = findViewById(R.id.foodRatingBar);
        foodRatingText = findViewById(R.id.foodRatingText);

        ingredientsTextView = findViewById(R.id.ingredientsTextView);
        toppingsRecyclerView = findViewById(R.id.toppingsRecyclerView);
        decreaseButton = findViewById(R.id.decreaseButton);
        increaseButton = findViewById(R.id.increaseButton);
        quantityTextView = findViewById(R.id.quantityTextView);
        totalPriceTextView = findViewById(R.id.totalPriceTextView);
        addToCartButton = findViewById(R.id.addToCartButton);

        // Set up RecyclerView
        toppingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadFoodDetails() {
        databaseReference.child("menuItems").child(foodId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
                    Toast.makeText(FoodDetailActivity.this, "Không tìm thấy thông tin món ăn", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load food details: " + databaseError.getMessage());
                Toast.makeText(FoodDetailActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateFoodDetails() {
        // Set food name in the collapsing toolbar
//        collapsingToolbar.setTitle(foodName);
        
        // Set food name
        foodNameTextView.setText(foodName);

        // Set category
        // Set rating
        foodRatingBar.setRating(foodRating);
        foodRatingText.setText(String.format("%.1f", foodRating));

        // Format and set food price
        String formattedPrice = currencyFormat.format(foodPrice).replace("₫", "đ");
        foodPriceTextView.setText(formattedPrice);

        // Set food description
        foodDescriptionTextView.setText(foodDescription);
        
        // Set ingredients
        if (foodIngredients != null && !foodIngredients.isEmpty()) {
            ingredientsTextView.setText(String.join(", ", foodIngredients));
        } else {
            ingredientsTextView.setText("Không có thông tin");
        }

        // Load food image based on image URL from Firebase or fallback to category image
        loadFoodImage();

        // Calculate and update total price
        updateTotalPrice();
    }

    private void loadFoodImage() {
        // Use the ImageUtils class to load the image
        ImageUtils.loadImage(
            this,
            foodImageUrl, 
            foodImageView, 
            R.drawable.edit_text,
            R.drawable.ic_restaurant
        );
    }


    private void loadToppings() {
        // Check if there are toppings in Firebase
        databaseReference.child("toppings").child(foodId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                options.clear();

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    // Load toppings from Firebase
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String id = snapshot.getKey();
                        String name = snapshot.child("name").getValue(String.class);
                        double price = snapshot.child("price").getValue(Double.class);

                        Option option = new Option(id, name, price);
                        options.add(option);
                    }
                } else {
                    // Create sample toppings based on food category
                    createSampleToppings();
                }

                // Update toppings RecyclerView
                OptionAdapter adapter = new OptionAdapter(options, FoodDetailActivity.this);
                toppingsRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load toppings: " + databaseError.getMessage());
                // Create sample toppings as fallback
                createSampleToppings();

                // Update toppings RecyclerView
                OptionAdapter adapter = new OptionAdapter(options, FoodDetailActivity.this);
                toppingsRecyclerView.setAdapter(adapter);
            }
        });
    }

    private void createSampleToppings() {
        // Create sample toppings based on food category
        if (foodCategory != null) {
            switch (foodCategory.toLowerCase()) {
                case "phở, bún":
                    options.add(new Option("t1", "Thêm thịt bò", 15000));
                    options.add(new Option("t2", "Thêm hành", 5000));
                    options.add(new Option("t3", "Thêm nước dùng", 10000));
                    break;
                case "cơm":
                    options.add(new Option("t1", "Thêm sườn", 20000));
                    options.add(new Option("t2", "Thêm trứng", 5000));
                    options.add(new Option("t3", "Thêm cơm", 10000));
                    break;
                case "đồ uống":
                    options.add(new Option("t1", "Thêm đường", 0));
                    options.add(new Option("t2", "Thêm trân châu", 5000));
                    options.add(new Option("t3", "Size lớn", 10000));
                    break;
                case "bánh mỳ":
                    options.add(new Option("t1", "Thêm pate", 5000));
                    options.add(new Option("t2", "Thêm thịt", 10000));
                    options.add(new Option("t3", "Thêm trứng", 5000));
                    break;
                default:
                    options.add(new Option("t1", "Topping 1", 5000));
                    options.add(new Option("t2", "Topping 2", 10000));
                    break;
            }
        } else {
            options.add(new Option("t1", "Topping 1", 5000));
            options.add(new Option("t2", "Topping 2", 10000));
        }
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

        // Add topping prices
        for (Option option : selectedToppings.values()) {
            totalPrice += option.getPrice() * quantity;
        }

        // Format and display total price
        String formattedPrice = currencyFormat.format(totalPrice).replace("₫", "đ");
        totalPriceTextView.setText(formattedPrice);
    }

    private void addToCart() {
        // Create a list of selected toppings
        List<Option> selectedToppingsList = new ArrayList<>(selectedToppings.values());
        
        // Create a cart item using the CartItem class
        CartItem cartItem = new CartItem(
            foodId,
            "", // restaurantId (can get from context if needed)
            foodName,
            foodPrice,
            quantity,
            foodCategory, 
            selectedToppingsList,
            foodImageUrl
        );
        
        // Add to cart using CartManager
        cartManager.addItem(cartItem);
        
        // Show success message
        Toast.makeText(this, foodName + " đã được thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        
        // Close the activity
        finish();
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
}