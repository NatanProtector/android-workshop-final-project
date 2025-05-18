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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private Button logoutButton, submitButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText bioInput, usernameInput;
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
        usernameInput = findViewById(R.id.usernameInput);
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
                    String username = documentSnapshot.getString("username");

                    bioInput.setText(bio);
                    usernameInput.setText(username);
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
        String newUsername = usernameInput.getText().toString().trim();

        // Validate inputs
        if (newUsername.isEmpty()) {
            usernameInput.setError("Username cannot be empty");
            return;
        }

        // Check if username is unique (if changed)
        if (!newUsername.equals(currentUser.getDisplayName())) {
            db.collection("users")
                .whereEqualTo("username", newUsername)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            usernameInput.setError("Username is already taken");
                        } else {
                            // Username is unique, proceed with update
                            updateUserData(newBio, newUsername);
                        }
                    } else {
                        Log.w(TAG, "Error checking username", task.getException());
                        Toast.makeText(this, "Error checking username availability", Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            // Username not changed, proceed with update
            updateUserData(newBio, newUsername);
        }
    }

    private void updateUserData(String bio, String username) {
        // Update display name (username)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build();

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User profile updated");
                    
                    // Update Firestore document
                    db.collection("users").document(currentUser.getUid())
                        .update(
                            "bio", bio,
                            "username", username
                        )
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "User data updated in Firestore");
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            
                            // Navigate back to ProfileActivity with updated username
                            Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
                            intent.putExtra("USER_NAME", username);
                            intent.putExtra("USER_IMAGE", R.mipmap.ic_launcher); // Keep the same profile image
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the activity stack
                            startActivity(intent);
                            finish(); // Close the SettingsActivity
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error updating user data", e);
                            Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    Log.w(TAG, "Error updating profile", task.getException());
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                }
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