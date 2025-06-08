package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.Promotion;
import com.example.foodorderappcustomer.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {
    private List<Promotion> promotions;
    private Context context;
    private OnPromotionClickListener listener;

    public interface OnPromotionClickListener {
        void onPromotionClick(Promotion promotion);
    }

    public PromotionAdapter(List<Promotion> promotions, OnPromotionClickListener listener) {
        this.promotions = promotions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promotion = promotions.get(position);
        holder.bind(promotion);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPromotionClick(promotion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    public void updateData(List<Promotion> newPromotions) {
        this.promotions = newPromotions;
        notifyDataSetChanged();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        private ImageView promotionImage;
        private TextView promotionCode;
        private TextView promotionDescription;
        private TextView promotionValidity;
        private SimpleDateFormat dateFormat;
        private Context context;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            promotionImage = itemView.findViewById(R.id.promotionImage);
            promotionCode = itemView.findViewById(R.id.promotionCode);
            promotionDescription = itemView.findViewById(R.id.promotionDescription);
            promotionValidity = itemView.findViewById(R.id.promotionValidity);
            dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        }

        public void bind(Promotion promotion) {
            // Set promotion code
            promotionCode.setText(promotion.getPromoCode());
            
            // Set description
            promotionDescription.setText(promotion.getDescription());
            
            // Format and set validity date
            String validUntil = "Có hiệu lực đến " + (promotion.getEndDate());
            promotionValidity.setText(validUntil);
            Log.d("PromotionAdapter", "restaurantId: " + promotion.getRestaurantId());
            
            // Load restaurant image
            if (promotion.getRestaurantId() != null && !promotion.getRestaurantId().isEmpty()) {
                FirebaseDatabase.getInstance().getReference("restaurants")
                    .child(promotion.getRestaurantId())
                    .child("imageUrl")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (snapshot.exists()) {
                                String imageUrl = snapshot.getValue(String.class);
                                Log.d("PromotionAdapter", "imageUrl: " + imageUrl);
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    Glide.with(context)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.loading_img)
                                        .error(R.drawable.logo2)
                                        .into(promotionImage);
                                } else {
                                    promotionImage.setImageResource(R.drawable.logo2);
                                    Log.d("PromotionAdapter", "Error loading restaurant image: ");
                                }
                            } else {
                                promotionImage.setImageResource(R.drawable.logo2);
                                Log.d("PromotionAdapter", "Error loading restaurant image: ");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle error
                            promotionImage.setImageResource(R.drawable.logo2);
                            Log.e("PromotionAdapter", "Error loading restaurant image: " + error.getMessage());
                        }
                    });
            } else {
                promotionImage.setImageResource(R.drawable.logo2);
                Log.d("PromotionAdapter", "Error loading restaurant image: ");
            }
        }
        
        private String formatDate(Date date) {
            if (date == null) return "";
            return dateFormat.format(date);
        }
    }
} 