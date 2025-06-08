package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.Models.Reply;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.Models.User;
import com.example.foodorderappcustomer.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;
    private Map<String, User> userMap; // userId -> User (fullName + profileImageUrl)
    private OnReplyClickListener replyClickListener;
    private String restaurantId;
    private String restaurantName;


    public interface OnReplyClickListener {
        void onReplyClick(int position, Review review);
    }

    public ReviewAdapter(Context context, List<Review> reviewList, Map<String, User> userMap,
                         String restaurantId,
                         OnReplyClickListener replyClickListener) {
        this.context = context;
        this.reviewList = reviewList;
        this.userMap = userMap;
        this.restaurantId = restaurantId;
        this.replyClickListener = replyClickListener;
    }

    private void loadOrderItem(String orderId, OrderItemCallback callback) {
        final List<String> itemNames = new ArrayList<>();

        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference().child("orders");
        orderRef.child(orderId).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemNames.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String itemName = snap.child("itemName").getValue(String.class);
                    if (itemName != null) {
                        itemNames.add(itemName);
                    }
                }
                callback.onItemNamesLoaded(String.join(", ", itemNames));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ReviewAdapter", "Failed to load order items: " + error.getMessage());
                callback.onItemNamesLoaded("Không có món ăn"); // Provide a fallback
            }
        });
    }

    // Interface callback để nhận dữ liệu món ăn
    private interface OrderItemCallback {
        void onItemNamesLoaded(String itemNames);
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Set rating
        holder.ratingBar.setRating(review.getRating());

        // Set comment
        holder.textComment.setText(review.getComment());
        
        // Load and set order item names asynchronously
        loadOrderItem(review.getOrderId(), itemNames -> {
            holder.orderItem.setText("Đã đặt: " + itemNames);
        });

        // Set timestamp
        Date timestamp = review.getTimestamp();
        if (timestamp != null) {
            String formattedTime = DateFormat.format("dd/MM/yyyy HH:mm", timestamp).toString();
            holder.textTimestamp.setText(formattedTime);
        } else {
            holder.textTimestamp.setText("");
        }

        // Set user info
        User user = userMap.get(review.getUserId());
        if (user != null) {
            holder.textUserName.setText(user.getFullName());
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.loading_img)
                    .error(R.drawable.logo2)
                    .into(holder.imageAvatar);
        }   else {
            holder.textUserName.setText("any");
            holder.imageAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // Set image review (hiển thị ảnh đầu tiên nếu có)
        if (review.getImageUrls() != null && !review.getImageUrls().isEmpty()) {
            holder.recyclerViewImages.setVisibility(View.VISIBLE);
            holder.recyclerViewImages.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            ReviewImageAdapter2 imageAdapter = new ReviewImageAdapter2(context, review.getImageUrls());
            holder.recyclerViewImages.setAdapter(imageAdapter);
        } else {
            holder.recyclerViewImages.setVisibility(View.GONE);
        }


        holder.btnReply.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onReplyClick(position, review);
            }
        });

        setupRepliesRecyclerView(holder.recyclerViewReplies, holder.btnReply, review);


    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }



    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imageAvatar;
        TextView textUserName, textComment, textTimestamp, orderItem;
        RatingBar ratingBar;
        RecyclerView recyclerViewImages;
        RecyclerView recyclerViewReplies;

        Button btnReply;


        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            recyclerViewImages = itemView.findViewById(R.id.recyclerViewImages);
            textUserName = itemView.findViewById(R.id.textUserName);
            textComment = itemView.findViewById(R.id.textComment);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            orderItem = itemView.findViewById(R.id.orderItem);

            recyclerViewReplies = itemView.findViewById(R.id.recyclerViewReplies);
            btnReply = itemView.findViewById(R.id.btnReply);
            recyclerViewImages.setLayoutManager(new LinearLayoutManager(itemView.getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
            recyclerViewReplies.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }


    private void loadRestaurantDataForReplies(List<Reply> replyList, ReplyAdapterReplyCallback callback) {
        DatabaseReference restaurantsRef = FirebaseDatabase.getInstance().getReference("restaurants");
        restaurantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> userNames = new HashMap<>();
                Map<String, String> avatarUrls = new HashMap<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String fullName = userSnapshot.child("name").getValue(String.class);
                    String avatar = userSnapshot.child("imageUrl").getValue(String.class);
                    if (userId != null) {
                        userNames.put(userId, fullName != null ? fullName : "Nha hang");
                        avatarUrls.put(userId, avatar != null ? avatar : "");
                    }
                }
                // Callback trả về map data
                callback.onUserDataLoaded(userNames, avatarUrls);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onUserDataLoaded(new HashMap<>(), new HashMap<>());
            }
        });
    }

    // Interface callback để nhận dữ liệu
    private interface ReplyAdapterReplyCallback {
        void onUserDataLoaded(Map<String, String> userNames, Map<String, String> avatarUrls);
    }

    private void loadUserData(String userID, RestaurantDataCallback callback) {
        DatabaseReference restaurantsRef = FirebaseDatabase.getInstance().getReference("users").child(userID);
        restaurantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String lastName = snapshot.child("lastName").getValue(String.class);
                    String name = firstName + " " + lastName;
                    String avatarUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    callback.onDataLoaded(name != null ? name : "Nhà hàng", avatarUrl != null ? avatarUrl : "");
                } else {
                    callback.onDataLoaded("Nhà hàng", ""); // default data if not found
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onDataLoaded("Nhà hàng", "");
            }
        });
    }

    private interface RestaurantDataCallback {
        void onDataLoaded(String restaurantName, String avatarUrl);
    }

    private void setupRepliesRecyclerView(RecyclerView recyclerViewReplies, Button btnReply, Review review) {
        if (review.getReplies() == null || review.getReplies().isEmpty()) {
            recyclerViewReplies.setVisibility(View.GONE);
            btnReply.setVisibility(View.GONE);
            return;
        }

        List<Reply> replyList = new ArrayList<>(review.getReplies().values());
        btnReply.setVisibility(View.VISIBLE);

        loadUserData(restaurantId, (restName, restAvatar) -> {
            loadRestaurantDataForReplies(replyList, (userNames, avatarUrls) -> {
                ReplyAdapter replyAdapter = new ReplyAdapter(
                        context,
                        replyList,
                        restaurantId,
                        restName,
                        restAvatar,
                        userNames,
                        avatarUrls
                );
                recyclerViewReplies.setAdapter(replyAdapter);
                recyclerViewReplies.setVisibility(View.VISIBLE);
            });
        });
    }
}

