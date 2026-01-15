package com.example.appointable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedDate, tvAppointmentIndicators, btnAddForSelectedDate;
    private RecyclerView rvAppointments;

    private AppointmentAdapter adapter;

    // All appointments for this student (from Firestore)
    private final List<Appointment> allAppointments = new ArrayList<>();

    // Appointments only for the selected day (used by adapter)
    private final List<Appointment> dayAppointments = new ArrayList<>();

    private String selectedDate = null;

    private static final int MAX_APPOINTMENTS_PER_DAY = 10;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.activity_home_fragment, container, false);

        // Initialize UI
        calendarView = root.findViewById(R.id.calendarView);
        tvSelectedDate = root.findViewById(R.id.tvSelectedDate);
        tvAppointmentIndicators = root.findViewById(R.id.tvAppointmentIndicators);
        btnAddForSelectedDate = root.findViewById(R.id.btnAddForSelectedDate);
        rvAppointments = root.findViewById(R.id.rvAppointments);

        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        // Use dayAppointments as the adapter list
        adapter = new AppointmentAdapter(dayAppointments, null); // Home page doesn't use cancel/resched
        rvAppointments.setAdapter(adapter);

        btnAddForSelectedDate.setOnClickListener(v ->
                        Toast.makeText(getContext(), "Open your Add Appointment dialog here", Toast.LENGTH_SHORT).show()
                // You can call your existing showAddAppointmentDialog(selectedDate) here if you add that method
        );

        loadAllAppointments();
        setupCalendarSelection();

        return root;
    }

    // ---------------------------------------------------------------------
    // LOAD ALL APPOINTMENTS FOR THE LOGGED-IN USER
    // ---------------------------------------------------------------------
    private void loadAllAppointments() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("studentId", userId)
                .get()
                .addOnSuccessListener(query -> {

                    allAppointments.clear();
                    for (DocumentSnapshot doc : query) {
                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt != null) {
                            allAppointments.add(appt);
                        }
                    }

                    // Default to today
                    selectedDate = getTodayDate();
                    tvSelectedDate.setText(formatDateDisplay(selectedDate));

                    filterAppointmentsForDay(selectedDate);
                });
    }

    private String getTodayDate() {
        Calendar c = Calendar.getInstance();
        return (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.YEAR);
    }

    // ---------------------------------------------------------------------
    // WHEN USER SELECTS A DATE ON THE CALENDAR
    // ---------------------------------------------------------------------
    private void setupCalendarSelection() {

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {

            String newDate = (month + 1) + "/" + dayOfMonth + "/" + year;

            // Prevent selecting fully booked days
            int count = countAppointmentsOnDate(newDate);
            if (count >= MAX_APPOINTMENTS_PER_DAY) {
                Toast.makeText(getContext(), "This day is fully booked", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedDate = newDate;

            tvSelectedDate.setText(formatDateDisplay(newDate));

            filterAppointmentsForDay(newDate);
        });
    }

    // Count appointments for a date
    private int countAppointmentsOnDate(String date) {
        int count = 0;
        for (Appointment appt : allAppointments) {
            String apptDate = appt.getDate();
            if (apptDate != null && apptDate.equals(date)) {
                count++;
            }
        }
        return count;
    }

    // ---------------------------------------------------------------------
    // FILTER APPOINTMENTS WHEN A DATE IS SELECTED
    // ---------------------------------------------------------------------
    private void filterAppointmentsForDay(String date) {

        dayAppointments.clear();

        for (Appointment appt : allAppointments) {
            String apptDate = appt.getDate();
            if (apptDate != null && apptDate.equals(date)) {
                dayAppointments.add(appt);
            }
        }

        // Sort by time string (if your time is like "8:00 AM", "1:30 PM")
        Collections.sort(dayAppointments, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment a1, Appointment a2) {
                // Simple string compare is usually okay for "h:mm a" format
                String t1 = a1.getTime() != null ? a1.getTime() : "";
                String t2 = a2.getTime() != null ? a2.getTime() : "";
                return t1.compareTo(t2);
            }
        });

        adapter.notifyDataSetChanged();

        // ---------------------------------------
        // DOT-STYLE INDICATOR & ADD BUTTON
        // ---------------------------------------
        if (dayAppointments.isEmpty()) {
            tvAppointmentIndicators.setText("• No appointments for this day");
            btnAddForSelectedDate.setVisibility(View.VISIBLE);
        } else {
            tvAppointmentIndicators.setText("• " + dayAppointments.size() + " appointment(s)");
            btnAddForSelectedDate.setVisibility(View.GONE);
        }
    }

    // Format "11/25/2025" → "November 25, 2025"
    private String formatDateDisplay(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            return output.format(input.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
