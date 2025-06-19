package com.natanp_josefm_michaelk.picturegram;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the ActionBar for a true fullscreen view
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_full_screen_image);

        ImageView imageView = findViewById(R.id.fullScreenImageView);

        // Path (local) or URL (remote) passed from the caller
        String imgPath = getIntent().getStringExtra("IMAGE_PATH");

        Glide.with(this)
                .load(imgPath)
                .fitCenter()
                .placeholder(R.mipmap.ic_launcher)
                .into(imageView);

        // Tap anywhere to close
        imageView.setOnClickListener(v -> finish());
    }
}
