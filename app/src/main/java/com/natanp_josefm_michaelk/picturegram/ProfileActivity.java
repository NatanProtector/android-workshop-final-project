package com.natanp_josefm_michaelk.picturegram;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private static final int PHOTO_REQUEST_CODE = 1001;
    
    private ImageView profileImageView;
    private TextView profileNameTextView;
    private Button uploadPhotoButton;
    private RecyclerView photosRecyclerView;
    private PhotoAdapter photoAdapter;
    
    private List<UserPhoto> userPhotoList;
    private String userName;
    private int profileImageId;
    
    private ActivityResultLauncher<Intent> photoSelectionLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri currentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get views
        profileImageView = findViewById(R.id.profileImageView);
        profileNameTextView = findViewById(R.id.profileNameTextView);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        photosRecyclerView = findViewById(R.id.photosRecyclerView);

        // Get the data passed from the adapter
        userName = getIntent().getStringExtra("USER_NAME");
        profileImageId = getIntent().getIntExtra("USER_IMAGE", R.mipmap.ic_launcher);

        // Set the data to the views
        profileNameTextView.setText(userName);
        profileImageView.setImageResource(profileImageId);
        
        // Initialize photo list
        userPhotoList = new ArrayList<>();
        
        // Add some sample photos (in a real app, these would come from a database)
        addSamplePhotos();
        
        // Setup RecyclerView for photos with a grid layout
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        photosRecyclerView.setLayoutManager(layoutManager);
        
        // Create and set adapter
        photoAdapter = new PhotoAdapter(userPhotoList, this);
        photosRecyclerView.setAdapter(photoAdapter);
        
        // Setup item touch helper for drag and drop reordering
        setupItemTouchHelper();
        
        // Register for activity result to get selected photos
        photoSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<Integer> selectedPhotoIds = result.getData().getIntegerArrayListExtra("SELECTED_PHOTOS");
                        if (selectedPhotoIds != null && !selectedPhotoIds.isEmpty()) {
                            // Add the selected photos to the user's gallery
                            for (Integer photoId : selectedPhotoIds) {
                                UserPhoto newPhoto = new UserPhoto(photoId);
                                userPhotoList.add(newPhoto);
                            }
                            photoAdapter.notifyDataSetChanged();
                            Toast.makeText(this, selectedPhotoIds.size() + " photos added", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Register for gallery photo selection
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // In a real app, you would save this image to your app's storage
                            // For this example, we'll use a placeholder drawable
                            UserPhoto newPhoto = new UserPhoto(R.drawable.my_img1);
                            newPhoto.setDescription("Photo from gallery");
                            userPhotoList.add(newPhoto);
                            photoAdapter.notifyItemInserted(userPhotoList.size() - 1);
                            Toast.makeText(this, "Photo added from gallery", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Register for camera photo capture
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentPhotoUri != null) {
                        // In a real app, you would save this image to your app's storage
                        // For this example, we'll use a placeholder drawable
                        UserPhoto newPhoto = new UserPhoto(R.drawable.my_img2);
                        newPhoto.setDescription("Photo from camera");
                        userPhotoList.add(newPhoto);
                        photoAdapter.notifyItemInserted(userPhotoList.size() - 1);
                        Toast.makeText(this, "Photo captured from camera", Toast.LENGTH_SHORT).show();
                    }
                });
        
        // Set upload button listener
        uploadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoSourceDialog();
            }
        });
    }
    
    private void setupItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END,
                0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                @NonNull RecyclerView.ViewHolder viewHolder, 
                                @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                Collections.swap(userPhotoList, fromPosition, toPosition);
                photoAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used for drag & drop
            }
        };
        
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(photosRecyclerView);
    }
    
    private void showPhotoSourceDialog() {
        String[] options = {"Choose from Gallery", "Take Photo", "Choose from Sample Photos"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Gallery
                        openGallery();
                        break;
                    case 1: // Camera
                        openCamera();
                        break;
                    case 2: // Sample photos
                        openSamplePhotos();
                        break;
                }
            }
        });
        builder.show();
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }
    
    private void openCamera() {
        try {
            // Create a file to save the image
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(this,
                    "com.natanp_josefm_michaelk.picturegram.fileprovider",
                    photoFile);
            cameraLauncher.launch(currentPhotoUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }
    
    private void openSamplePhotos() {
        Intent intent = new Intent(ProfileActivity.this, PhotoSelectionActivity.class);
        photoSelectionLauncher.launch(intent);
    }
    
    private void addSamplePhotos() {
        // Add some sample photos from our drawable resources
        userPhotoList.add(new UserPhoto(R.drawable.my_img1, "My first photo"));
        userPhotoList.add(new UserPhoto(R.drawable.my_img2, "Another cute cat"));
        userPhotoList.add(new UserPhoto(R.drawable.my_img3));
        userPhotoList.add(new UserPhoto(R.drawable.my_img4, "Just chillin'"));
    }

    @Override
    public void onPhotoClick(UserPhoto photo, int position) {
        // In a full implementation, this would open a detail view of the photo
        Toast.makeText(this, "Photo clicked at position " + position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLikeClick(UserPhoto photo, int position) {
        // Increase like count
        photo.setLikeCount(photo.getLikeCount() + 1);
        photoAdapter.notifyItemChanged(position);
    }
    
    @Override
    public void onDeleteClick(UserPhoto photo, int position) {
        // Remove the photo from the list
        userPhotoList.remove(position);
        photoAdapter.notifyItemRemoved(position);
        Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onEditDescriptionClick(UserPhoto photo, int position) {
        // Show dialog to edit description
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Description");
        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(photo.getDescription());
        builder.setView(input);
        
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newDescription = input.getText().toString();
                photo.setDescription(newDescription);
                photoAdapter.notifyItemChanged(position);
            }
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
}