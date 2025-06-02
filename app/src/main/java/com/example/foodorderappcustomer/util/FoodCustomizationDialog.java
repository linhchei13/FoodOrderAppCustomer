package com.example.foodorderappcustomer.util;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Adapter.OptionAdapter;
import com.example.foodorderappcustomer.Models.MenuItem;
import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.Models.OptionGroup;
import com.example.foodorderappcustomer.R;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodCustomizationDialog {    private Context context;
    private Dialog dialog;
    private MenuItem menuItem;
    private OnFoodCustomizedListener listener;
    private int quantity = 1;
    private List<Option> selectedOptions = new ArrayList<>();
    private String customerNote = "";
    private double totalPrice = 0;
    private NumberFormat currencyFormat;
    private List<OptionGroup> optionGroups = new ArrayList<>();    public interface OnFoodCustomizedListener {
        void onFoodCustomized(MenuItem menuItem, int quantity, List<Option> options, String note, double totalPrice);
    }    public FoodCustomizationDialog(Context context, MenuItem menuItem, OnFoodCustomizedListener listener) {
        this.context = context;
        this.menuItem = menuItem;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalPrice = menuItem.getPrice();
        
        // Create default option groups if none are provided in the menu item
        createDefaultOptionGroups();
        setupDialog();
    }
    
    private void createDefaultOptionGroups() {
        // Create Ice Option group
        OptionGroup iceOptionGroup = new OptionGroup("ice", "Đá (Topping, tối đa 1)", 1, false);
        iceOptionGroup.addOption(new Option("ice_50", "50% đá", 0));
        iceOptionGroup.addOption(new Option("ice_70", "70% đá", 0));
        iceOptionGroup.addOption(new Option("ice_100", "100% đá", 0));
        optionGroups.add(iceOptionGroup);
        
        // Create Size Option group
        OptionGroup sizeOptionGroup = new OptionGroup("size", "SIZE (Topping, tối đa 1)", 1, false);
        sizeOptionGroup.addOption(new Option("upsize", "Upsize", 6000));
        optionGroups.add(sizeOptionGroup);
        
        // Add any available options from the menu item
        if (menuItem.getAvailableOptions() != null && !menuItem.getAvailableOptions().isEmpty()) {
            OptionGroup additionalOptionGroup = new OptionGroup("additional", "Thêm Topping", 5, false);
            for (Option option : menuItem.getAvailableOptions()) {
                additionalOptionGroup.addOption(option);
            }
            optionGroups.add(additionalOptionGroup);
        }
    }    private void setupDialog() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_food_customization);

        // Set dialog width to match parent
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Initialize views
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);
        ImageView foodImageView = dialog.findViewById(R.id.foodImageView);
        TextView foodNameTextView = dialog.findViewById(R.id.foodName);
        TextView foodDescriptionTextView = dialog.findViewById(R.id.foodDescriptionTextView);
        TextView foodStatsTextView = dialog.findViewById(R.id.foodStatsTextView);
        TextView priceTextView = dialog.findViewById(R.id.priceTextView);
        ImageButton decreaseButton = dialog.findViewById(R.id.decreaseButton);
        TextView quantityTextView = dialog.findViewById(R.id.quantityTextView);
        ImageButton increaseButton = dialog.findViewById(R.id.increaseButton);

        LinearLayout optionsContainer = dialog.findViewById(R.id.optionsContainer);
        EditText noteEditText = dialog.findViewById(R.id.noteEditText);
        Button addToCartButton = dialog.findViewById(R.id.addToCartButton);

        // Set initial values
        foodNameTextView.setText(menuItem.getName());
        foodDescriptionTextView.setText(menuItem.getDescription());
        foodStatsTextView.setText(menuItem.getStats());
        priceTextView.setText(formatPrice(menuItem.getPrice()));
        quantityTextView.setText(String.valueOf(quantity));
        updateAddToCartButton(addToCartButton);

        // Load image if available
        if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
            Picasso.get().load(menuItem.getImageUrl()).into(foodImageView);
        }

        // Set click listeners
        closeButton.setOnClickListener(v -> dialog.dismiss());

        decreaseButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                quantityTextView.setText(String.valueOf(quantity));
                updateAddToCartButton(addToCartButton);
            }
        });

        increaseButton.setOnClickListener(v -> {
            quantity++;
            quantityTextView.setText(String.valueOf(quantity));
            updateAddToCartButton(addToCartButton);
        });        // Set up option groups
        if (optionsContainer != null) {
            // Clear any existing views first
            optionsContainer.removeAllViews();
            
            // Create and add option groups dynamically
            for (OptionGroup group : optionGroups) {
                RecyclerView recyclerView = new RecyclerView(context);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                
                // Group title
                TextView titleView = new TextView(context);
                titleView.setText(group.getName());
                titleView.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray, null));
                titleView.setPadding(32, 16, 32, 16);
                optionsContainer.addView(titleView);
                
                // Options list
                OptionAdapter adapter = new OptionAdapter(group.getOptions(), (option, isSelected) -> {
                    // Handle option selection
                    if (isSelected) {
                        if (group.getMaxSelections() == 1) {
                            // Enforce single selection in the group
                            for (Option otherOption : group.getOptions()) {
                                if (otherOption != option && otherOption.isSelected()) {
                                    otherOption.setSelected(false);
                                }
                            }
                        }
                        selectedOptions.add(option);
                    } else {
                        selectedOptions.remove(option);
                    }
                    
                    // Update price
                    calculateTotalPrice();
                    updateAddToCartButton(addToCartButton);
                });
                
                recyclerView.setAdapter(adapter);
                optionsContainer.addView(recyclerView);
            }
        }        // Add to cart button
        addToCartButton.setOnClickListener(v -> {
            customerNote = noteEditText.getText().toString().trim();
            
            // Collect all selected options from all groups
            List<Option> finalSelectedOptions = new ArrayList<>();
            for (OptionGroup group : optionGroups) {
                finalSelectedOptions.addAll(group.getSelectedOptions());
            }
            
            listener.onFoodCustomized(menuItem, quantity, finalSelectedOptions, customerNote, totalPrice);
            dialog.dismiss();
            Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        });
    }    private void calculateTotalPrice() {
        totalPrice = menuItem.getPrice();
        
        // Add price for selected options from all option groups
        for (OptionGroup group : optionGroups) {
            for (Option option : group.getOptions()) {
                if (option.isSelected()) {
                    totalPrice += option.getPrice();
                }
            }
        }
        
        // Multiply by quantity
        totalPrice *= quantity;
    }

    private void updateAddToCartButton(Button button) {
        calculateTotalPrice();
        button.setText("Thêm vào giỏ hàng - " + formatPrice(totalPrice));
    }

    private String formatPrice(double price) {
        return currencyFormat.format(price).replace("₫", "đ");
    }

    public void show() {
        dialog.show();
    }
}
