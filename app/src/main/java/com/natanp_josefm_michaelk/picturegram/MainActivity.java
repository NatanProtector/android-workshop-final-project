package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/*  Notes:
 *  - changing screen orientation of screen destroys and creates the screen, must save info and reload
 *  - never block the main thread by putting loops in event listeners, USE ANOTHER THREAD
 *  - must add splash activity with our names and logo (optional)
 * */

public class MainActivity extends AppCompatActivity {

    EditText editTextEmail;
    EditText editTextPassword;
    Button buttonLogin;
    TextView textViewRegisterLink;

    private static final String TAG = "MainActivity"; // Tag for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();

                // Log the inputs
                Log.d(TAG, "Email: " + email);
                Log.d(TAG, "Password: " + password); // Be cautious logging passwords in production!

                // Basic validation (you can add more complex logic here)
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                } else {
                    // TODO: Add actual login logic here (e.g., call an API, check database)
                    Toast.makeText(MainActivity.this, "Login details logged", Toast.LENGTH_SHORT).show(); // Inform user action was logged
                }
            }
        });

        textViewRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to start RegisterActivity
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // The method must match the name from XML and have this exact signature
    public void openUsersActivity(View view) {
        Intent intent = new Intent(this, UsersActivity.class);
        startActivity(intent);
    }

    public void openSettingsActivity(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

}