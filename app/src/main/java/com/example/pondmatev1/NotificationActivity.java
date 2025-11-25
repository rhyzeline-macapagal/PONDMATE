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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private LinearLayout notificationLayout;
    private final Handler handler = new Handler();
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds
    private static final String NOTIF_URL = "https://pondmate.alwaysdata.net/get_notifications.php";

    private ArrayList<NotificationStore.NotificationItem> allNotifications = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notificationLayout = findViewById(R.id.notificationLayout);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Load stored notifications first
        allNotifications = NotificationStore.getNotifications(this);
        sortNotifications();
        displayNotifications(allNotifications);

        // Immediately fetch latest notifications
        fetchLatestNotifications();

        // Start periodic updates
        startNotificationUpdates();
    }

    private void startNotificationUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchLatestNotifications();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, 1000);
    }

    private void fetchLatestNotifications() {
        new Thread(() -> {
            try {
                URL url = new URL(NOTIF_URL + "?limit=20");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONArray data = new JSONArray(response.toString());
                ArrayList<NotificationStore.NotificationItem> newNotifs = new ArrayList<>();

                // Get the latest timestamp to avoid duplicates
                String lastTimestamp = allNotifications.isEmpty() ? "" : allNotifications.get(0).timestamp;

                for (int i = 0; i < data.length(); i++) {
                    JSONObject n = data.getJSONObject(i);
                    String pondName = n.optString("pondName", "Unknown Pond");
                    String notifMessage = n.optString("title", "") + ": " + n.optString("message", "");
                    String scheduledFor = n.optString("created_at", "");

                    if (scheduledFor.compareTo(lastTimestamp) > 0) {
                        NotificationStore.NotificationItem notifItem =
                                new NotificationStore.NotificationItem(pondName, notifMessage, scheduledFor);
                        NotificationStore.addNotification(this, notifItem); // Add to storage
                        newNotifs.add(notifItem);
                    }
                }

                if (!newNotifs.isEmpty()) {
                    // Add new notifications to the top of the list
                    allNotifications.addAll(0, newNotifs);
                    sortNotifications();

                    runOnUiThread(() -> displayNotifications(allNotifications));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sortNotifications() {
        // Sort notifications newest first
        allNotifications.sort((n1, n2) -> n2.timestamp.compareTo(n1.timestamp));
    }

    private void displayNotifications(ArrayList<NotificationStore.NotificationItem> notifications) {
        notificationLayout.removeAllViews();

        if (notifications == null || notifications.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No notifications yet.");
            empty.setPadding(20, 20, 20, 20);
            empty.setTextSize(16f);
            notificationLayout.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        for (NotificationStore.NotificationItem notif : notifications) {
            CardView card = new CardView(this);
            card.setRadius(16f);
            card.setCardElevation(6f);
            card.setUseCompatPadding(true);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 24);
            card.setLayoutParams(cardParams);

            LinearLayout horizontalLayout = new LinearLayout(this);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

            boolean isNew = notif.timestamp.startsWith(today);

            View sideBar = new View(this);
            LinearLayout.LayoutParams sideBarParams = new LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT);
            sideBar.setLayoutParams(sideBarParams);
            sideBar.setBackgroundColor(Color.parseColor("#2196F3"));
            sideBar.setVisibility(isNew ? View.VISIBLE : View.GONE);

            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setPadding(24, 16, 24, 16);
            textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView pondView = new TextView(this);
            pondView.setText(notif.pondName);
            pondView.setTextSize(14f);
            pondView.setAlpha(0.7f);

            TextView messageView = new TextView(this);
            messageView.setText("🔔 " + notif.message);
            messageView.setTextSize(16f);
            messageView.setTypeface(Typeface.DEFAULT_BOLD);
            messageView.setPadding(0, 8, 0, 8);

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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
