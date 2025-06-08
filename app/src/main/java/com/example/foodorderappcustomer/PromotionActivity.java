package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.PromotionAdapter2;
import com.example.foodorderappcustomer.Models.Promotion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromotionActivity extends AppCompatActivity implements PromotionAdapter2.OnPromotionSelectedListener {

    private static final String TAG = "PromotionActivity";

    // UI Components
    private ImageButton backButton;
    private EditText searchEditText;
    private RecyclerView promotionsRecyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;

    // Data
    private String restaurantId;
    private double orderTotal;
    private List<Promotion> promotionList;
    private PromotionAdapter2 promotionAdapter;
    private DatabaseReference databaseReference;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion);

        // Get data from intent
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");
        orderTotal = getIntent().getDoubleExtra("ORDER_TOTAL", 0);

        if (restaurantId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin nhà hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        initializeViews();
        setupClickListeners();

        // Initialize data
        promotionList = new ArrayList<>();
        promotionAdapter = new PromotionAdapter2(this, promotionList, orderTotal, this);
        promotionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        promotionsRecyclerView.setAdapter(promotionAdapter);

        // Load promotions
        loadPromotions();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        searchEditText = findViewById(R.id.searchEditText);
        promotionsRecyclerView = findViewById(R.id.promotionsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                promotionAdapter.filter(s.toString());
                updateEmptyView();
            }
        });
    }

    private void loadPromotions() {
        showLoading(true);

        // Query promotions for this restaurant
        Query query = databaseReference.child("promotions")
                .orderByChild("restaurantId")
                .equalTo(restaurantId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                promotionList.clear();
                Date currentDate = new Date();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Promotion promotion = snapshot.getValue(Promotion.class);
                        if (promotion != null) {
                            promotion.setId(snapshot.getKey());

                            // Check if promotion is expired
                            try {
                                Date endDate = dateFormat.parse(promotion.getEndDate());
                                if (endDate != null && endDate.before(currentDate)) {
                                    promotion.setExpired(true);
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing date: " + e.getMessage());
                            }

                            promotionList.add(promotion);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing promotion: " + e.getMessage());
                    }
                }

                promotionAdapter = new PromotionAdapter2(PromotionActivity.this, promotionList, orderTotal, PromotionActivity.this);
                promotionsRecyclerView.setAdapter(promotionAdapter);
                
                showLoading(false);
                updateEmptyView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading promotions: " + databaseError.getMessage());
                Toast.makeText(PromotionActivity.this, "Lỗi tải mã khuyến mãi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                showLoading(false);
                updateEmptyView();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        promotionsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyView() {
        if (promotionAdapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPromotionSelected(Promotion promotion) {
        // Return selected promotion to calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("PROMO_CODE", promotion.getPromoCode());
        resultIntent.putExtra("PROMO_ID", promotion.getId());
        
        // Calculate discount amount based on type
        double discountAmount = 0;
        if ("percentage".equals(promotion.getDiscountType())) {
            discountAmount = orderTotal * (promotion.getDiscountAmount() / 100);
            // Apply max discount if applicable
            if (promotion.getMaxDiscountAmount() > 0 && discountAmount > promotion.getMaxDiscountAmount()) {
                discountAmount = promotion.getMaxDiscountAmount();
            }
        } else { // fixed_amount
            discountAmount = promotion.getDiscountAmount();
        }
        
        resultIntent.putExtra("DISCOUNT_AMOUNT", discountAmount);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
