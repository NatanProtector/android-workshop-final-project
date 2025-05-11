package com.natanp_josefm_michaelk.picturegram;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.Group;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private RecyclerView    recyclerView;
    private UserAdapter     adapter;
    private List<User>      userList;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep your EdgeToEdge logic if you like
        EdgeToEdge.enable(this);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        
        // Apply theme based on saved preference
        boolean isDarkMode = sharedPreferences.getBoolean("darkMode", false);
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        setContentView(R.layout.activity_users);

        // 1) Auth check
        TextView notAuth           = findViewById(R.id.notAuthenticatedTextView);
        Group   contentGroup       = findViewById(R.id.usersContentGroup);
        FirebaseUser currentUser   = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            notAuth.setVisibility(View.VISIBLE);
            contentGroup.setVisibility(View.GONE);
            return;
        } else {
            notAuth.setVisibility(View.GONE);
            contentGroup.setVisibility(View.VISIBLE);
        }

        // 2) Insets (optional)
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                }
        );

        // 3) RecyclerView setup
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter  = new UserAdapter(userList);
        recyclerView.setAdapter(adapter);

        // 4) Firestore fetch
        db = FirebaseFirestore.getInstance();
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    userList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String username = doc.getString("username");
                        // use default launcher icon for now
                        userList.add(new User(username, R.mipmap.ic_launcher));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.w("UsersActivity", "Error loading users", e));
    }
}
