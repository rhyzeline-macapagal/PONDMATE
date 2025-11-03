package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.util.Locale;

public class PondInfoFragment extends Fragment {

    private TextView tvPondName, tvBreed, tvFishCount, tvCostPerFish, tvDateStarted, tvHarvestDate, tvCaretaker;
    private TextView tvArea, tvDateStocking, tvMortalityRate;
    private Button btnEdit;
    private PondModel pond;

    public PondInfoFragment() {}

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
        tvArea = view.findViewById(R.id.tvPondArea);
        tvDateStocking = view.findViewById(R.id.tvDateStocking);
        tvMortalityRate = view.findViewById(R.id.tvMortalityRate);
        btnEdit = view.findViewById(R.id.btnEditPond);
        tvCaretaker = view.findViewById(R.id.tvCaretaker);
        loadPondData();
        setupEditButton();
    }

    private void loadPondData() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson != null) {
            pond = new Gson().fromJson(pondJson, PondModel.class);
            displayPondData(pond);
        }
    }

    private void displayPondData(PondModel pond) {
        // --- Basic info ---
        tvPondName.setText(notEmpty(pond.getName()));
        tvBreed.setText(notEmpty(pond.getBreed()));
        tvCaretaker.setText("Caretaker: " + notEmpty(pond.getCaretakerName()));

        tvFishCount.setText(pond.getFishCount() > 0
                ? String.valueOf(pond.getFishCount())
                : "Not yet added");

        tvCostPerFish.setText(pond.getCostPerFish() > 0
                ? String.format("₱%.2f", pond.getCostPerFish())
                : "Not yet added");

        // --- Pond area ---
        if (pond.getPondArea() > 0) {
            tvArea.setText(String.format(Locale.US, "%.2f sq.m", pond.getPondArea()));
        } else {
            tvArea.setText("Not yet added");
        }

        tvDateStocking.setText(notEmptyDate(pond.getDateStocking()));
        tvDateStarted.setText(notEmptyDate(pond.getDateStarted()));
        tvHarvestDate.setText(notEmptyDate(pond.getDateHarvest()));

        double mortality = pond.getMortalityRate();
        if (mortality > 0) {
            tvMortalityRate.setText(String.format(Locale.US, "%.2f%%", mortality));
        } else if (pond.getFishCount() > 0) {
            updateMortalityData(pond.getFishCount());
        } else {
            tvMortalityRate.setText("Not yet added");
        }

        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("pond_name", pond.getName());
            editor.putString("pond_breed", pond.getBreed());
            editor.putFloat("pond_area", (float) pond.getPondArea());
            editor.putInt("pond_fingerlings", pond.getFishCount());
            editor.putFloat("pond_mortality_rate", (float) pond.getMortalityRate());

            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEditButton() {
        String userType = new SessionManager(requireContext()).getUsertype();
        btnEdit.setVisibility("owner".equalsIgnoreCase(userType) ? View.VISIBLE : View.GONE);

        btnEdit.setOnClickListener(v -> {
            if (pond == null) {
                Toast.makeText(getContext(), "Pond data not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            EditPondDialog dialog = new EditPondDialog(pond, updatedPond -> {

                pond = updatedPond;
                displayPondData(updatedPond);

                SharedPreferences prefs = requireContext()
                        .getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                prefs.edit().putString("selected_pond", new Gson().toJson(updatedPond)).apply();

                PondSyncManager.updatePondToServer(requireContext(), updatedPond, "", new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(getContext(), "Pond updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Failed to update pond: " + error, Toast.LENGTH_LONG).show();
                        Log.d("Pond Edit", error);
                    }
                });
            });

            dialog.show(getParentFragmentManager(), "EditPondDialog");
        });
    }


    private String notEmpty(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null")) {
            return "Not yet added";
        }
        return value;
    }

    private String notEmptyDate(String date) {
        if (date == null || date.isEmpty() || date.equalsIgnoreCase("null")) {
            return "Not yet added"; // DISPLAY ONLY
        }
        return formatDateDisplay(date);
    }

    private void updateMortalityData(int fishCount) {
        if (fishCount > 0) {
            int estimatedDead = (int) Math.ceil(fishCount * 0.10);
            double estimatedMortality = (estimatedDead / (double) fishCount) * 100;
            tvMortalityRate.setText(String.format(Locale.US, "%.2f%% or %d pieces", estimatedMortality, estimatedDead));
        } else {
            tvMortalityRate.setText("Not yet added");
        }
    }

    private String formatDateDisplay(String inputDate) {
        try {
            if (inputDate == null || inputDate.isEmpty()) return "—";
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM. dd, yyyy", Locale.US);
            return outputFormat.format(inputFormat.parse(inputDate));
        } catch (Exception e) {
            return inputDate != null ? inputDate : "—";
        }
    }
}
