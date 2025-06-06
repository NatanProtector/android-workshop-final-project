package com.natanp_josefm_michaelk.picturegram;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 3000;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize notification channel
        try {
            NotificationHelper.createNotificationChannel(this);
            Log.d(TAG, "Notification channel initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing notification channel", e);
        }
        
        // Setup permission launcher with better feedback
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                } else {
                    Log.w(TAG, "Notification permission denied");
                    Toast.makeText(this, 
                            "Notifications will be disabled. You can enable them in app settings.", 
                            Toast.LENGTH_LONG).show();
                }
            }
        );
        
        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Start the background notification service
        startService(new Intent(this, BackgroundNotificationService.class));

        // Update FCM token if user is already signed in
        FCMTokenService.updateCurrentUserToken();

        // Create a handler to delay the transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is already signed in
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;
            
            if (currentUser != null) {
                // User is signed in, go directly to ProfileActivity
                intent = new Intent(SplashActivity.this, ProfileActivity.class);
                String userName = (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) 
                                ? currentUser.getDisplayName() 
                                : "Default User";
                intent.putExtra("USER_NAME", userName);
                intent.putExtra("USER_IMAGE", R.mipmap.ic_launcher);
            } else {
                // User is not signed in, go to LoginActivity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            
            startActivity(intent);
            finish(); // Close the splash activity
        }, SPLASH_DELAY);
    }
    
    private void requestNotificationPermission() {
        try {
            // Only needed for Android 13+ (API level 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != 
                        PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Requesting POST_NOTIFICATIONS permission");
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    Log.d(TAG, "POST_NOTIFICATIONS permission already granted");
                }
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission not needed for this Android version");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting notification permission", e);
        }
    }
}