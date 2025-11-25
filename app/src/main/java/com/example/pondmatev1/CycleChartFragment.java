package com.example.pondmatev1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CycleChartFragment extends Fragment {

    private static final String GET_PONDS_URL = "https://pondmate.alwaysdata.net/get_ponds.php";
    private RecyclerView pondRecyclerView;
    private final List<JSONObject> ponds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cycle_chart, container, false);
        pondRecyclerView = root.findViewById(R.id.pondRecyclerView);
        pondRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fetchPondsFromServer();
        return root;
    }

    private void fetchPondsFromServer() {
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

                // Sort ponds: Phase 4 (Harvesting) first
                ponds.sort((o1, o2) -> {
                    int phase1 = getPhase(o1);
                    int phase2 = getPhase(o2);
                    return Integer.compare(phase2, phase1); // descending order
                });

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (ponds.isEmpty()) {
                            Toast.makeText(getContext(), "No pond data available", Toast.LENGTH_SHORT).show();
                        } else {
                            pondRecyclerView.setAdapter(new PondAdapter(ponds));
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to fetch ponds", Toast.LENGTH_SHORT).show()
                    );
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // Helper method to determine current phase
    private int getPhase(JSONObject pond) {
        try {
            String dateStocking = pond.optString("date_stocking", "");
            String dateHarvest = pond.optString("date_harvest", "");
            long now = System.currentTimeMillis();
            long stockingMillis = parseDateToMillis(dateStocking);
            long harvestMillis = parseDateToMillis(dateHarvest);

            if (now < stockingMillis) return 1;
            else if (now < harvestMillis) return 2;
            else return 4; // Harvesting
        } catch (Exception e) {
            return 1;
        }
    }

    // reuse parseDateToMillis method from your adapter
    private long parseDateToMillis(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return sdf.parse(dateStr).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }


    private static class PondAdapter extends RecyclerView.Adapter<PondAdapter.PondViewHolder> {

        private final List<JSONObject> pondList;

        PondAdapter(List<JSONObject> pondList) {
            this.pondList = pondList;
        }

        @NonNull
        @Override
        public PondViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pond_cycle, parent, false);
            return new PondViewHolder(view);
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

            String phaseName, lifeStage;
            int currentPhase;

            // Calculate total cycle length
            long cycleDuration = harvestMillis - stockingMillis;
            long thirtyPercent = stockingMillis + (long)(cycleDuration * 0.30);

            // PHASE 1 — Preparation (before stocking)
            if (now < stockingMillis) {
                phaseName = "Preparation";
                lifeStage = "Not Applicable";
                currentPhase = 1;
                holder.itemView.setBackgroundColor(Color.WHITE);

                // PHASE 2 — Fingerlings Stocking (from stocking to early cycle)
            } else if (now >= stockingMillis && now < thirtyPercent) {
                phaseName = "Fingerlings Stocking";
                lifeStage = "Fingerlings";
                currentPhase = 2;
                holder.itemView.setBackgroundColor(Color.WHITE);

                // PHASE 3 — Growing (mid cycle until harvest)
            } else if (now < harvestMillis) {
                phaseName = "Growing";
                lifeStage = "Sub-adult → Grow-out";
                currentPhase = 3;
                holder.itemView.setBackgroundColor(Color.WHITE);

                // PHASE 4 — Harvesting (ready to harvest)
            } else {
                phaseName = "Harvesting";
                lifeStage = "Adult / Ready to Harvest";
                currentPhase = 4;
                holder.itemView.setBackgroundColor(Color.parseColor("#C6F6D5")); // green
            }

            holder.pondPhase.setText("Phase: " + phaseName);
            holder.pondStage.setText("Life Stage: " + lifeStage);

            setPhaseColor(holder, currentPhase);

            // Clicking a circle expands or collapses the info section
            View.OnClickListener expandListener = v -> {
                holder.infoContainer.setVisibility(
                        holder.infoContainer.getVisibility() == View.GONE ? View.VISIBLE : View.GONE
                );
            };

            holder.phase1Circle.setOnClickListener(expandListener);
            holder.phase2Circle.setOnClickListener(expandListener);
            holder.phase3Circle.setOnClickListener(expandListener);
            holder.phase4Circle.setOnClickListener(expandListener);
        }


        private void setPhaseColor(PondViewHolder holder, int currentPhase) {
            holder.phase1Circle.setBackgroundResource(R.drawable.circle_inactive);
            holder.phase2Circle.setBackgroundResource(R.drawable.circle_inactive);
            holder.phase3Circle.setBackgroundResource(R.drawable.circle_inactive);
            holder.phase4Circle.setBackgroundResource(R.drawable.circle_inactive);

            switch (currentPhase) {
                case 1:
                    holder.phase1Circle.setBackgroundResource(R.drawable.circle_active);
                    break;
                case 2:
                    holder.phase1Circle.setBackgroundResource(R.drawable.circle_active);
                    holder.phase2Circle.setBackgroundResource(R.drawable.circle_active);
                    break;
                case 3:
                    holder.phase1Circle.setBackgroundResource(R.drawable.circle_active);
                    holder.phase2Circle.setBackgroundResource(R.drawable.circle_active);
                    holder.phase3Circle.setBackgroundResource(R.drawable.circle_active);
                    break;
                case 4:
                    holder.phase1Circle.setBackgroundResource(R.drawable.circle_active);
                    holder.phase2Circle.setBackgroundResource(R.drawable.circle_active);
                    holder.phase3Circle.setBackgroundResource(R.drawable.circle_active);
                    holder.phase4Circle.setBackgroundResource(R.drawable.circle_active);
                    break;
            }
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

        static class PondViewHolder extends RecyclerView.ViewHolder {
            TextView pondName, pondPhase, pondStage;
            TextView phase1Circle, phase2Circle, phase3Circle, phase4Circle;
            LinearLayout infoContainer;

            PondViewHolder(@NonNull View itemView) {
                super(itemView);
                pondName = itemView.findViewById(R.id.pondName);
                pondPhase = itemView.findViewById(R.id.pondPhase);
                pondStage = itemView.findViewById(R.id.pondStage);
                infoContainer = itemView.findViewById(R.id.infoContainer);

                phase1Circle = itemView.findViewById(R.id.phase1Circle);
                phase2Circle = itemView.findViewById(R.id.phase2Circle);
                phase3Circle = itemView.findViewById(R.id.phase3Circle);
                phase4Circle = itemView.findViewById(R.id.phase4Circle);
            }
        }
    }
}
