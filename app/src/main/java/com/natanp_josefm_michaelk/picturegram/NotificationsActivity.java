package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";
    
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Initialize UI components
        recyclerView = findViewById(R.id.notificationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);
        
        // Load notifications
        loadNotifications();
    }
    
    private void loadNotifications() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getDisplayName() == null) {
            Toast.makeText(this, "You need to be signed in to view notifications", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error: User is null or has no display name");
            return;
        }
        
        Log.d(TAG, "Attempting to load notifications for user: " + user.getDisplayName());
        
        try {
            // Query Firestore for this user's notifications - simplified query
            firestore.collection("notifications")
                .whereEqualTo("toUser", user.getDisplayName())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Query succeeded. Document count: " + 
                          (queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : "null"));
                    
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(NotificationsActivity.this, "No notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    notificationList.clear();
                    List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                    
                    // Prepare a batch to mark all notifications as read
                    WriteBatch batch = firestore.batch();
                    
                    for (DocumentSnapshot document : documents) {
                        try {
                            Log.d(TAG, "Processing document: " + document.getId());
                            Notification notification = document.toObject(Notification.class);
                            if (notification != null) {
                                // Set the document ID
                                notification.setId(document.getId());
                                
                                // Log each notification for debugging
                                Log.d(TAG, "Retrieved notification: id=" + notification.getId() + 
                                      ", type=" + notification.getType() + 
                                      ", from=" + notification.getFromUser() + 
                                      ", timestamp=" + notification.getTimestamp());
                                
                                // Make sure timestamp is valid (might be very old test data)
                                if (notification.getTimestamp() <= 1) {
                                    notification.setTimestamp(System.currentTimeMillis());
                                }
                                
                                notificationList.add(notification);
                                
                                // Mark as read in the batch
                                batch.update(document.getReference(), "isRead", true);
                            } else {
                                Log.w(TAG, "Document couldn't be converted to Notification: " + document.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing notification document: " + document.getId(), e);
                        }
                    }
                    
                    // Sort the list by timestamp (newest first) after loading
                    Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    
                    // Commit the batch to mark all as read
                    if (!notificationList.isEmpty()) {
                        batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "All notifications marked as read");
                            })
                            .addOnFailureListener(e -> {
                                String errorMessage = e.getMessage();
                                Log.e(TAG, "Error marking notifications as read: " + errorMessage, e);
                                
                                // Check if this is a missing index error
                                if (errorMessage != null && errorMessage.contains("FAILED_PRECONDITION") && 
                                    errorMessage.contains("index")) {
                                    // This is likely a missing index error
                                    Toast.makeText(NotificationsActivity.this, 
                                        "First-time setup: Please check logs and create the required index in Firebase console", 
                                        Toast.LENGTH_LONG).show();
                                    Log.w(TAG, "Missing index detected. Please follow the URL in the error message to create it in Firebase console.");
                                } else {
                                    Toast.makeText(NotificationsActivity.this, 
                                        "Error marking notifications as read: " + errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                    } else {
                        Log.w(TAG, "No notifications to mark as read");
                    }
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage();
                    Log.e(TAG, "Error loading notifications: " + errorMessage, e);
                    
                    // Check if this is a missing index error
                    if (errorMessage != null && errorMessage.contains("FAILED_PRECONDITION") && 
                        errorMessage.contains("index")) {
                        // This is likely a missing index error
                        Toast.makeText(NotificationsActivity.this, 
                            "First-time setup: Please check logs and create the required index in Firebase console", 
                            Toast.LENGTH_LONG).show();
                        Log.w(TAG, "Missing index detected. Please follow the URL in the error message to create it in Firebase console.");
                    } else {
                        Toast.makeText(NotificationsActivity.this, 
                            "Error loading notifications: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in loadNotifications: " + e.getMessage(), e);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Adapter for the notifications RecyclerView
     */
    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
        
        private final List<Notification> notifications;
        
        NotificationAdapter(List<Notification> notifications) {
            this.notifications = notifications;
        }
        
        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            Notification notification = notifications.get(position);
            
            // Set the icon based on notification type
            String type = notification.getType();
            Log.d(TAG, "Binding notification with type: " + type);
            
            if ("like".equals(type)) {
                holder.iconView.setImageResource(android.R.drawable.btn_star_big_on);
                holder.messageView.setText(notification.getFromUser() + " liked your photo");
            } else if ("follow".equals(type)) {
                holder.iconView.setImageResource(android.R.drawable.ic_menu_add);
                holder.messageView.setText(notification.getFromUser() + " started following you");
            } else {
                // Default case for any unexpected types
                holder.iconView.setImageResource(android.R.drawable.ic_dialog_info);
                holder.messageView.setText(notification.getFromUser() + " interacted with your profile");
                Log.w(TAG, "Unknown notification type: " + type);
            }
            
            // Set the time
            try {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                        notification.getTimestamp(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                holder.timeView.setText(timeAgo);
            } catch (Exception e) {
                Log.e(TAG, "Error formatting time", e);
                holder.timeView.setText("Recently");
            }
            
            // Set click listener to navigate to the sender's profile
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationsActivity.this, ProfileActivity.class);
                intent.putExtra("USER_NAME", notification.getFromUser());
                startActivity(intent);
            });
        }
        
        @Override
        public int getItemCount() {
            return notifications.size();
        }
        
        class NotificationViewHolder extends RecyclerView.ViewHolder {
            ImageView iconView;
            TextView messageView;
            TextView timeView;
            
            NotificationViewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.notificationIcon);
                messageView = itemView.findViewById(R.id.notificationMessage);
                timeView = itemView.findViewById(R.id.notificationTime);
            }
        }
    }
} 