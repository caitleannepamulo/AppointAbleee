package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private final List<Appointment> appointmentList;
    private final OnAppointmentActionListener listener;

    private final Map<String, Boolean> expandedMap = new HashMap<>();

    public interface OnAppointmentActionListener {
        void onCancel(Appointment appt);
        void onReschedule(Appointment appt);
        void onMoreOptions(Appointment appt, View anchor);
    }

    public AppointmentAdapter(List<Appointment> appointmentList, OnAppointmentActionListener listener) {
        this.appointmentList = appointmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {

        Appointment appt = appointmentList.get(position);

        String teacherName = safe(appt.getTeacherName());
        String service = safe(appt.getService());
        String date = safe(appt.getDate());
        String time = safe(appt.getTime());
        String status = safe(appt.getStatus());
        String comment = safe(appt.getRescheduleComment());

        holder.tvTeacherName.setText(teacherName.isEmpty() ? "—" : teacherName);
        holder.tvService.setText(service.isEmpty() ? "—" : service);
        holder.tvDate.setText(formatDisplayDate(date));
        holder.tvTime.setText(time.isEmpty() ? "—" : time);
        holder.tvStatus.setText(status.isEmpty() ? "—" : status);

        if (listener != null) {
            holder.btnOptions.setVisibility(View.VISIBLE);
            holder.btnOptions.setOnClickListener(v -> listener.onMoreOptions(appt, v));
        } else {
            holder.btnOptions.setVisibility(View.GONE);
        }

        holder.tvComment.setText("Comment: " + (comment.isEmpty() ? "None" : comment));

        String baseId = safe(appt.getId());
        if (baseId.isEmpty()) {
            baseId = String.valueOf((teacherName + "|" + service + "|" + date + "|" + time).hashCode());
        }

        final String keyId = baseId; // ✅ effectively final for lambdas

        boolean expanded = expandedMap.getOrDefault(keyId, false);
        boolean isRescheduled = status.equalsIgnoreCase("Rescheduled");

        if (isRescheduled) {

            holder.imgArrow.setVisibility(View.VISIBLE);

            holder.layoutDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
            holder.imgArrow.setRotation(expanded ? 180f : 0f);

            View.OnClickListener toggle = v -> {
                boolean newState = !expandedMap.getOrDefault(keyId, false);
                expandedMap.put(keyId, newState);
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
            };

            holder.layoutRoot.setOnClickListener(toggle);
            holder.imgArrow.setOnClickListener(toggle);

        } else {

            holder.imgArrow.setVisibility(View.GONE);
            holder.layoutDetails.setVisibility(View.GONE);
            expandedMap.put(keyId, false);

            holder.layoutRoot.setOnClickListener(null);
            holder.imgArrow.setOnClickListener(null);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String formatDisplayDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "—";

        SimpleDateFormat input = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
        input.setLenient(false);

        SimpleDateFormat output = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

        try {
            Date date = input.parse(dateStr.trim());
            if (date == null) return dateStr;
            return output.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        LinearLayout layoutRoot, layoutDetails;
        TextView tvTeacherName, tvService, tvDate, tvTime, tvStatus, tvComment;
        ImageView btnOptions, imgArrow;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutRoot = itemView.findViewById(R.id.layoutRoot);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);

            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            tvService = itemView.findViewById(R.id.tvService);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvComment = itemView.findViewById(R.id.tvComment);

            btnOptions = itemView.findViewById(R.id.btnOptions);
            imgArrow = itemView.findViewById(R.id.imgArrow);
        }
    }
}
