package com.example.pondmatev1;

import android.content.Context;
import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;

/**
 * Custom decorator for highlighting activity dates.
 */
public class EventDecorator implements DayViewDecorator {

    private final HashSet<CalendarDay> dates;
    private final int highlightColor;

    public EventDecorator(Context context, HashSet<CalendarDay> dates) {
        this.dates = dates;
        this.highlightColor = Color.parseColor("#00BFA5"); // teal color accent
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // 🌟 Make the date bold, teal, and underlined
        view.addSpan(new ForegroundColorSpan(highlightColor));
        view.addSpan(new StyleSpan(android.graphics.Typeface.BOLD));
        view.addSpan(new UnderlineSpan());
    }
}
