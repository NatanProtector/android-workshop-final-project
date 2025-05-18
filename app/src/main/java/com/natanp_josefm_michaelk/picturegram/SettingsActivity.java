package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private Button logoutButton, submitButton;
    private FirebaseAuth mAuth;
    private TextInputEditText bioInput, emailInput, usernameInput;
    private MaterialSwitch themeSwitch, notificationsSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Initialize UI elements
        initializeViews();
        setupThemeSwitch();
        setupNotificationsSwitch();
        setupButtons();
    }

    private void initializeViews() {
        logoutButton = findViewById(R.id.logoutButton);
        submitButton = findViewById(R.id.submitButton);
        bioInput = findViewById(R.id.bioInput);
        emailInput = findViewById(R.id.emailInput);
        usernameInput = findViewById(R.id.usernameInput);
        themeSwitch = findViewById(R.id.themeSwitch);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
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
        submitButton.setOnClickListener(v -> {
            // Do nothing for now
            Toast.makeText(this, "Changes will be implemented soon", Toast.LENGTH_SHORT).show();
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