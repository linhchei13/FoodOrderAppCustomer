package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.FoodItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    private List<FoodItem> cartItems;
    private final NumberFormat currencyFormat;
    private CartItemListener listener;

    public interface CartItemListener {
        void onQuantityChanged(FoodItem item, int newQuantity);
        void onRemoveItem(FoodItem item);
    }

    public CartItemAdapter(List<FoodItem> cartItems) {
        this.cartItems = cartItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setCartItems(List<FoodItem> cartItems) {
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
        FoodItem cartItem = cartItems.get(position);
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
        private final TextView distance;
        private final TextView quantityTextView;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            foodImageView = itemView.findViewById(R.id.foodImageView);
            foodNameTextView = itemView.findViewById(R.id.foodNameTextView);
            deliveryTime = itemView.findViewById(R.id.deliveryTime);
            distance = itemView.findViewById(R.id.distance);
            quantityTextView = itemView.findViewById(R.id.quantity);

        }

        public void bind(FoodItem cartItem) {
            // Set food name
            foodNameTextView.setText(cartItem.getItemName());

            // Set food price
            String formattedPrice = currencyFormat.format(cartItem.getItemPrice()).replace("₫", "đ");
            deliveryTime.setText(formattedPrice);

            // Set toppings text
            if (cartItem.getToppings() != null && !cartItem.getToppings().isEmpty()) {
                String toppingsText = "Toppings: " + cartItem.getToppings().stream()
                        .map(Option::getName)
                        .collect(Collectors.joining(", "));
                distance.setText(toppingsText);
                distance.setVisibility(View.VISIBLE);
            } else {
                distance.setVisibility(View.GONE);
            }

            // Set quantity
            quantityTextView.setText(String.valueOf(cartItem.getQuantity()));


            // Load food image
            if (cartItem.getImageUrl() != null && !cartItem.getImageUrl().isEmpty()) {
                ImageUtils.loadImage(
                        cartItem.getImageUrl(),
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