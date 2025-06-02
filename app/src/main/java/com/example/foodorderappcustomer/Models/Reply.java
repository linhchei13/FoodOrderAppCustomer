package com.example.foodorderappcustomer.Models;

public class Reply {
    private String senderId;   // Ai gửi (userId hoặc restaurantId)
    private String content;    // Nội dung phản hồi
    private long timestamp;    // Thời gian phản hồi (long)

    public Reply() {
        // Required for Firebase
    }

    public Reply(String senderId, String content, long timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}