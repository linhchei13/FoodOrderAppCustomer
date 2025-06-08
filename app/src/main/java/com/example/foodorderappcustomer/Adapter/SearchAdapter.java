//package com.example.foodorderappcustomer.Adapter;
//
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Paint;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.foodorderappcustomer.Models.MenuItem;
//import com.example.foodorderappcustomer.Models.Restaurant;
//import com.example.foodorderappcustomer.R;
//import com.example.foodorderappcustomer.RestaurantMenuActivity;
//
//import java.text.NumberFormat;
//import java.util.List;
//import java.util.Locale;
//
//public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.RestaurantViewHolder> {
//
//    private Context context;
//    private List<Restaurant> restaurants;
//    private NumberFormat currencyFormat;
//
//    public SearchAdapter(Context context, List<Restaurant> restaurants) {
//        this.context = context;
//        this.restaurants = restaurants;
//        this.currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
//    }
//
//    @NonNull
//    @Override
//    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
//        return new RestaurantViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
//        Restaurant restaurant = restaurants.get(position);
//
//        // Restaurant info
//        holder.tvRestaurantName.setText(restaurant.getName());
//        holder.tvRating.setText(String.format("%.1f (%d+)", restaurant.getRating(), 0));
//        holder.tvDistance.setText(String.format("%.1f km", restaurant.getDistance()));
//        holder.tvDeliveryFee.setText(String.format("Khoảng %sd", currencyFormat.format(restaurant.getDeliveryFee())));
//
//        // Promotions
//        holder.tvPromotion.setText("promo");
//        holder.tvDeliveryType.setText("delivery");
//
//        // Super store indicator
//        holder.ivSuperStore.setVisibility(false ? View.VISIBLE : View.GONE);
//
//        // Menu items
//        holder.layoutMenuItems.removeAllViews();
//        List<MenuItem> menuItems = restaurant.getMenuItems();
//
//        for (int i = 0; i < Math.min(menuItems.size(), 4); i++) {
//            MenuItem item = menuItems.get(i);
//            View itemView = LayoutInflater.from(context).inflate(R.layout.item_search_menu, holder.layoutMenuItems, false);
//
//            TextView tvItemName = itemView.findViewById(R.id.tvItemName);
//            TextView tvCurrentPrice = itemView.findViewById(R.id.tvCurrentPrice);
//            TextView tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
//
//            tvItemName.setText(item.getName());
//            tvCurrentPrice.setText(currencyFormat.format(item.getPrice()) + "đ");
//
//            holder.layoutMenuItems.addView(itemView);
//        }
//
//        holder.itemView.setOnClickListener(v -> {
//            Intent intent = new Intent(context, RestaurantMenuActivity.class);
//            intent.putExtra("RESTAURANT_ID", restaurant.getId());
//            intent.putExtra("RESTAURANT_NAME", restaurant.getName());
//            context.startActivity(intent);
//        });
//
////        // Show "View all" if there are more items
////        if (menuItems.size() > 4) {
////            View viewAllView = LayoutInflater.from(context).inflate(R.layout.item_view_all, holder.layoutMenuItems, false);
////            TextView tvViewAll = viewAllView.findViewById(R.id.tvViewAll);
////            tvViewAll.setText(String.format("Xem tất cả %d cửa hàng", menuItems.size()));
////            holder.layoutMenuItems.addView(viewAllView);
////        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return restaurants.size();
//    }
//
//    public static class RestaurantViewHolder extends RecyclerView.ViewHolder {
//        TextView tvRestaurantName, tvRating, tvDistance, tvDeliveryFee;
//        TextView tvPromotion, tvDeliveryType;
//        ImageView ivSuperStore;
//        LinearLayout layoutMenuItems;
//
//        public RestaurantViewHolder(@NonNull View itemView) {
//            super(itemView);
//
//            tvRestaurantName = itemView.findViewById(R.id.restaurantName);
//            tvRating = itemView.findViewById(R.id.ratingValue);
//            tvDeliveryFee = itemView.findViewById(R.id.deliveryFeeText);
//            layoutMenuItems = itemView.findViewById(R.id.layoutMenuItems);
//        }
//    }
//}