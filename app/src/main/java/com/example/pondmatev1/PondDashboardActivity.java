package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
//
//        ImageView profileIcon = findViewById(R.id.profileIcon);
//        profileIcon.setOnClickListener(v -> {
//            UserProfileDialogFragment dialog = new UserProfileDialogFragment();
//            dialog.show(getSupportFragmentManager(), "UserProfileDialog");
//        });
//
        if (!"owner".equalsIgnoreCase(userType)) {
            adminIcon.setVisibility(View.GONE);
        }

        pondRecyclerView = findViewById(R.id.pondRecyclerView);
        int spacing = getResources().getDimensionPixelSize(R.dimen.pond_card_spacing);
        pondRecyclerView.addItemDecoration(new SpacingItemDecoration(spacing));
        pondRecyclerView.setBackgroundColor(Color.TRANSPARENT);

        pondList = new ArrayList<>();
        pondAdapter = new PondAdapter(this, pondList, userType);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        pondRecyclerView.setLayoutManager(layoutManager);
        pondRecyclerView.setAdapter(pondAdapter);

        roiBarChart = findViewById(R.id.roiBarChart);

        loadPondsFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPondsFromServer();
    }
    private void loadPondsFromServer() {
        pondList.clear();

        if ("owner".equalsIgnoreCase(userType)) {
            pondList.add(new PondModel("ADD_BUTTON"));
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_ponds.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray pondsArray = new JSONArray(response.toString());
                for (int i = 0; i < pondsArray.length(); i++) {
                    JSONObject pond = pondsArray.getJSONObject(i);
                    pondList.add(new PondModel(
                            pond.getString("name"),
                            pond.getString("breed"),
                            pond.getInt("fish_count"),
                            pond.getDouble("cost_per_fish"),
                            pond.getString("date_started"),
                            pond.getString("date_harvest"),
                            "DATA"
                    ));
                }

                runOnUiThread(() -> {
                    pondAdapter.notifyDataSetChanged();
                    loadChartData(); // load chart after ponds are fetched
                });

            } catch (Exception e) {
                Log.e("LOAD_PONDS", "Error: " + e.getMessage(), e);
            }
        }).start();
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
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return roiMap;
    }

    @Override
    public void loadChartData() {
        new Thread(() -> {
            try {
                pondNames.clear();
                dateRanges.clear();
                List<BarEntry> entries = new ArrayList<>();
                Map<String, Float> roiData = getAllROIData();

                // Fetch pond start/end dates from API
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_dates.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray datesArray = new JSONArray(response.toString());
                Map<String, String[]> pondDates = new LinkedHashMap<>();

                for (int i = 0; i < datesArray.length(); i++) {
                    JSONObject obj = datesArray.getJSONObject(i);
                    String name = obj.getString("name");
                    String start = obj.getString("date_started");
                    String harvest = obj.getString("date_harvest");
                    pondDates.put(name, new String[]{start, harvest});
                }

                int index = 0;
                for (Map.Entry<String, Float> entry : roiData.entrySet()) {
                    String pondName = entry.getKey();
                    float roiValue = entry.getValue();

                    String[] dates = pondDates.getOrDefault(pondName, new String[]{"", ""});
                    String range = "";
                    if (!dates[0].isEmpty() && !dates[1].isEmpty()) {
                        range = formatMonthShort(dates[0]) + " - " + formatMonthShort(dates[1]);
                    }

                    entries.add(new BarEntry(index, roiValue));
                    pondNames.add(pondName);
                    dateRanges.add(range);
                    index++;
                }

                runOnUiThread(() -> {
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

                    XAxis xAxis = roiBarChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(pondNames));
                    xAxis.setGranularity(1f);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setDrawGridLines(false);

                    roiBarChart.getDescription().setEnabled(false);
                    roiBarChart.setExtraBottomOffset(15f);
                    roiBarChart.setFitBars(true);
                    roiBarChart.animateY(1000);
                    roiBarChart.invalidate();
                });

            } catch (Exception e) {
                Log.e("LOAD_CHART", "Error: " + e.getMessage(), e);
            }
        }).start();
    }

    private String formatMonthShort(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM yy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            return "";
        }
    }
}
