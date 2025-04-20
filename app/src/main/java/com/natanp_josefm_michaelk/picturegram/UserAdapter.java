package com.natanp_josefm_michaelk.picturegram;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;

    // Constructor
    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate your item_user.xml to create View objects
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        // Populate each row with data
        User user = userList.get(position);
        holder.userNameText.setText(user.getName());
        holder.userImageView.setImageResource(user.getImageResourceId());

        // 1) Make sure your root layout in item_user.xml has:
        //    android:clickable="true"
        //    android:focusable="true"
        //    android:foreground="?attr/selectableItemBackground"
        // 2) Handle the click on the entire row:
        holder.itemView.setOnClickListener(v -> {
            // Do something, e.g. show a toast or open another activity
            Toast.makeText(
                    holder.itemView.getContext(),
                    "Clicked on: " + user.getName(),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // "ViewHolder" class that holds reference to each row's views
    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userImageView;
        TextView userNameText;

        UserViewHolder(View itemView) {
            super(itemView);
            userImageView = itemView.findViewById(R.id.userImageView);
            userNameText  = itemView.findViewById(R.id.userNameText);
        }
    }
}
