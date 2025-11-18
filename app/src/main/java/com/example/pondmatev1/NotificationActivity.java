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
    private int lastNotifId = 0;
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds
    private static final String NOTIF_URL = "https://pondmate.alwaysdata.net/get_notifications.php";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notificationLayout = findViewById(R.id.notificationLayout);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());


        // Load stored notifications first
        ArrayList<NotificationStore.NotificationItem> saved = NotificationStore.getNotifications(this);
        displayNotifications(saved);
        fetchNewNotifications(); // immediately fetch latest on open


        // Get last saved notification ID (if any)
        if (!saved.isEmpty()) {
            lastNotifId = getLastSavedId(saved);
        }

        // Start real-time updates
        startNotificationUpdates();
    }

    private void startNotificationUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchNewNotifications();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, 1000);
    }

    private void fetchNewNotifications() {
        new Thread(() -> {
            try {
                URL url = new URL(NOTIF_URL + "?after_id=" + lastNotifId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.getString("status").equals("success")) {
                    JSONArray data = json.getJSONArray("data");
                    if (data.length() > 0) {
                        ArrayList<NotificationStore.NotificationItem> newNotifs = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject n = data.getJSONObject(i);
                            int notifId = n.getInt("id");  // ✅ track latest ID
                            String pondName = n.optString("pond_name", "Unknown Pond");
                            String message = n.optString("title", "") + ": " + n.optString("message", "");
                            String timestamp = n.optString("created_at", "");

                            NotificationStore.NotificationItem item =
                                    new NotificationStore.NotificationItem(pondName, message, timestamp);

                            newNotifs.add(0, item);

                            // ✅ update lastNotifId with the highest ID
                            if (notifId > lastNotifId) {
                                lastNotifId = notifId;
                            }
                        }

                        // Merge new + old
                        ArrayList<NotificationStore.NotificationItem> all = NotificationStore.getNotifications(this);
                        all.addAll(0, newNotifs);
                        NotificationStore.saveNotifications(this, all);

                        runOnUiThread(() -> displayNotifications(all));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private int getLastSavedId(ArrayList<NotificationStore.NotificationItem> saved) {
        // In this version, we’ll just return 0 since local items don’t store an ID.
        // The server sync will handle updating `lastNotifId` when new ones are fetched.
        return 0;
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
        String today = sdf.format(new Date()); // e.g., "2025-11-04"

        for (NotificationStore.NotificationItem notif : notifications) {
            // Create CardView
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

            // Horizontal layout for blue bar + content
            LinearLayout horizontalLayout = new LinearLayout(this);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

            // Determine if notification is new
            boolean isNew = notif.timestamp.startsWith(today); // today = "YYYY-MM-DD"

            // Blue side bar
            View sideBar = new View(this);
            LinearLayout.LayoutParams sideBarParams = new LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT);
            sideBar.setLayoutParams(sideBarParams);
            sideBar.setBackgroundColor(Color.parseColor("#2196F3")); // Blue
            sideBar.setVisibility(isNew ? View.VISIBLE : View.GONE); // Only show if new

            // Vertical layout for text content
            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setPadding(24, 16, 24, 16);
            textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // Pond name
            TextView pondView = new TextView(this);
            pondView.setText(notif.pondName);
            pondView.setTextSize(14f);
            pondView.setAlpha(0.7f);

            // Notification message
            TextView messageView = new TextView(this);
            messageView.setText("🔔 " + notif.message);
            messageView.setTextSize(16f);
            messageView.setTypeface(Typeface.DEFAULT_BOLD);
            messageView.setPadding(0, 8, 0, 8);

            // Timestamp
            TextView timeView = new TextView(this);
            timeView.setText(notif.timestamp);
            timeView.setTextSize(12f);
            timeView.setGravity(Gravity.END);
            timeView.setAlpha(0.6f);

            // Add views to text layout
            textLayout.addView(pondView);
            textLayout.addView(messageView);
            textLayout.addView(timeView);

            // Add side bar + text layout to horizontal layout
            horizontalLayout.addView(sideBar);
            horizontalLayout.addView(textLayout);

            // Add horizontal layout to card
            card.addView(horizontalLayout);

            // Add card to parent layout
            notificationLayout.addView(card);
        }



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
