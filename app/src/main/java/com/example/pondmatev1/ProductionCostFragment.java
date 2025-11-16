package com.example.pondmatev1;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Chunk;

import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ProductionCostFragment extends Fragment {
    TextView tvBreed, tvCount, tvAmountPerPiece, tvTotalCost;
    TextView tvSummaryFingerlings, tvSummaryFeeds, tvSummaryMaintenance, tvSummaryTotal;
    TextView tvEstimatedRoI;
    TextView tvSummarySalary, tvSamplingFeedBreakdown;
    LinearLayout LlMaintenanceBreakdown;
    private double salaryPerPond = 0.0;

    private double totalCost = 0.0;
    private String pondName = "";
    private String currentBreed = "";
    private int currentFishCount = 0;
    Button btnFeedLogs, btnSampling, btnViewROIBreakdown;
    File generatedPdfFile;
    private int selectedCycleMonths = 6;
    private String pondId;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_production_cost, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences pondPrefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = pondPrefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
            currentBreed = pond.getBreed();
            currentFishCount = pond.getFishCount();
            pondId = pond.getId();
            setupStockFingerlingsButton(view, pond);
            handleFingerlingStockedStatus(view, pond);
        }

        loadMaintenanceTotal();
        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            String pondId = pond.getId(); // Make sure this is actually the numeric ID from your DB
            Log.d("FEED_DEBUG", "Pond ID sent: " + pondId);
            fetchTotalFeedCost(pondId);
        }

        tvSummarySalary = view.findViewById(R.id.tvSummarySalary);
        btnFeedLogs = view.findViewById(R.id.btnFeedLogs);

        tvBreed = view.findViewById(R.id.fishbreedpcostdisplay);
        tvCount = view.findViewById(R.id.numoffingerlings);
        tvAmountPerPiece = view.findViewById(R.id.amtperpiece);
        tvTotalCost = view.findViewById(R.id.amtoffingerlings);

        tvSummaryFingerlings = view.findViewById(R.id.tvSummaryFingerlings);
        tvSummaryFeeds = view.findViewById(R.id.tvSummaryFeeds);
        tvSummaryMaintenance = view.findViewById(R.id.tvSummaryMaintenance);
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal);

        tvEstimatedRoI = view.findViewById(R.id.tvEstimatedROI);
        btnViewROIBreakdown = view.findViewById(R.id.btnViewROIBreakdown);

        tvSamplingFeedBreakdown = view.findViewById(R.id.tvSamplingFeedBreakdown);
        LlMaintenanceBreakdown = view.findViewById(R.id.layoutMaintenanceBreakdown);

        computeEstimatedROI();

        btnSampling = view.findViewById(R.id.btnSampling);

        btnSampling.setOnClickListener(v -> openSamplingDialog());

        if (pondId != null && !pondId.trim().isEmpty()) {
            updateSamplingButtonState(pondId);
        }


        Button btnAddMaintenance = view.findViewById(R.id.btnAddProductionCost);
        btnAddMaintenance.setOnClickListener(v -> showAddMaintenanceDialog());
        btnFeedLogs.setOnClickListener(v -> showFeedLogs());
        Button btnGenerateReport = view.findViewById(R.id.btnGenerateReport);
        btnGenerateReport.setOnClickListener(v -> {
            if (pondJson != null) {
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                String pondNameLocal = pond.getName();
                String pondId = pond.getId();
                if (pondId == null || pondId.trim().isEmpty()) {
                    Toast.makeText(getContext(), "Missing Pond ID — please re-sync or reopen the pond.", Toast.LENGTH_SHORT).show();
                    return;
                }

                PondSyncManager.fetchPondReportData(pondNameLocal, new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                JSONObject json = new JSONObject(String.valueOf(response));
                                Log.d("REPORT_DEBUG", "Report JSON: " + json.toString());
                                // ✅ Standard report (no forced INACTIVE)
                                json.put("action", "REPORT");

                                // ✅ Ensure report + expenses section exists
                                JSONObject report = json.optJSONObject("report");
                                if (report == null) {
                                    report = new JSONObject();
                                    json.put("report", report);
                                }

                                JSONObject expenses = report.optJSONObject("expenses");
                                if (expenses == null) {
                                    expenses = new JSONObject();
                                    report.put("expenses", expenses);
                                }

                                JSONObject fingerlings = new JSONObject();
                                JSONArray fingerlingDetails = new JSONArray();
                                JSONObject fingerlingEntry = new JSONObject();

                                fingerlingEntry.put("description", tvBreed.getText().toString());
                                fingerlingEntry.put("quantity", tvCount.getText().toString());
                                fingerlingEntry.put("cost_per_unit", tvAmountPerPiece.getText().toString().replace("₱", ""));
                                fingerlingEntry.put("amount", tvTotalCost.getText().toString().replace("₱", ""));
                                fingerlingDetails.put(fingerlingEntry);

                                fingerlings.put("details", fingerlingDetails);
                                fingerlings.put("total_cost", tvTotalCost.getText().toString().replace("₱", ""));
                                expenses.put("Fingerlings", fingerlings);

                                JSONObject salarySection = new JSONObject();
                                JSONArray salaryDetails = new JSONArray();
                                JSONObject salaryEntry = new JSONObject();

                                double monthlySalary = salaryPerPond; // already computed in fragment
                                int months = selectedCycleMonths;
                                double totalSalary = monthlySalary * months;

                                salaryEntry.put("description", "Caretaker Salary (" + months + " months)");
                                salaryEntry.put("amount", totalSalary);
                                salaryDetails.put(salaryEntry);

                                salarySection.put("details", salaryDetails);
                                salarySection.put("total_cost", totalSalary);
                                expenses.put("Salary", salarySection);

                                File pdfFile = PondPDFGenerator.generatePDF(requireContext(), json, pondId);
                                if (pdfFile != null && pdfFile.exists()) {
                                    previewPDF(pdfFile);
                                    savePDFToDownloads(pdfFile);
                                    Toast.makeText(getContext(), "Report generated for " + pond.getName(), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            // ✅ Show error in Toast
                            Toast.makeText(getContext(), "Network Error: " + error, Toast.LENGTH_SHORT).show();

                            // ✅ Also log the error in Logcat
                            Log.e("NET ERROR", "Network Error: " + error);
                        });
                    }
                });
            } else {
                Toast.makeText(getContext(), "No pond selected", Toast.LENGTH_SHORT).show();
            }
        });

        Spinner spinnerMonths = view.findViewById(R.id.spinnerMonths);

        int savedMonths = pondPrefs.getInt("selected_cycle_months", 6);
        selectedCycleMonths = savedMonths;

        spinnerMonths.post(() -> {
            int index = Math.max(0, Math.min(savedMonths - 1, spinnerMonths.getCount() - 1));
            try {
                spinnerMonths.setSelection(index, false);
            } catch (NoSuchMethodError name) {
                spinnerMonths.setSelection(index);
            }
            loadSalarySummary(savedMonths);
        });

        btnViewROIBreakdown.setOnClickListener(v -> {
            if (ROIHelper.lastBreakdown == null) {
                Toast.makeText(getContext(), "ROI not calculated yet.", Toast.LENGTH_SHORT).show();
                return;
            }

            showROIBreakdownDialog(ROIHelper.lastBreakdown);
        });

        spinnerMonths.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstCall = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                try {
                    String selectedLabel = parent.getItemAtPosition(position).toString();
                    int selectedMonths = Integer.parseInt(selectedLabel.split(" ")[0]);

                    Log.d("PRODUCTION_COST", "Selected months: " + selectedMonths);

                    SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                    prefs.edit().putInt("selected_cycle_months", selectedMonths).apply();

                    selectedCycleMonths = selectedMonths;
                    loadSalarySummary(selectedMonths);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);

            String breed = pond.getBreed();
            int fishCount = pond.getFishCount();
            double costPerFish = pond.getCostPerFish();

            tvBreed.setText(breed);
            tvCount.setText(String.valueOf(fishCount));
            tvAmountPerPiece.setText("₱" + formatPrice(costPerFish));

            double totalFingerlingCost = fishCount * costPerFish;

            tvTotalCost.setText("₱" + formatPrice(totalFingerlingCost));
            tvSummaryFingerlings.setText("₱" + formatPrice(totalFingerlingCost));

            double feedCost = 0.0;
            double maintenanceCost = 0.0;


            tvSummaryFeeds.setText("₱" + formatPrice(feedCost));
            totalCost = totalFingerlingCost + feedCost + maintenanceCost + salaryPerPond;
            tvSummaryTotal.setText("₱" + formatPrice(totalCost));

            updateTotalCost();
        }

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
        }

        if (!pondName.isEmpty()) {
            SharedPreferences sp =  requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
            float savedEstimatedROI = sp.getFloat(pondName + "_roi_diff", -1f);

            if (savedEstimatedROI != -1f) {
                tvEstimatedRoI.setText(formatPrice(savedEstimatedROI) + "%");
            }
        }
    }



    private void showStockFingerlingsDialog(PondModel pond) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_stock_fingerlings, null);
        builder.setView(dialogView);

        // ✅ Pond info fields
        TextView tvPondName = dialogView.findViewById(R.id.tvPondName);
        TextView tvPondArea = dialogView.findViewById(R.id.tvPondArea);
        TextView tvStockingDate = dialogView.findViewById(R.id.tvDateStocking);
        TextView tvHarvestDate = dialogView.findViewById(R.id.tvHarvestDate);

        // ✅ Set pond details
        double rawPondArea = pond != null ? pond.getPondArea() : 0;
        if (pond != null) {
            tvPondName.setText(pond.getName() != null ? pond.getName() : "—");
            tvPondArea.setText(String.format(Locale.getDefault(), "%.2f sqm", pond.getPondArea()));
            tvStockingDate.setText(pond.getDateStocking() != null ? pond.getDateStocking() : "—");
            tvHarvestDate.setText(pond.getDateHarvest() != null ? pond.getDateHarvest() : "—");
        }

        // ✅ Input fields
        Spinner spinnerSpecies = dialogView.findViewById(R.id.spinnerSpecies);
        EditText etFishCount = dialogView.findViewById(R.id.etFishCount);
        EditText etUnitCost = dialogView.findViewById(R.id.etUnitCost);
        TextView tvTotalCost = dialogView.findViewById(R.id.tvTotalFingerlingsCost);
        Button btnConfirm = dialogView.findViewById(R.id.btnSavePond);
        TextView btnCancel = dialogView.findViewById(R.id.btnClose);

        // ✅ Species options
        List<String> speciesList = new ArrayList<>();
        speciesList.add("Bangus");
        speciesList.add("Tilapia");

        ArrayAdapter<String> speciesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                speciesList
        );
        spinnerSpecies.setAdapter(speciesAdapter);

        // ✅ Compute total
        final Runnable computeTotal = () -> {
            String fishCountStr = etFishCount.getText().toString().trim();
            String unitCostStr = etUnitCost.getText().toString().trim();

            if (fishCountStr.isEmpty() || unitCostStr.isEmpty()) {
                tvTotalCost.setText("₱0.00");
                return;
            }

            try {
                double total = Double.parseDouble(fishCountStr) * Double.parseDouble(unitCostStr);
                tvTotalCost.setText(String.format(Locale.getDefault(), "₱%.2f", total));
            } catch (NumberFormatException e) {
                tvTotalCost.setText("₱0.00");
            }
        };

        // ✅ Real-time stocking density checker
        final Runnable checkStockingDensityRealtime = () -> {
            String breed = spinnerSpecies.getSelectedItem() != null ? spinnerSpecies.getSelectedItem().toString() : "";
            String fishCountStr = etFishCount.getText().toString().trim();

            if (fishCountStr.isEmpty() || rawPondArea <= 0) {
                etFishCount.setError(null);
                return;
            }

            try {
                double fishCount = Double.parseDouble(fishCountStr);
                double pondArea = rawPondArea;
                double density = fishCount / pondArea;

                double minRecommended = 0;
                double maxAllowed = 0;

                switch (breed) {
                    case "Tilapia":
                        minRecommended = 2.0;
                        maxAllowed = 4.0;
                        break;
                    case "Bangus":
                        minRecommended = 0.8;
                        maxAllowed = 1.2;
                        break;
                }

                double minFishCount = minRecommended * pondArea;
                double maxFishCount = maxAllowed * pondArea;

                if (density > maxAllowed) {
                    etFishCount.setError(String.format(Locale.getDefault(),
                            "⚠️ Overstocked! Recommended: %.0f–%.0f fish.", minFishCount, maxFishCount));
                } else if (density < minRecommended) {
                    etFishCount.setError(String.format(Locale.getDefault(),
                            "⚠️ Understocked! Recommended: %.0f–%.0f fish.", minFishCount, maxFishCount));
                } else {
                    etFishCount.setError(null);
                }

            } catch (NumberFormatException e) {
                etFishCount.setError(null);
            }
        };

        // ✅ Auto-set unit cost by species
        spinnerSpecies.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = spinnerSpecies.getSelectedItem().toString();
                if (selected.equalsIgnoreCase("Bangus")) {
                    etUnitCost.setText("0.50");
                } else {
                    etUnitCost.setText("0.35");
                }
                computeTotal.run();
                checkStockingDensityRealtime.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ✅ Real-time total + density checking
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                computeTotal.run();
                checkStockingDensityRealtime.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etFishCount.addTextChangedListener(watcher);
        etUnitCost.addTextChangedListener(watcher);

        // ✅ Dialog creation
        AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String species = spinnerSpecies.getSelectedItem().toString();
            String fishCountStr = etFishCount.getText().toString().trim();
            String unitCostStr = etUnitCost.getText().toString().trim();
            String totalCostStr = tvTotalCost.getText().toString().replace("₱", "").trim();

            if (fishCountStr.isEmpty() || unitCostStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double fishCount = Double.parseDouble(fishCountStr);
                double pondArea = rawPondArea;
                double density = fishCount / pondArea;

                double minRecommended = 0;
                double maxAllowed = 0;

                switch (species) {
                    case "Tilapia":
                        minRecommended = 2.0;
                        maxAllowed = 4.0;
                        break;
                    case "Bangus":
                        minRecommended = 0.8;
                        maxAllowed = 1.2;
                        break;
                }

                if (density > maxAllowed) {
                    Toast.makeText(requireContext(),
                            "❌ Overstocked! Please reduce fish count.", Toast.LENGTH_LONG).show();
                    return;
                } else if (density < minRecommended) {
                    Toast.makeText(requireContext(),
                            "⚠️ Understocked! Please increase fish count.", Toast.LENGTH_LONG).show();
                    return;
                }

                // ✅ Confirmation dialog before saving
                new AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Stocking")
                        .setMessage("Are you sure you want to add these fingerlings?")
                        .setPositiveButton("Yes", (dialogConfirm, which) -> {

                            // 🔹 Now sync with your PHP server
                            PondSyncManager.stockFingerlingsOnServer(
                                    tvPondName.getText().toString(),
                                    spinnerSpecies.getSelectedItem().toString(),
                                    Integer.parseInt(etFishCount.getText().toString()),
                                    Double.parseDouble(etUnitCost.getText().toString()),
                                    "0", // mortality placeholder (if not yet used)
                                    tvHarvestDate.getText().toString(),
                                    new PondSyncManager.Callback() {
                                        @Override
                                        public void onSuccess(Object result) {
                                            // ✅ Update local data after successful sync
                                            PondModel updatedPond = pond;
                                            updatedPond.setBreed(species);
                                            updatedPond.setFishCount((int) fishCount);
                                            updatedPond.setCostPerFish(Double.parseDouble(unitCostStr));

                                            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                                            prefs.edit().putString("selected_pond", new Gson().toJson(updatedPond)).apply();

                                            updateProductionCostUI(updatedPond);
                                            updateTotalCost(species, (int) fishCount);
                                            Toast.makeText(requireContext(), "✅ Fingerlings stocked successfully!", Toast.LENGTH_SHORT).show();
                                            handleFingerlingStockedStatus(requireView(), updatedPond);
                                            dialog.dismiss();
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Toast.makeText(requireContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                                        }
                                    }
                            );

                        })
                        .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                        .show();

            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid number format.", Toast.LENGTH_SHORT).show();
            }
        });



        dialog.show();
    }


    private void updateProductionCostUI(PondModel pond) {
        if (pond == null || getView() == null) return;

        TextView tvBreed = getView().findViewById(R.id.fishbreedpcostdisplay);
        TextView tvTotalStocks = getView().findViewById(R.id.numoffingerlings);
        TextView tvUnitCost = getView().findViewById(R.id.amtperpiece);
        TextView tvTotalCost = getView().findViewById(R.id.amtoffingerlings);
        TextView tvSummaryFingerlings = getView().findViewById(R.id.tvSummaryFingerlings);

        // Species (breed)
        tvBreed.setText(pond.getBreed() != null ? pond.getBreed() : "--");

        // Total stocks
        tvTotalStocks.setText(String.valueOf(pond.getFishCount()));

        // Unit cost
        tvUnitCost.setText(String.format(Locale.getDefault(), "₱%.2f", pond.getCostPerFish()));

        // Total cost
        double totalCost = pond.getFishCount() * pond.getCostPerFish();
        tvTotalCost.setText(String.format(Locale.getDefault(), "₱%.2f", totalCost));

        // Total fingerlings cost in summary table
        tvSummaryFingerlings.setText(String.format(Locale.getDefault(), "₱%.2f", totalCost));
    }
    

    private void openSamplingDialog() {
        SamplingDialog dialog = new SamplingDialog();
        dialog.show(getParentFragmentManager(), "SamplingDialog");
    }


    public void updateSamplingButtonState(String pondId) {
        if (!isAdded()) return;

        PondSyncManager.fetchLatestSamplingRecord(pondId, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                if (!isAdded() || getActivity() == null) return;

                Log.d("SAMPLING_DEBUG", "Response: " + response);

                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || btnSampling == null) return;

                    try {
                        JSONObject json = new JSONObject(response.toString());
                        String nextSamplingDate = json.optString("next_sampling_date", "").trim();

                        // If no stored sampling yet → ENABLE
                        if (nextSamplingDate.isEmpty() || nextSamplingDate.equalsIgnoreCase("null")) {
                            setSamplingButtonEnabled(true);
                            return;
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date today = sdf.parse(sdf.format(new Date())); // normalize
                        Date next = sdf.parse(nextSamplingDate);

                        // today >= next ? enable : disable
                        boolean allowSampling = !today.before(next);
                        setSamplingButtonEnabled(allowSampling);

                    } catch (Exception e) {
                        setSamplingButtonEnabled(true); // fail-safe
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> setSamplingButtonEnabled(true));
            }
        });
    }


    private void showFeedLogs() {
        BlindFeedingFragment feedLogsDialog = new BlindFeedingFragment();
        feedLogsDialog.show(getParentFragmentManager(), "feedLogsDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSamplingButtonState(pondId);
        refreshROI();
        try {
            View root = getView();
            if (root == null) return;
            Spinner spinnerMonths = root.findViewById(R.id.spinnerMonths);
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            int savedMonths = prefs.getInt("selected_cycle_months", selectedCycleMonths);
            int index = Math.max(0, Math.min(savedMonths - 1, spinnerMonths.getCount() - 1));
            spinnerMonths.post(() -> {
                try {
                    spinnerMonths.setSelection(index, false);
                } catch (NoSuchMethodError name) {
                    spinnerMonths.setSelection(index);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setupStockFingerlingsButton(View view, PondModel pond) {
        Button btnStockFingerlings = view.findViewById(R.id.btnStockFingerlings);

        String stockingDate = pond.getDateStocking();

        if (stockingDate != null && !stockingDate.trim().isEmpty() && !stockingDate.equalsIgnoreCase("null")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date today = sdf.parse(sdf.format(new Date()));
                Date pondStockingDate = sdf.parse(stockingDate);

                if (!today.equals(pondStockingDate)) {
                    btnStockFingerlings.setVisibility(View.VISIBLE);
                    btnStockFingerlings.setEnabled(false);
                    btnStockFingerlings.setBackgroundTintList(
                            ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray)
                    );

                    btnStockFingerlings.setOnClickListener(v ->
                            Toast.makeText(requireContext(),
                                    "Can't add fingerlings yet — finish pond preparation first.",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                } else {
                    btnStockFingerlings.setVisibility(View.VISIBLE);
                    btnStockFingerlings.setEnabled(true);
                    btnStockFingerlings.setBackgroundTintList(
                            ContextCompat.getColorStateList(requireContext(), R.color.blue_pond_btn)
                    );

                    btnStockFingerlings.setOnClickListener(v -> showStockFingerlingsDialog(pond));

                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            btnStockFingerlings.setVisibility(View.GONE);
        }
    }

    private void setSamplingButtonEnabled(boolean enabled) {
        btnSampling.setEnabled(enabled);

        if (enabled) {
            btnSampling.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.blue_pond_btn)
            );
            btnSampling.setOnClickListener(v -> openSamplingDialog());

        } else {
            btnSampling.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray)
            );
            btnSampling.setOnClickListener(v ->
                    Toast.makeText(requireContext(),
                            "Next sampling is not due yet.",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }





    private void handleFingerlingStockedStatus(View view, PondModel pond) {
        Button btnStockFingerlings = view.findViewById(R.id.btnStockFingerlings);

        if (pond != null && pond.getBreed() != null &&
                !pond.getBreed().trim().isEmpty() &&
                !pond.getBreed().equalsIgnoreCase("null")) {

            btnStockFingerlings.setEnabled(false);
            btnStockFingerlings.setText("Stocking Done");
            btnStockFingerlings.setBackgroundColor(Color.GRAY);

            Toast.makeText(requireContext(), "Fingerlings already stocked for this pond.", Toast.LENGTH_SHORT).show();
        } else {
            btnStockFingerlings.setEnabled(true);
            btnStockFingerlings.setText("Fingerlings");
            btnStockFingerlings.setBackgroundColor(getResources().getColor(R.color.blue_pond_btn)); // replace with your original color
        }
    }

    private void fetchTotalFeedCost(String pondId) {
        new Thread(() -> {
            try {
                if (pondId == null || pondId.trim().isEmpty()) return;

                String urlString = "https://pondmate.alwaysdata.net/fetch_total_feed_cost.php?pond_id=" + pondId;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    Log.d("FEED_DEBUG", "PHP response: " + sb);

                    JSONObject json = new JSONObject(sb.toString());

                    if (json.optString("status").equalsIgnoreCase("success")) {

                        final double blindFeed = json.optDouble("blind_feed_total", 0);
                        final double samplingFeed = json.optDouble("sampling_feed_total", 0);
                        final double totalFeedCost = json.optDouble("total_feed_cost", 0);

                        Log.d("FEED_DEBUG", "Blind Feed = " + blindFeed);
                        Log.d("FEED_DEBUG", "Sampling Feed = " + samplingFeed);
                        Log.d("FEED_DEBUG", "Total Feed Cost = " + totalFeedCost);

                        requireActivity().runOnUiThread(() -> {
                            View view = getView();
                            if (view != null) {
                                tvSamplingFeedBreakdown.setText("₱" + formatPrice(blindFeed));

                                // Accumulated Feed Cost from sampling cycles
                                tvSummaryFeeds.setText("₱" + formatPrice(samplingFeed));

                                // No total feed cost displayed anymore.

                                updateTotalCost(); // keep this since other expenses still use it
                            }
                        });
                    }
                } else {
                    Log.e("FeedCost", "Server returned: " + responseCode);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("FeedCost", "Error: ", e);
            }
        }).start();
    }

    private void loadSalarySummary(int months) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                String pondJson = prefs.getString("selected_pond", null);
                if (pondJson == null) return;

                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                String pondId = pond.getId();

                if (pondId == null || pondId.trim().isEmpty()) return;

                URL url = new URL("https://pondmate.alwaysdata.net/get_salary_by_pond.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_id=" + URLEncoder.encode(pondId, "UTF-8") +
                        "&months=" + URLEncoder.encode(String.valueOf(months), "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes());
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.getString("status").equals("success")) {
                    double caretakerCost = json.getDouble("caretaker_cost_share");
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        salaryPerPond = caretakerCost;
                        if (tvSummarySalary != null) {
                            tvSummarySalary.setText("₱" + formatPrice(caretakerCost));
                        }
                        if (isAdded()) {
                            updateTotalCost(currentBreed, currentFishCount);
                        }

                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load caretaker salary", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
    private void updateTotalCost() {
        if (!isAdded()) return; // ✅ prevent call if fragment not attached
        updateTotalCost(currentBreed, currentFishCount);
    }

    private void updateTotalCost(String breed, int fishCount) {
        if (!isAdded() || getContext() == null) return; // ✅ safe guard

        double fingerlings = parseDouble(tvSummaryFingerlings.getText().toString());
        double blindFeed = parseDouble(tvSamplingFeedBreakdown.getText().toString()); // ✅ ADD THIS
        double accumulatedFeed = parseDouble(tvSummaryFeeds.getText().toString());
        double maintenance = parseDouble(tvSummaryMaintenance.getText().toString());

        totalCost = fingerlings + blindFeed + accumulatedFeed + maintenance + salaryPerPond; // ✅ UPDATED

        tvSummaryTotal.setText("₱" + formatPrice(totalCost));

        if (!pondName.isEmpty()) {
            SharedPreferences prefs = getContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson != null) {
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                computeEstimatedROI();
            }
        }
    }

    private void previewPDF(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(requireContext(), "PDF not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getContext(), PdfPreviewActivity.class);
        intent.putExtra(PdfPreviewActivity.EXTRA_PDF_PATH, pdfFile.getAbsolutePath());
        startActivity(intent);
    }
    private void savePDFToDownloads(File sourceFile) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "pond_report_" + System.currentTimeMillis() + ".pdf");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContext().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = getContext().getContentResolver().openOutputStream(uri)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File destFile = new File(downloadsDir, "pond_report_" + System.currentTimeMillis() + ".pdf");

                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
                getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destFile)));
            }
            Toast.makeText(getContext(), "Saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void loadMaintenanceTotal() {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_maintenance_total.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "name=" + pondName;
                conn.getOutputStream().write(postData.getBytes("UTF-8"));

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject obj = new JSONObject(response.toString());

                if (obj.getString("status").equals("success")) {

                    double maintenanceCost = obj.getDouble("total_maintenance");
                    JSONArray details = obj.optJSONArray("details");

                    requireActivity().runOnUiThread(() -> {

                        // ✅ Show TOTAL cost only
                        tvSummaryMaintenance.setText("₱" + formatPrice(maintenanceCost));

                        // ✅ Show breakdown separately
                        StringBuilder breakdown = new StringBuilder();
                        breakdown.append("Breakdown of Expenses:\n\n");

                        if (details != null && details.length() > 0) {
                            for (int i = 0; i < details.length(); i++) {
                                JSONObject item = details.optJSONObject(i);
                                breakdown.append("• ")
                                        .append(item.optString("description", "-"))
                                        .append(" — ₱")
                                        .append(formatPrice(item.optDouble("amount", 0)))
                                        .append("\n");
                            }
                        } else {
                            breakdown.append("No maintenance expenses recorded.");
                        }

                        LlMaintenanceBreakdown.removeAllViews(); // Clear previous rows

                        if (details != null && details.length() > 0) {
                            for (int i = 0; i < details.length(); i++) {
                                JSONObject d = details.optJSONObject(i);
                                if (d == null) continue;

                                String desc = d.optString("description", "-");
                                double amt = d.optDouble("amount", 0);

                                // Create a row
                                LinearLayout row = new LinearLayout(requireContext());
                                row.setOrientation(LinearLayout.HORIZONTAL);
                                row.setPadding(12, 6, 12, 6);

                                TextView tvDesc = new TextView(requireContext());
                                tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                                tvDesc.setText(desc);
                                tvDesc.setTextSize(14);
                                tvDesc.setTextColor(Color.parseColor("#424242"));

                                TextView tvAmt = new TextView(requireContext());
                                tvAmt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                                tvAmt.setText("₱" + formatPrice(amt));
                                tvAmt.setTextSize(14);
                                tvAmt.setTextColor(Color.parseColor("#424242"));
                                tvAmt.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);

                                row.addView(tvDesc);
                                row.addView(tvAmt);
                                LlMaintenanceBreakdown.addView(row);
                            }
                        } else {
                            TextView empty = new TextView(requireContext());
                            empty.setText("No maintenance records.");
                            empty.setTextSize(14);
                            empty.setPadding(12, 6, 12, 6);
                            LlMaintenanceBreakdown.addView(empty);
                        }

                        // ✅ Recompute final total
                        double fingerlings = parseDouble(tvSummaryFingerlings.getText().toString().replace("₱",""));
                        double feeds = parseDouble(tvSummaryFeeds.getText().toString().replace("₱",""));
                        double total = fingerlings + feeds + maintenanceCost;

                        tvSummaryTotal.setText("₱" + formatPrice(total));
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

        List<String> types = Arrays.asList("Supplies & Material", "Repairs & Maintenance", "Miscellaneous Expenses", "Labor", "Others");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                etOtherType.setVisibility(selected.equals("Others") ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String selectedType = spinnerType.getSelectedItem().toString();
            String description = selectedType.equals("Others") ? etOtherType.getText().toString().trim() : selectedType;
            String costStr = etCost.getText().toString().trim();

            if (description.isEmpty() || costStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;}

            double amount;
            try {
                amount = Double.parseDouble(costStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid cost", Toast.LENGTH_SHORT).show();
                return;}

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Expenses")
                    .setMessage("Are you sure you want to add this Expenses?\n\n" +
                            "Pond: " + pondName + "\n" +
                            "Type: " + description + "\n" +
                            "Cost: ₱" + amount)
                    .setPositiveButton("Yes", (dialogConfirm, which) -> {
                        PondSyncManager.uploadMaintenanceToServer(pondName, description, amount, new PondSyncManager.Callback() {
                            @Override
                            public void onSuccess(Object result) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(),
                                            "Expense added for " + pondName + ": " + description + " ₱" + amount,
                                            Toast.LENGTH_SHORT).show();
                                    loadMaintenanceTotal();
                                });
                            }
                            @Override
                            public void onError(String error) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(),
                                            "Error uploading expenses: " + error,
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        dialog.show();
    }

    private void computeEstimatedROI() {
        Log.d("ROI_DEBUG", "Starting computeEstimatedROI()...");
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson == null) {
                tvEstimatedRoI.setText("—");
                return;
            }
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            if (pond == null) {
                tvEstimatedRoI.setText("—");
                return;
            }
            double fingerlingsCount = pond.getFishCount();
            double mortalityRate = pond.getMortalityRate();
            double survivalRate = 100 - mortalityRate;
            String breed = pond.getBreed();
            PondSyncManager.fetchFarmgatePrice(breed, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object result) {

                    try {
                        JSONObject json = new JSONObject(result.toString());
                        if (!json.optString("status").equals("success")) {
                            updateROIText("—");
                            return;
                        }

                        double farmGatePrice = json.optDouble("price", 0.0);
                        if (farmGatePrice <= 0) {
                            updateROIText("—");
                            return;
                        }

                        // Harvest & Revenue Projection
                        double totalHarvestKg = (fingerlingsCount * (survivalRate / 100.0)) / 4.0;
                        double grossSales = totalHarvestKg * farmGatePrice;

                        // Fixed Costs (6 months cycle)
                        double fingerlingsCost = getDoubleFromText(tvSummaryFingerlings);
                        double salaryCost = getDoubleFromText(tvSummarySalary);
                        double supplies = 1000 * 6;
                        double misc = 1000 * 6;
                        double harvesting = grossSales * 0.02;
                        double caretaker = grossSales * 0.05;
                        double maintenance = grossSales * 0.02;

                        double baseCost = fingerlingsCost + salaryCost + supplies + misc + harvesting + caretaker + maintenance;

                        // Allocations (based on standard fish farming cost ratios)
                        double totalCost = baseCost / 0.3694;
                        double feedCost = totalCost * 0.60;
                        double fertilizerCost = totalCost * 0.0306;

                        double totalExpenses = baseCost + feedCost + fertilizerCost;
                        if (totalExpenses <= 0) {
                            updateROIText("—");
                            return;
                        }

                        double estimatedROI = ((grossSales - totalExpenses) / totalExpenses) * 100;

                        // Round numbers
                        totalHarvestKg = round(totalHarvestKg);
                        grossSales = round(grossSales);
                        feedCost = round(feedCost);
                        fertilizerCost = round(fertilizerCost);
                        totalExpenses = round(totalExpenses);
                        estimatedROI = round(estimatedROI);

                        // Save breakdown for viewing
                        ROIBreakdown b = new ROIBreakdown();
                        b.fingerlingsCount = fingerlingsCount;
                        b.mortalityRate = mortalityRate;
                        b.survivalRate = survivalRate;
                        b.farmGatePrice = farmGatePrice;
                        b.totalHarvestKg = totalHarvestKg;
                        b.grossSales = grossSales;

                        b.fingerlingsCost = fingerlingsCost;
                        b.salaryCost = salaryCost;
                        b.supplies = supplies;
                        b.misc = misc;
                        b.harvesting = harvesting;
                        b.caretaker = caretaker;
                        b.maintenance = maintenance;

                        b.feedCost = feedCost;
                        b.fertilizerCost = fertilizerCost;
                        b.totalExpenses = totalExpenses;
                        b.estimatedROI = estimatedROI;

                        ROIHelper.lastBreakdown = b;

                        updateROIText(String.format(Locale.US, "%.2f%%", estimatedROI));
                    } catch (Exception e) {
                        updateROIText("—");
                    }
                }
                @Override
                public void onError(String error) {
                    updateROIText("—");
                }
            });
        } catch (Exception e) {
            tvEstimatedRoI.setText("—");
        }
    }
    private void updateROIText(String value) {
        if (!isAdded() || getActivity() == null) {
            Log.w("ROI_DEBUG", "Fragment not attached — skipping UI update");
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (tvEstimatedRoI != null) {
                tvEstimatedRoI.setText(value);
            }
        });
    }
    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
    private void showROIBreakdownDialog(ROIBreakdown b) {
        View view = getLayoutInflater().inflate(R.layout.dialog_roi_breakdown, null);
        TableLayout table = view.findViewById(R.id.tableROI);

        addRow(table, "Fingerlings Stocked:", String.format("%.0f pcs", b.fingerlingsCount));
        addRow(table, "Mortality Rate:", String.format("%.2f%%", b.mortalityRate));
        addRow(table, "Survival Rate:", String.format("%.2f%%", b.survivalRate));

        addSpacer(table);
        addHeader(table, "PROJECTED SALES");
        addRow(table, "Harvest Weight:", String.format("%.2f kg", b.totalHarvestKg));
        addRow(table, "Farm-Gate Price:", String.format("₱%.2f/kg", b.farmGatePrice));
        addRow(table, "Gross Revenue:", String.format("₱%.2f", b.grossSales));

        addSpacer(table);
        addHeader(table, "FIXED COSTS (6 months)");
        addRow(table, "Fingerlings Cost:", formatPeso(b.fingerlingsCost));
        addRow(table, "Caretaker Salary:", formatPeso(b.salaryCost));
        addRow(table, "Supplies & Materials:", formatPeso(b.supplies));
        addRow(table, "Misc. Expenses:", formatPeso(b.misc));
        addRow(table, "Harvesting (2%):", formatPeso(b.harvesting));
        addRow(table, "Caretaker Incentives (5%):", formatPeso(b.caretaker));
        addRow(table, "Other Expenses (2%):", formatPeso(b.maintenance));

        addSpacer(table);
        addHeader(table, "VARIABLE COSTS");
        addRow(table, "Feeds (60%):", formatPeso(b.feedCost));
        addRow(table, "Fertilizer (3.06%):", formatPeso(b.fertilizerCost));

        addSpacer(table);
        addHeader(table, "TOTAL & ROI");
        addRow(table, "TOTAL EXPENSES:", formatPeso(b.totalExpenses));
        addRow(table, "ESTIMATED ROI:", String.format("%.2f%%", b.estimatedROI));

        new AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("OK", null)
                .show();
    }
    private void addRow(TableLayout table, String label, String value) {
        TableRow row = new TableRow(requireContext());

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(14);
        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        tvValue.setTextSize(14);
        row.addView(tvLabel);
        row.addView(tvValue);
        table.addView(row);
    }
    private void addHeader(TableLayout table, String text) {
        TextView header = new TextView(requireContext());
        header.setText(text);
        header.setTypeface(header.getTypeface(), Typeface.BOLD);
        header.setPadding(0, 16, 0, 4);
        table.addView(header);
    }
    private void addSpacer(TableLayout table) {
        View spacer = new View(requireContext());
        spacer.setMinimumHeight(12);
        table.addView(spacer);
    }
    private String formatPeso(double value) {
        return String.format("₱%.2f", value);
    }
    private double getDoubleFromText(TextView textView) {
        if (textView == null) {
            Log.w("ROI_DEBUG", "TextView reference is null — returning 0");
            return 0.0;
        }
        try {
            String raw = textView.getText().toString().trim();
            if (raw.isEmpty()) {
                Log.w("ROI_DEBUG", "Empty value in " + getResources().getResourceEntryName(textView.getId()) + " — returning 0");
                return 0.0;
            }
            // Remove peso signs, commas, or any other non-numeric characters
            String cleaned = raw.replaceAll("[^0-9.]", "");
            double value = cleaned.isEmpty() ? 0.0 : Double.parseDouble(cleaned);

            Log.d("ROI_DEBUG", "Parsed value from " +
                    getResources().getResourceEntryName(textView.getId()) +
                    " = " + value);
            return value;
        } catch (Exception e) {
            Log.e("ROI_DEBUG", "Error parsing value from TextView", e);
            return 0.0;
        }
    }
    private void refreshROI() {
        computeEstimatedROI();
    }
}
