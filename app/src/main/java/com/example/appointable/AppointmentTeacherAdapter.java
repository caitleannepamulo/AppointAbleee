package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppointmentTeacherAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_APPOINTMENT = 0;
    private static final int VIEW_TYPE_EMPTY = 1;

    private List<AppointmentTeacher> items;

    public AppointmentTeacherAdapter(List<AppointmentTeacher> items) {
        this.items = items;
    }

    public void updateList(List<AppointmentTeacher> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // If there are no appointments, show the "No Scheduled AppointmentTeacher" row
        if (items == null || items.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return VIEW_TYPE_APPOINTMENT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_EMPTY) {
            View view = inflater.inflate(R.layout.item_appointment_empty, parent, false);
            return new EmptyViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_appointment_teacher, parent, false);
            return new AppointmentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {

        if (holder instanceof AppointmentViewHolder) {
            if (items == null || items.isEmpty()) return;

            AppointmentTeacher item = items.get(position);
            AppointmentViewHolder vh = (AppointmentViewHolder) holder;

            vh.tvChildName.setText(item.getChildName());
            vh.tvService.setText(item.getService());
            vh.tvTime.setText(item.getTime());
            vh.tvStatus.setText(item.getStatus());
        } else if (holder instanceof EmptyViewHolder) {
            // nothing extra to bind, static text is already in XML
        }
    }

    @Override
    public int getItemCount() {
        // When empty, we still return 1 item so the empty row shows
        if (items == null || items.isEmpty()) {
            return 1;
        } else {
            return items.size();
        }
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName, tvService, tvTime, tvStatus;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvService = itemView.findViewById(R.id.tvService);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoAppointment;

        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoAppointment = itemView.findViewById(R.id.tvNoAppointment);
        }
    }
}
