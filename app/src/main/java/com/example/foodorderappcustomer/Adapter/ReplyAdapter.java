package com.example.foodorderappcustomer.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.Models.Reply;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
    private final List<Reply> replyList;
    private final Context context;
    private final Map<String, String> userNames;    // userId -> tên người dùng
    private final Map<String, String> avatarUrls;   // userId -> avatar URL
    private final String restaurantId;
    private final String restaurantName;
    private final String restaurantAvatarUrl;       // avatar nhà hàng

    public ReplyAdapter(Context context, List<Reply> replyList,
                        String restaurantId, String restaurantName, String restaurantAvatarUrl,
                        Map<String, String> userNames, Map<String, String> avatarUrls) {
        this.context = context;
        this.replyList = replyList;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantAvatarUrl = restaurantAvatarUrl;
        this.userNames = userNames;
        this.avatarUrls = avatarUrls;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Reply reply = replyList.get(position);

        String senderId = reply.getSenderId();

        // Tên người gửi
        if (senderId.equals(restaurantId)) {
            holder.textUserName.setText(restaurantName);
        } else {
            holder.textUserName.setText(userNames.getOrDefault(senderId, "Người dùng"));
        }

        // Avatar người gửi
        if (senderId.equals(restaurantId)) {
            // Avatar nhà hàng
            if (restaurantAvatarUrl != null && !restaurantAvatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(restaurantAvatarUrl)
                        .placeholder(R.drawable.loading_img)
                        .error(R.drawable.logo2)
                        .into(holder.imageAvatar);
            }
//            else {
//                holder.imageAvatar.setImageResource(R.drawable.avatar_default);
//            }
        } else {
            // Avatar user
            String avatarUrl = avatarUrls.get(senderId);
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // Sử dụng thư viện Glide để tải và hiển thị ảnh
                Glide.with(context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.loading_img)
                        .error(R.drawable.logo2)
                        .into(holder.imageAvatar);
            }
//            else {
//                holder.imageAvatar.setImageResource(R.drawable.avatar_default);
//            }
        }

        // Nội dung và thời gian phản hồi
        if (reply.getContent() != null) {
            holder.textReplyContent.setText(reply.getContent());
        }

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.textReplyTimestamp.setText(sdf.format(new Date(reply.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageAvatar;
        TextView textUserName, textReplyContent, textReplyTimestamp;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            textUserName = itemView.findViewById(R.id.textUserName);
            textReplyContent = itemView.findViewById(R.id.textReplyContent);
            textReplyTimestamp = itemView.findViewById(R.id.textReplyTimestamp);
        }
    }
}