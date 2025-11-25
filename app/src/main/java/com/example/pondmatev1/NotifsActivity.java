package com.example.pondmatev1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashSet;
import java.util.Set;

public class NotifsActivity extends AppCompatActivity {

    private LinearLayout notifContainer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshTask;
    private final Set<Integer> displayedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifs);

        notifContainer = findViewById(R.id.notifContainer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPollingNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshTask);
    }

    private void startPollingNotifications() {
        refreshTask = new Runnable() {
            @Override
            public void run() {
                PondSyncManager.fetchNotifications(new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        try {
                            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                            if (json.get("status").getAsString().equals("success")) {
                                JsonArray data = json.getAsJsonArray("data");
                                displayNewNotifications(data);
                            }
                        } catch (Exception e) {
                            Log.e("NotifsActivity", "Error parsing notifications: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("NotifsActivity", "Error fetching notifications: " + error);
                    }
                });
                handler.postDelayed(this, 5000); // poll every 5 seconds
            }
        };
        handler.post(refreshTask);
    }

    private void displayNewNotifications(JsonArray data) {
        runOnUiThread(() -> {
            for (int i = 0; i < data.size(); i++) {
                JsonObject notif = data.get(i).getAsJsonObject();
                int notifId = notif.get("id").getAsInt();

                if (!displayedIds.contains(notifId)) {
                    displayedIds.add(notifId);

                    String title = notif.get("title").getAsString();
                    String message = notif.get("message").getAsString();
                    String date = notif.get("created_at").getAsString();
                    String pondsId = notif.get("ponds_id").getAsString();

                    TextView notifView = new TextView(NotifsActivity.this); // Use proper context
                    notifView.setText("📢 " + title + "\n" + message + "\n⏱ " + date);
                    notifView.setPadding(20, 20, 20, 20);
                    notifView.setBackgroundResource(R.drawable.notif_bg);
                    notifView.setTextSize(15);

                    notifContainer.addView(notifView);

                    // Updated to 5 arguments: Context, PondName, message/title, isLocal, username
                    NotificationHelper.showActivityDoneNotification(
                            NotifsActivity.this,  // context
                            pondsId,              // pondName or pondId (you can map to pondName if needed)
                            title,                // notification title
                            false,                // isLocal: false because it's broadcast
                            message,               // username/message included in broadcast
                            notifId           // Unique notification ID
                    );
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshTask);
    }
}
