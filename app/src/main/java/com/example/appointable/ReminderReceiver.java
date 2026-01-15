package com.example.appointable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String appointmentId = intent.getStringExtra("appointmentId");
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String roleLabel = intent.getStringExtra("roleLabel");

        if (title == null) title = "Appointment Reminder";
        if (message == null) message = "";
        if (roleLabel == null) roleLabel = "";

        // Save due reminder so we can show in-app dialog next open
        ReminderStore.markDue(context, appointmentId, title, message, System.currentTimeMillis());

        // Show system notification
        String fullMsg = roleLabel.isEmpty() ? message : (roleLabel + ": " + message);

        NotificationHelper.showNotification(context, title, fullMsg);
    }
}
