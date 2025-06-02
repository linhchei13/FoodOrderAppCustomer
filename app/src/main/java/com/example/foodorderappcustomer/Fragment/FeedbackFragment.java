package com.example.foodorderappcustomer.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.Adapter.ReviewAdapter;
import com.example.foodorderappcustomer.Models.Reply;
import com.example.foodorderappcustomer.Models.Review;
import com.example.foodorderappcustomer.Models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeedbackFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();
    private Map<String, User> userMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewFeedback);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        String restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        adapter = new ReviewAdapter(getContext(), reviewList, userMap, restaurantId, (position, review) -> {
            showReplyDialog(review);
        });

        recyclerView.setAdapter(adapter);

        loadReviews();

        return view;
    }

    private void loadReviews() {
        String restaurantId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("FeedbackFragment", "Loading reviews for restaurant: " + restaurantId);

        DatabaseReference reviewRef = FirebaseDatabase.getInstance().getReference("reviews");

        // Show loading state
        if (getView() != null) {
            getView().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }

        reviewRef.orderByChild("userId").equalTo(restaurantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        reviewList.clear();
                        Log.d("FeedbackFragment", "Found " + snapshot.getChildrenCount() + " reviews in database");
                        
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String orderId = snap.getKey();
                            Review review = snap.getValue(Review.class);
                            if (review != null) {
                                review.setOrderId(orderId); // Set orderId as the identifier
                                reviewList.add(review);
                                Log.d("FeedbackFragment", "Added review for order: " + orderId);
                            }
                        }

                        // Update UI
//                        if (getView() != null) {
//                            getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
//                        }
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }

                        loadUsers();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FeedbackFragment", "Error loading reviews: " + error.getMessage());
                        if (getView() != null) {
                            getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void loadUsers() {
        userMap.clear();

        // Tạo danh sách userId duy nhất từ reviewList
        Set<String> restaurantIDs = new HashSet<>();
        for (Review review : reviewList) {
            if (review.getRestaurantId() != null) {
                restaurantIDs.add(review.getRestaurantId());
            }
        }

        if (restaurantIDs.isEmpty()) {
            // Không có userId nào => thông báo cập nhật adapter luôn
            adapter.notifyDataSetChanged();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");

        // Đếm số lượng user cần load để biết khi nào xong
        final int totalUsers = restaurantIDs.size();
        final int[] loadedCount = {0};


        for (String userId : restaurantIDs) {
            userRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = null;
                    try {
                        user = snapshot.getValue(User.class);
                    } catch (Exception e) {
                        Log.e("FirebaseParse", "Lỗi khi parse user: " + snapshot.getKey(), e);
                    }
                    if (user != null) {
                        userMap.put(userId, user);
                    }
                    loadedCount[0]++;
                    // Khi đã load hết user thì thông báo adapter
                    if (loadedCount[0] == totalUsers) {
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FeedbackFragment", "Load user lỗi: " + error.getMessage());
                    loadedCount[0]++;
                    if (loadedCount[0] == totalUsers) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private void showReplyDialog(Review review) {
        if (review.getOrderId() == null) {
            Toast.makeText(getContext(), "Không thể thêm phản hồi: ID đơn hàng không hợp lệ", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Trả lời đánh giá");

        final EditText input = new EditText(getContext());
        input.setHint("Nhập trả lời của bạn...");
        builder.setView(input);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String replyContent = input.getText().toString().trim();
            if (replyContent.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập nội dung phản hồi", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Reply reply = new Reply(senderId, replyContent, System.currentTimeMillis());

            DatabaseReference replyRef = FirebaseDatabase.getInstance()
                    .getReference("reviews")
                    .child(review.getOrderId())  // Use orderId
                    .child("replies");

            String newReplyKey = replyRef.push().getKey();
            if (newReplyKey != null) {
                replyRef.child(newReplyKey).setValue(reply)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Phản hồi đã được gửi thành công", 
                                Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Gửi phản hồi thất bại: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}