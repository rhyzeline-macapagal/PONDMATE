package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private Spinner spinnerToggle;
    private LinearLayout llActivityList;
    private Map<CalendarDay, List<String>> activityMap = new HashMap<>();
    private String breed;
    private Date startDate;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarView = view.findViewById(R.id.calendarView);
        spinnerToggle = view.findViewById(R.id.spinnerCalendarToggle);
        llActivityList = view.findViewById(R.id.llActivityList);

        calendarView.setBackgroundColor(Color.parseColor("#FFF9E5")); // Light brown tint

        SharedPreferences prefs = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);
        breed = prefs.getString("fish_breed", "tilapia");
        String startDateStr = prefs.getString("date_started", "2024-01-01");

        try {
            startDate = sdf.parse(startDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        generateSchedule(breed, startDate);

        spinnerToggle.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Show Calendar", "Hide Calendar"}));
        spinnerToggle.setSelection(0);

        calendarView.setVisibility(View.VISIBLE);

        CalendarDay initialDay = CalendarDay.from(startDate);
        calendarView.setSelectedDate(initialDay);
        displayActivitiesForDate(initialDay);

        Set<CalendarDay> dots = activityMap.keySet();
        calendarView.addDecorator(new DotDecorator(Color.BLUE, dots));

        spinnerToggle.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                CalendarDay selectedDate = calendarView.getSelectedDate();
                if (position == 0) {
                    calendarView.setVisibility(View.VISIBLE);
                    if (selectedDate != null) displayActivitiesForDate(selectedDate);
                } else {
                    calendarView.setVisibility(View.GONE);
                    if (selectedDate != null) displayWeekActivities(selectedDate);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (calendarView.getVisibility() == View.VISIBLE) {
                    displayActivitiesForDate(date);
                }
            }
        });
    }

    private void displayActivitiesForDate(CalendarDay date) {
        llActivityList.removeAllViews();
        addTitle("My Activities");

        List<String> activities = activityMap.getOrDefault(date, new ArrayList<>());
        addDayRow(date.getDate(), activities);
    }

    private void displayWeekActivities(CalendarDay startDay) {
        llActivityList.removeAllViews();
        addTitle("My Activities");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDay.getDate());

        for (int i = 0; i < 7; i++) {
            CalendarDay day = CalendarDay.from(calendar);
            List<String> activities = activityMap.getOrDefault(day, new ArrayList<>());
            addDayRow(calendar.getTime(), activities);
            calendar.add(Calendar.DATE, 1);
        }
    }

    private void addTitle(String title) {
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTextSize(18);
        tvTitle.setGravity(View.TEXT_ALIGNMENT_CENTER);
        tvTitle.setPadding(0, 16, 0, 8);
        tvTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        llActivityList.addView(tvTitle);
    }

    private void addDayRow(Date date, List<String> activities) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 24, 24, 24);
        container.setBackgroundColor(Color.parseColor("#FFF9E5")); // Match calendar tint

        TextView dateView = new TextView(getContext());
        dateView.setText(new SimpleDateFormat("EEE, MMM dd", Locale.US).format(date));
        dateView.setTextColor(Color.parseColor("#2196F3"));
        dateView.setTextSize(16);
        dateView.setPadding(0, 0, 0, 8);
        container.addView(dateView);

        if (activities.isEmpty()) {
            TextView noAct = new TextView(getContext());
            noAct.setText("• No activities scheduled.");
            noAct.setTextSize(14);
            noAct.setPadding(32, 2, 0, 2);
            container.addView(noAct);
        } else {
            for (String act : activities) {
                TextView actView = new TextView(getContext());
                actView.setText("• " + act);
                actView.setTextSize(14);
                actView.setPadding(32, 2, 0, 2);
                container.addView(actView);
            }
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 12);
        llActivityList.addView(container, params);
    }

    private void generateSchedule(String breed, Date start) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        int duration = breed.equalsIgnoreCase("alimango") ? 270 : 120;

        for (int day = 0; day <= duration; day++) {
            CalendarDay calDay = CalendarDay.from(calendar);
            List<String> acts = new ArrayList<>();

            if (day == 0) acts.add("Pond preparation");
            if (day == 1) acts.add("Water Quality Testing");
            if (day == 3) acts.add("Stocking");

            if (day % 7 == 0 && day > 3 && day < duration) {
                acts.add("Weekly Cleaning");
                acts.add("Feeding");
                acts.add("Check water level");
                acts.add("Remove uneaten feeds");
                acts.add("Monitor temperature and DO levels");
            }

            if (day >= 7 && day <= 30) acts.add("Pre-starter feeding");
            if (day > 30 && day <= 60) acts.add("Starter feeding");
            if (day > 60 && day <= 90) acts.add("Grower feeding");
            if (day > 90 && day < duration) acts.add("Finisher feeding");

            if (day == duration) acts.clear(); acts.add("Harvesting");

            if (!acts.isEmpty()) {
                activityMap.put(calDay, acts);
            }

            calendar.add(Calendar.DATE, 1);
        }
    }

    static class DotDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;

        DotDecorator(int color, Collection<CalendarDay> dates) {
            this.color = color;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(6, color));
        }
    }
}
