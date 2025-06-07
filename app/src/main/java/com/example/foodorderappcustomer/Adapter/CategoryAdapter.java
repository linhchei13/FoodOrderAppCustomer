package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.CategoryMenuActivity;
import com.example.foodorderappcustomer.Models.Category;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categories;
    private Context context;

    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.categoryName.setText(category.getName());
        Picasso.get()
                .load(category.getImageUrl())
                .into(holder.categoryImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d("CategoryAdapter", "Image loaded successfully from HTTP URL");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("CategoryAdapter", "Error loading image from HTTP URL: " + e.getMessage());
                    }
                });

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            // Launch CategoryMenuActivity when a category is clicked
            Intent intent = new Intent(context, CategoryMenuActivity.class);
            intent.putExtra("CATEGORY_NAME", category.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateData(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
            categoryName = itemView.findViewById(R.id.categoryName);
        }
    }
}