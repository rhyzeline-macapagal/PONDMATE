package com.example.pondmatev1;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NotificationHandler {

    private Context context;
    private static final String CHANNEL_ID = "pondmate_channel";

    public NotificationHandler(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    // Send a notification immediately
    public void sendNotification(String title, String message) {
        String dateStr = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                .format(new Date());

        Intent intent = new Intent(context, NotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
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

        saveNotificationToPrefs(title, message, dateStr);
    }

    private void saveNotificationToPrefs(String title, String message, String dateStr) {
        SharedPreferences prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE);
        Set<String> notifications = new HashSet<>(prefs.getStringSet("notif_list", new HashSet<>()));

        String entry = title + " - " + message + " (" + dateStr + ")";
        notifications.add(entry);

        prefs.edit().putStringSet("notif_list", notifications).apply();
    }

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

    // Schedule a feeding reminder notification
    public void scheduleFeedingReminder(String activityName, String pondName, String scheduledDate) {
        try {
            Calendar calendar = Calendar.getInstance();

            // Parse the scheduled date
            Date activityDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(scheduledDate);
            if (activityDate == null) return;

            // Set reminder one day before at 8 AM
            calendar.setTime(activityDate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 8);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            long triggerAtMillis = calendar.getTimeInMillis();
            int requestCode = (scheduledDate + activityName).hashCode();

            // Create intent for BroadcastReceiver
            Intent intent = new Intent(context, FeedingReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );


            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
