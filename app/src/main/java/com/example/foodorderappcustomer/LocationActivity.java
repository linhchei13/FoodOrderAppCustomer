package com.example.foodorderappcustomer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.widget.AutoCompleteTextView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.view.ViewGroup.LayoutParams;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.util.Log;

public class LocationActivity extends AppCompatActivity {
    private static final String TAG = "LocationActivity";
    private EditText searchEditText;
    private TextView selectedLocationText;
    private TextView coordinatesText;
    private Button confirmButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private ArrayAdapter<String> adapter;
    private List<String> searchResults;
    private JsonObject selectedLocation;
    private boolean isAddressSelected = false;
    private OkHttpClient client;
    private Gson gson;
    private ListView suggestionsListView;
    private ConstraintLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);

        // Initialize views
        searchEditText = findViewById(R.id.autocomplete_fragment);
        selectedLocationText = findViewById(R.id.selectedLocationText);
        coordinatesText = findViewById(R.id.coordinatesText);
        confirmButton = findViewById(R.id.confirmButton);
        mainLayout = findViewById(R.id.main);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize OkHttp and Gson
        client = new OkHttpClient();
        gson = new Gson();

        // Initialize search results list and adapter
        searchResults = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_1, 
            searchResults);

        // Create and setup ListView for suggestions
        suggestionsListView = new ListView(this);
        suggestionsListView.setAdapter(adapter);
        suggestionsListView.setDivider(new ColorDrawable(Color.LTGRAY));
        suggestionsListView.setDividerHeight(1);
        suggestionsListView.setBackgroundColor(Color.WHITE);
        
        // Add ListView to layout
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        suggestionsListView.setId(View.generateViewId());
        suggestionsListView.setLayoutParams(params);
        suggestionsListView.setVisibility(View.GONE);
        mainLayout.addView(suggestionsListView);

        // Set constraints for ListView
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainLayout);
        constraintSet.connect(suggestionsListView.getId(), ConstraintSet.TOP, R.id.search_bar, ConstraintSet.BOTTOM, 0);
        constraintSet.connect(suggestionsListView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        constraintSet.connect(suggestionsListView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        constraintSet.applyTo(mainLayout);

        // Set up search functionality
        setupSearch();

        // Set up confirm button
        setupConfirmButton();

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Text changed: " + s.toString());
                if (s.length() > 0) {
                    isAddressSelected = false;
                    fetchAddressSuggestions(s.toString());
                } else {
                    searchResults.clear();
                    adapter.notifyDataSetChanged();
                    suggestionsListView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        suggestionsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedAddress = searchResults.get(position);
            Log.d(TAG, "Selected address: " + selectedAddress);
            searchEditText.setText(selectedAddress);
            isAddressSelected = true;
            suggestionsListView.setVisibility(View.GONE);
            
            // Hide keyboard after selection
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            
            if (selectedLocation != null) {
                updateLocationDisplay(selectedLocation);
            }
        });

        // Add focus change listener
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "Focus changed: " + hasFocus);
            if (hasFocus && !searchResults.isEmpty()) {
                Log.d(TAG, "Showing suggestions on focus");
                suggestionsListView.setVisibility(View.VISIBLE);
            } else {
                suggestionsListView.setVisibility(View.GONE);
            }
        });

        // Add touch listener to show suggestions on touch
        searchEditText.setOnTouchListener((v, event) -> {
            Log.d(TAG, "Touch event received");
            if (!searchResults.isEmpty()) {
                Log.d(TAG, "Showing suggestions on touch");
                suggestionsListView.setVisibility(View.VISIBLE);
            }
            return false;
        });
    }

    private void fetchAddressSuggestions(String query) {
        if (isAddressSelected) {
            return;
        }

        String url = "https://photon.komoot.io/api/?q=" + Uri.encode(query) + "&limit=5&countrycodes=vn";
        Log.d(TAG, "Fetching address suggestions for: " + url);
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(LocationActivity.this, 
                        "Lỗi tìm kiếm địa điểm: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API Response: " + responseBody);
                    
                    try {
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray features = jsonResponse.getAsJsonArray("features");
                        
                        runOnUiThread(() -> {
                            searchResults.clear();
                            if (features != null && features.size() > 0) {
                                Log.d(TAG, "Found " + features.size() + " features");
                                for (int i = 0; i < features.size(); i++) {
                                    JsonObject feature = features.get(i).getAsJsonObject();
                                    JsonObject properties = feature.getAsJsonObject("properties");
                                    
                                    // Build full address
                                    StringBuilder fullAddress = new StringBuilder();
                                    
                                    // Log each property for debugging
                                    Log.d(TAG, "Feature " + i + " properties: " + properties.toString());
                                    
                                    if (properties.has("housenumber") && !properties.get("housenumber").isJsonNull()) {
                                        fullAddress.append(properties.get("housenumber").getAsString()).append(" ");
                                    }
                                    if (properties.has("street") && !properties.get("street").isJsonNull()) {
                                        fullAddress.append(properties.get("street").getAsString());
                                    }
                                    if (properties.has("city") && !properties.get("city").isJsonNull()) {
                                        if (fullAddress.length() > 0) fullAddress.append(", ");
                                        fullAddress.append(properties.get("city").getAsString());
                                    }
                                    if (properties.has("state") && !properties.get("state").isJsonNull()) {
                                        if (fullAddress.length() > 0) fullAddress.append(", ");
                                        fullAddress.append(properties.get("state").getAsString());
                                    }
                                    
                                    String address = fullAddress.toString();
                                    Log.d(TAG, "Built address: " + address);
                                    if (!address.isEmpty()) {
                                        searchResults.add(address);
                                    }
                                }
                                
                                // Update adapter and show suggestions
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "Adapter updated with " + searchResults.size() + " items");
                                
                                if (searchEditText.hasFocus()) {
                                    Log.d(TAG, "Showing suggestions after adapter update");
                                    suggestionsListView.setVisibility(View.VISIBLE);
                                }
                                
                                // Store the first result as selected location
                                if (!searchResults.isEmpty()) {
                                    selectedLocation = features.get(0).getAsJsonObject();
                                    updateLocationDisplay(selectedLocation);
                                }
                            } else {
                                Log.d(TAG, "No features found in response");
                                adapter.notifyDataSetChanged();
                                suggestionsListView.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                        runOnUiThread(() -> {
                            Toast.makeText(LocationActivity.this,
                                "Lỗi xử lý kết quả tìm kiếm",
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "API call unsuccessful: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(LocationActivity.this,
                            "Không thể kết nối đến máy chủ",
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateLocationDisplay(JsonObject feature) {
        try {
            Log.d(TAG, "Updating location display with feature: " + feature.toString());
            selectedLocation = feature;
            JsonObject properties = feature.getAsJsonObject("properties");
            JsonArray coordinates = feature.getAsJsonObject("geometry").getAsJsonArray("coordinates");
            
            // Build full address
            StringBuilder fullAddress = new StringBuilder();
            
            if (properties.has("housenumber") && !properties.get("housenumber").isJsonNull()) {
                fullAddress.append(properties.get("housenumber").getAsString()).append(" ");
            }
            if (properties.has("street") && !properties.get("street").isJsonNull()) {
                fullAddress.append(properties.get("street").getAsString());
            }
            if (properties.has("city") && !properties.get("city").isJsonNull()) {
                if (fullAddress.length() > 0) fullAddress.append(", ");
                fullAddress.append(properties.get("city").getAsString());
            }
            if (properties.has("state") && !properties.get("state").isJsonNull()) {
                if (fullAddress.length() > 0) fullAddress.append(", ");
                fullAddress.append(properties.get("state").getAsString());
            }

            String address = fullAddress.toString();
            Log.d(TAG, "Final address to display: " + address);

            if (coordinates != null && coordinates.size() >= 2) {
                double longitude = coordinates.get(0).getAsDouble();
                double latitude = coordinates.get(1).getAsDouble();
                
                selectedLocationText.setText(address);
                coordinatesText.setText(String.format("Tọa độ: %.6f, %.6f", latitude, longitude));
                
                Log.d(TAG, "Updated UI with address: " + address + " and coordinates: " + latitude + ", " + longitude);
            } else {
                Log.e(TAG, "Invalid coordinates in feature");
                Toast.makeText(this, "Không thể xác định tọa độ", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating location display", e);
            Toast.makeText(this, "Lỗi hiển thị địa chỉ", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupConfirmButton() {
        confirmButton.setOnClickListener(v -> {
            if (selectedLocation != null) {
                saveLocationToFirebase(selectedLocation);
            } else {
                Toast.makeText(this, "Vui lòng chọn một địa điểm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLocationToFirebase(JsonObject location) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> addressMap = new HashMap<>();
            JsonObject properties = location.getAsJsonObject("properties");
            JsonArray coordinates = location.getAsJsonObject("geometry").getAsJsonArray("coordinates");
            
            if (properties.has("street")) addressMap.put("street", properties.get("street").getAsString());
            if (properties.has("city")) addressMap.put("city", properties.get("city").getAsString());
            if (properties.has("state")) addressMap.put("state", properties.get("state").getAsString());
            if (properties.has("country")) addressMap.put("country", properties.get("country").getAsString());
            if (properties.has("postcode")) addressMap.put("postcode", properties.get("postcode").getAsString());
            if (properties.has("housenumber")) addressMap.put("houseNumber", properties.get("housenumber").getAsString());
            
            addressMap.put("latitude", coordinates.get(1).getAsDouble());
            addressMap.put("longitude", coordinates.get(0).getAsDouble());

            databaseReference.child("users").child(currentUser.getUid())
                .child("address")
                .setValue(addressMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LocationActivity.this, 
                        "Đã lưu địa chỉ thành công", 
                        Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LocationActivity.this, 
                        "Lỗi khi lưu địa chỉ: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        }
    }
}