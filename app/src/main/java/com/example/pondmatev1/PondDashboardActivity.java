package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // ✅ for View.GONE
import android.widget.ImageView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream; // ✅ Needed for OutputStream
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
    private Map<String, String> dateRangeMap = new LinkedHashMap<>();

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
        if (!"owner".equalsIgnoreCase(userType)) {
            adminIcon.setVisibility(View.GONE); // ✅ Fixed
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

    @Override
    public void loadChartData() {
        runOnUiThread(this::renderChartData);
    }

    private void loadPondsFromServer() {
        runOnUiThread(() -> {
            pondList.clear();
            if ("owner".equalsIgnoreCase(userType)) {
                pondList.add(new PondModel("ADD_BUTTON"));
            }
            pondAdapter.notifyDataSetChanged();
        });

        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_ponds.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONArray pondsArray = new JSONArray(response.toString());
                ArrayList<PondModel> newPonds = new ArrayList<>();
                if ("owner".equalsIgnoreCase(userType)) newPonds.add(new PondModel("ADD_BUTTON"));

                for (int i = 0; i < pondsArray.length(); i++) {
                    JSONObject pond = pondsArray.getJSONObject(i);

                    String id = pond.getString("pond_id");
                    String name = pond.getString("name");
                    String breed = pond.getString("breed");
                    int fishCount = pond.getInt("fish_count");
                    double costPerFish = pond.getDouble("cost_per_fish");
                    String dateStarted = pond.getString("date_started");
                    String dateHarvest = pond.getString("date_harvest");
                    String imagePath = pond.getString("image_path");

                    float actualROI = (float) pond.optDouble("actual_roi", 0);
                    float estimatedROI = (float) pond.optDouble("estimated_roi", 0);

                    newPonds.add(new PondModel(id, name, breed, fishCount, costPerFish,
                            dateStarted, dateHarvest, imagePath, actualROI, estimatedROI));
                }

                runOnUiThread(() -> {
                    pondList.clear();
                    pondList.addAll(newPonds);
                    pondAdapter.notifyDataSetChanged();
                    renderChartData();
                });

            } catch (Exception e) {
                Log.e("LOAD_PONDS", "Error: " + e.getMessage(), e);
            }
        }).start();
    }

    private void renderChartData() {
        Map<String, float[]> roiMap = new LinkedHashMap<>();
        dateRangeMap.clear();

        for (PondModel pond : pondList) {
            if ("ADD_BUTTON".equals(pond.getMode())) continue;

            String pondName = pond.getName();
            float actual = pond.getActualROI();
            float compare = pond.getEstimatedROI();

            String startRaw = pond.getDateStarted();
            String harvestRaw = pond.getDateHarvest();
            String range = (startRaw.isEmpty() || harvestRaw.isEmpty())
                    ? "N/A"
                    : formatDateDisplay(startRaw) + " - " + formatDateDisplay(harvestRaw);

            dateRangeMap.put(pondName, range);
            roiMap.put(pondName, new float[]{actual, compare});
        }

        List<BarEntry> actualEntries = new ArrayList<>();
        List<BarEntry> compareEntries = new ArrayList<>();
        List<String> pondLabels = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, float[]> e : roiMap.entrySet()) {
            float a = e.getValue()[0];
            float c = e.getValue()[1];
            actualEntries.add(new BarEntry(i, a));
            compareEntries.add(new BarEntry(i, c));
            pondLabels.add(e.getKey());
            i++;
        }

        roiBarChart.clear();
        if (pondLabels.isEmpty()) {
            roiBarChart.invalidate();
            return;
        }

        BarDataSet setActual = new BarDataSet(actualEntries, "Actual ROI");
        setActual.setColor(Color.parseColor("#4CAF50"));
        BarDataSet setCompare = new BarDataSet(compareEntries, "Estimated ROI");
        setCompare.setColor(Color.parseColor("#2196F3"));

        BarData data = new BarData(setActual, setCompare);
        float groupSpace = 0.28f;
        float barSpace = 0.04f;
        float barWidth = 0.32f;
        data.setBarWidth(barWidth);
        roiBarChart.setData(data);

        XAxis xAxis = roiBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(pondLabels));
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        float startX = 0f;
        xAxis.setAxisMinimum(startX);
        xAxis.setAxisMaximum(startX + data.getGroupWidth(groupSpace, barSpace) * pondLabels.size());
        roiBarChart.groupBars(startX, groupSpace, barSpace);

        YAxis left = roiBarChart.getAxisLeft();
        left.setAxisMinimum(0f);
        roiBarChart.getAxisRight().setEnabled(false);

        roiBarChart.getDescription().setEnabled(false);
        roiBarChart.animateY(800);

        CustomMarkerView markerView = new CustomMarkerView(this, R.layout.custom_marker,
                dateRangeMap, pondLabels, actualEntries, compareEntries);
        roiBarChart.setMarker(markerView);

        roiBarChart.invalidate();
    }

    private String formatDateDisplay(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.US);
            return outputFormat.format(inputFormat.parse(inputDate));
        } catch (Exception e) {
            return inputDate;
        }
    }

    private void saveROIsToServer() {
        List<Map<String, Object>> pondsToSend = new ArrayList<>();

        for (PondModel pond : pondList) {
            if ("ADD_BUTTON".equals(pond.getMode())) continue;

            Map<String, Object> pondMap = new HashMap<>();
            pondMap.put("name", pond.getName());
            pondMap.put("actual_roi", pond.getActualROI());
            pondMap.put("estimated_roi", pond.getEstimatedROI());
            pondsToSend.add(pondMap);
        }

        if (pondsToSend.isEmpty()) return;

        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_roi.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("ponds", new JSONArray(pondsToSend));

                conn.getOutputStream().write(json.toString().getBytes("UTF-8"));

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                Log.d("ROI_SYNC", response.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
