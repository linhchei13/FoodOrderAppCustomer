// ToppingAdapter.java
package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.ToppingViewHolder> {

    private List<Option> options;
    private OnToppingSelectedListener listener;

    public interface OnToppingSelectedListener {
        void onToppingSelected(Option option, boolean isSelected);
    }

    public OptionAdapter(List<Option> options, OnToppingSelectedListener listener) {
        this.options = options;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ToppingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topping, parent, false);
        return new ToppingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ToppingViewHolder holder, int position) {
        Option option = options.get(position);
        holder.bind(option);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    class ToppingViewHolder extends RecyclerView.ViewHolder {
        private CheckBox toppingCheckBox;
        private TextView toppingPriceTextView;

        public ToppingViewHolder(@NonNull View itemView) {
            super(itemView);
            toppingCheckBox = itemView.findViewById(R.id.toppingCheckBox);
            toppingPriceTextView = itemView.findViewById(R.id.toppingPriceTextView);
        }

        void bind(final Option option) {
            toppingCheckBox.setText(option.getName());
            toppingCheckBox.setChecked(option.isSelected());

            // Format price with Vietnamese currency
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String formattedPrice = currencyFormatter.format(option.getPrice()).replace("₫", "đ");
            toppingPriceTextView.setText("+" + formattedPrice);

            toppingCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                option.setSelected(isChecked);
                if (listener != null) {
                    listener.onToppingSelected(option, isChecked);
                }
            });
        }
    }
}