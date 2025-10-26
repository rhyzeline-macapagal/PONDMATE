package com.example.pondmatev1;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private LinearLayout activitiesLayout;
    private Spinner calendarToggleSpinner;
    private PondModel selectedPond;

    private static final String TAG = "ActivityFragment";
    private static final String GET_POND_DATES_URL = "https://pondmate.alwaysdata.net/get_pond_dates.php";

    private final Map<String, List<ActivityItem>> activitiesByDate = new HashMap<>();
    private Thread currentFetchThread;
    private ProgressDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        activitiesLayout = view.findViewById(R.id.activitiesLayout);
        calendarToggleSpinner = view.findViewById(R.id.calendarToggleSpinner);

        loadingDialog = new ProgressDialog(getContext());
        loadingDialog.setMessage("Please wait while the calendar is loading...");
        loadingDialog.setCancelable(false);

        loadSelectedPond();
        setupCalendarToggle();
        setupCalendarDateClick();
        setupCalendarMonthChange();

        if (selectedPond != null) {
            CalendarDay today = CalendarDay.today();
            fetchActivities(selectedPond.getId(), today.getMonth() + 1, today.getYear());
        } else {
            Toast.makeText(getContext(), "No pond selected", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadSelectedPond() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson != null) {
            selectedPond = new Gson().fromJson(pondJson, PondModel.class);
            Log.d(TAG, "Loaded pond ID: " + selectedPond.getId());
        }
    }

    private void setupCalendarToggle() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Show Calendar", "Hide Calendar"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calendarToggleSpinner.setAdapter(adapter);

        calendarToggleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calendarView.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupCalendarDateClick() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            String clickedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.getDate());
            List<ActivityItem> items = activitiesByDate.get(clickedDate);
            if (items != null && !items.isEmpty()) {
                displayActivities(items);
            } else {
                activitiesLayout.removeAllViews();
                TextView tv = new TextView(getContext());
                tv.setText("No activities on this date.");
                activitiesLayout.addView(tv);
            }
        });
    }

    private void setupCalendarMonthChange() {
        calendarView.setOnMonthChangedListener((widget, date) -> {
            if (selectedPond != null) {
                int month = date.getMonth() + 1;
                int year = date.getYear();
                fetchActivities(selectedPond.getId(), month, year);
            }
        });
    }

    private void fetchActivities(String pondId, int month, int year) {
        requireActivity().runOnUiThread(() -> loadingDialog.show());

        // Cancel ongoing fetch
        if (currentFetchThread != null && currentFetchThread.isAlive()) {
            currentFetchThread.interrupt();
        }

        currentFetchThread = new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String urlStr = GET_POND_DATES_URL + "?pond_id=" + pondId + "&month=" + month + "&year=" + year;
                conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.interrupted()) return;
                    sb.append(line);
                }
                reader.close();

                String jsonString = sb.toString().trim();
                if (jsonString.isEmpty() || !jsonString.startsWith("{")) {
                    showError("Invalid server response");
                    return;
                }

                JSONObject response = new JSONObject(jsonString);
                if (!response.optBoolean("success", false)) {
                    showError(response.optString("error", "Unknown error"));
                    return;
                }

                JSONArray data = response.optJSONArray("data");
                if (data == null) data = new JSONArray();

                Map<String, List<ActivityItem>> newMap = new HashMap<>();
                for (int i = 0; i < data.length(); i++) {
                    if (Thread.interrupted()) return;
                    JSONObject obj = data.getJSONObject(i);
                    String name = obj.optString("activity_name", "Unnamed");
                    String type = obj.optString("activity_type", "Unknown");
                    String dateStr = obj.optString("scheduled_date", "");
                    newMap.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(new ActivityItem(name, type, dateStr));
                }

                requireActivity().runOnUiThread(() -> {
                    activitiesByDate.clear();
                    activitiesByDate.putAll(newMap);
                    addDotsToCalendar();

                    String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Calendar.getInstance().getTime());
                    if (activitiesByDate.containsKey(todayStr)) {
                        displayActivities(activitiesByDate.get(todayStr));
                    } else {
                        activitiesLayout.removeAllViews();
                        TextView tv = new TextView(getContext());
                        tv.setText("No activities today.");
                        activitiesLayout.addView(tv);
                    }

                    loadingDialog.dismiss();
                });

            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) e.printStackTrace();
                showError("Error fetching activities");
            } finally {
                if (conn != null) conn.disconnect();
            }
        });

        currentFetchThread.start();
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            loadingDialog.dismiss();
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void addDotsToCalendar() {
        calendarView.removeDecorators();
        for (String dateStr : activitiesByDate.keySet()) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                if (date != null) {
                    calendarView.addDecorator(new DotDecorator(CalendarDay.from(date)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void displayActivities(List<ActivityItem> items) {
        activitiesLayout.removeAllViews();
        if (items == null || items.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("No activities on this date.");
            activitiesLayout.addView(tv);
            return;
        }

        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("ActivityPrefs", Context.MODE_PRIVATE);
        NotificationHandler handler = new NotificationHandler(context);

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        for (ActivityItem item : items) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(item.getName());

            boolean isToday = todayStr.equals(item.getScheduledDate());
            checkBox.setEnabled(isToday);
            checkBox.setTextColor(isToday ? Color.BLACK : Color.GRAY);

            String key = selectedPond.getId() + "_" + item.getScheduledDate() + "_" + item.getName();
            checkBox.setChecked(prefs.getBoolean(key, false));

            checkBox.setOnCheckedChangeListener((buttonView, checked) -> {
                if (!isToday) return;
                prefs.edit().putBoolean(key, checked).apply();
                String pondName = selectedPond.getName();
                String msg = checked ?
                        "You marked \"" + item.getName() + "\" as done for " + pondName + "." :
                        "\"" + item.getName() + "\" is pending again for " + pondName + ".";
                handler.sendNotification(checked ? "Activity Completed" : "Activity Pending", msg);
            });

            activitiesLayout.addView(checkBox);
        }
    }

    // --- Supporting Classes ---
    public static class PondModel {
        private String id, name, breed, dateStarted, dateHarvest;
        private int fishCount;
        private double costPerFish;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getBreed() { return breed; }
        public String getDateStarted() { return dateStarted; }
        public String getDateHarvest() { return dateHarvest; }
        public int getFishCount() { return fishCount; }
        public double getCostPerFish() { return costPerFish; }
    }

    public static class ActivityItem {
        private final String name, type, scheduledDate;
        public ActivityItem(String name, String type, String scheduledDate) {
            this.name = name;
            this.type = type;
            this.scheduledDate = scheduledDate;
        }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getScheduledDate() { return scheduledDate; }
    }

    private static class DotDecorator implements DayViewDecorator {
        private final CalendarDay day;
        DotDecorator(CalendarDay day) { this.day = day; }
        @Override public boolean shouldDecorate(CalendarDay d) { return day.equals(d); }
        @Override public void decorate(DayViewFacade view) {
            view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(10, 0xFF2196F3));
        }
    }
}
