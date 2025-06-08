package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.OrderItemManager;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder> {

    private List<MenuItem> menuItems;
    private OnItemClickListener listener;
    private static NumberFormat currencyFormat;
    private OrderItemManager orderItemManager;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(MenuItem menuItem);
        void onAddClick(MenuItem menuItem, View view);
        void onDecreaseClick(MenuItem menuItem, View view);
    }

    public MenuItemAdapter(Context context, List<MenuItem> menuItems) {
        this.context = context;
        this.menuItems = menuItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.orderItemManager = OrderItemManager.getInstance(context);

        // Listen for cart updates
        orderItemManager.setOnCartUpdateListener((cartItems, total) -> {
            notifyDataSetChanged(); // Refresh all items when cart changes
        });
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<MenuItem> newMenuItems) {
        this.menuItems = newMenuItems;
        notifyDataSetChanged();
    }

    // Add method to update specific item quantity
    public void updateItemQuantity(String itemId, int quantity) {
        for (int i = 0; i < menuItems.size(); i++) {
            if (menuItems.get(i).getId().equals(itemId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    private OrderItem findItemInCart(MenuItem menuItem) {
        for (OrderItem cartItem : orderItemManager.getCartItems()) {
            if (cartItem.getItemId().equals(menuItem.getId())) {
                return cartItem;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new MenuItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position) {
        MenuItem menuItem = menuItems.get(position);
        holder.bind(menuItem, listener, orderItemManager);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class MenuItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView menuItemImg;
        private TextView itemName;
        private TextView itemPrice;
        private TextView sales;
        private ImageButton decreaseButton;
        private TextView quantityTextView;
        private ImageButton increaseButton;

        public MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            menuItemImg = itemView.findViewById(R.id.menuItemImg);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            sales = itemView.findViewById(R.id.salesTV);
            decreaseButton = itemView.findViewById(R.id.decreaseButton);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            increaseButton = itemView.findViewById(R.id.increaseButton);
        }

        public void bind(final MenuItem menuItem, final OnItemClickListener listener, OrderItemManager orderItemManager) {
            // Set name
            itemName.setText(menuItem.getName());
            sales.setText(String.valueOf(menuItem.getSales()));

            // Format price
            String formattedPrice = currencyFormat.format(menuItem.getPrice()).replace("₫", "đ");
            itemPrice.setText(formattedPrice);

            // Load image safely
            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                try {
                    // Sử dụng context của itemView thay vì activity context
                    Context context = itemView.getContext();
                    if (context != null) {
                        Glide.with(context)
                                .load(menuItem.getImageUrl())
                                .placeholder(R.drawable.loading_img)
                                .error(R.drawable.logo2)
                                .into(menuItemImg);
                    }
                } catch (Exception e) {
                    Log.e("MenuItemAdapter", "Error loading image: " + e.getMessage());
                    menuItemImg.setImageResource(R.drawable.logo2);
                }
            } else {
                menuItemImg.setImageResource(R.drawable.logo2);
            }

            // Always update quantity UI when binding
            updateQuantityUI(menuItem, orderItemManager);

            // Set up click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(menuItem);
                }
            });

            // Set up increase button click listener
            increaseButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddClick(menuItem, v);
                }
            });

            // Set up decrease button click listener
            decreaseButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDecreaseClick(menuItem, v);
                }
            });
        }

        private void updateQuantityUI(MenuItem menuItem, OrderItemManager orderItemManager) {
            // Check if item is in cart
            OrderItem currentItem = null;
            for (OrderItem item : orderItemManager.getCartItems()) {
                if (item.getItemId().equals(menuItem.getId())) {
                    currentItem = item;
                    break;
                }
            }

            // Show/hide quantity controls based on cart status
            if (currentItem != null && currentItem.getQuantity() > 0) {
                // Item is in cart, show quantity controls
                decreaseButton.setVisibility(View.VISIBLE);
                quantityTextView.setVisibility(View.VISIBLE);
                quantityTextView.setText(String.valueOf(currentItem.getQuantity()));

                // Add animation when quantity changes
                animateQuantityChange(quantityTextView);
            } else {
                // Item is not in cart, hide quantity controls
                decreaseButton.setVisibility(View.GONE);
                quantityTextView.setVisibility(View.GONE);
            }
        }

        private void animateQuantityChange(TextView textView) {
            // Simple scale animation for quantity change
            android.view.animation.ScaleAnimation scaleAnimation = new android.view.animation.ScaleAnimation(
                    0.8f, 1.0f, // Start and end X scale
                    0.8f, 1.0f, // Start and end Y scale
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y
            );
            scaleAnimation.setDuration(200);
            textView.startAnimation(scaleAnimation);
        }
    }

    // Method to refresh specific item when quantity changes
    public void refreshItem(String itemId) {
        for (int i = 0; i < menuItems.size(); i++) {
            if (menuItems.get(i).getId().equals(itemId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    // Method to refresh all items
    public void refreshAllItems() {
        notifyDataSetChanged();
    }
}