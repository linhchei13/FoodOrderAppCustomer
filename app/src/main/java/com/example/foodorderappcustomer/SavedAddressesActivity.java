package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.foodorderappcustomer.Adapter.SavedAddressAdapter;
import com.example.foodorderappcustomer.Models.SavedAddress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SavedAddressesActivity extends AppCompatActivity {
    private static final String TAG = "SavedAddressesActivity";

    // UI Components
    private RecyclerView recyclerViewSavedAddresses;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private TextView btnAddAddress;
    private Button btnAddNewAddress;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    // Data and Adapter
    private List<SavedAddress> addressList;
    private SavedAddressAdapter adapter;

    // ActivityResultLauncher for LocationActivity
    private ActivityResultLauncher<Intent> locationSelectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_addresses);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Not signed in, redirect to login
            redirectToLogin();
            return;
        }
        userId = currentUser.getUid();

        // Initialize the ActivityResultLauncher
        locationSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selectedAddress = result.getData().getStringExtra("selected_address");
                    String placeId = result.getData().getStringExtra("place_id");
                    if (selectedAddress != null && !selectedAddress.isEmpty()) {
                        // Return selected location to the activity that launched SavedAddressesActivity
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("selected_address", selectedAddress);
                        resultIntent.putExtra("place_id", placeId);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }
            }
        );

        // Initialize UI components
        initViews();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up click listeners
        setupListeners();
        
        // Load saved addresses
        loadSavedAddresses();
    }

    private void initViews() {
        recyclerViewSavedAddresses = findViewById(R.id.recyclerViewSavedAddresses);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnAddAddress = findViewById(R.id.btnAddAddress);
        btnAddNewAddress = findViewById(R.id.btnAddNewAddress);
    }

    private void setupRecyclerView() {
        addressList = new ArrayList<>();
        adapter = new SavedAddressAdapter(addressList, new SavedAddressAdapter.OnAddressClickListener() {
            @Override
            public void onAddressClick(SavedAddress address) {
                // Handle address click
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_address", address.getAddress());
                resultIntent.putExtra("place_id", address.getPlaceId());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        recyclerViewSavedAddresses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSavedAddresses.setAdapter(adapter);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());
        
        // Add address button in toolbar
        btnAddAddress.setOnClickListener(v -> openLocationActivity());
        
        // Add new address button in empty state
        btnAddNewAddress.setOnClickListener(v -> openLocationActivity());
    }

    private void openLocationActivity() {
        Intent intent = new Intent(this, AddSavedAddressActivity.class);
        locationSelectionLauncher.launch(intent); // Use the launcher
    }

    private void loadSavedAddresses() {
        showLoading();
        
        mDatabase.child("users").child(userId).child("saved_addresses")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    addressList.clear();
                    
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        SavedAddress address = snapshot.getValue(SavedAddress.class);
                        if (address != null) {
                            // Ensure the ID is set
                            if (address.getId() == null) {
                                address.setId(snapshot.getKey());
                            }
                            addressList.add(address);
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    hideLoading();
                    
                    // Show empty state if no addresses
                    if (addressList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error loading addresses: ", databaseError.toException());
                    hideLoading();
                    showEmptyState();
                }
            });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewSavedAddresses.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerViewSavedAddresses.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        recyclerViewSavedAddresses.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyState.setVisibility(View.GONE);
        recyclerViewSavedAddresses.setVisibility(View.VISIBLE);
    }

    private void redirectToLogin() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload addresses in case they were modified
        if (mAuth.getCurrentUser() != null) {
            loadSavedAddresses();
        }
    }
}
