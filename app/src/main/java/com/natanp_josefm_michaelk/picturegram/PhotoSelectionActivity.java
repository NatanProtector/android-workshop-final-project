package com.natanp_josefm_michaelk.picturegram;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PhotoSelectionActivity extends AppCompatActivity {

    private GridView photoGridView;
    private Button uploadSelectedButton;
    private Spinner categorySpinner;
    private PhotoSelectionAdapter adapter;
    private List<Integer> availablePhotos;
    private List<Integer> selectedPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_selection);

        photoGridView = findViewById(R.id.photoGridView);
        uploadSelectedButton = findViewById(R.id.uploadSelectedButton);
        categorySpinner = findViewById(R.id.categorySpinner);
        
        availablePhotos = new ArrayList<>();
        selectedPhotos = new ArrayList<>();
        
        // Setup category spinner
        setupCategorySpinner();
        
        // Load sample photos from drawables
        populatePhotoList("my_img"); // Default category
        
        adapter = new PhotoSelectionAdapter(this, availablePhotos, selectedPhotos);
        photoGridView.setAdapter(adapter);
        
        uploadSelectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPhotos.isEmpty()) {
                    Toast.makeText(PhotoSelectionActivity.this, 
                            "Please select at least one photo", Toast.LENGTH_SHORT).show();
                } else {
                    // Return selected photos to ProfileActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putIntegerArrayListExtra("SELECTED_PHOTOS", 
                            new ArrayList<>(selectedPhotos));
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        });
    }
    
    private void setupCategorySpinner() {
        // Create an array of cat categories
        String[] categories = {"Default Images", "Cute Cats", "House Cats", "Pet Cats", "Domestic Cats", "Ragdoll Cats"};
        
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        
        // Specify the layout to use when the list of choices appears
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Apply the adapter to the spinner
        categorySpinner.setAdapter(spinnerAdapter);
        
        // Set a listener to handle category selection
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Clear current photos and selected photos
                availablePhotos.clear();
                selectedPhotos.clear();
                
                // Load photos based on selected category
                switch (position) {
                    case 0: // Default Images
                        populatePhotoList("my_img");
                        break;
                    case 1: // Cute Cats
                        populateDrawablesByPrefix("cute_cat", "Image_");
                        break;
                    case 2: // House Cats
                        populateDrawablesByPrefix("house_cat", "Image_");
                        break;
                    case 3: // Pet Cats
                        populateDrawablesByPrefix("pet_cat", "Image_");
                        break;
                    case 4: // Domestic Cats
                        populateDrawablesByPrefix("domestic_cat", "image");
                        break;
                    case 5: // Ragdoll Cats
                        populateDrawablesByPrefix("ragdoll_cat", "Image_");
                        break;
                }
                
                // Notify adapter about the data change
                adapter.notifyDataSetChanged();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void populatePhotoList(String prefix) {
        // Add default drawable images
        Field[] drawables = R.drawable.class.getFields();
        
        for (Field field : drawables) {
            try {
                String name = field.getName();
                if (name.startsWith(prefix)) {
                    int resourceId = field.getInt(null);
                    availablePhotos.add(resourceId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // If no photos were found, add some defaults
        if (availablePhotos.isEmpty()) {
            availablePhotos.add(R.drawable.my_img1);
            availablePhotos.add(R.drawable.my_img2);
            availablePhotos.add(R.drawable.my_img3);
            availablePhotos.add(R.drawable.my_img4);
        }
    }
    
    private void populateDrawablesByPrefix(String folder, String prefix) {
        // This method is a simplified approach. In a real app, you would query file system
        // or use a database to get the images from specific folders.
        // For this example, we'll just add the default images since we can't 
        // dynamically enumerate resources by folder path through reflection.
        
        // Add the default images
        availablePhotos.add(R.drawable.my_img1);
        availablePhotos.add(R.drawable.my_img2);
        availablePhotos.add(R.drawable.my_img3);
        availablePhotos.add(R.drawable.my_img4);
        
        // Show a message about the limitation
        Toast.makeText(this, "In a real app, this would load images from the " + 
                folder + " folder with prefix " + prefix, Toast.LENGTH_SHORT).show();
    }
} 