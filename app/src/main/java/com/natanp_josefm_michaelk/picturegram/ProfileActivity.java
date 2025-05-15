package com.natanp_josefm_michaelk.picturegram;

import com.google.firebase. auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
import android.widget.FrameLayout;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
import java.util.UUID;

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

    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase instances
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

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
        addFriendButton.setText("Follow");
        addFriendButton.setOnClickListener(v -> {
            // Show a toast with the follow action
            Toast.makeText(ProfileActivity.this, "You are now following " + userName, Toast.LENGTH_SHORT).show();
            
            // Create a notification for the follow action
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getDisplayName() != null) {
                // Create and save notification to Firestore
                Notification notification = new Notification("follow", currentUser.getDisplayName(), userName);
                
                // Log notification data before saving
                Log.d(TAG, "Creating follow notification: from=" + notification.getFromUser() + 
                      ", to=" + notification.getToUser() + 
                      ", timestamp=" + notification.getTimestamp());
                
                firestore.collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Follow notification created with ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating follow notification", e);
                    });
            }
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
        setupGalleryLauncher();

        // Register for camera photo capture
        setupCameraLauncher();
        
        // Set upload button listener
        uploadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoSourceDialog();
            }
        });

        // Initialize notification views and listener
        FrameLayout notificationContainer = findViewById(R.id.notificationContainer);
        ImageView notificationBell = findViewById(R.id.notificationBell);
        TextView notificationCount = findViewById(R.id.notificationCount);
        
        // Only show notifications for the current user, not when viewing others' profiles
        if (userName != null && userName.equals(currentUserName)) {
            notificationContainer.setVisibility(View.VISIBLE);
            
            // Set up real-time listener for unread notifications count
            setupNotificationCounter(notificationCount);
            
            // Set up click listener for notification bell
            notificationBell.setOnClickListener(v -> {
                // Open NotificationsActivity when bell is clicked
                Intent intent = new Intent(ProfileActivity.this, NotificationsActivity.class);
                startActivity(intent);
            });
        } else {
            notificationContainer.setVisibility(View.GONE);
        }
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getDisplayName() == null) {
            Toast.makeText(this, "You must be logged in to like photos", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUsername = currentUser.getDisplayName();
        boolean isCurrentlyLiked = photo.isLikedByUser(currentUsername);
        
        // Check if this photo has a Firestore ID
        if (photo.hasFirestoreId()) {
            // This photo is in Firestore, update likes there
            updateLikeInFirestore(photo, currentUsername, !isCurrentlyLiked);
        } else {
            // This is a legacy photo, just use local storage
            handleLegacyLike(photo, position, currentUsername);
        }
    }
    
    private void updateLikeInFirestore(UserPhoto photo, String username, boolean isLiking) {
        // Get the Firestore document reference for this photo
        String photoId = photo.getFirestoreId();
        
        // Create a reference to the photos document
        // Update the Firestore document
        if (isLiking) {
            // Add the user's name to likedBy array and increment likeCount
            firestore.collection("photos").document(photoId)
                .update(
                    "likedBy", FieldValue.arrayUnion(username),
                    "likeCount", FieldValue.increment(1)
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Like added to Firestore for photo: " + photoId);
                    
                    // Update local object
                    if (!photo.isLikedByUser(username)) {
                        photo.getLikedByUsers().add(username);
                        photo.setLikeCount(photo.getLikeCount() + 1);
                        photoAdapter.notifyItemChanged(userPhotoList.indexOf(photo));
                    }
                    
                    // Show success message
                    Toast.makeText(this, "Photo liked", Toast.LENGTH_SHORT).show();
                    
                    // Create notification (if this is not the user's own photo)
                    if (!username.equals(photo.getAuthorName())) {
                        createLikeNotification(username, photo.getAuthorName());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating like in Firestore", e);
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                });
        } else {
            // Remove the user's name from likedBy array and decrement likeCount
            firestore.collection("photos").document(photoId)
                .update(
                    "likedBy", FieldValue.arrayRemove(username),
                    "likeCount", FieldValue.increment(-1)
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Like removed from Firestore for photo: " + photoId);
                    
                    // Update local object
                    if (photo.isLikedByUser(username)) {
                        photo.getLikedByUsers().remove(username);
                        photo.setLikeCount(Math.max(0, photo.getLikeCount() - 1)); // Ensure we don't go below 0
                        photoAdapter.notifyItemChanged(userPhotoList.indexOf(photo));
                    }
                    
                    // Show success message
                    Toast.makeText(this, "Like removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing like in Firestore", e);
                    Toast.makeText(this, "Failed to remove like", Toast.LENGTH_SHORT).show();
                });
        }
    }
    
    private void handleLegacyLike(UserPhoto photo, int position, String username) {
        // Toggle like with the user's name (legacy approach)
        boolean likeToggled = photo.toggleLike(username);
        
        if (likeToggled) {
            // The like state changed (either added or removed)
            photoAdapter.notifyItemChanged(position);
            savePhotos();
            
            // Show appropriate message
            String message = photo.isLikedByUser(username) ? "Photo liked" : "Like removed";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            
            // If this is a new like (not a removal), create a notification
            if (photo.isLikedByUser(username)) {
                if (!username.equals(photo.getAuthorName())) {
                    createLikeNotification(username, photo.getAuthorName());
                }
            }
        }
    }
    
    private void createLikeNotification(String fromUsername, String toUsername) {
        // Create and save notification to Firestore
        Notification notification = new Notification("like", fromUsername, toUsername);
        
        // Log notification data before saving
        Log.d(TAG, "Creating like notification: from=" + notification.getFromUser() + 
              ", to=" + notification.getToUser() + 
              ", timestamp=" + notification.getTimestamp());
        
        firestore.collection("notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Notification created with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating notification", e);
            });
    }
    
    @Override
    public void onDeleteClick(UserPhoto photo, int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !photo.isAuthor(currentUser.getUid())) {
            Toast.makeText(this, "Only the author can delete the photo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // First check if this is a Firestore photo
                if (photo.hasFirestoreId()) {
                    // Delete from Firestore first
                    deletePhotoFromFirestore(photo, position);
                } else {
                    // Old approach for legacy photos
                    deletePhotoLegacy(photo, position);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deletePhotoFromFirestore(UserPhoto photo, int position) {
        // Delete the Firestore document
        firestore.collection("photos").document(photo.getFirestoreId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Photo document deleted from Firestore: " + photo.getFirestoreId());
                
                // Now delete from Storage if URL exists
                if (photo.getStorageUrl() != null) {
                    deletePhotoFromStorage(photo, position);
                } else {
                    // No storage URL, just remove from local list
                    deletePhotoFromList(photo, position);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting photo document from Firestore", e);
                Toast.makeText(this, "Failed to delete photo from database", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void deletePhotoLegacy(UserPhoto photo, int position) {
        // Delete from Firebase Storage if URL exists
        if (photo.getStorageUrl() != null) {
            deletePhotoFromStorage(photo, position);
        } else {
            deletePhotoFromList(photo, position);
        }
    }
    
    private void deletePhotoFromStorage(UserPhoto photo, int position) {
        StorageReference photoRef = storage.getReferenceFromUrl(photo.getStorageUrl());
        photoRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                deletePhotoFromList(photo, position);
            } else {
                Toast.makeText(this, "Failed to delete photo from storage", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onEditDescriptionClick(UserPhoto photo, int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !photo.isAuthor(currentUser.getUid())) {
            Toast.makeText(this, "Only the author can edit the description", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show dialog to edit description
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Description");
        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(photo.getDescription());
        builder.setView(input);
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDescription = input.getText().toString();
            photo.setDescription(newDescription);
            photoAdapter.notifyItemChanged(position);
            savePhotos();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
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

    private void uploadPhotoToFirebase(Uri imageUri, String description, OnPhotoUploadListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to upload photos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        try {
            // Generate a unique photo ID
            String photoId = UUID.randomUUID().toString();
            
            // Create a unique filename with user ID to avoid conflicts
            String filename = currentUser.getUid() + "/" + photoId + ".jpg";
            StorageReference photoRef = storage.getReference().child("photos/" + filename);

            Log.d(TAG, "Starting upload to: " + photoRef.getPath());

            // Upload the file
            UploadTask uploadTask = photoRef.putFile(imageUri);
            
            // Add progress listener
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Upload progress: " + progress + "%");
            });

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Upload failed: " + task.getException().getMessage(), task.getException());
                    throw task.getException();
                }
                Log.d(TAG, "Upload completed, getting download URL");
                return photoRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    Log.d(TAG, "Got download URL: " + downloadUri);
                    
                    // Create new photo with Firebase Storage URL
                    UserPhoto newPhoto = new UserPhoto(
                        imageUri.toString(),
                        description,
                        currentUser.getUid(),
                        currentUser.getDisplayName()
                    );
                    newPhoto.setStorageUrl(downloadUri.toString());
                    
                    // Create a Firestore document for the photo
                    savePhotoToFirestore(photoId, newPhoto, downloadUri.toString());
                    
                    // Save to local storage and update UI
                    userPhotoList.add(newPhoto);
                    photoAdapter.notifyItemInserted(userPhotoList.size() - 1);
                    savePhotos();
                    
                    if (listener != null) {
                        listener.onPhotoUploaded(newPhoto);
                    }
                    
                    Toast.makeText(this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Exception e = task.getException();
                    Log.e(TAG, "Upload failed", e);
                    String errorMessage = "Upload failed: ";
                    if (e != null) {
                        errorMessage += e.getMessage();
                        if (e.getCause() != null) {
                            errorMessage += " (" + e.getCause().getMessage() + ")";
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            Toast.makeText(this, "Error preparing upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void savePhotoToFirestore(String photoId, UserPhoto photo, String storageUrl) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        
        // Create a new photo document in Firestore
        java.util.Map<String, Object> photoData = new java.util.HashMap<>();
        photoData.put("description", photo.getDescription());
        photoData.put("storageUrl", storageUrl);
        photoData.put("uploadedBy", currentUser.getDisplayName());
        photoData.put("authorId", currentUser.getUid());
        photoData.put("timestamp", System.currentTimeMillis());
        photoData.put("likeCount", 0);
        photoData.put("likedBy", new ArrayList<String>());
        
        // Save to Firestore
        firestore.collection("photos")
            .document(photoId)
            .set(photoData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Photo document added to Firestore with ID: " + photoId);
                // Set the Firestore document ID in the UserPhoto object
                photo.setFirestoreId(photoId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding photo document to Firestore", e);
            });
    }

    // Interface for photo upload callback
    private interface OnPhotoUploadListener {
        void onPhotoUploaded(UserPhoto photo);
    }

    // Update gallery photo handling
    private void handleGalleryPhoto(Uri selectedImageUri) {
        try {
            // Show description input dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Description");
            
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            
            builder.setPositiveButton("Upload", (dialog, which) -> {
                String description = input.getText().toString();
                uploadPhotoToFirebase(selectedImageUri, description, null);
            });
            
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error handling gallery photo: " + e.getMessage());
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    // Update camera photo handling
    private void handleCameraPhoto() {
        if (currentPhotoUri != null) {
            try {
                // Show description input dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Add Description");
                
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                
                builder.setPositiveButton("Upload", (dialog, which) -> {
                    String description = input.getText().toString();
                    uploadPhotoToFirebase(currentPhotoUri, description, null);
                });
                
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                
                builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error handling camera photo: " + e.getMessage());
                Toast.makeText(this, "Failed to process camera image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deletePhotoFromList(UserPhoto photo, int position) {
        // Remove the photo from the list
        userPhotoList.remove(position);
        photoAdapter.notifyItemRemoved(position);
        
        // Save the updated list
        savePhotos();
        
        Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
    }

    // Update gallery launcher to use new photo handling
    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        handleGalleryPhoto(selectedImageUri);
                    }
                }
            });
    }

    // Update camera launcher to use new photo handling
    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && currentPhotoUri != null) {
                    handleCameraPhoto();
                }
            });
    }

    private void setupNotificationCounter(TextView countView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            // Query for unread notifications
            firestore.collection("notifications")
                .whereEqualTo("toUser", user.getDisplayName())
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for notifications.", e);
                        return;
                    }
                    
                    if (snapshot != null && !snapshot.isEmpty()) {
                        int count = snapshot.size();
                        countView.setVisibility(View.VISIBLE);
                        countView.setText(String.valueOf(count));
                    } else {
                        countView.setVisibility(View.GONE);
                    }
                });
        }
    }
}