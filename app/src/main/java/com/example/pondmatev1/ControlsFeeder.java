package com.example.pondmatev1;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ControlsFeeder extends Fragment {

    private TextView feedleveltv;
    private Button monitorbtn;
    private ImageView feedlvlicon;
    private TextView textCurrentDate;
    private TextView lastFeedingTimeTV;
    private TextView nextFeedingTimeTV;
    private final Handler timeHandler = new Handler();

    private int currentIndex = 0;
    // Feeding schedule hours (24h format)
    private final int[] feedingHours = {7, 12, 17}; // 7AM, 12PM, 5PM
    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));

            // Update current date & time display
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy - hh:mm a", Locale.ENGLISH);
            dateTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
            String currentDateTime = dateTimeFormat.format(calendar.getTime());
            if (textCurrentDate != null) {
                textCurrentDate.setText(currentDateTime);
            }

            // Update feeding schedule times
            updateFeedingTimes(calendar);

            // Schedule next update in 60 seconds
            timeHandler.postDelayed(this, 60000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.controls_feeder, container, false);


        // Update feeding times immediately on load
        updateFeedingTimes(Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila")));

        // Start periodic time and feeding times update
        timeHandler.post(timeUpdater);


        Button btnToggleFeeder = view.findViewById(R.id.btnToggleFeeder);
        TextView feederStatusText = view.findViewById(R.id.feederStatusText);

        final String baseUrl = "http://192.168.254.100"; // your ESP IP
        final boolean[] isConnected = {false};

        // Initial sync
        syncTimeToESP(baseUrl);
        syncFeedingTimesToESP(baseUrl);

        btnToggleFeeder.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    URL url = new URL(baseUrl + "/on");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    int responseCode = connection.getResponseCode();
                    connection.disconnect();

                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;

                            if (responseCode == 200) {
                                isConnected[0] = true;
                                btnToggleFeeder.setText("Connected");
                                btnToggleFeeder.setEnabled(false);
                                feederStatusText.setText("Status: Connected ✅");

                                // Manual resync after connection
                                syncTimeToESP(baseUrl);
                                syncFeedingTimesToESP(baseUrl);
                            } else {
                                feederStatusText.setText("Error: HTTP " + responseCode);
                            }
                        });
                    }

                } catch (Exception e) {
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            feederStatusText.setText("Connection failed: " + e.getMessage());
                        });
                    }
                }
            }).start();

        });



        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timeHandler.removeCallbacks(timeUpdater);
    }

    private void updateFeedingTimes(Calendar now) {
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        // Find last feeding time
        int lastHour = feedingHours[feedingHours.length - 1];
        for (int hour : feedingHours) {
            if (hour <= currentHour) {
                lastHour = hour;
            }
        }

        // Find next feeding time
        int nextHour = feedingHours[0];
        for (int hour : feedingHours) {
            if (hour > currentHour) {
                nextHour = hour;
                break;
            }
        }

        // If current time is after last feeding hour, next feeding is tomorrow 7 AM
        if (currentHour >= feedingHours[feedingHours.length - 1]) {
            nextHour = feedingHours[0];
        }

        String lastTimeStr = formatHourTo12(lastHour);
        String nextTimeStr = formatHourTo12(nextHour);

        if (lastFeedingTimeTV != null && nextFeedingTimeTV != null) {
            lastFeedingTimeTV.setText(lastTimeStr);
            nextFeedingTimeTV.setText(nextTimeStr);
            System.out.println("Last feeding: " + lastTimeStr + ", Next feeding: " + nextTimeStr);
        } else {
            System.out.println("Error: lastFeedingTimeTV or nextFeedingTimeTV is null!");
        }
    }

    private String formatHourTo12(int hour24) {
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        String ampm = (hour24 >= 12) ? "PM" : "AM";
        return String.format(Locale.ENGLISH, "%02d:00 %s", hour12, ampm);
    }

    // Sync time to ESP
    private void syncTimeToESP(String baseUrl) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

        String dateStr = dateFormat.format(calendar.getTime());
        String timeStr = timeFormat.format(calendar.getTime());

        String syncUrl = baseUrl + "/sync?date=" + dateStr + "&time=" + timeStr;

        new Thread(() -> {
            try {
                URL url = new URL(syncUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                int responseCode = connection.getResponseCode();
                connection.disconnect();

                // ✅ check muna bago mag UI thread
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        if (responseCode == 200) {
                            System.out.println("✅ Time synced to ESP.");
                        } else {
                            System.out.println("❌ Time sync failed: HTTP " + responseCode);
                        }
                    });
                }

            } catch (Exception e) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        System.out.println("⚠ Sync error: " + e.getMessage());
                    });
                }
            }
        }).start();

    }

    // New method: sync feeding times to ESP as comma separated string, e.g. "07,12,17"
    private void syncFeedingTimesToESP(String baseUrl) {
        // Prepare feeding times string
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < feedingHours.length; i++) {
            sb.append(String.format("%02d", feedingHours[i]));
            if (i < feedingHours.length - 1) {
                sb.append(",");
            }
        }
        String feedingTimesStr = sb.toString();

        String syncUrl = baseUrl + "/setFeedTimes?times=" + feedingTimesStr;

        new Thread(() -> {
            try {
                URL url = new URL(syncUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                int responseCode = connection.getResponseCode();
                connection.disconnect();

                // ✅ check muna kung attached bago mag-UI update
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        if (responseCode == 200) {
                            System.out.println("✅ Feeding times synced to ESP: " + feedingTimesStr);
                        } else {
                            System.out.println("❌ Feeding times sync failed: HTTP " + responseCode);
                        }
                    });
                }

            } catch (Exception e) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        System.out.println("⚠ Sync error: " + e.getMessage());
                    });
                }
            }
        }).start();

    }
}
