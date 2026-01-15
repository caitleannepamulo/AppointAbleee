package com.example.appointable;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    // If you already released a channel with the same ID, Android keeps its old settings.
    // Changing the CHANNEL ID forces Android to create a fresh channel.
    // ✅ Keep App.CHANNEL_ID, but add a fallback here if you want later.
    private static final String CHANNEL_NAME = "Appointment Reminders";
    private static final String CHANNEL_DESC = "Reminds you before an appointment.";

    public static void showNotification(Context ctx, String title, String message) {
        if (ctx == null) return;

        ensureChannel(ctx);

        String safeTitle = (title == null || title.trim().isEmpty()) ? "Appointment Reminder" : title.trim();
        String safeMsg = (message == null) ? "" : message.trim();

        // ✅ Tap opens app
        Intent openIntent = new Intent(ctx, LoginActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
                ctx,
                9911,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, App.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar_pending)
                .setContentTitle(safeTitle)
                .setContentText(safeMsg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(safeMsg))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(contentIntent)
                // ✅ sound + vibration on Android 11 devices that need it
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // unique id
        int notifId = (int) (System.currentTimeMillis() & 0x7fffffff);

        NotificationManagerCompat.from(ctx).notify(notifId, b.build());
    }

    private static void ensureChannel(Context ctx) {
        if (ctx == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel existing = nm.getNotificationChannel(App.CHANNEL_ID);
        if (existing != null) return;

        NotificationChannel channel = new NotificationChannel(
                App.CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription(CHANNEL_DESC);
        channel.enableVibration(true);
        channel.enableLights(true);

        // optional custom sound (uses default notification sound)
        Uri sound = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(sound, attrs);

        nm.createNotificationChannel(channel);
    }
}
