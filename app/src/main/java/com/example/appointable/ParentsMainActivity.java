package com.example.appointable;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class ParentsMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // fragment mapping
        fragmentMap.put(R.id.nav_home, new ParentHomeFragment());
        fragmentMap.put(R.id.nav_appointments, new AppointmentsFragment());
        fragmentMap.put(R.id.nav_messages, new ParentMessagesFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment frag = fragmentMap.get(item.getItemId());
            if (frag != null) {
                loadFragment(frag);

                // clear badge when Messages tab is opened
                if (item.getItemId() == R.id.nav_messages) {
                    updateMessagesBadge(0);
                }
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        // ✅ show due reminder dialog (if any) when main screen opens
        ReminderDialogHelper.showDueReminderDialogIfAny(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ show due reminder dialog again if user comes back to app
        ReminderDialogHelper.showDueReminderDialogIfAny(this);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // chat uses these
    public void hideBottomNav() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    public void showBottomNav() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }

    public void updateMessagesBadge(int unreadCount) {
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_messages);

        if (unreadCount <= 0) {
            badge.clearNumber();
            badge.setVisible(false);
            return;
        }

        badge.setVisible(true);
        badge.setNumber(unreadCount);
        badge.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        );
        badge.setBadgeTextColor(
                ContextCompat.getColor(this, android.R.color.white)
        );
    }
}
