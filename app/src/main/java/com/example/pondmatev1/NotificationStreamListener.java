package com.example.pondmatev1;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotificationStreamListener {

    private static NotificationStreamListener instance;
    private final Context appContext;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int lastNotifId = 0;

    private static final int INTERVAL = 4000;
    private static final String URL_NOTIF =
            "https://pondmate.alwaysdata.net/get_notifications.php";

    private NotificationStreamListener(Context ctx) {
        appContext = ctx.getApplicationContext();

        // ✅ restore last received id (IMPORTANT)
        lastNotifId = appContext
                .getSharedPreferences("NOTIF_PREF", Context.MODE_PRIVATE)
                .getInt("last_notif_id", 0);
    }

    public static synchronized NotificationStreamListener getInstance(Context ctx) {
        if (instance == null) {
            instance = new NotificationStreamListener(ctx);
        }
        return instance;
    }

    public void start() {
        Log.d("NotifStream", "🚀 Stream listener STARTED");
        handler.post(streamRunnable);
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
    }

    private final Runnable streamRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("NotifStream", "🔄 Poll tick – listener alive");
            fetchAndDispatch();
            handler.postDelayed(this, INTERVAL);
        }
    };

    private void fetchAndDispatch() {
        new Thread(() -> {
            Log.d("NotifStream", "🌐 Fetching notifications from server...");

            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(URL_NOTIF).openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                in.close();

                JsonObject root = JsonParser
                        .parseString(sb.toString())
                        .getAsJsonObject();
                Log.d("NotifStream", "📨 Server response = " + sb.toString());


                JsonArray arr = root.getAsJsonArray("notifications");
                if (arr == null) return;

                for (int i = 0; i < arr.size(); i++) {
                    JsonObject n = arr.get(i).getAsJsonObject();
                    int id = n.get("id").getAsInt();

                    if (id <= lastNotifId) continue;

                    lastNotifId = id;
                    Log.d("NotifStream", "📦 Parsing notifications (lastNotifId=" + lastNotifId + ")");

                    persistLastId(id);

                    String title = n.get("title").getAsString();
                    String message = n.get("message").getAsString();
                    String timestamp = n.get("created_at").getAsString();
                    int pondsId = n.get("ponds_id").getAsInt();

                    String pondName = getPondNameFromPrefs();
                    String timeAgo = formatTimeAgo(timestamp);


                    Log.d("NotifStream", "📣 Showing PUSH banner for id=" + id);


                    Log.d("NotifStream",
                            "🆕 NEW NOTIF → id=" + id
                                    + ", title=" + title);

                    NotificationHelper.showActivityDoneNotification(
                            appContext,
                            pondName,                  // Pond name from SharedPreferences
                            title + ": " + message,    // Notification content
                            true,                      // Some flag (keep as is)
                            "",                        // No time ago
                            id                         // Notification ID
                    );


                }

            } catch (Exception e) {
                Log.e("NotifStream", "Fetch error", e);
            }
        }).start();
    }
    private String getPondNameFromPrefs() {
        try {
            String pondJson = appContext
                    .getSharedPreferences("POND_PREF", Context.MODE_PRIVATE)
                    .getString("selected_pond", null);

            if (pondJson != null) {
                // Deserialize using Gson
                PondModel pond = new com.google.gson.Gson().fromJson(pondJson, PondModel.class);
                if (pond != null) {
                    return pond.getName(); // return the pond name directly
                }
            }
        } catch (Exception e) {
            Log.e("NotifStream", "Failed to read pond name", e);
        }

        return "Pond: "; // fallback
    }

    private void persistLastId(int id) {
        appContext.getSharedPreferences("NOTIF_PREF", Context.MODE_PRIVATE)
                .edit()
                .putInt("last_notif_id", id)
                .apply();
        Log.d("NotifStream", "💾 Saved lastNotifId=" + id);
    }

    private String formatTimeAgo(String timestamp) {
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

            java.util.Date notifDate = sdf.parse(timestamp);
            long diff = System.currentTimeMillis() - notifDate.getTime();

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " mins ago";
            } else if (hours < 24) {
                return hours + " hrs ago";
            } else {
                return new java.text.SimpleDateFormat(
                        "MMM dd, yyyy • hh:mm a",
                        java.util.Locale.getDefault()
                ).format(notifDate);
            }
        } catch (Exception e) {
            return "";
        }
    }

}
