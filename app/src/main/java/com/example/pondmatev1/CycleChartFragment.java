package com.example.pondmatev1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class CycleChartFragment extends Fragment {

    private static final String GET_PONDS_URL = "https://pondmate.alwaysdata.net/get_ponds.php";

    private ProgressBar progressBarChart;
    private TextView noDataText;
    private RecyclerView pondRecyclerView;
    private final List<JSONObject> ponds = new ArrayList<>();
    private Thread fetchThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cycle_chart, container, false);

        progressBarChart = root.findViewById(R.id.progressBarChart);
        noDataText = root.findViewById(R.id.noDataText);
        pondRecyclerView = root.findViewById(R.id.pondRecyclerView);

        fetchPondsFromServer();
        return root;
    }

    private void fetchPondsFromServer() {
        progressBarChart.setVisibility(View.VISIBLE);
        noDataText.setVisibility(View.GONE);

        fetchThread = new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(GET_PONDS_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String jsonString = sb.toString().trim();
                if (jsonString.isEmpty() || !jsonString.startsWith("[")) {
                    showError("Invalid server response");
                    return;
                }

                JSONArray response = new JSONArray(jsonString);
                List<JSONObject> loadedPonds = new ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    loadedPonds.add(response.getJSONObject(i));
                }

                requireActivity().runOnUiThread(() -> {
                    progressBarChart.setVisibility(View.GONE);
                    ponds.clear();
                    ponds.addAll(loadedPonds);

                    if (ponds.isEmpty()) {
                        noDataText.setVisibility(View.VISIBLE);
                    } else {
                        noDataText.setVisibility(View.GONE);
                        setupRecyclerView();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to fetch ponds");
            } finally {
                if (conn != null) conn.disconnect();
            }
        });

        fetchThread.start();
    }

    private void showError(String msg) {
        requireActivity().runOnUiThread(() -> {
            progressBarChart.setVisibility(View.GONE);
            noDataText.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        pondRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        pondRecyclerView.setAdapter(new PondAdapter(ponds));
    }

    // ===================== ADAPTER =====================
    private class PondAdapter extends RecyclerView.Adapter<PondAdapter.PondViewHolder> {
        private final List<JSONObject> pondList;

        PondAdapter(List<JSONObject> pondList) {
            this.pondList = pondList;
        }

        @NonNull
        @Override
        public PondViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(16, 16, 16, 16);
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);

            CircularProgressIndicator progressCircle = new CircularProgressIndicator(parent.getContext());
            progressCircle.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
            progressCircle.setMax(100);
            progressCircle.setTrackThickness(10);
            card.addView(progressCircle);

            LinearLayout textContainer = new LinearLayout(parent.getContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setPadding(24, 0, 0, 0);
            card.addView(textContainer);

            TextView pondName = new TextView(parent.getContext());
            pondName.setTextSize(16);
            pondName.setTextColor(Color.BLACK);
            textContainer.addView(pondName);

            TextView pondPhase = new TextView(parent.getContext());
            pondPhase.setTextSize(14);
            pondPhase.setTextColor(Color.DKGRAY);
            textContainer.addView(pondPhase);

            TextView pondLifeStage = new TextView(parent.getContext());
            pondLifeStage.setTextSize(14);
            pondLifeStage.setTextColor(Color.DKGRAY);
            textContainer.addView(pondLifeStage);

            TextView pondProgressText = new TextView(parent.getContext());
            pondProgressText.setTextSize(14);
            pondProgressText.setTextColor(Color.parseColor("#1565C0"));
            textContainer.addView(pondProgressText);

            return new PondViewHolder(card, pondName, pondPhase, pondLifeStage, pondProgressText, progressCircle);
        }

        @Override
        public void onBindViewHolder(@NonNull PondViewHolder holder, int position) {
            JSONObject pond = pondList.get(position);

            String name = pond.optString("name", "Unnamed Pond");
            String breed = pond.optString("breed", "Tilapia");
            String dateCreated = pond.optString("date_started", "");
            String stockingDate = pond.optString("date_stocking", "");

            holder.pondName.setText(name);

            long startMillis = parseDateToMillis(stockingDate.isEmpty() ? dateCreated : stockingDate);
            long now = System.currentTimeMillis();
            long dayMs = 24L * 60 * 60 * 1000;

            // Define phase durations
            long pondPrepEnd = startMillis + (14L * dayMs);
            long stockingEnd = pondPrepEnd + dayMs;
            long maintenanceEnd = stockingEnd + (180L * dayMs);
            long harvestDate = maintenanceEnd + dayMs;

            String phaseName;
            String lifeStage;
            int progressPercent;

            if (now < startMillis) {
                phaseName = "Pond Preparation";
                lifeStage = "Not Applicable, wait for stocking";
                progressPercent = 0;
                holder.pondProgressCircle.setIndicatorColor(Color.GRAY);
            } else if (now <= pondPrepEnd) {
                phaseName = "Pond Preparation";
                lifeStage = "Not Applicable, wait for stocking";
                progressPercent = (int) ((now - startMillis) * 100 / (pondPrepEnd - startMillis));
                holder.pondProgressCircle.setIndicatorColor(Color.GRAY);
            } else if (now <= stockingEnd) {
                phaseName = "Stocking";
                lifeStage = "Fingerling";
                progressPercent = 100;
                holder.pondProgressCircle.setIndicatorColor(Color.BLUE);
            } else if (now <= maintenanceEnd) {
                phaseName = "Daily Maintenance";
                long daysSinceStocking = (now - stockingEnd) / dayMs;

                if (breed.equalsIgnoreCase("Tilapia")) {
                    if (daysSinceStocking <= 60) lifeStage = "Fingerling → Juvenile";
                    else if (daysSinceStocking <= 90) lifeStage = "Juvenile → Sub-adult";
                    else lifeStage = "Sub-adult → Adult / Harvest";
                } else { // Bangus
                    if (daysSinceStocking <= 70) lifeStage = "Fingerling → Juvenile";
                    else if (daysSinceStocking <= 90) lifeStage = "Juvenile → Sub-adult";
                    else lifeStage = "Sub-adult → Adult / Harvest";
                }

                progressPercent = (int) ((now - stockingEnd) * 100 / (maintenanceEnd - stockingEnd));
                holder.pondProgressCircle.setIndicatorColor(Color.BLUE);
            } else if (now <= harvestDate) {
                phaseName = "Harvesting";
                lifeStage = "Adult / Ready to Harvest";
                progressPercent = 100;
                holder.pondProgressCircle.setIndicatorColor(Color.GREEN);
            } else {
                phaseName = "Harvest Completed";
                lifeStage = "Not Applicable, already harvested";
                progressPercent = 100;
                holder.pondProgressCircle.setIndicatorColor(Color.GREEN);
            }

            holder.pondPhase.setText("Phase: " + phaseName);
            holder.pondLifeStage.setText("Life Stage: " + lifeStage);
            holder.pondProgressText.setText("Progress: " + progressPercent + "%");
            holder.pondProgressCircle.setProgress(progressPercent);
        }

        @Override
        public int getItemCount() {
            return pondList.size();
        }

        private long parseDateToMillis(String dateStr) {
            try {
                if (dateStr == null || dateStr.isEmpty()) return System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return sdf.parse(dateStr).getTime();
            } catch (Exception e) {
                return System.currentTimeMillis();
            }
        }

        class PondViewHolder extends RecyclerView.ViewHolder {
            TextView pondName, pondPhase, pondLifeStage, pondProgressText;
            CircularProgressIndicator pondProgressCircle;

            PondViewHolder(@NonNull View itemView, TextView name, TextView phase, TextView stage,
                           TextView progressText, CircularProgressIndicator progressCircle) {
                super(itemView);
                pondName = name;
                pondPhase = phase;
                pondLifeStage = stage;
                pondProgressText = progressText;
                pondProgressCircle = progressCircle;
            }
        }
    }
}
