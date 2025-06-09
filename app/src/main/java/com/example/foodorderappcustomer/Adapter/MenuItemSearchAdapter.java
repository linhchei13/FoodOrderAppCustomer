package com.example.foodorderappcustomer.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.MenuItemDetailActivity;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.RestaurantMenuActivity;
import com.example.foodorderappcustomer.util.FoodCustomizationDialog;
import com.example.foodorderappcustomer.util.OrderItemManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MenuItemSearchAdapter extends RecyclerView.Adapter<MenuItemSearchAdapter.MenuItemViewHolder> {
    private List<MenuItem> menuItems;
    private Context context;
    private OrderItemManager orderItemManager;
    private Map<String, Integer> itemQuantities;

    public MenuItemSearchAdapter(List<MenuItem> menuItems, Context context) {
        this.menuItems = menuItems;
        this.context = context;
        this.orderItemManager = OrderItemManager.getInstance(context);
        this.itemQuantities = new HashMap<>();
        // Initialize quantities from cart
        for (MenuItem item : menuItems) {
            itemQuantities.put(item.getId(), orderItemManager.getItemQuantity(item.getId()));
        }
    }

    public void updateData(List<MenuItem> newMenuItems) {
        this.menuItems = newMenuItems;
        // Update quantities for new items
        for (MenuItem item : newMenuItems) {
            if (!itemQuantities.containsKey(item.getId())) {
                itemQuantities.put(item.getId(), orderItemManager.getItemQuantity(item.getId()));
            }
        }
        notifyDataSetChanged();
    }

    public void refreshItem(String itemId) {
        // Update quantity from OrderItemManager
        int quantity = orderItemManager.getItemQuantity(itemId);
        itemQuantities.put(itemId, quantity);
        
        // Find and update the item in the list
        for (int i = 0; i < menuItems.size(); i++) {
            if (menuItems.get(i).getId().equals(itemId)) {
                notifyItemChanged(i);
                Log.d("MenuItemSearchAdapter", "Refreshed item: " + itemId + " with quantity: " + quantity);
                break;
            }
        }
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_search, parent, false);
        return new MenuItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position) {
        MenuItem menuItem = menuItems.get(position);
        holder.bind(menuItem);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    class MenuItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView menuItemImage;
        private TextView menuItemName;
        private TextView menuItemPrice;
        private TextView menuItemOriginalPrice;
        private ImageButton addButton;
        private TextView tvQuantity;

        public MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            menuItemImage = itemView.findViewById(R.id.menuItemImage);
            menuItemName = itemView.findViewById(R.id.menuItemName);
            menuItemPrice = itemView.findViewById(R.id.menuItemPrice);
            menuItemOriginalPrice = itemView.findViewById(R.id.menuItemOriginalPrice);
            addButton = itemView.findViewById(R.id.addButton);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
        }

        public void bind(MenuItem menuItem) {
            menuItemName.setText(menuItem.getName());

            // Format price
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formattedPrice = formatter.format(menuItem.getPrice()) + "đ";
            menuItemPrice.setText(formattedPrice);

            // Show original price if there's a discount
            if (menuItem.getPrice() > 0 && menuItem.getPrice() > menuItem.getPrice()) {
                String originalPrice = formatter.format(menuItem.getPrice()) + "đ";
                menuItemOriginalPrice.setText(originalPrice);
                menuItemOriginalPrice.setVisibility(View.VISIBLE);
                menuItemOriginalPrice.setPaintFlags(menuItemOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                menuItemOriginalPrice.setVisibility(View.GONE);
            }

            // Load image
            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(menuItem.getImageUrl())
                        .placeholder(R.drawable.loading_img)
                        .error(R.drawable.loading_img)
                        .into(menuItemImage);
            } else {
                menuItemImage.setImageResource(R.drawable.loading_img);
            }

            // Get current quantity from OrderItemManager
            int quantity = orderItemManager.getItemQuantity(menuItem.getId());
            Log.d("MenuItemSearchAdapter", "Binding item: " + menuItem.getId() + " with quantity: " + quantity);
            
            // Update UI based on quantity
            if (quantity > 0) {
                tvQuantity.setVisibility(View.VISIBLE);
                tvQuantity.setText(String.valueOf(quantity));
                addButton.setVisibility(View.GONE);
            } else {
                tvQuantity.setVisibility(View.GONE);
                addButton.setVisibility(View.VISIBLE);
            }

            // Set click listeners
            addButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, MenuItemDetailActivity.class);
                intent.putExtra("FOOD_ID", menuItem.getId());
                intent.putExtra("RESTAURANT_ID", menuItem.getRestaurantId());
                intent.putExtra("CURRENT_QUANTITY", quantity);
                ((Activity) context).startActivityForResult(intent, 1);
            });

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MenuItemDetailActivity.class);
                intent.putExtra("FOOD_ID", menuItem.getId());
                intent.putExtra("RESTAURANT_ID", menuItem.getRestaurantId());
                intent.putExtra("CURRENT_QUANTITY", quantity);
                ((Activity) context).startActivityForResult(intent, 1);
            });
        }
    }
}
