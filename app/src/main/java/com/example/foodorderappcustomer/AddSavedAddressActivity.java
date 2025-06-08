package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodorderappcustomer.API.AutoCompleteApi;
import com.example.foodorderappcustomer.API.PlaceResponse;
import com.example.foodorderappcustomer.Models.SavedAddress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddSavedAddressActivity extends AppCompatActivity {
    private static final String TAG = "AddSavedAddressActivity";
    private EditText labelEditText;
    private EditText addressEditText;
    private Button saveButton;
    private ImageView  clearButton;
    private ImageButton backButton;
    private DatabaseReference savedAddressRef;
    private String userId;
    private String placeId; // This will now be updated by autocomplete selection
    private String selectedAddress; // This will now be updated by autocomplete selection

    private ActivityResultLauncher<Intent> locationPickerLauncher;

    // Autocomplete related fields
    private ListView suggestionsListView;
    private ArrayAdapter<String> suggestionsAdapter;
    private List<String> searchResults;
    private List<PlaceResponse.Predictions> predictions;
    private AutoCompleteApi placeApi;
    private String goongApiKey;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DEBOUNCE_DELAY = 500; // 500ms delay
    private static final int MIN_SEARCH_LENGTH = 4; // Minimum characters to start search

    // Location bias for API
    private double biasLat = 21.0285; // Default to Hanoi
    private double biasLong = 105.8542; // Default to Hanoi
    private static final int RADIUS = 200; // 200km radius around

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_saved_address);

        // Initialize the launcher for re-selecting address from LocationActivity
        locationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String newSelectedAddress = result.getData().getStringExtra("selected_address");
                    String newPlaceId = result.getData().getStringExtra("place_id");
                    if (newSelectedAddress != null && !newSelectedAddress.isEmpty()) {
                        selectedAddress = newSelectedAddress;
                        placeId = newPlaceId;
                        addressEditText.setText(selectedAddress);
                        suggestionsListView.setVisibility(View.GONE); // Hide suggestions after selection
                    }
                }
            }
        );

        // Get data from intent (initial address if coming from LocationActivity or current location bias)
        placeId = getIntent().getStringExtra("place_id");
        selectedAddress = getIntent().getStringExtra("selected_address");
        biasLat = getIntent().getDoubleExtra("bias_lat", biasLat);
        biasLong = getIntent().getDoubleExtra("bias_long", biasLong);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        savedAddressRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("saved_addresses");

        initViews();
        initApi();
        setupListeners();
        setupAutocomplete();
    }

    private void initViews() {
        labelEditText = findViewById(R.id.labelEditText);
        addressEditText = findViewById(R.id.addressEditText);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        clearButton = findViewById(R.id.clearButton);
        suggestionsListView = findViewById(R.id.suggestionsListView); // Assuming you'll add this to your layout

        // Initialize search results list and adapter for suggestions
        searchResults = new ArrayList<>();
        predictions = new ArrayList<>();
        suggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, // Default Android list item layout
                searchResults);
        suggestionsListView.setAdapter(suggestionsAdapter);

        // Set the address from initial intent, if any
        if (selectedAddress != null && !selectedAddress.isEmpty()) {
            addressEditText.setText(selectedAddress);
        }
    }

    private void initApi() {
        placeApi = AutoCompleteApi.apiInterface;
        goongApiKey = getString(R.string.goong_api_key); // Ensure you have this in strings.xml
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        clearButton.setOnClickListener(v -> {
            addressEditText.setEnabled(true);
            addressEditText.setText("");
        });

        // Listener for re-selecting address using LocationActivity
        addressEditText.setOnClickListener(v -> {
            Intent intent = new Intent(AddSavedAddressActivity.this, LocationActivity.class);
            intent.putExtra("bias_lat", biasLat); // Pass current bias
            intent.putExtra("bias_long", biasLong); // Pass current bias
            locationPickerLauncher.launch(intent);
        });

        saveButton.setOnClickListener(v -> {
            String label = labelEditText.getText().toString().trim();
            String address = addressEditText.getText().toString().trim();

            if (label.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên địa chỉ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (address.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn địa chỉ", Toast.LENGTH_SHORT).show();
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
                        address,
                        placeId // Use the updated placeId from autocomplete or direct selection
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
                } else {
                    Toast.makeText(AddSavedAddressActivity.this, 
                        "Lỗi khi kiểm tra địa chỉ đã lưu: " + task.getException().getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupAutocomplete() {
        addressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (query.length() >= MIN_SEARCH_LENGTH) {
                        fetchAddressSuggestions(query);
                    } else {
                        clearSuggestions();
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        suggestionsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < predictions.size()) {
                PlaceResponse.Predictions selectedPrediction = predictions.get(position);
                selectedAddress = searchResults.get(position);
                placeId = selectedPrediction.getPlaceId();

                addressEditText.setText(selectedAddress); // Set selected address to EditText
                suggestionsListView.setVisibility(View.GONE);
                addressEditText.setEnabled(false);// Hide suggestions
            }
        });
    }

    private void fetchAddressSuggestions(String query) {
        Log.d(TAG, "Fetching address suggestions for: " + query);

        String locationBias = biasLat + "," + biasLong;
        String locationRestriction = String.valueOf(RADIUS);

        placeApi.getPlace(goongApiKey, query, 10, locationBias, locationRestriction) // Max 10 results
                .enqueue(new Callback<PlaceResponse>() {
                    @Override
                    public void onResponse(Call<PlaceResponse> call, Response<PlaceResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PlaceResponse placeResponse = response.body();
                            if (placeResponse.getPredictions() != null && !placeResponse.getPredictions().isEmpty()) {
                                processSearchResults(placeResponse.getPredictions());
                            } else {
                                clearSuggestions();
                            }
                        } else {
                            Log.e(TAG, "API response not successful: " + response.code());
                            showError("Không thể tìm kiếm địa điểm");
                        }
                    }

                    @Override
                    public void onFailure(Call<PlaceResponse> call, Throwable t) {
                        Log.e(TAG, "API call failed", t);
                        showError("Lỗi kết nối mạng");
                    }
                });
    }

    private void processSearchResults(List<PlaceResponse.Predictions> newPredictions) {
        runOnUiThread(() -> {
            searchResults.clear();
            predictions.clear();

            for (PlaceResponse.Predictions prediction : newPredictions) {
                String mainText = prediction.getStructuredFormatting().getMainText();
                String secondaryText = prediction.getStructuredFormatting().getSecondaryText();
                String suggestion = String.format("%s, %s", mainText, secondaryText);

                searchResults.add(suggestion);
                predictions.add(prediction);
            }

            suggestionsAdapter.notifyDataSetChanged();
            suggestionsListView.setVisibility(searchResults.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void clearSuggestions() {
        runOnUiThread(() -> {
            searchResults.clear();
            predictions.clear();
            suggestionsAdapter.notifyDataSetChanged();
            suggestionsListView.setVisibility(View.GONE);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(AddSavedAddressActivity.this, message, Toast.LENGTH_SHORT).show();
            clearSuggestions();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
} 