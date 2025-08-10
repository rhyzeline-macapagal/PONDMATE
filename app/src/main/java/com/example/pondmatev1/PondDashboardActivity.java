package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PondDashboardActivity extends AppCompatActivity implements ROIChartUpdater {

    RecyclerView pondRecyclerView;
    PondAdapter pondAdapter;
    ArrayList<PondModel> pondList;
    String userType;
    private BarChart roiBarChart;
    private List<String> pondNames = new ArrayList<>();
    private List<String> dateRanges = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pond_dashboard);

        userType = new SessionManager(this).getUsertype();

        ImageView adminIcon = findViewById(R.id.adminIcon);
        adminIcon.setOnClickListener(v -> {
            Intent intent = new Intent(PondDashboardActivity.this, CaretakerDashboardActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.drop_in, R.anim.fade_out);
        });

        ImageView profileIcon = findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(v -> {
            UserProfileDialogFragment dialog = new UserProfileDialogFragment();
            dialog.show(getSupportFragmentManager(), "UserProfileDialog");
        });

        if (!"owner".equalsIgnoreCase(userType)) {
            adminIcon.setVisibility(View.GONE);
        } else {
            adminIcon.setVisibility(View.VISIBLE);
        }

        pondRecyclerView = findViewById(R.id.pondRecyclerView);
        int spacing = getResources().getDimensionPixelSize(R.dimen.pond_card_spacing);
        pondRecyclerView.addItemDecoration(new SpacingItemDecoration(spacing));
        pondRecyclerView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        pondList = new ArrayList<>();
        pondAdapter = new PondAdapter(this, pondList, userType);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        pondRecyclerView.setLayoutManager(layoutManager);
        pondRecyclerView.setAdapter(pondAdapter);

        PondSyncManager.syncPondsFromServer(this);
        loadLocalPonds();
        PondSyncManager.syncProductionCostsFromServer(this);

        roiBarChart = findViewById(R.id.roiBarChart);

        loadChartData();
    }

    private void loadLocalPonds() {
        pondList.clear();

        if ("owner".equalsIgnoreCase(userType)) {
            pondList.add(new PondModel("ADD_BUTTON"));
        }

        DatabaseHelper db = new DatabaseHelper(this);
        Cursor c = db.getAllPonds();

        if (c != null && c.moveToFirst()) {
            do {
                PondModel pond = new PondModel(
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("breed")),
                        c.getInt(c.getColumnIndexOrThrow("fish_count")),
                        c.getDouble(c.getColumnIndexOrThrow("cost_per_fish")),
                        c.getString(c.getColumnIndexOrThrow("date_started")),
                        c.getString(c.getColumnIndexOrThrow("date_harvest")),
                        "DATA"
                );
                pondList.add(pond);
            } while (c.moveToNext());
            c.close();
        }

        Log.d("POND_LOAD", "Loaded pond count: " + pondList.size());
        pondAdapter.notifyDataSetChanged();
    }

    private Map<String, Float> getAllROIData() {
        SharedPreferences sharedPreferences = getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        Map<String, Float> roiMap = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().endsWith("_roi")) {
                String pondName = entry.getKey().replace("_roi", "");
                try {
                    float roiValue = Float.parseFloat(entry.getValue().toString());
                    roiMap.put(pondName, roiValue);
                } catch (NumberFormatException e) {

                }
            }
        }
        return roiMap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChartData();
    }

    @Override
    public void loadChartData() {
        runOnUiThread(() -> {
            pondNames.clear();
            dateRanges.clear();

            Map<String, Float> roiData = getAllROIData();
            List<BarEntry> entries = new ArrayList<>();

            DatabaseHelper db = new DatabaseHelper(this);

            int index = 0;
            for (Map.Entry<String, Float> entry : roiData.entrySet()) {
                String pondName = entry.getKey();
                float roiValue = entry.getValue();

                String startDate = db.getPondStartDate(pondName);
                String endDate = db.getPondHarvestDate(pondName);

                String range = "";
                if (startDate != null && endDate != null) {
                    range = formatMonthShort(startDate) + " - " + formatMonthShort(endDate);
                }

                entries.add(new BarEntry(index, roiValue));
                pondNames.add(pondName);
                dateRanges.add(range);
                index++;
            }

            if (entries.isEmpty()) {
                roiBarChart.clear();
                roiBarChart.invalidate();
                return;
            }

            BarDataSet dataSet = new BarDataSet(entries, "ROI (%)");
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.BLACK);

            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getBarLabel(BarEntry barEntry) {
                    return "";
                }
            });

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.9f);

            roiBarChart.setData(data);
            roiBarChart.setRenderer(new MultiLineBarChartRenderer(roiBarChart, dateRanges));
            roiBarChart.invalidate();
            roiBarChart.notifyDataSetChanged();


            XAxis xAxis = roiBarChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(pondNames));
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setLabelRotationAngle(-0f);

            roiBarChart.getDescription().setEnabled(false);
            roiBarChart.setExtraBottomOffset(15f);
            roiBarChart.setFitBars(true);
            roiBarChart.animateY(1000);
            roiBarChart.invalidate();

        });
    }

    private String formatMonthShort(String dateStr) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM yy");
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            return "";
        }
    }
}
