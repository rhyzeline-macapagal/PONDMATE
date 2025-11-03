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

    private TextView txtRemainingFeed;
    private ImageView imgFeedLevel;
    private Button btnAddFeedStock;

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

        txtRemainingFeed = view.findViewById(R.id.txtRemainingFeed);
        imgFeedLevel = view.findViewById(R.id.imgFeedLevel);
        btnAddFeedStock = view.findViewById(R.id.btnAddFeedStock);

        // Single SharedPreferences instance (source of truth)
        prefs = requireContext().getSharedPreferences(PREF_FEED, Context.MODE_PRIVATE);

        // Initialize UI with stored values
        updateFeedUI();

        // Wire add-feed button
        if (btnAddFeedStock != null) {
            btnAddFeedStock.setOnClickListener(v -> openAddFeedDialog());
        }

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
    public void onResume() {
        super.onResume();

        // Start live UI refresh (every 3 seconds)
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    updateFeedUI();
                } catch (Exception e) {
                    Log.w(TAG, "Live refresh update error", e);
                } finally {
                    liveHandler.postDelayed(this, 3000);
                }
            }
        };
        liveHandler.post(refreshRunnable);
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

    // ------------------------
    // Add Feed Dialog
    // ------------------------
    private void openAddFeedDialog() {
        if (!isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Feed to Container (g)");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter amount in grams");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("ADD", (dialog, which) -> {
            String txt = input.getText().toString().trim();
            if (txt.isEmpty()) return;

            int added;
            try {
                added = Integer.parseInt(txt);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Robust read — try int, if type mismatch occurs migrate from float
            int current = readRemainingFeedSafely();
            int newTotal = current + added;
            if (newTotal > MAX_CAPACITY) newTotal = MAX_CAPACITY;

            prefs.edit().putInt(KEY_REMAINING_FEED, newTotal).apply();
            Log.d(TAG, "Manual add: +" + added + "g | New total: " + newTotal + "g");
            updateFeedUI();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ------------------------
    // Public deduction API
    // Other classes (ScheduleFeeder) should call this when a feed event is confirmed (status == "Fed")
    // You may pass the dfr value (grams) from schedule row.
    // ------------------------
    public void deductFeedOnFed(float dfrAmount) {
        if (!isAdded()) return;

        // READ current feed safely
        int previous = readRemainingFeedSafely();
        int deduct = Math.round(dfrAmount);
        int newRemaining = previous - deduct;
        if (newRemaining < 0) newRemaining = 0;

        // ✅ LOG EVERYTHING
        Log.d("FEED_DEDUCTION",
                "FEED EVENT → Previous: " + previous + "g | Deduct: "
                        + deduct + "g | New Remaining: " + newRemaining + "g");

        prefs.edit().putInt(KEY_REMAINING_FEED, newRemaining).apply();

        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateFeedUI);
        }
    }

    public void deductFeedOnFed() {
        deductFeedOnFed((float) dfrPerFeeding);
    }

    // Optional setter so another component can update the current dfrPerFeeding value
    public void setDfrPerFeeding(int dfr) {
        this.dfrPerFeeding = dfr;
    }

    // ------------------------
    // Utility: robust read that handles stored ints or legacy floats
    // ------------------------
    private int readRemainingFeedSafely() {
        // Try to read as int first. If app previously saved a float and getInt throws,
        // catch the ClassCastException and read as float then convert.
        try {
            return prefs.getInt(KEY_REMAINING_FEED, 0);
        } catch (ClassCastException e) {
            // previously stored as float
            try {
                float oldFloat = prefs.getFloat(KEY_REMAINING_FEED, 0f);
                int converted = Math.round(oldFloat);
                prefs.edit().putInt(KEY_REMAINING_FEED, converted).apply();
                Log.d(TAG, "Migrated old float remaining_feed (" + oldFloat + ") -> int " + converted);
                return converted;
            } catch (ClassCastException ex) {
                // Unexpected; fallback to 0 but log
                Log.w(TAG, "Unexpected preference type for remaining_feed", ex);
                return 0;
            }
        }
    }

    private void updateFeedUI() {
        if (!isAdded()) return;

        int remaining = readRemainingFeedSafely();

        if (txtRemainingFeed != null) {
            txtRemainingFeed.setText("Remaining Feed: " + remaining + " g");
        }

        double pct = (MAX_CAPACITY > 0) ? (remaining / (double) MAX_CAPACITY) * 100.0 : 0.0;

        if (imgFeedLevel != null) {
            if (pct > 60.0) {
                imgFeedLevel.setImageResource(R.drawable.feed_full);
            } else if (pct > 30.0) {
                imgFeedLevel.setImageResource(R.drawable.feed_half);
            } else {
                imgFeedLevel.setImageResource(R.drawable.feed_low);
            }
        }

        Log.d(TAG, "UI Updated — Remaining: " + remaining + "g (" + String.format("%.1f", pct) + "%)");
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
