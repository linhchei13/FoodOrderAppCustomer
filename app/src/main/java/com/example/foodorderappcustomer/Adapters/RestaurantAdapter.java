package com.example.foodorderappcustomer.Adapters;

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
import com.example.foodorderappcustomer.RestaurantDetailActivity;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    private List<Restaurant> restaurants;
    private Context context;

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
            Intent intent = new Intent(context, RestaurantDetailActivity.class);
            intent.putExtra("RESTAURANT_ID", restaurant.getId());
            intent.putExtra("RESTAURANT_NAME", restaurant.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    static class RestaurantViewHolder extends RecyclerView.ViewHolder {
        private ImageView restaurantImage;
        private TextView restaurantName;
        private TextView restaurantAddress;
        private TextView restaurantRating;

        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantAddress = itemView.findViewById(R.id.restaurantAddress);
//            restaurantRating = itemView.findViewById(R.id.ratingText);
        }

        public void bind(Restaurant restaurant) {
            restaurantName.setText(restaurant.getName());
            restaurantAddress.setText(restaurant.getAddress());
//            restaurantRating.setText(String.valueOf(restaurant.getRating()));
            restaurantImage.setImageResource(restaurant.getImageResource());
        }
    }
}