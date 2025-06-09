package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.Restaurant;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.RestaurantMenuActivity;
import com.example.foodorderappcustomer.util.OrderItemManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RestaurantSearchAdapter extends RecyclerView.Adapter<RestaurantSearchAdapter.RestaurantViewHolder> {
    private List<Restaurant> restaurants;
    private Map<String, List<MenuItem>> restaurantMenuItems;
    private Map<String, List<MenuItem>> matchingMenuItems;
    private Context context;
    private OrderItemManager orderItemManager;
    private MenuItemSearchAdapter menuItemAdapter;

    public RestaurantSearchAdapter(List<Restaurant> restaurants, Map<String, List<MenuItem>> restaurantMenuItems, Context context) {
        this.restaurants = restaurants;
        this.restaurantMenuItems = restaurantMenuItems;
        this.matchingMenuItems = new HashMap<>();
        this.context = context;
        this.orderItemManager = OrderItemManager.getInstance(context);
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant_search, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);
        holder.bind(restaurant);
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    public void updateMenuItems(Map<String, List<MenuItem>> menuItems) {
        this.restaurantMenuItems = menuItems;
        notifyDataSetChanged();
    }

    public void updateMatchingMenuItems(Map<String, List<MenuItem>> matchingMenuItems) {
        this.matchingMenuItems = matchingMenuItems;
        notifyDataSetChanged();
    }

    public void refreshMenuItem(String menuItemId) {
        // Update matching menu items if they exist
        if (matchingMenuItems != null) {
            for (Map.Entry<String, List<MenuItem>> entry : matchingMenuItems.entrySet()) {
                List<MenuItem> items = entry.getValue();
                for (MenuItem item : items) {
                    if (item.getId().equals(menuItemId)) {
                        // If this item is in matching items, refresh the restaurant
                        for (int i = 0; i < restaurants.size(); i++) {
                            if (restaurants.get(i).getId().equals(entry.getKey())) {
                                notifyItemChanged(i);
                                return;
                            }
                        }
                    }
                }
            }
        }

        // If not found in matching items, check all menu items
        for (Map.Entry<String, List<MenuItem>> entry : restaurantMenuItems.entrySet()) {
            List<MenuItem> items = entry.getValue();
            for (MenuItem item : items) {
                if (item.getId().equals(menuItemId)) {
                    // Find and refresh the restaurant
                    for (int i = 0; i < restaurants.size(); i++) {
                        if (restaurants.get(i).getId().equals(entry.getKey())) {
                            notifyItemChanged(i);
                            return;
                        }
                    }
                }
            }
        }
    }

    class RestaurantViewHolder extends RecyclerView.ViewHolder {
        private ImageView restaurantImage;
        private TextView restaurantName;
        private TextView restaurantRating;
        private TextView restaurantDistance;
        private TextView restaurantPriceRange;
        private RecyclerView menuItemsRecyclerView;
        private LinearLayout restaurantContainer;

        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantRating = itemView.findViewById(R.id.restaurantRating);
            restaurantDistance = itemView.findViewById(R.id.restaurantDistance);
            restaurantPriceRange = itemView.findViewById(R.id.restaurantPriceRange);
            menuItemsRecyclerView = itemView.findViewById(R.id.menuItemsRecyclerView);
            restaurantContainer = itemView.findViewById(R.id.restaurantContainer);
        }

        public void bind(Restaurant restaurant) {
            // Set restaurant basic info
            restaurantName.setText(restaurant.getName());

            // Set rating
            if (restaurant.getRating() > 0) {
                restaurantRating.setText(String.format("⭐ %.1f (%d)",
                        restaurant.getRating(), restaurant.getTotalRatings()));
            } else {
                restaurantRating.setText("");
            }

            // Set distance (you might want to calculate this based on user location)
            restaurantDistance.setText("📍" + String.format("%.1f km", restaurant.getDistance()));

            // Set price range
            if (restaurant.getAveragePrice() != null && !restaurant.getAveragePrice().isEmpty()) {
                restaurantPriceRange.setText("💰 Khoảng " + restaurant.getAveragePrice() + "K");
            }

            // Load restaurant image
            if (restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(restaurant.getImageUrl())
                        .placeholder(R.drawable.loading_img)
                        .error(R.drawable.loading_img)
                        .into(restaurantImage);
            } else {
                restaurantImage.setImageResource(R.drawable.loading_img);
            }

            // Setup menu items
            setupMenuItems(restaurant.getId());

            // Set click listener for restaurant
            restaurantContainer.setOnClickListener(v -> {
                Intent intent = new Intent(context, RestaurantMenuActivity.class);
                intent.putExtra("RESTAURANT_ID", restaurant.getId());
                intent.putExtra("RESTAURANT_NAME", restaurant.getName());
                context.startActivity(intent);
            });
        }

        private void setupMenuItems(String restaurantId) {
            RecyclerView menuItemsRecyclerView = itemView.findViewById(R.id.menuItemsRecyclerView);
            List<MenuItem> itemsToShow;

            if (matchingMenuItems != null && matchingMenuItems.containsKey(restaurantId)) {
                // Show matching menu items if available
                itemsToShow = matchingMenuItems.get(restaurantId);
            } else if (restaurantMenuItems.containsKey(restaurantId)) {
                // Otherwise show all menu items
                itemsToShow = restaurantMenuItems.get(restaurantId);
            } else {
                itemsToShow = new ArrayList<>();
            }

            menuItemAdapter = new MenuItemSearchAdapter(itemsToShow, context);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            menuItemsRecyclerView.setLayoutManager(layoutManager);
            menuItemsRecyclerView.setAdapter(menuItemAdapter);
        }
    }
}
