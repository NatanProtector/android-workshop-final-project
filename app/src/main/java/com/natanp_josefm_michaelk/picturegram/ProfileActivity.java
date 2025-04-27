package com.natanp_josefm_michaelk.picturegram;

import com.google.firebase. auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private static final String TAG = "ProfileActivity";
    private static final String PHOTOS_PREFS = "photos_prefs";
    private static final String PHOTOS_KEY = "user_photos";
    
    // Permission Request Codes
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 101;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 102;

    private RecyclerView photosRecyclerView;
    private PhotoAdapter photoAdapter;
    
    private List<UserPhoto> userPhotoList;
    private String userName;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri currentPhotoUri;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView notAuthenticatedTextView = findViewById(R.id.notAuthenticatedTextView);
        androidx.constraintlayout.widget.Group profileContentGroup = findViewById(R.id.profileContentGroup);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // User is NOT signed in
            notAuthenticatedTextView.setVisibility(View.VISIBLE);
            profileContentGroup.setVisibility(View.GONE);
            return; // Stop further initialization
        } else {
            // User is signed in
            notAuthenticatedTextView.setVisibility(View.GONE);
            profileContentGroup.setVisibility(View.VISIBLE);
        }

        // Get views
        ImageView profileImageView = findViewById(R.id.profileImageView);
        TextView profileNameTextView = findViewById(R.id.profileNameTextView);
        Button uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        photosRecyclerView = findViewById(R.id.photosRecyclerView);
        Button settingsButton = findViewById(R.id.settingsButton);
        Button addFriendButton = findViewById(R.id.addFriendButton);

        // Get the data passed from the adapter
        userName = getIntent().getStringExtra("USER_NAME");
        int profileImageId = getIntent().getIntExtra("USER_IMAGE", R.mipmap.ic_launcher);

        // Set the data to the views
        profileNameTextView.setText(userName);
        profileImageView.setImageResource(profileImageId);
        
        // Check if this profile belongs to the current user
        String currentUserName = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "";
        if (userName != null && userName.equals(currentUserName)) {
            // It's the current user's profile
            settingsButton.setVisibility(View.VISIBLE);
            addFriendButton.setVisibility(View.GONE);
            // Optionally allow uploading only on own profile
            uploadPhotoButton.setVisibility(View.VISIBLE);
            uploadPhotoButton.setEnabled(true);
        } else {
            // It's someone else's profile
            settingsButton.setVisibility(View.GONE);
            addFriendButton.setVisibility(View.VISIBLE);
            // Optionally hide uploading on other profiles
            uploadPhotoButton.setVisibility(View.GONE);
            uploadPhotoButton.setEnabled(false);
        }

        // Set listener for Add Friend button
        addFriendButton.setOnClickListener(v -> {
            // TODO: Implement actual friend request logic
            Toast.makeText(ProfileActivity.this, "Friend request sent to " + userName, Toast.LENGTH_SHORT).show();
        });
        
        // Initialize photo list
        userPhotoList = loadPhotos();
        
        // Setup RecyclerView for photos with a grid layout
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        photosRecyclerView.setLayoutManager(layoutManager);
        
        // Create and set adapter
        photoAdapter = new PhotoAdapter(userPhotoList, this, userName);
        photosRecyclerView.setAdapter(photoAdapter);
        
        // Setup item touch helper for drag and drop reordering
        setupItemTouchHelper();
        
        // Register for gallery photo selection
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                // Copy the file to our app's storage
                                String filePath = saveImageToInternalStorage(selectedImageUri);
                                
                                // Create a new UserPhoto with the file path
                                UserPhoto newPhoto = new UserPhoto(filePath, "Photo from gallery");
                                userPhotoList.add(newPhoto);
                                photoAdapter.notifyItemInserted(userPhotoList.size() - 1);
                                savePhotos();
                                Toast.makeText(this, "Photo added from gallery", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Log.e(TAG, "Error saving image: " + e.getMessage());
                                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // Register for camera photo capture
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentPhotoUri != null) {
                        try {
                            Log.d(TAG, "Camera photo captured successfully at: " + currentPhotoPath);
                            
                            // Create a new UserPhoto with the file path
                            UserPhoto newPhoto = new UserPhoto(currentPhotoPath, "Photo from camera");
                            userPhotoList.add(newPhoto);
                            photoAdapter.notifyItemInserted(userPhotoList.size() - 1);
                            savePhotos();
                            
                            // Display success message
                            Toast.makeText(this, "Photo captured and saved successfully", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing camera image: " + e.getMessage(), e);
                            Toast.makeText(this, "Failed to process camera image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Camera capture failed or canceled: success=" + success + ", uri=" + currentPhotoUri);
                        if (!success) {
                            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show();
                        }
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
    
    private String getStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    private String saveImageToInternalStorage(Uri imageUri) throws IOException {
        // Get a file name from the URI if possible
        String fileName = getFileNameFromUri(imageUri);
        if (fileName == null) {
            // Create a unique file name if we couldn't get one
            fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        }
        
        // Create the file in our app's private directory
        File directory = new File(getFilesDir(), "photos");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File file = new File(directory, fileName);
        try (InputStream is = getContentResolver().openInputStream(imageUri);
             OutputStream os = new FileOutputStream(file)) {
            
            if (is == null) {
                throw new IOException("Failed to open input stream from URI");
            }
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            
            return file.getAbsolutePath();
        }
    }
    
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        result = cursor.getString(idx);
                    }
                }
            }
        }
        
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        
        return result;
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
                savePhotos(); // Save the new order
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used for drag & drop
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(photosRecyclerView);
        photoAdapter.setTouchHelper(touchHelper);
    }
    
    private void showPhotoSourceDialog() {
        String[] options = {"Choose from Gallery", "Take Photo"};
        
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
                }
            }
        });
        builder.show();
    }
    
    private void openGallery() {
        String storagePermission = getStoragePermission();
        if (ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{storagePermission}, REQUEST_CODE_STORAGE_PERMISSION);
        }
    }
    
    private void openCamera() {
        Log.d(TAG, "openCamera called");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with camera logic
            launchCameraIntent();
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
        }
    }
    
    // Separated camera intent launching logic
    private void launchCameraIntent() {
        try {
            // Create a file to save the image
            File photoFile = createImageFile();
            currentPhotoPath = photoFile.getAbsolutePath();
            Log.d(TAG, "Created image file at: " + currentPhotoPath);

            // Get URI for the file using FileProvider
            currentPhotoUri = FileProvider.getUriForFile(this,
                    "com.natanp_josefm_michaelk.picturegram.fileprovider",
                    photoFile);
            Log.d(TAG, "FileProvider URI: " + currentPhotoUri);

            // Launch camera
            cameraLauncher.launch(currentPhotoUri);
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file: " + ex.getMessage(), ex);
            Toast.makeText(this, "Error creating image file: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error opening camera: " + ex.getMessage(), ex);
            Toast.makeText(this, "Camera error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        
        // Create a directory for storing photos if it doesn't exist
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            Log.d(TAG, "Creating pictures directory");
            storageDir.mkdirs();
        }
        
        // Create the file
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        
        return image;
    }
    
    @Override
    public void onPhotoClick(UserPhoto photo, int position) {
        // In a full implementation, this would open a detail view of the photo
        Toast.makeText(this, "Photo clicked at position " + position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLikeClick(UserPhoto photo, int position) {
        // Toggle like with the current user's name
        boolean likeToggled = photo.toggleLike(userName);
        
        if (likeToggled) {
            // The like state changed (either added or removed)
            photoAdapter.notifyItemChanged(position);
            savePhotos();
            
            // Show appropriate message
            String message = photo.isLikedByUser(userName) ? "Photo liked" : "Like removed";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDeleteClick(UserPhoto photo, int position) {
        // If it's a file-based photo, delete the actual file
        if (photo.hasFilePath()) {
            File photoFile = new File(photo.getFilePath());
            if (photoFile.exists()) {
                photoFile.delete();
            }
        }
        
        // Remove the photo from the list
        userPhotoList.remove(position);
        photoAdapter.notifyItemRemoved(position);
        
        // Save the updated list
        savePhotos();
        
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
                savePhotos();
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
    
    private void savePhotos() {
        SharedPreferences sharedPreferences = getSharedPreferences(PHOTOS_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        Gson gson = new Gson();
        String json = gson.toJson(userPhotoList);
        
        // Save photos for the current user
        editor.putString(PHOTOS_KEY + "_" + userName, json);
        editor.apply();
    }
    
    private List<UserPhoto> loadPhotos() {
        SharedPreferences sharedPreferences = getSharedPreferences(PHOTOS_PREFS, Context.MODE_PRIVATE);
        
        Gson gson = new Gson();
        String json = sharedPreferences.getString(PHOTOS_KEY + "_" + userName, "");
        
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<ArrayList<UserPhoto>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void openUsersActivityFromProfile(View view) {
        Intent intent = new Intent(this, UsersActivity.class);
        startActivity(intent);
    }

    public void openSettingsActivityFromProfile(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Storage permission granted, open gallery
                openGallery();
            } else {
                // Storage permission denied
                Toast.makeText(this, "Storage permission is required to choose photos from gallery.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, launch camera
                launchCameraIntent();
            } else {
                // Camera permission denied
                Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}