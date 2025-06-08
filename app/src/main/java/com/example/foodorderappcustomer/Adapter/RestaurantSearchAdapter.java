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
                restaurantRating.setText(String.format("⭐ %.1f (%d+)",
                        restaurant.getRating(), restaurant.getTotalRatings()));
            } else {
                restaurantRating.setText("⭐ Chưa có đánh giá");
            }

            // Set distance (you might want to calculate this based on user location)
            restaurantDistance.setText("📍 0.9 km");

            // Set price range
            if (restaurant.getAveragePrice() != null && !restaurant.getAveragePrice().isEmpty()) {
                restaurantPriceRange.setText("💰 Khoảng " + restaurant.getAveragePrice() + "K");
            } else {
                restaurantPriceRange.setText("💰 Khoảng 20K");
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

            // Set up menu items horizontal scroll
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
            // First check if we have matching menu items for this restaurant
            if (matchingMenuItems.containsKey(restaurantId)) {
                List<MenuItem> items = matchingMenuItems.get(restaurantId);
                if (items != null && !items.isEmpty()) {
                    // Show matching menu items
                    MenuItemSearchAdapter adapter = new MenuItemSearchAdapter(items, context);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                    menuItemsRecyclerView.setLayoutManager(layoutManager);
                    menuItemsRecyclerView.setAdapter(adapter);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    menuItemsRecyclerView.setHasFixedSize(true);
                    return;
                }
            }

            // If no matching items, show regular menu items
            if (restaurantMenuItems.containsKey(restaurantId)) {
                List<MenuItem> menuItems = restaurantMenuItems.get(restaurantId);
                if (menuItems != null && !menuItems.isEmpty()) {
                    MenuItemSearchAdapter adapter = new MenuItemSearchAdapter(menuItems, context);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                    menuItemsRecyclerView.setLayoutManager(layoutManager);
                    menuItemsRecyclerView.setAdapter(adapter);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    menuItemsRecyclerView.setHasFixedSize(true);
                } else {
                    menuItemsRecyclerView.setVisibility(View.GONE);
                }
            } else {
                menuItemsRecyclerView.setVisibility(View.GONE);
            }
        }
    }
}
