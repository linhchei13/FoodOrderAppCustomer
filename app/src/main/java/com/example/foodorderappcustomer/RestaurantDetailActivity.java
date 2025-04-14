package com.example.foodorderappcustomer;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapters.MenuItemAdapter;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RestaurantDetailActivity extends AppCompatActivity {
    private static final String TAG = "RestaurantDetailActivity";

    private String restaurantId;
    private String restaurantName;
    private RecyclerView menuItemsRecyclerView;
    private MenuItemAdapter menuItemAdapter;
    private List<MenuItem> menuItems;
    private DatabaseReference databaseReference;
    private TextView restaurantNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Get restaurant ID from intent
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");
        restaurantName = getIntent().getStringExtra("RESTAURANT_NAME");

        if (restaurantId == null) {
            Toast.makeText(this, "Restaurant ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        restaurantNameTextView = findViewById(R.id.restaurantNameTextView);
        restaurantNameTextView.setText(restaurantName);

        menuItemsRecyclerView = findViewById(R.id.menuItemsRecyclerView);
        menuItems = new ArrayList<>();
        menuItemAdapter = new MenuItemAdapter(menuItems);

        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuItemsRecyclerView.setAdapter(menuItemAdapter);

        // Set click listeners for menu items
        menuItemAdapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MenuItem menuItem) {
                // Handle menu item click (show details, etc.)
                Toast.makeText(RestaurantDetailActivity.this,
                        "Selected: " + menuItem.getName(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddClick(MenuItem menuItem) {
                // Handle add to cart
                Toast.makeText(RestaurantDetailActivity.this,
                        "Added to cart: " + menuItem.getName(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Load menu items for this restaurant
        loadMenuItems();
    }

    private void loadMenuItems() {
        // Query menu items for this restaurant
        Query query = databaseReference.child("menu_items").orderByChild("restaurant_id").equalTo(Integer.parseInt(restaurantId));
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                menuItems.clear();

                for (DataSnapshot menuItemSnapshot : dataSnapshot.getChildren()) {
                    String id = menuItemSnapshot.getKey();
                    String name = menuItemSnapshot.child("item_name").getValue(String.class);
                    Double price = menuItemSnapshot.child("price").getValue(Double.class);
                    String category = menuItemSnapshot.child("category").getValue(String.class);
                    Float rating = menuItemSnapshot.child("rating").getValue(Float.class);

                    // Default image based on category
                    int imageResource = getMenuItemImage(category);

                    MenuItem menuItem = new MenuItem(id, name, price, category, rating, imageResource);
                    menuItems.add(menuItem);
                }

                menuItemAdapter.updateData(menuItems);

                if (menuItems.isEmpty()) {
                    Log.d(TAG, "No menu items found for restaurant ID: " + restaurantId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load menu items: " + databaseError.getMessage());
            }
        });
    }

    private int getMenuItemImage(String category) {
        // Return appropriate image based on category
        if (category == null) return R.drawable.nemnuong;

        switch (category.toLowerCase()) {
            case "cơm":
                return R.drawable.comsuon;
            case "phở, bún":
                return R.drawable.buncha;
            case "đồ uống":
                return R.drawable.coffe;
            case "bánh mỳ":
                return R.drawable.banhmy;
            default:
                return R.drawable.nemnuong;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}