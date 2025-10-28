package com.example.pondmatev1;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class ScheduleFeeder extends Fragment {


    private Button selectDate, selectTime, Createbtn, selectTime1, selectTime2;
    private TextView dateFS, timeFS, fishqty, timeFS1, timeFS2, feedqttycont, Fweight;
    private TableLayout SummaryT;
    private boolean isCreating = true;
    private PondSharedViewModel pondSharedViewModel;
    private CheckBox checkbox;
    private boolean suppressCheckboxListener = false;
    private final List<String> savedFeedingTimes = new ArrayList<>();
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_REMEMBER = "remember_defaults";
    private static final String KEY_TIME_MAIN = "time_main";
    private static final String KEY_TIME_1 = "time_1";
    private static final String KEY_TIME_2 = "time_2";
    private static final String KEY_DYNAMIC_TIMES = "dynamic_times";
    private String pondName = "";


    private View layoutContent, layoutEmptyState;
    private Button btnAddSchedule;

    private static class ScheduleRow {
        String date;   // yyyy-MM-dd
        String time;   // hh:mm a
        String feedQty;
        long timeMillis; // exact timestamp in millis
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.schedule_feeder, container, false);

        SummaryT = view.findViewById(R.id.SummaryT);

        Createbtn = view.findViewById(R.id.btnEditSchedule);
        timeFS = view.findViewById(R.id.tvFirstFeeding);
        timeFS1 = view.findViewById(R.id.tvSecondFeeding);
        feedqttycont = view.findViewById(R.id.tvFeedQty);
        Fweight = view.findViewById(R.id.tvFeedPrice);

        TextView tvFishCount = view.findViewById(R.id.tvFishCount);
        TextView tvFishWeight = view.findViewById(R.id.tvFishWeight);
        TextView tvFeedType = view.findViewById(R.id.tvFeedType);

        // Get selected pond from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            String pondNameLocal = pond.getName();
            int fishCount = pond.getFishCount();
            tvFishCount.setText(String.valueOf(fishCount));

            PondSyncManager.fetchPondSchedule(pondNameLocal, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object result) {
                    String response = (String) result; // cast to String

                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            JSONObject json = new JSONObject(response);

                            if ("success".equals(json.getString("status"))) {

                                JSONArray schedulesArray = json.optJSONArray("schedules");
                                if (schedulesArray != null && schedulesArray.length() > 0) {

                                    JSONObject scheduleObj = schedulesArray.getJSONObject(0);

                                    String schedOne = scheduleObj.optString("sched_one", "");
                                    String schedTwo = scheduleObj.optString("sched_two", "");
                                    String schedThree = scheduleObj.optString("sched_three", "");
                                    String feedQty = scheduleObj.optString("feed_amount", "0.0") + " kg";
                                    String feedType = scheduleObj.optString("feeder_type", "Starter");
                                    String feedPrice = "₱" + scheduleObj.optString("feed_price", "0.00");
                                    String fishWeight = scheduleObj.optString("fish_weight", "0.00") + " g";


                                    tvFishWeight.setText(fishWeight);
                                    feedqttycont.setText(feedQty);
                                    tvFeedType.setText(feedType);
                                    Fweight.setText(feedPrice);

                                    if (timeFS != null) timeFS.setText(formatTime(schedOne));
                                    if (timeFS1 != null) timeFS1.setText(formatTime(schedTwo));
                                    if (timeFS2 != null) timeFS2.setText(formatTime(schedThree));

                                    // populateScheduleTable(schedOne, schedTwo, schedThree, feedQty);
                                }

                            } else {
                                Toast.makeText(getContext(),
                                        "Error: " + json.optString("message", "Unknown error"),
                                        Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(),
                                    "Parse error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to fetch schedule: " + error, Toast.LENGTH_SHORT).show());
                }
            });

            prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            pondJson = prefs.getString("selected_pond", null);

            if (pondJson != null) {
                pond = new Gson().fromJson(pondJson, PondModel.class);
                String pondName = pond.getName();

                PondSyncManager.fetchWeeklyScheduleByName(pondName, new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object result) {
                        String response = (String) result;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                JSONObject json = new JSONObject(response);
                                if ("success".equals(json.getString("status"))) {
                                    JSONArray schedulesArray = json.getJSONArray("schedules");
                                    populateWeeklyScheduleTable(schedulesArray); // call the function here
                                } else {
                                    Toast.makeText(getContext(), json.optString("message", "Error fetching schedules"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), "JSON parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                });

            }

        }

        // Create button listener
        Createbtn.setOnClickListener(v -> {
            ScheduleFeederDialog dialog = new ScheduleFeederDialog();
            dialog.show(getChildFragmentManager(), "ScheduleFeederDialog");
        });

        return view;
    }


    // ==================== Populate Table ====================


    private void populateWeeklyScheduleTable(JSONArray schedulesArray) {
        if (SummaryT == null) {
            Log.d("ScheduleFeeder", "SummaryT is null");
            return;
        }

        Log.d("ScheduleFeeder", "populateWeeklyScheduleTable called with " + schedulesArray.length() + " schedules");

        // Clear previous rows except header
        int childCount = SummaryT.getChildCount();
        if (childCount > 1) {
            SummaryT.removeViews(1, childCount - 1);
        }

        List<ScheduleRow> scheduleRows = new ArrayList<>();
        SimpleDateFormat sdfServer = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat sdfDisplay = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            for (int i = 0; i < schedulesArray.length(); i++) {
                JSONObject scheduleObj = schedulesArray.getJSONObject(i);
                Log.d("ScheduleFeeder", "Processing schedule object: " + scheduleObj.toString());

                String feedQty = scheduleObj.optString("feed_amount", "0.0") + " kg";
                String[] scheds = {
                        scheduleObj.optString("sched_one", ""),
                        scheduleObj.optString("sched_two", ""),
                        scheduleObj.optString("sched_three", "")
                };
                String dateStr = scheduleObj.optString("schedule_date", sdfDate.format(new Date()));

                String sched = scheduleObj.optString("schedule_time", "");
                if (!sched.isEmpty()) {
                    ScheduleRow row = new ScheduleRow();
                    row.date = scheduleObj.optString("schedule_date", sdfDate.format(new Date()));
                    row.feedQty = scheduleObj.optString("feed_amount", "0.0") + " kg";

                    Date dateTime = sdfServer.parse(sched);
                    Calendar cal = Calendar.getInstance();
                    String[] parts = row.date.split("-");
                    cal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                    cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                    cal.set(Calendar.HOUR_OF_DAY, dateTime.getHours());
                    cal.set(Calendar.MINUTE, dateTime.getMinutes());
                    cal.set(Calendar.SECOND, dateTime.getSeconds());
                    cal.set(Calendar.MILLISECOND, 0);

                    row.timeMillis = cal.getTimeInMillis();
                    row.time = sdfDisplay.format(cal.getTime());

                    scheduleRows.add(row);
                    Log.d("ScheduleFeeder", "Added row: date=" + row.date + " time=" + row.time + " feedQty=" + row.feedQty);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ScheduleFeeder", "Error parsing schedules: " + e.getMessage());
            Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (scheduleRows.isEmpty()) {
            Log.d("ScheduleFeeder", "No valid schedule rows to display");
            return;
        }

        // Sort by timestamp
        scheduleRows.sort((a, b) -> Long.compare(a.timeMillis, b.timeMillis));
        long now = System.currentTimeMillis();

        TableRow nextRowForCountdown = null;
        TextView nextTvCountdown = null;

        for (ScheduleRow row : scheduleRows) {
            TableRow tableRow = new TableRow(getContext());
            tableRow.setPadding(8, 8, 8, 8);

            TextView tvDate = new TextView(getContext());
            tvDate.setText(row.date);
            tvDate.setGravity(Gravity.CENTER);

            TextView tvTime = new TextView(getContext());
            tvTime.setText(row.time);
            tvTime.setGravity(Gravity.CENTER);

            TextView tvQty = new TextView(getContext());
            tvQty.setText(row.feedQty);
            tvQty.setGravity(Gravity.CENTER);

            TextView tvStatus = new TextView(getContext());
            tvStatus.setGravity(Gravity.CENTER);

            if (row.timeMillis > now && nextRowForCountdown == null) {
                nextRowForCountdown = tableRow;
                nextTvCountdown = tvStatus;
            } else {
                tvStatus.setText("Fed");
            }

            tableRow.addView(tvDate);
            tableRow.addView(tvTime);
            tableRow.addView(tvQty);
            tableRow.addView(tvStatus);

            SummaryT.addView(tableRow);
        }

        Log.d("ScheduleFeeder", "Table populated with " + scheduleRows.size() + " rows");

        // Countdown
        if (nextTvCountdown != null) {
            final TextView finalTvCountdown = nextTvCountdown;
            final long targetTime = scheduleRows.stream()
                    .filter(r -> r.timeMillis > now)
                    .findFirst()
                    .get()
                    .timeMillis;

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    long diff = targetTime - System.currentTimeMillis();
                    if (diff > 0) {
                        long hrs = diff / (1000 * 60 * 60);
                        long mins = (diff / (1000 * 60)) % 60;
                        long secs = (diff / 1000) % 60;
                        finalTvCountdown.setText(String.format("%02d:%02d:%02d", hrs, mins, secs));
                        finalTvCountdown.postDelayed(this, 1000);
                    } else {
                        finalTvCountdown.setText("Fed");
                    }
                }
            }, 0);
        }
    }



    // ==================== Helper Methods ====================
    private String formatTime(String time) {
        if (time == null || time.isEmpty()) return "-";
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date date = sdf24.parse(time);
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf12.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return time;
        }
    }

}

