package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.OrderActivity;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;

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
            .inflate(R.layout.item_cart, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        holder.bind(cartItem);
        holder.itemView.setOnClickListener(v -> {
            listener.onCartItemClick(cartItems.get(position));
            Intent intent = new Intent(context, OrderActivity.class);
            intent.putExtra("RESTAURANT_ID", cartItems.get(position).getRestaurantId());
            intent.putExtra("RESTAURANT_NAME", cartItems.get(position).getRestaurantName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView restaurantImageView;
        private final TextView restaurantNameTextView;
        private final TextView totalPriceTextView;
        private final TextView itemCountTextView;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImageView = itemView.findViewById(R.id.foodImageView);
            restaurantNameTextView = itemView.findViewById(R.id.foodName);
            totalPriceTextView = itemView.findViewById(R.id.deliveryTime);
            itemCountTextView = itemView.findViewById(R.id.quantity);

//            itemView.setOnClickListener(v -> {
//                int position = getAdapterPosition();
//                if (position != RecyclerView.NO_POSITION && listener != null) {
//                    listener.onCartItemClick(cartItems.get(position));
//                    Intent intent = new Intent(itemView.getContext(), OrderActivity.class);
//                    intent.putExtra("RESTAURANT_ID", cartItems.get(position).getRestaurantId());
//                    intent.putExtra("RESTAURANT_NAME", cartItems.get(position).getRestaurantName());
//                    itemView.getContext().startActivity(intent);
//                }
//            });
        }

        public void bind(CartItem cartItem) {
            // Set restaurant name
            restaurantNameTextView.setText(cartItem.getRestaurantName());

            // Set total price
            String formattedPrice = currencyFormat.format(cartItem.getTotalPrice()).replace("₫", "đ");
            totalPriceTextView.setText(formattedPrice);

            // Set item count
            itemCountTextView.setText(String.valueOf(cartItem.getTotalQuantity()));

            // Load restaurant image
            if (cartItem.getRestaurantImage() != null && !cartItem.getRestaurantImage().isEmpty()) {
                ImageUtils.loadImage(
                    cartItem.getRestaurantImage(),
                    restaurantImageView,
                    R.drawable.ic_restaurant,
                    R.drawable.ic_restaurant
                );
            } else {
                restaurantImageView.setImageResource(R.drawable.ic_restaurant);
            }
        }
    }
} 