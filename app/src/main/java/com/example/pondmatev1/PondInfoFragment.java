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
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;

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

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel selectedPond = new Gson().fromJson(pondJson, PondModel.class);

            int fishCount = selectedPond.getFishCount();
            double costPerFish = selectedPond.getCostPerFish();

            tvPondName.setText(selectedPond.getName());
            tvBreed.setText(selectedPond.getBreed());
            tvFishCount.setText(String.valueOf(fishCount));
            tvCostPerFish.setText(String.valueOf(costPerFish));
            tvDateStarted.setText(selectedPond.getDateStarted());
            tvHarvestDate.setText(selectedPond.getDateHarvest());

            updateMortalityData(fishCount);

            pond = selectedPond;

            PondSharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(PondSharedViewModel.class);
            viewModel.setSelectedPond(pond);
        }

        String userType = new SessionManager(requireContext()).getUsertype();
        btnEdit.setVisibility("owner".equalsIgnoreCase(userType) ? View.VISIBLE : View.GONE);

        btnEdit.setOnClickListener(v -> {
            if (!isEditing) {
                btnEdit.setText("Save");
                tvPondName.setEnabled(true);
                tvCostPerFish.setEnabled(true);
                tvPondName.requestFocus();
                isEditing = true;
            } else {
                btnEdit.setText("Edit");
                tvPondName.setEnabled(false);
                tvCostPerFish.setEnabled(false);
                // TODO: Save updated values
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