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
    private static final String GET_POND_ACTIVITIES_URL = "https://pondmate.alwaysdata.net/get_pond_activities.php";

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

                requireActivity().runOnUiThread(() -> {
                    progressBarChart.setVisibility(View.GONE);
                    if (ponds.isEmpty()) {
                        noDataText.setVisibility(View.VISIBLE);
                    } else {
                        setupRecyclerView();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to fetch ponds");
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
        private final Map<String, Integer> cachedProgress = new HashMap<>();

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
            String dateCreated = pond.optString("date_created", "");

            holder.pondName.setText(name);

            if (cachedProgress.containsKey(name)) {
                updateUI(holder, breed, cachedProgress.get(name));
                return;
            }

            holder.pondPhase.setText("Loading progress...");
            holder.pondLifeStage.setText("");
            holder.pondProgressText.setText("");
            holder.pondProgressCircle.setProgress(0);

            new Thread(() -> fetchPondActivities(name, breed, holder)).start();
        }

        private void fetchPondActivities(String pondName, String breed, PondViewHolder holder) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(GET_POND_ACTIVITIES_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                String postData = "pond_name=" + pondName;
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString().trim());
                if (!response.optString("status").equals("success")) return;

                JSONArray activities = response.getJSONArray("data");
                if (activities.length() == 0) return;

                String firstDate = activities.getJSONObject(0).getString("date");
                String lastDate = activities.getJSONObject(activities.length() - 1).getString("date");

                long startMillis = parseDateToMillis(firstDate);
                long endMillis = parseDateToMillis(lastDate);
                long now = System.currentTimeMillis();

                int progress = (int) (((double) (now - startMillis) / (endMillis - startMillis)) * 100);
                progress = Math.max(0, Math.min(progress, 100));

                cachedProgress.put(pondName, progress);

                int finalProgress = progress;
                requireActivity().runOnUiThread(() -> updateUI(holder, breed, finalProgress));

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        private void updateUI(PondViewHolder holder, String breed, int progress) {
            String phaseName, lifeStage;

            if (progress < 10) {
                phaseName = "Pond Preparation";
                lifeStage = "Not Applicable";
                holder.pondProgressCircle.setIndicatorColor(Color.GRAY);
            } else if (progress < 30) {
                phaseName = "Stocking";
                lifeStage = "Fingerling";
                holder.pondProgressCircle.setIndicatorColor(Color.BLUE);
            } else if (progress < 90) {
                phaseName = "Daily Maintenance";
                lifeStage = breed.equalsIgnoreCase("Tilapia")
                        ? "Juvenile → Sub-adult"
                        : "Sub-adult → Grow-out";
                holder.pondProgressCircle.setIndicatorColor(Color.CYAN);
            } else {
                phaseName = "Harvesting";
                lifeStage = "Adult / Ready to Harvest";
                holder.pondProgressCircle.setIndicatorColor(Color.GREEN);
            }

            holder.pondPhase.setText("Phase: " + phaseName);
            holder.pondLifeStage.setText("Life Stage: " + lifeStage);
            holder.pondProgressText.setText("Progress: " + progress + "%");
            holder.pondProgressCircle.setProgress(progress);
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
