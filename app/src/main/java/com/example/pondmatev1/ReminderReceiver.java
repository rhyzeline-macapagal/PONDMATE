package com.example.pondmatev1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            int notifId = intent.getIntExtra("notif_id", NotifsHelper.NOTIF_ID);
            String title = intent.getStringExtra("title");
            String message = intent.getStringExtra("message");

            // show pinned notification - notifId should match DB id if possible
            NotifsHelper.showPersistentNotification(ctx, title, message, notifId);

            // Optionally: inform server that notification was delivered (call PHP to set delivered=1)
            // sendDeliveryPingToServer(notifId); // implement if needed
        } catch (Exception e) {
            Log.e("ReminderReceiver", "onReceive error: " + e.getMessage());
        }
    }
}
