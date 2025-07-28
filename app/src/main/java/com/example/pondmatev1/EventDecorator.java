package com.example.pondmatev1;

import android.graphics.Color;

import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventDecorator implements DayViewDecorator {
    private final int color;
    private final Set<CalendarDay> dates;

    public EventDecorator(int color, List<CalendarDay> dates) {
        this.color = color;
        this.dates = new HashSet<>(dates);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpan(8, color)); // Shows a colored dot under the date
    }
}
