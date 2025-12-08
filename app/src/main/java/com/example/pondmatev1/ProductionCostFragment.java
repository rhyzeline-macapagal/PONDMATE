package com.example.pondmatev1;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.json.JSONException;
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
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductionCostFragment extends Fragment {
    private Button btnSelectTime1, btnSelectTime2, btnSave;
    private Dialog samplingDialog;

    private TextView tvPondName, tvNextSampling, tvDaysOfCulture, tvLifeStage, tvTotalStocks, tvMortality, tvDFRPerCycle;
    private TextView tvTimeFeeding1, tvTimeFeeding2, tvABWResult, tvDFRResult, tvFeedCost, tvRemainingFeed;
    private EditText etSampledWeight, etNumSamples, etFeedingRate;
    private TextView tvSurvivalRate; // display-only
    private int time1Minutes = -1, time2Minutes = -1;
    private String formattedTime1, formattedTime2;

    private String species = "";
    private long daysOfCulture = 1;
    private Double currentPricePerKg = null;

    private TextView tvFeedType, etFeedCost;
    private EditText etFeedQuantity, etFeedDate;
    private TableLayout tableFeedLogs;
    private Button btnAddFeed, btnCancelFeed;
    private Calendar selectedDate = Calendar.getInstance();
    private Map<String, Double> feedPriceMap = new HashMap<>();
    private boolean isLockedInFragment = false;
    private static final String PREF_LOCK_STATE = "LOCK_PREF";
    private static final String KEY_IS_LOCKED = "isLockedInBlindFeeding";

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat mysqlFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final String TAG = "BlindFeedingFragment";

    private android.app.AlertDialog loadingDialog;
    TextView tvBreed, tvCount, tvAmountPerPiece, tvTotalCost;
    TextView tvSummaryFingerlings, tvSummaryFeeds, tvSummaryMaintenance, tvSummaryTotal;
    TextView tvEstimatedRoI;
    TextView tvSummarySalary, tvSamplingFeedBreakdown;
    LinearLayout LlMaintenanceBreakdown;
    private double salaryPerPond = 0.0;

    private double totalCost = 0.0;
    private String pondName;
    private String currentBreed = "";
    private int currentFishCount = 0;
    Button btnFeedLogs, btnSampling, btnViewROIBreakdown;
    File generatedPdfFile;
    private int selectedCycleMonths = 6;
    private String pondId;
    private View view;

    private TableLayout mainTableFeedLogs;


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

        Button btnAddMaintenance = view.findViewById(R.id.btnAddProductionCost);
        btnAddMaintenance.setOnClickListener(v -> showAddMaintenanceDialog());
        btnFeedLogs.setOnClickListener(v -> showBlindFeedingDialog());

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

            if (breed == null || breed.trim().isEmpty() || breed.equalsIgnoreCase("null")) {
                breed = "Not Set";
            }

            tvBreed.setText(breed);
            tvCount.setText(formatNumber(fishCount));
            tvAmountPerPiece.setText(formatPeso(costPerFish));

            double totalFingerlingCost = fishCount * costPerFish;
            tvTotalCost.setText(formatPeso(totalFingerlingCost));
            tvSummaryFingerlings.setText(formatPeso(totalFingerlingCost));

            double feedCost = 0.0;
            double maintenanceCost = 0.0;

            tvSummaryFeeds.setText(formatPeso(feedCost));
            double totalCost = totalFingerlingCost + feedCost + maintenanceCost + salaryPerPond;
            tvSummaryTotal.setText(formatPeso(totalCost));
            updateTotalCost();
        }

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
        }

        if (!pondName.isEmpty()) {
            SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
            float savedEstimatedROI = sp.getFloat(pondName + "_roi_diff", -1f);
            if (savedEstimatedROI != -1f) tvEstimatedRoI.setText(formatNumber(savedEstimatedROI) + "%");
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
            tvPondArea.setText(String.format(Locale.getDefault(), "%.2f", pond.getPondArea()));
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
        EditText etMortality = dialogView.findViewById(R.id.etMortality);


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

                // If species not yet selected → skip check
                if (maxAllowed == 0) {
                    etFishCount.setError(null);
                    return;
                }

                double minFishCount = minRecommended * pondArea;
                double maxFishCount = maxAllowed * pondArea;

                String rangeText = String.format(Locale.getDefault(),
                        "Recommended range: %.0f – %.0f fish.", minFishCount, maxFishCount);

                if (density > maxAllowed) {
                    etFishCount.setError("❌ Overstocked! Reduce fish count.\n" + rangeText);
                    return;
                }

                if (density < minRecommended) {
                    etFishCount.setError(null); // clear red error
                    Toast.makeText(getContext(),
                            "⚠️ Below minimum stocking density.\n" + rangeText,
                            Toast.LENGTH_LONG).show();
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
            String mortalityStr = etMortality.getText().toString().trim();

            if (fishCountStr.isEmpty() || unitCostStr.isEmpty() || mortalityStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }


            try {
                double fishCount = Double.parseDouble(fishCountStr);
                double pondArea = rawPondArea;
                double mortalityRate = Double.parseDouble(mortalityStr);

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

                double minFishCount = minRecommended * pondArea;
                double maxFishCount = maxAllowed * pondArea;
                String rangeText = String.format(Locale.getDefault(),
                        "Recommended range: %.0f – %.0f fish.", minFishCount, maxFishCount);

                if (density > maxAllowed) {
                    Toast.makeText(requireContext(),
                            "❌ Overstocked! Please reduce fish count.\n" + rangeText,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (density < minRecommended) {
                    Toast.makeText(requireContext(),
                            "⚠️ Understocked! You are below the recommended stocking density.\n" + rangeText,
                            Toast.LENGTH_LONG).show();
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
                                    mortalityStr, // mortality placeholder (if not yet used)
                                    tvHarvestDate.getText().toString(),
                                    new PondSyncManager.Callback() {
                                        @Override
                                        public void onSuccess(Object result) {
                                            // ✅ Update local data after successful sync
                                            PondModel updatedPond = pond;
                                            updatedPond.setBreed(species);
                                            updatedPond.setFishCount((int) fishCount);
                                            updatedPond.setCostPerFish(Double.parseDouble(unitCostStr));
                                            updatedPond.setMortalityRate(mortalityRate);

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
    private void showBlindFeedingDialog() {
        Dialog dialog = new Dialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_blind_feeding, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        mainTableFeedLogs = view.findViewById(R.id.tableFeedLogs);
        TextView tvFeedType = view.findViewById(R.id.tvFeedType);
        EditText etFeedQuantity = view.findViewById(R.id.etFeedQuantity);
        TextView etFeedCost = view.findViewById(R.id.etFeedCost);
        EditText etFeedDate = view.findViewById(R.id.etFeedDate);
        TableLayout tableFeedLogs = view.findViewById(R.id.tableFeedLogs);
        Button btnAddFeed = view.findViewById(R.id.btnAddFeed);
        Button btnCancelFeed = view.findViewById(R.id.btnCancelFeed);
        TextView btnClose = view.findViewById(R.id.btnClose);
        tvFeedType.setText("Frymash");

        btnAddFeed.setOnClickListener(v -> addFeedLog(etFeedQuantity, etFeedDate, tableFeedLogs));
        etFeedDate.setOnClickListener(v -> showDateSelectionDialog(etFeedDate));

        final Calendar selectedDate = Calendar.getInstance();
        final boolean[] isLockedInFragment = {false};

        if (getArguments() != null) {
            pondName = getArguments().getString("pondName");
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson != null) {
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                pondName = pond.getName();
            } else {
                pondName = "Unknown Pond";
            }
        }

        SharedPreferences lockPrefs = requireContext().getSharedPreferences("LOCK_PREF", Context.MODE_PRIVATE);
        isLockedInFragment[0] = lockPrefs.getBoolean("isLockedInBlindFeeding", false);

        if (isLockedInFragment[0]) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⚠️ Locked")
                    .setMessage("You still need to complete all blind feeding logs before you can exit this section.")
                    .setPositiveButton("OK", null)
                    .show();
        }

        View.OnClickListener closeListener = v -> {
            if (isLockedInFragment[0]) {
                Toast.makeText(requireContext(),
                        "You cannot close this until all blind feeding logs are complete.",
                        Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
            }
        };
        btnCancelFeed.setOnClickListener(closeListener);
        btnClose.setOnClickListener(closeListener);

        // Intercept back button
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                if (isLockedInFragment[0]) {
                    Toast.makeText(requireContext(),
                            "You cannot close this until all blind feeding logs are complete.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            return false;
        });


        btnCancelFeed.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        etFeedQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String quantityStr = s.toString().trim();
                if (quantityStr.isEmpty()) {
                    etFeedCost.setText("");
                    return;
                }

                double quantityInGrams;
                try {
                    quantityInGrams = Double.parseDouble(quantityStr);
                } catch (NumberFormatException e) {
                    etFeedCost.setText("");
                    return;
                }

                // Get pond breed
                SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                String pondJson = prefs.getString("selected_pond", null);
                if (pondJson == null) return;
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                String breed = pond.getBreed();

                PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        requireActivity().runOnUiThread(() -> {
                            try {
                                JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
                                JsonArray feeds = root.getAsJsonArray("feeds");
                                boolean found = false;
                                for (int i = 0; i < feeds.size(); i++) {
                                    JsonObject feed = feeds.get(i).getAsJsonObject();
                                    String feedBreed = feed.get("breed").getAsString();
                                    String type = feed.get("feed_type").getAsString();

                                    if (feedBreed.equalsIgnoreCase(breed) && type.equalsIgnoreCase("frymash")) {
                                        double pricePerKg = Double.parseDouble(feed.get("price_per_kg").getAsString());
                                        double cost = (quantityInGrams / 1000) * pricePerKg; // convert grams to kg
                                        etFeedCost.setText(String.format(Locale.getDefault(), "%.2f", cost));
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    etFeedCost.setText("0.00");
                                    Toast.makeText(getContext(), "Feed price not found for this breed", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                etFeedCost.setText("0.00");
                                Toast.makeText(getContext(), "Error parsing feed price: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Cannot fetch feed price", e);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            etFeedCost.setText("0.00");
                            Toast.makeText(getContext(), "Cannot fetch feed price: " + error, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, error);
                        });
                    }
                });
            }
        });

        // ✅ Load logs and check last blind feeding day outside TextWatcher
        loadFeedLogs(tableFeedLogs);
        checkIfLastBlindFeedingDay();

        dialog.show();
    }

    private void checkIfLastBlindFeedingDay() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String stockingDateStr = pond.getDateStocking();
        if (stockingDateStr == null || stockingDateStr.isEmpty()) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date stockingDate = sdf.parse(stockingDateStr);
            Calendar today = Calendar.getInstance();

            long diffMillis = today.getTimeInMillis() - stockingDate.getTime();
            int daysSinceStocking = (int) (diffMillis / (1000 * 60 * 60 * 24));

            Calendar lastBlindFeedingDay = Calendar.getInstance();
            lastBlindFeedingDay.setTime(stockingDate);
            lastBlindFeedingDay.add(Calendar.DAY_OF_YEAR, 30);

            Log.d("BlindFeedingDebug", "🗓 Stocking Date: " + stockingDateStr +
                    " | Today: " + sdf.format(today.getTime()) +
                    " | Last Blind Feeding Day: " + sdf.format(lastBlindFeedingDay.getTime()) +
                    " | Days Since Stocking: " + daysSinceStocking);

            // ✅ Fetch logs to decide what to do
            PondSyncManager.fetchBlindFeedLogs(pond.getName(), new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object response) {
                    try {
                        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) return;

                        JsonArray logs = json.getAsJsonArray("data");
                        int logCount = logs.size();

                        requireActivity().runOnUiThread(() -> {
                            SharedPreferences lockPrefs = requireContext().getSharedPreferences(PREF_LOCK_STATE, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = lockPrefs.edit();

                            if (daysSinceStocking == 30) {
                                // 🗓 It's the final blind feeding day
                                if (logCount < 30) {
                                    // 🔒 Lock until logs complete
                                    isLockedInFragment = true;
                                    editor.putBoolean(KEY_IS_LOCKED, true);
                                    editor.apply();

                                    new android.app.AlertDialog.Builder(requireContext())
                                            .setTitle("⚠️ Final Day of Blind Feeding")
                                            .setMessage("You have only completed " + logCount + " out of 30 blind feeding logs.\n\nPlease finish all logs today before exiting.")
                                            .setCancelable(false)
                                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                            .show();

                                    Log.d("BlindFeedingDebug", "🔒 Final day and incomplete logs. Locked fragment.");
                                } else {
                                    // ✅ All logs complete
                                    isLockedInFragment = false;
                                    editor.putBoolean(KEY_IS_LOCKED, false);
                                    editor.apply();

                                    new android.app.AlertDialog.Builder(requireContext())
                                            .setTitle("✅ Blind Feeding Complete")
                                            .setMessage("You have completed all 30 blind feeding logs.\n\nNo further entries are required.")
                                            .setCancelable(false)

                                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                            .show();

                                    // 🚫 Disable adding more logs
                                    if (btnAddFeed != null) btnAddFeed.setEnabled(false);
                                    Log.d("BlindFeedingDebug", "✅ All 30 logs complete on final day. Disabled log button.");
                                }

                            } else if (daysSinceStocking > 30) {
                                // ⛔ After blind feeding phase
                                isLockedInFragment = false;
                                editor.putBoolean(KEY_IS_LOCKED, false);
                                editor.apply();

                                new android.app.AlertDialog.Builder(requireContext())
                                        .setTitle("⛔ Blind Feeding Period Over")
                                        .setMessage("The 30-day blind feeding period has ended.\nYou can no longer add blind feeding logs.")
                                        .setCancelable(false)
                                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                        .show();

                                if (btnAddFeed != null) btnAddFeed.setEnabled(false);
                                Log.d("BlindFeedingDebug", "⛔ Day > 30. Blind feeding disabled.");

                            } else {
                                // 👌 Before day 30 — allow normal logging
                                isLockedInFragment = false;
                                editor.putBoolean(KEY_IS_LOCKED, false);
                                editor.apply();

                                Log.d("BlindFeedingDebug", "✅ Day " + daysSinceStocking + ": logging allowed.");
                            }
                        });

                    } catch (Exception e) {
                        Log.e("BlindFeedingDebug", "Error parsing logs: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("BlindFeedingDebug", "Error fetching blind feed logs: " + error);
                }
            });

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void showDateSelectionDialog(EditText targetEditText) {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String stockingDateStr = pond.getDateStocking();
        if (stockingDateStr == null || stockingDateStr.isEmpty()) return;

        String[] displayDates = generateFeedingDates(stockingDateStr);
        String[] serverDates = generateFeedingDatesForServer(stockingDateStr);

        if (displayDates.length == 0) return;

        final int[] selectedIndex = {0}; // default selection

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Feeding Date")
                .setSingleChoiceItems(displayDates, 0, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (targetEditText != null) {
                        targetEditText.setText(displayDates[selectedIndex[0]]);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();

        hideLoadingDialog();
    }
    private String formatDateForDisplay(String dateStr) {
        try {
            Date date = mysqlFormat.parse(dateStr); // parse yyyy-MM-dd
            return displayFormat.format(date);       // format to MMM dd, yyyy
        } catch (ParseException e) {
            e.printStackTrace();
            return dateStr; // fallback to original if parsing fails
        }
    }
    private void fetchFeedPriceFromAllFeeds(String breed, String feedType, FeedPriceCallback callback) {
        PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                try {
                    JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
                    JsonArray feeds = root.getAsJsonArray("feeds"); // <-- use the "feeds" array
                    for (int i = 0; i < feeds.size(); i++) {
                        JsonObject feed = feeds.get(i).getAsJsonObject();
                        String feedBreed = feed.get("breed").getAsString();
                        String type = feed.get("feed_type").getAsString();

                        if (feedBreed.equalsIgnoreCase(breed) && type.equalsIgnoreCase(feedType)) {
                            double pricePerKg = Double.parseDouble(feed.get("price_per_kg").getAsString());
                            callback.onPriceFetched(pricePerKg);
                            return;
                        }
                    }
                    callback.onError("Feed price not found for this breed");
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    interface FeedPriceCallback {
        void onPriceFetched(double pricePerKg);
        void onError(String error);
    }
    private void addFeedLog(EditText etFeedQuantity, EditText etFeedDate, TableLayout tableFeedLogs) {
        final String feedType = "Frymash";
        final String quantityStr = etFeedQuantity.getText().toString().trim();
        final String dateStr = etFeedDate.getText().toString().trim();

        if (quantityStr.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        final String feedingDateMysql;
        try {
            Date selected = displayFormat.parse(dateStr);
            feedingDateMysql = mysqlFormat.format(selected);
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) {
            Toast.makeText(getContext(), "Pond not selected", Toast.LENGTH_SHORT).show();
            return;
        }
        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        final String pondName = pond.getName();

        computeFeedCost(feedType, quantity, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object costObj) {
                final double cost = (double) costObj;

                // Check for duplicates
                PondSyncManager.fetchBlindFeedLogs(pondName, new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        try {
                            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                            JsonArray logs = json.getAsJsonArray("data");
                            boolean dateExists = false;
                            for (int i = 0; i < logs.size(); i++) {
                                JsonObject log = logs.get(i).getAsJsonObject();
                                String existingDate = log.has("feeding_date") && !log.get("feeding_date").isJsonNull()
                                        ? log.get("feeding_date").getAsString() : "";
                                if (existingDate.equals(feedingDateMysql)) {
                                    dateExists = true;
                                    break;
                                }
                            }

                            if (dateExists) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    String displayDate;
                                    try {
                                        Date d = mysqlFormat.parse(feedingDateMysql);
                                        displayDate = displayFormat.format(d);
                                    } catch (ParseException e) {
                                        displayDate = feedingDateMysql;
                                    }
                                    Toast.makeText(
                                            getContext(),
                                            "A feeding log for " + displayDate + " already exists.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                            } else {
                                new Handler(Looper.getMainLooper()).post(() -> showConfirmAddDialog(quantity, cost, feedType, dateStr, feedingDateMysql, pondName, tableFeedLogs, etFeedQuantity, etFeedDate, etFeedCost));
                            }

                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> showConfirmAddDialog(
                                    quantity,
                                    cost,
                                    feedType,
                                    dateStr,
                                    feedingDateMysql,
                                    pondName,
                                    tableFeedLogs,
                                    etFeedQuantity,
                                    etFeedDate,
                                    etFeedCost
                            )
);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> showConfirmAddDialog(
                                quantity,
                                cost,
                                feedType,
                                dateStr,
                                feedingDateMysql,
                                pondName,
                                tableFeedLogs,
                                etFeedQuantity,
                                etFeedDate,
                                etFeedCost
                        )
);
                    }
                });
            }

            @Override
            public void onError(String error) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Error computing cost: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    private void showConfirmAddDialog(
            double quantity,
            double cost,
            String feedType,
            String dateStr,
            String feedingDateMysql,
            String pondName,
            TableLayout tableFeedLogs,
            EditText etFeedQuantity,
            EditText etFeedDate,
            TextView etFeedCost
    )
 {
        if (!isAdded() || getContext() == null) return;

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Feeding Log")
                .setMessage("Add feeding log?\n\nFeed Type: " + feedType +
                        "\nQuantity: " + quantity + " kg" +
                        "\nCost: ₱" + String.format(Locale.getDefault(), "%.2f", cost) +
                        "\nDate: " + dateStr)
                .setPositiveButton("Yes", (dialog, which) -> {
                    showLoadingDialog("Uploading feeding log...");
                    String recordAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    PondSyncManager.uploadBlindFeedLog(
                            pondName, feedType, quantity, cost, feedingDateMysql, recordAt,
                            new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object response) {
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        loadFeedLogs(tableFeedLogs);


                                        // Reset fields ONLY IF dialog still exists and view is attached
                                        if (etFeedQuantity != null) etFeedQuantity.setText("");
                                        if (etFeedDate != null) etFeedDate.setText("");
                                        if (etFeedCost != null) etFeedCost.setText("");

                                        Toast.makeText(getContext(), "Feed log added!", Toast.LENGTH_SHORT).show();
                                        fetchTotalFeedCost(pondId);
                                        hideLoadingDialog();
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    hideLoadingDialog();
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                    );
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String[] generateFeedingDates(String stockingDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date stockingDate = sdf.parse(stockingDateStr);

            Calendar cal = Calendar.getInstance();
            cal.setTime(stockingDate);

            int totalDays = 30; // adjust number of selectable dates

            // We'll keep two arrays: one for server format, one for display
            String[] displayDates = new String[totalDays];

            for (int i = 0; i < totalDays; i++) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                displayDates[i] = displayFormat.format(cal.getTime()); // MMM dd, yyyy
            }

            return displayDates;
        } catch (ParseException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
    private String[] generateFeedingDatesForServer(String stockingDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date stockingDate = sdf.parse(stockingDateStr);

            Calendar cal = Calendar.getInstance();
            cal.setTime(stockingDate);

            int totalDays = 30;
            String[] serverDates = new String[totalDays];

            for (int i = 0; i < totalDays; i++) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                serverDates[i] = mysqlFormat.format(cal.getTime()); // yyyy-MM-dd
            }

            return serverDates;
        } catch (ParseException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
    private void updateFeedLog(String pondName, String feedType, double quantity, double cost, String feedingDate, String feedLogId, TableLayout parentTable) {
        showLoadingDialog("Updating feeding log...");

        PondSyncManager.updateBlindFeedingLog(
                pondName, feedType, quantity, cost, feedingDate, feedLogId,
                new PondSyncManager.OnDataSyncListener() {
                    @Override
                    public void onSuccess(String response) {

                        new Handler(Looper.getMainLooper()).post(() -> {

                            if (!isAdded() || getContext() == null) return;

                            try {
                                JSONObject json = new JSONObject(response);

                                if (json.getString("status").equals("success")) {

                                    Toast.makeText(getContext(), "Feed log updated!", Toast.LENGTH_SHORT).show();
                                    hideLoadingDialog();
                                    // Safely reload table
                                    if (parentTable != null) {
                                        parentTable.removeViews(1, Math.max(parentTable.getChildCount() - 1, 0));
                                        fetchTotalFeedCost(pondId);
                                        loadFeedLogs(parentTable);
                                    } else {
                                        Log.e("UPDATE_FEED_LOG", "parentTable is NULL!");
                                    }


                                } else {
                                    hideLoadingDialog();
                                    Toast.makeText(getContext(), json.getString("message"), Toast.LENGTH_LONG).show();
                                }

                            } catch (Exception e) {
                                hideLoadingDialog();
                                Toast.makeText(getContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.d("errt", e.getMessage());
                            }
                        });
                    }


                    @Override
                    public void onError(String error) {
                        hideLoadingDialog();
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(getContext(), "Error updating: " + error, Toast.LENGTH_LONG).show()
                        );
                    }
                }
        );
    }
    private void loadFeedLogs(TableLayout tableFeedLogs) {
        Log.d("FEED_LOG", "Reloading table...");

        if (tableFeedLogs == null) {
            Log.e(TAG, "TableLayout is null. Cannot load feed logs.");
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        showLoadingDialog("Loading feed records...");

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String pondName = pond.getName();

        PondSyncManager.fetchBlindFeedLogs(pondName, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                hideLoadingDialog();
                requireActivity().runOnUiThread(() -> {
                    try {
                        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) {
                            Toast.makeText(getContext(), "No logs found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JsonArray logs = json.getAsJsonArray("data");
                        int childCount = tableFeedLogs.getChildCount();
                        if (childCount > 1) tableFeedLogs.removeViews(1, childCount - 1);

                        for (int i = 0; i < logs.size(); i++) {
                            JsonObject log = logs.get(i).getAsJsonObject();
                            String logId = log.get("id").getAsString();
                            String date = log.get("feeding_date").getAsString();
                            String feedType = log.get("feed_type").getAsString();
                            String quantity = log.get("quantity").getAsString();
                            String cost = log.get("cost").getAsString();

                            TableRow row = new TableRow(getContext());
                            row.setPadding(4, 4, 4, 4);

                            // Actions layout
                            LinearLayout actionLayout = new LinearLayout(getContext());
                            actionLayout.setOrientation(LinearLayout.HORIZONTAL);
                            actionLayout.setGravity(Gravity.CENTER_VERTICAL);
                            actionLayout.setPadding(8, 4, 8, 4);

                            ImageView editIcon = new ImageView(getContext());
                            editIcon.setImageResource(android.R.drawable.ic_menu_edit);
                            editIcon.setPadding(8, 0, 8, 0);
                            editIcon.setOnClickListener(v -> showEditDialog(logId, date, feedType, quantity, cost, mainTableFeedLogs));

                            ImageView deleteIcon = new ImageView(getContext());
                            deleteIcon.setImageResource(android.R.drawable.ic_menu_delete);
                            deleteIcon.setPadding(8, 0, 8, 0);
                            deleteIcon.setOnClickListener(v -> confirmDeleteLog(logId, tableFeedLogs));

                            actionLayout.addView(editIcon);
                            actionLayout.addView(deleteIcon);

                            row.addView(actionLayout);
                            addCell(row, formatDateForDisplay(date));
                            addCell(row, feedType);
                            addCell(row, quantity);
                            addCell(row, "₱" + cost);

                            tableFeedLogs.addView(row);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing feed logs: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(getContext(), "Error loading logs: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showLoadingDialog(String message) {
        requireActivity().runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                TextView loadingText = loadingDialog.findViewById(R.id.loadingText);
                if (loadingText != null) loadingText.setText(message);
                return; // already showing
            }

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);

            ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
            TextView loadingText = dialogView.findViewById(R.id.loadingText);
            loadingText.setText(message);

            Animation rotate = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate);
            if (fishLoader != null) fishLoader.startAnimation(rotate);

            builder.setView(dialogView);
            builder.setCancelable(false);
            loadingDialog = builder.create();
            loadingDialog.show();
        });
    }


    private void hideLoadingDialog() {
        requireActivity().runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
                loadingDialog = null;
            }
        });
    }
    private void computeFeedCost(String feedType, double quantityInGrams, PondSyncManager.Callback callback) {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) {
            callback.onError("Pond not selected");
            return;
        }
        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String pondBreed = pond.getBreed();

        PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                try {
                    JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject(); // root object
                    JsonArray feeds = root.getAsJsonArray("feeds"); // ✅ correct

                    double pricePerKg = -1;
                    for (int i = 0; i < feeds.size(); i++) {
                        JsonObject feed = feeds.get(i).getAsJsonObject();
                        String breed = feed.get("breed").getAsString();
                        String type = feed.get("feed_type").getAsString();
                        if (breed.equalsIgnoreCase(pondBreed) && type.equalsIgnoreCase(feedType)) {
                            pricePerKg = feed.get("price_per_kg").getAsDouble();
                            break;
                        }
                    }

                    if (pricePerKg < 0) {
                        callback.onError("Price not found for " + feedType + " (" + pondBreed + ")");
                        return;
                    }

                    double cost = (quantityInGrams / 1000.0) * pricePerKg; // grams -> kg
                    callback.onSuccess(cost);

                } catch (Exception e) {
                    callback.onError("Error parsing feeds: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    private void showEditDialog(String logId, String date, String feedType, String quantity, String cost, TableLayout parentTable) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_feed, null);
        EditText etQuantity = dialogView.findViewById(R.id.etEditQuantity);
        EditText etCost = dialogView.findViewById(R.id.etEditCost);

        etQuantity.setText(quantity);
        etCost.setText(cost);

        etQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double qtyInGrams = Double.parseDouble(s.toString());
                    SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                    String pondJson = prefs.getString("selected_pond", null);
                    if (pondJson == null) {
                        etCost.setText("0.00");
                        return;
                    }
                    PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                    String breed = pond.getBreed();

                    PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
                        @Override
                        public void onSuccess(Object response) {
                            try {
                                JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
                                JsonArray feeds = root.getAsJsonArray("feeds");
                                boolean found = false;
                                for (int i = 0; i < feeds.size(); i++) {
                                    JsonObject feed = feeds.get(i).getAsJsonObject();
                                    String feedBreed = feed.get("breed").getAsString();
                                    String type = feed.get("feed_type").getAsString();

                                    if (feedBreed.equalsIgnoreCase(breed) && type.equalsIgnoreCase(feedType)) {
                                        double pricePerKg = Double.parseDouble(feed.get("price_per_kg").getAsString());
                                        double cost = qtyInGrams * pricePerKg / 1000.0; // if input in grams
                                        etCost.setText(String.format(Locale.getDefault(), "%.2f", cost));
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) etCost.setText("0.00");
                            } catch (Exception e) {
                                etCost.setText("0.00");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            etCost.setText("0.00");
                        }
                    });

                } catch (NumberFormatException e) {
                    etCost.setText("0.00");
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });


        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                double newQuantity = Double.parseDouble(etQuantity.getText().toString());
                double newCost = Double.parseDouble(etCost.getText().toString());

                SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                String pondJson = prefs.getString("selected_pond", null);
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                String pondName = pond.getName();

                updateFeedLog(pondName, feedType, newQuantity, newCost, date, logId, parentTable);

            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid input: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss() );
        hideLoadingDialog();
        builder.show();
    }
    private void confirmDeleteLog(String logId, TableLayout tableFeedLogs) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this feeding log?\nThis action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteFeedLog(logId, tableFeedLogs))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void deleteFeedLog(String logId, TableLayout tableFeedLogs) {
        // 🔥 Always safely read pondName from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String pondName = pond.getName();

        if (pondName == null || logId == null) {
            Toast.makeText(getContext(), "Delete error: Missing pond or log ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.e("DELETE_FEED_LOG", "Deleting logId=" + logId + ", pondName=" + pondName);

        showLoadingDialog("Deleting feeding log...");
        PondSyncManager.deleteBlindFeedingLog(logId, pondName, new PondSyncManager.OnDataSyncListener() {
            @Override
            public void onSuccess(String response) {
                hideLoadingDialog();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Feed log deleted!", Toast.LENGTH_SHORT).show();
                    fetchTotalFeedCost(pondId);
                    loadFeedLogs(tableFeedLogs);
                    hideLoadingDialog();
                });
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Error deleting: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    private void addCell(TableRow row, String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(16, 8, 16, 8);
        tv.setTextSize(14);
        row.addView(tv);
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
        tvTotalStocks.setText(formatNumber(pond.getFishCount()));

        // Unit cost with ₱ + commas
        tvUnitCost.setText(formatPeso(pond.getCostPerFish()));

        // Total cost
        double totalCost = pond.getFishCount() * pond.getCostPerFish();
        tvTotalCost.setText(formatPeso(totalCost));
        tvSummaryFingerlings.setText(formatPeso(totalCost));
    }

    private void showSamplingDialog() {
        samplingDialog = new Dialog(requireContext());
        Dialog dialog = samplingDialog;  // optional alias

        dialog.setContentView(R.layout.dialog_add_sampling);
        dialog.setCancelable(true);

        TextView btnClose = dialog.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        // Initialize TextViews
        tvPondName = dialog.findViewById(R.id.tvPondName);
        tvNextSampling = dialog.findViewById(R.id.tvNextSampling);
        tvDFRPerCycle = dialog.findViewById(R.id.tvDFRPerCycle);
        tvDaysOfCulture = dialog.findViewById(R.id.tvDaysOfCulture);
        tvLifeStage = dialog.findViewById(R.id.tvLifeStage);
        tvTotalStocks = dialog.findViewById(R.id.tvTotalStocks);
        tvMortality = dialog.findViewById(R.id.tvMortality);
        tvTimeFeeding1 = dialog.findViewById(R.id.timeoffeeding1);
        tvTimeFeeding2 = dialog.findViewById(R.id.timeoffeeding2);
        tvABWResult = dialog.findViewById(R.id.tvABWResult);
        tvDFRResult = dialog.findViewById(R.id.tvDFRResult);
        tvFeedType = dialog.findViewById(R.id.tvFeedType);
        tvFeedCost = dialog.findViewById(R.id.tvFeedCost);

        // Initialize EditTexts and Survival Rate (display-only TextView)
        etSampledWeight = dialog.findViewById(R.id.etSampledWeight);
        etNumSamples = dialog.findViewById(R.id.etNumSamples);
        etFeedingRate = dialog.findViewById(R.id.etFeedingRate);
        tvSurvivalRate = dialog.findViewById(R.id.etSurvivalRate); // you said this is a TextView (id kept)

        btnSelectTime1 = dialog.findViewById(R.id.btnselecttime1);
        btnSelectTime2 = dialog.findViewById(R.id.btnselecttime2);

        // Load pond data and setup UI
        loadPondData();
        setDefaultFeedingTimes();
        addTextWatchers();

        tvRemainingFeed = dialog.findViewById(R.id.tvRemainingFeed);
        btnSave = dialog.findViewById(R.id.btnSaveSampling);
        btnSave.setOnClickListener(v -> validateAndSave());
        refreshFeedLevel();

        btnSelectTime1.setOnClickListener(v -> showTimePickerDialog(tvTimeFeeding1, 1));
        btnSelectTime2.setOnClickListener(v -> showTimePickerDialog(tvTimeFeeding2, 2));

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                feedUpdateReceiver,
                new IntentFilter("FEED_LEVEL_UPDATED")
        );
        Log.d("FEED_UI", "SamplingDialog receiver registered");

        Log.d("FEED_TEST", "SamplingDialog onViewCreated: Calling refreshRemainingFeedUI()");
        refreshRemainingFeedUI();

        dialog.show();
        // Apply sizing and background (equivalent to onStart() in DialogFragment)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

    }

    private void loadPondData() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", android.content.Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        pondId = pond.getId();
        Log.d("FEED_TEST", "Loaded pondId for SamplingDialog = " + pondId);
        if (pond == null) return;

        tvPondName.setText(pond.getName());

        // next sampling date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 15);

        tvNextSampling.setText("Next Sampling: " + sdf.format(calendar.getTime()));

        // compute daysOfCulture (class-level)
        if (pond.getDateStocking() != null && !pond.getDateStocking().isEmpty()) {
            try {
                Date stockingDate = sdf.parse(pond.getDateStocking());
                daysOfCulture = (new Date().getTime() - stockingDate.getTime()) / (1000L * 60 * 60 * 24);
            } catch (Exception e) {
                daysOfCulture = 1;
            }
        } else {
            daysOfCulture = 1;
        }
        if (daysOfCulture < 1) daysOfCulture = 1;
        tvDaysOfCulture.setText(daysOfCulture + " days");

        // normalize breed
        species = pond.getBreed() != null ? pond.getBreed().toLowerCase(Locale.ROOT) : "";
        String breedClean = (species.contains("bangus") || species.contains("milkfish")) ? "bangus" : "tilapia";

        // life stage
        String lifeStage;
        if (breedClean.equals("tilapia")) {
            if (daysOfCulture <= 60) lifeStage = "Fingerling";
            else if (daysOfCulture <= 90) lifeStage = "Juvenile";
            else if (daysOfCulture <= 180) lifeStage = "Sub-adult / Adult (Harvest)";
            else lifeStage = "Post-harvest";
        } else {
            if (daysOfCulture <= 70) lifeStage = "Fingerling";
            else if (daysOfCulture <= 90) lifeStage = "Juvenile";
            else if (daysOfCulture <= 180) lifeStage = "Sub-adult / Adult (Harvest)";
            else lifeStage = "Post-harvest";
        }
        tvLifeStage.setText(lifeStage);

        // fill other UI
        tvTotalStocks.setText(String.valueOf(pond.getFishCount()));
        tvMortality.setText(pond.getMortalityRate() + "%");
        tvSurvivalRate.setText(String.format(Locale.getDefault(), "%.2f%%", (100 - pond.getMortalityRate())));

        // fetch feed type and price for this pond/day
        Log.d(TAG, "Loading feed price for " + breedClean + " days=" + daysOfCulture);
        fetchFeedPrice(breedClean, daysOfCulture);

        tvDFRResult.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateFeedCostSampling(); }
        });
    }

    private void fetchFeedPrice(String breedClean, long days) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_feed_price_by_day.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String postData = "breed=" + URLEncoder.encode(breedClean, "UTF-8")
                        + "&days=" + URLEncoder.encode(String.valueOf(days), "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes("UTF-8"));
                    os.flush();
                }

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + code);
                }

                StringBuilder sb = new StringBuilder();
                try (InputStream is = conn.getInputStream();
                     BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }

                Log.d(TAG, "fetchFeedPrice response: " + sb.toString());

                JSONObject obj = new JSONObject(sb.toString());
                final String feedType = obj.optString("feed_type", "No Feed");
                final double pricePerKg = obj.optDouble("price_per_kg", 0);

                requireActivity().runOnUiThread(() -> {
                    currentPricePerKg = pricePerKg;
                    tvFeedType.setText(feedType);
                    tvFeedType.setTag(pricePerKg);
                    Log.d(TAG, "FeedType=" + feedType + " price=" + pricePerKg);
                    updateFeedCostSampling(); // recalc now that price is present
                });

            } catch (Exception e) {
                Log.e(TAG, "fetchFeedPrice error", e);
                requireActivity().runOnUiThread(() -> {
                    currentPricePerKg = null;
                    tvFeedType.setText("N/A");
                    tvFeedCost.setText("₱00.00");
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void updateFeedCostSampling() {
        if (currentPricePerKg == null) {
            tvFeedCost.setText("₱00.00");
            return;
        }

        // ✅ Get TOTAL DFR per day (not per feeding)
        String dfrText = tvDFRResult.getText().toString().replace(" g", "").trim();
        double dfrGrams = parseDoubleSampling(dfrText);

        // ✅ Convert grams → kilograms
        double dfrKgPerDay = dfrGrams / 1000.0;

        // ✅ Compute daily feed cost
        double dailyCost = dfrKgPerDay * currentPricePerKg;

        // ✅ Display formatted
        tvFeedCost.setText("₱" + String.format(Locale.getDefault(), "%.2f", dailyCost));

        Log.d(TAG, "updateFeedCost — DFR(g)=" + dfrGrams +
                ", DFR(kg/day)=" + dfrKgPerDay +
                ", price/kg=" + currentPricePerKg +
                ", dailyCost=" + dailyCost);
    }

    private void validateAndSave() {
        SharedPreferences prefs = requireContext().getSharedPreferences("FEED_LEVEL_PREF", Context.MODE_PRIVATE);
        float remainingFeed = prefs.getFloat("feed_remaining_" + pondId, 0f);

        if (remainingFeed <= 0) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Feed Container is Empty")
                    .setMessage("You must store feeds in the container before you can save a sampling.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        if (remainingFeed < 2000) {
            Toast.makeText(getContext(), "⚠️ Feed is below 2,000g — refill soon.", Toast.LENGTH_LONG).show();
        }



        String sampledWeight = etSampledWeight.getText().toString().trim();
        String numSamples = etNumSamples.getText().toString().trim();
        String feedingRates = etFeedingRate.getText().toString().trim();
        String survivalRates = tvSurvivalRate.getText().toString().trim(); // display-only

        String feedingTime1 = tvTimeFeeding1.getText().toString().trim();
        String feedingTime2 = tvTimeFeeding2.getText().toString().trim();

        if (sampledWeight.isEmpty() || numSamples.isEmpty() ||
                feedingRates.isEmpty() || survivalRates.isEmpty() ||
                feedingTime1.isEmpty() || feedingTime2.isEmpty()) {

            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Incomplete Fields")
                    .setMessage("Please complete all required fields before saving.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        try {
            double sw = Double.parseDouble(sampledWeight);
            double ns = Double.parseDouble(numSamples);
            double fr = Double.parseDouble(feedingRates);

            if (sw <= 0 || ns <= 0 || fr <= 0) {
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Invalid Input")
                        .setMessage("Values must be greater than zero.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
                return;
            }
        } catch (NumberFormatException e) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Invalid Format")
                    .setMessage("Please enter valid numeric values.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        SharedPreferences pondPrefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = pondPrefs.getString("selected_pond", null);
        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);

        pondId = pond.getId();

        new Thread(() -> {
            try {
                String feederId = "feeder_001";
                URL url = new URL("https://pondmate.alwaysdata.net/get_feeder_assignment.php?feeder_id=" + feederId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();

                JSONObject json = new JSONObject(result.toString());
                String assignedPondId = json.optString("pond_id", "none");

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (assignedPondId.equals("none")) {
                        // ❌ No feeder assigned
                        new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Feeder Not Assigned")
                                .setMessage("The feeder device is not connected to any pond.\n\nWould you like to assign it to this pond now?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    assignFeederToPondAndUpload(pond.getId(), pond.getName(),
                                            formattedTime1, formattedTime2,
                                            parseDoubleSampling(tvDFRPerCycle.getText().toString().replace(" g", ""))
                                    );
                                })
                                .setNegativeButton("No", (dialog, which) ->
                                        Toast.makeText(requireContext(), "Sampling not saved. Please connect feeder first.", Toast.LENGTH_SHORT).show()
                                )
                                .show();
                    } else if (!assignedPondId.equals(pond.getId())) {
                        // ❌ Assigned to a different pond
                        new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Feeder Connected to Another Pond")
                                .setMessage("The feeder is currently connected to another pond.\n\nWould you like to reassign it to this pond?")
                                .setPositiveButton("Reassign", (dialog, which) -> {
                                    assignFeederToPondAndUpload(
                                            pond.getId(),
                                            pond.getName(),
                                            formattedTime1,
                                            formattedTime2,
                                            parseDoubleSampling(tvDFRPerCycle.getText().toString().replace(" g", ""))
                                    );
                                })
                                .setNegativeButton("No", (dialog, which) -> {
                                    dialog.dismiss(); // ❌ Do nothing, no save
                                })
                                .show();


                    } else {

                        saveSamplingRecord(pond);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(requireContext(), "Error checking feeder assignment.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
    private void saveSamplingRecord(PondModel pond) {
        int daysOfCultureVal = Integer.parseInt(tvDaysOfCulture.getText().toString().replace(" days", ""));
        String growthStage = tvLifeStage.getText().toString();
        int totalStocks = pond.getFishCount();
        double mortalityRate = pond.getMortalityRate();

        double abw = parseDoubleSampling(tvABWResult.getText().toString().replace(" g", ""));
        double feedingRate = parseDoubleSampling(etFeedingRate.getText().toString());
        double survivalRate = parseDoubleSampling(tvSurvivalRate.getText().toString().replace("%", ""));
        double dfr = parseDoubleSampling(tvDFRResult.getText().toString().replace(" g", ""));
        double dfrFeed = parseDoubleSampling(tvDFRResult.getText().toString().replace(" g", ""));
        double dailyFeedCost = parseDoubleSampling(tvFeedCost.getText().toString().replace("₱", "").trim());


        String feedingOne = formattedTime1; // use 24-hour version
        String feedingTwo = formattedTime2;

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        String nextSamplingDate = tvNextSampling.getText().toString()
                .replace("Next Sampling: ", "")
                .trim();

        PondSyncManager.uploadSamplingRecord(
                pond.getId(), daysOfCultureVal, growthStage, totalStocks, mortalityRate,
                feedingOne, feedingTwo, abw, feedingRate, survivalRate, dfr, dfrFeed, dailyFeedCost,
                now, now, nextSamplingDate,
                new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Sampling record saved!", Toast.LENGTH_SHORT).show();

                            float feedPerCycle = (float) parseDoubleSampling(tvDFRPerCycle.getText().toString().replace(" g", ""));
                            float totalToDeduct = feedPerCycle * 2f; // 2 feedings for the current day
                            Log.d("SAMPLING_FEED", "Feed deduction computed: perCycle=" + feedPerCycle + ", totalDeduct=" + totalToDeduct);

                            FeedStorage.deductFeed(requireContext(), pondId, totalToDeduct);

                            float updated  = FeedStorage.getRemainingFeed(requireContext(), pondId);
                            Log.d("SAMPLING_FEED", "After deduction, remaining feed = " + updated );
                            tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g", updated ));

                            refreshFeedLevel();

                            // Notify ControlsFeeder (so it updates UI too)
                            LocalBroadcastManager.getInstance(requireContext())
                                    .sendBroadcast(new Intent("FEED_LEVEL_UPDATED"));



                            if (samplingDialog != null && samplingDialog.isShowing()) {
                                samplingDialog.dismiss();
                            }

                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show()
                        );
                    }
                }
        );
    }

    private void assignFeederToPondAndUpload(String pondId, String pondName, String feedingOne,
                                             String feedingTwo, double dfrFeedGrams) {
        String feederId = "feeder_001"; // fixed ID
        OkHttpClient client = new OkHttpClient();

        RequestBody body = new FormBody.Builder()
                .add("feeder_id", feederId)
                .add("pond_id", pondId)
                .build();

        Request request = new Request.Builder()
                .url("https://pondmate.alwaysdata.net/update_feeder_assignment.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to assign feeder", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Feeder_001 assigned successfully!", Toast.LENGTH_SHORT).show();
                        uploadDFRToAdafruit(pondName, feedingOne, feedingTwo, dfrFeedGrams);
                        if (samplingDialog != null && samplingDialog.isShowing()) {
                            samplingDialog.dismiss();
                        }

                    });
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Server error assigning feeder", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void refreshRemainingFeedUI() {
        float remaining = FeedStorage.getRemainingFeed(requireContext(), pondId);
        Log.d("FEED_TEST", "refreshRemainingFeedUI() -> remaining feed = " + remaining);
        tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g", remaining));
    }

    private void refreshFeedLevel() {
        SharedPreferences prefs = requireContext().getSharedPreferences("FEED_LEVEL_PREF", Context.MODE_PRIVATE);
        float remainingFeed = FeedStorage.getRemainingFeed(requireContext(), pondId);
        tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g", remainingFeed));

        if (remainingFeed <= 0) {
            btnSave.setEnabled(false);
            btnSave.setAlpha(0.5f);
        } else {
            btnSave.setEnabled(true);
            btnSave.setAlpha(1f);
        }
    }

    private final BroadcastReceiver feedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshFeedLevel();
        }
    };

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(feedUpdateReceiver);
        super.onDestroyView();
    }


    private void setDefaultFeedingTimes() {
        formattedTime1 = "08:00:00";
        formattedTime2 = "16:00:00";
        tvTimeFeeding1.setText("8:00 AM");
        tvTimeFeeding2.setText("4:00 PM");
    }

    private void showTimePickerDialog(TextView targetTextView, int timeNumber) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timepicker, null);
        TimePicker timePicker = dialogView.findViewById(R.id.timePickerSpinner);
        timePicker.setIs24HourView(false);
        timePicker.setHour(timeNumber == 1 ? 8 : 16);
        timePicker.setMinute(0);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Feeding Time " + timeNumber)
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    String amPm = (hour >= 12) ? "PM" : "AM";
                    int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
                    String formatted12 = String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm);
                    targetTextView.setText(formatted12);

                    // Save 24-hour format for database
                    String formatted24 = String.format(Locale.getDefault(), "%02d:%02d:00", hour, minute);
                    if (timeNumber == 1) {
                        formattedTime1 = formatted24;
                    } else {
                        formattedTime2 = formatted24;
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void addTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { computeValues(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        etSampledWeight.addTextChangedListener(watcher);
        etNumSamples.addTextChangedListener(watcher);
        etFeedingRate.addTextChangedListener(watcher);
        // DO NOT add tvSurvivalRate here — it's not editable by user
    }


    private void computeValues() {
        try {
            String sampledWeightStr = etSampledWeight.getText().toString().trim();
            String numSamplesStr = etNumSamples.getText().toString().trim();
            String feedingRateStr = etFeedingRate.getText().toString().trim();
            String survivalText = tvSurvivalRate.getText().toString().replace("%", "").trim();

            // 1️⃣ Compute ABW first, even if feedingRate/survival are empty
            double abw = 0;
            if (!sampledWeightStr.isEmpty() && !numSamplesStr.isEmpty()) {
                double sampledWeight = parseDoubleSampling(sampledWeightStr);
                double numSamples = parseDoubleSampling(numSamplesStr);
                abw = (numSamples > 0) ? (sampledWeight / numSamples) : 0;
                if (Double.isNaN(abw) || Double.isInfinite(abw)) abw = 0;
            }
            tvABWResult.setText(String.format(Locale.getDefault(), "%.2f g", abw));
            Log.d(TAG, "ABW (g): " + abw);

            // 2️⃣ Compute DFR only if feedingRate and survival are provided
            double dfrGrams = 0;
            if (!feedingRateStr.isEmpty() && !survivalText.isEmpty()) {
                double feedingRate = parseDoubleSampling(feedingRateStr);
                double survivalRate = parseDoubleSampling(survivalText);

                double totalStocks = parseDoubleSampling(tvTotalStocks.getText().toString());
                double feedingRateDecimal = feedingRate / 100.0;
                double survivalDecimal = survivalRate / 100.0;

                dfrGrams = feedingRateDecimal * survivalDecimal * abw * totalStocks;
                if (Double.isNaN(dfrGrams) || Double.isInfinite(dfrGrams)) dfrGrams = 0;
            }

            tvDFRResult.setText(String.format(Locale.getDefault(), "%.2f g", dfrGrams));
            tvDFRPerCycle.setText(String.format(Locale.getDefault(), "%.2f g", (dfrGrams / 2.0)));
            Log.d(TAG, "DFR (g): " + dfrGrams);

            // 3️⃣ Update feed cost (optional)
            updateFeedCostSampling();

        } catch (Exception e) {
            Log.e(TAG, "computeValues error", e);
            tvABWResult.setText("0.00 g");
            tvDFRResult.setText("0.00 g");
            tvDFRPerCycle.setText("0.00 g");
            updateFeedCostSampling();
        }
    }


    private double parseDoubleSampling(String s) {
        if (s == null) return 0.0;
        s = s.trim();
        if (s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0.0; }
    }



    private void uploadDFRToAdafruit(String pondName, String feedingOne24, String feedingTwo24, double dfrFeedGrams) {
        OkHttpClient client = new OkHttpClient();

        String feedKey = "schedule"; // your Adafruit feed key
        String username = getString(R.string.adafruit_username);
        String aioKey = getString(R.string.adafruit_aio_key);

        // Divide by 10
        int reducedGrams = (int) Math.round(dfrFeedGrams / 10);

        // Value format: PondName|feedingOne,feedingTwo|reducedGrams
        String value = pondName + "|" + feedingOne24 + "," + feedingTwo24 + "|" + reducedGrams + "g";

        try {
            JSONObject json = new JSONObject();
            json.put("value", value);

            RequestBody body = RequestBody.create(json.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            String url = "https://io.adafruit.com/api/v2/" + username + "/feeds/" + feedKey + "/data";

            Log.d(TAG, "Posting to Adafruit: " + value);
            Log.d(TAG, "URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-AIO-Key", aioKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Adafruit request failed", e);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Failed to post to Adafruit: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String bodyStr = (response.body() != null) ? response.body().string() : "";
                    Log.d(TAG, "Adafruit response code: " + response.code());
                    Log.d(TAG, "Adafruit response body: " + bodyStr);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "DFR posted to Adafruit!", Toast.LENGTH_SHORT).show());

                    }
                }
            });

        } catch (JSONException ex) {
            Log.e(TAG, "JSON error", ex);
            Toast.makeText(getContext(), "JSON error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver feedUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float updated = FeedStorage.getRemainingFeed(context, pondId);
            Log.d("FEED_UI", "SamplingDialog updated feed display = " + updated);
            tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g.", updated));
        }
    };
    public void updateSamplingButtonState(String pondId) {
        PondSyncManager.fetchSamplingDates(pondId, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response.toString());
                        String nextSamplingDate = json.optString("next_sampling_date", "");
                        JSONArray debugArray = json.optJSONArray("debug");

                        // 1. Log and show toast for all debug info (contains sampling dates)
                        if (debugArray != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < debugArray.length(); i++) {
                                sb.append(debugArray.getString(i)).append("\n");
                                Log.d("SAMPLING_DEBUG", debugArray.getString(i));
                            }
                            Toast.makeText(getContext(), sb.toString(), Toast.LENGTH_LONG).show();
                        }

                        // 2. Compare today's date with nextSamplingDate
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date today = sdf.parse(sdf.format(new Date()));

                        if (!nextSamplingDate.isEmpty()) {
                            Date next = sdf.parse(nextSamplingDate);

                            if (today.equals(next)) {
                                // Today is a scheduled sampling → enable button
                                setSamplingButtonEnabled(true);
                            } else {
                                // Not today → disable button with toast
                                setSamplingButtonEnabled(false);
                                btnSampling.setOnClickListener(v ->
                                        Toast.makeText(getContext(),
                                                "Next sampling is on " + nextSamplingDate,
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        } else {
                            // No pending sampling
                            setSamplingButtonEnabled(false);
                            btnSampling.setOnClickListener(v ->
                                    Toast.makeText(getContext(),
                                            "No more sampling scheduled.",
                                            Toast.LENGTH_SHORT).show()
                            );
                        }

                    } catch (Exception e) {
                        Log.e("SAMPLING_DEBUG", "Error parsing response: " + e.getMessage());
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    Log.e("SAMPLING_DEBUG", "Server error: " + error);
                    Toast.makeText(getContext(), "Server error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
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
            btnSampling.setOnClickListener(v -> showSamplingDialog());
        } else {
            btnSampling.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray)
            );
            // The toast for not today is set in updateSamplingButtonState
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

        SharedPreferences pondPrefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = pondPrefs.getString("selected_pond", null);
        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
            computeEstimatedROI();
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

    private String formatPrice(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(amount);
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
                        String totalHarvestKgStr = formatNumber(round(totalHarvestKg));
                        String grossSalesStr = formatNumber(round(grossSales));
                        String feedCostStr = formatNumber(round(feedCost));
                        String fertilizerCostStr = formatNumber(round(fertilizerCost));
                        String totalExpensesStr = formatNumber(round(totalExpenses));
                        String estimatedROIStr = formatNumber(round(estimatedROI));

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

                        updateROIText(formatNumber(estimatedROI) + "%");

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

    private String formatNumber(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(value);
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

        addRow(table, "Fingerlings Stocked:", formatNumber(b.fingerlingsCount) + " pcs.");
        addRow(table, "Mortality Rate:", formatNumber(b.mortalityRate) + "%");
        addRow(table, "Survival Rate:", formatNumber(b.survivalRate) + "%");

        addSpacer(table);
        addHeader(table, "PROJECTED SALES");
        addRow(table, "Harvest Weight:", formatNumber(b.totalHarvestKg) + " kg.");
        addRow(table, "Farm-Gate Price:", "₱" + formatNumber(b.farmGatePrice) + "/kg.");
        addRow(table, "Gross Revenue:", "₱" + formatNumber(b.grossSales));

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
        addRow(table, "ESTIMATED ROI:", formatNumber(b.estimatedROI) + "%");

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
        return "₱" + new DecimalFormat("#,###.00").format(value);
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
