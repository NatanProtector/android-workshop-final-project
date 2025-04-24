package com.natanp_josefm_michaelk.picturegram;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.util.List;

public class PhotoSelectionAdapter extends BaseAdapter {
    
    private Context context;
    private List<Integer> photoList;
    private List<Integer> selectedPhotos;
    private LayoutInflater inflater;
    
    public PhotoSelectionAdapter(Context context, List<Integer> photoList, List<Integer> selectedPhotos) {
        this.context = context;
        this.photoList = photoList;
        this.selectedPhotos = selectedPhotos;
        this.inflater = LayoutInflater.from(context);
    }
    
    @Override
    public int getCount() {
        return photoList.size();
    }
    
    @Override
    public Object getItem(int position) {
        return photoList.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_photo_selection, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.selectionImageView);
            holder.checkBox = convertView.findViewById(R.id.selectionCheckBox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        final Integer photoResId = photoList.get(position);
        
        // Set image resource
        holder.imageView.setImageResource(photoResId);
        
        // Set checkbox state
        holder.checkBox.setChecked(selectedPhotos.contains(photoResId));
        
        // Handle click actions
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPhotos.contains(photoResId)) {
                    selectedPhotos.remove(photoResId);
                    holder.checkBox.setChecked(false);
                } else {
                    selectedPhotos.add(photoResId);
                    holder.checkBox.setChecked(true);
                }
            }
        });
        
        return convertView;
    }
    
    private static class ViewHolder {
        ImageView imageView;
        CheckBox checkBox;
    }
} 