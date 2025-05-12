package com.example.foodorderappcustomer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderappcustomer.Models.Message;
import com.example.foodorderappcustomer.R;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages;
    private String currentUserId;
    private SimpleDateFormat timeFormat;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageContent;
        private TextView messageTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.messageContent);
            messageTime = itemView.findViewById(R.id.messageTime);
        }

        public void bind(Message message) {
            messageContent.setText(message.getContent());
            messageTime.setText(timeFormat.format(message.getTimestamp()));

            // Align message based on sender
            if (message.getSenderId().equals(currentUserId)) {
                messageContent.setBackgroundResource(R.drawable.bg_message_sent);
                messageContent.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                ((ViewGroup.MarginLayoutParams) messageContent.getLayoutParams()).leftMargin = 100;
                ((ViewGroup.MarginLayoutParams) messageContent.getLayoutParams()).rightMargin = 0;
            } else {
                messageContent.setBackgroundResource(R.drawable.bg_message);
                messageContent.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                ((ViewGroup.MarginLayoutParams) messageContent.getLayoutParams()).leftMargin = 0;
                ((ViewGroup.MarginLayoutParams) messageContent.getLayoutParams()).rightMargin = 100;
            }
        }
    }
} 