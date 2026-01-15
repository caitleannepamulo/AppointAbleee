package com.example.appointable;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class TeacherMessagesAdapter extends RecyclerView.Adapter<TeacherMessagesAdapter.ViewHolder> {

    public interface OnMessageClickListener {
        void onMessageClick(TeacherMessageModel model);
    }

    private final List<TeacherMessageModel> messageList;
    private final OnMessageClickListener listener;

    public TeacherMessagesAdapter(List<TeacherMessageModel> messageList,
                                  OnMessageClickListener listener) {
        this.messageList = messageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TeacherMessagesAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TeacherMessagesAdapter.ViewHolder holder, int position) {

        TeacherMessageModel model = messageList.get(position);

        holder.tvName.setText(model.getName());
        holder.tvLastMessage.setText(model.getLastMessage());
        holder.tvTime.setText(model.getTime());

        String url = model.getProfileImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(holder.imgProfile.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_profile);
        }

        int unread = model.getUnreadCount();

        if (unread > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(unread));

            holder.tvName.setTypeface(holder.tvName.getTypeface(), Typeface.BOLD);
            holder.tvLastMessage.setTypeface(holder.tvLastMessage.getTypeface(), Typeface.BOLD);
            holder.tvTime.setTypeface(holder.tvTime.getTypeface(), Typeface.BOLD);
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);

            holder.tvName.setTypeface(holder.tvName.getTypeface(), Typeface.NORMAL);
            holder.tvLastMessage.setTypeface(holder.tvLastMessage.getTypeface(), Typeface.NORMAL);
            holder.tvTime.setTypeface(holder.tvTime.getTypeface(), Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProfile;
        TextView tvName, tvLastMessage, tvTime, tvUnreadBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}
