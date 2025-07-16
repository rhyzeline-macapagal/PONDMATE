package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class PondInfoFragment extends Fragment {

    private TextView tvBreed, tvFishCount, tvDateStarted, tvHarvestDate, tvMortalityRate, tvEstDeadFish;
    private Button btnEdit;

    private PondModel pond;
    private boolean isEditing = false;
    private EditText tvPondName, tvCostPerFish;

    public PondInfoFragment() {}

    public static PondInfoFragment newInstance(PondModel pond) {
        PondInfoFragment fragment = new PondInfoFragment();
        Bundle args = new Bundle();
        args.putString("name", pond.getName());
        args.putString("breed", pond.getBreed());
        args.putInt("fish_count", pond.getFishCount());
        args.putDouble("cost_per_fish", pond.getCostPerFish());
        args.putString("date_started", pond.getDateStarted());
        args.putString("date_harvest", pond.getDateHarvest());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pond_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPondName = view.findViewById(R.id.tvPondName);
        tvBreed = view.findViewById(R.id.tvBreed);
        tvFishCount = view.findViewById(R.id.tvFishCount);
        tvCostPerFish = view.findViewById(R.id.tvCostPerFish);
        tvDateStarted = view.findViewById(R.id.tvDateStarted);
        tvHarvestDate = view.findViewById(R.id.tvHarvestDate);
        tvMortalityRate = view.findViewById(R.id.tvMortalityRate);
        tvEstDeadFish = view.findViewById(R.id.tvEstimatedDeadFish);
        btnEdit = view.findViewById(R.id.btnEditPond);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fish_breed", tvBreed.getText().toString().trim());
        editor.putString("fish_amount", tvCostPerFish.getText().toString().trim());
        editor.putString("number_fish", tvFishCount.getText().toString().trim());
        editor.apply();

        if (getArguments() != null) {
            int fishCount = getArguments().getInt("fish_count");
            double costPerFish = getArguments().getDouble("cost_per_fish");

            tvPondName.setText(getArguments().getString("name"));
            tvBreed.setText(getArguments().getString("breed"));
            tvFishCount.setText(String.valueOf(getArguments().getInt("fish_count")));
            tvCostPerFish.setText(String.valueOf(getArguments().getDouble("cost_per_fish")));
            tvDateStarted.setText(getArguments().getString("date_started"));
            tvHarvestDate.setText(getArguments().getString("date_harvest"));

            updateMortalityData(fishCount);
        }

        String userType = new SessionManager(requireContext()).getUsertype();
        if ("owner".equalsIgnoreCase(userType)) {
            btnEdit.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE);
        }

        btnEdit.setOnClickListener(v -> {
            if (!isEditing) {
                // Switch to Edit mode
                btnEdit.setText("Save");
                tvPondName.setEnabled(true);
                tvCostPerFish.setEnabled(true);
                tvPondName.requestFocus();
                isEditing = true;
            } else {
                // Switch to Save mode
                btnEdit.setText("Edit");
                tvPondName.setEnabled(false);
                tvCostPerFish.setEnabled(false);

                // Optionally save or process the updated values here
                String newName = tvPondName.getText().toString().trim();
                String newCost = tvCostPerFish.getText().toString().trim();
                // TODO: Save these values (database)

                isEditing = false;
            }
        });

    }


    private void updateMortalityData(int fishCount) {
        if (fishCount > 0) {
            int estimatedDead = (int) Math.ceil(fishCount * 0.10);
            tvEstDeadFish.setText(String.valueOf(estimatedDead));

            double mortalityRate = (estimatedDead / (double) fishCount) * 100;
            tvMortalityRate.setText(String.format(Locale.US, "%.2f%%", mortalityRate));
        } else {
            tvEstDeadFish.setText("0");
            tvMortalityRate.setText("0.00%");
        }
    }

}
