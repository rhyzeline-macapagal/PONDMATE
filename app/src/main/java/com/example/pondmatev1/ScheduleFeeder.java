package com.example.pondmatev1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class ScheduleFeeder extends Fragment {

    private static final String TAG = "SCHEDULE_FEEDER";
    private TableLayout SummaryT;

    private static class FeedingScheduleRow {
        String feedingDate;
        String feedingTimeDisplay;
        String originalTime;   // <-- used for saving to server
        String dfrPerFeeding;
        long timeMillis;
    }

    private boolean wasDeducted(String key) {
        SharedPreferences prefs = requireContext().getSharedPreferences("FEED_DEDUCTIONS", Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    private void markDeducted(String key) {
        SharedPreferences prefs = requireContext().getSharedPreferences("FEED_DEDUCTIONS", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, true).apply();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "Fragment Loaded: ScheduleFeeder");

        View view = inflater.inflate(R.layout.schedule_feeder, container, false);
        SummaryT = view.findViewById(R.id.SummaryT);

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson == null) {
            Toast.makeText(getContext(), "No pond selected", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No pond selected in shared prefs");
            return view;
        }

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        Log.d(TAG, "Selected Pond: " + pond.getName() + " (ID: " + pond.getId() + ")");

        Map<String, String> params = new HashMap<>();
        params.put("pond_id", pond.getId());
        Log.d(TAG, "Requesting feeding schedule logs... pond_id=" + pond.getId());

        PondSyncManager.postData("get_feeding_logs.php", params, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                String response = (String) result;
                Log.d(TAG, "API Response: " + response);

                // ✅ Prevent IllegalStateException when fragment is detached
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment not attached — skipping UI update");
                    return;
                }

                // ✅ Safe way to post UI updates
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if ("success".equals(json.optString("status"))) {
                            populateFeedingScheduleTable(json.getJSONArray("data"), pond.getId());
                        } else {
                            Toast.makeText(getContext(), "No schedule found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "JSON Parse Error", e);
                    }
                });
            }


            @Override
            public void onError(String error) {
                Log.e(TAG, "API Request Failed: " + error);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Unable to load schedule", Toast.LENGTH_SHORT).show());
            }
        });

        return view;
    }


    private void populateFeedingScheduleTable(JSONArray dataArray, String pondId) {

        if (SummaryT == null) return;

        if (SummaryT.getChildCount() > 1)
            SummaryT.removeViews(1, SummaryT.getChildCount() - 1);

        List<FeedingScheduleRow> rows = new ArrayList<>();
        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat showTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        long now = System.currentTimeMillis();

        try {
            for (int i = 0; i < dataArray.length(); i++) {

                JSONObject o = dataArray.getJSONObject(i);
                FeedingScheduleRow r = new FeedingScheduleRow();

                r.feedingDate = o.getString("feeding_date");
                r.originalTime = o.getString("feeding_time");  // store original time for DB insert
                r.dfrPerFeeding = o.getString("dfr_per_feeding");

                Date dt = fullFormat.parse(r.feedingDate + " " + r.originalTime);
                r.timeMillis = dt.getTime();
                r.feedingTimeDisplay = showTime.format(dt);

                rows.add(r);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing schedule rows", e);
        }

        rows.sort((a, b) -> Long.compare(a.timeMillis, b.timeMillis));

        for (FeedingScheduleRow row : rows) {
            TableRow tr = new TableRow(getContext());
            tr.setPadding(8, 8, 8, 8);

            tr.addView(makeCell(row.feedingDate));
            tr.addView(makeCell(row.feedingTimeDisplay));
            tr.addView(makeCell(row.dfrPerFeeding + " g"));

            long diff = row.timeMillis - now;

            String deductionKey = pondId + "_" + row.feedingDate + "_" + row.originalTime;

            String statusText;
            if (diff <= 0) {
                statusText = "Fed";

                // ✅ Deduct feed ONLY once
                if (!wasDeducted(deductionKey)) {
                    try {
                        // parse dfr_per_feeding (may be decimal) and round to nearest gram
                        float feedUsedFloat = Float.parseFloat(row.dfrPerFeeding);
                        int feedUsed = Math.round(feedUsedFloat);

                        SharedPreferences stockPrefs = requireContext().getSharedPreferences("FEED_STORAGE", Context.MODE_PRIVATE);
                        int stock = stockPrefs.getInt("remaining_feed", 0);

                        stock -= feedUsed;
                        if (stock < 0) stock = 0;

                        stockPrefs.edit().putInt("remaining_feed", stock).apply();
                        markDeducted(deductionKey);

                        updateFeedFunnelLevel(stock);

                        Log.d(TAG, "Deducted " + feedUsed + "g. Remaining: " + stock + "g");

                    } catch (Exception e) {
                        Log.e(TAG, "Deduction error: ", e);
                    }
                }

            } else {
                statusText = (diff / (1000 * 60 * 60)) + "h " + ((diff / (1000 * 60)) % 60) + "m left";
            }

            TextView status = makeCell(statusText);
            status.setTextColor(diff <= 0 ? Color.GRAY : Color.parseColor("#2E7D32"));
            tr.addView(status);

            SummaryT.addView(tr);
        }

        Log.d(TAG, "Table pop complete. Uploading to DB...");

        uploadScheduleToServer(rows, pondId);
    }


    private void uploadScheduleToServer(List<FeedingScheduleRow> rows, String pondId) {
        try {
            JSONArray array = new JSONArray();
            for (FeedingScheduleRow r : rows) {
                JSONObject obj = new JSONObject();
                obj.put("pond_id", pondId);
                obj.put("feeding_date", r.feedingDate);
                obj.put("feeding_time", r.originalTime);
                obj.put("dfr_per_feeding", r.dfrPerFeeding);
                obj.put("logged_by", "system-auto");
                array.put(obj);
            }

            Map<String, String> params = new HashMap<>();
            params.put("data", array.toString());

            Log.d(TAG, "Uploading " + array.length() + " rows to server...");

            PondSyncManager.postData("save_feeding_schedule.php", params, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Upload Success → " + result);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Upload Failed: " + error);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Upload Build Error", e);
        }
    }

    private TextView makeCell(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        return tv;
    }

    private void updateFeedFunnelLevel(int currentStock) {
        float maxCapacity = 8000f;

        float levelPercent = currentStock / maxCapacity;
        if (levelPercent < 0) levelPercent = 0;
        if (levelPercent > 1) levelPercent = 1;

        ImageView img = getActivity().findViewById(R.id.imgFeedLevel);
        if (img == null) return;

        Drawable d = img.getDrawable();
        if (d instanceof LayerDrawable) {
            Drawable clip = ((LayerDrawable) d).getDrawable(0);
            clip.setLevel((int) (levelPercent * 10000));
        }
    }

}
