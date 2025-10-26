package com.example.pondmatev1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

    private List<PondModel> ponds;

    // Production phases as Y-axis labels
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

        cycleBarChart.getDescription().setEnabled(false);
        cycleBarChart.getAxisRight().setEnabled(false);

        // Load ponds passed from the dashboard
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
        showPondsChart(ponds);
    }

    private void showPondsChart(List<PondModel> ponds) {
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

        cycleBarChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                int index = (int) e.getX();
                float progress = e.getY() / phases.length * 100;
                if (index >= 0 && index < pondNames.size()) {
                    Toast.makeText(requireContext(),
                            pondNames.get(index) + " - " + String.format("%.1f%% completed", progress),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {}
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
