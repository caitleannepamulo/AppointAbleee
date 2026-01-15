package com.example.appointable;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ParentHomeFragment extends Fragment {

    private TextView tvGreeting, tvNameOfUser, tvSelectedDate, tvAppointmentIndicators;
    private MaterialCalendarView calendarView;

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;

    private final List<Appointment> allAppointments = new ArrayList<>();
    private final List<Appointment> filteredAppointments = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Calendar currentSelectedDate;
    private EventDotDecorator dotDecorator;

    public ParentHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_home_fragment, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvGreeting              = view.findViewById(R.id.morningNight);
        tvNameOfUser            = view.findViewById(R.id.nameOfUser);
        tvSelectedDate          = view.findViewById(R.id.tvSelectedDate);
        tvAppointmentIndicators = view.findViewById(R.id.tvAppointmentIndicators);

        calendarView   = view.findViewById(R.id.calendarView);
        rvAppointments = view.findViewById(R.id.rvAppointments);

        setupGreeting();
        initRecycler();
        setupCalendarClick();

        // Default to today
        currentSelectedDate = Calendar.getInstance();
        normalizeCalendar(currentSelectedDate);
        setSelectedDateLabel(currentSelectedDate);

        // Select + focus today after layout
        calendarView.post(() -> {
            CalendarDay today = calendarDayFromCalendar(currentSelectedDate);
            calendarView.setCurrentDate(today);
            calendarView.setSelectedDate(today);
        });

        loadAppointments();

        return view;
    }

    // ---------------- GREETING ----------------

    private void setupGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) tvGreeting.setText("Good Morning,");
        else if (hour < 18) tvGreeting.setText("Good Afternoon,");
        else tvGreeting.setText("Good Evening,");

        loadUserName();
    }

    private void loadUserName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String firstName = snapshot.getString("firstName");
                    if (firstName != null && !firstName.isEmpty()) {
                        tvNameOfUser.setText(firstName + "!");
                    }
                });
    }

    // ---------------- RECYCLER ----------------

    private void initRecycler() {
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AppointmentAdapter(
                filteredAppointments,
                new AppointmentAdapter.OnAppointmentActionListener() {
                    @Override public void onCancel(Appointment appt) { showCancelConfirmation(appt); }
                    @Override public void onReschedule(Appointment appt) { showRescheduleDialog(appt); }
                    @Override public void onMoreOptions(Appointment appt, View anchor) { showPopupOptions(appt, anchor); }
                });

        rvAppointments.setAdapter(adapter);
    }

    // ---------------- LOAD FIRESTORE ----------------

    private void loadAppointments() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("studentId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {

                    allAppointments.clear();

                    for (DocumentSnapshot doc : query) {
                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt != null) {
                            if (appt.getId() == null || appt.getId().isEmpty()) {
                                appt.setId(doc.getId());
                            }
                            allAppointments.add(appt);
                        }
                    }

                    applyAppointmentDots();
                    filterAppointmentsForDate(currentSelectedDate);
                });
    }

    // ---------------- CALENDAR CLICK ----------------

    private void setupCalendarClick() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (!selected || date == null) return;

            // MaterialCalendarView 1.4.3: date.getMonth() is 0-based (Jan=0)
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, date.getYear());
            cal.set(Calendar.MONTH, date.getMonth());   // ✅ NO -1
            cal.set(Calendar.DAY_OF_MONTH, date.getDay());
            normalizeCalendar(cal);

            currentSelectedDate = (Calendar) cal.clone();

            setSelectedDateLabel(currentSelectedDate);
            filterAppointmentsForDate(currentSelectedDate);
        });
    }

    private void setSelectedDateLabel(Calendar cal) {
        String formatted = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                .format(cal.getTime());
        tvSelectedDate.setText(formatted);
    }

    // ---------------- DOTS ----------------

    private void applyAppointmentDots() {
        Set<CalendarDay> daysWithAppointments = new HashSet<>();

        for (Appointment a : allAppointments) {
            CalendarDay day = toCalendarDayFromAppointment(a);
            if (day != null) daysWithAppointments.add(day);
        }

        if (dotDecorator != null) {
            calendarView.removeDecorator(dotDecorator);
        }

        dotDecorator = new EventDotDecorator(daysWithAppointments);
        calendarView.addDecorator(dotDecorator);
        calendarView.invalidateDecorators();
    }

    private CalendarDay toCalendarDayFromAppointment(Appointment a) {
        try {
            if (a == null || a.getDate() == null) return null;

            // Firestore example: "1/11/2026"
            String[] parts = a.getDate().split("/");
            if (parts.length != 3) return null;

            int month1to12 = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            int month0to11 = month1to12 - 1; // ✅ convert to 0-based for v1.4.3
            return CalendarDay.from(year, month0to11, day);

        } catch (Exception e) {
            return null;
        }
    }

    private CalendarDay calendarDayFromCalendar(Calendar cal) {
        return CalendarDay.from(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH), // 0-based
                cal.get(Calendar.DAY_OF_MONTH)
        );
    }

    // ---------------- FILTERING ----------------

    private void filterAppointmentsForDate(Calendar selectedDay) {

        filteredAppointments.clear();

        Calendar selected = (Calendar) selectedDay.clone();
        normalizeCalendar(selected);

        int count = 0;

        for (Appointment a : allAppointments) {
            try {
                if (a == null || a.getDate() == null) continue;

                Calendar apptCal = Calendar.getInstance();
                apptCal.setTime(parseDateOnly(a.getDate()));
                normalizeCalendar(apptCal);

                if (isSameDay(selected, apptCal)) {
                    filteredAppointments.add(a);
                    count++;
                }

            } catch (Exception ignored) {}
        }

        tvAppointmentIndicators.setText(count + " appointment(s)");
        sortAppointments();
        adapter.notifyDataSetChanged();
    }

    private Date parseDateOnly(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
        return sdf.parse(dateStr);
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) &&
                (c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR));
    }

    private void normalizeCalendar(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    // ---------------- SORT ----------------

    private void sortAppointments() {
        Collections.sort(filteredAppointments, (a1, a2) -> {
            Date d1 = parseDateTime(a1);
            Date d2 = parseDateTime(a2);
            if (d1 == null || d2 == null) return 0;
            return d1.compareTo(d2);
        });
    }

    private Date parseDateTime(Appointment appt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault());
            return sdf.parse(appt.getDate() + " " + appt.getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    // ---------------- OPTIONS MENU ----------------

    private void showPopupOptions(Appointment appt, View anchor) {

        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_appointment_options, popup.getMenu());

        String status = appt.getStatus() != null ? appt.getStatus() : "";

        if (status.equalsIgnoreCase("Canceled")) {
            popup.getMenu().findItem(R.id.action_reschedule).setVisible(false);
            popup.getMenu().findItem(R.id.action_cancel).setVisible(false);
            popup.getMenu().findItem(R.id.action_remove).setVisible(true);
        } else {
            popup.getMenu().findItem(R.id.action_reschedule).setVisible(true);
            popup.getMenu().findItem(R.id.action_cancel).setVisible(true);
            popup.getMenu().findItem(R.id.action_remove).setVisible(false);
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_reschedule) { showRescheduleDialog(appt); return true; }
            if (item.getItemId() == R.id.action_cancel) { showCancelConfirmation(appt); return true; }
            if (item.getItemId() == R.id.action_remove) { showDeleteConfirmation(appt); return true; }
            return false;
        });

        popup.show();
    }

    // ---------------- DELETE ----------------

    private void showDeleteConfirmation(Appointment appt) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Appointment")
                .setMessage("Remove this appointment permanently?")
                .setPositiveButton("Yes", (d, w) -> removeAppointment(appt))
                .setNegativeButton("No", null)
                .show();
    }

    private void removeAppointment(Appointment appt) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Appointment removed", Toast.LENGTH_SHORT).show();
                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to remove", Toast.LENGTH_SHORT).show());
    }

    // ---------------- CANCEL ----------------

    private void showCancelConfirmation(Appointment appt) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes", (d, w) -> updateStatus(appt, "Canceled"))
                .setNegativeButton("No", null)
                .show();
    }

    // ---------------- RESCHEDULE ----------------

    private void showRescheduleDialog(Appointment appt) {

        View view = getLayoutInflater().inflate(R.layout.dialog_reschedule, null);

        EditText etNewDate = view.findViewById(R.id.etNewDate);
        EditText etNewTime = view.findViewById(R.id.etNewTime);

        etNewDate.setOnClickListener(v -> pickDate(etNewDate));
        etNewTime.setOnClickListener(v -> pickTime(etNewTime));

        new AlertDialog.Builder(getContext())
                .setTitle("Reschedule Appointment")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {

                    String newDate = etNewDate.getText().toString();
                    String newTime = etNewTime.getText().toString();

                    if (newDate.isEmpty() || newTime.isEmpty()) {
                        Toast.makeText(getContext(), "Pick new date and time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateReschedule(appt, newDate, newTime);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickDate(EditText et) {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(
                getContext(),
                (picker, y, m, d) -> et.setText((m + 1) + "/" + d + "/" + y),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void pickTime(EditText et) {
        Calendar c = Calendar.getInstance();

        new TimePickerDialog(
                getContext(),
                (picker, h, m) -> {
                    String ampm = (h >= 12) ? "PM" : "AM";
                    int hour = (h % 12 == 0) ? 12 : h % 12;
                    et.setText(hour + ":" + String.format("%02d", m) + " " + ampm);
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
        ).show();
    }

    // ---------------- FIRESTORE UPDATES ----------------

    private void updateStatus(Appointment appt, String status) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update("status", status)
                .addOnSuccessListener(a -> loadAppointments());
    }

    private void updateReschedule(Appointment appt, String newDate, String newTime) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update(
                        "date", newDate,
                        "time", newTime,
                        "status", "Rescheduled"
                )
                .addOnSuccessListener(a -> loadAppointments());
    }
}
