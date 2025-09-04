package com.example.pondmatev1;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

public class ProductionCostFragment extends Fragment {

    TextView tvBreed, tvCount, tvAmountPerPiece, tvTotalCost;
    TextView tvSummaryFingerlings, tvSummaryFeeds, tvSummaryMaintenance, tvSummaryTotal;
    TextView tvCapital, tvROIAmount, tvROI, tvEstimatedRoI, tvRoIDifference;
    EditText etEstimatedSales, etEstimatedRevenue;

    private double totalCost = 0.0;
    private String pondName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_production_cost, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔑 Load pond from SharedPreferences (POND_PREF)
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName(); // ✅ Save it globally for this fragment
        }


        if (getArguments() != null) {
            String pondId = getArguments().getString("pond_id", "--");   // ✅ get pond_id
            String pondName = getArguments().getString("pond_name", "");

            TextView tvPondId = view.findViewById(R.id.tvPondId);
            tvPondId.setText("Pond ID: " + pondId);
        }

        Button btnAddMaintenance = view.findViewById(R.id.btnAddProductionCost);
        btnAddMaintenance.setOnClickListener(v -> showAddMaintenanceDialog());

        tvBreed = view.findViewById(R.id.fishbreedpcostdisplay);
        tvCount = view.findViewById(R.id.numoffingerlings);
        tvAmountPerPiece = view.findViewById(R.id.amtperpiece);
        tvTotalCost = view.findViewById(R.id.amtoffingerlings);

        tvSummaryFingerlings = view.findViewById(R.id.tvSummaryFingerlings);
        tvSummaryFeeds = view.findViewById(R.id.tvSummaryFeeds);
        tvSummaryMaintenance = view.findViewById(R.id.tvSummaryMaintenance);
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal);

        tvCapital = view.findViewById(R.id.tvROICapital);
        tvROIAmount = view.findViewById(R.id.tvROIAmount);
        tvROI = view.findViewById(R.id.tvROI);
        etEstimatedSales = view.findViewById(R.id.etActualSales);

        etEstimatedRevenue = view.findViewById(R.id.etEstimatedRevenue);
        tvEstimatedRoI = view.findViewById(R.id.tvEstimatedROI);
        tvRoIDifference = view.findViewById(R.id.tvROIDifference);

        // Load values from SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);
        String breed = sharedPreferences.getString("fish_breed", "--");
        String amountStr = sharedPreferences.getString("fish_amount", "0");
        String countStr = sharedPreferences.getString("number_fish", "0");

        tvBreed.setText(breed);
        tvAmountPerPiece.setText("₱" + amountStr);
        tvCount.setText(countStr);

        double amountPerPiece = parseDouble(amountStr);
        double count = parseDouble(countStr);
        double totalFingerlingCost = amountPerPiece * count;

        tvTotalCost.setText("₱" + formatPrice(totalFingerlingCost));
        tvSummaryFingerlings.setText("₱" + formatPrice(totalFingerlingCost));

        double feedCost = 0.0;
        double maintenanceCost = 0.0;

        tvSummaryFeeds.setText("₱" + formatPrice(feedCost));
        tvSummaryMaintenance.setText("₱" + formatPrice(maintenanceCost));

        totalCost = totalFingerlingCost + feedCost + maintenanceCost;
        tvSummaryTotal.setText("₱" + formatPrice(totalCost));
        tvCapital.setText("₱" + formatPrice(totalCost));

        // === Actual Sales → save ONLY <pond>_roi ===
        etEstimatedSales.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (!str.isEmpty() && !str.startsWith("₱")) {
                    etEstimatedSales.removeTextChangedListener(this);
                    etEstimatedSales.setText("₱" + str);
                    etEstimatedSales.setSelection(etEstimatedSales.getText().length());
                    etEstimatedSales.addTextChangedListener(this);
                    return;
                }

                double revenue = parseDouble(s.toString());
                double roiAmount = revenue - totalCost;
                double roiPercent = (totalCost > 0) ? (roiAmount / totalCost) * 100 : 0;

                tvROIAmount.setText("₱" + formatPrice(roiAmount));
                tvROI.setText(formatPrice(roiPercent) + "%");

                if (!pondName.isEmpty()) {
                    saveActualROI(pondName, roiPercent);
                    notifyChart();
                }
            }
        });

        // === Estimated Revenue → save ONLY <pond>_roi_diff ===
        etEstimatedRevenue.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (!str.isEmpty() && !str.startsWith("₱")) {
                    etEstimatedRevenue.removeTextChangedListener(this);
                    etEstimatedRevenue.setText("₱" + str);
                    etEstimatedRevenue.setSelection(etEstimatedRevenue.getText().length());
                    etEstimatedRevenue.addTextChangedListener(this);
                    return;
                }

                double estimatedRevenue = parseDouble(s.toString());
                double capital = parseDouble(tvCapital.getText().toString());
                double netProfit = estimatedRevenue - capital;
                double roiPercent = (capital > 0) ? (netProfit / capital) * 100 : 0;
                if (roiPercent < 25) roiPercent = 25; // Ensure minimum 25%

                tvEstimatedRoI.setText(formatPrice(roiPercent) + "%");
                tvRoIDifference.setText(formatPrice(roiPercent) + "%");

                if (!pondName.isEmpty()) {
                    saveComparisonROI(pondName, roiPercent);
                    notifyChart();
                }
            }
        });
    }

    private void notifyChart() {
        if (getActivity() instanceof ROIChartUpdater) {
            ((ROIChartUpdater) getActivity()).loadChartData();
        }
    }

    private void saveActualROI(String pond, double roiPercent) {
        SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        sp.edit().putFloat(pond + "_roi", (float) roiPercent).apply();
    }

    private void saveComparisonROI(String pond, double roiPercent) {
        SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        sp.edit().putFloat(pond + "_roi_diff", (float) roiPercent).apply();
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str.replace("₱", "").replace("%", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatPrice(double value) {
        return String.format("%.2f", value);
    }

    private void showAddMaintenanceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_maintenance, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Spinner spinnerType = dialogView.findViewById(R.id.spinnerMaintenanceType);
        EditText etOtherType = dialogView.findViewById(R.id.etOtherMaintenanceType);
        EditText etCost = dialogView.findViewById(R.id.etMaintenanceCost);
        Button btnAdd = dialogView.findViewById(R.id.btnAddMaintenance);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelMaintenance);
        TextView btnClose = dialogView.findViewById(R.id.btnClose);

        List<String> types = Arrays.asList("Water Change", "Water Monitoring", "Waste Removal", "Algae Control",
                "Cleaning Ponds & Filters", "Leak Repair", "Inspection",
                "Pump & Pipe Maintenance", "Parasite Treatment", "Net or Screen Repair", "Others");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                etOtherType.setVisibility(selected.equals("Other") ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String selectedType = spinnerType.getSelectedItem().toString();
            String description = selectedType.equals("Other") ? etOtherType.getText().toString().trim() : selectedType;
            String costStr = etCost.getText().toString().trim();

            if (description.isEmpty() || costStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(costStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid cost", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Show confirmation before uploading
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Maintenance")
                    .setMessage("Are you sure you want to add this maintenance?\n\n" +
                            "Pond: " + pondName + "\n" +
                            "Type: " + description + "\n" +
                            "Cost: ₱" + amount)
                    .setPositiveButton("Yes", (dialogConfirm, which) -> {
                        // Upload maintenance to server
                        PondSyncManager.uploadMaintenanceToServer(pondName, description, amount, new PondSyncManager.Callback() {
                            @Override
                            public void onSuccess(Object result) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(),
                                            "Maintenance added for " + pondName + ": " + description + " ₱" + amount,
                                            Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onError(String error) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(),
                                            "Error uploading maintenance: " + error,
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });

                        dialog.dismiss(); // close the add-maintenance dialog
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


        dialog.show();
    }

}
