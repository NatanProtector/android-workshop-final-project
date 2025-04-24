package com.natanp_josefm_michaelk.picturegram;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private int imageResourceId; // Profile image
    private List<UserPhoto> userPhotos; // List of user's gallery photos

    public User(String name, int imageResourceId) {
        this.name = name;
        this.imageResourceId = imageResourceId;
        this.userPhotos = new ArrayList<>();
    }

    public String getName() { return name; }
    public int getImageResourceId() { return imageResourceId; }
    
    // Methods for managing the user's photo gallery
    public List<UserPhoto> getUserPhotos() {
        return userPhotos;
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
}
