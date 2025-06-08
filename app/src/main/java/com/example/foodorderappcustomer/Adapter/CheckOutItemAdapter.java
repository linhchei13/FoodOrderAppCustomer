package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CheckOutItemAdapter extends RecyclerView.Adapter<CheckOutItemAdapter.CheckOutItemViewHolder> {
    private List<OrderItem> orderItems;
    private Context context;
    private final NumberFormat currencyFormat;

    public CheckOutItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CheckOutItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_checkout, parent, false);
        return new CheckOutItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckOutItemViewHolder holder, int position) {
        OrderItem orderItem = orderItems.get(position);
        holder.bind(orderItem);
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    class CheckOutItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView itemImageView;
        private final TextView quantityTextView;
        private final TextView itemNameTextView;
        private final TextView toppingsTextView;
        private final TextView priceTextView;

        public CheckOutItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImageView = itemView.findViewById(R.id.itemImageView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            itemNameTextView = itemView.findViewById(R.id.itemNameTextView);
            toppingsTextView = itemView.findViewById(R.id.toppingsTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
        }

        public void bind(OrderItem orderItem) {
            // Set quantity and item name
            quantityTextView.setText(orderItem.getQuantity() + "x");
            itemNameTextView.setText(orderItem.getItemName());

            // Set toppings if available
            if (orderItem.getToppings() != null && !orderItem.getToppings().isEmpty()) {
                StringBuilder toppingsText = new StringBuilder();
                for (Option topping : orderItem.getToppings()) {
                    if (toppingsText.length() > 0) {
                        toppingsText.append(", ");
                    }
                    toppingsText.append(topping.getName());
                }
                toppingsTextView.setText(toppingsText.toString());
                toppingsTextView.setVisibility(View.VISIBLE);
            } else {
                toppingsTextView.setVisibility(View.GONE);
            }

            // Set total price
            String formattedPrice = currencyFormat.format(orderItem.getTotalPrice()).replace("₫", "đ");
            priceTextView.setText(formattedPrice);

            // Load item image
            if (orderItem.getImageUrl() != null && !orderItem.getImageUrl().isEmpty()) {
                try {
                    Glide.with(context)
                            .load(orderItem.getImageUrl())
                            .placeholder(R.drawable.loading_img)
                            .error(R.drawable.logo2)
                            .into(itemImageView);
                } catch (Exception e) {
                    itemImageView.setImageResource(R.drawable.logo2);
                }
            } else {
                itemImageView.setImageResource(R.drawable.logo2);
            }
        }
    }
}