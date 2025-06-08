package com.example.foodorderappcustomer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodorderappcustomer.Models.SavedAddress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddSavedAddressActivity extends AppCompatActivity {
    private EditText labelEditText;
    private EditText addressEditText;
    private Button saveButton;
    private ImageButton backButton;
    private DatabaseReference savedAddressRef;
    private String userId;
    private String placeId;
    private String selectedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_saved_address);

        // Get data from intent
        placeId = getIntent().getStringExtra("place_id");
        selectedAddress = getIntent().getStringExtra("selected_address");

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        savedAddressRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("saved_addresses");

        initViews();
        setupListeners();
    }

    private void initViews() {
        labelEditText = findViewById(R.id.labelEditText);
        addressEditText = findViewById(R.id.addressEditText);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);

        // Set the address from location selection
        addressEditText.setText(selectedAddress);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            String label = labelEditText.getText().toString().trim();
            
            if (label.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên địa chỉ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user already has 5 saved addresses
            savedAddressRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    long addressCount = task.getResult().getChildrenCount();
                    if (addressCount >= 5) {
                        Toast.makeText(AddSavedAddressActivity.this, 
                            "Bạn đã đạt giới hạn 5 địa chỉ đã lưu", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create new saved address
                    String addressId = savedAddressRef.push().getKey();
                    SavedAddress savedAddress = new SavedAddress(
                        addressId,
                        label,
                        selectedAddress,
                        placeId
                    );

                    // Save to Firebase
                    savedAddressRef.child(addressId).setValue(savedAddress)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AddSavedAddressActivity.this, 
                                "Đã lưu địa chỉ thành công", 
                                Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AddSavedAddressActivity.this, 
                                "Lỗi khi lưu địa chỉ: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                }
            });
        });
    }
} 