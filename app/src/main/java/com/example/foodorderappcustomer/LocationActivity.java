package com.example.foodorderappcustomer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.foodorderappcustomer.API.AutoCompleteApi;
import com.example.foodorderappcustomer.API.PlaceResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class LocationActivity extends AppCompatActivity {
    private static final String TAG = "LocationActivity";
    private static final int SEARCH_DEBOUNCE_DELAY = 500; // 500ms delay
    private static final int MAX_SEARCH_RESULTS = 10;
    private static final int MIN_SEARCH_LENGTH = 4; // Minimum characters to start search

    private double biasLat = 21.0285;
    private double biasLong = 105.8542;
    private static final int RADIUS = 200; // 200km radius around

    private EditText searchEditText;
    private ListView suggestionsListView;
    private ArrayAdapter<String> adapter;
    private List<String> searchResults;
    private List<PlaceResponse.Predictions> predictions;
    private AutoCompleteApi placeApi;
    private String goongApiKey;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView clearButton;
    private ImageButton backButton;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(this, "Cần quyền truy cập vị trí để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
                }
            }
        );

        initViews();
        initApi();
        getCurrentLocation();
        setupSearch();
        backButton.setOnClickListener(v -> finish());
    }

    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        suggestionsListView = findViewById(R.id.suggestionsListView);
        clearButton = findViewById(R.id.clearButton);
        backButton = findViewById(R.id.backButton);

        // Initialize search results list and adapter
        searchResults = new ArrayList<>();
        predictions = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                searchResults);
        suggestionsListView.setAdapter(adapter);
    }

    private void initApi() {
        placeApi = AutoCompleteApi.apiInterface;
        goongApiKey = getString(R.string.goong_api_key); // Make sure you have this in strings.xml
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Create new search runnable
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (query.length() >= MIN_SEARCH_LENGTH) {
                        fetchAddressSuggestions(query);
                    } else {
                        clearSuggestions();
                    }
                };

                // Post delayed search
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        suggestionsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < predictions.size()) {
                PlaceResponse.Predictions selectedPlace = predictions.get(position);
                String selectedAddress = searchResults.get(position);

                // Return selected location to calling activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_address", selectedAddress);
                resultIntent.putExtra("place_id", selectedPlace.getPlaceId());

                // You can also add coordinates if needed
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSuggestions();
        });
    }

    private void setupCurrentLocationButton() {
        clearButton.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    // Use the location to search for address
                    biasLat = location.getLatitude();
                    biasLong = location.getLongitude();
                } else {
                    Toast.makeText(this, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi khi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void fetchAddressSuggestions(String query) {
        Log.d(TAG, "Fetching address suggestions for: " + query);

        // Add location bias and restriction for Hanoi
        String locationBias = biasLat + "," + biasLong;
        String locationRestriction = Double.toString(RADIUS);

        placeApi.getPlace(goongApiKey, query, MAX_SEARCH_RESULTS, locationBias, locationRestriction)
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

            adapter.notifyDataSetChanged();
            suggestionsListView.setVisibility(searchResults.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void clearSuggestions() {
        runOnUiThread(() -> {
            searchResults.clear();
            predictions.clear();
            adapter.notifyDataSetChanged();
            suggestionsListView.setVisibility(View.GONE);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(LocationActivity.this, message, Toast.LENGTH_SHORT).show();
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