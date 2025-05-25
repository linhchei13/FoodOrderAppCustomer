package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.FoodItem;
import com.example.foodorderappcustomer.R;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<FoodItem> orderItems;

    public OrderItemAdapter(List<FoodItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        FoodItem item = orderItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private TextView textItemQuantity;
        private TextView textItemName;
        private TextView textItemPrice;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textItemQuantity = itemView.findViewById(R.id.textItemQuantity);
            textItemName = itemView.findViewById(R.id.textItemName);
            textItemPrice = itemView.findViewById(R.id.textItemPrice);
        }

        public void bind(FoodItem item) {
            textItemQuantity.setText(item.getQuantity() + "x");
            textItemName.setText(item.getItemName());
            textItemPrice.setText(String.format("%,.0f đ", item.getItemPrice()));
        }
    }
} 