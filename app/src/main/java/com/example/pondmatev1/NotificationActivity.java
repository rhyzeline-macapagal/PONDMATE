package com.example.pondmatev1;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class NotificationActivity extends AppCompatActivity {

    private LinearLayout notificationLayout;
    private final Handler handler = new Handler();
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds
    private static final String NOTIF_URL = "https://pondmate.alwaysdata.net/get_notifications.php";

    private ArrayList<NotificationItem> allNotifications = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notificationLayout = findViewById(R.id.notificationLayout);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Fetch from server immediately
        fetchNotifications();

        // Start polling
        startNotificationUpdates();
    }

    private void startNotificationUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchNotifications();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, 1000);
    }

    private void fetchNotifications() {
        new Thread(() -> {
            try {
                URL url = new URL(NOTIF_URL + "?limit=70");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject root = new JSONObject(response.toString());
                if (!root.getString("status").equals("success")) return;

                JSONArray data = root.getJSONArray("notifications");
                ArrayList<NotificationItem> freshNotifications = new ArrayList<>();

                // Get selected pond from SharedPreferences
                String pondName = "Pond";
                try {
                    String pondJson = getSharedPreferences("POND_PREF", MODE_PRIVATE)
                            .getString("selected_pond", null);
                    if (pondJson != null) {
                        PondModel pond = new com.google.gson.Gson().fromJson(pondJson, PondModel.class);
                        if (pond != null) {
                            pondName = pond.getName();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < data.length(); i++) {
                    JSONObject n = data.getJSONObject(i);

                    String title = n.optString("title", "");
                    String message = n.optString("message", "");
                    String timestamp = n.optString("created_at", ""); // Use created_at
                    String timeAgo = formatTimeAgo(timestamp);

                    if (!title.isEmpty() && !message.isEmpty()) {
                        NotificationItem notifItem = new NotificationItem(
                                pondName,
                                title + ": " + message,
                                timeAgo
                        );
                        freshNotifications.add(notifItem);
                    }
                }

                allNotifications = freshNotifications;

                runOnUiThread(() -> displayNotifications(allNotifications));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String formatTimeAgo(String timestamp) {
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // treat server time as UTC

            java.util.Date notifDate = sdf.parse(timestamp);
            long diff = System.currentTimeMillis() - notifDate.getTime();

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) return "Just now";
            else if (minutes < 60) return minutes + " mins ago";
            else if (hours < 24) return hours + " hrs ago";
            else return new java.text.SimpleDateFormat(
                        "MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()
                ).format(notifDate);
        } catch (Exception e) {
            return "";
        }
    }



    private void displayNotifications(ArrayList<NotificationItem> notifications) {
        notificationLayout.removeAllViews();

        if (notifications == null || notifications.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No notifications yet.");
            empty.setPadding(20, 20, 20, 20);
            empty.setTextSize(16f);
            notificationLayout.addView(empty);
            return;
        }

        int index = 0;

        for (NotificationItem notif : notifications) {

            boolean isNewest = (index == 0);

            CardView card = new CardView(this);
            card.setRadius(18f);
            card.setCardElevation(isNewest ? 12f : 6f);
            card.setUseCompatPadding(true);
            card.setCardBackgroundColor(isNewest ? Color.parseColor("#FFF3E0") : Color.WHITE);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 24);
            card.setLayoutParams(cardParams);

            LinearLayout horizontalLayout = new LinearLayout(this);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

            // LEFT BAR
            View sideBar = new View(this);
            LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(12, LinearLayout.LayoutParams.MATCH_PARENT);
            sideBar.setLayoutParams(sideParams);
            sideBar.setBackgroundColor(isNewest ? Color.parseColor("#FF9800") : Color.TRANSPARENT);

            // TEXT LAYOUT
            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setPadding(24, 16, 24, 16);

            TextView pondView = new TextView(this);
            pondView.setText(notif.pondName);
            pondView.setTextSize(14f);
            pondView.setAlpha(0.7f);
            if (isNewest) pondView.setTypeface(Typeface.DEFAULT_BOLD);

            TextView messageView = new TextView(this);
            messageView.setText("🔔 " + notif.message);
            messageView.setTextSize(16f);
            messageView.setPadding(0, 8, 0, 8);
            if (isNewest) messageView.setTypeface(Typeface.DEFAULT_BOLD);

            TextView timeView = new TextView(this);
            timeView.setText(notif.timestamp);
            timeView.setTextSize(12f);
            timeView.setGravity(Gravity.END);
            timeView.setAlpha(0.6f);

            textLayout.addView(pondView);
            textLayout.addView(messageView);
            textLayout.addView(timeView);

            horizontalLayout.addView(sideBar);
            horizontalLayout.addView(textLayout);
            card.addView(horizontalLayout);

            notificationLayout.addView(card);

            index++;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // Simple model class
    private static class NotificationItem {
        String pondName;
        String message;
        String timestamp;

        NotificationItem(String pondName, String message, String timestamp) {
            this.pondName = pondName;
            this.message = message;
            this.timestamp = timestamp;
        }
    }


}
