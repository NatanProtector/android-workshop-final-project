
package com.natanp_josefm_michaelk.picturegram;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
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

    private TextView      notAuthTv;
    private Group         contentGroup;
    private EditText      searchEt;
    private RecyclerView  recyclerView;

    private List<User>    allUsers       = new ArrayList<>();
    private List<User>    displayedUsers = new ArrayList<>();
    private UserAdapter   adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1) Edge-to-Edge support
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                }
        );

        setContentView(R.layout.activity_users);

        // 2) bind views
        notAuthTv    = findViewById(R.id.notAuthenticatedTextView);
        contentGroup = findViewById(R.id.usersContentGroup);
        searchEt     = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.recyclerView);

        // 3) auth-guard
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) {
            notAuthTv.setVisibility(TextView.VISIBLE);
            contentGroup.setVisibility(Group.GONE);
            return;
        } else {
            notAuthTv.setVisibility(TextView.GONE);
            contentGroup.setVisibility(Group.VISIBLE);
        }

        // 4) RecyclerView + adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(displayedUsers);
        recyclerView.setAdapter(adapter);

        // 5) Firestore init & load all users
        db = FirebaseFirestore.getInstance();
        final String currentUserId = me.getUid();
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String userId   = doc.getId();
                        if (userId.equals(currentUserId)) continue;  // skip yourself
                        String username = doc.getString("username");
                        String picUrl   = doc.getString("profilePictureUrl");
                        allUsers.add(new User(userId, username, picUrl));
                    }
                    filterUsers("");
                })
                .addOnFailureListener(e -> Log.w("UsersActivity", "load failed", e));

        // 6) filter as you type
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            @Override public void afterTextChanged(Editable e) {}
            @Override
            public void onTextChanged(CharSequence s,int st,int b,int c) {
                filterUsers(s.toString());
            }
        });
    }

    /** Filters the master allUsers list into displayedUsers, then refreshes adapter */
    private void filterUsers(String query) {
        displayedUsers.clear();
        if (query == null || query.trim().isEmpty()) {
            displayedUsers.addAll(allUsers);
        } else {
            String lower = query.toLowerCase().trim();
            for (User u : allUsers) {
                if (u.getName().toLowerCase().contains(lower)) {
                    displayedUsers.add(u);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}

