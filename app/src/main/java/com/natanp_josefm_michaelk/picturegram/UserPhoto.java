package com.natanp_josefm_michaelk.picturegram;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserPhoto implements Serializable {
    private int imageResourceId;
    private String description;
    private Date timestamp;
    private int likeCount;
    private List<String> likedByUsers; // List of usernames who liked this photo
    private String filePath; // Path to saved image file (for camera or gallery photos)
    private String storageUrl; // Firebase Storage URL
    private String authorId;   // Firebase user ID of the author
    private String authorName; // Display name of the author
    private String firestoreId; // Firestore document ID for this photo

    public UserPhoto(int imageResourceId) {
        this.imageResourceId = imageResourceId;
        this.description = "";
        this.timestamp = new Date();
        this.likeCount = 0;
        this.likedByUsers = new ArrayList<>();
    }

    public UserPhoto(int imageResourceId, String description) {
        this.imageResourceId = imageResourceId;
        this.description = description;
        this.timestamp = new Date();
        this.likeCount = 0;
        this.likedByUsers = new ArrayList<>();
    }
    
    public UserPhoto(String filePath, String description, String authorId, String authorName) {
        this.imageResourceId = 0;
        this.filePath = filePath;
        this.description = description;
        this.timestamp = new Date();
        this.likeCount = 0;
        this.likedByUsers = new ArrayList<>();
        this.authorId = authorId;
        this.authorName = authorName;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public boolean hasFilePath() {
        return filePath != null && !filePath.isEmpty();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
    
    public boolean isLikedByUser(String username) {
        return likedByUsers.contains(username);
    }
    
    /**
     * Toggles the like status for a user.
     * If the user has already liked the photo, their like is removed.
     * If the user hasn't liked the photo yet, their like is added.
     * 
     * @param username The username of the user toggling the like
     * @return true if the like state changed (added or removed), false otherwise
     */
    public boolean toggleLike(String username) {
        if (isLikedByUser(username)) {
            // User already liked this photo - remove the like
            likedByUsers.remove(username);
            likeCount--;
            return true;
        } else {
            // User hasn't liked this photo yet - add the like
            likedByUsers.add(username);
            likeCount++;
            return true;
        }
    }
    
    public List<String> getLikedByUsers() {
        return likedByUsers;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean isAuthor(String userId) {
        return authorId != null && authorId.equals(userId);
    }

    public String getFirestoreId() {
        return firestoreId;
    }

    public void setFirestoreId(String firestoreId) {
        this.firestoreId = firestoreId;
    }

    public boolean hasFirestoreId() {
        return firestoreId != null && !firestoreId.isEmpty();
    }
} 