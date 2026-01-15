package com.example.appointable;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class ReminderStore {

    private static final String PREF = "reminder_store";
    private static final String KEY_DUE = "due_id";

    private static String keyFor(String id) {
        return "rem_" + (id == null ? "" : id);
    }

    public static void save(Context ctx, String id, String title, String msg, long triggerAt, boolean due) {
        if (ctx == null) return;
        if (id == null || id.trim().isEmpty()) return;

        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        JSONObject o = new JSONObject();

        try {
            o.put("title", title == null ? "" : title);
            o.put("msg", msg == null ? "" : msg);
            o.put("triggerAt", triggerAt);
            o.put("due", due);
        } catch (JSONException ignored) {}

        sp.edit().putString(keyFor(id), o.toString()).apply();
    }

    public static void markDue(Context ctx, String id, String title, String msg, long now) {
        if (ctx == null) return;
        if (id == null || id.trim().isEmpty()) return;

        save(ctx, id, title, msg, now, true);

        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_DUE, id).apply();
    }

    public static String popDueId(Context ctx) {
        if (ctx == null) return null;

        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String id = sp.getString(KEY_DUE, null);
        if (id != null) sp.edit().remove(KEY_DUE).apply();
        return id;
    }

    public static JSONObject get(Context ctx, String id) {
        if (ctx == null) return null;
        if (id == null || id.trim().isEmpty()) return null;

        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = sp.getString(keyFor(id), null);
        if (raw == null) return null;

        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    public static void remove(Context ctx, String id) {
        if (ctx == null) return;
        if (id == null || id.trim().isEmpty()) return;

        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().remove(keyFor(id)).apply();
    }
}
