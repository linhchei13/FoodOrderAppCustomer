package com.example.foodorderappcustomer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodorderappcustomer.Models.User;
import com.example.foodorderappcustomer.util.ImageUploadUtils;
import com.example.foodorderappcustomer.util.ImageUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    
    private TextInputEditText firstNameEditText, lastNameEditText, emailEditText, phoneEditText;
    private Button saveButton;
    private ImageView profileImageView;
    private TextView changePhotoText;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private User user;
    private FirebaseAuth mAuth;
    private Uri selectedImageUri;
    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                profileImageView.setImageURI(selectedImageUri);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        init();
        setupClickListeners();
    }

    private void init() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);
        profileImageView = findViewById(R.id.profileImageView);
        changePhotoText = findViewById(R.id.changePhotoText);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang cập nhật...");
        progressDialog.setCancelable(false);

        // Get current user data
        String userId = mAuth.getCurrentUser().getUid();
        databaseReference.child(userId).get().addOnSuccessListener(dataSnapshot -> {
            user = dataSnapshot.getValue(User.class);
            if (user != null) {
                // Populate the fields with current user data
                firstNameEditText.setText(user.getFirstName());
                lastNameEditText.setText(user.getLastName());
                emailEditText.setText(user.getEmail());
                phoneEditText.setText(user.getPhone());
                
                // Load profile image if exists
                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    ImageUtils.loadImage(
                        this,
                        user.getProfileImageUrl(),
                        profileImageView,
                        R.drawable.baseline_person_24,
                        R.drawable.baseline_person_24
                    );
                }
            }
        });
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> updateUserData());
        
        // Profile image click listener
        profileImageView.setOnClickListener(v -> openImagePicker());
        changePhotoText.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void updateUserData() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        // Validate input fields
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        progressDialog.show();

        // If there's a new image selected, upload it first
        if (selectedImageUri != null) {
            uploadImageAndUpdateProfile(firstName, lastName, email, phone);
        } else {
            // Update profile without changing image
            updateProfileData(firstName, lastName, email, phone, user != null ? user.getProfileImageUrl() : null);
        }
    }

    private void uploadImageAndUpdateProfile(String firstName, String lastName, String email, String phone) {
        // Delete old image if exists
        if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            ImageUploadUtils.deleteImage(user.getProfileImageUrl(), new ImageUploadUtils.ImageUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // After deleting old image, upload new one
                    uploadNewImage(firstName, lastName, email, phone);
                }

                @Override
                public void onFailure(Exception e) {
                    // Continue with upload even if delete fails
                    uploadNewImage(firstName, lastName, email, phone);
                }

                @Override
                public void onProgress(double progress) {
                    // Not needed for delete operation
                }
            });
        } else {
            // No old image to delete, upload new one directly
            uploadNewImage(firstName, lastName, email, phone);
        }
    }

    private void uploadNewImage(String firstName, String lastName, String email, String phone) {
        ImageUploadUtils.uploadImage(this, selectedImageUri, "profile_images", new ImageUploadUtils.ImageUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // Update profile with new image URL
                updateProfileData(firstName, lastName, email, phone, downloadUrl);
            }

            @Override
            public void onFailure(Exception e) {
                progressDialog.dismiss();
                Toast.makeText(EditProfileActivity.this, "Lỗi khi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(double progress) {
                progressDialog.setMessage("Đang tải ảnh lên: " + (int) progress + "%");
            }
        });
    }

    private void updateProfileData(String firstName, String lastName, String email, String phone, String imageUrl) {
        // Get current user ID
        String userId = mAuth.getCurrentUser().getUid();

        // Create updated user object
        User updatedUser = new User(userId, firstName, lastName, email, phone);
        updatedUser.setProfileImageUrl(imageUrl);
        if (user != null) {
            updatedUser.setAddress(user.getAddress());
            updatedUser.setCreatedAt(user.getCreatedAt());
        }

        // Update user data in Firebase
        databaseReference.child(userId).setValue(updatedUser)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Lỗi khi cập nhật thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}