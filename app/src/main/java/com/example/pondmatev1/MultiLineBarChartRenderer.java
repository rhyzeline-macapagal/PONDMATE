package com.example.pondmatev1;

import android.graphics.Canvas;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.Transformer;

import java.util.List;

public class MultiLineBarChartRenderer extends com.github.mikephil.charting.renderer.BarChartRenderer {

    private final List<String> dateRanges;

    public MultiLineBarChartRenderer(BarChart chart, List<String> dateRanges) {
        super(chart, chart.getAnimator(), chart.getViewPortHandler());
        this.dateRanges = dateRanges;
    }

    @Override
    public void drawValues(Canvas c) {
        if (mChart.getData() == null) return;

        List<IBarDataSet> dataSets = mChart.getData().getDataSets();
        if (dataSets == null) return;

        final float valueTextHeight = mValuePaint.getFontMetrics(null) * 1.2f;

        for (int i = 0; i < dataSets.size(); i++) {
            IBarDataSet dataSet = dataSets.get(i);
            if (dataSet == null) continue;

            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

            int entryCount = dataSet.getEntryCount();
            if (entryCount == 0) continue;

            float[] positions = new float[entryCount * 2];

            for (int j = 0; j < entryCount; j++) {
                BarEntry entry = dataSet.getEntryForIndex(j);
                if (entry == null) continue;

                positions[j * 2] = entry.getX();
                positions[j * 2 + 1] = entry.getY();
            }

            trans.pointValuesToPixel(positions);

            for (int j = 0; j < entryCount; j++) {
                BarEntry entry = dataSet.getEntryForIndex(j);
                if (entry == null) continue;

                float x = positions[j * 2];
                float y = positions[j * 2 + 1];

                String roiText = String.format("%.0f%%", entry.getY());
                String dateRangeText = (dateRanges != null && j < dateRanges.size()) ? dateRanges.get(j) : "";

                float totalHeight = valueTextHeight * 2;
                float startY = y - totalHeight;

                c.drawText(roiText, x, startY, mValuePaint);
                c.drawText(dateRangeText, x, startY + valueTextHeight, mValuePaint);
            }
        }
    }
}
