package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FeedLogStorage {

    private static final String PREF_NAME = "FEED_LOG_PREF";

    public static void addLog(Context context, String pondId, float amount) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String key = "logs_" + pondId;
        String json = prefs.getString(key, "[]");

        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> logs = gson.fromJson(json, type);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        logs.add(0, timestamp + " • Stored " + amount + "g");

        prefs.edit().putString(key, gson.toJson(logs)).apply();
    }

    public static List<String> getLogs(Context context, String pondId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String key = "logs_" + pondId;
        String json = prefs.getString(key, "[]");

        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
