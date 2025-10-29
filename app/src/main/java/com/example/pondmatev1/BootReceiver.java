package com.example.pondmatev1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Device booted — re-scheduling alarms");
            // TODO: fetch scheduled notifications from server or local DB
            // and re-schedule alarms using AlarmManager
        }
    }
}
