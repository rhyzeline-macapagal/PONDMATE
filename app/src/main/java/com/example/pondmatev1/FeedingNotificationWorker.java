package com.example.pondmatev1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class FeedingNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "feeding_schedule_channel";

    public FeedingNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String pondName = getInputData().getString("pondName");
        String feedingLabel = getInputData().getString("feedingLabel");

        if (pondName == null || feedingLabel == null) {
            return Result.failure();
        }

        sendNotification(pondName, feedingLabel);

        return Result.success();
    }

    private void sendNotification(String pondName, String feedingLabel) {
        Context context = getApplicationContext();

        String title = "Feeding Reminder – " + pondName;
        String message = "It's time for " + feedingLabel;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Feeding Schedule",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Use pondName.hashCode() + feedingLabel.hashCode() as unique ID
        notificationManager.notify((pondName + feedingLabel).hashCode(), builder.build());
    }
}
