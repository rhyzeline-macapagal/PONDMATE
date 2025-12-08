package com.example.pondmatev1;

import android.graphics.Color;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.HashSet;

public class ActivityDatesDecorator implements DayViewDecorator {

    private final HashSet<CalendarDay> activityDates;

    public ActivityDatesDecorator(HashSet<CalendarDay> dates) {
        this.activityDates = dates;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return activityDates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpan(10, Color.parseColor("#002D6F"))); // Dark Blue
    }
}
