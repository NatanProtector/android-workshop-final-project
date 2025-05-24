package com.natanp_josefm_michaelk.picturegram;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Helper class to manage system notifications for the app
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    // Notification channel ID
    private static final String CHANNEL_ID = "picturegram_notifications";
    // Notification types
    private static final int NOTIFICATION_FOLLOW = 1;
    private static final int NOTIFICATION_LIKE = 2;
    
    // Create notification channel (required for Android 8.0 and above)
    public static void createNotificationChannel(Context context) {
        try {
            // Only needed for API level 26+ (Android 8.0 and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "PictureGram Notifications";
                String description = "Notifications for new followers and likes";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                
                // Register the channel with the system
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created successfully");
                } else {
                    Log.e(TAG, "Could not get NotificationManager service");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel", e);
        }
    }
    
    // Show a follow notification
    public static void showFollowNotification(Context context, String username) {
        try {
            // Check if we have notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != 
                        PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "No notification permission granted");
                    return;
                }
            }
            
            // Intent to open NotificationsActivity when notification is tapped
            Intent intent = new Intent(context, NotificationsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // Use FLAG_IMMUTABLE or FLAG_MUTABLE based on API level
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);
            
            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
                    .setContentTitle("New Follower")
                    .setContentText(username + " started following you")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Show the notification
            showNotification(context, NOTIFICATION_FOLLOW, builder);
        } catch (Exception e) {
            Log.e(TAG, "Error showing follow notification", e);
            Toast.makeText(context, "Could not show notification", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Show a like notification
    public static void showLikeNotification(Context context, String username) {
        try {
            // Check if we have notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != 
                        PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "No notification permission granted");
                    return;
                }
            }
            
            // Intent to open NotificationsActivity when notification is tapped
            Intent intent = new Intent(context, NotificationsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // Use FLAG_IMMUTABLE or FLAG_MUTABLE based on API level
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);
            
            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
                    .setContentTitle("New Like")
                    .setContentText(username + " liked your photo")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Show the notification
            showNotification(context, NOTIFICATION_LIKE, builder);
        } catch (Exception e) {
            Log.e(TAG, "Error showing like notification", e);
            Toast.makeText(context, "Could not show notification", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Helper method to show a notification
    private static void showNotification(Context context, int notificationId, NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            
            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != 
                        PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "POST_NOTIFICATIONS permission not granted");
                    Toast.makeText(context, 
                            "Please grant notification permission in app settings", 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification shown successfully with ID: " + notificationId);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when showing notification", e);
            Toast.makeText(context, 
                    "Notification permission not granted", 
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error showing notification", e);
            Toast.makeText(context, 
                    "Error showing notification", 
                    Toast.LENGTH_SHORT).show();
        }
    }
} 