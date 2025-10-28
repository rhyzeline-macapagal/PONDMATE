package com.example.pondmatev1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CycleChartFragment extends Fragment {

    private ProgressBar progressBarChart;
    private TextView noDataText;
    private RecyclerView pondRecyclerView;
    private List<PondModel> ponds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cycle_chart, container, false);

        progressBarChart = root.findViewById(R.id.progressBarChart);
        noDataText = root.findViewById(R.id.noDataText);
        pondRecyclerView = root.findViewById(R.id.pondRecyclerView);

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
        } else {
            noDataText.setVisibility(View.GONE);
            setupRecyclerView();
        }
    }

    private void setupRecyclerView() {
        pondRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        pondRecyclerView.setAdapter(new PondAdapter(ponds));
    }

    // --- Adapter ---
    private class PondAdapter extends RecyclerView.Adapter<PondAdapter.PondViewHolder> {

        private final List<PondModel> pondList;

        PondAdapter(List<PondModel> pondList) {
            this.pondList = pondList;
        }

        @NonNull
        @Override
        public PondViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(16,16,16,16);
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0,0,0,16);
            card.setLayoutParams(cardParams);

            CircularProgressIndicator progressCircle = new CircularProgressIndicator(parent.getContext());
            progressCircle.setLayoutParams(new LinearLayout.LayoutParams(120,120));
            progressCircle.setMax(100);
            progressCircle.setTrackThickness(10);
            card.addView(progressCircle);

            LinearLayout textContainer = new LinearLayout(parent.getContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setPadding(24,0,0,0);
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
            PondModel pond = pondList.get(position);

            String breed = pond.getBreed() != null ? pond.getBreed() : "Tilapia";
            holder.pondName.setText(pond.getName() != null ? pond.getName() : "Pond " + (position + 1));

            long startMillis = parseDateToMillis(pond.getDateStarted());
            long now = System.currentTimeMillis();

            long daysInMs = 24L * 60 * 60 * 1000;

            // --- Phase durations ---
            long pondPrepEnd = startMillis + (14L * daysInMs); // 2 weeks prep
            long stockingDate = pondPrepEnd + (1L * daysInMs); // next day after prep
            long fingerlingEnd = stockingDate + (74L * daysInMs); // 10–84 days
            long juvenileEnd = stockingDate + (120L * daysInMs); // up to day 120
            long subAdultEnd = stockingDate + (180L * daysInMs); // up to 6 months
            long harvestDate = subAdultEnd + (1L * daysInMs); // harvesting day

            String lifeStage;
            String phaseName;
            int progressPercent;

            if (now < startMillis) {
                phaseName = "Pond Prep";
                lifeStage = "Not Applicable, wait for stocking";
                progressPercent = 0;
            } else if (now <= pondPrepEnd) {
                phaseName = "Pond Prep";
                lifeStage = "Not Applicable, wait for stocking"; // <-- updated
                progressPercent = (int) ((now - startMillis) * 100 / (pondPrepEnd - startMillis));
            } else if (now <= stockingDate) {
                phaseName = "Stocking";
                lifeStage = "Fingerling / Juvenile";
                progressPercent = (int) ((now - pondPrepEnd) * 100 / (stockingDate - pondPrepEnd));
            } else if (now <= fingerlingEnd) {
                phaseName = "Fingerling Growth";
                lifeStage = "Fingerling / Juvenile";
                progressPercent = (int) ((now - stockingDate) * 100 / (fingerlingEnd - stockingDate));
            } else if (now <= juvenileEnd) {
                phaseName = "Juvenile Growth";
                lifeStage = "Juvenile / Sub-adult";
                progressPercent = (int) ((now - fingerlingEnd) * 100 / (juvenileEnd - fingerlingEnd));
            } else if (now <= subAdultEnd) {
                phaseName = "Sub-adult Growth";
                lifeStage = "Sub-adult / Adult";
                progressPercent = (int) ((now - juvenileEnd) * 100 / (subAdultEnd - juvenileEnd));
            } else if (now <= harvestDate) {
                phaseName = "Harvesting";
                lifeStage = "Adult / Ready to Harvest";
                progressPercent = 100;
            } else {
                phaseName = "Harvest Completed";
                lifeStage = "Not Applicable, already harvested"; // <-- updated
                progressPercent = 100;
            }

            holder.pondPhase.setText("Phase: " + phaseName);
            holder.pondLifeStage.setText("Life Stage: " + lifeStage);
            holder.pondProgress.setText("Progress: " + progressPercent + "%");
            holder.pondProgressCircle.setProgress(progressPercent);
        }



        @Override
        public int getItemCount() {
            return pondList.size();
        }

        private long parseDateToMillis(String dateStr){
            try{
                if(dateStr==null || dateStr.isEmpty()) return System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return sdf.parse(dateStr).getTime();
            }catch(Exception e){
                return System.currentTimeMillis();
            }
        }

        class PondViewHolder extends RecyclerView.ViewHolder {
            TextView pondName, pondPhase, pondLifeStage, pondProgress;
            CircularProgressIndicator pondProgressCircle;

            PondViewHolder(@NonNull View itemView, TextView name, TextView phase,
                           TextView stage, TextView progress, CircularProgressIndicator progressCircle){
                super(itemView);
                pondName=name;
                pondPhase=phase;
                pondLifeStage=stage;
                pondProgress=progress;
                pondProgressCircle=progressCircle;
            }
        }
    }
}
