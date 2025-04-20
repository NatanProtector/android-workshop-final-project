package com.natanp_josefm_michaelk.picturegram;


import android.os.Bundle;

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
        userList.add(new User("Alice", R.drawable.my_img1));
        userList.add(new User("Bob", R.drawable.my_img2));
        userList.add(new User("Charlie", R.drawable.my_img3));
        userList.add(new User("Alice", R.drawable.my_img4));
        userList.add(new User("Bob", R.mipmap.ic_launcher));
        userList.add(new User("Charlie", R.mipmap.ic_launcher));
        userList.add(new User("Alice", R.mipmap.ic_launcher));
        userList.add(new User("Bob", R.mipmap.ic_launcher));
        userList.add(new User("Charlie", R.mipmap.ic_launcher));
        userList.add(new User("Alice", R.mipmap.ic_launcher));
        userList.add(new User("Bob", R.mipmap.ic_launcher));
        userList.add(new User("Charlie", R.mipmap.ic_launcher));

        // 6) Create the adapter and attach it to the RecyclerView
        adapter = new UserAdapter(userList);
        recyclerView.setAdapter(adapter);
    }
}
