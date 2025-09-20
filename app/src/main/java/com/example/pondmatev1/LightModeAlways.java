package com.example.pondmatev1;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class LightModeAlways extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Force light mode globally
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
