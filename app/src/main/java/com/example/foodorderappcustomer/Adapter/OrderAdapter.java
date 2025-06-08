package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.Order;
import com.example.foodorderappcustomer.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orders;
    private SimpleDateFormat dateFormat;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateData(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView textOrderId;
        private TextView textRestaurantName;
        private TextView textOrderTime;
        private TextView textTotal;
        private TextView textStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderId = itemView.findViewById(R.id.textOrderId);
            textRestaurantName = itemView.findViewById(R.id.textRestaurantName);
            textOrderTime = itemView.findViewById(R.id.textOrderTime);
            textTotal = itemView.findViewById(R.id.textTotal);
            textStatus = itemView.findViewById(R.id.textStatus);

            // Add click listener to the entire item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onOrderClick(orders.get(position));
                }
            });
        }

        public void bind(Order order) {
            textOrderId.setText("Đơn hàng #" + order.getId());
            textRestaurantName.setText(order.getRestaurantName());
            textOrderTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(order.getOrderTime()));
            textTotal.setText(String.format("%,.0f đ", order.getTotal()));

            
            // Set status text and color
            String statusText;
            int statusColor;
            switch (order.getStatus()) {
                case "pending":
                    statusText = "Đang chờ xác nhận";
                    statusColor = itemView.getContext().getResources().getColor(R.color.status_pending);
                    break;
                case "processing":
                    statusText = "Đang xử lý";
                    statusColor = itemView.getContext().getResources().getColor(R.color.status_processing);
                    break;
                case "completed":
                    statusText = "Hoàn thành";
                    statusColor = itemView.getContext().getResources().getColor(R.color.status_completed);
                    break;
                case "cancelled":
                    statusText = "Đã hủy";
                    statusColor = itemView.getContext().getResources().getColor(R.color.status_cancelled);
                    break;
                case "canceled":
                    statusText = "Đã hủy";
                    statusColor = itemView.getContext().getResources().getColor(R.color.status_cancelled);
                    break;
                case "contacted":
                    statusText = "Đang giao";
                    statusColor = itemView.getContext().getResources().getColor(R.color.rebecca_purple);
                    break;
                default:
                    statusText = order.getStatus();
                    statusColor = itemView.getContext().getResources().getColor(R.color.status_default);
            }
            textStatus.setText(statusText);
            textStatus.setTextColor(statusColor);
        }
    }
} 