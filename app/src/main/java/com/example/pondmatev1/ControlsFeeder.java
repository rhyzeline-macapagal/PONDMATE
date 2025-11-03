package com.example.pondmatev1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ControlsFeeder extends Fragment {

    private static final String TAG = "ControlsFeeder";

    private TextView tvRemainingFeed;
    private ImageView imgFeedLevel;
    private Button btnStoreFeeds;

    // Shared prefs (single source of truth) — matches ScheduleFeeder which reads/writes "FEED_STORAGE"
    private SharedPreferences prefs;
    private static final String PREF_FEED = "FEED_STORAGE";
    private static final String KEY_REMAINING_FEED = "remaining_feed";

    // Config (match ScheduleFeeder's maxCapacity)
    private static final int MAX_CAPACITY = 8000;
    private int dfrPerFeeding = 0; // fallback/default; will be set from schedule when available

    // Time & schedule update
    private final Handler timeHandler = new Handler();
    private final int[] feedingHours = {7, 12, 17}; // 7AM, 12PM, 5PM

    // Live UI refresh handler
    private final Handler liveHandler = new Handler();
    private Runnable refreshRunnable;

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            try {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy - hh:mm a", Locale.ENGLISH);
                dateTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                // (no UI field here for datetime in this fragment — keep as placeholder)
            } catch (Exception e) {
                Log.w(TAG, "timeUpdater error", e);
            } finally {
                timeHandler.postDelayed(this, 60_000);
            }
        }
    };

    public ControlsFeeder() {
        // required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.controls_feeder, container, false);

        tvRemainingFeed = view.findViewById(R.id.tvRemainingFeed);
        imgFeedLevel = view.findViewById(R.id.imgFeedLevel);
        btnStoreFeeds = view.findViewById(R.id.btnStoreFeeds);

        // Single SharedPreferences instance (source of truth)
        prefs = requireContext().getSharedPreferences(PREF_FEED, Context.MODE_PRIVATE);

        // Existing ESP / toggle button logic (preserve behavior)
        Button btnToggleFeeder = view.findViewById(R.id.btnToggleFeeder);
        TextView feederStatusText = view.findViewById(R.id.feederStatusText);

        final String baseUrl = "http://192.168.254.100"; // your ESP IP
        final boolean[] isConnected = {false};

        // Update feeding time labels immediately and start periodic updater
        timeHandler.post(timeUpdater);

        // Try initial syncs (non-blocking)
        syncTimeToESP(baseUrl);
        syncFeedingTimesToESP(baseUrl);

        // Preserve existing toggle behavior, but keep it safe and non-blocking
        if (btnToggleFeeder != null) {
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
                                    if (feederStatusText != null)
                                        feederStatusText.setText("Status: Connected ✅");

                                    // Manual resync after connection
                                    syncTimeToESP(baseUrl);
                                    syncFeedingTimesToESP(baseUrl);
                                } else {
                                    if (feederStatusText != null)
                                        feederStatusText.setText("Error: HTTP " + responseCode);
                                }
                            });
                        }
                    } catch (Exception e) {
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                if (feederStatusText != null)
                                    feederStatusText.setText("Connection failed: " + e.getMessage());
                            });
                        }
                    }
                }).start();
            });
        }

        return view;
    }


    @Override
    public void onPause() {
        super.onPause();
        // stop live refresh
        liveHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timeHandler.removeCallbacks(timeUpdater);
        liveHandler.removeCallbacks(refreshRunnable);
    }

    private void syncTimeToESP(String baseUrl) {
        new Thread(() -> {
            try {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

                String urlStr = baseUrl + "/sync?date=" + dateFormat.format(calendar.getTime()) + "&time=" + timeFormat.format(calendar.getTime());
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                conn.disconnect();

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // optional UI feedback could go here
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "syncTimeToESP error", e);
            }
        }).start();
    }

    private void syncFeedingTimesToESP(String baseUrl) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < feedingHours.length; i++) {
                    sb.append(String.format(Locale.ENGLISH, "%02d", feedingHours[i]));
                    if (i < feedingHours.length - 1) sb.append(",");
                }
                String urlStr = baseUrl + "/setFeedTimes?times=" + sb.toString();
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                conn.disconnect();

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // optional UI feedback could go here
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "syncFeedingTimesToESP error", e);
            }
        }).start();
    }
}
