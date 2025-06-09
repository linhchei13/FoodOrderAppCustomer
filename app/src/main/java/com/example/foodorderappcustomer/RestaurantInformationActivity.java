package com.example.foodorderappcustomer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.OpeningHoursAdapter;
import com.example.foodorderappcustomer.Adapter.ReviewAdapter;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.Models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RestaurantInformationActivity extends AppCompatActivity {

    private static final String TAG = "RestaurantInfoActivity";

    // UI components
    private ImageButton backButton;
    private TextView restaurantNameTextView;
    private TextView restaurantAddressTextView;
    private TextView restaurantDescriptionTextView;
    private RatingBar restaurantRatingBar;
    private TextView ratingValueTextView;
    private TextView reviewCountTextView;
    private RecyclerView operatingHoursRecyclerView;
    private RecyclerView reviewsRecyclerView;

    // Data
    private String restaurantId;
    private String restaurantName;
    private DatabaseReference databaseReference;
    private List<Review> reviewList;
    private Map<String, User> userMap; // userId -> User (for review adapter)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_information);

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

        // Initialize UI components
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Load restaurant details and reviews
        loadRestaurantDetails();
        loadRestaurantReviews();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        restaurantNameTextView = findViewById(R.id.restaurantNameTextView);
        restaurantAddressTextView = findViewById(R.id.restaurantAddressTextView);
//        restaurantDescriptionTextView = findViewById(R.id.restaurantDescriptionTextView);
        restaurantRatingBar = findViewById(R.id.restaurantRatingBar);
        ratingValueTextView = findViewById(R.id.ratingValueTextView);
        reviewCountTextView = findViewById(R.id.reviewCountTextView);
        operatingHoursRecyclerView = findViewById(R.id.operatingHoursRV);
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);

        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        userMap = new HashMap<>();

        // Set up Operating Hours RecyclerView
        operatingHoursRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadRestaurantDetails() {
        databaseReference.child("restaurants").child(restaurantId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        // Update UI
                        restaurantNameTextView.setText(restaurant.getName());
                        restaurantAddressTextView.setText(restaurant.getAddress());
//                        restaurantDescriptionTextView.setText(restaurant.getDescription());

                        // Load operating hours
                        loadOpeningHours(dataSnapshot.child("openingHours"));
                    }
                } else {
                    Toast.makeText(RestaurantInformationActivity.this, "Restaurant details not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load restaurant details: " + databaseError.getMessage());
                Toast.makeText(RestaurantInformationActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOpeningHours(DataSnapshot openingHoursSnapshot) {
        List<OpeningHoursAdapter.OpeningHourItem> openingHourItems = new ArrayList<>();

        if (!openingHoursSnapshot.exists() || openingHoursSnapshot.getChildrenCount() == 0) {
            TextView noHoursTextView = new TextView(this);
            noHoursTextView.setText("Không có thông tin giờ hoạt động");
            noHoursTextView.setTextSize(16);
            noHoursTextView.setPadding(0, 8, 0, 8);
            openingHourItems.add(new OpeningHoursAdapter.OpeningHourItem("", "Không có thông tin giờ hoạt động"));
            
            // Set adapter and return
            OpeningHoursAdapter adapter = new OpeningHoursAdapter(openingHourItems);
            operatingHoursRecyclerView.setAdapter(adapter);
            return;
        }

        Map<String, String> operatingHours = new HashMap<>();
        for (DataSnapshot daySnapshot : openingHoursSnapshot.getChildren()) {
            String dayKey = daySnapshot.getKey();
            String openTime = daySnapshot.child("open").getValue(String.class);
            String closeTime = daySnapshot.child("close").getValue(String.class);

            if (dayKey != null && openTime != null && closeTime != null) {
                String vietnameseDay = convertDayToVietnamese(dayKey);
                String hours = openTime + " - " + closeTime;
                operatingHours.put(vietnameseDay, hours);
            }
        }

        String[] daysOrder = {"Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"};

        for (String day : daysOrder) {
            String hours = operatingHours.get(day);
            if (hours != null) {
                openingHourItems.add(new OpeningHoursAdapter.OpeningHourItem(day, hours));
            }
        }

        OpeningHoursAdapter adapter = new OpeningHoursAdapter(openingHourItems);
        operatingHoursRecyclerView.setAdapter(adapter);
    }

    private String convertDayToVietnamese(String englishDay) {
        switch (englishDay.toLowerCase(Locale.ROOT)) {
            case "sunday": return "Chủ nhật";
            case "monday": return "Thứ hai";
            case "tuesday": return "Thứ ba";
            case "wednesday": return "Thứ tư";
            case "thursday": return "Thứ năm";
            case "friday": return "Thứ sáu";
            case "saturday": return "Thứ bảy";
            default: return englishDay; // Return original if not matched
        }
    }

    private void loadRestaurantReviews() {
        if (restaurantId == null) {
            Log.e(TAG, "Restaurant ID is null");
            return;
        }

        Log.d(TAG, "Loading reviews for restaurant: " + restaurantId);
        
        databaseReference.child("reviews")
            .orderByChild("restaurantId")
            .equalTo(restaurantId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        reviewList.clear();
                        userMap.clear();
                        double totalRating = 0;
                        int reviewCount = 0;

                        if (!snapshot.exists()) {
                            Log.d(TAG, "No reviews found for restaurant: " + restaurantId);
                            updateReviewUI(0, 0, new ArrayList<>());
                            return;
                        }

                        List<String> userIdsToFetch = new ArrayList<>();

                        for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                            try {
                                Review review = reviewSnapshot.getValue(Review.class);
                                if (review != null && review.getUserId() != null) {
                                    reviewList.add(review);
                                    totalRating += review.getRating();
                                    reviewCount++;
                                    userIdsToFetch.add(review.getUserId());
                                    Log.d(TAG, "Loaded review: " + review.getComment() + " from user: " + review.getUserId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing review: " + e.getMessage());
                            }
                        }

                        // Calculate average rating
                        float averageRating = reviewCount > 0 ? (float) (totalRating / reviewCount) : 0;
                        updateReviewUI(averageRating, reviewCount, reviewList);

                        // Sort reviews by timestamp (newest first)
                        Collections.sort(reviewList, (r1, r2) -> {
                            if (r1.getTimestamp() == null || r2.getTimestamp() == null) return 0;
                            return Long.compare(r2.getTimestamp().getTime(), r1.getTimestamp().getTime());
                        });

                        // Fetch user details for reviews
                        if (!userIdsToFetch.isEmpty()) {
                            fetchUserDetailsForReviews(userIdsToFetch, () -> {
                                try {
                                    ReviewAdapter reviewAdapter = new ReviewAdapter(
                                        RestaurantInformationActivity.this, 
                                        reviewList, 
                                        userMap, 
                                        restaurantId, 
                                        null  // Set replyClickListener to null to hide reply button
                                    );
                                    reviewsRecyclerView.setAdapter(reviewAdapter);
                                    Log.d(TAG, "Review adapter updated with " + reviewList.size() + " reviews");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error setting up review adapter: " + e.getMessage());
                                }
                            });
                        } else {
                            // If no users to fetch, still show reviews
                            ReviewAdapter reviewAdapter = new ReviewAdapter(
                                RestaurantInformationActivity.this, 
                                reviewList, 
                                userMap, 
                                restaurantId, 
                                null
                            );
                            reviewsRecyclerView.setAdapter(reviewAdapter);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing reviews: " + e.getMessage());
                        Toast.makeText(RestaurantInformationActivity.this, 
                            "Có lỗi khi tải đánh giá", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading restaurant reviews: " + error.getMessage());
                    Toast.makeText(RestaurantInformationActivity.this, 
                        "Lỗi khi tải đánh giá: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateReviewUI(float averageRating, int reviewCount, List<Review> reviews) {
        try {
            restaurantRatingBar.setRating(averageRating);
            ratingValueTextView.setText(String.format("%.1f", averageRating));
            reviewCountTextView.setText(String.format("(%d đánh giá)", reviewCount));

            if (reviews.isEmpty()) {
                // Show a message when there are no reviews
                TextView noReviewsText = new TextView(this);
                noReviewsText.setText("Chưa có đánh giá nào");
                noReviewsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                noReviewsText.setPadding(0, 32, 0, 32);
                reviewsRecyclerView.setAdapter(null);
                reviewsRecyclerView.addView(noReviewsText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating review UI: " + e.getMessage());
        }
    }

    private void fetchUserDetailsForReviews(List<String> userIds, Runnable onComplete) {
        if (userIds.isEmpty()) {
            onComplete.run();
            return;
        }

        int totalUsers = userIds.size();
        final int[] fetchedUsers = {0};

        DatabaseReference usersRef = databaseReference.child("users");
        for (String userId : userIds) {
            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            userMap.put(userId, user);
                            Log.d(TAG, "Loaded user details for: " + userId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user data for " + userId + ": " + e.getMessage());
                    }

                    fetchedUsers[0]++;
                    if (fetchedUsers[0] == totalUsers) {
                        Log.d(TAG, "All user details fetched. Total: " + userMap.size());
                        onComplete.run();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user details for " + userId + ": " + error.getMessage());
                    fetchedUsers[0]++;
                    if (fetchedUsers[0] == totalUsers) {
                        onComplete.run();
                    }
                }
            });
        }
    }
} 