package com.example.appointable;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONObject;

public class ReminderDialogHelper {

    public static void showDueReminderDialogIfAny(Context ctx) {
        if (ctx == null) return;

        String dueId = ReminderStore.popDueId(ctx);
        if (dueId == null) return;

        JSONObject o = ReminderStore.get(ctx, dueId);
        if (o == null) return;

        String title = o.optString("title", "Reminder");
        String msg = o.optString("msg", "");

        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();

        ReminderStore.remove(ctx, dueId);
    }
}
