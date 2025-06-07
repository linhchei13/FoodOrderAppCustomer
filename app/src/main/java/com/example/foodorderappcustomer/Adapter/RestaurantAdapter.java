package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.RestaurantMenuActivity;
import com.example.foodorderappcustomer.util.ImageUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    private List<Restaurant> restaurants;
    private Context context;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public RestaurantAdapter(List<Restaurant> restaurants) {
        this.restaurants = restaurants;
    }

    public void updateData(List<Restaurant> newRestaurants) {
        this.restaurants = newRestaurants;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);
        holder.bind(restaurant);

        // Set click listener to open RestaurantDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RestaurantMenuActivity.class);
            intent.putExtra("RESTAURANT_ID", restaurant.getId());
            intent.putExtra("RESTAURANT_NAME", restaurant.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    class RestaurantViewHolder extends RecyclerView.ViewHolder {
        private ImageView restaurantImage;
        private TextView restaurantName;
        private TextView restaurantAddress;
        private TextView ratingValue;
        private TextView deliveryTime;
        private TextView distance;

        private TextView averagePriceTV;
        private TextView ratingCountTv;
        private ChipGroup categoryChipGroup;

        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantAddress = itemView.findViewById(R.id.restaurantAddress);
            ratingValue = itemView.findViewById(R.id.ratingValue);
            deliveryTime = itemView.findViewById(R.id.deliveryTime);
            averagePriceTV = itemView.findViewById(R.id.averagePriceTV);
            ratingCountTv = itemView.findViewById(R.id.ratingCountTv);
            categoryChipGroup = itemView.findViewById(R.id.categoryChipGroup);
        }

        public void bind(Restaurant restaurant) {
            restaurantName.setText(restaurant.getName());
            restaurantAddress.setText(restaurant.getAddress());
            
            // Set rating
            float rating = (float) restaurant.getRating();
            ratingValue.setText(String.format("%.1f", rating));
            ratingCountTv.setText(String.format("(%d)", restaurant.getTotalRatings()));

            averagePriceTV.setText("Khoảng " + restaurant.getAveragePrice() + "K");
            
            // Set delivery time
            int avgTime = restaurant.getAverageDeliveryTime();
            if (avgTime > 0) {
                deliveryTime.setText(String.format("%d phút", avgTime));
            } else {
                deliveryTime.setText("30 phút");
            }
            
            // Set distance (placeholder for now)
//            distance.setText(String.format("%s", currencyFormat.format(restaurant.getDeliveryFee())));
            
            // Set restaurant image
            ImageUtils.loadImage(restaurant.getImageUrl(), restaurantImage, R.drawable.bg, R.drawable.logo2);

            
            // Add cuisine type chips
            categoryChipGroup.removeAllViews();
            List<String> cuisineTypes = restaurant.getCuisineTypes();
            if (cuisineTypes != null && !cuisineTypes.isEmpty()) {
                // Only add up to 2 cuisine types to avoid overcrowding
                int maxChips = Math.min(cuisineTypes.size(), 2);
                for (int i = 0; i < maxChips; i++) {
                    Chip chip = new Chip(context);
                    chip.setText(cuisineTypes.get(i));
                    chip.setChipBackgroundColorResource(android.R.color.transparent);
                    chip.setChipStrokeWidth(1f);
                    chip.setChipStrokeColorResource(android.R.color.darker_gray);
                    chip.setTextSize(12);
                    categoryChipGroup.addView(chip);
                }
            }
        }
    }
}