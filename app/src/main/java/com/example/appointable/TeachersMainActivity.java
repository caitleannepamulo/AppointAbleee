package com.example.appointable;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class TeachersMainActivity extends AppCompatActivity {

    private View customNavBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        if (savedInstanceState == null) {
            loadFragment(new TeacherHomeFragment());
        }

        customNavBar = findViewById(R.id.custom_navbar);

        setupCustomNav();

        // ✅ show due reminder dialog (if any) when main screen opens
        ReminderDialogHelper.showDueReminderDialogIfAny(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ show due reminder dialog again if user comes back to app
        ReminderDialogHelper.showDueReminderDialogIfAny(this);
    }

    private void setupCustomNav() {
        Button btnHome = findViewById(R.id.btn_home);
        Button btnAppointments = findViewById(R.id.btn_appointments);
        Button btnStudents = findViewById(R.id.btn_students);
        Button btnGame = findViewById(R.id.btn_game);
        Button btnMessages = findViewById(R.id.btn_messages);
        Button btnProfile = findViewById(R.id.btn_profile);

        btnHome.setOnClickListener(v -> loadFragment(new TeacherHomeFragment()));
        btnAppointments.setOnClickListener(v -> loadFragment(new Schedule_TeacherFragment()));
        btnStudents.setOnClickListener(v -> loadFragment(new Student_TeacherFragment()));
        btnMessages.setOnClickListener(v -> loadFragment(new Messages_TeacherFragment()));
        btnProfile.setOnClickListener(v -> loadFragment(new Profile_TeacherFragment()));

        btnGame.setOnClickListener(v -> loadFragment(new TaskSelectionFragment()));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void hideBottomNav() {
        if (customNavBar != null) {
            customNavBar.animate()
                    .translationY(customNavBar.getHeight())
                    .setDuration(200)
                    .start();
        }
    }

    public void showBottomNav() {
        if (customNavBar != null) {
            customNavBar.animate()
                    .translationY(0)
                    .setDuration(200)
                    .start();
        }
    }

    public void updateMessagesBadge(int count) {
        View msgButton = findViewById(R.id.btn_messages);
        // View msgBadge = findViewById(R.id.msg_badge); // create a small view in XML for badge visibility

        /* if (msgButton != null && msgBadge != null) {
            msgBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        } */
    }
}
