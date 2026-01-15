package com.example.appointable;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

public class UserSearchAdapter extends ArrayAdapter<TeacherMessageModel> {

    private final LayoutInflater inflater;

    public UserSearchAdapter(@NonNull Context context,
                             @NonNull List<TeacherMessageModel> users) {
        super(context, 0, users);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_user_suggestion, parent, false);
        }

        TeacherMessageModel user = getItem(position);
        if (user == null) return convertView;

        ImageView imgAvatar = convertView.findViewById(R.id.imgSuggestionAvatar);
        TextView tvName = convertView.findViewById(R.id.tvSuggestionName);

        tvName.setText(user.getName());

        String url = user.getProfileImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(imgAvatar.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_profile);
        }

        return convertView;
    }
}
