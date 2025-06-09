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

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.RestaurantMenuActivity;
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
        private TextView categoryName;
        private TextView distanceTextView;
        private TextView averagePriceTV;
        private TextView ratingCountTv;
        private ChipGroup categoryChipGroup;

        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantAddress = itemView.findViewById(R.id.restaurantAddress);
            ratingValue = itemView.findViewById(R.id.ratingValue);
            categoryName = itemView.findViewById(R.id.categoryName);
            averagePriceTV = itemView.findViewById(R.id.averagePriceTV);
            ratingCountTv = itemView.findViewById(R.id.ratingCountTv);
            distanceTextView = itemView.findViewById(R.id.distanceTextView);
        }

        public void bind(Restaurant restaurant) {
            restaurantName.setText(restaurant.getName());
            restaurantAddress.setText(restaurant.getAddress());
            
            // Set rating
            float rating = (float) restaurant.getRating();
            if (rating == 0) {
                ratingValue.setVisibility(View.GONE);
                ratingCountTv.setVisibility(View.GONE);
            } else {
                ratingValue.setVisibility(View.VISIBLE);
                ratingCountTv.setVisibility(View.VISIBLE);
                ratingValue.setText(String.format("%.1f", rating));
                ratingCountTv.setText(String.format("(%d)", restaurant.getTotalRatings()));
            }

            categoryName.setText(restaurant.getCategory());

            // Set distance
            if (restaurant.getDistance() > 0) {
                String distanceText = String.format(Locale.getDefault(), "📍 %.1f km", restaurant.getDistance());
                distanceTextView.setText(distanceText);
                distanceTextView.setVisibility(View.VISIBLE);
            } else {
                distanceTextView.setVisibility(View.GONE);
            }

            averagePriceTV.setText("Khoảng " + restaurant.getAveragePrice() + "K");
            Glide.with(context)
                    .load(restaurant.getImageUrl())
                    .placeholder(R.drawable.loading_img)
                    .error(R.drawable.logo2)
                    .into(restaurantImage);
        }
    }
}