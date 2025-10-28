package com.example.pondmatev1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


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
    TextView tvSummaryFingerlings, tvSummaryFeeds, tvSummaryMaintenance, tvSummaryTotal, tvActualSales;
    TextView tvCapital, tvROIAmount, tvROI, tvEstimatedRoI, tvRoIDifference;
    EditText etEstimatedSales, etEstimatedRevenue;
    TextView tvSummarySalary;
    private double salaryPerPond = 0.0;

    private double totalCost = 0.0;
    private String pondName = "";
    private String currentBreed = "";
    private int currentFishCount = 0;
    Button btnDownload, btnFeedLogs, btnSampling;

    File generatedPdfFile;

    // keep selected months in a field so other methods can reference if needed
    private int selectedCycleMonths = 6;

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
            handleFingerlingVisibility(view, pond);
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


        // --- view bindings (same as before) ---
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
        tvActualSales = view.findViewById(R.id.etActualSales);

        etEstimatedRevenue = view.findViewById(R.id.etEstimatedRevenue);
        tvEstimatedRoI = view.findViewById(R.id.tvEstimatedROI);
        tvRoIDifference = view.findViewById(R.id.tvROIDifference);

        Button btnSampling = view.findViewById(R.id.btnSampling);
        btnSampling.setOnClickListener(v -> openSamplingDialog());



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
                                // ✅ Standard report (no forced INACTIVE)
                                json.put("action", "REPORT");

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
                            Log.e("NETERROR", "Network Error: " + error);
                        });
                    }

                });

            } else {
                Toast.makeText(getContext(), "No pond selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Spinner setup with persistence
        Spinner spinnerMonths = view.findViewById(R.id.spinnerMonths);

        // Read saved value (default 6)
        int savedMonths = pondPrefs.getInt("selected_cycle_months", 6);
        selectedCycleMonths = savedMonths;

        // Ensure adapter is present (if you set spinner entries in XML you don't need to set adapter here)
        // Defer the selection until the spinner is ready
        spinnerMonths.post(() -> {
            int index = Math.max(0, Math.min(savedMonths - 1, spinnerMonths.getCount() - 1));
            try {
                // use the two-arg setSelection to avoid re-triggering animations where available
                spinnerMonths.setSelection(index, false);
            } catch (NoSuchMethodError nsme) {
                spinnerMonths.setSelection(index);
            }
            // Load salary for the saved value once spinner set
            loadSalarySummary(savedMonths);
        });

        spinnerMonths.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstCall = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                try {
                    String selectedLabel = parent.getItemAtPosition(position).toString();
                    int selectedMonths = Integer.parseInt(selectedLabel.split(" ")[0]); // gets the number before "month(s)"

                    Log.d("PRODUCTION_COST", "Selected months: " + selectedMonths);

                    // Save to prefs
                    SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                    prefs.edit().putInt("selected_cycle_months", selectedMonths).apply();

                    // Update field and reload summary
                    selectedCycleMonths = selectedMonths;
                    loadSalarySummary(selectedMonths);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // fill UI with pond data
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
            tvCapital.setText("₱" + formatPrice(totalCost));

            updateTotalCost();
            calculateEstimatedROI(totalCost);
        }

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
        }
        TextView feederTypeTv = view.findViewById(R.id.feedtypefeeders);

        if (pondName != null) {
            PondSyncManager.fetchFeederTypeByName(pondName, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object result) {
                    if (getActivity() == null) return; // prevent crash if fragment detached
                    requireActivity().runOnUiThread(() -> {
                        try {
                            JSONObject json = (JSONObject) result;
                            String feederType = json.optString("feeder_type", "N/A");
                            int pondAgeDays = json.optInt("pond_age_days", 0);

                            // 🔹 Update the UI
                            feederTypeTv.setText(feederType);

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "Parse error", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } else {
            Toast.makeText(requireContext(), "No pond name provided", Toast.LENGTH_SHORT).show();
        }

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
                tvActualSales.setText("");
            }

            if (savedEstimatedROI != -1f) {
                tvEstimatedRoI.setText(formatPrice(savedEstimatedROI) + "%");
                tvRoIDifference.setText(formatPrice(savedEstimatedROI) + "%");
                etEstimatedRevenue.setText("");
            }
        }
    }

    private void openSamplingDialog() {
        SamplingDialog dialog = new SamplingDialog();
        dialog.show(getParentFragmentManager(), "SamplingDialog");
    }


    private void showFeedLogs() {
        BlindFeedingFragment feedLogsDialog = new BlindFeedingFragment();
        feedLogsDialog.show(getParentFragmentManager(), "feedLogsDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        // re-apply saved months selection in case fragment was recreated/resumed
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
                } catch (NoSuchMethodError nsme) {
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
                Date today = sdf.parse(sdf.format(new Date())); // current date
                Date pondStockingDate = sdf.parse(stockingDate); // stored date

                if (!today.equals(pondStockingDate)) {
                    // 🚫 Not stocking day → gray and disabled
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
                    // ✅ Stocking date is today → enable button (blue color)
                    btnStockFingerlings.setVisibility(View.VISIBLE);
                    btnStockFingerlings.setEnabled(true);
                    btnStockFingerlings.setBackgroundTintList(
                            ContextCompat.getColorStateList(requireContext(), R.color.blue_pond_btn)
                    );

                    // 🔹 Open your DialogFragment here
                    btnStockFingerlings.setOnClickListener(v -> {
                        StockFingerlingsDialog dialog = new StockFingerlingsDialog(pond);
                        dialog.show(getParentFragmentManager(), "StockFingerlingsDialog");
                    });
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            btnStockFingerlings.setVisibility(View.GONE);
        }

    }





    private void handleFingerlingVisibility(View view, PondModel pond) {
        // Find the buttons and sections
        Button btnAddProductionCost = view.findViewById(R.id.btnAddProductionCost);
        Button btnStockFingerlings = view.findViewById(R.id.btnStockFingerlings);
        Button btnGenerateReport = view.findViewById(R.id.btnGenerateReport);

        // Identify the major layout sections (cards)
        LinearLayout pondInfoSection = view.findViewById(R.id.pondInformationSection);
        LinearLayout productionCostSummary = view.findViewById(R.id.productionCostSummarySection);
        LinearLayout roiSection = view.findViewById(R.id.roiSection);

        // ✅ Defensive null check
        if (pond == null) {
            btnAddProductionCost.setVisibility(View.VISIBLE);
            btnStockFingerlings.setVisibility(View.VISIBLE);
            btnGenerateReport.setVisibility(View.GONE);

            pondInfoSection.setVisibility(View.GONE);
            productionCostSummary.setVisibility(View.GONE);
            roiSection.setVisibility(View.GONE);
            return;
        }

        if (pond.getBreed() == null ||
                pond.getBreed().trim().isEmpty() ||
                pond.getBreed().equalsIgnoreCase("null")) {
            // Show only Add Production Cost and Fingerlings buttons
            btnAddProductionCost.setVisibility(View.VISIBLE);
            btnStockFingerlings.setVisibility(View.VISIBLE);
            btnGenerateReport.setVisibility(View.GONE);

            // Hide all data sections
            pondInfoSection.setVisibility(View.GONE);
            productionCostSummary.setVisibility(View.GONE);
            roiSection.setVisibility(View.GONE);

            Toast.makeText(requireContext(), "Please stock fingerlings first before viewing production cost.", Toast.LENGTH_SHORT).show();
        } else {
            // If breed exists, show all content normally
            btnAddProductionCost.setVisibility(View.VISIBLE);
            btnStockFingerlings.setVisibility(View.VISIBLE);
            btnGenerateReport.setVisibility(View.VISIBLE);

            pondInfoSection.setVisibility(View.VISIBLE);
            productionCostSummary.setVisibility(View.VISIBLE);
            roiSection.setVisibility(View.VISIBLE);
        }


    }

    private void handleFingerlingStockedStatus(View view, PondModel pond) {
        Button btnStockFingerlings = view.findViewById(R.id.btnStockFingerlings);

        if (pond != null && pond.getBreed() != null &&
                !pond.getBreed().trim().isEmpty() &&
                !pond.getBreed().equalsIgnoreCase("null")) {

            // ✅ Disable the button
            btnStockFingerlings.setEnabled(false);
            btnStockFingerlings.setText("Stocking Done");
            btnStockFingerlings.setBackgroundColor(Color.GRAY);

            Toast.makeText(requireContext(), "Fingerlings already stocked for this pond.", Toast.LENGTH_SHORT).show();
        } else {
            // ✅ Enable if no fingerlings yet
            btnStockFingerlings.setEnabled(true);
            btnStockFingerlings.setText("Fingerlings");
            btnStockFingerlings.setBackgroundColor(getResources().getColor(R.color.blue_pond_btn)); // replace with your original color
        }
    }



    private void fetchTotalFeedCost(String pondId) {
        new Thread(() -> {
            try {
                // Build URL with pond_id as GET parameter
                String urlString = "https://pondmate.alwaysdata.net/fetch_total_feed_cost.php";
                if (pondId != null && !pondId.isEmpty()) {
                    urlString += "?pond_id=" + pondId;
                }

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET"); // Use GET to match PHP
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    Log.d("FEED_DEBUG", "PHP response: " + sb.toString());

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("total_feed_cost")) {
                        final double totalFeedCost = json.getDouble("total_feed_cost");

                        // Update UI on main thread
                        requireActivity().runOnUiThread(() -> {
                            View view = getView();
                            if (view != null) {
                                TextView tvSummaryFeeds = view.findViewById(R.id.tvSummaryFeeds);
                                tvSummaryFeeds.setText("₱" + formatPrice(totalFeedCost));
                                updateTotalCost(); // recalc total after feeds loaded
                            }
                        });

                    }
                } else {
                    Log.e("FeedCost", "Server returned: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
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

                // 🔹 Call the new PHP endpoint
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
                        updateTotalCost(currentBreed, currentFishCount);
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
        updateTotalCost(currentBreed, currentFishCount);
    }

    private void updateTotalCost(String breed, int fishCount) {
        double fingerlings = parseDouble(tvSummaryFingerlings.getText().toString());
        double feeds = parseDouble(tvSummaryFeeds.getText().toString());
        double maintenance = parseDouble(tvSummaryMaintenance.getText().toString());

        totalCost = fingerlings + feeds + maintenance + salaryPerPond;

        tvSummaryTotal.setText("₱" + formatPrice(totalCost));
        tvCapital.setText("₱" + formatPrice(totalCost));

        if (!pondName.isEmpty()) {
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson != null) {
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                calculateActualSalesAndROI(pond.getBreed(), pond.getFishCount(), totalCost);
                calculateEstimatedROI(totalCost);
            }
        }
    }

    // === Actual Sales → save ONLY <pond>_roi ===
    private void calculateActualSalesAndROI(String breed, int fishCount, double totalCost) {
        double pricePerKilo = 0;
        double avgWeightGrams = 0;

        switch (breed.toLowerCase()) {
            case "tilapia":
                pricePerKilo = 120;
                avgWeightGrams = 333.33;
                break;
            case "bangus":
                pricePerKilo = 180;
                avgWeightGrams = 333.33;
                break;

        }

        // Revenue
        double tons = (fishCount * avgWeightGrams) / 1_000_000.0;
        double kilos = tons * 1000;
        double actualSalesValue = kilos * pricePerKilo;

        // ROI
        double roiAmount = actualSalesValue - totalCost;
        double roiPercent = (totalCost > 0) ? (roiAmount / totalCost) * 100 : 0;

        // Update UI
        tvActualSales.setText(String.format("₱%,.2f", actualSalesValue));
        tvROIAmount.setText(String.format("₱%,.2f", roiAmount));
        tvROI.setText(String.format("%.2f%%", roiPercent));

        if (!pondName.isEmpty()) {
            saveActualROI(pondName, roiPercent);
        }
    }

    private void calculateEstimatedROI(double totalCost) {
        // 25% industry standard ROI
        double estimatedRevenue = totalCost * 1.30;
        double estimatedROIPercent = 30.0; // fixed industry standard

        // Update UI
        etEstimatedRevenue.setText(String.format("₱%,.2f", estimatedRevenue));
        tvEstimatedRoI.setText(String.format("%.2f%%", estimatedROIPercent));
        tvRoIDifference.setText(String.format("%.2f%%",estimatedROIPercent));

        if (!pondName.isEmpty()) {
            saveEstimatedROI(pondName, estimatedROIPercent);
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }

    PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

    class BorderPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle rect = document.getPageSize();

            float offset = 20f;

            float left   = document.leftMargin() - offset;
            float right  = rect.getRight() - document.rightMargin() + offset;
            float top    = rect.getTop() - document.topMargin() + offset;
            float bottom = document.bottomMargin() - offset;

            canvas.setLineWidth(1.5f);
            canvas.rectangle(left, bottom, right - left, top - bottom);
            canvas.stroke();
        }
    }

    private String formatTime(String time24) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()); // server format
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // 12hr format
            Date date = inputFormat.parse(time24);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return time24; // fallback to original if parsing fails
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

    private void notifyChart() {
        if (getActivity() instanceof ROIChartUpdater) {
            ((ROIChartUpdater) getActivity()).loadChartData();
        }
    }

    private void saveActualROI(String pond, double roiPercent) {
        SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        sp.edit().putFloat(pond + "_roi", (float) roiPercent).apply();

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

    private void saveEstimatedROI(String pondName, double roiPercent) {
        SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        sp.edit().putFloat(pondName + "_estimated_roi", (float) roiPercent).apply();

        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_roi.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_name=" + pondName + "&estimated_roi=" + roiPercent;
                conn.getOutputStream().write(postData.getBytes("UTF-8"));

                conn.getInputStream().close();
                conn.disconnect();
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
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                JSONObject obj = new JSONObject(json);

                if (obj.getString("status").equals("success")) {
                    double maintenanceCost = obj.getDouble("total_maintenance");

                    requireActivity().runOnUiThread(() -> {
                        tvSummaryMaintenance.setText("₱" + formatPrice(maintenanceCost));

                        double fingerlings = parseDouble(tvSummaryFingerlings.getText().toString());
                        double feeds = parseDouble(tvSummaryFeeds.getText().toString());
                        double total = fingerlings + feeds + maintenanceCost;

                        tvSummaryTotal.setText("₱" + formatPrice(total));
                        tvCapital.setText("₱" + formatPrice(total));
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

        List<String> types = Arrays.asList("Supplies & Material", "Repairs & Maintenance", "Salaries", "Miscellaneous Expenses", "Labor", "Others");
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
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(costStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid cost", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Maintenance")
                    .setMessage("Are you sure you want to add this maintenance?\n\n" +
                            "Pond: " + pondName + "\n" +
                            "Type: " + description + "\n" +
                            "Cost: ₱" + amount)
                    .setPositiveButton("Yes", (dialogConfirm, which) -> {
                        PondSyncManager.uploadMaintenanceToServer(pondName, description, amount, new PondSyncManager.Callback() {
                            @Override
                            public void onSuccess(Object result) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(),
                                            "Maintenance added for " + pondName + ": " + description + " ₱" + amount,
                                            Toast.LENGTH_SHORT).show();
                                    loadMaintenanceTotal();
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

                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    private void uploadPdfAndSaveHistory(String pondId, String pondName, File pdfFile, String existingPdfPath, String actionType) {
        new Thread(() -> {
            try {
                String safePondId = (pondId != null && !pondId.trim().isEmpty()) ? pondId : "INACTIVE_POND";

                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                URL url = new URL("https://pondmate.alwaysdata.net/savePondHistory.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream outputStream = conn.getOutputStream()) {
                    // pond_id
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"pond_id\"\r\n\r\n".getBytes());
                    outputStream.write(pondId.getBytes());
                    outputStream.write("\r\n".getBytes());

                    // name
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"name\"\r\n\r\n".getBytes());
                    outputStream.write(pondName.getBytes());
                    outputStream.write("\r\n".getBytes());

                    // action
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"action\"\r\n\r\n".getBytes());
                    outputStream.write(actionType.getBytes());
                    outputStream.write("\r\n".getBytes());

                    // optional existing pdf_path
                    if (existingPdfPath != null && !existingPdfPath.isEmpty()) {
                        outputStream.write(("--" + boundary + "\r\n").getBytes());
                        outputStream.write("Content-Disposition: form-data; name=\"pdf_path\"\r\n\r\n".getBytes());
                        outputStream.write(existingPdfPath.getBytes());
                        outputStream.write("\r\n".getBytes());
                    }

                    // optional new pdf file upload
                    if (pdfFile != null && pdfFile.exists()) {
                        outputStream.write(("--" + boundary + "\r\n").getBytes());
                        outputStream.write(("Content-Disposition: form-data; name=\"pdf_file\"; filename=\"" +
                                pdfFile.getName() + "\"\r\n").getBytes());
                        outputStream.write("Content-Type: application/pdf\r\n\r\n".getBytes());

                        try (FileInputStream fileInputStream = new FileInputStream(pdfFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                        outputStream.write("\r\n".getBytes());
                    }

                    // end of form
                    outputStream.write(("--" + boundary + "--\r\n").getBytes());
                    outputStream.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 400)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                Log.d("PDF_UPLOAD", "Server Response: " + response);

                requireActivity().runOnUiThread(() -> {
                    if (response.toString().toLowerCase().contains("success")) {
                        Toast.makeText(requireContext(), "PDF successfully uploaded!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Upload completed (check logs).", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(requireContext(), "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
