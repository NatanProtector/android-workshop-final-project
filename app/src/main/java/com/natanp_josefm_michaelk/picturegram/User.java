package com.natanp_josefm_michaelk.picturegram;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String name;
    private int imageResourceId; // Profile image
    private List<UserPhoto> userPhotos; // List of user's gallery photos
    private List<String> following; // List of users this user follows
    private List<String> followers; // List of users following this user

    // Empty constructor for Firestore
    public User() {
        this.userPhotos = new ArrayList<>();
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
    }

    public User(String userId, String name, int imageResourceId) {
        this.userId = userId;
        this.name = name;
        this.imageResourceId = imageResourceId;
        this.userPhotos = new ArrayList<>();
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getImageResourceId() { return imageResourceId; }
    public void setImageResourceId(int imageResourceId) { this.imageResourceId = imageResourceId; }
    
    // Methods for managing the user's photo gallery
    public List<UserPhoto> getUserPhotos() {
        return userPhotos;
    }
    
    public void setUserPhotos(List<UserPhoto> userPhotos) {
        this.userPhotos = userPhotos != null ? userPhotos : new ArrayList<>();
    }
    
    public void addPhoto(UserPhoto photo) {
        userPhotos.add(photo);
    }
    
    public void removePhoto(UserPhoto photo) {
        userPhotos.remove(photo);
    }
    
    public int getPhotoCount() {
        return userPhotos.size();
    }
    
    // Methods for managing followers and following
    public List<String> getFollowing() {
        return following;
    }
    
    public void setFollowing(List<String> following) {
        this.following = following != null ? following : new ArrayList<>();
    }
    
    public List<String> getFollowers() {
        return followers;
    }
    
    public void setFollowers(List<String> followers) {
        this.followers = followers != null ? followers : new ArrayList<>();
    }
    
    public boolean isFollowing(String userId) {
        return following != null && following.contains(userId);
    }
    
    public boolean isFollowedBy(String userId) {
        return followers != null && followers.contains(userId);
    }
    
    public int getFollowingCount() {
        return following != null ? following.size() : 0;
    }
    
    public int getFollowersCount() {
        return followers != null ? followers.size() : 0;
    }
}
