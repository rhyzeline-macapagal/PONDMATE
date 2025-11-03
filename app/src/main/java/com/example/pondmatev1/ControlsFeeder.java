package com.example.pondmatev1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
    private static final String PREF_FEED = "FEED_LEVEL_PREF";

    private TextView tvRemainingFeed;
    private ImageView imgFeedLevel;
    private Button btnStoreFeeds;
    private SharedPreferences prefs;
    private String pondId;

    private final Handler timeHandler = new Handler();
    private final int[] feedingHours = {7, 12, 17};
    private final Handler liveHandler = new Handler();
    private Runnable refreshRunnable;

    private final android.content.BroadcastReceiver feedUpdateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received feed update broadcast → Refreshing UI");
            refreshFeedUI();
        }
    };

    public ControlsFeeder() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.controls_feeder, container, false);

        if (getArguments() != null) {
            pondId = getArguments().getString("pond_id");
        }
        Log.e("FEED_TEST", "ControlsFeeder RECEIVED pondId = '" + pondId + "'");
        Log.d(TAG, "✅ ControlsFeeder Loaded pondId = " + pondId);

        tvRemainingFeed = view.findViewById(R.id.tvRemainingFeed);
        imgFeedLevel = view.findViewById(R.id.imgFeedLevel);
        btnStoreFeeds = view.findViewById(R.id.btnStoreFeeds);

        btnStoreFeeds.setOnClickListener(v -> showStoreFeedDialog());

        updateRemainingFeedDisplay();

        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(feedUpdateReceiver, new android.content.IntentFilter("FEED_LEVEL_UPDATED"));

        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(feedUpdateReceiver, new android.content.IntentFilter("FEED_DEDUCTION_APPLIED"));

        prefs = requireContext().getSharedPreferences(PREF_FEED, Context.MODE_PRIVATE);

        Button btnToggleFeeder = view.findViewById(R.id.btnToggleFeeder);
        TextView feederStatusText = view.findViewById(R.id.feederStatusText);

        final String baseUrl = "http://192.168.254.100";
        final boolean[] isConnected = {false};

        timeHandler.post(timeUpdater);
        syncTimeToESP(baseUrl);
        syncFeedingTimesToESP(baseUrl);

        if (btnToggleFeeder != null) {
            btnToggleFeeder.setOnClickListener(v -> new Thread(() -> {
                try {
                    URL url = new URL(baseUrl + "/on");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            }).start());
        }

        return view;
    }

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            timeHandler.postDelayed(this, 60_000);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timeHandler.removeCallbacks(timeUpdater);
        liveHandler.removeCallbacks(refreshRunnable);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(feedUpdateReceiver);
    }

    private void showStoreFeedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Store Feeds");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter amount in grams (e.g. 5000)");
        builder.setView(input);

        builder.setPositiveButton("STORE", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an amount.", Toast.LENGTH_SHORT).show();
                return;
            }

            float addedFeed = Float.parseFloat(value);

            Log.d(TAG, "➕ Adding feed: pondId=" + pondId + " amount=" + addedFeed);
            FeedStorage.addFeed(requireContext(), pondId, addedFeed);

            refreshFeedUI();
            LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(new Intent("FEED_LEVEL_UPDATED"));

            Toast.makeText(requireContext(), "Stored " + addedFeed + "g feed!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateRemainingFeedDisplay() {
        float remaining = FeedStorage.getRemainingFeed(requireContext(), pondId);
        Log.d(TAG, "📦 Remaining feed updated → " + remaining + "g");
        tvRemainingFeed.setText(String.format("Remaining Feed: %.2f g", remaining));
    }

    private void refreshFeedUI() {
        updateRemainingFeedDisplay();
    }

    private void syncTimeToESP(String baseUrl) {}
    private void syncFeedingTimesToESP(String baseUrl) {}
}
