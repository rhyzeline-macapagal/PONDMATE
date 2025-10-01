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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;

import java.util.Locale;

public class PondInfoFragment extends Fragment {
    private TextView tvBreed, tvFishCount, tvDateStarted, tvHarvestDate, tvMortalityRate, tvEstDeadFish, tvCostPerFish, tvPondName;
    private Button btnEdit;
    private PondModel pond;
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

        btnEdit = view.findViewById(R.id.btnEditPond);

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            pond = new Gson().fromJson(pondJson, PondModel.class);


            int fishCount = pond.getFishCount();
            double costPerFish = pond.getCostPerFish();

            tvPondName.setText(pond.getName());
            tvBreed.setText(pond.getBreed());
            tvFishCount.setText(String.valueOf(fishCount));
            tvCostPerFish.setText(String.valueOf(costPerFish));

            String formattedDateStarted = formatDateDisplay(pond.getDateStarted());
            String formattedDateHarvest = formatDateDisplay(pond.getDateHarvest());

            tvDateStarted.setText(formattedDateStarted);
            tvHarvestDate.setText(formattedDateHarvest);

            updateMortalityData(fishCount);

            // ✅ Save per-pond data into SharedPreferences
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
            SharedPreferences.Editor roiEditor = sharedPreferences.edit();
            String pondName = pond.getName();

            // Save per-pond date range
            roiEditor.putString(pondName + "_date_started", formattedDateStarted);
            roiEditor.putString(pondName + "_date_harvest", formattedDateHarvest);

            // Save other general pond info
            roiEditor.putString("fish_breed", tvBreed.getText().toString().trim());
            roiEditor.putString("fish_amount", tvCostPerFish.getText().toString().trim());
            roiEditor.putString("number_fish", tvFishCount.getText().toString().trim());
            roiEditor.apply();

            // Update ViewModel
            PondSharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(PondSharedViewModel.class);
            viewModel.setSelectedPond(pond);
            viewModel.setFishCount(fishCount);
        }

        PondSharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(PondSharedViewModel.class);
        String fishCountStr = tvFishCount.getText().toString();

        try {
            int fishCount = Integer.parseInt(fishCountStr);
            viewModel.setFishCount(fishCount);
        } catch (NumberFormatException e) {
            viewModel.setFishCount(0);
        }

        String userType = new SessionManager(requireContext()).getUsertype();
        btnEdit.setVisibility("owner".equalsIgnoreCase(userType) ? View.VISIBLE : View.GONE);

        btnEdit.setOnClickListener(v -> {
            ResetPondDialogFragment dialog = new ResetPondDialogFragment();
            dialog.setPond(pond); // Pass current pond object

            dialog.setOnPondResetListener(updatedPond -> {
                // ✅ Update pond object
                pond = updatedPond;

                // ✅ Update UI
                tvPondName.setText(updatedPond.getName());
                tvBreed.setText(updatedPond.getBreed());
                tvFishCount.setText(String.valueOf(updatedPond.getFishCount()));
                tvCostPerFish.setText(String.valueOf(updatedPond.getCostPerFish()));
                tvDateStarted.setText(formatDateDisplay(updatedPond.getDateStarted()));
                tvHarvestDate.setText(formatDateDisplay(updatedPond.getDateHarvest()));

                updateMortalityData(updatedPond.getFishCount());

                // ✅ Optionally sync to server
                PondSyncManager.uploadPondToServer(updatedPond, "", new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(getContext(), "Pond updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Failed to update pond: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            });

            dialog.show(getParentFragmentManager(), "ResetPondDialog");
        });

    }



    private void updateMortalityData(int fishCount) {
        if (fishCount > 0) {
            int estimatedDead = (int) Math.ceil(fishCount * 0.10);

            double mortalityRate = (estimatedDead / (double) fishCount) * 100;
            String result = String.format(Locale.US, "%.2f%% or %d pieces", mortalityRate, estimatedDead);
            tvMortalityRate.setText(result);
        } else {
            tvMortalityRate.setText("0.00% or 0 pieces");
        }
    }


    private String formatDateDisplay(String inputDate) {
        try {
            // Assuming the original date format is yyyy-MM-dd
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM. dd, yyyy", Locale.US);
            return outputFormat.format(inputFormat.parse(inputDate));
        } catch (Exception e) {
            e.printStackTrace();
            return inputDate;
        }
    }
}