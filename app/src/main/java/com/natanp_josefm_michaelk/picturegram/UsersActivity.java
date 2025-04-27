package com.natanp_josefm_michaelk.picturegram;


import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.natanp_josefm_michaelk.picturegram.R;
import com.natanp_josefm_michaelk.picturegram.User;
import com.natanp_josefm_michaelk.picturegram.UserAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    // 1) References
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep your existing EdgeToEdge code (if needed)
        EdgeToEdge.enable(/* ThisEnableEdgeToEdge: */ this);

        // 2) Inflate the layout that contains the RecyclerView
        setContentView(R.layout.activity_users);
        
        TextView notAuthenticatedTextView = findViewById(R.id.notAuthenticatedTextView);
        androidx.constraintlayout.widget.Group usersContentGroup = findViewById(R.id.usersContentGroup);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is NOT signed in
            notAuthenticatedTextView.setVisibility(View.VISIBLE);
            usersContentGroup.setVisibility(View.GONE);
            // Optional: Disable EdgeToEdge or other UI setup if needed
            // EdgeToEdge.disable(this);
            return; // Stop further setup if not authenticated
        } else {
            // User is signed in
            notAuthenticatedTextView.setVisibility(View.GONE);
            usersContentGroup.setVisibility(View.VISIBLE);
        }

        // If you have code for handling window insets:
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 3) Get the RecyclerView from the layout
        recyclerView = findViewById(R.id.recyclerView);

        // 4) Set a LayoutManager (LinearLayoutManager for a vertical list)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 5) Create some sample data (User objects)
        userList = new ArrayList<>();
        
        // Create users with sample photos
        User alice = new User("Alice", R.drawable.my_img1);
        addSamplePhotosToUser(alice);
        
        User bob = new User("Bob", R.drawable.my_img2);
        addSamplePhotosToUser(bob);
        
        User charlie = new User("Charlie", R.drawable.my_img3);
        addSamplePhotosToUser(charlie);
        
        User david = new User("David", R.drawable.my_img4);
        addSamplePhotosToUser(david);
        
        // Add users to the list
        userList.add(alice);
        userList.add(bob);
        userList.add(charlie);
        userList.add(david);
        userList.add(new User("Eve", R.mipmap.ic_launcher));
        userList.add(new User("Frank", R.mipmap.ic_launcher));
        userList.add(new User("Grace", R.mipmap.ic_launcher));
        userList.add(new User("Heidi", R.mipmap.ic_launcher));
        userList.add(new User("Ivan", R.mipmap.ic_launcher));

        // 6) Create the adapter and attach it to the RecyclerView
        adapter = new UserAdapter(userList);
        recyclerView.setAdapter(adapter);
    }
    
    private void addSamplePhotosToUser(User user) {
        // Add some sample photos to the user's gallery
        user.addPhoto(new UserPhoto(R.drawable.my_img1, "First cat photo"));
        user.addPhoto(new UserPhoto(R.drawable.my_img2, "Second cat photo"));
        user.addPhoto(new UserPhoto(R.drawable.my_img3, "Third cat photo"));
        user.addPhoto(new UserPhoto(R.drawable.my_img4, "Fourth cat photo"));
        
        // In a real app, you would load these from a database or storage
    }
}
