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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.MenuItemDetailActivity;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.RestaurantMenuActivity;
import com.example.foodorderappcustomer.util.OrderItemManager;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuItemSearchAdapter extends RecyclerView.Adapter<MenuItemSearchAdapter.MenuItemViewHolder> {
    private List<MenuItem> menuItems;
    private Context context;
    private OrderItemManager orderItemManager;

    public MenuItemSearchAdapter(List<MenuItem> menuItems, Context context) {
        this.menuItems = menuItems;
        this.context = context;
        this.orderItemManager = OrderItemManager.getInstance(context);
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
        private ImageView addButton;
        private LinearLayout menuItemInfo;

        public MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            menuItemImage = itemView.findViewById(R.id.menuItemImage);
            menuItemName = itemView.findViewById(R.id.menuItemName);
            menuItemPrice = itemView.findViewById(R.id.menuItemPrice);
            menuItemOriginalPrice = itemView.findViewById(R.id.menuItemOriginalPrice);
            addButton = itemView.findViewById(R.id.addButton);
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
                // Add strikethrough effect
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

            // Set add button click listener
            addButton.setOnClickListener(v -> {
                // Add animation effect
                Intent intent = new Intent(context, RestaurantMenuActivity.class);

                context.startActivity(intent);
            });

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                // You can add item detail view here if needed
                Toast.makeText(context, menuItem.getName(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}
