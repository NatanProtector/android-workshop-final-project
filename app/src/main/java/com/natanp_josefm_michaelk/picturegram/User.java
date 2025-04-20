package com.natanp_josefm_michaelk.picturegram;

public class User {
    private String name;
    private int imageResourceId; // or a URL if you load images from the web

    public User(String name, int imageResourceId) {
        this.name = name;
        this.imageResourceId = imageResourceId;
    }

    public String getName() { return name; }
    public int getImageResourceId() { return imageResourceId; }
}
