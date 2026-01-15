package com.example.appointable;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderScheduler {

    public static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    public static void schedule24HoursBefore(
            Context context,
            long appointmentMillis,
            int requestCode,
            String title,
            String message,
            String appointmentId,
            String roleLabel
    ) {
        if (context == null) return;

        long triggerAt = appointmentMillis - ONE_DAY_MS;

        // If already passed, don't schedule
        if (triggerAt <= System.currentTimeMillis()) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("appointmentId", appointmentId == null ? "" : appointmentId);
        intent.putExtra("title", title == null ? "Appointment Reminder" : title);
        intent.putExtra("message", message == null ? "" : message);
        intent.putExtra("roleLabel", roleLabel == null ? "" : roleLabel);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancelReminder(Context context, int requestCode) {
        if (context == null) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        am.cancel(pi);
        pi.cancel();
    }
}
