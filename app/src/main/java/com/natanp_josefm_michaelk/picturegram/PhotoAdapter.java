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

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    
    private List<UserPhoto> photoList;
    private OnPhotoClickListener listener;
    private ItemTouchHelper touchHelper;
    private String currentUsername; // Username of the current user
    
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
        
        // *** Add this: Reset ImageView to a known state before loading ***
        holder.photoImageView.setImageResource(0); // Clear previous image
        // Or set a placeholder: 
        // holder.photoImageView.setImageResource(R.drawable.placeholder_image); 
        
        // Set image - check if it's a resource or a file/URI
        if (photo.hasFilePath()) {
            // Load from file path or URI string
            String pathOrUri = photo.getFilePath();
            try {
                Uri imageUri;
                // Check if it's a content URI or file URI before parsing
                if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                    imageUri = Uri.parse(pathOrUri);
                } else {
                    // Assume it's a plain file path
                    File imageFile = new File(pathOrUri);
                    if (!imageFile.exists()) {
                        // Fallback if file doesn't exist
                        holder.photoImageView.setImageResource(R.drawable.my_img1); // Or some placeholder
                        Log.w("PhotoAdapter", "File not found: " + pathOrUri + ". Loading fallback.");
                        // Skip the rest of the image loading for this item
                        // You might want to handle description/likes differently here too
                        // Set description (if available)
                        if (photo.getDescription() != null && !photo.getDescription().isEmpty()) {
                            holder.photoDescriptionTextView.setVisibility(View.VISIBLE);
                            holder.photoDescriptionTextView.setText(photo.getDescription());
                        } else {
                            holder.photoDescriptionTextView.setText("No description (File Error)");
                            holder.photoDescriptionTextView.setVisibility(View.VISIBLE);
                        }
                        // Reset like state appearance
                        holder.likeImageView.setImageResource(android.R.drawable.btn_star_big_off);
                        holder.likeImageView.clearColorFilter();
                        holder.likeCountTextView.setText("0");
                        // Clear listeners or disable buttons if needed
                        holder.photoImageView.setOnClickListener(null);
                        holder.likeImageView.setOnClickListener(null);
                        holder.deletePhotoButton.setOnClickListener(null);
                        holder.editDescriptionButton.setOnClickListener(null);
                        holder.dragHandleButton.setOnTouchListener(null);
                        return; // Stop processing this item further
                    }
                    imageUri = Uri.fromFile(imageFile);
                }
                holder.photoImageView.setImageURI(imageUri);
            } catch (Exception e) {
                Log.e("PhotoAdapter", "Error loading image URI: " + pathOrUri, e);
                // Fallback to a default image on any error
                holder.photoImageView.setImageResource(R.drawable.my_img1); // Or some placeholder
            }
        } else {
            // Load from resource
            // *** Optional: Check if resource ID is valid before setting ***
            if (photo.getImageResourceId() != 0) {
                holder.photoImageView.setImageResource(photo.getImageResourceId());
            } else {
                // Handle case where it's not a file path but resource ID is 0 (shouldn't happen ideally)
                Log.w("PhotoAdapter", "UserPhoto has no file path and resource ID is 0 at position: " + position);
                holder.photoImageView.setImageResource(R.drawable.my_img1); // Fallback
            }
        }
        
        // Set description (if available)
        if (photo.getDescription() != null && !photo.getDescription().isEmpty()) {
            holder.photoDescriptionTextView.setVisibility(View.VISIBLE);
            holder.photoDescriptionTextView.setText(photo.getDescription());
        } else {
            holder.photoDescriptionTextView.setText("No description");
            holder.photoDescriptionTextView.setVisibility(View.VISIBLE);
        }
        
        // Set like count
        holder.likeCountTextView.setText(String.valueOf(photo.getLikeCount()));
        
        // Set like icon appearance based on whether the current user has liked the photo
        if (photo.isLikedByUser(currentUsername)) {
            // User has liked this photo - show filled red heart
            holder.likeImageView.setImageResource(android.R.drawable.btn_star_big_on);
            holder.likeImageView.setColorFilter(Color.RED);
        } else {
            // User hasn't liked this photo - show empty heart
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
            if (listener != null) {
                listener.onDeleteClick(photo, holder.getAdapterPosition());
            }
        });
        
        holder.editDescriptionButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditDescriptionClick(photo, holder.getAdapterPosition());
            }
        });
        
        // Set drag handle touch listener
        holder.dragHandleButton.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
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