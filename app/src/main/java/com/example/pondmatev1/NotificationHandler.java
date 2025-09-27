package com.example.pondmatev1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class NotificationHandler {

    private Context context;
    private ImageView notificationIcon;
    private static final String CHANNEL_ID = "pondmate_channel";

    public NotificationHandler(Context context, ImageView notificationIcon) {
        this.context = context;
        this.notificationIcon = notificationIcon;
        init();
        createNotificationChannel();
    }

    private void init() {
        if (notificationIcon != null) {
            notificationIcon.setOnClickListener(v -> {
                // Show a Toast
                Toast.makeText(context, "Notification clicked!", Toast.LENGTH_SHORT).show();

                // Open NotificationActivity
                openNotificationActivity();
            });
        }
    }

    private void openNotificationActivity() {
        Intent intent = new Intent(context, NotificationActivity.class);
        context.startActivity(intent);
    }

    // 🔔 New method to send a system notification
    public void sendNotification(String title, String message) {
        // --- Send System Notification ---
        Intent intent = new Intent(context, NotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());

        // --- Save to SharedPreferences for NotificationActivity ---
        saveNotificationToPrefs(title, message);
    }

    private void saveNotificationToPrefs(String title, String message) {
        SharedPreferences prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        long timestamp = System.currentTimeMillis();
        String entry = title + " - " + message + " (" + new Date(timestamp).toString() + ")";

        Set<String> notifications = new HashSet<>(prefs.getStringSet("notif_list", new HashSet<>()));
        notifications.add(entry);

        editor.putStringSet("notif_list", notifications);
        editor.apply();
    }


    // Needed for Android 8+
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "PondMate Channel";
            String description = "Notifications for PondMate activities";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
