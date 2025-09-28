package com.example.pondmatev1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

public class FeedingReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "FeedingReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String pondName = intent.getStringExtra("pond_name");
        String feedingLabel = intent.getStringExtra("feeding_label");
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);

        if (pondName == null) pondName = "Pond";
        if (feedingLabel == null) feedingLabel = "Feeding";

        Log.d(TAG, "onReceive -> pond=" + pondName + ", label=" + feedingLabel
                + ", hour=" + hour + ", minute=" + minute + ", now=" + System.currentTimeMillis());

        // Use your NotificationHandler to show and save the notification
        NotificationHandler handler = new NotificationHandler(context);
        handler.sendNotification("Feeding Reminder - " + pondName, "Time for " + feedingLabel + "!");

        // --- Reschedule next day's alarm (so it runs daily) ---
        if (hour >= 0 && minute >= 0) {
            Calendar next = Calendar.getInstance();
            next.set(Calendar.HOUR_OF_DAY, hour);
            next.set(Calendar.MINUTE, minute);
            next.set(Calendar.SECOND, 0);
            // schedule for tomorrow
            next.add(Calendar.DAY_OF_YEAR, 1);

            Intent nextIntent = new Intent(context, FeedingReminderReceiver.class);
            nextIntent.putExtra("pond_name", pondName);
            nextIntent.putExtra("feeding_label", feedingLabel);
            nextIntent.putExtra("hour", hour);
            nextIntent.putExtra("minute", minute);

            int requestCode = (pondName + feedingLabel + hour + minute).hashCode();
            PendingIntent nextPi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), nextPi);
                Log.d(TAG, "Rescheduled alarm for next day: " + feedingLabel + " at "
                        + String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                        + " reqCode=" + requestCode);
            } else {
                Log.e(TAG, "AlarmManager null when rescheduling");
            }
        } else {
            Log.w(TAG, "Missing hour/minute extras; cannot reschedule next day.");
        }
    }
}
