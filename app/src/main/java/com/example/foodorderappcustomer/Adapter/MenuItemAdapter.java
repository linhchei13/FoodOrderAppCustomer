package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder> {

    private List<MenuItem> menuItems;
    private OnItemClickListener listener;
    private NumberFormat currencyFormat;

    public interface OnItemClickListener {
        void onItemClick(MenuItem menuItem);
        void onAddClick(MenuItem menuItem, View view);
    }

    public MenuItemAdapter(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<MenuItem> newMenuItems) {
        this.menuItems = newMenuItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new MenuItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position) {
        MenuItem menuItem = menuItems.get(position);
        holder.bind(menuItem, listener);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class MenuItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView menuItemImg;
        private TextView itemName;
        private TextView itemPrice;
        private TextView itemDescription;
        private RatingBar menuItemRating;
        private TextView ratingText;
        private TextView addButton;

        public MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            menuItemImg = itemView.findViewById(R.id.menuItemImg);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            itemDescription = itemView.findViewById(R.id.itemDescription);
            menuItemRating = itemView.findViewById(R.id.menuItemRating);
            ratingText = itemView.findViewById(R.id.ratingText);
            addButton = itemView.findViewById(R.id.add);
        }

        public void bind(final MenuItem menuItem, final OnItemClickListener listener) {
            // Set name
            itemName.setText(menuItem.getName());

            // Format price
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String formattedPrice = currencyFormat.format(menuItem.getPrice()).replace("₫", "đ");
            itemPrice.setText(formattedPrice);
            
            // Set description
            itemDescription.setText(menuItem.getDescription());
            
            // Set rating
            if (menuItem.getRating() > 0) {
                menuItemRating.setRating(menuItem.getRating());
                ratingText.setText(String.format("%.1f", menuItem.getRating()));
            } else {
                menuItemRating.setRating(0);
                ratingText.setText("New");
            }

            // Set image - try to load from Firebase first if image URL is available
            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                // Use ImageUtils to load the image
                ImageUtils.loadImage(
                    itemView.getContext(),
                    menuItem.getImageUrl(),
                    menuItemImg,
                    R.drawable.bg,
                    getDefaultImage(menuItem.getCategory())
                );
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(menuItem);
                }
            });

            addButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddClick(menuItem, v);
                }
            });
        }
        
        // Helper method to get default image based on category
        private int getDefaultImage(String category) {
            if (category == null) return R.drawable.nemnuong;

            switch (category.toLowerCase()) {
                case "cơm":
                    return R.drawable.icons_rice;
                case "phở, bún":
                    return R.drawable.icons_pho;
                case "đồ uống":
                    return R.drawable.icons_drink;
                case "bánh mỳ":
                    return R.drawable.icons8_bread;
                default:
                    return R.drawable.nemnuong;
            }

        }
    }
}