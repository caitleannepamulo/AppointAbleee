package com.example.appointable;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.SharedPreferences;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;


import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;        // <-- added
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class TeacherHomeFragment extends Fragment {

    private TextView tvGreeting, tvNameOfUser, tvTodayTitle;
    private ImageView ivTodayCalendar, ivQuoteImage;
    private RecyclerView rvToday;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppointmentTeacherAdapter appointmentTeacherAdapter;
    private LineChart teacherProgressChart;
    private SharedPreferences prefs;
    private ArrayList<Entry> scoreEntries = new ArrayList<>();
    private final List<AppointmentTeacher> allAppointmentTeachers = new ArrayList<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat dbDateFormat =
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private Calendar selectedDate = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_teacher_home_fragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvGreeting = view.findViewById(R.id.goodMorning);
        tvNameOfUser = view.findViewById(R.id.nameOfUser);
        tvTodayTitle = view.findViewById(R.id.tvTodayTitle);

        ivTodayCalendar = view.findViewById(R.id.ivTodayCalendar);
        ivQuoteImage = view.findViewById(R.id.ivQuoteImage);

        rvToday = view.findViewById(R.id.rvToday);

        teacherProgressChart = view.findViewById(R.id.teacherProgressChart);
        prefs = requireContext().getSharedPreferences("Scores", Context.MODE_PRIVATE);

        setupChart();
        loadScoresFromFirestore();
        setupGreeting();
        setupRecyclerViews();

        tvTodayTitle.setText(dateFormat.format(selectedDate.getTime()));

        loadAppointmentsFromFirestore();
        setupCalendarPicker();
        showRandomQuoteImage();
    }

    // ------------------ greeting + user name ------------------

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
        if (user == null) {
            tvNameOfUser.setText("Unknown User!");
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String firstName = snapshot.getString("firstName");
                    if (firstName != null && !firstName.isEmpty()) {
                        String formatted = firstName.substring(0, 1).toUpperCase() +
                                firstName.substring(1).toLowerCase();
                        tvNameOfUser.setText(formatted + "!");
                    }
                })
                .addOnFailureListener(e -> tvNameOfUser.setText("Unknown User!"));
    }

    // ------------------ RecyclerViews ------------------

    private void setupRecyclerViews() {
        rvToday.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentTeacherAdapter = new AppointmentTeacherAdapter(new ArrayList<>());
        rvToday.setAdapter(appointmentTeacherAdapter);
    }

    // ------------------ Firestore: load appointments with specific statuses ------------------

    private void loadAppointmentsFromFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = user.getUid();

        db.collection("appointments")
                .whereEqualTo("teacherId", teacherId)
                .whereIn("status", Arrays.asList(
                        "Accepted",
                        "Rescheduled",
                        "Canceled",
                        "Completed"
                ))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allAppointmentTeachers.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String childName = doc.getString("childName");
                        String service = doc.getString("service");
                        String timeStr = doc.getString("time");
                        String dateStr = doc.getString("date");
                        String statusStr = doc.getString("status");

                        if (childName == null) childName = "";
                        if (service == null) service = "";
                        if (timeStr == null) timeStr = "";
                        if (statusStr == null) statusStr = "";

                        Calendar dateCal = Calendar.getInstance();
                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                dateCal.setTime(dbDateFormat.parse(dateStr));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        allAppointmentTeachers.add(
                                new AppointmentTeacher(childName, service, timeStr, dateCal, statusStr)
                        );
                    }

                    updateAppointmentsForSelectedDate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Failed to load appointments.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAppointmentsForSelectedDate() {
        List<AppointmentTeacher> filtered = new ArrayList<>();

        for (AppointmentTeacher a : allAppointmentTeachers) {
            if (sameDay(a.getDate(), selectedDate)) filtered.add(a);
        }

        appointmentTeacherAdapter.updateList(filtered);
        tvTodayTitle.setText(dateFormat.format(selectedDate.getTime()));
    }

    private boolean sameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    // --- Progress Chart Logic ---
    private void loadScoresFromFirestore() {
        scoreEntries.clear();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("scores")
                .get()
                .addOnSuccessListener(query -> {
                    ArrayList<Entry> pronunciationEntries = new ArrayList<>();
                    ArrayList<Entry> followInstructionEntries = new ArrayList<>();

                    int pronIndex = 1;
                    int followIndex = 1;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId(); // "pronunciation" or "followInstruction"
                        Double easy = doc.getDouble("easy");
                        Double medium = doc.getDouble("medium");
                        Double hard = doc.getDouble("hard");

                        if (easy != null || medium != null || hard != null) {
                            float avg = 0f;
                            int count = 0;
                            if (easy != null) { avg += easy; count++; }
                            if (medium != null) { avg += medium; count++; }
                            if (hard != null) { avg += hard; count++; }
                            if (count > 0) {
                                avg /= count;

                                if ("pronunciation".equalsIgnoreCase(id)) {
                                    pronunciationEntries.add(new Entry(pronIndex++, avg));
                                } else if ("followInstruction".equalsIgnoreCase(id)) {
                                    followInstructionEntries.add(new Entry(followIndex++, avg));
                                }
                            }
                        }
                    }

                    updateChart(pronunciationEntries, followInstructionEntries);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to load scores", e));
    }

    private void setupChart() {
        teacherProgressChart.getDescription().setEnabled(false);
        teacherProgressChart.setDrawGridBackground(false);
        teacherProgressChart.setBackgroundColor(Color.parseColor("#F0F8FF"));
        teacherProgressChart.setNoDataText("No progress data yet!");
        XAxis xAxis = teacherProgressChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setGridColor(Color.BLACK);
        xAxis.setAxisLineColor(Color.BLACK);

        YAxis leftAxis = teacherProgressChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setGridColor(Color.BLACK);
        leftAxis.setAxisLineColor(Color.BLACK);

        teacherProgressChart.getAxisRight().setEnabled(false);
        teacherProgressChart.getLegend().setTextColor(Color.WHITE);
    }

    private void updateChart(ArrayList<Entry> pronunciationEntries, ArrayList<Entry> followEntries) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        if (!pronunciationEntries.isEmpty()) {
            LineDataSet pronunciationSet = new LineDataSet(pronunciationEntries, "Pronunciation");
            pronunciationSet.setColor(ContextCompat.getColor(requireContext(), R.color.purple_500));
            pronunciationSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.purple_500));
            pronunciationSet.setLineWidth(2f);
            pronunciationSet.setCircleRadius(4f);
            pronunciationSet.setValueTextSize(10f);
            pronunciationSet.setValueTextColor(Color.WHITE);
            pronunciationSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSets.add(pronunciationSet);
        }

        if (!followEntries.isEmpty()) {
            LineDataSet followSet = new LineDataSet(followEntries, "Follow Instruction");
            followSet.setColor(ContextCompat.getColor(requireContext(), R.color.teal_700));
            followSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.teal_700));
            followSet.setLineWidth(2f);
            followSet.setCircleRadius(4f);
            followSet.setValueTextSize(10f);
            followSet.setValueTextColor(Color.WHITE);
            followSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSets.add(followSet);
        }

        if (dataSets.isEmpty()) {
            teacherProgressChart.clear();
            teacherProgressChart.setNoDataText("No progress data available");
            teacherProgressChart.invalidate();
            return;
        }

        LineData lineData = new LineData(dataSets);
        teacherProgressChart.setData(lineData);
        teacherProgressChart.invalidate();
    }


    // ------------------ Date picker ------------------

    private void setupCalendarPicker() {
        ivTodayCalendar.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();

            new DatePickerDialog(
                    getContext(),
                    (view, year, month, day) -> {
                        selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, day);

                        updateAppointmentsForSelectedDate();
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // ------------------ Random quote image ------------------

    private void showRandomQuoteImage() {
        int[] images = {
                R.drawable.pic1,
                R.drawable.pic2,
                R.drawable.pic3,
                R.drawable.pic4,
                R.drawable.pic5,
                R.drawable.pic6,
                R.drawable.pic7,
                R.drawable.pic8,
                R.drawable.pic9,
                R.drawable.pic10,
                R.drawable.pic11,
                R.drawable.pic12
        };

        ivQuoteImage.setImageResource(
                images[new Random().nextInt(images.length)]
        );
    }
}
