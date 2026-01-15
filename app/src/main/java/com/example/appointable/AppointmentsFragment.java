package com.example.appointable;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppointmentsFragment extends Fragment {

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private final List<Appointment> appointmentList = new ArrayList<>();
    private final List<Appointment> allAppointments = new ArrayList<>();

    private String currentStatusFilter = "Pending";

    private MaterialCardView layoutCanceled, layoutRescheduled, layoutPending, layoutCompleted;
    private TextView tvCanceledCount, tvRescheduledCount, tvCompletedCount, tvPendingCount;

    public AppointmentsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.activity_parent_appointment_fragment, container, false);

        initViews(root);
        initRecycler();
        loadAppointments();

        return root;
    }

    private void initViews(View root) {

        tvCanceledCount    = root.findViewById(R.id.tvCanceledCount);
        tvRescheduledCount = root.findViewById(R.id.tvRescheduledCount);
        tvCompletedCount   = root.findViewById(R.id.tvCompletedCount);
        tvPendingCount     = root.findViewById(R.id.tvPending);

        rvAppointments = root.findViewById(R.id.rvAppointments);

        layoutCanceled    = root.findViewById(R.id.layoutCanceled);
        layoutRescheduled = root.findViewById(R.id.layoutRescheduled);
        layoutPending     = root.findViewById(R.id.layoutPending);
        layoutCompleted   = root.findViewById(R.id.layoutCompleted);

        layoutPending.setOnClickListener(v -> setStatusFilter("Pending"));
        layoutCanceled.setOnClickListener(v -> setStatusFilter("Canceled"));
        layoutRescheduled.setOnClickListener(v -> setStatusFilter("Rescheduled"));
        layoutCompleted.setOnClickListener(v -> setStatusFilter("Completed"));

        FloatingActionButton fab = root.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddAppointmentDialog());
    }

    private void initRecycler() {
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(appointmentList, new AppointmentAdapter.OnAppointmentActionListener() {
            @Override
            public void onCancel(Appointment appt) {
                showCancelConfirmation(appt);
            }
            @Override
            public void onReschedule(Appointment appt) {
                showRescheduleDialog(appt);
            }
            @Override
            public void onMoreOptions(Appointment appt, View anchor) {
                showPopupOptions(appt, anchor);
            }
        });
        rvAppointments.setAdapter(adapter);
    }

    private void loadAppointments() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("studentId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {

                    allAppointments.clear();
                    appointmentList.clear();

                    int canceled = 0, rescheduled = 0, pending = 0, completed = 0;

                    for (DocumentSnapshot doc : query) {

                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt == null) continue;

                        // Ensure local object has id
                        if (appt.getId() == null || appt.getId().trim().isEmpty()) {
                            appt.setId(doc.getId());
                        }

                        allAppointments.add(appt);

                        String statusRaw = appt.getStatus();
                        String status = statusRaw != null ? statusRaw.toLowerCase() : "";

                        switch (status) {
                            case "canceled":
                            case "cancelled":
                                canceled++;
                                break;
                            case "rescheduled":
                                rescheduled++;
                                break;
                            case "completed":
                                completed++;
                                break;
                            case "pending":
                            case "accepted":
                                pending++;
                                break;
                        }
                    }

                    tvCanceledCount.setText(String.valueOf(canceled));
                    tvRescheduledCount.setText(String.valueOf(rescheduled));
                    tvPendingCount.setText(String.valueOf(pending));
                    tvCompletedCount.setText(String.valueOf(completed));

                    // ✅ IMPORTANT: ensure parent reminders are always scheduled locally
                    syncParentReminders(allAppointments);

                    applyStatusFilter();
                    highlightSelectedStatus();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ✅ This solves "parent side not notifying" by scheduling reminders every time we load data.
    // This way even if teacher accepts/reschedules on their device, the parent device will schedule on next open.
    private void syncParentReminders(List<Appointment> list) {
        if (list == null || list.isEmpty()) return;
        if (!isAdded()) return;

        Set<Integer> activeRequestCodes = new HashSet<>();

        for (Appointment appt : list) {
            if (appt == null) continue;

            String apptId = appt.getId();
            if (apptId == null || apptId.trim().isEmpty()) continue;

            String status = appt.getStatus() == null ? "" : appt.getStatus().trim();

            int requestCode = ("APPT_" + apptId).hashCode();
            activeRequestCodes.add(requestCode);

            // cancel for canceled/completed
            if (status.equalsIgnoreCase("Canceled") ||
                    status.equalsIgnoreCase("Cancelled") ||
                    status.equalsIgnoreCase("Completed")) {

                ReminderScheduler.cancelReminder(requireContext(), requestCode);
                continue;
            }

            // schedule only if date/time valid and future
            long apptMillis = parseAppointmentMillis(appt.getDate(), appt.getTime());
            if (apptMillis <= 0) continue;

            String title = "Appointment Reminder";

            String child = appt.getChildName() == null ? "" : appt.getChildName().trim();
            String service = appt.getService() == null ? "" : appt.getService().trim();
            String time = appt.getTime() == null ? "" : appt.getTime().trim();

            String msg = "Tomorrow: " +
                    (child.isEmpty() ? "Your appointment" : child) +
                    (service.isEmpty() ? "" : " — " + service) +
                    (time.isEmpty() ? "" : " at " + time);

            ReminderScheduler.schedule24HoursBefore(
                    requireContext(),
                    apptMillis,
                    requestCode,
                    title,
                    msg,
                    apptId,
                    "Parent"
            );
        }
    }

    private void setStatusFilter(String status) {
        currentStatusFilter = status;
        applyStatusFilter();
        highlightSelectedStatus();
    }

    private void applyStatusFilter() {
        appointmentList.clear();

        for (Appointment a : allAppointments) {
            String st = a.getStatus();
            if (st == null) continue;

            if ("Pending".equalsIgnoreCase(currentStatusFilter)) {
                if (st.equalsIgnoreCase("Pending") || st.equalsIgnoreCase("Accepted")) {
                    appointmentList.add(a);
                }
            } else {
                if (st.equalsIgnoreCase(currentStatusFilter)) {
                    appointmentList.add(a);
                }
            }
        }

        Collections.sort(appointmentList, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment a1, Appointment a2) {
                Date d1 = parseAppointmentDateTime(a1);
                Date d2 = parseAppointmentDateTime(a2);

                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;

                return d1.compareTo(d2);
            }
        });

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void highlightSelectedStatus() {
        if (layoutPending == null) return;

        layoutPending.setAlpha(1f);
        layoutCanceled.setAlpha(1f);
        layoutRescheduled.setAlpha(1f);
        layoutCompleted.setAlpha(1f);

        switch (currentStatusFilter) {
            case "Pending":
                layoutPending.setAlpha(0.7f);
                break;
            case "Canceled":
                layoutCanceled.setAlpha(0.7f);
                break;
            case "Rescheduled":
                layoutRescheduled.setAlpha(0.7f);
                break;
            case "Completed":
                layoutCompleted.setAlpha(0.7f);
                break;
        }
    }

    private Date parseAppointmentDateTime(Appointment appt) {
        if (appt == null) return null;

        String dateStr = appt.getDate();
        String timeStr = appt.getTime();
        if (dateStr == null || timeStr == null) return null;

        String combined = dateStr + " " + timeStr;

        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault());
        sdf.setLenient(false);
        try {
            return sdf.parse(combined);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private long parseAppointmentMillis(String date, String time) {
        if (date == null || time == null) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault());
            sdf.setLenient(false);
            Date d = sdf.parse(date.trim() + " " + time.trim());
            return d == null ? -1 : d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void showAddAppointmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_add_appointment, null);
        builder.setView(view);

        Spinner spinnerType = view.findViewById(R.id.spinnerType);
        EditText etDate = view.findViewById(R.id.etDate);
        EditText etTime = view.findViewById(R.id.etTime);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveAppointment);

        String[] types = {
                "--Select Appointment Type--",
                "Admin Officer",
                "OT Associates",
                "Sped Teacher",
                "Sped Coordinator",
                "Occupational Therapist"
        };

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                types
        ) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) v;
                tv.setTextColor(position == 0 ? 0xFF9E9E9E : 0xFF000000);
                return v;
            }
        };

        spinnerType.setAdapter(typeAdapter);
        spinnerType.setSelection(0);

        etDate.setOnClickListener(v -> pickDate(etDate));
        etTime.setOnClickListener(v -> pickTime(etTime, etDate));

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {

            int typePos = spinnerType.getSelectedItemPosition();

            if (typePos == 0) {
                Toast.makeText(getContext(), "Please select appointment type", Toast.LENGTH_SHORT).show();
                return;
            }

            String service = spinnerType.getSelectedItem().toString();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            if (date.isEmpty() || time.isEmpty()) {
                Toast.makeText(getContext(), "Pick date & time", Toast.LENGTH_SHORT).show();
                return;
            }

            saveAppointment(service, date, time);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void pickDate(EditText et) {
        Calendar c = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(
                getContext(),
                (picker, y, m, d) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, y);
                    selected.set(Calendar.MONTH, m);
                    selected.set(Calendar.DAY_OF_MONTH, d);
                    selected.set(Calendar.HOUR_OF_DAY, 0);
                    selected.set(Calendar.MINUTE, 0);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    int day = selected.get(Calendar.DAY_OF_WEEK);
                    boolean weekend = (day == Calendar.SATURDAY || day == Calendar.SUNDAY);

                    if (weekend) {
                        Toast.makeText(getContext(),
                                "Weekend dates are not allowed. Please choose a weekday.",
                                Toast.LENGTH_SHORT).show();
                        et.setText("");
                        pickDate(et);
                        return;
                    }

                    et.setText((m + 1) + "/" + d + "/" + y);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dp.show();
    }

    private void pickTime(EditText timeEt, EditText dateEt) {

        String dateStr = dateEt.getText().toString().trim();
        if (dateStr.isEmpty()) {
            Toast.makeText(getContext(), "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
        df.setLenient(false);

        Date selectedDate;
        try {
            selectedDate = df.parse(dateStr);
        } catch (ParseException e) {
            timeEt.setText("");
            Toast.makeText(getContext(), "Invalid date. Please pick again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTime(selectedDate);
        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);

        Calendar nowCal = Calendar.getInstance();
        Calendar todayZero = Calendar.getInstance();
        todayZero.set(Calendar.HOUR_OF_DAY, 0);
        todayZero.set(Calendar.MINUTE, 0);
        todayZero.set(Calendar.SECOND, 0);
        todayZero.set(Calendar.MILLISECOND, 0);

        boolean isToday = selectedCal.getTimeInMillis() == todayZero.getTimeInMillis();

        Calendar c = Calendar.getInstance();

        new TimePickerDialog(
                getContext(),
                (picker, hourOfDay, minute) -> {

                    int minMinutes = 7 * 60;
                    int maxMinutes = 16 * 60;
                    int pickedMinutes = hourOfDay * 60 + minute;

                    if (pickedMinutes < minMinutes || pickedMinutes > maxMinutes) {
                        Toast.makeText(getContext(),
                                "Please select a time between 7:00 AM and 4:00 PM",
                                Toast.LENGTH_SHORT).show();
                        timeEt.setText("");
                        pickTime(timeEt, dateEt);
                        return;
                    }

                    if (isToday) {
                        int nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE);
                        if (pickedMinutes < nowMinutes) {
                            Toast.makeText(getContext(),
                                    "That time has already passed. Choose a later time.",
                                    Toast.LENGTH_SHORT).show();
                            timeEt.setText("");
                            pickTime(timeEt, dateEt);
                            return;
                        }
                    }

                    String ampm = hourOfDay >= 12 ? "PM" : "AM";
                    int hr = (hourOfDay % 12 == 0 ? 12 : hourOfDay % 12);
                    timeEt.setText(hr + ":" + String.format(Locale.getDefault(), "%02d", minute) + " " + ampm);

                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void saveAppointment(String service, String date, String time) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(getContext(), "Pick date & time", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = user.getUid();

        db.collection("users").document(studentId)
                .get()
                .addOnSuccessListener(doc -> {

                    String first = doc.getString("firstName");
                    String last = doc.getString("lastName");

                    String childName = (first == null ? "" : first) + " " + (last == null ? "" : last);
                    childName = childName.trim();

                    // ✅ make final copies for lambda use
                    final String finalChildName = childName;
                    final String finalService = service;
                    final String finalDate = date;
                    final String finalTime = time;

                    String id = db.collection("appointments").document().getId();

                    Appointment appt = new Appointment(
                            id,
                            studentId,
                            finalChildName,
                            "",
                            "",
                            finalService,
                            finalDate,
                            finalTime,
                            "Pending"
                    );

                    db.collection("appointments").document(id)
                            .set(appt)
                            .addOnSuccessListener(a -> {

                                // ✅ schedule reminder 24 hours before
                                long apptMillis = parseAppointmentMillis(finalDate, finalTime);
                                if (apptMillis > 0) {
                                    int requestCode = ("APPT_" + id).hashCode();

                                    String title = "Appointment Reminder";

                                    String msg = "Tomorrow: " +
                                            (finalChildName.isEmpty() ? "Your appointment" : finalChildName) +
                                            (finalService == null || finalService.trim().isEmpty() ? "" : " — " + finalService.trim()) +
                                            " at " + finalTime;

                                    ReminderScheduler.schedule24HoursBefore(
                                            requireContext(),
                                            apptMillis,
                                            requestCode,
                                            title,
                                            msg,
                                            id,        // appointmentId
                                            "Parent"   // roleLabel
                                    );
                                }

                                Toast.makeText(getContext(), "Appointment Sent", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showPopupOptions(Appointment appt, View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_appointment_options, popup.getMenu());

        String status = appt.getStatus() != null ? appt.getStatus() : "";

        if (status.equalsIgnoreCase("Canceled") || status.equalsIgnoreCase("Cancelled")) {
            popup.getMenu().findItem(R.id.action_reschedule).setVisible(false);
            popup.getMenu().findItem(R.id.action_cancel).setVisible(false);
            popup.getMenu().findItem(R.id.action_remove).setVisible(true);
        } else {
            popup.getMenu().findItem(R.id.action_reschedule).setVisible(true);
            popup.getMenu().findItem(R.id.action_cancel).setVisible(true);
            popup.getMenu().findItem(R.id.action_remove).setVisible(false);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_reschedule) {
                showRescheduleDialog(appt);
                return true;
            }

            if (id == R.id.action_cancel) {
                showCancelConfirmation(appt);
                return true;
            }

            if (id == R.id.action_remove) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Remove Appointment")
                        .setMessage("Remove this appointment permanently?")
                        .setPositiveButton("Yes", (d, w) -> removeAppointment(appt))
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void showCancelConfirmation(Appointment appt) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Appointment")
                .setMessage("Cancel this appointment?")
                .setPositiveButton("Yes", (d, w) -> updateStatus(appt, "Canceled"))
                .setNegativeButton("No", null)
                .show();
    }

    private void showRescheduleDialog(Appointment appt) {

        View view = getLayoutInflater().inflate(R.layout.dialog_reschedule, null);

        EditText etNewDate = view.findViewById(R.id.etNewDate);
        EditText etNewTime = view.findViewById(R.id.etNewTime);
        EditText etComment = view.findViewById(R.id.etComment);

        etNewDate.setOnClickListener(v -> pickDate(etNewDate));
        etNewTime.setOnClickListener(v -> pickTime(etNewTime, etNewDate));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Reschedule Appointment")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newDate = etNewDate.getText().toString().trim();
                String newTime = etNewTime.getText().toString().trim();
                String comment = etComment.getText().toString().trim();

                boolean hasError = false;

                if (newDate.isEmpty()) {
                    etNewDate.setError("Please select a new date");
                    hasError = true;
                } else {
                    etNewDate.setError(null);
                }

                if (newTime.isEmpty()) {
                    etNewTime.setError("Please select a new time");
                    hasError = true;
                } else {
                    etNewTime.setError(null);
                }

                if (comment.isEmpty()) {
                    etComment.setError("Please provide a comment / reason");
                    hasError = true;
                } else {
                    etComment.setError(null);
                }

                if (hasError) return;

                updateReschedule(appt, newDate, newTime, comment);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void updateStatus(Appointment appt, String newStatus) {
        if (appt == null || appt.getId() == null || appt.getId().trim().isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update("status", newStatus)
                .addOnSuccessListener(a -> {

                    // Cancel reminder if user cancels the appointment
                    if ("Canceled".equalsIgnoreCase(newStatus) || "Cancelled".equalsIgnoreCase(newStatus)) {
                        int requestCode = ("APPT_" + appt.getId()).hashCode();
                        ReminderScheduler.cancelReminder(requireContext(), requestCode);
                    }

                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateReschedule(Appointment appt, String newDate, String newTime, String comment) {
        if (appt == null || appt.getId() == null || appt.getId().trim().isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update(
                        "date", newDate,
                        "time", newTime,
                        "status", "Rescheduled",
                        "rescheduleComment", comment
                )
                .addOnSuccessListener(a -> {

                    // ✅ Reschedule reminder (24 hours before new date/time)
                    long apptMillis = parseAppointmentMillis(newDate, newTime);
                    if (apptMillis > 0) {
                        int requestCode = ("APPT_" + appt.getId()).hashCode();

                        ReminderScheduler.cancelReminder(requireContext(), requestCode);

                        String child = appt.getChildName() == null ? "" : appt.getChildName().trim();
                        String service = appt.getService() == null ? "" : appt.getService().trim();

                        String title = "Appointment Reminder";
                        String message = "Tomorrow: " +
                                (child.isEmpty() ? "Your appointment" : child) +
                                (service.isEmpty() ? "" : " — " + service) +
                                " at " + newTime;

                        ReminderScheduler.schedule24HoursBefore(
                                requireContext(),
                                apptMillis,
                                requestCode,
                                title,
                                message,
                                appt.getId(),
                                "Parent"
                        );
                    }

                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to reschedule: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void removeAppointment(Appointment appt) {
        if (appt == null || appt.getId() == null || appt.getId().trim().isEmpty()) return;

        int requestCode = ("APPT_" + appt.getId()).hashCode();
        ReminderScheduler.cancelReminder(requireContext(), requestCode);

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Appointment removed", Toast.LENGTH_SHORT).show();
                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
