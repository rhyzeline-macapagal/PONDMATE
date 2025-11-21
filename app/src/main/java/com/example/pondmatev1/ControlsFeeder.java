package com.example.pondmatev1;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private View rootView;

    private final BroadcastReceiver feedUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ("FEED_LEVEL_UPDATED".equals(action)) {
                Log.d(TAG, "📥 FEED_LEVEL_UPDATED received → STORE event");
                updateRemainingFeedDisplay();
            }
            else if ("FEED_DEDUCTION_APPLIED".equals(action)) {
                Log.d(TAG, "📤 FEED_DEDUCTION_APPLIED received → DEDUCT event");
                updateRemainingFeedDisplay();
            }
        }
    };


    public ControlsFeeder() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.controls_feeder, container, false);
        View view = rootView;

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

        loadFeedHistory();
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
        Log.d(TAG, "📥 Store Feed dialog opened for pondId=" + pondId);

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

            Log.d(TAG, "✅ USER CONFIRMED STORE FEEDS | pondId=" + pondId + " | amount=" + addedFeed + "g");

            // 1) Add feed locally
            FeedStorage.addFeed(requireContext(), pondId, addedFeed);

            // 2) Get accurate updated remaining feed
            float remainingAfter = FeedStorage.getRemainingFeed(requireContext(), pondId);

            // 3) Log & Sync ONE TIME with correct remaining value
            FeedStorage.sendRemainingToServer(requireContext(), pondId, remainingAfter);

            // 4) Refresh UI & notify components
            refreshFeedUI();
            loadFeedHistory();
            LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(new Intent("FEED_LEVEL_UPDATED"));

            Toast.makeText(requireContext(), "Stored " + addedFeed + "g feed!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Log.d(TAG, "❌ Store Feed dialog canceled");
            dialog.dismiss();
        });

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

    private void loadFeedHistory() {
        if (pondId == null || pondId.isEmpty()) {
            Log.e("FEED_HISTORY", "❌ pondId is NULL — cannot load logs.");
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_feed_storage_logs.php?pond_id=" + pondId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("FEED_HISTORY", "🌐 Server Response: " + response);

                JSONArray jsonArray = new JSONArray(response.toString());
                List<String> data = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    String action = obj.getString("action_type");  // STORE or DEDUCT
                    String amount = obj.getString("amount");
                    String remaining = obj.getString("remaining_after");
                    String date = obj.getString("created_at");

                    String entry;

                    if (action.equalsIgnoreCase("STORE")) {
                        entry = "Stored " + amount + "g"
                                + " | Remaining: " + remaining + "g\n"
                                + date;
                    } else { // DEDUCT
                        entry = "Deducted " + amount + "g"
                                + " | Remaining: " + remaining + "g\n"
                                + date;
                    }

                    data.add(entry);
                }

                requireActivity().runOnUiThread(() -> {
                    RecyclerView logRecycler = rootView.findViewById(R.id.feedLogRecycler);
                    logRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
                    logRecycler.setAdapter(new FeedLogAdapter(requireContext(), data));

                    Log.d("FEED_HISTORY", "✅ Displayed " + data.size() + " logs.");
                });

            } catch (Exception e) {
                Log.e("FEED_HISTORY", "❌ Error: " + e.getMessage());
            }
        }).start();
    }


}
