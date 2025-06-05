package com.natanp_josefm_michaelk.picturegram;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMTokenService extends FirebaseMessagingService {
    private static final String TAG = "FCMTokenService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Update the token in Firestore for the current user
        updateTokenInFirestore(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            
            // Show notification through BackgroundNotificationService
            android.content.Intent serviceIntent = new android.content.Intent(this, BackgroundNotificationService.class);
            serviceIntent.putExtra("message", remoteMessage);
            startService(serviceIntent);
        }
    }

    private void updateTokenInFirestore(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM Token updated successfully in Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating FCM token in Firestore", e);
                });
        } else {
            Log.w(TAG, "No authenticated user found, cannot update FCM token");
        }
    }

    /**
     * Static method to update FCM token for the current user
     * This can be called from other activities when user signs in
     */
    public static void updateCurrentUserToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "Current FCM Token: " + token);

                    // Update token in Firestore
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .update("fcmToken", token)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "FCM Token updated successfully for current user");
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error updating FCM token for current user", e);
                        });
                });
        }
    }
} 