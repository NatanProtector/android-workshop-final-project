package com.natanp_josefm_michaelk.picturegram;

import java.util.Date;

public class UserPhoto {
    private int imageResourceId;
    private String description;
    private Date timestamp;
    private int likeCount;

    public UserPhoto(int imageResourceId) {
        this.imageResourceId = imageResourceId;
        this.description = "";
        this.timestamp = new Date();
        this.likeCount = 0;
    }

    public UserPhoto(int imageResourceId, String description) {
        this.imageResourceId = imageResourceId;
        this.description = description;
        this.timestamp = new Date();
        this.likeCount = 0;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
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
} 