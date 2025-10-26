package com.example.pondmatev1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CycleChartFragment extends Fragment {

    private BarChart cycleBarChart;
    private ProgressBar progressBarChart;
    private TextView noDataText;
    private LinearLayout markerContainer;
    private TextView markerText, markerLifeStage, markerProgress;

    private List<PondModel> ponds;

    // Production phases on Y-axis
    private final String[] phases = new String[]{
            "Pond Prep", "Stocking", "Daily Maint",
            "Pre-Starter", "Starter", "Grower", "Finisher", "Harvest"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cycle_chart, container, false);

        cycleBarChart = root.findViewById(R.id.cycleBarChart);
        progressBarChart = root.findViewById(R.id.progressBarChart);
        noDataText = root.findViewById(R.id.noDataText);

        markerContainer = root.findViewById(R.id.markerContainer);
        markerText = root.findViewById(R.id.markerText);
        markerLifeStage = root.findViewById(R.id.markerLifeStage);
        markerProgress = root.findViewById(R.id.markerProgress);

        cycleBarChart.getDescription().setEnabled(false);
        cycleBarChart.getAxisRight().setEnabled(false);

        loadPondsFromArguments();

        return root;
    }

    private void loadPondsFromArguments() {
        progressBarChart.setVisibility(View.VISIBLE);

        if (getArguments() != null && getArguments().containsKey("pondsJson")) {
            String pondsJson = getArguments().getString("pondsJson");
            PondModel[] pondArray = new Gson().fromJson(pondsJson, PondModel[].class);
            ponds = new ArrayList<>();
            Collections.addAll(ponds, pondArray);
        } else {
            ponds = new ArrayList<>();
        }

        progressBarChart.setVisibility(View.GONE);

        if (ponds.isEmpty()) {
            noDataText.setVisibility(View.VISIBLE);
            return;
        }

        noDataText.setVisibility(View.GONE);
        showPondsChart();
    }

    private void showPondsChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> pondNames = new ArrayList<>();

        for (int i = 0; i < ponds.size(); i++) {
            PondModel pond = ponds.get(i);
            pondNames.add(pond.getName() != null ? pond.getName() : "Pond " + (i + 1));

            long startMillis = parseDateToMillis(pond.getDateStarted());
            long now = System.currentTimeMillis();
            long totalCycleMillis = 180L * 24 * 60 * 60 * 1000; // 6 months
            float progress = (float) Math.min((now - startMillis) / (double) totalCycleMillis, 1.0);
            entries.add(new BarEntry(i, progress * phases.length));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Pond Production Cycle");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        cycleBarChart.setData(barData);

        // X Axis
        XAxis xAxis = cycleBarChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < pondNames.size()) {
                    return pondNames.get(index);
                }
                return "";
            }
        });

        // Y Axis
        YAxis yAxisLeft = cycleBarChart.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setAxisMaximum(phases.length);
        yAxisLeft.setLabelCount(phases.length, true);
        yAxisLeft.setGranularity(1f);
        yAxisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < phases.length) return phases[index];
                return "";
            }
        });

        cycleBarChart.setFitBars(true);

        // Show marker inside fragment on bar click
        cycleBarChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                int pondIndex = (int) e.getX();
                float yValue = e.getY();
                if (pondIndex < 0 || pondIndex >= ponds.size()) return;

                PondModel pond = ponds.get(pondIndex);
                int phaseIndex = Math.min((int) yValue, phases.length - 1);

                // Pond Name & Phase
                markerText.setText(pond.getName() + " - " + phases[phaseIndex]);

                // Fish Life Stage
                String lifeStage;
                if (phaseIndex == 0) {
                    lifeStage = "Not Applicable, wait for stocking"; // Pond Prep
                } else if (phaseIndex == 1) {
                    lifeStage = "Fingerlings"; // Stocking
                } else if (phaseIndex <= 4) {
                    lifeStage = "Juvenile"; // Pre-Starter, Starter
                } else if (phaseIndex <= 6) {
                    lifeStage = "Adult"; // Grower, Finisher
                } else {
                    lifeStage = "Not Applicable, already harvested"; // Harvest
                }
                markerLifeStage.setText("Fish Life Stage: " + lifeStage);

                // Completion %
                float progressPercent = Math.min((yValue / phases.length) * 100f, 100f);
                markerProgress.setText(String.format(Locale.US, "Progress: %.1f%%", progressPercent));

                markerContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected() {
                markerContainer.setVisibility(View.GONE);
            }
        });

        cycleBarChart.invalidate();
    }

    private long parseDateToMillis(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return sdf.parse(dateStr).getTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
