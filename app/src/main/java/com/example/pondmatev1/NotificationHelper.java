package com.example.pondmatev1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class NotificationHelper {

    private static final String CHANNEL_ID = "pondmate_channel";
    private static final String CHANNEL_NAME = "PondMate Notifications";
    private static final String CHANNEL_DESC = "Notifications for pond activities";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Show a local or broadcast notification banner.
     *
     * @param context Context
     * @param pondName Pond name (will appear as title)
     * @param activityMessage Full message to display in notification body
     * @param isLocal True if local notification (username is appended automatically), false for broadcast
     * @param username Username of the user (used only for local notification)
     */
    public static void showActivityDoneNotification(Context context, String pondName, String activityMessage, boolean isLocal, String username) {
        createNotificationChannel(context);

        String message;
        if (isLocal && username != null && !username.isEmpty()) {
            message = activityMessage + " completed by " + username;
        } else {
            // Broadcast already contains the full message
            message = activityMessage;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fish)
                .setContentTitle(pondName)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int notificationId = (int) System.currentTimeMillis(); // unique per notification
        notificationManager.notify(notificationId, builder.build());
    }
}
