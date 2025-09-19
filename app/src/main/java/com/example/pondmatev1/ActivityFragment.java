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


public class  ActivityFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private LinearLayout activitiesLayout;
    private Spinner calendarToggleSpinner;
    private PondModel selectedPond;

    private static final String TAG = "ActivityFragment";
    private static final String GET_POND_DATES_URL = "https://pondmate.alwaysdata.net/get_pond_dates.php";

    // Map to store date -> list of activities
    private final Map<String, List<ActivityItem>> activitiesByDate = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

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
        } else {
            Log.d(TAG, "No selected pond found in SharedPreferences");
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
            if (items != null) {
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
        new Thread(() -> {
            try {
                String urlStr = GET_POND_DATES_URL + "?pond_id=" + pondId;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String jsonString = sb.toString();
                Log.d(TAG, "JSON Response: " + jsonString);

                JSONObject response = new JSONObject(jsonString);
                boolean success = response.optBoolean("success", false);

                if (success) {
                    JSONArray data = response.getJSONArray("data");
                    List<ActivityItem> allActivities = new ArrayList<>();

                    // Clear map
                    activitiesByDate.clear();

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        String name = obj.getString("activity_name");
                        String type = obj.getString("activity_type");
                        String date = obj.getString("scheduled_date");
                        ActivityItem item = new ActivityItem(name, type, date);

                        allActivities.add(item);

                        // Add to map for date grouping
                        if (!activitiesByDate.containsKey(date)) {
                            activitiesByDate.put(date, new ArrayList<>());
                        }
                        activitiesByDate.get(date).add(item);
                    }

                    // Add blue dots to calendar
                    requireActivity().runOnUiThread(() -> {
                        addDotsToCalendar();
                        // Initially display today’s activities if available
                        CalendarDay today = CalendarDay.today();
                        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.getDate());
                        if (activitiesByDate.containsKey(todayStr)) {
                            displayActivities(activitiesByDate.get(todayStr));
                        } else {
                            activitiesLayout.removeAllViews();
                        }
                    });

                } else {
                    String error = response.optString("error", "Unknown error");
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error fetching activities", Toast.LENGTH_SHORT).show());
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

    private void displayActivities(List<ActivityItem> activities) {
        activitiesLayout.removeAllViews();
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);

        for (ActivityItem item : activities) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(item.getName() + " (" + item.getScheduledDate() + ")");
            cb.setTextColor(Color.BLACK);
            String key = selectedPond.getId() + "_" + item.getScheduledDate() + "_" + item.getName();
            cb.setChecked(prefs.getBoolean(key, false));

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(key, isChecked);
                editor.apply();
            });

            activitiesLayout.addView(cb);
        }
    }

    // --- Supporting classes inside the fragment ---

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

    // --- Decorator class for blue dot ---
    private static class DotDecorator implements DayViewDecorator {
        private final CalendarDay day;

        DotDecorator(CalendarDay day) {
            this.day = day;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return this.day.equals(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(10, 0xFF2196F3)); // Blue dot
        }
    }
}
