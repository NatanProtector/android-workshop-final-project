package com.natanp_josefm_michaelk.picturegram;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get views
        ImageView profileImageView = findViewById(R.id.profileImageView);
        TextView profileNameTextView = findViewById(R.id.profileNameTextView);

        // Get the data passed from the adapter
        String userName = getIntent().getStringExtra("USER_NAME");
        int imageResourceId = getIntent().getIntExtra("USER_IMAGE", R.mipmap.ic_launcher);

        // Set the data to the views
        profileNameTextView.setText(userName);
        profileImageView.setImageResource(imageResourceId);
    }
}