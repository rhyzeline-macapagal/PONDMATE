package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.Scanner;

public class ActivityFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private Spinner calendarToggleSpinner;
    private ListView activitiesListView;
    private TextView activitiesHeader;
    private SharedPreferences prefs;

    private String selectedBreed;
    private Date startDate;

    private final Map<CalendarDay, List<String>> activityMap = new HashMap<>();
    private final Map<String, Boolean> completedActivities = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        calendarToggleSpinner = view.findViewById(R.id.calendarToggleSpinner);
        activitiesListView = view.findViewById(R.id.activitiesListView);
        activitiesHeader = view.findViewById(R.id.activitiesHeader);

        prefs = requireActivity().getSharedPreferences("SharedData", Context.MODE_PRIVATE);

        selectedBreed = prefs.getString("fish_breed", "Tilapia");
        String startDateStr = prefs.getString("date_started", null);

        // Parse date using multiple possible formats
        startDate = parseDate(startDateStr);
        if (startDate == null) {
            startDate = new Date(); // fallback
        }

        fetchActivitiesFromServer();

        // Spinner setup
        calendarToggleSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.calendar_toggle_options)));

        calendarToggleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // Hide calendar
                    calendarView.setVisibility(View.GONE);
                } else { // Show calendar
                    calendarView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            showActivitiesForDate(date);
        });

        // Select start date by default
        CalendarDay initialDay = CalendarDay.from(startDate);
        calendarView.setSelectedDate(initialDay);
        showActivitiesForDate(initialDay);

        loadCompletedActivities();

        return view;
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null) return null;
        List<SimpleDateFormat> formats = Arrays.asList(
                new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault()), // e.g. Jul. 28, 2025
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())     // e.g. 2025-07-28
        );
        for (SimpleDateFormat sdf : formats) {
            try {
                return sdf.parse(dateStr);
            } catch (ParseException ignored) { }
        }
        return null;
    }

    private void fetchActivitiesFromServer() {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_dates.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                Scanner sc = new Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                sc.close();

                String json = sb.toString();
                System.out.println("🔥 RAW SERVER RESPONSE: " + json);

                JSONObject root = new JSONObject(json);
                if (!root.optBoolean("success", false)) return;

                JSONArray arr = root.getJSONArray("data");

                Calendar baseCal = Calendar.getInstance();
                baseCal.setTime(startDate);

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);

                    String breed = obj.getString("breed");
                    if (!breed.equalsIgnoreCase(selectedBreed)) continue;

                    String activityName = obj.getString("activity_name");
                    String type = obj.optString("activity_type", "Core");

                    if ("Daily".equalsIgnoreCase(type)) {
                        // Repeat daily for 120 days (or adjust per species)
                        for (int d = 0; d <= 120; d++) {
                            Calendar activityCal = (Calendar) baseCal.clone();
                            activityCal.add(Calendar.DAY_OF_YEAR, d);
                            CalendarDay calDay = CalendarDay.from(activityCal);
                            activityMap.computeIfAbsent(calDay, k -> new ArrayList<>()).add(activityName);
                        }
                    } else if ("Weekly".equalsIgnoreCase(type)) {
                        // Repeat weekly for 16 weeks
                        for (int w = 0; w <= 16; w++) {
                            Calendar activityCal = (Calendar) baseCal.clone();
                            activityCal.add(Calendar.DAY_OF_YEAR, w * 7);
                            CalendarDay calDay = CalendarDay.from(activityCal);
                            activityMap.computeIfAbsent(calDay, k -> new ArrayList<>()).add(activityName);
                        }
                    } else {
                        // Core activities use suggested_day
                        if (!obj.isNull("suggested_day")) {
                            int suggestedDay = obj.getInt("suggested_day");
                            Calendar activityCal = (Calendar) baseCal.clone();
                            activityCal.add(Calendar.DAY_OF_YEAR, suggestedDay);
                            CalendarDay calDay = CalendarDay.from(activityCal);
                            activityMap.computeIfAbsent(calDay, k -> new ArrayList<>()).add(activityName);
                        }
                    }
                }

                if (getActivity() == null || !isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setupCalendarDots();
                    CalendarDay initialDay = CalendarDay.from(startDate);
                    calendarView.setSelectedDate(initialDay);
                    showActivitiesForDate(initialDay);
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() ->
                            android.widget.Toast.makeText(requireContext(),
                                    "❌ Error fetching activities", android.widget.Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }




    private void setupCalendarDots() {
        calendarView.removeDecorators();
        List<CalendarDay> daysWithActivities = new ArrayList<>(activityMap.keySet());
        calendarView.addDecorator(new EventDecorator(Color.BLUE, daysWithActivities));
        calendarView.invalidateDecorators();
    }

    private void showActivitiesForDate(CalendarDay date) {
        List<String> activities = activityMap.getOrDefault(date, Collections.emptyList());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_multiple_choice,
                activities);
        activitiesListView.setAdapter(adapter);
        activitiesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        String dateKey = formatDateKey(date);
        for (int i = 0; i < activities.size(); i++) {
            String key = dateKey + "_" + activities.get(i);
            boolean done = completedActivities.getOrDefault(key, false);
            activitiesListView.setItemChecked(i, done);
        }

        activitiesListView.setOnItemClickListener((parent, view, position, id) -> {
            boolean checked = ((CheckedTextView) view).isChecked();
            String key = dateKey + "_" + activities.get(position);
            completedActivities.put(key, checked);
            saveCompletedActivities();
        });

        activitiesHeader.setText("My Activities - " + formatDateKey(date));
    }

    private String formatDateKey(CalendarDay date) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                date.getYear(), date.getMonth() + 1, date.getDay());
    }

    private void saveCompletedActivities() {
        SharedPreferences prefs = requireContext().getSharedPreferences("CompletedActivities", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, Boolean> entry : completedActivities.entrySet()) {
            editor.putBoolean(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    private void loadCompletedActivities() {
        SharedPreferences prefs = requireContext().getSharedPreferences("CompletedActivities", Context.MODE_PRIVATE);
        Map<String, ?> allPrefs = prefs.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                completedActivities.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }
    }

    private static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;

        public EventDecorator(int color, Collection<CalendarDay> dates) {
            this.color = color;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(8, color));
        }
    }
}
