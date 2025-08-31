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

import java.net.HttpURLConnection;
import java.net.URL;
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

        TextView tvPondId = view.findViewById(R.id.tvPondId);

        // ✅ Get pond_id and pond_name from arguments if available
        String pondId = "--";
        if (getArguments() != null) {
            pondId = getArguments().getString("pond_id", "--");
            this.pondName = getArguments().getString("pond_name", ""); // ✅ assign to field, not local var
        }

        // ✅ Fallback to SharedPreferences if pond_id is missing
        if (pondId == null || pondId.equals("--")) {
            SharedPreferences sp = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);
            pondId = sp.getString("pond_id", "--");
        }

        // ✅ Display Pond ID
        tvPondId.setText("Pond ID: " + pondId);

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
        tvRoIDifference = view. findViewById(R.id.tvROIDifference);

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

        // --- Load stored ROI values for this pond ---
        if (!pondName.isEmpty()) {
            SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
            float savedActualROI = sp.getFloat(pondName + "_roi", -1f);
            float savedEstimatedROI = sp.getFloat(pondName + "_roi_diff", -1f);
            String savedActualSales = sp.getString(pondName + "_actual_sales", "");
            String savedEstimatedRevenue = sp.getString(pondName + "_estimated_revenue", "");

            if (savedActualROI != -1f) {
                double roiAmountValue = parseDouble(tvCapital.getText().toString()) * savedActualROI / 100;
                tvROIAmount.setText("₱" + formatPrice(roiAmountValue));
                tvROI.setText(formatPrice(savedActualROI) + "%");

                // Optionally, populate EditText
                etEstimatedSales.setText(""); // Keep user input blank or format as needed
            }

            if (savedEstimatedROI != -1f) {
                tvEstimatedRoI.setText(formatPrice(savedEstimatedROI) + "%");
                tvRoIDifference.setText(formatPrice(savedEstimatedROI) + "%");

                // Optionally, populate EditText
                etEstimatedRevenue.setText(""); // Keep user input blank or format as needed
            }
        }

        // === Actual Sales → save ONLY <pond>_roi ===
        etEstimatedSales.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
                sp.edit().putString(pondName + "_actual_sales", str).apply();

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
                SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
                sp.edit().putString(pondName + "_estimated_revenue", str).apply();

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

        // Send immediately to server
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_roi.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_name=" + pond + "&actual_roi=" + roiPercent;
                conn.getOutputStream().write(postData.getBytes("UTF-8"));
                conn.getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void saveComparisonROI(String pond, double roiPercent) {
        SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        sp.edit().putFloat(pond + "_roi_diff", (float) roiPercent).apply();

        // Send immediately to server
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_roi.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_name=" + pond + "&estimated_roi=" + roiPercent;
                conn.getOutputStream().write(postData.getBytes("UTF-8"));
                conn.getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

            Toast.makeText(requireContext(), "Added: " + description + " ₱" + amount, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}
