package com.example.foodorderappcustomer.Adapter;

import android.content.Context;
import android.text.format.DateFormat;
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

import com.example.foodorderappcustomer.Models.Reply;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.Models.User;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;
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
            ImageUtils.loadImage(user.getProfileImageUrl(), holder.imageAvatar, R.drawable.logo2, R.drawable.logo2);
        } else {
            holder.textUserName.setText("Người dùng ẩn danh");
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

        setupRepliesRecyclerView(holder.recyclerViewReplies, review);


    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imageAvatar;
        TextView textUserName, textComment, textTimestamp;
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

            recyclerViewReplies = itemView.findViewById(R.id.recyclerViewReplies);
            btnReply = itemView.findViewById(R.id.btnReply);
            recyclerViewImages.setLayoutManager(new LinearLayoutManager(itemView.getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
            recyclerViewReplies.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }


    private void loadUserDataForReplies(List<Reply> replyList, ReplyAdapterReplyCallback callback) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> userNames = new HashMap<>();
                Map<String, String> avatarUrls = new HashMap<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String fullName = userSnapshot.child("fullName").getValue(String.class);
                    String avatar = userSnapshot.child("profileImageUrl").getValue(String.class);
                    if (userId != null) {
                        userNames.put(userId, fullName != null ? fullName : "Người dùng");
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

    private void loadRestaurantData(String restaurantId, RestaurantDataCallback callback) {
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance().getReference("restaurants").child(restaurantId);
        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String avatarUrl = snapshot.child("imageUrl").getValue(String.class); // giả sử trường avatarUrl
                    callback.onDataLoaded(name != null ? name : "Nhà hàng", avatarUrl != null ? avatarUrl : "");
                } else {
                    callback.onDataLoaded("Nhà hàng", ""); // dữ liệu mặc định nếu không có
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

    private void setupRepliesRecyclerView(RecyclerView recyclerViewReplies, Review review) {
        if (review.getReplies() == null || review.getReplies().isEmpty()) {
            recyclerViewReplies.setVisibility(View.GONE);
            return;
        }

        List<Reply> replyList = new ArrayList<>(review.getReplies().values());

        loadRestaurantData(restaurantId, (restName, restAvatar) -> {
            loadUserDataForReplies(replyList, (userNames, avatarUrls) -> {
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

