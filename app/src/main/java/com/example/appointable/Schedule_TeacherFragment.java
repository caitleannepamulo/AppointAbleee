package com.example.appointable;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Schedule_TeacherFragment extends Fragment {

    private static final int STATUS_ACCEPTED = 0;   // Accepted or Rescheduled
    private static final int STATUS_COMPLETED = 1;
    private static final int STATUS_CANCELED = 2;

    private RecyclerView rvAllAppointments;
    private TextView tvRemainingCount, tvCompletedCount, tvCanceledCount;
    private ImageView ivSort;
    private CardView cardRemainingAppointments, cardCompletedAppointments, cardCanceledAppointments;
    private TextView tvMonday, tvTuesday, tvWednesday, tvThursday, tvFriday, tvSaturday, tvSunday;

    private final List<ScheduleModel> weekAppointments = new ArrayList<>();
    private final List<ScheduleModel> workingAppointments = new ArrayList<>();
    private final List<ScheduleModel> displayedAppointments = new ArrayList<>();
    private ScheduleAdapter adapter;

    private boolean isAscending = true;

    // filters
    private int statusFilter = STATUS_ACCEPTED;
    private int selectedDay = 0;
    private int weekOffset = 0;

    // current visible week (for filtering)
    private int currentWeekYear;
    private int currentWeekOfYear;

    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // in-app dialog memory (avoid repeated dialogs)
    private static final String PREFS = "appt_inapp_dialogs";
    private static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    public Schedule_TeacherFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule_teacher, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvRemainingCount = view.findViewById(R.id.tvRemainingCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvCanceledCount = view.findViewById(R.id.tvCanceledCount);

        rvAllAppointments = view.findViewById(R.id.rvAllAppointments);
        ivSort = view.findViewById(R.id.ivSort);

        cardRemainingAppointments = view.findViewById(R.id.cardRemainingAppointments);
        cardCompletedAppointments = view.findViewById(R.id.cardCompletedAppointments);
        cardCanceledAppointments = view.findViewById(R.id.cardCanceledAppointments);

        tvMonday = view.findViewById(R.id.tvMonday);
        tvTuesday = view.findViewById(R.id.tvTuesday);
        tvWednesday = view.findViewById(R.id.tvWednesday);
        tvThursday = view.findViewById(R.id.tvThursday);
        tvFriday = view.findViewById(R.id.tvFriday);
        tvSaturday = view.findViewById(R.id.tvSaturday);
        tvSunday = view.findViewById(R.id.tvSunday);

        setupRecyclerView();
        setupSortButton();
        setupFilterCards();
        setupDayClickListeners();
        setupSwipeGesture(view);

        setWeekDayLabels();
        autoSelectToday();

        loadAppointmentsFromFirestore();

        return view;
    }

    private void loadAppointmentsFromFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = user.getUid();

        db.collection("appointments")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnCompleteListener(this::onAppointmentsLoaded);
    }

    private void onAppointmentsLoaded(@NonNull Task<QuerySnapshot> task) {
        if (!task.isSuccessful()) {
            Toast.makeText(getContext(), "Failed to load appointments.", Toast.LENGTH_SHORT).show();
            return;
        }

        weekAppointments.clear();

        // ✅ Firestore stores like "1/16/2026"
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
        dateFormat.setLenient(false);

        for (QueryDocumentSnapshot doc : task.getResult()) {
            String childName = doc.getString("childName");
            String service = doc.getString("service");
            String timeStr = doc.getString("time");
            String dateStr = doc.getString("date");
            String statusStr = doc.getString("status");
            String docId = doc.getId();

            if (statusStr != null) statusStr = statusStr.trim();
            if (statusStr == null) continue;

            int status;
            if (statusStr.equalsIgnoreCase("Completed")) {
                status = STATUS_COMPLETED;
            } else if (statusStr.equalsIgnoreCase("Canceled") || statusStr.equalsIgnoreCase("Cancelled")) {
                status = STATUS_CANCELED;
            } else if (statusStr.equalsIgnoreCase("Accepted") || statusStr.equalsIgnoreCase("Rescheduled")) {
                status = STATUS_ACCEPTED;
            } else {
                continue;
            }

            int sortMinutes = parseTimeToMinutes(timeStr);
            int dayOfWeek = 0;
            int weekYear = 0;
            int weekOfYear = 0;

            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    Date date = dateFormat.parse(dateStr.trim());
                    if (date != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        dayOfWeek = mapCalendarDayToCustom(cal.get(Calendar.DAY_OF_WEEK));
                        weekYear = cal.get(Calendar.YEAR);
                        weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            ScheduleModel m = new ScheduleModel(
                    childName != null ? childName : "",
                    service != null ? service : "",
                    timeStr != null ? timeStr : "",
                    sortMinutes,
                    dayOfWeek,
                    docId,
                    dateStr != null ? dateStr : "",
                    weekYear,
                    weekOfYear
            );
            m.setStatus(status);
            weekAppointments.add(m);

            // ✅ schedule notification (works even if app is closed)
            if (status == STATUS_ACCEPTED) {
                schedule24hNotification(m);
            }
        }

        setStatsFromData();
        setWeekDayLabels();
        applyFilterSortAndReset();

        // ✅ show in-app popup if within 24h (only when app open)
        showInAppDialogIfWithin24Hours();
    }

    // ------------------ NOTIFICATION SCHEDULING ------------------

    private void schedule24hNotification(ScheduleModel m) {
        if (getContext() == null) return;

        long apptMillis = toMillis(m.getDateString(), m.getTime());
        if (apptMillis <= System.currentTimeMillis()) return;

        int requestCode = (m.getDocumentId() == null || m.getDocumentId().isEmpty())
                ? (m.getChildName() + m.getDateString() + m.getTime()).hashCode()
                : m.getDocumentId().hashCode();

        String title = "Appointment Reminder";

        String child = m.getChildName() == null ? "" : m.getChildName().trim();
        String service = m.getService() == null ? "" : m.getService().trim();

        String message = "Tomorrow: " +
                (child.isEmpty() ? "Student" : child) +
                (service.isEmpty() ? "" : " — " + service) +
                " at " + m.getTime();

        // ✅ match schedule24HoursBefore(Context, long, int, title, message, appointmentId, roleLabel)
        ReminderScheduler.schedule24HoursBefore(
                getContext(),
                apptMillis,
                requestCode,
                title,
                message,
                m.getDocumentId(),
                "Teacher"
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ------------------ IN-APP DIALOG (OPEN APP) ------------------

    private void showInAppDialogIfWithin24Hours() {
        if (getContext() == null) return;

        long now = System.currentTimeMillis();
        long end = now + ONE_DAY_MS;

        ScheduleModel nearest = null;
        long nearestMillis = Long.MAX_VALUE;

        for (ScheduleModel m : weekAppointments) {
            if (m.getStatus() != STATUS_ACCEPTED) continue;

            long apptMillis = toMillis(m.getDateString(), m.getTime());
            if (apptMillis <= now) continue;
            if (apptMillis > end) continue;

            String key = getDialogKey(m, apptMillis);
            if (wasDialogShown(key)) continue;

            if (apptMillis < nearestMillis) {
                nearestMillis = apptMillis;
                nearest = m;
            }
        }

        if (nearest == null) return;

        long apptMillis = toMillis(nearest.getDateString(), nearest.getTime());
        String key = getDialogKey(nearest, apptMillis);
        markDialogShown(key);

        new AlertDialog.Builder(requireContext())
                .setTitle("Upcoming Appointment")
                .setMessage(
                        "Within the next 24 hours:\n\n" +
                                "Student: " + nearest.getChildName() + "\n" +
                                "Service: " + nearest.getService() + "\n" +
                                "Date: " + nearest.getDateString() + "\n" +
                                "Time: " + nearest.getTime()
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private String getDialogKey(ScheduleModel m, long apptMillis) {
        String id = (m.getDocumentId() == null) ? "" : m.getDocumentId();
        return "shown_" + id + "_" + apptMillis;
    }

    private boolean wasDialogShown(String key) {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(key, false);
    }

    private void markDialogShown(String key) {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key, true).apply();
    }

    private long toMillis(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return 0;
        if (timeStr == null || timeStr.trim().isEmpty()) return 0;

        try {
            // ✅ matches Firestore: "1/16/2026" + "1:45 PM"
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault());
            sdf.setLenient(false);

            Date dt = sdf.parse(dateStr.trim() + " " + timeStr.trim());
            return dt == null ? 0 : dt.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null) return 0;
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            timeFormat.setLenient(false);

            Date date = timeFormat.parse(timeStr.trim());
            if (date == null) return 0;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ------------------ swipe ------------------

    private void setupSwipeGesture(View rootView) {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)
                        && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) changeWeek(-1);
                    else changeWeek(1);

                    return true;
                }
                return false;
            }
        });

        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void changeWeek(int delta) {
        weekOffset += delta;
        selectedDay = 0;
        clearDayHighlights();
        setWeekDayLabels();
        applyFilterSortAndReset();
    }

    // ------------------ week labels ------------------

    private void setWeekDayLabels() {
        Calendar realToday = Calendar.getInstance();
        int todayMapped = mapCalendarDayToCustom(realToday.get(Calendar.DAY_OF_WEEK));

        Calendar monday = (Calendar) realToday.clone();
        monday.add(Calendar.DAY_OF_MONTH, 1 - todayMapped);
        monday.add(Calendar.WEEK_OF_YEAR, weekOffset);

        currentWeekYear = monday.get(Calendar.YEAR);
        currentWeekOfYear = monday.get(Calendar.WEEK_OF_YEAR);

        Calendar tuesday = (Calendar) monday.clone(); tuesday.add(Calendar.DAY_OF_MONTH, 1);
        Calendar wednesday = (Calendar) monday.clone(); wednesday.add(Calendar.DAY_OF_MONTH, 2);
        Calendar thursday = (Calendar) monday.clone(); thursday.add(Calendar.DAY_OF_MONTH, 3);
        Calendar friday = (Calendar) monday.clone(); friday.add(Calendar.DAY_OF_MONTH, 4);
        Calendar saturday = (Calendar) monday.clone(); saturday.add(Calendar.DAY_OF_MONTH, 5);
        Calendar sunday = (Calendar) monday.clone(); sunday.add(Calendar.DAY_OF_MONTH, 6);

        SimpleDateFormat df = new SimpleDateFormat("MMM d", Locale.getDefault());

        setDayLabel(tvMonday, "Mon", monday, realToday, df);
        setDayLabel(tvTuesday, "Tue", tuesday, realToday, df);
        setDayLabel(tvWednesday, "Wed", wednesday, realToday, df);
        setDayLabel(tvThursday, "Thu", thursday, realToday, df);
        setDayLabel(tvFriday, "Fri", friday, realToday, df);
        setDayLabel(tvSaturday, "Sat", saturday, realToday, df);
        setDayLabel(tvSunday, "Sun", sunday, realToday, df);
    }

    private void setDayLabel(TextView tv, String dayShortName, Calendar dayCal, Calendar realToday, SimpleDateFormat df) {
        String dateText = (isSameDay(dayCal, realToday) && weekOffset == 0) ? "Today" : df.format(dayCal.getTime());
        tv.setText(dayShortName + "\n" + dateText);
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void autoSelectToday() {
        Calendar today = Calendar.getInstance();
        selectedDay = mapCalendarDayToCustom(today.get(Calendar.DAY_OF_WEEK));
        statusFilter = STATUS_ACCEPTED;
        applyFilterSortAndReset();

        switch (selectedDay) {
            case 1: highlightSelectedDay(tvMonday); break;
            case 2: highlightSelectedDay(tvTuesday); break;
            case 3: highlightSelectedDay(tvWednesday); break;
            case 4: highlightSelectedDay(tvThursday); break;
            case 5: highlightSelectedDay(tvFriday); break;
            case 6: highlightSelectedDay(tvSaturday); break;
            case 7: highlightSelectedDay(tvSunday); break;
        }
    }

    private int mapCalendarDayToCustom(int calDay) {
        switch (calDay) {
            case Calendar.MONDAY: return 1;
            case Calendar.TUESDAY: return 2;
            case Calendar.WEDNESDAY: return 3;
            case Calendar.THURSDAY: return 4;
            case Calendar.FRIDAY: return 5;
            case Calendar.SATURDAY: return 6;
            case Calendar.SUNDAY: return 7;
            default: return 0;
        }
    }

    // ------------------ stats ------------------

    private void setStatsFromData() {
        int accepted = 0, completed = 0, canceled = 0;

        for (ScheduleModel m : weekAppointments) {
            if (m.getStatus() == STATUS_COMPLETED) completed++;
            else if (m.getStatus() == STATUS_CANCELED) canceled++;
            else accepted++;
        }

        tvRemainingCount.setText(String.valueOf(accepted));
        tvCompletedCount.setText(String.valueOf(completed));
        tvCanceledCount.setText(String.valueOf(canceled));
    }

    // ------------------ recycler / filters ------------------

    private void setupRecyclerView() {
        rvAllAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ScheduleAdapter(displayedAppointments, () -> {
            setStatsFromData();
            applyFilterSortAndReset();
        });

        rvAllAppointments.setAdapter(adapter);
    }

    private void setupSortButton() {
        ivSort.setOnClickListener(v -> {
            isAscending = !isAscending;
            applyFilterSortAndReset();
        });
    }

    private void setupFilterCards() {
        cardRemainingAppointments.setOnClickListener(v -> {
            statusFilter = STATUS_ACCEPTED;
            selectedDay = 0;
            clearDayHighlights();
            applyFilterSortAndReset();
        });

        cardCompletedAppointments.setOnClickListener(v -> {
            statusFilter = STATUS_COMPLETED;
            selectedDay = 0;
            clearDayHighlights();
            applyFilterSortAndReset();
        });

        cardCanceledAppointments.setOnClickListener(v -> {
            statusFilter = STATUS_CANCELED;
            selectedDay = 0;
            clearDayHighlights();
            applyFilterSortAndReset();
        });
    }

    private void setupDayClickListeners() {
        tvMonday.setOnClickListener(v -> { selectedDay = 1; applyFilterSortAndReset(); highlightSelectedDay(tvMonday); });
        tvTuesday.setOnClickListener(v -> { selectedDay = 2; applyFilterSortAndReset(); highlightSelectedDay(tvTuesday); });
        tvWednesday.setOnClickListener(v -> { selectedDay = 3; applyFilterSortAndReset(); highlightSelectedDay(tvWednesday); });
        tvThursday.setOnClickListener(v -> { selectedDay = 4; applyFilterSortAndReset(); highlightSelectedDay(tvThursday); });
        tvFriday.setOnClickListener(v -> { selectedDay = 5; applyFilterSortAndReset(); highlightSelectedDay(tvFriday); });
        tvSaturday.setOnClickListener(v -> { selectedDay = 6; applyFilterSortAndReset(); highlightSelectedDay(tvSaturday); });
        tvSunday.setOnClickListener(v -> { selectedDay = 7; applyFilterSortAndReset(); highlightSelectedDay(tvSunday); });
    }

    private void clearDayHighlights() {
        TextView[] all = { tvMonday, tvTuesday, tvWednesday, tvThursday, tvFriday, tvSaturday, tvSunday };
        for (TextView tv : all) {
            tv.setBackground(null);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(11f);
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void highlightSelectedDay(TextView selected) {
        clearDayHighlights();
        selected.setBackgroundResource(R.drawable.bg_day_select);
        selected.setTextColor(Color.WHITE);
        selected.setTextSize(13f);
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void applyFilterSortAndReset() {
        workingAppointments.clear();

        for (ScheduleModel m : weekAppointments) {
            boolean matchesStatus =
                    (statusFilter == STATUS_ACCEPTED && m.getStatus() == STATUS_ACCEPTED)
                            || (statusFilter == STATUS_COMPLETED && m.getStatus() == STATUS_COMPLETED)
                            || (statusFilter == STATUS_CANCELED && m.getStatus() == STATUS_CANCELED);

            boolean matchesWeek = m.getWeekYear() == currentWeekYear && m.getWeekOfYear() == currentWeekOfYear;
            boolean matchesDay = (selectedDay == 0) || (m.getDayOfWeek() == selectedDay);

            if (matchesStatus && matchesWeek && matchesDay) workingAppointments.add(m);
        }

        Collections.sort(workingAppointments, new Comparator<ScheduleModel>() {
            @Override
            public int compare(ScheduleModel a, ScheduleModel b) {
                return isAscending
                        ? Integer.compare(a.getSortTimeMinutes(), b.getSortTimeMinutes())
                        : Integer.compare(b.getSortTimeMinutes(), a.getSortTimeMinutes());
            }
        });

        displayedAppointments.clear();
        displayedAppointments.addAll(workingAppointments);
        adapter.notifyDataSetChanged();
    }
}
