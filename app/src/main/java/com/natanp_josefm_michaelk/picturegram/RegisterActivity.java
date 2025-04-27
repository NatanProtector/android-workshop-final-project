package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    EditText editTextUsername;
    EditText editTextEmail;
    EditText editTextPassword;
    EditText editTextConfirmPassword;
    Button buttonSubmit;
    Button buttonBack;

    private FirebaseAuth mAuth;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Find views by ID
        editTextUsername = findViewById(R.id.editTextRegisterUsername);
        editTextEmail = findViewById(R.id.editTextRegisterEmail);
        editTextPassword = findViewById(R.id.editTextRegisterPassword);
        editTextConfirmPassword = findViewById(R.id.editTextRegisterConfirmPassword);
        buttonSubmit = findViewById(R.id.buttonRegisterSubmit);
        buttonBack = findViewById(R.id.buttonRegisterBack);

        // Set listener for the submit button
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUsername.getText().toString().trim();
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String confirmPassword = editTextConfirmPassword.getText().toString().trim();

                // Log the inputs
                Log.d(TAG, "Username: " + username);
                Log.d(TAG, "Email: " + email);
                Log.d(TAG, "Password attempt"); // Be cautious logging passwords

                // Basic validation
                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                } 
                
                if (password.length() < 6) { // Firebase requires passwords >= 6 characters
                    Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    // Optionally clear password fields
                    // editTextPassword.setText("");
                    // editTextConfirmPassword.setText("");
                    return;
                } 
                
                // Perform Firebase Registration
                registerUser(username, email, password);
            }
        });

        // Set listener for the back button
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish current activity and go back to the previous one (MainActivity)
                finish();
            }
        });
    }
    
    private void registerUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    // Update profile with username
                    updateUserProfile(user, username);
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                    Toast.makeText(RegisterActivity.this,
                            "Registration failed: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
    }

    private void updateUserProfile(FirebaseUser user, String username) {
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated with username.");
                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            // Navigate to MainActivity (Login Screen) after registration
                            navigateToLogin();
                        } else {
                            Log.w(TAG, "Failed to update profile.", task.getException());
                            // Still count as registration success, but inform user about profile issue
                            Toast.makeText(RegisterActivity.this, "Registration successful, but failed to set username.", Toast.LENGTH_LONG).show();
                            navigateToLogin(); 
                        }
                    });
        } else {
             // Should not happen if registration was successful, but handle it
             Log.e(TAG, "User object is null after registration.");
             Toast.makeText(RegisterActivity.this, "Registration completed but user data issue.", Toast.LENGTH_SHORT).show();
             navigateToLogin();
        }
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        // Optional: Clear previous activities if you don't want users going back to register
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close RegisterActivity
    }
}