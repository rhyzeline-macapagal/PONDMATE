package com.example.pondmatev1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Locale;

public class FeedingReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String pondName = intent.getStringExtra("pond_name");
        String feedingLabel = intent.getStringExtra("feeding_label");
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);

        // Debug log to confirm trigger
        Log.d("FeedingReminderReceiver", "Triggered -> " + pondName + " " + feedingLabel + " at " + timeStr);

        // Use your existing NotificationHandler
        NotificationHandler handler = new NotificationHandler(context);
        handler.sendNotification(
                "Feeding Reminder",
                pondName + " - Time for " + feedingLabel + " at " + timeStr
        );
    }
}
