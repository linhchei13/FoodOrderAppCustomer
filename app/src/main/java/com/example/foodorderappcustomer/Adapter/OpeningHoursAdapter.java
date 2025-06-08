package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.R;

import java.util.List;

public class OpeningHoursAdapter extends RecyclerView.Adapter<OpeningHoursAdapter.OpeningHourViewHolder> {

    private List<OpeningHourItem> openingHoursList;

    public OpeningHoursAdapter(List<OpeningHourItem> openingHoursList) {
        this.openingHoursList = openingHoursList;
    }

    @NonNull
    @Override
    public OpeningHourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_opening_hour, parent, false);
        return new OpeningHourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpeningHourViewHolder holder, int position) {
        OpeningHourItem item = openingHoursList.get(position);
        holder.dayTextView.setText(item.getDay());
        holder.hoursTextView.setText(item.getHours());
        // You can set the icon if needed, for now it's static in the layout
    }

    @Override
    public int getItemCount() {
        return openingHoursList.size();
    }

    public static class OpeningHourViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView;
        TextView hoursTextView;
        ImageView iconImageView;

        public OpeningHourViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            dayTextView = itemView.findViewById(R.id.dayTextView);
            hoursTextView = itemView.findViewById(R.id.hoursTextView);
        }
    }

    // Helper class to represent a single opening hour item
    public static class OpeningHourItem {
        private String day;
        private String hours;

        public OpeningHourItem(String day, String hours) {
            this.day = day;
            this.hours = hours;
        }

        public String getDay() {
            return day;
        }

        public String getHours() {
            return hours;
        }
    }
} 