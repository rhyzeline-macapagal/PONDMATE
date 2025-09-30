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
        String date;
        String time;
        String feedQty;
        String status;
        long millisUntil;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.schedule_feeder, container, false);

        // UI Elements
        Createbtn = view.findViewById(R.id.btnEditSchedule);
        timeFS = view.findViewById(R.id.tvFirstFeeding);
        timeFS1 = view.findViewById(R.id.tvSecondFeeding);
        timeFS2 = view.findViewById(R.id.tvThirdFeeding);
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

                                    // Populate table
//                                    populateScheduleTable(schedOne, schedTwo, schedThree, feedQty);
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


        }

        // Create button listener
        Createbtn.setOnClickListener(v -> {
            ScheduleFeederDialog dialog = new ScheduleFeederDialog();
            dialog.show(getChildFragmentManager(), "ScheduleFeederDialog");
        });

        return view;
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

