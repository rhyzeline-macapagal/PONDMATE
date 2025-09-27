package com.example.pondmatev1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Arrays;
import java.util.List;

public class PendingActivityWorker extends Worker {

    private static final String CHANNEL_ID = "pending_activities_channel";

    public PendingActivityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // ✅ Get data passed in when scheduling
        int pondId = getInputData().getInt("pondId", -1);
        String pondName = getInputData().getString("pondName");
        String[] activitiesArray = getInputData().getStringArray("activities");

        if (activitiesArray == null || activitiesArray.length == 0) {
            return Result.failure();
        }

        List<String> activities = Arrays.asList(activitiesArray);

        // ✅ Send notification
        sendNotification(pondId, pondName, activities);

        return Result.success();
    }

    private void sendNotification(int pondId, String pondName, List<String> activities) {
        Context context = getApplicationContext();

        // Title & message
        String title = "Pending Activities – " + pondName;
        String message = TextUtils.join(", ", activities);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pending Activities",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: replace with your app icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show notification (use pondId as unique notification ID)
        notificationManager.notify(pondId, builder.build());
    }
}
