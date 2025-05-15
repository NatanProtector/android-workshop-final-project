package com.natanp_josefm_michaelk.picturegram;

/**
 * Model class for notification data stored in Firestore
 */
public class Notification {
    private String id; // Firestore document ID
    private String type; // "like" or "follow"
    private String fromUser; // sender's username
    private String toUser; // recipient's username
    private long timestamp; // timestamp in milliseconds
    private boolean isRead; // whether the notification has been read

    // Empty constructor required for Firestore
    public Notification() {
    }

    // Constructor for creating new notifications
    public Notification(String type, String fromUser, String toUser) {
        // Validate the type to ensure it's either "like" or "follow"
        if (!"like".equals(type) && !"follow".equals(type)) {
            throw new IllegalArgumentException("Type must be either 'like' or 'follow'");
        }
        this.type = type;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
} 