package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.Promotion;
import com.example.foodorderappcustomer.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromotionAdapter2 extends RecyclerView.Adapter<PromotionAdapter2.PromotionViewHolder> {

    private List<Promotion> promotionList;
    private List<Promotion> filteredList;
    private Context context;
    private OnPromotionSelectedListener listener;
    private double orderTotal;
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnPromotionSelectedListener {
        void onPromotionSelected(Promotion promotion);
    }

    public PromotionAdapter2(Context context, List<Promotion> promotionList, double orderTotal, OnPromotionSelectedListener listener) {
        this.context = context;
        this.promotionList = promotionList;
        this.filteredList = new ArrayList<>(promotionList);
        this.listener = listener;
        this.orderTotal = orderTotal;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion2, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promotion = filteredList.get(position);
        holder.bind(promotion);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(promotionList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Promotion promotion : promotionList) {
                if (promotion.getPromoCode().toLowerCase().contains(lowerCaseQuery) ||
                        promotion.getDescription().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(promotion);
                }
            }
        }
        notifyDataSetChanged();
    }

    class PromotionViewHolder extends RecyclerView.ViewHolder {
        private TextView promoCodeTextView;
        private TextView discountDescriptionTextView;
        private TextView validityTextView;
        private Button useButton;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            promoCodeTextView = itemView.findViewById(R.id.promoCodeTextView);
            discountDescriptionTextView = itemView.findViewById(R.id.discountDescriptionTextView);
            validityTextView = itemView.findViewById(R.id.validityTextView);
            useButton = itemView.findViewById(R.id.useButton);
        }

        public void bind(Promotion promotion) {
            promoCodeTextView.setText(promotion.getPromoCode());
            discountDescriptionTextView.setText(promotion.getDescription());

            // Format and display validity date
            try {
                Date endDate = apiDateFormat.parse(promotion.getEndDate());
                String formattedEndDate = displayDateFormat.format(endDate);
                validityTextView.setText("Hết hạn: " + formattedEndDate);
            } catch (ParseException e) {
                validityTextView.setText("Hết hạn: " + promotion.getEndDate());
            }

            // Check if promotion is applicable
            boolean isApplicable = isPromotionApplicable(promotion);
            useButton.setEnabled(isApplicable);
            useButton.setAlpha(isApplicable ? 1.0f : 0.5f);

            useButton.setOnClickListener(v -> {
                if (isApplicable && listener != null) {
                    listener.onPromotionSelected(promotion);
                }
            });
        }

        private boolean isPromotionApplicable(Promotion promotion) {
            // Check if promotion is expired
            if (promotion.isExpired()) {
                return false;
            }

            // Check minimum order amount
            try {
                double minimumOrder = Double.parseDouble(promotion.getMinimumOrder());
                if (orderTotal < minimumOrder) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // If parsing fails, assume no minimum
            }

            // Check validity dates
            try {
                Date now = new Date();
                Date startDate = apiDateFormat.parse(promotion.getStartDate());
                Date endDate = apiDateFormat.parse(promotion.getEndDate());

                if (now.before(startDate) || now.after(endDate)) {
                    return false;
                }
            } catch (ParseException e) {
                // If parsing fails, assume valid dates
            }

            return true;
        }
    }
}
