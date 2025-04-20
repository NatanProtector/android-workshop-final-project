package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // The method must match the name from XML and have this exact signature
    public void openUsersActivity(View view) {
        Intent intent = new Intent(this, UsersActivity.class);
        startActivity(intent);
    }

}