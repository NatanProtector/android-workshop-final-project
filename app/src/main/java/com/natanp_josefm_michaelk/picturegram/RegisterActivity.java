package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.messaging.FirebaseMessaging;

// ⬅️ NEW imports for Firestore
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText editTextUsername;
    EditText editTextEmail;
    EditText editTextPassword;
    EditText editTextConfirmPassword;
    Button buttonSubmit;
    Button buttonBack;

    private FirebaseAuth mAuth;
    private static final String TAG = "RegisterActivity";

    // ⬅️ NEW: Firestore instance
    private FirebaseFirestore db;

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

        // Initialize Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();         // ⬅️ NEW

        editTextUsername        = findViewById(R.id.editTextRegisterUsername);
        editTextEmail           = findViewById(R.id.editTextRegisterEmail);
        editTextPassword        = findViewById(R.id.editTextRegisterPassword);
        editTextConfirmPassword = findViewById(R.id.editTextRegisterConfirmPassword);
        buttonSubmit            = findViewById(R.id.buttonRegisterSubmit);
        buttonBack              = findViewById(R.id.buttonRegisterBack);

        buttonSubmit.setOnClickListener(v -> {
            String username       = editTextUsername.getText().toString().trim();
            String email          = editTextEmail.getText().toString().trim();
            String password       = editTextPassword.getText().toString().trim();
            String confirmPassword= editTextConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if username is already taken
            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Username is already taken
                            Toast.makeText(RegisterActivity.this, 
                                "Username is already taken. Please choose another one.", 
                                Toast.LENGTH_SHORT).show();
                        } else {
                            // Username is available, proceed with registration
                            createUserAccount(email, password, username);
                        }
                    } else {
                        // Error checking username
                        Log.w(TAG, "Error checking username availability", task.getException());
                        Toast.makeText(RegisterActivity.this,
                            "Error checking username availability. Please try again.",
                            Toast.LENGTH_SHORT).show();
                    }
                });
        });

        buttonBack.setOnClickListener(v -> finish());
    }

    // New method to handle user account creation
    private void createUserAccount(String email, String password, String username) {
        // 1) Create the user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // 2) Update displayName
                        UserProfileChangeRequest profileUpdates =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build();

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        Log.d(TAG, "Profile updated");
                                        // 3) Save to Firestore
                                        saveUserToFirestore(user, username);
                                        navigateToLogin();
                                    } else {
                                        Log.w(TAG, "Profile update failed", profileTask.getException());
                                        // still save to Firestore & continue
                                        saveUserToFirestore(user, username);
                                        navigateToLogin();
                                    }
                                });
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ⬅️ NEW: Write user info into Firestore
    private void saveUserToFirestore(FirebaseUser firebaseUser, String username) {
        String uid = firebaseUser.getUid();
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", firebaseUser.getEmail());
        userMap.put("profileImageUrl", ""); // will fill later if you add a profile photo
        userMap.put("bio", "");
        
        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                userMap.put("fcmToken", token);

                // Save user to Firestore with FCM token
                db.collection("users")
                    .document(uid)
                    .set(userMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firestore with FCM token"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error saving user to Firestore", e));
            });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
