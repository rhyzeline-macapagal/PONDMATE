package com.example.pondmatev1;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collections;

public class TodayDecorator implements DayViewDecorator {

    private final CalendarDay today;

    public TodayDecorator() {
        today = CalendarDay.today();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(today); // Only decorate today
    }

    @Override
    public void decorate(DayViewFacade view) {
        // Add a small dot below the date
        view.addSpan(new DotSpan(8, Color.RED)); // radius 8px, color red
        // Optional: make today date bold
        view.setDaysDisabled(false); // Ensure today is clickable
    }
}
