package com.example.pondmatev1;

import android.content.Context;
import android.util.Log;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PondPDFGenerator {

    public static File generatePDF(Context context, JSONObject data, String pondId) {
        String baseName;
        if (pondId != null && !pondId.isEmpty()) {
            baseName = "pond_report_" + pondId + ".pdf";
        } else {
            JSONObject pondObj = data.optJSONObject("pond");
            String pname = (pondObj != null) ? pondObj.optString("name", "unknown") : "unknown";
            pname = pname.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            baseName = "pond_report_" + pname + ".pdf";
        }

        File pdfFile = new File(context.getCacheDir(), baseName);
        Log.d("PDF_DEBUG", "PondPDFGenerator: Generating PDF at: " + pdfFile.getAbsolutePath());

        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new BorderPageEvent());
            document.open();

            // HEADER
            document.add(new Paragraph("PRODUCTION REPORT"));
            String dateCreated = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault())
                    .format(new Date());
            document.add(new Paragraph("Report Generated: " + dateCreated));
            document.add(new Paragraph("-------------------------------------------------------------------------------\n"));

            // Pond info
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

            // Fingerlings Table
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
                ftable.addCell("No data");
                ftable.addCell("-");
                ftable.addCell("-");
                ftable.addCell("-");
            }
            document.add(ftable);
            document.add(new Paragraph("\n"));

            // Maintenance table
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
                mtable.addCell("No data");
                mtable.addCell("-");
            }
            document.add(mtable);
            document.add(new Paragraph("\n"));
            // Salary Cost (Per Pond)
            JSONObject salary = data.optJSONObject("salary");
            double totalSalary = (salary != null) ? salary.optDouble("total_salary", 0) : 0;


            document.add(new Paragraph("\n")); // optional spacing

            // Totals and ROI
            maintenance = data.optJSONObject("maintenance");
            salary = data.optJSONObject("salary");
            JSONObject roi = data.optJSONObject("roi");

            document.add(new Paragraph("Maintenance Total: ₱" + (maintenance != null ? String.format("%.2f", maintenance.optDouble("total_maintenance", 0)) : "0.00")));
            document.add(new Paragraph("Salary Total: ₱" + (salary != null ? String.format("%.2f", salary.optDouble("total_salary", 0)) : "0.00")));
            document.add(new Paragraph("\nROI"));
            document.add(new Paragraph("Total Cost: ₱" + (roi != null ? String.format("%.2f", roi.optDouble("total_cost", 0)) : "0.00")));
            document.add(new Paragraph("Estimated ROI: " + (roi != null ? String.format("%.2f", roi.optDouble("estimated", 0)) : "0.00")));
            document.add(new Paragraph("Actual ROI: " + (roi != null ? String.format("%.2f", roi.optDouble("actual", 0)) : "0.00")));



            document.newPage();
            document.newPage();
            Paragraph feedsTitle = new Paragraph("Feeds Schedules", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD));
            feedsTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(feedsTitle);
            document.add(new Paragraph("\n"));

// ✅ Table with 5 columns
            PdfPTable feedsTable = new PdfPTable(5);
            feedsTable.setWidthPercentage(100);
            feedsTable.setWidths(new float[]{2.5f, 2f, 2f, 2f, 2f});

// Headers
            feedsTable.addCell(headerCell("Date"));
            feedsTable.addCell(headerCell("Time"));
            feedsTable.addCell(headerCell("Feed Type"));
            feedsTable.addCell(headerCell("Feed Amount (kg)"));
            feedsTable.addCell(headerCell("Feed Cost (pesos)"));

// Populate rows
            JSONObject feedingSchedule = data.optJSONObject("feeding_schedule");
            JSONArray feedLogs = feedingSchedule != null ? feedingSchedule.optJSONArray("logs") : null;

            if (feedLogs != null && feedLogs.length() > 0) {
                // Date & time formatters
                SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US); // September 2, 2025

                SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
                SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a", Locale.US); // 05:47 PM

                for (int i = 0; i < feedLogs.length(); i++) {
                    JSONObject f = feedLogs.getJSONObject(i);

                    // Format date
                    String rawDate = f.optString("schedule_date", "-");
                    String formattedDate = rawDate;
                    try {
                        Date date = inputDateFormat.parse(rawDate);
                        if (date != null) formattedDate = outputDateFormat.format(date);
                    } catch (Exception e) { e.printStackTrace(); }

                    // Format time
                    String rawTime = f.optString("schedule_time", "-");
                    String formattedTime = rawTime;
                    try {
                        Date time = inputTimeFormat.parse(rawTime);
                        if (time != null) formattedTime = outputTimeFormat.format(time);
                    } catch (Exception e) { e.printStackTrace(); }

                    // Add row
                    feedsTable.addCell(formattedDate);
                    feedsTable.addCell(formattedTime);
                    feedsTable.addCell(f.optString("feeder_type", "-"));
                    feedsTable.addCell(f.optString("feed_amount", "0"));
                    feedsTable.addCell(f.optString("feed_price", "0"));
                }

                // ✅ Total row
                PdfPCell totalLabel = new PdfPCell(new Phrase("Total Feed Cost"));
                totalLabel.setColspan(4);
                totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                feedsTable.addCell(totalLabel);

                double totalFeedCostValue = feedingSchedule.optDouble("total_feed_cost", 0.0);


                String totalFeedCost = String.format(Locale.US, "%.2f", totalFeedCostValue);

                feedsTable.addCell(totalFeedCost);


            } else {
                PdfPCell noDataCell = new PdfPCell(new Phrase("No feed logs available"));
                noDataCell.setColspan(5);
                noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                feedsTable.addCell(noDataCell);
            }

            document.add(new Paragraph("\n"));
            document.add(feedsTable);
            document.add(new Paragraph("\n\n--- End of Report ---"));


            document.close();

            Log.d("PDF_DEBUG", "PondPDFGenerator: PDF generated successfully. Size: " + pdfFile.length());

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PondPDFGenerator", "Error generating PDF: " + e.getMessage(), e);
            return null;
        }

        return pdfFile;
    }

    // Helper to format date string "yyyy-MM-dd" -> "MMMM d, yyyy"
    private static String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dateString));
        } catch (Exception e) {
            return dateString;
        }
    }

    // Build a header cell like in your fragment
    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

    // Page border event (same visuals as in your fragment)
    private static class BorderPageEvent extends PdfPageEventHelper {
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
}
