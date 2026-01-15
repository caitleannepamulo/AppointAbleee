package com.example.appointable;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final List<ScheduleModel> list;

    public static final int STATUS_ACCEPTED = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_CANCELED = 2;

    public interface StatusChangeListener {
        void onStatusChanged();
    }

    private final StatusChangeListener statusChangeListener;

    public ScheduleAdapter(List<ScheduleModel> list,
                           StatusChangeListener statusChangeListener) {
        this.list = list;
        this.statusChangeListener = statusChangeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleModel model = list.get(position);

        holder.tvChildName.setText(model.getChildName());
        holder.tvService.setText(model.getService());
        holder.tvTime.setText(model.getTime());
        holder.tvDate.setText(formatDate(model.getDateString()));

        if (model.getStatus() == STATUS_COMPLETED) {
            holder.ivCalendar.setImageResource(R.drawable.ic_calendar_complete);
            holder.ivCalendar.setAlpha(0.4f);
        } else if (model.getStatus() == STATUS_CANCELED) {
            holder.ivCalendar.setImageResource(R.drawable.ic_calendar_cancelled);
            holder.ivCalendar.setAlpha(0.4f);
        } else {
            holder.ivCalendar.setImageResource(R.drawable.ic_calendar_pending);
            holder.ivCalendar.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> showAppointmentDetails(v, model));

        holder.ivCalendar.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            ScheduleModel clickedModel = list.get(adapterPos);

            if (clickedModel.getStatus() == STATUS_COMPLETED) {
                Toast.makeText(v.getContext(),
                        "This appointment is already completed.",
                        Toast.LENGTH_SHORT).show();
                showAppointmentDetails(v, clickedModel);
                return;
            }

            if (clickedModel.getStatus() == STATUS_CANCELED) {
                Toast.makeText(v.getContext(),
                        "This appointment is already canceled.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            showCompletedDialog(v, clickedModel, adapterPos);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void showAppointmentDetails(View v, ScheduleModel model) {
        String message =
                "Service: " + model.getService() +
                        "\nTime: " + model.getTime() +
                        "\nDate: " + formatDate(model.getDateString());

        if (model.getStatus() == STATUS_COMPLETED) {
            String docId = model.getDocumentId();
            if (docId == null || docId.isEmpty()) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(model.getChildName())
                        .setMessage(message + "\n\nSession feedback: (No feedback found)")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("appointments")
                    .document(docId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String feedback = "";
                        if (snapshot != null && snapshot.exists()) {
                            feedback = snapshot.getString("sessionFeedback");
                        }
                        if (feedback == null || feedback.trim().isEmpty()) {
                            feedback = "(No feedback saved)";
                        }

                        new AlertDialog.Builder(v.getContext())
                                .setTitle(model.getChildName())
                                .setMessage(message + "\n\nSession feedback:\n" + feedback)
                                .setPositiveButton("OK", null)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle(model.getChildName())
                                .setMessage(message + "\n\nSession feedback: (Failed to load)")
                                .setPositiveButton("OK", null)
                                .show();
                    });

            return;
        }

        new AlertDialog.Builder(v.getContext())
                .setTitle(model.getChildName())
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showCompletedDialog(View v, ScheduleModel model, int position) {
        new AlertDialog.Builder(v.getContext())
                .setTitle("Mark appointment")
                .setMessage("Do you confirm that this session is completed?")
                .setNegativeButton("No", (dialog, which) -> {
                    showCanceledCheckDialog(v, model, position);
                })
                .setPositiveButton("Yes", (dialog, which) -> {
                    showFeedbackDialog(v, model, position);
                })
                .show();
    }

    private void showFeedbackDialog(View v, ScheduleModel model, int position) {
        View dialogView = LayoutInflater.from(v.getContext())
                .inflate(R.layout.dialog_session_feedback, null, false);

        EditText etFeedback = dialogView.findViewById(R.id.etSessionFeedback);

        AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSave.setEnabled(false);

            etFeedback.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s == null ? "" : s.toString().trim();
                    btnSave.setEnabled(!text.isEmpty());
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            btnSave.setOnClickListener(view -> {
                String feedback = etFeedback.getText() == null ? "" : etFeedback.getText().toString().trim();

                if (feedback.isEmpty()) {
                    etFeedback.setError("Feedback is required.");
                    etFeedback.requestFocus();
                    return;
                }

                completeAppointment(v, model, position, feedback);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void completeAppointment(View v, ScheduleModel model, int position, String feedback) {
        model.setStatus(STATUS_COMPLETED);
        notifyItemChanged(position);
        runFadeAnimation(v);

        if (statusChangeListener != null) {
            statusChangeListener.onStatusChanged();
        }

        updateCompletedInFirestore(model, feedback, v);

        Toast.makeText(v.getContext(),
                "Marked as completed.",
                Toast.LENGTH_SHORT).show();
    }

    private void showCanceledCheckDialog(View v, ScheduleModel model, int position) {
        new AlertDialog.Builder(v.getContext())
                .setTitle("Cancel appointment")
                .setMessage("Does that mean the appointment is canceled?")
                .setNegativeButton("No", (dialog, which) -> {
                    showCompletedDialog(v, model, position);
                })
                .setPositiveButton("Yes", (dialog, which) -> {
                    model.setStatus(STATUS_CANCELED);
                    notifyItemChanged(position);
                    runFadeAnimation(v);

                    if (statusChangeListener != null) {
                        statusChangeListener.onStatusChanged();
                    }

                    updateStatusInFirestore(
                            model,
                            "Canceled",
                            "Marked as canceled / not attended.",
                            v
                    );

                    Toast.makeText(v.getContext(),
                            "Marked as canceled / not attended.",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void updateCompletedInFirestore(ScheduleModel model, String feedback, View v) {
        String docId = model.getDocumentId();
        if (docId == null || docId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(docId)
                .update(
                        "status", "Completed",
                        "sessionFeedback", feedback
                )
                .addOnFailureListener(e -> {
                    Toast.makeText(v.getContext(),
                            "Failed to update in server.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStatusInFirestore(ScheduleModel model,
                                         String statusString,
                                         String reasonString,
                                         View v) {
        String docId = model.getDocumentId();
        if (docId == null || docId.isEmpty()) return;

        if (reasonString != null) {
            FirebaseFirestore.getInstance()
                    .collection("appointments")
                    .document(docId)
                    .update(
                            "status", statusString,
                            "reason", reasonString
                    )
                    .addOnFailureListener(e -> {
                        Toast.makeText(v.getContext(),
                                "Failed to update status in server.",
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            FirebaseFirestore.getInstance()
                    .collection("appointments")
                    .document(docId)
                    .update("status", statusString)
                    .addOnFailureListener(e -> {
                        Toast.makeText(v.getContext(),
                                "Failed to update status in server.",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void runFadeAnimation(View target) {
        if (target == null) return;
        target.setAlpha(0f);
        target.animate()
                .alpha(1f)
                .setDuration(250)
                .start();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            // ✅ matches Firestore: 1/16/2026
            SimpleDateFormat input = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
            input.setLenient(false);

            SimpleDateFormat output = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            Date date = input.parse(rawDate);
            if (date == null) return rawDate;
            return output.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return rawDate;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName, tvService, tvTime, tvDate;
        ImageView ivCalendar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvService = itemView.findViewById(R.id.tvService);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivCalendar = itemView.findViewById(R.id.ivCalendar);
        }
    }
}
