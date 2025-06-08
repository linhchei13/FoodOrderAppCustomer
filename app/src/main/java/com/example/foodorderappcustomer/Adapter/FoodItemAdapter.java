package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

public class FoodItemAdapter extends RecyclerView.Adapter<FoodItemAdapter.FoodViewHolder> {

    private List<MenuItem> menuItems;
    private OnFoodItemClickListener listener;
    private NumberFormat currencyFormatter;

    public interface OnFoodItemClickListener {
        void onFoodItemClick(MenuItem menuItem);
        void onAddToCartClick(MenuItem menuItem);
    }

    public FoodItemAdapter(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setOnFoodItemClickListener(OnFoodItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        MenuItem menuItem = menuItems.get(position);
        holder.bind(menuItem);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView foodImage;
        TextView foodName;
        TextView foodPrice;
        TextView originalPrice;
        ImageButton btnAddToCart;

        FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            foodImage = itemView.findViewById(R.id.foodImage);
            foodName = itemView.findViewById(R.id.foodName);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFoodItemClick(menuItems.get(position));
                }
            });

            btnAddToCart.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAddToCartClick(menuItems.get(position));
                }
            });
        }

        void bind(MenuItem menuItem) {
            foodName.setText(menuItem.getName());
            
            // Format the price
            String formattedPrice = currencyFormatter.format(menuItem.getPrice()).replace("₫", "đ");
            foodPrice.setText(formattedPrice);
            
            // If there's a discount, show the original price

                originalPrice.setVisibility(View.GONE);


            // Load image with Glide if URL exists
            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(menuItem.getImageUrl())
                        .placeholder(R.drawable.loading_img)
                        .into(foodImage);
            } else {
                foodImage.setImageResource(R.drawable.logo2);
            }
        }
    }
}
