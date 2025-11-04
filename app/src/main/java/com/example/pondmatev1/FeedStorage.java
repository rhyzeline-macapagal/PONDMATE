package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class FeedStorage {

    private static final String PREF_NAME = "FEED_LEVEL_PREF";

    public static float getRemainingFeed(Context context, String pondId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat("feed_remaining_" + pondId, 0f);
    }

    public static void setRemainingFeed(Context context, String pondId, float value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putFloat("feed_remaining_" + pondId, Math.max(value, 0)).apply();
    }

    public static void addFeed(Context context, String pondId, float amount) {
        float current = getRemainingFeed(context, pondId);
        setRemainingFeed(context, pondId, current + amount);
    }

    public static void deductFeed(Context context, String pondId, float amount) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        float current = prefs.getFloat("feed_remaining_" + pondId, 0f);

        Log.d("FEED_STORAGE", "Current feed before deduction: " + current + "g");
        Log.d("FEED_STORAGE", "Amount to deduct: " + amount + "g");

        float newLevel = Math.max(0, current - amount);

        prefs.edit().putFloat("feed_remaining_" + pondId, newLevel).apply();

        Log.d("FEED_STORAGE", "New stored feed level: " + newLevel + "g");

        // notify UI components that feed changed
        context.sendBroadcast(new Intent("FEED_LEVEL_UPDATED"));
    }

}
