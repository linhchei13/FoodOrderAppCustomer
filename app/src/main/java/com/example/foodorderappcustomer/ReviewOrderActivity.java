package com.example.foodorderappcustomer;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.ReviewImageAdapter;
import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.util.ImageUploadUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReviewOrderActivity extends AppCompatActivity implements ReviewImageAdapter.OnImageRemoveListener {
    private static final String EXTRA_ORDER_ID = "order_id";
    private static final String EXTRA_RESTAURANT_ID = "restaurant_id";

    private RatingBar ratingBar;
    private EditText editTextReview;
    private RecyclerView imagesRecyclerView;
    private MaterialButton addImageButton;
    private MaterialButton submitButton;
    private ImageButton backButton;

    private String orderId;
    private String restaurantId;
    private Order currentOrder;
    private DatabaseReference databaseReference;
    private ReviewImageAdapter imageAdapter;
    private List<Uri> selectedImageUris;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                if (result.getData().getClipData() != null) {
                    // Handle multiple images
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                        if(imageUri != null) {
                            imageAdapter.addImage(imageUri);
                            selectedImageUris.add(imageUri);
                        }
                    }
                } else if (result.getData().getData() != null) {
                    // Handle single image
                    Uri imageUri = result.getData().getData();
                    if(imageUri != null) {
                        imageAdapter.addImage(imageUri);
                        selectedImageUris.add(imageUri);
                    }
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_order);

        // Initialize selectedImageUris
        selectedImageUris = new ArrayList<>();

        // Get data from intent
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        restaurantId = getIntent().getStringExtra(EXTRA_RESTAURANT_ID);

        if (orderId == null || restaurantId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initViews();
        setupClickListeners();
        setupRecyclerView();

        // Load order data
        loadOrderData();
    }

    private void initViews() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        backButton = findViewById(R.id.backButton);
        ratingBar = findViewById(R.id.ratingBar);
        ratingBar.setIsIndicator(false);
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            // Handle rating change if needed
        });
        
        editTextReview = findViewById(R.id.commentInput);
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView);
        addImageButton = findViewById(R.id.addImageButton);
        submitButton = findViewById(R.id.submitButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        addImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            imagePickerLauncher.launch(intent);
        });

        submitButton.setOnClickListener(v -> submitReview());
    }

    private void setupRecyclerView() {
        imagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ReviewImageAdapter(this);
        imagesRecyclerView.setAdapter(imageAdapter);
    }

    private void loadOrderData() {
        databaseReference.child("orders").child(orderId).get()
            .addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    currentOrder = dataSnapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        checkExistingReview();
                    }
                } else {
                    Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void checkExistingReview() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference.child("reviews")
            .child(orderId)
            .get()
            .addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    // Review exists, disable review submission
                    Review existingReview = dataSnapshot.getValue(Review.class);
                    if (existingReview != null) {
                        ratingBar.setRating(existingReview.getRating());
                        ratingBar.setIsIndicator(true);
                        editTextReview.setEnabled(false);
                        submitButton.setEnabled(false);
                        addImageButton.setEnabled(false);
                    }
                } else {
                    // No review exists, ensure rating bar is enabled
                    ratingBar.setIsIndicator(false);
                    ratingBar.setEnabled(true);
                }
            });
    }

    @Override
    public void onImageRemove(int position) {
        imageAdapter.removeImage(position);
        if (position < selectedImageUris.size()) {
            selectedImageUris.remove(position);
        }
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = editTextReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đánh giá của bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog with progress
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
            .setTitle("Đang xử lý")
            .setMessage("Đang chuẩn bị tải ảnh lên...")
            .setCancelable(false)
            .create();
        loadingDialog.show();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Review review = new Review();
        review.setUserId(userId);
        review.setOrderId(orderId);
        review.setRestaurantId(restaurantId);
        review.setRating(rating);
        review.setComment(comment);
        review.setTimestamp(new Date());

        List<Uri> validImageUris = imageAdapter.getImages();
        if (!validImageUris.isEmpty()) {
            // Update dialog message for image upload
            loadingDialog.setMessage("Đang tải ảnh lên (0/" + validImageUris.size() + ")...");
            // Upload images
            uploadImages(validImageUris, 0, new ArrayList<>(), review, loadingDialog);
        } else {
            // No images to upload, save review directly
            saveReviewToDatabase(review, new ArrayList<>(), loadingDialog);
        }
    }

    private void uploadImages(List<Uri> imageUris, int index, List<String> uploadedUrls,
                            Review review, AlertDialog loadingDialog) {
        if (index >= imageUris.size()) {
            // All images uploaded, save review
            loadingDialog.setMessage("Đang lưu đánh giá...");
            saveReviewToDatabase(review, uploadedUrls, loadingDialog);
            return;
        }

        Uri imageUri = imageUris.get(index);
        String folder = "reviews/" + orderId;
        
        // Update progress message
        loadingDialog.setMessage("Đang tải ảnh lên (" + (index + 1) + "/" + imageUris.size() + ")...");
        
        ImageUploadUtils.uploadImage(imageUri, folder, new ImageUploadUtils.ImageUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                uploadedUrls.add(downloadUrl);
                // Continue with next image
                uploadImages(imageUris, index + 1, uploadedUrls, review, loadingDialog);
            }

            @Override
            public void onFailure(Exception e) {
                loadingDialog.dismiss();
                Toast.makeText(ReviewOrderActivity.this, 
                    "Lỗi khi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(double progress) {
                // Update progress message with percentage
                loadingDialog.setMessage(String.format("Đang tải ảnh lên (%d/%d)... %.0f%%", 
                    index + 1, imageUris.size(), progress));
            }
        });
    }

    private void saveReviewToDatabase(Review review, List<String> imageUrls, AlertDialog loadingDialog) {
        review.setImageUrls(imageUrls);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        databaseReference.child("reviews")
            .child(orderId)
            .setValue(review)
            .addOnSuccessListener(aVoid -> {
                loadingDialog.dismiss();
                Toast.makeText(this, "Đánh giá đã được gửi thành công", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            })
            .addOnFailureListener(e -> {
                loadingDialog.dismiss();
                Toast.makeText(this, "Lỗi khi lưu đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    public static Intent newIntent(android.content.Context context, String orderId, String restaurantId) {
        Intent intent = new Intent(context, ReviewOrderActivity.class);
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        intent.putExtra(EXTRA_RESTAURANT_ID, restaurantId);
        return intent;
    }
} 