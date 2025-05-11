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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    
    private List<UserPhoto> photoList;
    private OnPhotoClickListener listener;
    private ItemTouchHelper touchHelper;
    private String currentUsername; // Username of the current user
    private FirebaseAuth auth;
    
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
        FirebaseUser currentUser = auth.getCurrentUser();
        boolean isAuthor = currentUser != null && photo.isAuthor(currentUser.getUid());
        
        // Show/hide edit and delete buttons based on author status
        holder.deletePhotoButton.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        holder.editDescriptionButton.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        
        // Reset ImageView
        holder.photoImageView.setImageResource(0);
        
        // Load image from Firebase Storage URL if available
        if (photo.getStorageUrl() != null && !photo.getStorageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(photo.getStorageUrl())
                .centerCrop()
                .placeholder(R.drawable.my_img1)
                .error(R.drawable.my_img1)
                .into(holder.photoImageView);
        }
        // Fallback to file path if no storage URL
        else if (photo.hasFilePath()) {
            String pathOrUri = photo.getFilePath();
            try {
                Uri imageUri;
                if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                    imageUri = Uri.parse(pathOrUri);
                } else {
                    File imageFile = new File(pathOrUri);
                    if (!imageFile.exists()) {
                        holder.photoImageView.setImageResource(R.drawable.my_img1);
                        Log.w("PhotoAdapter", "File not found: " + pathOrUri);
                        return;
                    }
                    imageUri = Uri.fromFile(imageFile);
                }
                Glide.with(holder.itemView.getContext())
                    .load(imageUri)
                    .centerCrop()
                    .placeholder(R.drawable.my_img1)
                    .error(R.drawable.my_img1)
                    .into(holder.photoImageView);
            } catch (Exception e) {
                Log.e("PhotoAdapter", "Error loading image: " + e.getMessage());
                holder.photoImageView.setImageResource(R.drawable.my_img1);
            }
        }
        // Fallback to resource ID
        else if (photo.getImageResourceId() != 0) {
            holder.photoImageView.setImageResource(photo.getImageResourceId());
        }
        // Final fallback
        else {
            holder.photoImageView.setImageResource(R.drawable.my_img1);
        }
        
        // Set description
        if (photo.getDescription() != null && !photo.getDescription().isEmpty()) {
            holder.photoDescriptionTextView.setVisibility(View.VISIBLE);
            holder.photoDescriptionTextView.setText(photo.getDescription());
        } else {
            holder.photoDescriptionTextView.setText("No description");
            holder.photoDescriptionTextView.setVisibility(View.VISIBLE);
        }
        
        // Set like count and state
        holder.likeCountTextView.setText(String.valueOf(photo.getLikeCount()));
        if (photo.isLikedByUser(currentUsername)) {
            holder.likeImageView.setImageResource(android.R.drawable.btn_star_big_on);
            holder.likeImageView.setColorFilter(Color.RED);
        } else {
            holder.likeImageView.setImageResource(android.R.drawable.btn_star_big_off);
            holder.likeImageView.clearColorFilter();
        }
        
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
        holder.dragHandleButton.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        holder.dragHandleButton.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isAuthor) {
                if (touchHelper != null) {
                    touchHelper.startDrag(holder);
                }
            }
            return false;
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