package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/*  Notes:
 *  - changing screen orientation of screen destroys and creates the screen, must save info and reload
 *  - never block the main thread by putting loops in event listeners, USE ANOTHER THREAD
 *  - must add splash activity with our names and logo (optional)
 * */

public class LoginActivity extends AppCompatActivity {

    EditText editTextEmail;
    EditText editTextPassword;
    Button buttonLogin;
    TextView textViewRegisterLink;

    private FirebaseAuth mAuth;

    private static final String TAG = "MainActivity"; // Tag for logging

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToProfile(currentUser);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);

        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            // Log the inputs
            Log.d(TAG, "Email: " + email);
            Log.d(TAG, "Password: " + password); // Be cautious logging passwords in production!
            Log.d(TAG, "Password attempt");

            // Check if fields are empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Perform Firebase Login
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            // Update FCM token for the signed-in user
                            FCMTokenService.updateCurrentUserToken();
                            
                            navigateToProfile(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        textViewRegisterLink.setOnClickListener(v -> {
            // Intent to start RegisterActivity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void navigateToProfile(FirebaseUser user) {
        // Go to ProfileActivity
        // We should pass the user's name to ProfileActivity if possible
        String userName = (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) 
                        ? user.getDisplayName() 
                        : "Default User"; // Or derive from email if display name is null
        
        Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
        // Pass user data if needed, for example, user name for ProfileActivity display
        intent.putExtra("USER_NAME", userName); 
        // You might also want to pass a default profile image or retrieve it based on the user
        intent.putExtra("USER_IMAGE", R.mipmap.ic_launcher); // Example default
        startActivity(intent);
        finish(); // Optional: finish MainActivity so user can't go back to login screen
    }

}