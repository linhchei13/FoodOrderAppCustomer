package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.Option;
import com.example.foodorderappcustomer.Models.OptionGroup;
import com.example.foodorderappcustomer.R;

import java.util.List;

public class OptionGroupAdapter extends RecyclerView.Adapter<OptionGroupAdapter.OptionGroupViewHolder> {
    private List<OptionGroup> optionGroups;
    private Context context;
    private OptionAdapter.OnToppingSelectedListener listener;

    public OptionGroupAdapter(Context context, List<OptionGroup> optionGroups, OptionAdapter.OnToppingSelectedListener listener) {
        this.context = context;
        this.optionGroups = optionGroups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OptionGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.dish_options_group, parent, false);
        return new OptionGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionGroupViewHolder holder, int position) {
        OptionGroup optionGroup = optionGroups.get(position);
        holder.bind(optionGroup);
    }

    @Override
    public int getItemCount() {
        return optionGroups.size();
    }

    class OptionGroupViewHolder extends RecyclerView.ViewHolder {
        private TextView optionGroupTitle;
        private RecyclerView optionsRecyclerView;

        public OptionGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            optionGroupTitle = itemView.findViewById(R.id.optionGroupTitle);
            optionsRecyclerView = itemView.findViewById(R.id.optionsRecyclerView);
        }

        void bind(OptionGroup optionGroup) {
            // Set the option group title (e.g., "Ice Options", "Size Options", etc.)
            optionGroupTitle.setText(optionGroup.getName() + (optionGroup.getMaxSelections() > 0 ? 
                    " (Tối đa " + optionGroup.getMaxSelections() + ")" : ""));

            // Set up RecyclerView for options
            optionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            OptionAdapter optionAdapter = new OptionAdapter(optionGroup.getOptions(), (option, isSelected) -> {
                // Handle mutual exclusivity if required
                if (isSelected && optionGroup.getMaxSelections() == 1) {
                    for (Option otherOption : optionGroup.getOptions()) {
                        if (otherOption != option && otherOption.isSelected()) {
                            otherOption.setSelected(false);
                        }
                    }
                }
                // Enforce max selections
                if (isSelected && optionGroup.getMaxSelections() > 1) {
                    int selectedCount = 0;
                    for (Option op : optionGroup.getOptions()) {
                        if (op.isSelected()) selectedCount++;
                    }
                    if (selectedCount > optionGroup.getMaxSelections()) {
                        option.setSelected(false);
                        return;
                    }
                }
                
                // Notify the parent listener
                if (listener != null) {
                    listener.onToppingSelected(option, isSelected);
                }
                
                // Update the adapter to reflect changes
                optionsRecyclerView.getAdapter().notifyDataSetChanged();
            });
            optionsRecyclerView.setAdapter(optionAdapter);
        }
    }
}
