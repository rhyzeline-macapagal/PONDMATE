package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ProductionCostFragment extends Fragment {

    TextView tvBreed, tvCount, tvAmountPerPiece, tvTotalCost;
    TextView tvSummaryFingerlings, tvSummaryFeeds, tvSummaryMaintenance, tvSummaryTotal;
    TextView tvCapital, tvROIAmount, tvROI;
    EditText etEstimatedSales;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_production_cost, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // For now feeds and maintenance are placeholders
        double feedCost = 0.0;  // Replace later when you implement feeds
        double maintenanceCost = 0.0; // Replace later when you add maintenance entries

        tvSummaryFeeds.setText("₱" + formatPrice(feedCost));
        tvSummaryMaintenance.setText("₱" + formatPrice(maintenanceCost));

        double totalCost = totalFingerlingCost + feedCost + maintenanceCost;

        tvSummaryTotal.setText("₱" + formatPrice(totalCost));
        tvCapital.setText("₱" + formatPrice(totalCost));

        // ROI calculation listener
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

                double estimatedRevenue = parseDouble(s.toString());
                double roiAmount = estimatedRevenue - totalCost;
                double roiPercent = (totalCost > 0) ? (roiAmount / totalCost) * 100 : 0;

                tvROIAmount.setText("₱" + formatPrice(roiAmount));
                tvROI.setText(formatPrice(roiPercent) + "%");

                saveROIToSharedPref(roiPercent);
                if (getActivity() instanceof ROIChartUpdater) {
                    ((ROIChartUpdater) getActivity()).loadChartData();
                }
            }
        });

    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str.replace("₱", "").trim());
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

        // Sample maintenance types
        List<String> types = Arrays.asList("Water Change", "Water Monitoring", "Waste Removal", "Algae Control",
                "Cleaning Ponds & Filters", "Leak Repair", "Inspection",
                "Pump & Pipe Maintenance", "Parasite Treatment", "Net or Screen Repair", "Others");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);

        // Show/hide "Other" field
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                etOtherType.setVisibility(selected.equals("Other") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Cancel or Close
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Add Maintenance Handler
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

            // Sample: You can store to database here or display it dynamically
            Toast.makeText(requireContext(), "Added: " + description + " ₱" + amount, Toast.LENGTH_SHORT).show();

            // You may call calculateTotalCapital() here if needed
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveROIToSharedPref(double roiPercent) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // to get pond name from arguments (passed from MainActivity)
        String pondName = "";
        if (getArguments() != null) {
            pondName = getArguments().getString("pond_name", "");
        }

        if (!pondName.isEmpty()) {
            editor.putFloat(pondName + "_roi", (float) roiPercent);
            editor.commit();
        }
    }


}
