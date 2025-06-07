package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.OrderItem;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.OrderItemManager;
import com.example.foodorderappcustomer.util.ImageUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder> {

    private List<MenuItem> menuItems;
    private OnItemClickListener listener;
    private NumberFormat currencyFormat;
    private OrderItemManager orderItemManager;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(MenuItem menuItem);
        void onAddClick(MenuItem menuItem, View view);
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
        private TextView itemDescription;
        private View quantityControls;
        private ImageButton decreaseButton;
        private TextView quantityTextView;
        private ImageButton increaseButton;

        public MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            menuItemImg = itemView.findViewById(R.id.menuItemImg);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            quantityControls = itemView.findViewById(R.id.quantityControls);
            decreaseButton = itemView.findViewById(R.id.decreaseButton);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            increaseButton = itemView.findViewById(R.id.increaseButton);
        }

        public void bind(final MenuItem menuItem, final OnItemClickListener listener, OrderItemManager orderItemManager) {
            // Set name
            itemName.setText(menuItem.getName());

            // Format price
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String formattedPrice = currencyFormat.format(menuItem.getPrice()).replace("₫", "đ");
            itemPrice.setText(formattedPrice);
            
            // Set description
//            itemDescription.setText(menuItem.getDescription());
//
//            // Set rating
//            if (menuItem.getRating() > 0) {
//                menuItemRating.setRating(menuItem.getRating());
//                ratingText.setText(String.format("%.1f", menuItem.getRating()));
//            } else {
//                menuItemRating.setRating(0);
//                ratingText.setText("New");
//            }

            // Set image - try to load from Firebase first if image URL is available
            if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
                // Use ImageUtils to load the image
                ImageUtils.loadImage(
                    menuItem.getImageUrl(),
                    menuItemImg,
                    R.drawable.bg,
                    R.drawable.logo2
                );
            }

            // Check if item is in cart
            OrderItem curentItem = null;
            for (OrderItem item : orderItemManager.getCartItems()) {
                if (item.getItemId().equals(menuItem.getId())) {
                    curentItem = item;
                    break;
                }
            }

            // Show/hide quantity controls based on cart status
            if (curentItem != null) {
                final OrderItem cartItem = curentItem;
                decreaseButton.setVisibility(View.VISIBLE);
                quantityTextView.setText(String.valueOf(cartItem.getQuantity()));
                quantityTextView.setVisibility(View.VISIBLE);

                // Set up quantity control listeners
                decreaseButton.setOnClickListener(v -> {
                    int newQuantity = cartItem.getQuantity() - 1;
                    if (newQuantity <= 0) {
                        orderItemManager.removeItem(cartItem);
                    } else {
                        orderItemManager.updateItemQuantity(cartItem, newQuantity);
                    }
                });

                increaseButton.setOnClickListener(v -> {
                    orderItemManager.updateItemQuantity(cartItem, cartItem.getQuantity() + 1);
                });
            } else {
                decreaseButton.setVisibility(View.GONE);
                quantityTextView.setVisibility(View.GONE);
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(menuItem);
                }
            });

            increaseButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddClick(menuItem, v);
                }
            });
        }

    }
}