package com.example.pondmatev1;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // ✅ for View.GONE
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import androidx.work.*;


public class PondDashboardActivity extends AppCompatActivity{
    private AlertDialog loadingDialog;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    RecyclerView pondRecyclerView;
    PondAdapter pondAdapter;
    ArrayList<PondModel> pondList;
    String userType;
    private Map<String, String> dateRangeMap = new LinkedHashMap<>();
    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private ArrayList<HistoryModel> historyList = new ArrayList<>();
    private static final String BASE_URL = "http://pondmate.alwaysdata.net/getPondHistory.php";
    public static PondDashboardActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pond_dashboard);

        userType = new SessionManager(this).getUsertype();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        ImageView adminIcon = findViewById(R.id.adminIcon);
        adminIcon.setOnClickListener(v -> {
            // Inflate the custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_options, null);

            AlertDialog dialog = new AlertDialog.Builder(PondDashboardActivity.this, R.style.TransparentDialog) // optional style
                    .setView(dialogView)
                    .create();

            // Close button (the ✖ on top-right)
            TextView btnClose = dialogView.findViewById(R.id.btnClose);
            btnClose.setOnClickListener(v1 -> dialog.dismiss());

            // Caretaker Dashboard button
            ImageButton btnCaretaker = dialogView.findViewById(R.id.btnCaretaker);
            btnCaretaker.setOnClickListener(v1 -> {
                startActivity(new Intent(PondDashboardActivity.this, CaretakerDashboardActivity.class));
                overridePendingTransition(R.anim.drop_in, R.anim.fade_out);
                dialog.dismiss();
            });

            // Feeds Prices button
            ImageButton btnFeedsPrice = dialogView.findViewById(R.id.btnFeedsPrice);
            btnFeedsPrice.setOnClickListener(v1 -> {
                startActivity(new Intent(PondDashboardActivity.this, FeedsPriceActivity.class));
                dialog.dismiss();
            });

            ImageButton btnFarmgatePrice = dialogView.findViewById(R.id.btnFarmgatePrice);
            btnFarmgatePrice.setOnClickListener(v1 -> {
                startActivity(new Intent(PondDashboardActivity.this, FarmgatePriceActivity.class));
                dialog.dismiss();
            });
            // Show dialog
            dialog.show();
        });

        if (!"owner".equalsIgnoreCase(userType)) {
            adminIcon.setVisibility(View.GONE); // ✅ Fixed
        }

        ImageView profileIcon = findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(v -> {
            Log.d("DEBUG", "Profile icon clicked");
            UserProfileDialogFragment dialog = new UserProfileDialogFragment();
            dialog.show(getSupportFragmentManager(), "UserProfileDialog");
        });

        ImageView notificationIcon = findViewById(R.id.notificationIcon); notificationIcon.setOnClickListener(v ->
        { Toast.makeText(this, "Notification clicked!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, NotificationActivity.class)); });


        pondRecyclerView = findViewById(R.id.pondRecyclerView);
        int spacing = getResources().getDimensionPixelSize(R.dimen.pond_card_spacing);
        pondRecyclerView.addItemDecoration(new SpacingItemDecoration(spacing));
        pondRecyclerView.setBackgroundColor(Color.TRANSPARENT);

        pondList = new ArrayList<>();
        pondAdapter = new PondAdapter(this, pondList, userType);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        pondRecyclerView.setLayoutManager(layoutManager);
        pondRecyclerView.setAdapter(pondAdapter);


        loadPondsFromServer();

        historyRecyclerView = findViewById(R.id.HistoryRecyclerView);
        int historySpacing = getResources().getDimensionPixelSize(R.dimen.history_card_spacing);
        historyRecyclerView.addItemDecoration(new SpacingItemDecoration(spacing));
        historyRecyclerView.setBackgroundColor(Color.TRANSPARENT);

        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(this, historyList);

        LinearLayoutManager historyLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        historyRecyclerView.setLayoutManager(historyLayoutManager);
        historyRecyclerView.setAdapter(historyAdapter);

        PeriodicWorkRequest pendingActivityWork =
                new PeriodicWorkRequest.Builder(PendingActivityWorker.class, 24, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PendingActivityWorker",
                ExistingPeriodicWorkPolicy.KEEP, // Keep the existing one if already enqueued
                pendingActivityWork
        );


    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPondsFromServer();
        loadAllHistory();
    }

    private void loadPondsFromServer() {
        runOnUiThread(this::showLoadingDialog);

        // Show ADD_BUTTON immediately for owners
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

                // Add "ADD" button only for owners
                if ("owner".equalsIgnoreCase(userType)) {
                    newPonds.add(new PondModel("ADD_BUTTON"));
                }

                SharedPreferences activityPrefs = getSharedPreferences("ActivityPrefs", MODE_PRIVATE);

                for (int i = 0; i < pondsArray.length(); i++) {
                    JSONObject pond = pondsArray.getJSONObject(i);

                    String id = pond.optString("pond_id", "0");
                    String name = pond.optString("name", "Not yet added");
                    String breed = pond.optString("breed", "Not yet added");
                    int fishCount = pond.has("fish_count") && !pond.isNull("fish_count") ? pond.optInt("fish_count") : 0;
                    double costPerFish = pond.has("cost_per_fish") && !pond.isNull("cost_per_fish") ? pond.optDouble("cost_per_fish") : 0;
                    String dateStarted = pond.optString("date_started", "Not yet added");
                    String dateHarvest = pond.optString("date_harvest", "Not yet added");
                    String dateStocking = pond.optString("date_stocking", "Not yet added");
                    double pondArea = pond.has("pond_area") && !pond.isNull("pond_area") ? pond.optDouble("pond_area") : 0;
                    double mortalityRate = pond.has("mortality_rate") && !pond.isNull("mortality_rate") ? pond.optDouble("mortality_rate") : 0;
                    String imagePath = pond.optString("image_path", null);
                    String pdfPath = pond.optString("pdf_path", null);
                    String caretakerName = pond.optString("caretaker_name", "Unassigned");
                    Log.d("LOAD_PONDS", "Caretaker for pond " + name + " = " + caretakerName);

                    float actualROI = (float) pond.optDouble("actual_roi", 0);
                    float estimatedROI = (float) pond.optDouble("estimated_roi", 0);

                    // Create PondModel
                    PondModel pondModel = new PondModel(
                            id,                // String id
                            name,              // String name
                            breed,             // String breed
                            fishCount,         // int fishCount
                            costPerFish,       // double costPerFish
                            dateStarted,       // String dateStarted
                            dateHarvest,       // String dateHarvest
                            dateStocking,              // String dateStocking (placeholder)
                            0.0,               // double pondArea (placeholder)
                            imagePath,         // String imagePath
                            null,              // String mode (placeholder)
                            actualROI,         // float actualROI
                            estimatedROI,      // float estimatedROI
                            pdfPath,
                            mortalityRate     // ✅ double comes before
                                      // double mortalityRate (placeholder)
                    );

                    pondModel.setPondArea(pondArea);
                    pondModel.setCaretakerName(caretakerName);
                    pondModel.setMortalityRate(mortalityRate);

                    newPonds.add(pondModel);
                }


                // ✅ Update UI thread safely
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    pondList.clear();
                    pondList.addAll(newPonds);
                    pondAdapter.notifyDataSetChanged();

                    // --- Pass pondList to fragment ---
                    String pondsJson = new Gson().toJson(pondList);
                    CycleChartFragment cycleChartFragment = new CycleChartFragment();
                    Bundle args = new Bundle();
                    args.putString("pondsJson", pondsJson);
                    cycleChartFragment.setArguments(args);

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.cycleChartContainer, cycleChartFragment)
                            .commitAllowingStateLoss(); // <-- FIX
                });

                // ✅ Schedule notifications for activities and feeding
                schedulePendingActivityNotifications();
                for (PondModel pond : pondList) {
                    scheduleFeedingNotifications(pond);
                }

            } catch (Exception e) {
                Log.e("LOAD_PONDS", "Error: " + e.getMessage(), e);
                runOnUiThread(this::hideLoadingDialog);
            } finally {
                runOnUiThread(this::hideLoadingDialog); // always close
            }
        }).start();
    }

    private void showLoadingDialog() {
        runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                return; // already showing, don’t recreate
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
            ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
            TextView loadingText = dialogView.findViewById(R.id.loadingText);
            loadingText.setText("Loading your PONDS please wait...");
            Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
            if (fishLoader != null) fishLoader.startAnimation(rotate);
            builder.setView(dialogView).setCancelable(false);
            loadingDialog = builder.create();
            loadingDialog.show();
        });
    }
    private void hideLoadingDialog() {
        runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
                loadingDialog = null;
            }
        });
    }

    public void loadHistory(String pondId) {
        new Thread(() -> {
            try {
                String urlStr = "https://pondmate.alwaysdata.net/getPondHistory.php";
                if (pondId != null && !pondId.isEmpty()) {
                    urlStr += "?pond_id=" + pondId;
                }
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("PondHistory", "Response: " + response);

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.optBoolean("success")) {
                    JSONArray historyArray = jsonResponse.getJSONArray("history");

                    ArrayList<HistoryModel> newHistory = new ArrayList<>();
                    for (int i = 0; i < historyArray.length(); i++) {
                        JSONObject obj = historyArray.getJSONObject(i);

                        String historypondId = obj.optString("pond_id", "");
                        String pondName = obj.optString("name", "");
                        String action = obj.optString("action", "");
                        String date = obj.optString("created_at", "");
                        String pdfPath = obj.optString("pdf_path", "");
                        if (!pdfPath.isEmpty() && !pdfPath.startsWith("http")) {
                            pdfPath = "https://pondmate.alwaysdata.net/" + (pdfPath.startsWith("/") ? pdfPath.substring(1) : pdfPath);
                        }
                        newHistory.add(new HistoryModel(historypondId, pondName, action, date, pdfPath));
                    }

                    runOnUiThread(() -> {
                        historyList.clear();
                        historyList.addAll(newHistory);
                        historyAdapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(PondDashboardActivity.this, "No history found", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e("PondHistory", "Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(PondDashboardActivity.this, "Error loading history", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    public void loadAllHistory() {
        fetchHistory("https://pondmate.alwaysdata.net/getPondHistory.php");
    }

    private void fetchHistory(String urlStr) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.optBoolean("success")) {
                    JSONArray historyArray = jsonResponse.getJSONArray("history");

                    ArrayList<HistoryModel> newHistory = new ArrayList<>();
                    for (int i = 0; i < historyArray.length(); i++) {
                        JSONObject obj = historyArray.getJSONObject(i);

                        String historypondId = obj.optString("pond_id", "");
                        String pondName = obj.optString("name", "");
                        String action = obj.optString("action", "");
                        String date = obj.optString("created_at", "");
                        String pdfPath = obj.optString("pdf_path", "");
                        newHistory.add(new HistoryModel(historypondId, pondName, action, date, pdfPath));
                    }

                    runOnUiThread(() -> {
                        historyList.clear();
                        historyList.addAll(newHistory);
                        historyAdapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(PondDashboardActivity.this, "No history found", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e("PondHistory", "Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(PondDashboardActivity.this, "Error loading history", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public SpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.right = spacing;
        }
    }

    public void generateInactivePondReport(String pondId, String pondName) {
        PondSyncManager.fetchPondReportData (pondName, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                runOnUiThread(() -> {
                    try {
                        String raw = String.valueOf(response);
                        JSONObject json = new JSONObject(raw);

                        // Mark it as INACTIVE for watermark/title
                        json.put("action", "INACTIVE");

                        // Ensure pond object exists
                        if (!json.has("pond") || json.optJSONObject("pond") == null) {
                            JSONObject pondObj = new JSONObject();
                            pondObj.put("id", pondId);
                            pondObj.put("name", pondName);
                            json.put("pond", pondObj);
                        }

                        // Use the same generator used in Production Cost Fragment
                        File pdfFile = PondPDFGenerator.generatePDF(PondDashboardActivity.this, json, pondId);

                        if (pdfFile != null && pdfFile.exists()) {
                            previewPDF(pdfFile);
                            Toast.makeText(PondDashboardActivity.this,
                                    "Inactive pond report generated for " + pondName, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PondDashboardActivity.this,
                                    "Failed to generate report", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(PondDashboardActivity.this,
                                "Error generating report: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(PondDashboardActivity.this,
                                "Server error: " + error,
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
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

    public static void refreshHistoryNow() {
        if (instance != null) {
            instance.runOnUiThread(() -> instance.loadHistory(""));
        }
    }

    private void schedulePendingActivityNotifications() {
        for (PondModel pond : pondList) {
            if ("ADD_BUTTON".equals(pond.getMode())) continue; // skip the ADD button

            int pondId = Integer.parseInt(pond.getId()); // make sure id is numeric
            String pondName = pond.getName();

            List<ActivityItem> activities = pond.getActivities();
            if (activities == null || activities.isEmpty()) continue;

            // Convert to String array
            String[] activitiesArray = new String[activities.size()];
            for (int i = 0; i < activities.size(); i++) {
                activitiesArray[i] = activities.get(i).getName(); // ✅ use getName() instead
            }

            Data inputData = new Data.Builder()
                    .putInt("pondId", pondId)
                    .putString("pondName", pondName)
                    .putStringArray("activities", activitiesArray)
                    .build();

            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    PendingActivityWorker.class, 24, TimeUnit.HOURS
            )
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "PendingActivityWorker_" + pondId,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
            );
        }
    }

    private void previewPDF(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer found.", Toast.LENGTH_SHORT).show();
        }
    }


    private void scheduleFeedingNotifications(PondModel pond) {
        if ("ADD_BUTTON".equals(pond.getMode())) return;

        String pondName = pond.getName();
        List<ActivityItem> activities = pond.getActivities();
        if (activities == null || activities.isEmpty()) return;

        Calendar now = Calendar.getInstance();

        for (ActivityItem activity : activities) {
            if (!"Feeding".equalsIgnoreCase(activity.getType())) continue;

            // Parse scheduledDate (format: yyyy-MM-dd HH:mm)
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                Calendar feedTime = Calendar.getInstance();
                feedTime.setTime(sdf.parse(activity.getScheduledDate()));

                // Skip if time already passed
                if (feedTime.getTimeInMillis() <= now.getTimeInMillis()) continue;

                long delay = feedTime.getTimeInMillis() - now.getTimeInMillis();

                Data inputData = new Data.Builder()
                        .putString("pondName", pondName)
                        .putString("feedingLabel", activity.getName())
                        .build();

                androidx.work.OneTimeWorkRequest feedingWork =
                        new androidx.work.OneTimeWorkRequest.Builder(FeedingNotificationWorker.class)
                                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .build();

                androidx.work.WorkManager.getInstance(this).enqueue(feedingWork);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
