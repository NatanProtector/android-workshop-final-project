package com.natanp_josefm_michaelk.picturegram;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    
    private static final String TAG = "PhotoAdapter";
    
    private List<UserPhoto> photoList;
    private OnPhotoClickListener listener;
    private ItemTouchHelper touchHelper;
    private String currentUsername; // Username of the current user
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    
    public interface OnPhotoClickListener {
        void onPhotoClick(UserPhoto photo, int position);
        void onLikeClick(UserPhoto photo, int position);
        void onDeleteClick(UserPhoto photo, int position);
        void onEditDescriptionClick(UserPhoto photo, int position);
    }
    
    public PhotoAdapter(List<UserPhoto> photoList, OnPhotoClickListener listener, String username) {
        this.photoList = photoList;
        this.listener = listener;
        this.currentUsername = username;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }
    
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        UserPhoto photo = photoList.get(position);
        
        // Load image
        if (photo.hasFilePath()) {
            try {
                // For new photos with file path (from gallery or camera)
                if (photo.getStorageUrl() != null && !photo.getStorageUrl().isEmpty()) {
                    // Use Firebase storage URL
                    Glide.with(holder.photoImageView.getContext())
                        .load(photo.getStorageUrl())
                        .centerCrop()
                        .into(holder.photoImageView);
                } else {
                    // Use local file path
                    Uri imageUri = Uri.parse(photo.getFilePath());
                    Glide.with(holder.photoImageView.getContext())
                        .load(imageUri)
                        .centerCrop()
                        .into(holder.photoImageView);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image", e);
                // Fallback to placeholder
                holder.photoImageView.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            // For resources (e.g. built-in drawables)
            holder.photoImageView.setImageResource(photo.getImageResourceId());
        }
        
        // Set other data
        holder.photoDescriptionTextView.setText(photo.getDescription());
        
        // Set up like count and icon
        updateLikeUI(holder, photo);
        
        // If this photo has a Firestore ID, check for current likes in Firestore
        if (photo.hasFirestoreId()) {
            loadLikesFromFirestore(holder, photo);
        }
        
        // Check if current user is the author - make it final for lambda capture
        final boolean isAuthor;
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            isAuthor = photo.isAuthor(currentUser.getUid());
        } else {
            isAuthor = false;
        }
        
        // Set delete and edit buttons visibility based on authorship
        holder.deletePhotoButton.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        holder.editDescriptionButton.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        holder.dragHandleButton.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        
        // Set click listeners
        holder.photoImageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(photo, holder.getAdapterPosition());
            }
        });
        
        holder.likeImageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(photo, holder.getAdapterPosition());
            }
        });
        
        holder.deletePhotoButton.setOnClickListener(v -> {
            if (listener != null && isAuthor) {
                listener.onDeleteClick(photo, holder.getAdapterPosition());
            }
        });
        
        holder.editDescriptionButton.setOnClickListener(v -> {
            if (listener != null && isAuthor) {
                listener.onEditDescriptionClick(photo, holder.getAdapterPosition());
            }
        });
        
        // Set drag handle touch listener (only for author)
        holder.dragHandleButton.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isAuthor) {
                if (touchHelper != null) {
                    touchHelper.startDrag(holder);
                }
            }
            return false;
        });
    }
    
    private void updateLikeUI(PhotoViewHolder holder, UserPhoto photo) {
        holder.likeCountTextView.setText(String.valueOf(photo.getLikeCount()));
        if (photo.isLikedByUser(currentUsername)) {
            holder.likeImageView.setImageResource(android.R.drawable.btn_star_big_on);
            holder.likeImageView.setColorFilter(Color.RED);
        } else {
            holder.likeImageView.setImageResource(android.R.drawable.btn_star_big_off);
            holder.likeImageView.clearColorFilter();
        }
    }
    
    private void loadLikesFromFirestore(PhotoViewHolder holder, UserPhoto photo) {
        // Load fresh like data from Firestore
        firestore.collection("photos").document(photo.getFirestoreId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Update like count
                    Long likeCount = documentSnapshot.getLong("likeCount");
                    if (likeCount != null) {
                        photo.setLikeCount(likeCount.intValue());
                    }
                    
                    // Update likedBy list
                    List<String> likedBy = (List<String>) documentSnapshot.get("likedBy");
                    if (likedBy != null) {
                        photo.getLikedByUsers().clear();
                        photo.getLikedByUsers().addAll(likedBy);
                    }
                    
                    // Update UI
                    updateLikeUI(holder, photo);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading likes from Firestore for photo: " + photo.getFirestoreId(), e);
            });
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }
    
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;
        TextView photoDescriptionTextView;
        ImageView likeImageView;
        TextView likeCountTextView;
        ImageButton deletePhotoButton;
        ImageButton editDescriptionButton;
        ImageButton dragHandleButton;
        
        PhotoViewHolder(View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.photoImageView);
            photoDescriptionTextView = itemView.findViewById(R.id.photoDescriptionTextView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            likeCountTextView = itemView.findViewById(R.id.likeCountTextView);
            deletePhotoButton = itemView.findViewById(R.id.deletePhotoButton);
            editDescriptionButton = itemView.findViewById(R.id.editDescriptionButton);
            dragHandleButton = itemView.findViewById(R.id.dragHandleButton);
        }
    }
} 