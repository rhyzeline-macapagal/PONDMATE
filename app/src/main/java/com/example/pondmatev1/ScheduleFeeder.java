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
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class ScheduleFeeder extends Fragment {

    private EditText Fweight, feedqttycont;
    private Button selectDate, selectTime, Createbtn, selectTime1, selectTime2;
    private TextView dateFS, timeFS, fishqty, timeFS1, timeFS2;
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

    private static class ScheduleRow {
        String date;
        String time;
        String feedQty;
        String status;
        long millisUntil;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.schedule_feeder, container, false);

        // Initialize views
        dateFS = view.findViewById(R.id.dateoffeedingschedule);
        timeFS = view.findViewById(R.id.timeoffeeding3);
        selectDate = view.findViewById(R.id.btnselectdate);
        selectTime = view.findViewById(R.id.btnselecttimes);
        Createbtn = view.findViewById(R.id.createbtn);
        SummaryT= view.findViewById(R.id.summaryTable);
        feedqttycont = view.findViewById(R.id.feedquantity);
        Fweight = view.findViewById(R.id.weight);
        fishqty = view.findViewById(R.id.tvsfishcount);
        checkbox = view.findViewById(R.id.checkBoxDefault);
        timeFS1 = view.findViewById(R.id.timeoffeeding1);
        timeFS2 = view.findViewById(R.id.timeoffeeding2);
        selectTime1 = view.findViewById(R.id.btnselecttime1);
        selectTime2 = view.findViewById(R.id.btnselecttime2);

        pondSharedViewModel = new ViewModelProvider(requireActivity()).get(PondSharedViewModel.class);
        pondSharedViewModel.getFishCount().observe(getViewLifecycleOwner(), count -> {
            computeFeedQuantityIfReady();
            fishqty.setText(String.valueOf(count));
        });

        if (pondSharedViewModel.getFishCount().getValue() != null) {
            computeFeedQuantityIfReady();
            fishqty.setText(String.valueOf(pondSharedViewModel.getFishCount().getValue()));
        }

        feedqttycont.setFocusable(false);
        feedqttycont.setClickable(false);
        feedqttycont.setCursorVisible(false);
        feedqttycont.setKeyListener(null);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        suppressCheckboxListener = true;
        checkbox.setEnabled(false);

        boolean hasDefaults = prefs.getBoolean(KEY_REMEMBER, false);
        checkbox.setChecked(hasDefaults);
        suppressCheckboxListener = false;

        if (hasDefaults) {
            timeFS.setText(prefs.getString(KEY_TIME_MAIN, ""));
            timeFS1.setText(prefs.getString(KEY_TIME_1, ""));
            timeFS2.setText(prefs.getString(KEY_TIME_2, ""));
            Set<String> savedDynamicTimes = prefs.getStringSet(KEY_DYNAMIC_TIMES, null);

            selectDate.setEnabled(false);
            selectTime1.setEnabled(false);
            selectTime2.setEnabled(false);
            selectTime.setEnabled(false);

            feedqttycont.setEnabled(false);
            Fweight.setEnabled(false);
        }

        Fweight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                computeFeedQuantityIfReady();
            }
        });

        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isCreating) return;
            if (suppressCheckboxListener) return;

            SharedPreferences inprefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (isChecked) {
                String defMain = inprefs.getString(KEY_TIME_MAIN, "");
                String def1 = inprefs.getString(KEY_TIME_1, "");
                String def2 = inprefs.getString(KEY_TIME_2, "");

                if (timeFS.getText().toString().isEmpty() && !defMain.isEmpty()) timeFS.setText(defMain);
                if (timeFS1.getText().toString().isEmpty() && !def1.isEmpty()) timeFS1.setText(def1);
                if (timeFS2.getText().toString().isEmpty() && !def2.isEmpty()) timeFS2.setText(def2);
            }
        });

        // Date & Time pickers
        selectDate.setOnClickListener(v -> showDatePicker());
        selectTime.setOnClickListener(v -> showTimePicker(timeFS));
        selectTime1.setOnClickListener(v -> showTimePicker(timeFS1));
        selectTime2.setOnClickListener(v -> showTimePicker(timeFS2));

        // Create / Save button
        Createbtn.setOnClickListener(v -> handleCreateSave());

        selectDate.setEnabled(false);
        selectTime.setEnabled(false);
        feedqttycont.setEnabled(false);
        Fweight.setEnabled(false);
        checkbox.setEnabled(false);

        return view;
    }
    private void uploadFeedingTimesToESP(String main, String t1, String t2) {
        OkHttpClient client = new OkHttpClient();
        try {
            String mainEnc = URLEncoder.encode(main, "UTF-8");
            String t1Enc = URLEncoder.encode(t1, "UTF-8");
            String t2Enc = URLEncoder.encode(t2, "UTF-8");

            String url = "http://192.168.1.7/setFeedTimes?main=" + mainEnc + "&t1=" + t1Enc + "&t2=" + t2Enc;
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to send schedule to ESP: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    final String body = (response.body() != null) ? response.body().string() : "No response body";
                    requireActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Schedule uploaded to ESP: " + body, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "ESP error: " + response.code() + " " + body, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(getContext(), "Encoding error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ----------------- Date & Time Pickers -----------------
    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view1, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    if (selected.before(Calendar.getInstance())) {
                        Toast.makeText(getContext(), "Cannot select a past date.", Toast.LENGTH_SHORT).show();
                    } else {
                        @SuppressLint("SimpleDateFormat")
                        SimpleDateFormat displayFormat =
                                new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
                        dateFS.setText(displayFormat.format(selected.getTime()));
                    }
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(now.getTimeInMillis());
        try { datePickerDialog.getDatePicker().setCalendarViewShown(false); datePickerDialog.getDatePicker().setSpinnersShown(true); } catch (Exception ignored) {}
        datePickerDialog.show();
    }

    private void showTimePicker(TextView targetTime) {
        String selectedDateStr = dateFS.getText().toString();
        if (selectedDateStr.isEmpty()) {
            Toast.makeText(getContext(), "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view12, selectedHour, selectedMinute) -> {
                    try {
                        @SuppressLint("SimpleDateFormat")
                        SimpleDateFormat parser = new SimpleDateFormat("MMM. dd, yyyy HH:mm", Locale.getDefault());
                        Calendar selectedTime = Calendar.getInstance();
                        selectedTime.setTime(parser.parse(selectedDateStr + " " + selectedHour + ":" + selectedMinute));

                        if (selectedTime.before(now)) {
                            Toast.makeText(getContext(), "Cannot select a past time.", Toast.LENGTH_SHORT).show();
                        } else {
                            Calendar displayCal = Calendar.getInstance();
                            displayCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                            displayCal.set(Calendar.MINUTE, selectedMinute);
                            @SuppressLint("SimpleDateFormat")
                            SimpleDateFormat displayFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                            targetTime.setText(displayFormat.format(displayCal.getTime()));
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Invalid date or time.", Toast.LENGTH_SHORT).show();
                    }
                },
                hour, minute, false
        );
        timePickerDialog.show();
    }

    // ----------------- Create / Save -----------------
    private void handleCreateSave() {
        if (isCreating) {
            selectDate.setEnabled(true);
            selectTime.setEnabled(true);
            selectTime1.setEnabled(true);
            selectTime2.setEnabled(true);
            Fweight.setEnabled(true);
            feedqttycont.setEnabled(false);
            checkbox.setEnabled(true);
            computeFeedQuantityIfReady();
            Createbtn.setText("Save");
            isCreating = false;
        } else {
            saveAndUploadSchedule();
        }
    }

    private void saveAndUploadSchedule() {
        String staticDate = dateFS.getText().toString();
        String staticTime = timeFS.getText().toString();
        String time1 = timeFS1.getText().toString().trim();
        String time2 = timeFS2.getText().toString().trim();
        String weight = Fweight.getText().toString().trim();

        String feedQuantityStr = feedqttycont.getText().toString().trim();
        String cleanedFeedQtyStr = feedQuantityStr.replaceAll("[^\\d.]", "");
        float feedAmount;
        try { feedAmount = Float.parseFloat(cleanedFeedQtyStr); } catch (NumberFormatException e) { feedAmount = 0.0f; }
        String feedQuantity = String.format(Locale.getDefault(), "%.2f kg", feedAmount);

        if (weight.isEmpty() || staticDate.isEmpty() || time1.isEmpty() || time2.isEmpty() || staticTime.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        float finalFeedAmount = feedAmount;
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Feeding Schedule")
                .setMessage("Do you really want to save and upload this feeding schedule?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    uploadFeedingTimesToESP(staticTime, time1, time2);
                    addTableRow(staticDate, time1, feedQuantity, getStatus(staticDate, time1));
                    addTableRow(staticDate, time2, feedQuantity, getStatus(staticDate, time2));
                    addTableRow(staticDate, staticTime, feedQuantity, getStatus(staticDate, staticTime));

                    Createbtn.setText("Create");
                    isCreating = true;

                    Toast.makeText(getContext(), "Schedule saved locally and sent to ESP", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> Toast.makeText(getContext(), "Schedule not saved", Toast.LENGTH_SHORT).show())
                .show();
    }



    // ----------------- Table Rendering -----------------
    private final List<ScheduleRow> tableRows = new ArrayList<>();
    private void addTableRow(String date, String time, String feedQty, String status) {
        ScheduleRow row = new ScheduleRow();
        row.date = date;
        row.time = time;
        row.feedQty = feedQty;
        row.status = status;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM. dd, yyyy hh:mm a", Locale.getDefault());
            Date scheduledDate = sdf.parse(date + " " + time);
            row.millisUntil = (scheduledDate != null) ? scheduledDate.getTime() - System.currentTimeMillis() : Long.MAX_VALUE;
        } catch (ParseException e) { row.millisUntil = Long.MAX_VALUE; }

        tableRows.add(row);
        sortAndRenderTable();
    }

    private void sortAndRenderTable() {
        tableRows.sort((a, b) -> Long.compare(a.millisUntil, b.millisUntil));
        SummaryT.removeViews(1, SummaryT.getChildCount() - 1);

        for (ScheduleRow r : tableRows) {
            TableRow rowView = new TableRow(getContext());
            rowView.addView(createCell(r.date));
            rowView.addView(createCell(r.time));
            rowView.addView(createCell(r.feedQty));
            rowView.addView(createCell(r.status));
            SummaryT.addView(rowView);
        }
    }

    private TextView createCell(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.parseColor("#4A4947"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(Typeface.SANS_SERIF);
        return tv;
    }

    private String getStatus(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.isEmpty() || timeStr == null || timeStr.isEmpty()) return "Incomplete";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM. dd, yyyy hh:mm a", Locale.getDefault());
            Date scheduledDate = sdf.parse(dateStr + " " + timeStr);
            if (scheduledDate == null) return "Invalid";

            long diffMillis = scheduledDate.getTime() - System.currentTimeMillis();
            if (diffMillis < 0) return "Past due";

            long totalMinutes = diffMillis / (60 * 1000);
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;

            return (hours > 0) ? hours + "hr" + (hours > 1 ? "s" : "") + ", " + minutes + "mn" + (minutes != 1 ? "s" : "") + " left"
                    : minutes + "mn" + (minutes != 1 ? "s" : "") + " left";
        } catch (ParseException e) { return "Invalid"; }
    }

    private void computeFeedQuantityIfReady() {
        String weightStr = Fweight.getText().toString().trim();
        Integer fishCount = pondSharedViewModel.getFishCount().getValue();

        if (weightStr.isEmpty() || fishCount == null || fishCount == 0) {
            feedqttycont.setText("--");
            return;
        }

        try {
            float fishWeight = Float.parseFloat(weightStr);
            float feedPercentage = 0.04f;
            float computedFeedQty = feedPercentage * fishWeight * fishCount;
            float computedKg = computedFeedQty / 1000f;
            feedqttycont.setText(String.format(Locale.getDefault(), "%.2f kg", computedKg));
        } catch (NumberFormatException e) {
            feedqttycont.setText("--");
        }
    }
}
