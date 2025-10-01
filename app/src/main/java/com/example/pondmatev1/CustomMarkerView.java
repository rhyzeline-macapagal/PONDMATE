package com.example.pondmatev1;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;
import java.util.Map;

public class CustomMarkerView extends MarkerView {
    private final TextView tvContent;
    private final Map<String, String> dateRangeMap;
    private final List<String> pondLabels;
    private final List<BarEntry> actualEntries;

    public CustomMarkerView(Context context, int layoutResource,
                            Map<String, String> dateRangeMap,
                            List<String> pondLabels,
                            List<BarEntry> actualEntries) {
        super(context, layoutResource);
        this.tvContent = findViewById(R.id.tvContent);
        this.dateRangeMap = dateRangeMap;
        this.pondLabels = pondLabels;
        this.actualEntries = actualEntries;
    }

    @Override
    public void refreshContent(com.github.mikephil.charting.data.Entry e, Highlight highlight) {
        int xIndex = (int) e.getX();
        if (xIndex >= 0 && xIndex < pondLabels.size()) {
            String pondName = pondLabels.get(xIndex);

            float actualROI = 0f;
            float compareROI = 0f;

            // find matching entries
            for (BarEntry entry : actualEntries) {
                if ((int) entry.getX() == xIndex) {
                    actualROI = entry.getY();
                    break;
                }
            }

            String dateRange = dateRangeMap.containsKey(pondName) ? dateRangeMap.get(pondName) : "N/A";

            String info = pondName + "\n" +
                    "Actual ROI: " + String.format("%.2f%%", actualROI) + "\n" +
                    "Date Range: " + dateRange;

            tvContent.setText(info);
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10);
    }
}
