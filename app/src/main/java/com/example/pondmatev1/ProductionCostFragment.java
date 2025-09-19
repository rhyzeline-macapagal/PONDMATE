package com.example.pondmatev1;

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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.gson.Gson;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
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
    TextView tvCapital, tvROIAmount, tvROI, tvEstimatedRoI, tvRoIDifference;
    EditText etEstimatedSales, etEstimatedRevenue;

    private double totalCost = 0.0;
    private String pondName = "";

    Button btnDownload;

    File generatedPdfFile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_production_cost, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
        }

        loadMaintenanceTotal();

        Button btnAddMaintenance = view.findViewById(R.id.btnAddProductionCost);
        btnAddMaintenance.setOnClickListener(v -> showAddMaintenanceDialog());
        Button btnGenerateReport = view.findViewById(R.id.btnGenerateReport);
        btnGenerateReport.setOnClickListener(v -> {
            if (pondJson != null) {
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                String pondNameLocal = pond.getName();

                PondSyncManager.fetchProductionReportByName(pondNameLocal, new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                JSONObject json = new JSONObject(String.valueOf(response));

                                if (json.getString("status").equals("success")) {
                                    // extract pond id from returned JSON
                                    JSONObject pondObj = json.optJSONObject("pond");
                                    String pondId = "";
                                    if (pondObj != null) {
                                        // Try common keys
                                        pondId = pondObj.optString("id", pondObj.optString("pond_id", ""));
                                    }

                                    // fallback to using name if no id
                                    if (pondId == null) pondId = "";
                                    generatedPdfFile = generatePDF(json, pondId);

                                    if (generatedPdfFile != null && generatedPdfFile.exists()) {
                                        // In-app preview using PDFView (via PdfPreviewActivity)
                                        previewPDF(generatedPdfFile);
                                        Log.d("PDF_DEBUG", "showPdfDialog called for file: " + generatedPdfFile.getAbsolutePath() +
                                                ", size: " + generatedPdfFile.length());

                                        // Still save a copy to Downloads
                                        savePDFToDownloads(generatedPdfFile);

                                        // Upload the generated PDF and save history (pass pond id & name)
                                        uploadPdfAndSaveHistory(pondId, pondNameLocal, generatedPdfFile);

                                        Toast.makeText(getContext(),
                                                "Report generated for " + pondNameLocal,
                                                Toast.LENGTH_SHORT).show();


                                    } else {
                                        Toast.makeText(getContext(), "Failed to create PDF", Toast.LENGTH_SHORT).show();
                                        Log.d("PDF_DEBUG", "generatePDF returned null or file does not exist");
                                    }

                                } else {
                                    Toast.makeText(getContext(),
                                            "Error: " + json.optString("message", "Unknown error"),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(),
                                        "Parse error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(getContext(),
                                        "Network Error: " + error,
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            } else {
                Toast.makeText(getContext(), "No pond selected", Toast.LENGTH_SHORT).show();
            }
        });

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
        etEstimatedSales = view.findViewById(R.id.etActualSales);

        etEstimatedRevenue = view.findViewById(R.id.etEstimatedRevenue);
        tvEstimatedRoI = view.findViewById(R.id.tvEstimatedROI);
        tvRoIDifference = view.findViewById(R.id.tvROIDifference);


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

            totalCost = totalFingerlingCost + feedCost + maintenanceCost;
            tvSummaryTotal.setText("₱" + formatPrice(totalCost));
            tvCapital.setText("₱" + formatPrice(totalCost));
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
                etEstimatedSales.setText("");
            }

            if (savedEstimatedROI != -1f) {
                tvEstimatedRoI.setText(formatPrice(savedEstimatedROI) + "%");
                tvRoIDifference.setText(formatPrice(savedEstimatedROI) + "%");
                etEstimatedRevenue.setText("");
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

    /**
     * generatePDF now takes pond JSON and the pondId (used for stable filename).
     * It writes the file to cacheDir and returns the File.
     */
    private File generatePDF(JSONObject data, String pondId) {
        // Build stable filename: prefer pondId; fallback to sanitized pond name + timestamp
        String baseName;
        if (pondId != null && !pondId.isEmpty()) {
            baseName = "pond_report_" + pondId + ".pdf";
        } else {
            JSONObject pondObj = data.optJSONObject("pond");
            String pname = (pondObj != null) ? pondObj.optString("name", "unknown") : "unknown";
            // sanitize name to use in filename
            pname = pname.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            baseName = "pond_report_" + pname + ".pdf";
        }

        File pdfFile = new File(requireContext().getCacheDir(), baseName);
        Log.d("PDF_DEBUG", "Generating PDF at: " + pdfFile.getAbsolutePath());

        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new BorderPageEvent());
            document.open();

            // --- content (same as before) ---
            document.add(new Paragraph("PRODUCTION REPORT"));
            String dateCreated = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault())
                    .format(new Date());
            document.add(new Paragraph("Report Generated: " + dateCreated));

            document.add(new Paragraph("-------------------------------------------------------------------------------\n"));

            JSONObject pond = data.optJSONObject("pond");

            String pondName = pond != null ? pond.optString("name", "N/A") : "N/A";
            String breed = pond != null ? pond.optString("breed", "N/A") : "N/A";
            String startRaw = pond != null ? pond.optString("date_started", "N/A") : "N/A";
            String endRaw = pond != null ? pond.optString("date_harvest", "N/A") : "N/A";
            String start = !startRaw.equals("N/A") ? formatDate(startRaw) : "N/A";
            String end = !endRaw.equals("N/A") ? formatDate(endRaw) : "N/A";

            document.add(new Paragraph("Pond Name: " + pondName));
            document.add(new Paragraph("Breed: " + breed));
            document.add(new Paragraph("Period: " + start + " to " + end));
            document.add(new Paragraph("\n"));

            // --- Fingerlings Table ---
            document.add(new Paragraph("Fingerlings Cost"));
            document.add(new Paragraph("\n"));
            PdfPTable ftable = new PdfPTable(4);
            ftable.setWidthPercentage(100);

            ftable.addCell(headerCell("Quantity"));
            ftable.addCell(headerCell("Unit"));
            ftable.addCell(headerCell("Cost per Unit"));
            ftable.addCell(headerCell("Amount"));

            JSONArray fingerlings = data.optJSONArray("fingerlings");
            if (fingerlings != null && fingerlings.length() > 0) {
                for (int i = 0; i < fingerlings.length(); i++) {
                    JSONObject f = fingerlings.getJSONObject(i);
                    ftable.addCell(f.optString("quantity", "0"));
                    ftable.addCell(f.optString("unit", "-"));
                    ftable.addCell("₱" + f.optString("cost_per_unit", "0"));
                    ftable.addCell("₱" + f.optString("amount", "0"));
                }
            } else {
                ftable.addCell("No data"); ftable.addCell("-"); ftable.addCell("-"); ftable.addCell("-");
            }
            document.add(ftable);
            document.add(new Paragraph("\n"));

            // --- Maintenance Table ---
            document.add(new Paragraph("Maintenance Cost"));
            document.add(new Paragraph("\n"));
            PdfPTable mtable = new PdfPTable(2);
            mtable.setWidthPercentage(100);
            mtable.addCell(headerCell("Description"));
            mtable.addCell(headerCell("Amount"));

            JSONObject maintenance = data.optJSONObject("maintenance");
            JSONArray maintenanceLogs = maintenance != null ? maintenance.optJSONArray("logs") : null;
            if (maintenanceLogs != null && maintenanceLogs.length() > 0) {
                for (int i = 0; i < maintenanceLogs.length(); i++) {
                    JSONObject m = maintenanceLogs.getJSONObject(i);
                    mtable.addCell(m.optString("description", "-"));
                    mtable.addCell("₱" + m.optString("amount", "0"));
                }
            } else {
                mtable.addCell("No data"); mtable.addCell("-");
            }
            document.add(mtable);
            document.add(new Paragraph("\n"));

            // --- Salary Table ---
            document.add(new Paragraph("Salary Cost"));
            document.add(new Paragraph("\n"));
            PdfPTable stable = new PdfPTable(5);
            stable.setWidthPercentage(100);
            stable.addCell(headerCell("Description"));
            stable.addCell(headerCell("Quantity"));
            stable.addCell(headerCell("Unit"));
            stable.addCell(headerCell("Cost per Unit"));
            stable.addCell(headerCell("Amount"));

            JSONObject salary = data.optJSONObject("salary");
            JSONArray salaryLogs = salary != null ? salary.optJSONArray("logs") : null;
            if (salaryLogs != null && salaryLogs.length() > 0) {
                for (int i = 0; i < salaryLogs.length(); i++) {
                    JSONObject s = salaryLogs.getJSONObject(i);
                    stable.addCell(s.optString("description", "-"));
                    stable.addCell(s.optString("quantity", "0"));
                    stable.addCell(s.optString("unit", "-"));
                    stable.addCell("₱" + s.optString("cost_per_unit", "0"));
                    stable.addCell("₱" + s.optString("amount", "0"));
                }
            } else {
                stable.addCell("No data"); stable.addCell("-"); stable.addCell("-"); stable.addCell("-"); stable.addCell("-");
            }
            document.add(stable);
            document.add(new Paragraph("\n"));

            // --- Feeds Logs Table ---
            document.add(new Paragraph("Feeds Logs"));
            document.add(new Paragraph("\n"));
            PdfPTable feedsTable = new PdfPTable(5);
            feedsTable.setWidthPercentage(100);
            feedsTable.addCell(headerCell("Sched 1"));
            feedsTable.addCell(headerCell("Sched 2"));
            feedsTable.addCell(headerCell("Sched 3"));
            feedsTable.addCell(headerCell("Feeder Type"));
            feedsTable.addCell(headerCell("Feed Amount"));

            JSONObject feeds = data.optJSONObject("feeds");
            JSONArray feedLogs = feeds != null ? feeds.optJSONArray("logs") : null;
            if (feedLogs != null && feedLogs.length() > 0) {
                for (int i = 0; i < feedLogs.length(); i++) {
                    JSONObject f = feedLogs.getJSONObject(i);
                    feedsTable.addCell(f.optString("sched_one", "-"));
                    feedsTable.addCell(f.optString("sched_two", "-"));
                    feedsTable.addCell(f.optString("sched_three", "-"));
                    feedsTable.addCell(f.optString("feeder_type", "-"));
                    feedsTable.addCell(f.optString("feed_amount", "0"));
                }
            } else {
                feedsTable.addCell("No data"); feedsTable.addCell("-"); feedsTable.addCell("-"); feedsTable.addCell("-"); feedsTable.addCell("-");
            }
            document.add(feedsTable);
            document.add(new Paragraph("\n"));

            feeds = data.optJSONObject("feeds");
            maintenance = data.optJSONObject("maintenance");
            salary = data.optJSONObject("salary");
            JSONObject roi = data.optJSONObject("roi");

            document.add(new Paragraph("Feeds Total Cost: ₱" + (feeds != null ? String.format("%.2f", feeds.optDouble("total_feed_cost", 0)) : "0.00")));
            document.add(new Paragraph("Maintenance Total: ₱" + (maintenance != null ? String.format("%.2f", maintenance.optDouble("total_maintenance", 0)) : "0.00")));
            document.add(new Paragraph("Salary Total: ₱" + (salary != null ? String.format("%.2f", salary.optDouble("total_salary", 0)) : "0.00")));


            // --- ROI Section ---
            document.add(new Paragraph("\nROI"));
            document.add(new Paragraph("Total Cost: ₱" + (roi != null ? String.format("%.2f", roi.optDouble("total_cost", 0)) : "0.00")));
            document.add(new Paragraph("Estimated ROI: " + (roi != null ? String.format("%.2f", roi.optDouble("estimated", 0)) : "0.00")));
            document.add(new Paragraph("Actual ROI: " + (roi != null ? String.format("%.2f", roi.optDouble("actual", 0)) : "0.00")));

            document.add(new Paragraph("\n\n--- End of Report ---"));

            document.close();

            Log.d("PDF_DEBUG", "PDF generated successfully. Size: " + pdfFile.length());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return pdfFile;
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

    private void saveComparisonROI(String pond, double roiPercent) {
        SharedPreferences sp = requireContext().getSharedPreferences("ROI_DATA", Context.MODE_PRIVATE);
        sp.edit().putFloat(pond + "_roi_diff", (float) roiPercent).apply();

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

    /**
     * Uploads PDF via multipart and writes the pond_history record (server should save file and pdf_path).
     * We POST pond_id, name, action and pdf_file.
     */
    private void uploadPdfAndSaveHistory(String pondId, String pondName, File pdfFile) {
        new Thread(() -> {
            try {
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                URL url = new URL("https://pondmate.alwaysdata.net/savePondHistory.php"); // ensure your server script handles multipart
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream outputStream = conn.getOutputStream()) {
                    // --- pond_id ---
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"pond_id\"\r\n\r\n".getBytes());
                    outputStream.write((pondId != null ? pondId : "").getBytes());
                    outputStream.write("\r\n".getBytes());

                    // --- name ---
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"name\"\r\n\r\n".getBytes());
                    outputStream.write((pondName != null ? pondName : "").getBytes());
                    outputStream.write("\r\n".getBytes());

                    // --- action ---
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write("Content-Disposition: form-data; name=\"action\"\r\n\r\n".getBytes());
                    outputStream.write("generate_report".getBytes());
                    outputStream.write("\r\n".getBytes());

                    // --- file field ---
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

                    // --- End boundary ---
                    outputStream.write(("--" + boundary + "--\r\n").getBytes());
                    outputStream.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("PDF_UPLOAD", "Server Response: " + response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "PDF uploaded & saved to pond history", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "PDF upload failed: HTTP " + responseCode, Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}
