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
import com.itextpdf.text.pdf.ColumnText;
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
import java.util.Iterator;
import java.util.Locale;

public class PondPDFGenerator {

    public static File generatePDF(Context context, JSONObject data, String pondId) {
        String action = data.optString("action", "REPORT").toUpperCase(Locale.ROOT);

        JSONObject report = data.optJSONObject("report");
        if (report == null) {
            Log.e("PDF_DEBUG", "No 'report' object found in JSON");
            return null;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String baseName = "pond_report_" + timestamp + ".pdf";
        File pdfFile = new File(context.getCacheDir(), baseName);

        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new BorderAndWatermarkEvent(action));
            document.open();

            // HEADER / TITLE n
            String titleText;
            Font titleFont;

            if (action.equals("EMERGENCY_HARVEST")) {
                titleText = "EMERGENCY HARVEST REPORT";
                titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.RED);
            } else if (action.equals("INACTIVE")) {
                titleText = "INACTIVE POND REPORT";
                titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(80, 80, 80));
            } else {
                titleText = "POND REPORT";
                titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
            }

            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);

            // Sub-header information stays the same
            document.add(new Paragraph(report.optString("report_period", ""), new Font(Font.FontFamily.HELVETICA, 12)));
            document.add(new Paragraph("Generated on: " +
                    new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault()).format(new Date()),
                    new Font(Font.FontFamily.HELVETICA, 11)));
            document.add(new Paragraph("\n"));

            // --- POND DETAILS ---
            JSONObject pondDetails = report.optJSONObject("pond_details");
            if (pondDetails != null) {
                document.add(new Paragraph("POND DETAILS", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
                PdfPTable ptable = new PdfPTable(2);
                ptable.setWidthPercentage(100);

                for (Iterator<String> it = pondDetails.keys(); it.hasNext();) {
                    String key = it.next();
                    ptable.addCell(headerCell(key));
                    ptable.addCell(pondDetails.optString(key, "-"));
                }

                document.add(ptable);
                document.add(new Paragraph("\n"));
            }

            // --- EXPENSES SECTION ---
            JSONObject expenses = report.optJSONObject("expenses");
            if (expenses != null) {
                // 🔹 Fingerlings Table
                JSONObject fingerlings = expenses.optJSONObject("Fingerlings");
                if (fingerlings != null) {
                    document.add(new Paragraph("FINGERLINGS", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
                    document.add(new Paragraph("\n"));
                    PdfPTable ftable = new PdfPTable(4);
                    ftable.setWidthPercentage(100);
                    ftable.addCell(headerCell("Description"));
                    ftable.addCell(headerCell("Quantity"));
                    ftable.addCell(headerCell("Cost per Unit"));
                    ftable.addCell(headerCell("Amount"));

                    JSONArray fDetails = fingerlings.optJSONArray("details");
                    if (fDetails != null && fDetails.length() > 0) {
                        for (int i = 0; i < fDetails.length(); i++) {
                            JSONObject f = fDetails.getJSONObject(i);
                            ftable.addCell(f.optString("description", "-"));
                            ftable.addCell(f.optString("quantity", "0"));
                            ftable.addCell("₱" + f.optString("cost_per_unit", "0"));
                            ftable.addCell("₱" + f.optString("amount", "0"));
                        }
                    } else {
                        PdfPCell noData = new PdfPCell(new Phrase("No fingerlings data available"));
                        noData.setColspan(4);
                        noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                        ftable.addCell(noData);
                    }

                    PdfPCell ftotal = new PdfPCell(new Phrase("Total: ₱" +
                            String.format("%.2f", fingerlings.optDouble("total_cost", 0))));
                    ftotal.setColspan(4);
                    ftotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    ftable.addCell(ftotal);

                    document.add(ftable);
                    document.add(new Paragraph("\n"));
                }

                // 🔹 Feeds Table
                JSONObject feeds = expenses.optJSONObject("Feeds");
                if (feeds != null) {
                    document.add(new Paragraph("FEEDS", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
                    document.add(new Paragraph("\n"));
                    PdfPTable feedTable = new PdfPTable(4);
                    feedTable.setWidthPercentage(100);
                    feedTable.addCell(headerCell("Description"));
                    feedTable.addCell(headerCell("Quantity"));
                    feedTable.addCell(headerCell("Cost per Unit"));
                    feedTable.addCell(headerCell("Amount"));

                    JSONArray feedDetails = feeds.optJSONArray("details");
                    if (feedDetails != null && feedDetails.length() > 0) {
                        for (int i = 0; i < feedDetails.length(); i++) {
                            JSONObject fd = feedDetails.getJSONObject(i);
                            feedTable.addCell(fd.optString("description", "-"));
                            feedTable.addCell(fd.optString("quantity", "0"));
                            feedTable.addCell("₱" + fd.optString("cost_per_unit", "0"));
                            feedTable.addCell("₱" + fd.optString("amount", "0"));
                        }
                    } else {
                        PdfPCell noData = new PdfPCell(new Phrase("No feeds data available"));
                        noData.setColspan(4);
                        noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                        feedTable.addCell(noData);
                    }

                    PdfPCell ftotal = new PdfPCell(new Phrase("Total: ₱" +
                            String.format("%.2f", feeds.optDouble("total_cost", 0))));
                    ftotal.setColspan(4);
                    ftotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    feedTable.addCell(ftotal);

                    document.add(feedTable);
                    document.add(new Paragraph("\n"));
                }

                // 🔹 Other Expenses
                JSONObject others = expenses.optJSONObject("Other Expenses");
                if (others != null) {
                    document.add(new Paragraph("OTHER EXPENSES", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
                    document.add(new Paragraph("\n"));
                    PdfPTable otable = new PdfPTable(3);
                    otable.setWidthPercentage(100);
                    otable.addCell(headerCell("Type"));
                    otable.addCell(headerCell("Description"));
                    otable.addCell(headerCell("Amount"));

                    JSONArray otherDetails = others.optJSONArray("details");
                    if (otherDetails != null && otherDetails.length() > 0) {
                        for (int i = 0; i < otherDetails.length(); i++) {
                            JSONObject o = otherDetails.getJSONObject(i);
                            otable.addCell(o.optString("cost_type", "-"));
                            otable.addCell(o.optString("description", "-"));
                            otable.addCell("₱" + o.optString("amount", "0"));
                        }
                    } else {
                        PdfPCell noData = new PdfPCell(new Phrase("No other expenses available"));
                        noData.setColspan(3);
                        noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                        otable.addCell(noData);
                    }

                    PdfPCell total = new PdfPCell(new Phrase("Total: ₱" +
                            String.format("%.2f", others.optDouble("total_cost", 0))));
                    total.setColspan(3);
                    total.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    otable.addCell(total);

                    document.add(otable);
                }
            }

            // --- FINAL TOTAL ---
            document.add(new Paragraph("\nTOTAL EXPENSES: ₱" +
                    String.format("%.2f", report.optDouble("total_expenses", 0)),
                    new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD)));

            document.add(new Paragraph("\n\n--- End of Report ---"));
            document.close();
            Log.d("PDF_DEBUG", "PDF generated successfully: " + pdfFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("PDF_ERROR", "Error generating report PDF: " + e.getMessage());
            return null;
        }

        return pdfFile;
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

    // ✅ Adds both border + watermark
    private static class BorderAndWatermarkEvent extends PdfPageEventHelper {
        private final String action;

        public BorderAndWatermarkEvent(String action) {
            this.action = action;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();

            Rectangle rect = document.getPageSize();
            float offset = 20f;
            float left = document.leftMargin() - offset;
            float right = rect.getRight() - document.rightMargin() + offset;
            float top = rect.getTop() - document.topMargin() + offset;
            float bottom = document.bottomMargin() - offset;

            // Border
            canvas.setLineWidth(1.5f);
            canvas.rectangle(left, bottom, right - left, top - bottom);
            canvas.stroke();

            // Watermark
            if (action.equals("INACTIVE")) {
                // Gray inactive watermark
                Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 60, Font.BOLD, new BaseColor(200, 200, 200, 70));
                Phrase watermark = new Phrase("INACTIVE POND", watermarkFont);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark,
                        rect.getWidth() / 2, rect.getHeight() / 2, 45);

            } else if (action.equals("EMERGENCY_HARVEST")) {
                // 🔥 Emergency Harvest — Red, eye-catching watermark
                Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 60, Font.BOLD, new BaseColor(255, 0, 0, 70));
                Phrase watermark = new Phrase("EMERGENCY HARVEST", watermarkFont);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark,
                        rect.getWidth() / 2, rect.getHeight() / 2, 45);
            }
        }
    }
}
