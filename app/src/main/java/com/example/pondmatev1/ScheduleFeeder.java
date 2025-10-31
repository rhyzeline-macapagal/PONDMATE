package com.example.pondmatev1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFeeder extends Fragment {

    private TableLayout SummaryT;
    private static final String TAG = "SCHEDULE_FEEDER";

    private static class FeedingScheduleRow {
        String feedingDate;
        String feedingTime;
        String dfrPerFeeding;
        long timeMillis;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.schedule_feeder, container, false);
        SummaryT = view.findViewById(R.id.SummaryT);

        Log.d(TAG, "Fragment Loaded: ScheduleFeeder");

        // Load selected pond
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson == null) {
            Log.e(TAG, "No pond selected in SharedPreferences");
            Toast.makeText(getContext(), "No pond selected", Toast.LENGTH_SHORT).show();
            return view;
        }

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        Log.d(TAG, "Selected Pond: " + pond.getName() + " (ID: " + pond.getId() + ")");

        // Fetch computed feeding logs (Upcoming Schedule)
        loadFeedingLogs(pond.getId());

        return view;
    }

    private void loadFeedingLogs(String pondId) {
        Log.d(TAG, "Requesting feeding schedule logs... pond_id=" + pondId);

        Map<String, String> params = new HashMap<>();
        params.put("pond_id", pondId);

        PondSyncManager.postData("get_feeding_logs.php", params, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                String response = (String) result;
                Log.d(TAG, "API Response: " + response);

                requireActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        if ("success".equals(json.optString("status"))) {
                            JSONArray data = json.getJSONArray("data");
                            Log.d(TAG, "Parsed " + data.length() + " feeding schedule rows.");
                            populateFeedingScheduleTable(data);
                        } else {
                            Log.e(TAG, "Server returned error: " + json.optString("message"));
                            Toast.makeText(getContext(), "No schedule data found.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "JSON Parse Error", e);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "API Error: " + error);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Unable to load feeding schedule", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void populateFeedingScheduleTable(JSONArray dataArray) {
        Log.d(TAG, "Populating table with schedule data...");

        if (SummaryT == null) {
            Log.e(TAG, "SummaryT TableLayout is NULL");
            return;
        }

        // Clear old rows
        if (SummaryT.getChildCount() > 1)
            SummaryT.removeViews(1, SummaryT.getChildCount() - 1);

        List<FeedingScheduleRow> rows = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat displayTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        long now = System.currentTimeMillis();

        try {
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject o = dataArray.getJSONObject(i);

                FeedingScheduleRow r = new FeedingScheduleRow();
                r.feedingDate = o.getString("feeding_date");
                r.feedingTime = o.getString("feeding_time");
                r.dfrPerFeeding = o.getString("dfr_per_feeding");

                Date full = sdf.parse(r.feedingDate + " " + r.feedingTime);
                r.timeMillis = full.getTime();
                r.feedingTime = displayTime.format(full);
                rows.add(r);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing feeding schedule rows", e);
        }

        rows.sort((a, b) -> Long.compare(a.timeMillis, b.timeMillis));
        Log.d(TAG, "Sorted " + rows.size() + " schedule rows.");

        for (FeedingScheduleRow row : rows) {
            TableRow tr = new TableRow(getContext());
            tr.setPadding(8, 8, 8, 8);

            tr.addView(makeCell(row.feedingDate));
            tr.addView(makeCell(row.feedingTime));
            tr.addView(makeCell(row.dfrPerFeeding));

            long diff = row.timeMillis - now;
            TextView tvStatus = makeCell(diff <= 0 ? "Fed" : (diff / (1000 * 60 * 60)) + "h " + (diff / (1000 * 60) % 60) + "m left");
            tvStatus.setTextColor(diff <= 0 ? Color.GRAY : Color.parseColor("#2E7D32"));
            tr.addView(tvStatus);

            SummaryT.addView(tr);
        }

        Log.d(TAG, "Table population complete.");
    }

    private TextView makeCell(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        return tv;
    }
}
