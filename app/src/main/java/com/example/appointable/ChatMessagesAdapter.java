package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_INCOMING = 0;
    private static final int VIEW_TYPE_OUTGOING = 1;
    private static final int VIEW_TYPE_DATE_HEADER = 2;

    private final List<ChatMessageModel> messages;

    private String currentUserImageUrl;
    private String otherUserImageUrl;

    public ChatMessagesAdapter(List<ChatMessageModel> messages) {
        this.messages = messages;
    }

    public void setCurrentUserImageUrl(String url) {
        this.currentUserImageUrl = url;
        notifyDataSetChanged();
    }

    public void setOtherUserImageUrl(String url) {
        this.otherUserImageUrl = url;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageModel msg = messages.get(position);
        if (msg.isDateHeader()) {
            return VIEW_TYPE_DATE_HEADER;
        }
        return msg.isSender() ? VIEW_TYPE_OUTGOING : VIEW_TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_OUTGOING) {
            View view = inflater.inflate(R.layout.item_message_outgoing, parent, false);
            return new OutgoingViewHolder(view);

        } else if (viewType == VIEW_TYPE_INCOMING) {
            View view = inflater.inflate(R.layout.item_message_incoming, parent, false);
            return new IncomingViewHolder(view);

        } else { // DATE HEADER
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        ChatMessageModel msg = messages.get(position);

        if (holder instanceof OutgoingViewHolder) {
            OutgoingViewHolder h = (OutgoingViewHolder) holder;

            h.tvMessage.setText(msg.getMessage());
            h.tvTime.setText(msg.getTime());

            if (currentUserImageUrl != null && !currentUserImageUrl.isEmpty()) {
                Glide.with(h.imgAvatar.getContext())
                        .load(currentUserImageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_profile);
            }

        } else if (holder instanceof IncomingViewHolder) {
            IncomingViewHolder h = (IncomingViewHolder) holder;

            h.tvMessage.setText(msg.getMessage());
            h.tvTime.setText(msg.getTime());

            if (otherUserImageUrl != null && !otherUserImageUrl.isEmpty()) {
                Glide.with(h.imgAvatar.getContext())
                        .load(otherUserImageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_profile);
            }

        } else if (holder instanceof DateHeaderViewHolder) {
            DateHeaderViewHolder h = (DateHeaderViewHolder) holder;
            h.tvHeader.setText(msg.getDateLabel());

            // simple fade-in animation for date headers
            h.itemView.setAlpha(0f);
            h.itemView.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView imgAvatar;

        public IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvIncomingMessage);
            tvTime = itemView.findViewById(R.id.tvIncomingTime);
            imgAvatar = itemView.findViewById(R.id.imgIncomingAvatar);
        }
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView imgAvatar;

        public OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvOutgoingMessage);
            tvTime = itemView.findViewById(R.id.tvOutgoingTime);
            imgAvatar = itemView.findViewById(R.id.imgOutgoingAvatar);
        }
    }

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvDateHeader);
        }
    }
}
