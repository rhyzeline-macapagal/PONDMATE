package com.example.pondmatev1;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockFingerlingsDialog extends DialogFragment {

    private PondModel pond;
    private Spinner spinnerSpecies, spinnerMortality;
    private EditText etFishCount, etUnitCost;
    private TextView tvTotalFingerlingsCost, tvDateStocking, tvPondName, btnClose, tvPondArea;
    private Button btnSavePond;

    private String rawStockingDate = "";
    private String rawHarvestDate = "";
    private double rawPondArea = 0;

    public StockFingerlingsDialog(PondModel pond) {
        this.pond = pond;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_stock_fingerlings, container, false);

        spinnerSpecies = view.findViewById(R.id.spinnerSpecies);
        spinnerMortality = view.findViewById(R.id.spinnerMortality);
        etFishCount = view.findViewById(R.id.etFishCount);
        tvPondArea = view.findViewById(R.id.tvPondArea);
        etUnitCost = view.findViewById(R.id.etUnitCost);
        tvTotalFingerlingsCost = view.findViewById(R.id.tvTotalFingerlingsCost);
        btnSavePond = view.findViewById(R.id.btnSavePond);

        tvDateStocking = view.findViewById(R.id.tvDateStocking);
        tvPondName = view.findViewById(R.id.tvPondName);
        btnClose = view.findViewById(R.id.btnClose);
        TextView tvHarvestDate = view.findViewById(R.id.tvHarvestDate);

        // ✅ Close button (X)
        btnClose.setOnClickListener(v -> dismiss());


// ✅ Retrieve from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel savedPond = new Gson().fromJson(pondJson, PondModel.class);

            // ✅ Populate TextViews from SharedPreferences data
            // ✅ Show Pond Name
            if (savedPond.getName() != null) {
                tvPondName.setText(savedPond.getName());
            } else {
                tvPondName.setText("N/A");
            }

// ✅ Show Pond Area
            TextView tvPondArea = view.findViewById(R.id.tvPondArea);

            if (savedPond.getPondArea() > 0) {
                rawPondArea = savedPond.getPondArea();
                tvPondArea.setText(String.format(Locale.getDefault(), "%.2f", rawPondArea));
            } else {
                rawPondArea = 0;
                tvPondArea.setText("N/A");
            }


            if (savedPond.getDateStocking() != null && !savedPond.getDateStocking().isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

                    Date stockingDate = inputFormat.parse(savedPond.getDateStocking());
                    rawStockingDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(stockingDate); // ✅ keep raw format
                    String formattedStockingDate = outputFormat.format(stockingDate);
                    tvDateStocking.setText(formattedStockingDate);

                    // ✅ Compute harvest date (6 months after)
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(stockingDate);
                    cal.add(Calendar.MONTH, 6);

                    Date harvestDate = cal.getTime();
                    rawHarvestDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(harvestDate); // ✅ keep raw format
                    String formattedHarvestDate = outputFormat.format(harvestDate);
                    tvHarvestDate.setText(formattedHarvestDate);

                } catch (Exception e) {
                    e.printStackTrace();
                    tvDateStocking.setText("N/A");
                    tvHarvestDate.setText("N/A");
                }
            }
            else {
                tvDateStocking.setText("N/A");
                tvHarvestDate.setText("N/A");
            }

        }


        // ✅ Populate Species Spinner
        List<String> speciesList = new ArrayList<>();
        speciesList.add("Bangus");
        speciesList.add("Tilapia");

        ArrayAdapter<String> speciesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                speciesList
        );
        spinnerSpecies.setAdapter(speciesAdapter);

        // ✅ Populate Mortality Spinner (5% - 10%)
        List<String> mortalityList = new ArrayList<>();
        for (int i = 5; i <= 10; i++) {
            mortalityList.add(i + "%");
        }

        ArrayAdapter<String> mortalityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                mortalityList
        );
        spinnerMortality.setAdapter(mortalityAdapter);

        // ✅ Set default species selection → ensures updateUnitCostBasedOnSpecies() works
        spinnerSpecies.setSelection(0); // "Bangus" first
        updateUnitCostBasedOnSpecies(); // force initial unit cost display

        // ✅ Listener to update cost on species change
        spinnerSpecies.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateUnitCostBasedOnSpecies();
                computeTotalCost();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });


        etFishCount.addTextChangedListener(new SimpleTextWatcher(() -> {
            checkStockingDensityRealtime();
            computeTotalCost();
        }));


        etUnitCost.addTextChangedListener(new SimpleTextWatcher(this::computeTotalCost));


        btnSavePond.setOnClickListener(v -> {
            if (!checkStockingDensity()) {
                return;
            }

            // Show confirmation dialog
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Stocking")
                    .setMessage("Are you sure you want to add these fingerlings?")
                    .setPositiveButton("Yes", (dialog, which) -> {


                        PondSyncManager.stockFingerlingsOnServer(
                                tvPondName.getText().toString(),
                                spinnerSpecies.getSelectedItem().toString(),
                                Integer.parseInt(etFishCount.getText().toString()),
                                Double.parseDouble(etUnitCost.getText().toString()),
                                spinnerMortality.getSelectedItem().toString(),
                                rawHarvestDate,
                                new PondSyncManager.Callback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        Toast.makeText(getContext(), "✅ Fingerlings stocked successfully!", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                                    }
                                }
                        );


                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
        return view;
    }

        @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public interface Callback {
        void onSuccess(Object result);
        void onError(String error);
    }


    private void computeTotalCost() {
        String fishCountStr = etFishCount.getText().toString().trim();
        String unitCostStr = etUnitCost.getText().toString().replace("₱", "").trim();

        if (fishCountStr.isEmpty() || unitCostStr.isEmpty()) {
            tvTotalFingerlingsCost.setText("₱0.00");
            return;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double unitCost = Double.parseDouble(unitCostStr);
            double total = fishCount * unitCost;

            tvTotalFingerlingsCost.setText(String.format(Locale.getDefault(), "₱%.2f", total));
        } catch (NumberFormatException e) {
            tvTotalFingerlingsCost.setText("₱0.00");
        }
    }


    private void updateUnitCostBasedOnSpecies() {
        String species = spinnerSpecies.getSelectedItem() != null
                ? spinnerSpecies.getSelectedItem().toString()
                : "";

        double cost = 0.0;
        if (species.equalsIgnoreCase("Tilapia")) {
            cost = 0.35;
        } else if (species.equalsIgnoreCase("Bangus")) {
            cost = 0.50;
        }

        etUnitCost.setText(String.format(Locale.getDefault(), "%.2f", cost));
    }


    private void checkStockingDensityRealtime() {
        String breed = spinnerSpecies.getSelectedItem() != null ? spinnerSpecies.getSelectedItem().toString() : "";
        String fishCountStr = etFishCount.getText().toString().trim();

        if (fishCountStr.isEmpty() || rawPondArea <= 0) {
            etFishCount.setError(null);
            return;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double pondArea = rawPondArea; // ✅ use raw value

            double density = fishCount / pondArea;
            double maxAllowed = 0;
            double minRecommended = 0;

            switch (breed) {
                case "Tilapia":
                    minRecommended = 3.0;
                    maxAllowed = 15.0;
                    break;
                case "Bangus":
                    minRecommended = 0.2;
                    maxAllowed = 3.0;
                    break;
            }

            if (density > maxAllowed) {
                etFishCount.setError(String.format(Locale.getDefault(),
                        "⚠️ Overstocked! %.2f fish/m² (max %.2f)", density, maxAllowed));
            } else if (density < minRecommended) {
                etFishCount.setError(String.format(Locale.getDefault(),
                        "Low density: %.2f fish/m² (min %.2f)", density, minRecommended));
            } else {
                etFishCount.setError(null);
            }

        } catch (NumberFormatException e) {
            etFishCount.setError(null);
        }
    }




    private boolean checkStockingDensity() {
        String breed = spinnerSpecies.getSelectedItem() != null ? spinnerSpecies.getSelectedItem().toString() : "";
        String fishCountStr = etFishCount.getText().toString().trim();

        if (fishCountStr.isEmpty() || rawPondArea <= 0) {
            Toast.makeText(getContext(), "Missing data: please check fish count or pond area.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double pondArea = rawPondArea; // ✅ use raw value

            double density = fishCount / pondArea;
            double maxAllowed = 0;
            double minRecommended = 0;

            switch (breed) {
                case "Tilapia":
                    minRecommended = 3.0;
                    maxAllowed = 15.0;
                    break;
                case "Bangus":
                    minRecommended = 0.2;
                    maxAllowed = 3.0;
                    break;
            }

            if (density > maxAllowed) {
                Toast.makeText(getContext(),
                        String.format(Locale.getDefault(),
                                "❌ Overstocked! %.2f fish/m² (max %.2f)", density, maxAllowed),
                        Toast.LENGTH_LONG).show();
                return false;
            }

            return true;

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid fish count format.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }



    // ✅ Inline TextWatcher
    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChange;

        public SimpleTextWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChange.run();
        }
        @Override
        public void afterTextChanged(Editable s) {}
    }
}
