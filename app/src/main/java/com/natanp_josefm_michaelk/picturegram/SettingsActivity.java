package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private Button logoutButton, submitButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText bioInput;
    private MaterialSwitch themeSwitch, notificationsSwitch;
    private SharedPreferences sharedPreferences;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Initialize UI elements
        initializeViews();
        setupThemeSwitch();
        setupNotificationsSwitch();
        setupButtons();
        loadUserData();
    }

    private void initializeViews() {
        logoutButton = findViewById(R.id.logoutButton);
        submitButton = findViewById(R.id.submitButton);
        bioInput = findViewById(R.id.bioInput);
        themeSwitch = findViewById(R.id.themeSwitch);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
    }

    private void loadUserData() {
        if (currentUser == null) return;

        // Load user data from Firestore
        db.collection("users").document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String bio = documentSnapshot.getString("bio");
                    bioInput.setText(bio);
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error loading user data", e);
                Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupThemeSwitch() {
        // Set initial state
        boolean isDarkMode = sharedPreferences.getBoolean("darkMode", false);
        themeSwitch.setChecked(isDarkMode);
        
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            sharedPreferences.edit().putBoolean("darkMode", isChecked).apply();
            
            // Apply theme
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void setupNotificationsSwitch() {
        // Set initial state
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications", true);
        notificationsSwitch.setChecked(notificationsEnabled);
        updateNotificationsText(notificationsEnabled);
        
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
            // Update the text
            updateNotificationsText(isChecked);
        });
    }

    private void updateNotificationsText(boolean isEnabled) {
        notificationsSwitch.setText(isEnabled ? "Disable Notifications" : "Enable Notifications");
    }

    private void setupButtons() {
        logoutButton.setOnClickListener(v -> logoutUser());
        submitButton.setOnClickListener(v -> updateUserProfile());
    }

    private void updateUserProfile() {
        if (currentUser == null) return;

        String newBio = bioInput.getText().toString().trim();

        // Update Firestore document
        db.collection("users").document(currentUser.getUid())
            .update("bio", newBio)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User data updated in Firestore");
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                
                // Navigate back to ProfileActivity
                Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
                intent.putExtra("USER_NAME", currentUser.getDisplayName());
                intent.putExtra("USER_IMAGE", R.mipmap.ic_launcher);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error updating user data", e);
                Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Navigate back to MainActivity (Login Screen)
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        // Clear the activity stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close the SettingsActivity
    }
}