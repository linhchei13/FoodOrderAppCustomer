package com.example.foodorderappcustomer.Models;

import java.util.Date;

public class Message {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private Date timestamp;
    private boolean isRead;
    private String senderType; // "customer" or "restaurant"

    public Message() {
        // Required empty constructor for Firebase
    }

    public Message(String id, String senderId, String receiverId, String content, String senderType) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = new Date();
        this.isRead = false;
        this.senderType = senderType;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }
} 