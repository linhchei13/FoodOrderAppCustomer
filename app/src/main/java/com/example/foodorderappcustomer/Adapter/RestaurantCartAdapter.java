//package com.example.foodorderappcustomer.Adapter;
//
//import android.content.Context;
//import android.content.Intent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.foodorderappcustomer.Models.CartItem;
//import com.example.foodorderappcustomer.OrderActivity;
//import com.example.foodorderappcustomer.R;
//import com.example.foodorderappcustomer.util.ImageUtils;
//
//import java.text.NumberFormat;
//import java.util.List;
//import java.util.Locale;
//
//public class RestaurantCartAdapter extends RecyclerView.Adapter<RestaurantCartAdapter.RestaurantCartViewHolder> {
//
//    private List<CartItem> restaurantCarts;
//    private final NumberFormat currencyFormat;
//    private final Context context;
//
//    public RestaurantCartAdapter(Context context, List<CartItem> restaurantCarts) {
//        this.context = context;
//        this.restaurantCarts = restaurantCarts;
//        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
//    }
//
//    public void setRestaurantCarts(List<CartItem> restaurantCarts) {
//        this.restaurantCarts = restaurantCarts;
//        notifyDataSetChanged();
//    }
//
//    @NonNull
//    @Override
//    public RestaurantCartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
//        return new RestaurantCartViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull RestaurantCartViewHolder holder, int position) {
//        CartItem restaurantCart = restaurantCarts.get(position);
//        holder.bind(restaurantCart);
//    }
//
//    @Override
//    public int getItemCount() {
//        return restaurantCarts.size();
//    }
//
//    class RestaurantCartViewHolder extends RecyclerView.ViewHolder {
//        private final ImageView restaurantImageView;
//        private final TextView restaurantNameTextView;
//        private final TextView restaurantTotalTextView;
//        private final RecyclerView itemsRecyclerView;
//        private final View restaurantHeader;
//
//        public RestaurantCartViewHolder(@NonNull View itemView) {
//            super(itemView);
//            restaurantImageView = itemView.findViewById(R.id.restaurantImageView);
//            restaurantNameTextView = itemView.findViewById(R.id.restaurantNameTextView);
//            restaurantTotalTextView = itemView.findViewById(R.id.restaurantTotalTextView);
//            itemsRecyclerView = itemView.findViewById(R.id.itemsRecyclerView);
//            restaurantHeader = itemView.findViewById(R.id.restaurantHeader);
//        }
//
//        public void bind(CartItem restaurantCart) {
//            // Set restaurant name
//            restaurantNameTextView.setText(restaurantCart.getRestaurantName());
//
//            // Set total price
//            String formattedTotal = currencyFormat.format(restaurantCart.getTotalPrice()).replace("₫", "đ");
//            restaurantTotalTextView.setText("Tổng tiền: " + formattedTotal);
//
//            // Load restaurant image
//            if (restaurantCart.getRestaurantImage() != null && !restaurantCart.getRestaurantImage().isEmpty()) {
//                ImageUtils.loadImage(
//                        restaurantCart.getRestaurantImage(),
//                        restaurantImageView,
//                        R.drawable.ic_restaurant,
//                        R.drawable.ic_restaurant
//                );
//            } else {
//                restaurantImageView.setImageResource(R.drawable.ic_restaurant);
//            }
//
//            // Setup items recycler view
//            CartItemAdapter itemsAdapter = new CartItemAdapter(restaurantCart.getItems());
//            itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
//            itemsRecyclerView.setAdapter(itemsAdapter);
//
//            // Setup click listener for restaurant header
//            restaurantHeader.setOnClickListener(v -> {
//                Intent intent = new Intent(context, OrderActivity.class);
//                intent.putExtra("restaurantId", restaurantCart.getRestaurantId());
//                intent.putExtra("restaurantName", restaurantCart.getRestaurantName());
//                context.startActivity(intent);
//            });
//        }
//    }
//}