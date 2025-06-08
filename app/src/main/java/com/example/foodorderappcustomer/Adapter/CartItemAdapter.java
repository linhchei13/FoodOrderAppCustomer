package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.OpeningHour;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    private Context context;
    private Map<String, List<OrderItem>> restaurantItemsMap;
    private List<String> restaurantIds;
    private List<String> selectedRestaurantIds;
    private boolean isManageMode = false;
    private final NumberFormat currencyFormat;
    private OnCartItemListener listener;
    private DatabaseReference databaseReference;

    public interface OnCartItemListener {
        void onRestaurantSelectionChanged(String restaurantId, boolean isSelected);
        void onRestaurantClicked(String restaurantId);
    }

    public CartItemAdapter(Context context, Map<String, List<OrderItem>> restaurantItemsMap, List<String> selectedRestaurantIds) {
        this.context = context;
        this.restaurantItemsMap = restaurantItemsMap;
        this.selectedRestaurantIds = selectedRestaurantIds;
        this.restaurantIds = new ArrayList<>(restaurantItemsMap.keySet());
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public void setRestaurantItemsMap(Map<String, List<OrderItem>> restaurantItemsMap) {
        this.restaurantItemsMap = restaurantItemsMap;
        this.restaurantIds = new ArrayList<>(restaurantItemsMap.keySet());
        notifyDataSetChanged();
    }

    public void setManageMode(boolean manageMode) {
        this.isManageMode = manageMode;
    }

    public void setOnCartItemListener(OnCartItemListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart_restaurant, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        String restaurantId = restaurantIds.get(position);
        List<OrderItem> items = restaurantItemsMap.get(restaurantId);

        if (items == null || items.isEmpty()) {
            return;
        }

        // Load restaurant details
        loadRestaurantDetails(holder, restaurantId, items);
    }

    private void loadRestaurantDetails(CartItemViewHolder holder, String restaurantId, List<OrderItem> items) {
        databaseReference.child("restaurants").child(restaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String restaurantName = dataSnapshot.child("name").getValue(String.class);
                            String restaurantImage = dataSnapshot.child("imageUrl").getValue(String.class);
                            String address = dataSnapshot.child("address").getValue(String.class);
                            Double distance = dataSnapshot.child("distance").getValue(Double.class);

                            // Load opening hours
                            Map<String, OpeningHour> openingHours = new HashMap<>();
                            DataSnapshot openingHoursSnapshot = dataSnapshot.child("openingHours");
                            if (openingHoursSnapshot.exists()) {
                                for (DataSnapshot daySnapshot : openingHoursSnapshot.getChildren()) {
                                    OpeningHour openingHour = daySnapshot.getValue(OpeningHour.class);
                                    if (openingHour != null) {
                                        openingHours.put(daySnapshot.getKey(), openingHour);
                                    }
                                }
                            }

                            // Bind data to view holder
                            holder.bind(
                                    restaurantId,
                                    restaurantName != null ? restaurantName : "Nhà hàng",
                                    restaurantImage,
                                    address,
                                    true,
                                    "Open",
                                    distance != null ? distance : 0.0,
                                    items
                            );
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Use default values
                        holder.bind(
                                restaurantId,
                                "Nhà hàng",
                                null,
                                null,
                                false,
                                "Thông tin giờ mở cửa không có sẵn",
                                0.0,
                                items
                        );
                    }
                });
    }

    @Override
    public int getItemCount() {
        return restaurantIds.size();
    }

    class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView restaurantImageView;
        private final TextView restaurantNameTextView;
        private final TextView distanceTextView;
        private final TextView priceTextView;
        private final TextView quantityTextView;
        private final TextView statusTextView;
        private final CheckBox selectionCheckBox;
        private String currentRestaurantId;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImageView = itemView.findViewById(R.id.restaurantImageView);
            restaurantNameTextView = itemView.findViewById(R.id.restaurantNameTextView);
            distanceTextView = itemView.findViewById(R.id.distanceTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            selectionCheckBox = itemView.findViewById(R.id.selectionCheckBox);

            // Set click listener for the whole item
            itemView.setOnClickListener(v -> {
                if (listener != null && currentRestaurantId != null) {
                    if (isManageMode) {
                        // In manage mode, toggle selection
                        boolean newState = !selectedRestaurantIds.contains(currentRestaurantId);
                        selectionCheckBox.setChecked(newState);
                        listener.onRestaurantSelectionChanged(currentRestaurantId, newState);
                    } else {
                        // In normal mode, open restaurant detail
                        listener.onRestaurantClicked(currentRestaurantId);
                    }
                }
            });

            // Set checkbox listener
            selectionCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null && currentRestaurantId != null && buttonView.isPressed()) {
                    listener.onRestaurantSelectionChanged(currentRestaurantId, isChecked);
                }
            });
        }

        public void bind(String restaurantId, String restaurantName, String restaurantImage,
                         String address, boolean isOpen, String status, double distance,
                         List<OrderItem> items) {

            this.currentRestaurantId = restaurantId;

            // Set restaurant name
            restaurantNameTextView.setText(restaurantName);

            // Set distance
            String distanceText = String.format(Locale.getDefault(), "%.2f km", distance);
            distanceTextView.setText(distanceText);

            // Calculate total price and quantity
            double totalPrice = 0;
            int totalQuantity = 0;
            for (OrderItem item : items) {
                totalPrice += item.getTotalPrice() * item.getQuantity();
                totalQuantity += item.getQuantity();
            }

            // Set price and quantity
            String formattedPrice = currencyFormat.format(totalPrice).replace("₫", "đ");
            priceTextView.setText(formattedPrice);
            quantityTextView.setText("(" + totalQuantity + " phần)");

            // Set status based on opening hours calculation
            statusTextView.setText(status);
            if (isOpen) {
                statusTextView.setTextColor(context.getResources().getColor(R.color.status_completed));
            } else {
                statusTextView.setTextColor(context.getResources().getColor(R.color.status_cancelled));
            }

            // Set checkbox visibility based on mode
            selectionCheckBox.setVisibility(isManageMode ? View.VISIBLE : View.GONE);

            // Set checkbox state
            selectionCheckBox.setOnCheckedChangeListener(null); // Remove listener temporarily
            selectionCheckBox.setChecked(selectedRestaurantIds.contains(restaurantId));
            selectionCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null && buttonView.isPressed()) {
                    listener.onRestaurantSelectionChanged(restaurantId, isChecked);
                }
            });

            // Load restaurant image
            if (restaurantImage != null && !restaurantImage.isEmpty()) {
                try {
                    Glide.with(context)
                            .load(restaurantImage)
                            .placeholder(R.drawable.loading_img)
                            .error(R.drawable.logo2)
                            .into(restaurantImageView);
                } catch (Exception e) {
                    restaurantImageView.setImageResource(R.drawable.logo2);
                }
            } else {
                restaurantImageView.setImageResource(R.drawable.logo2);
            }
        }
    }
}
