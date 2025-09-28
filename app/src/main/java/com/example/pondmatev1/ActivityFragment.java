package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import android.graphics.Color;

public class ActivityFragment extends Fragment {

    private ProgressBar progressBar;
    private MaterialCalendarView calendarView;
    private LinearLayout activitiesLayout;
    private Spinner calendarToggleSpinner;
    private PondModel selectedPond;

    private static final String TAG = "ActivityFragment";
    private static final String GET_POND_DATES_URL = "https://pondmate.alwaysdata.net/get_pond_dates.php";

    private final Map<String, List<ActivityItem>> activitiesByDate = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        calendarView = view.findViewById(R.id.calendarView);
        activitiesLayout = view.findViewById(R.id.activitiesLayout);
        calendarToggleSpinner = view.findViewById(R.id.calendarToggleSpinner);

        loadSelectedPond();
        setupCalendarToggle();
        setupCalendarDateClick();

        if (selectedPond != null) {
            fetchActivities(selectedPond.getId());
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

    private void fetchActivities(String pondId) {
        requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String urlStr = GET_POND_DATES_URL + "?pond_id=" + pondId;
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String jsonString = sb.toString().trim();
                Log.d(TAG, "JSON Response: " + jsonString);

                if (jsonString.isEmpty() || !jsonString.startsWith("{")) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Invalid server response", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                JSONObject response = new JSONObject(jsonString);
                boolean success = response.optBoolean("success", false);

                if (!success) {
                    String error = response.optString("error", "Unknown error");
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONArray data = response.optJSONArray("data");
                activitiesByDate.clear();

                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        String name = obj.optString("activity_name", "Unnamed");
                        String type = obj.optString("activity_type", "Unknown");
                        String date = obj.optString("scheduled_date", "");

                        ActivityItem item = new ActivityItem(name, type, date);

                        if (!activitiesByDate.containsKey(date)) {
                            activitiesByDate.put(date, new ArrayList<>());
                        }
                        activitiesByDate.get(date).add(item);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    addDotsToCalendar();

                    String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(CalendarDay.today().getDate());

                    if (activitiesByDate.containsKey(todayStr)) {
                        displayActivities(activitiesByDate.get(todayStr));
                    } else {
                        activitiesLayout.removeAllViews();
                        TextView tv = new TextView(getContext());
                        tv.setText("No activities today.");
                        activitiesLayout.addView(tv);
                    }

                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error fetching activities", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void addDotsToCalendar() {
        calendarView.removeDecorators();
        for (String dateStr : activitiesByDate.keySet()) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                if (date != null) {
                    CalendarDay day = CalendarDay.from(date);
                    calendarView.addDecorator(new DotDecorator(day));
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

            String pondId = selectedPond.getId();
            String key = pondId + "_" + item.getScheduledDate() + "_" + item.getName();
            boolean isChecked = prefs.getBoolean(key, false);
            checkBox.setChecked(isChecked);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked1) -> {
                if (!isToday) return;
                prefs.edit().putBoolean(key, isChecked1).apply();

                if (selectedPond != null) {
                    String pondName = selectedPond.getName();
                    String activityTitle = item.getName();
                    String date = item.getScheduledDate();

                    if (isChecked1) {
                        handler.sendNotification(
                                "Activity Completed",
                                "You marked \"" + activityTitle + "\" as done for " + pondName + " on " + date
                        );
                    } else {
                        handler.sendNotification(
                                "Activity Pending",
                                "\"" + activityTitle + "\" is pending again for " + pondName + " on " + date
                        );
                    }
                }
            });

            activitiesLayout.addView(checkBox);
        }
    }

    // --- Supporting classes ---
    public static class PondModel {
        private String id, name, breed, dateStarted, dateHarvest;
        private int fishCount;
        private double costPerFish;

        public PondModel(String id, String name, String breed, int fishCount, double costPerFish,
                         String dateStarted, String dateHarvest) {
            this.id = id;
            this.name = name;
            this.breed = breed;
            this.fishCount = fishCount;
            this.costPerFish = costPerFish;
            this.dateStarted = dateStarted;
            this.dateHarvest = dateHarvest;
        }

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

        @Override
        public boolean shouldDecorate(CalendarDay day) { return this.day.equals(day); }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(10, 0xFF2196F3));
        }
    }
}
