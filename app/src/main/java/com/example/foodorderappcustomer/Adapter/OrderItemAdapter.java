package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.CartItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.R;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<OrderItem> orderItems;
    private OrderItemListener listener;

    public OrderItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    public interface OrderItemListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    public void setListener(OrderItemListener listener) {
        this.listener = listener;
    }
    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private TextView textItemQuantity;
        private TextView textItemName;
        private TextView textItemPrice;

        private ImageView orderItemImage;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textItemQuantity = itemView.findViewById(R.id.textItemQuantity);
            textItemName = itemView.findViewById(R.id.textItemName);
            textItemPrice = itemView.findViewById(R.id.textItemPrice);
            orderItemImage = itemView.findViewById(R.id.orderItemImage);
        }

        public void bind(OrderItem item) {
            textItemQuantity.setText(item.getQuantity() + "x");
            textItemName.setText(item.getItemName());
            textItemPrice.setText(String.format("%,.0f đ", item.getItemPrice() * item.getQuantity()));
            Glide.with(itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.loading_img)
                    .error(R.drawable.logo2)
                    .into(orderItemImage);
        }
    }
} 