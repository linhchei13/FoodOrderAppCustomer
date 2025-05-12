package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    private List<CartItem> cartItems;
    private final NumberFormat currencyFormat;
    private CartItemListener listener;

    public interface CartItemListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    public CartItemAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        notifyDataSetChanged();
    }

    public void setListener(CartItemListener listener) {
        this.listener = listener;
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
        private final TextView foodPriceTextView;
        private final TextView toppingsTextView;
        private final TextView quantityTextView;
        private final TextView itemTotalTextView;
        private final Button decreaseButton;
        private final Button increaseButton;
        private final ImageButton removeItemButton;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            foodImageView = itemView.findViewById(R.id.foodImageView);
            foodNameTextView = itemView.findViewById(R.id.foodNameTextView);
            foodPriceTextView = itemView.findViewById(R.id.foodPriceTextView);
            toppingsTextView = itemView.findViewById(R.id.toppingsTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            itemTotalTextView = itemView.findViewById(R.id.itemTotalTextView);
            decreaseButton = itemView.findViewById(R.id.decreaseButton);
            increaseButton = itemView.findViewById(R.id.increaseButton);
            removeItemButton = itemView.findViewById(R.id.removeItemButton);
        }

        public void bind(CartItem cartItem) {
            // Set food name
            foodNameTextView.setText(cartItem.getItemName());

            // Set food price
            String formattedPrice = currencyFormat.format(cartItem.getItemPrice()).replace("₫", "đ");
            foodPriceTextView.setText(formattedPrice);

            // Set toppings text
            if (cartItem.getToppings() != null && !cartItem.getToppings().isEmpty()) {
                String toppingsText = "Toppings: " + cartItem.getToppings().stream()
                        .map(Option::getName)
                        .collect(Collectors.joining(", "));
                toppingsTextView.setText(toppingsText);
                toppingsTextView.setVisibility(View.VISIBLE);
            } else {
                toppingsTextView.setVisibility(View.GONE);
            }

            // Set quantity
            quantityTextView.setText(String.valueOf(cartItem.getQuantity()));

            // Set total price
            String formattedTotal = currencyFormat.format(cartItem.getTotalPrice()).replace("₫", "đ");
            itemTotalTextView.setText(formattedTotal);

            // Load food image
            if (cartItem.getImageUrl() != null && !cartItem.getImageUrl().isEmpty()) {
                ImageUtils.loadImage(
                        itemView.getContext(),
                        cartItem.getImageUrl(),
                        foodImageView,
                        R.drawable.ic_restaurant,
                        R.drawable.ic_restaurant
                );
            } else {
                foodImageView.setImageResource(R.drawable.ic_restaurant);
            }

            // Set click listeners
            decreaseButton.setOnClickListener(v -> {
                if (listener != null) {
                    int newQuantity = cartItem.getQuantity() - 1;
                    if (newQuantity >= 1) {
                        listener.onQuantityChanged(cartItem, newQuantity);
                    }
                }
            });

            increaseButton.setOnClickListener(v -> {
                if (listener != null) {
                    int newQuantity = cartItem.getQuantity() + 1;
                    listener.onQuantityChanged(cartItem, newQuantity);
                }
            });

            removeItemButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveItem(cartItem);
                }
            });
        }
    }
} 