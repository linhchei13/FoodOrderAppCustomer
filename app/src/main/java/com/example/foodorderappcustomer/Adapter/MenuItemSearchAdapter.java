package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuItemSearchAdapter extends RecyclerView.Adapter<MenuItemSearchAdapter.MenuItemViewHolder> {

    private List<MenuItem> menuItems;
    private OnMenuItemClickListener listener;
    private NumberFormat currencyFormatter;

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem menuItem);
        void onAddToCartClick(MenuItem menuItem);
    }

    public MenuItemSearchAdapter(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
        notifyDataSetChanged();
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_menu, parent, false);
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
        ImageView imgFood;
        TextView tvItemName;
        TextView tvCurrentPrice;
        TextView tvOriginalPrice;
        TextView tvRestaurantName;
        TextView tvStats;
        ImageView btnAdd;

        MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.imgFood);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvCurrentPrice = itemView.findViewById(R.id.tvCurrentPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvStats = itemView.findViewById(R.id.tvStats);
            btnAdd = itemView.findViewById(R.id.btnAdd);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMenuItemClick(menuItems.get(position));
                }
            });

            btnAdd.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAddToCartClick(menuItems.get(position));
                }
            });
        }

        void bind(MenuItem menuItem) {
            tvItemName.setText(menuItem.getName());

            // Format price
            String formattedPrice = formatPrice(menuItem.getPrice());
            tvCurrentPrice.setText(formattedPrice);

            // If there's a discount (not in your MenuItem model yet, you might want to add it)
            // For now, we'll just hide the original price view
            tvOriginalPrice.setVisibility(View.GONE);

            // Set restaurant name if available
            if (menuItem.getRestaurantId() != null && !menuItem.getRestaurantId().isEmpty()) {
                // Ideally, you would get the restaurant name from the restaurant object
                // For now, we'll hide this or set it to a default value
                tvRestaurantName.setVisibility(View.VISIBLE);
                tvRestaurantName.setText("Restaurant"); // Replace with actual restaurant name
            } else {
                tvRestaurantName.setVisibility(View.GONE);
            }

            // Set stats
            tvStats.setText(menuItem.getStats());

            // Load image with Glide if URL exists
            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                     .load(menuItem.getImageUrl())
                     .placeholder(R.drawable.logo2)
                     .into(imgFood);
            } else {
                imgFood.setImageResource(R.drawable.logo2);
            }
        }

        private String formatPrice(double price) {
            String formatted = currencyFormatter.format(price);
            return formatted.replace("₫", "đ");
        }
    }
}
////            // For now, we'll just hide the original price view
////            tvOriginalPrice.setVisibility(View.GONE);
////
////            // Load image with Glide if URL exists
////            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
////                Glide.with(itemView.getContext())
////                     .load(menuItem.getImageUrl())
////                     .placeholder(R.drawable.logo2)
////                     .into(imgFood);
////            } else {
////                imgFood.setImageResource(R.drawable.logo2);
////            }
////        }
////
////        private String formatPrice(double price) {
////            String formatted = currencyFormatter.format(price);
////            return formatted.replace("₫", "đ");
////        }
//    }
//}
