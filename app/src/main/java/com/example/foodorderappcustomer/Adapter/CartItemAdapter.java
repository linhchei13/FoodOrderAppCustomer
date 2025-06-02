package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    private List<CartItem> cartItems;
    private final NumberFormat currencyFormat;


    public CartItemAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        holder.bind(cartItem);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView foodImageView;
        private final TextView foodNameTextView;
        private final TextView deliveryTime;
//        private final TextView distance;
        private final TextView quantityTextView;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            foodImageView = itemView.findViewById(R.id.foodImageView);
            foodNameTextView = itemView.findViewById(R.id.foodName);
            deliveryTime = itemView.findViewById(R.id.deliveryTime);
//            distance = itemView.findViewById(R.id.distance);
            quantityTextView = itemView.findViewById(R.id.quantity);

        }

        public void bind(CartItem cartItem) {
            // Set food name
            foodNameTextView.setText(cartItem.getRestaurantName());

            // Set food price
            String formattedPrice = currencyFormat.format(cartItem.getTotalPrice()).replace("₫", "đ");
            deliveryTime.setText(formattedPrice);

            // Set quantity
            quantityTextView.setText(String.valueOf(cartItem.getQuantity()));


            // Load food image
            if (cartItem.getRestaurantImage() != null && !cartItem.getRestaurantImage().isEmpty()) {
                ImageUtils.loadImage(
                        cartItem.getRestaurantImage(),
                        foodImageView,
                        R.drawable.ic_restaurant,
                        R.drawable.ic_restaurant
                );
            } else {
                foodImageView.setImageResource(R.drawable.ic_restaurant);
            }
        }
    }
} 