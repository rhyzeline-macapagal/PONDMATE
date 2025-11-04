package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class NotificationStore {

    public static class NotificationItem {
        public String pondName;
        public String message;
        public String timestamp;

        public NotificationItem(String pondName, String message, String timestamp) {
            this.pondName = pondName;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    private static final String PREFS_NAME = "NOTIFICATION_PREFS";
    private static final String KEY_NOTIFICATIONS = "notifications";

    // Save notifications locally
    public static void saveNotifications(Context context, ArrayList<NotificationItem> notifications) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(notifications);
        editor.putString(KEY_NOTIFICATIONS, json);
        editor.apply();
    }

    // Retrieve notifications
    public static ArrayList<NotificationItem> getNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTIFICATIONS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<ArrayList<NotificationItem>>() {}.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception e) {
            Log.e("NotificationStore", "Error parsing stored notifications: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Add new notification on top
    public static void addNotification(Context context, NotificationItem notif) {
        ArrayList<NotificationItem> list = getNotifications(context);
        list.add(0, notif); // newest first
        saveNotifications(context, list);
    }
}
