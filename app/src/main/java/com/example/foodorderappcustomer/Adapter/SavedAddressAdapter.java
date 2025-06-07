package com.example.foodorderappcustomer.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.SavedAddress;
import com.example.foodorderappcustomer.R;

import java.util.ArrayList;
import java.util.List;

public class SavedAddressAdapter extends RecyclerView.Adapter<SavedAddressAdapter.ViewHolder> {
    private List<SavedAddress> addresses;
    private OnAddressClickListener listener;

    public interface OnAddressClickListener {
        void onAddressClick(SavedAddress address);
    }

    public SavedAddressAdapter(List<SavedAddress> addresses, OnAddressClickListener listener) {
        this.addresses = addresses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedAddress address = addresses.get(position);
        Log.d("SavedAddressAdapter", "Binding address: " + address.getLabel());
        holder.bind(address);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddressClick(address);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public void updateAddresses(List<SavedAddress> newAddresses) {
        this.addresses = new ArrayList<>();
        this.addresses.addAll(newAddresses);
        Log.d("SavedAddressAdapter", "Updated addresses: " + addresses);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView labelTextView;
        TextView addressTextView;

        ViewHolder(View itemView) {
            super(itemView);
            labelTextView = itemView.findViewById(R.id.addressLabel);
            addressTextView = itemView.findViewById(R.id.addressText);
        }

        public void bind(SavedAddress address) {
            labelTextView.setText(address.getLabel());
            addressTextView.setText(address.getAddress());
        }
    }
}