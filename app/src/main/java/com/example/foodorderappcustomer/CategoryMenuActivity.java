package com.example.foodorderappcustomer;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.RestaurantAdapter;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CategoryMenuActivity extends AppCompatActivity {
    private static final String TAG = "CategoryMenuActivity";

    private String categoryName;
    private TextView categoryNameTV;
    private RecyclerView restaurantRv;
    private ImageButton backBtn;

    private RestaurantAdapter restaurantAdapter;

    private List<Restaurant> restaurantList;

    private DatabaseReference databaseRef;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_menu);
        initViews();

        setRestaurantAdapter();
        loadRestaurants();
    }

    private void initViews() {
        categoryNameTV = findViewById(R.id.categoryTitleTextView);
        restaurantRv = findViewById(R.id.menuItemsRecyclerView);
        backBtn = findViewById(R.id.backButton);
        restaurantList = new ArrayList<>();
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        restaurantAdapter = new RestaurantAdapter(restaurantList);
        databaseRef = FirebaseDatabase.getInstance().getReference();
        categoryNameTV.setText(categoryName);
    }

    private void initListeners() {
        backBtn.setOnClickListener(v -> finish());
    }

    private void setRestaurantAdapter() {
        restaurantAdapter = new RestaurantAdapter(restaurantList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        restaurantRv.setLayoutManager(layoutManager);
        restaurantRv.setAdapter(restaurantAdapter);
    }


    private void loadRestaurants() {
        databaseRef.child("restaurants").orderByChild("category").equalTo(categoryName)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        restaurantList.clear();
                        for(DataSnapshot res: snapshot.getChildren()){
                            String id = res.getKey();
                            Restaurant restaurant = res.getValue(Restaurant.class);
                            restaurant.setId(id);
                            restaurantList.add(restaurant);
                        }
                        restaurantAdapter.updateData(restaurantList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}