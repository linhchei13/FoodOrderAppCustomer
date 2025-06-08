package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.CheckOutActivity;
import com.example.foodorderappcustomer.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {
    private List<CartItem> cartItems;
    private Context context;
    private final NumberFormat currencyFormat;
    private OnCartItemClickListener listener;

    public interface OnCartItemClickListener {
        void onCartItemClick(CartItem cartItem);
    }

    public CartItemAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        notifyDataSetChanged();
    }

    public void setOnCartItemClickListener(OnCartItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart_restaurant, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        holder.bind(cartItem);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCartItemClick(cartItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView restaurantImageView;
        private final TextView restaurantNameTextView;
        private final TextView addressTextView;
        private final TextView itemCountTextView;
        private final TextView distanceTextView;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImageView = itemView.findViewById(R.id.restaurantImageView);
            restaurantNameTextView = itemView.findViewById(R.id.restaurantNameTextView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            itemCountTextView = itemView.findViewById(R.id.itemCountTextView);
            distanceTextView = itemView.findViewById(R.id.distanceTextView);
        }

        public void bind(CartItem cartItem) {
            // Set restaurant name and address
            restaurantNameTextView.setText(cartItem.getRestaurantName());

            if (cartItem.getAddress() != null && !cartItem.getAddress().isEmpty()) {
                addressTextView.setText(cartItem.getAddress());
                addressTextView.setVisibility(View.VISIBLE);
            } else {
                addressTextView.setVisibility(View.GONE);
            }

            // Set item count and distance
            itemCountTextView.setText(cartItem.getTotalQuantity() + " món");
            distanceTextView.setText(cartItem.getFormattedDistance());

            // Load restaurant image
            if (cartItem.getRestaurantImage() != null && !cartItem.getRestaurantImage().isEmpty()) {
                try {
                    Glide.with(context)
                            .load(cartItem.getRestaurantImage())
                            .placeholder(R.drawable.loading_img)
                            .error(R.drawable.logo2)
                            .into(restaurantImageView);
                } catch (Exception e) {
                    restaurantImageView.setImageResource(R.drawable.logo2);
                }
            } else {
                restaurantImageView.setImageResource(R.drawable.logo2);
            }
        }
    }
}