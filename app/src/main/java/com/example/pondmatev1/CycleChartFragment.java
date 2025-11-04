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
import java.io.*;
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

        new Thread(() -> {
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

                JSONArray response = new JSONArray(sb.toString().trim());
                ponds.clear();
                for (int i = 0; i < response.length(); i++) {
                    ponds.add(response.getJSONObject(i));
                }

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBarChart.setVisibility(View.GONE);
                        if (ponds.isEmpty()) {
                            noDataText.setVisibility(View.VISIBLE);
                        } else {
                            setupRecyclerView();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                // ✅ Only show error if fragment still alive
                if (isAdded()) showError("Failed to fetch ponds");
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void showError(String msg) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            progressBarChart.setVisibility(View.GONE);
            noDataText.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
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
            String dateStarted = pond.optString("date_started", "");
            String dateStocking = pond.optString("date_stocking", "");
            String dateHarvest = pond.optString("date_harvest", "");

            holder.pondName.setText(name);

            long startMillis = parseDateToMillis(dateStarted);
            long stockingMillis = parseDateToMillis(dateStocking);
            long harvestMillis = parseDateToMillis(dateHarvest);
            long now = System.currentTimeMillis();

            int progress;
            String phaseName, lifeStage;

            if (now < stockingMillis) {
                phaseName = "Pond Preparation";
                lifeStage = "Not Applicable";
                holder.pondProgressCircle.setIndicatorColor(Color.GRAY);
                progress = 0;
            } else if (now < harvestMillis) {
                phaseName = "Stocked / Growing";
                lifeStage = breed.equalsIgnoreCase("Tilapia") ? "Juvenile → Sub-adult" : "Sub-adult → Grow-out";
                holder.pondProgressCircle.setIndicatorColor(Color.BLUE);
                progress = (int) (((double) (now - stockingMillis) / (harvestMillis - stockingMillis)) * 100);
                progress = Math.max(0, Math.min(progress, 100));
            } else {
                phaseName = "Harvesting";
                lifeStage = "Adult / Ready to Harvest";
                holder.pondProgressCircle.setIndicatorColor(Color.GREEN);
                progress = 100;
            }

            holder.pondPhase.setText("Phase: " + phaseName);
            holder.pondLifeStage.setText("Life Stage: " + lifeStage);
            holder.pondProgressCircle.setProgress(progress);

            // Remove percentage text
            holder.pondProgressText.setText("");
        }

        private long parseDateToMillis(String dateStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return sdf.parse(dateStr).getTime();
            } catch (Exception e) {
                return System.currentTimeMillis();
            }
        }

        @Override
        public int getItemCount() {
            return pondList.size();
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
