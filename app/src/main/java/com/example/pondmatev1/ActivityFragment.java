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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

        // Parse date with format "MMM. dd, yyyy" (example: "Jul. 28, 2025")
        SimpleDateFormat sdf = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
        if (startDateStr != null) {
            try {
                startDate = sdf.parse(startDateStr);
            } catch (ParseException e) {
                e.printStackTrace();
                startDate = new Date(); // fallback to today
            }
        } else {
            startDate = new Date();
        }

        generateSchedule();

        setupCalendarDots();

        // Spinner to toggle calendar visibility
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

    private void generateSchedule() {
        activityMap.clear();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        int durationDays;
        switch (selectedBreed.toLowerCase()) {
            case "alimango":
                durationDays = 270; // approx 9 months
                break;
            case "bangus":
                durationDays = 120; // approx 4 months
                break;
            case "tilapia":
            default:
                durationDays = 120;
                break;
        }

        for (int day = 0; day <= durationDays; day++) {
            CalendarDay calDay = CalendarDay.from(calendar);
            List<String> activities = new ArrayList<>();

            // Daily activities every day (starting day 0)
            activities.add("Daily feeding and monitoring");

            // Weekly activities every 7 days starting on day 7 (no weekly task before day 7)
            if (day >= 7 && day % 7 == 0 && day != durationDays) {
                activities.add("Weekly maintenance tasks");
            }

            // Special activity on day 0
            if (day == 0) {
                activities.add("Pond preparation");
                if (selectedBreed.equalsIgnoreCase("alimango")) {
                    activities.add("Stock crablets");
                } else {
                    activities.add("Stock fingerlings");
                }
            }

            // Harvest day
            if (day == durationDays) {
                activities.clear();
                activities.add("Harvesting");
            }

            activityMap.put(calDay, activities);
            calendar.add(Calendar.DATE, 1);
        }
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

        // Restore checked states
        String dateKey = formatDateKey(date);
        for (int i = 0; i < activities.size(); i++) {
            String key = dateKey + "_" + activities.get(i);
            boolean done = completedActivities.getOrDefault(key, false);
            activitiesListView.setItemChecked(i, done);
        }

        // Save on click
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
