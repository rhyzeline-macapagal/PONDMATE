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
    TextView tvSummaryFingerlings, tvSummaryFeeds, tvSummaryMaintenance, tvSummaryTotal;
    TextView tvEstimatedRoI;
    TextView tvSummarySalary;
    //private TextView tvEstimatedRevenue;

    private double salaryPerPond = 0.0;

    private double totalCost = 0.0;
    private String pondName = "";
    private String currentBreed = "";
    private int currentFishCount = 0;
    Button  btnFeedLogs;

    File generatedPdfFile;

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

        tvBreed = view.findViewById(R.id.fishbreedpcostdisplay);
        tvCount = view.findViewById(R.id.numoffingerlings);
        tvAmountPerPiece = view.findViewById(R.id.amtperpiece);
        tvTotalCost = view.findViewById(R.id.amtoffingerlings);

        tvSummaryFingerlings = view.findViewById(R.id.tvSummaryFingerlings);
        tvSummaryFeeds = view.findViewById(R.id.tvSummaryFeeds);
        tvSummaryMaintenance = view.findViewById(R.id.tvSummaryMaintenance);
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal);

        tvEstimatedRoI = view.findViewById(R.id.tvEstimatedROI);
        //tvEstimatedRevenue = view.findViewById(R.id.tvEstimatedRevenue);

        computeEstimatedROI();

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

        Spinner spinnerMonths = view.findViewById(R.id.spinnerMonths);

        int savedMonths = pondPrefs.getInt("selected_cycle_months", 6);
        selectedCycleMonths = savedMonths;

        spinnerMonths.post(() -> {
            int index = Math.max(0, Math.min(savedMonths - 1, spinnerMonths.getCount() - 1));
            try {
                spinnerMonths.setSelection(index, false);
            } catch (NoSuchMethodError nsme) {
                spinnerMonths.setSelection(index);
            }

            loadSalarySummary(savedMonths);
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

        if (!pondName.isEmpty()) {
            SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
           // float savedActualROI = sp.getFloat(pondName + "_roi", -1f);
            float savedEstimatedROI = sp.getFloat(pondName + "_roi_diff", -1f);
            //String savedActualSales = sp.getString(pondName + "_actual_sales", "");
            //String savedEstimatedRevenue = sp.getString(pondName + "_estimated_revenue", "");


            if (savedEstimatedROI != -1f) {
                tvEstimatedRoI.setText(formatPrice(savedEstimatedROI) + "%");
                //tvRoIDifference.setText(formatPrice(savedEstimatedROI) + "%");
                //etEstimatedRevenue.setText("");
            }
        }
    }

    private void showFeedLogs() {
        BlindFeedingFragment feedLogsDialog = new BlindFeedingFragment();
        feedLogsDialog.show(getParentFragmentManager(), "feedLogsDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
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
        Button btnAddProductionCost = view.findViewById(R.id.btnAddProductionCost);
        Button btnStockFingerlings = view.findViewById(R.id.btnStockFingerlings);
        Button btnGenerateReport = view.findViewById(R.id.btnGenerateReport);

        LinearLayout pondInfoSection = view.findViewById(R.id.pondInformationSection);
        LinearLayout productionCostSummary = view.findViewById(R.id.productionCostSummarySection);
        LinearLayout roiSection = view.findViewById(R.id.roiSection);

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
            btnAddProductionCost.setVisibility(View.VISIBLE);
            btnStockFingerlings.setVisibility(View.VISIBLE);
            btnGenerateReport.setVisibility(View.GONE);

            pondInfoSection.setVisibility(View.GONE);
            productionCostSummary.setVisibility(View.GONE);
            roiSection.setVisibility(View.GONE);

            Toast.makeText(requireContext(), "Please stock fingerlings first before viewing production cost.", Toast.LENGTH_SHORT).show();
        } else {
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
                String urlString = "https://pondmate.alwaysdata.net/fetch_total_feed_cost.php";
                if (pondId != null && !pondId.isEmpty()) {
                    urlString += "?pond_id=" + pondId;
                }

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
                    Log.d("FEED_DEBUG", "PHP response: " + sb.toString());

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("total_feed_cost")) {
                        final double totalFeedCost = json.getDouble("total_feed_cost");

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

        if (!pondName.isEmpty()) {
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
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
                    response.append(line);}
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
                return;}

            double amount;
            try {
                amount = Double.parseDouble(costStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid cost", Toast.LENGTH_SHORT).show();
                return;}

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

    private void computeEstimatedROI() {
        Log.d("ROI_DEBUG", "Starting computeEstimatedROI()...");

        try {
            // ----- 1. Load pond data -----
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson == null) {
                Log.w("ROI_DEBUG", "❌ No pond data found in SharedPreferences.");
                tvEstimatedRoI.setText("—");
                return;
            }

            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            if (pond == null) {
                Log.e("ROI_DEBUG", "❌ PondModel deserialization failed.");
                tvEstimatedRoI.setText("—");
                return;
            }

            double fingerlingsCount = pond.getFishCount();
            double mortalityRate = pond.getMortalityRate();
            double survivalRate = 100 - mortalityRate;
            String breed = pond.getBreed();

            Log.d("ROI_DEBUG", "✅ Loaded pond data: Fingerlings=" + fingerlingsCount +
                    ", Mortality=" + mortalityRate +
                    ", Survival=" + survivalRate +
                    ", Breed=" + breed);

            // ----- 2. Fetch farmgate price from DB -----
            PondSyncManager.fetchFarmgatePrice(breed, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object result) {
                    try {
                        JSONObject json = new JSONObject(result.toString());
                        if (!json.has("status") || !json.getString("status").equals("success")) {
                            requireActivity().runOnUiThread(() -> tvEstimatedRoI.setText("—"));
                            return;
                        }

                        double farmGatePrice = json.optDouble("price", 0.0);
                        if (farmGatePrice <= 0) {
                            requireActivity().runOnUiThread(() -> tvEstimatedRoI.setText("—"));
                            return;
                        }

                        Log.d("ROI_DEBUG", "💰 Farmgate Price = ₱" + farmGatePrice);

                        // ----- 3. Compute harvest & gross sales -----
                        double totalHarvestKg = (fingerlingsCount * (survivalRate / 100.0)) / 4.0;
                        double grossSales = totalHarvestKg * farmGatePrice;

                        Log.d("ROI_DEBUG", "🐟 Harvest Weight = " + totalHarvestKg + " kg");
                        Log.d("ROI_DEBUG", "💵 Gross Sales = ₱" + grossSales);

                        // ----- 4. User-entered & fixed base costs -----
                        double fingerlingsCost = getDoubleFromText(tvSummaryFingerlings);
                        double salaryCost = getDoubleFromText(tvSummarySalary);

                        double supplies = 1000 * 6;
                        double misc = 1000 * 6;
                        double harvesting = grossSales * 0.02;
                        double caretaker = grossSales * 0.05;
                        double maintenance = grossSales * 0.02;

                        double baseCost = fingerlingsCost + salaryCost + supplies + misc + harvesting + caretaker + maintenance;
                        Log.d("ROI_DEBUG", "🧱 Base Cost (excluding feeds & fertilizer) = ₱" + baseCost);

                        // ----- 5. Compute total cost using allocation ratios -----
                        // Feeds = 60% of total cost
                        // Fertilizer = 3.06% of total cost
                        // BaseCost = 36.94% of total cost → T = Base / 0.3694
                        double totalCost = baseCost / 0.3694;
                        double feedCost = totalCost * 0.60;
                        double fertilizerCost = totalCost * 0.0306;

                        Log.d("ROI_DEBUG", "🥬 Feed Cost (60%) = ₱" + feedCost);
                        Log.d("ROI_DEBUG", "🌾 Fertilizer Cost (3.06%) = ₱" + fertilizerCost);

                        // ----- 6. Total expenses -----
                        double totalExpenses = baseCost + feedCost + fertilizerCost;
                        Log.d("ROI_DEBUG", "💸 Total Expenses = ₱" + totalExpenses);

                        // ----- 7. ROI -----
                        if (totalExpenses <= 0) {
                            requireActivity().runOnUiThread(() -> tvEstimatedRoI.setText("—"));
                            return;
                        }

                        double estimatedROI = ((grossSales - totalExpenses) / totalExpenses) * 100;
                        Log.d("ROI_DEBUG", "📈 Estimated ROI = " + estimatedROI + "%");

                        requireActivity().runOnUiThread(() ->
                                tvEstimatedRoI.setText(String.format(Locale.US, "%.2f%%", estimatedROI))
                        );

                    } catch (Exception e) {
                        Log.e("ROI_DEBUG", "❌ Error parsing farmgate JSON", e);
                        requireActivity().runOnUiThread(() -> tvEstimatedRoI.setText("—"));
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("ROI_DEBUG", "❌ Error fetching farmgate price: " + error);
                    requireActivity().runOnUiThread(() -> tvEstimatedRoI.setText("—"));
                }
            });

        } catch (Exception e) {
            Log.e("ROI_DEBUG", "❌ Error computing ROI", e);
            tvEstimatedRoI.setText("—");
        }
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
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"pond_id\"\r\n\r\n".getBytes());
                    outputStream.write(pondId.getBytes());
                    outputStream.write("\r\n".getBytes());

                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"name\"\r\n\r\n".getBytes());
                    outputStream.write(pondName.getBytes());
                    outputStream.write("\r\n".getBytes());

                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"action\"\r\n\r\n".getBytes());
                    outputStream.write(actionType.getBytes());
                    outputStream.write("\r\n".getBytes());

                    if (existingPdfPath != null && !existingPdfPath.isEmpty()) {
                        outputStream.write(("--" + boundary + "\r\n").getBytes());
                        outputStream.write("Content-Disposition: form-data; name=\"pdf_path\"\r\n\r\n".getBytes());
                        outputStream.write(existingPdfPath.getBytes());
                        outputStream.write("\r\n".getBytes());
                    }

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
