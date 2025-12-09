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
                    Log.d("NotifStream", "📣 Showing PUSH banner for id=" + id);


                    Log.d("NotifStream",
                            "🆕 NEW NOTIF → id=" + id
                                    + ", title=" + title);

                    NotificationHelper.showActivityDoneNotification(
                            appContext,
                            "PondMate",
                            title + " — " + message,
                            true,
                            "",
                            id
                    );
                }

            } catch (Exception e) {
                Log.e("NotifStream", "Fetch error", e);
            }
        }).start();
    }

    private void persistLastId(int id) {
        appContext.getSharedPreferences("NOTIF_PREF", Context.MODE_PRIVATE)
                .edit()
                .putInt("last_notif_id", id)
                .apply();
        Log.d("NotifStream", "💾 Saved lastNotifId=" + id);

    }
}
