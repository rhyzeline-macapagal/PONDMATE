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

            // HEADER / TITLE
            String titleText;
            Font titleFont;

            if (action.equals("EMERGENCY_HARVEST")) {
                titleText = "COLLEGE OF FISHERIES AQUATIC SCIENCES AND TECHNOLOGY (CFAST) AQUACULTURE FARM\nEMERGENCY HARVEST REPORT";
                titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.RED);
            } else if (action.equals("INACTIVE")) {
                titleText = "COLLEGE OF FISHERIES AQUATIC SCIENCES AND TECHNOLOGY (CFAST) AQUACULTURE FARM\nINACTIVE POND REPORT";
                titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(80, 80, 80));
            } else {
                titleText = "COLLEGE OF FISHERIES AQUATIC SCIENCES AND TECHNOLOGY (CFAST) AQUACULTURE FARM\nPOND REPORT";
                titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
            }

            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);

            // Sub-header information
            document.add(new Paragraph(report.optString("report_period", ""),
                    new Font(Font.FontFamily.HELVETICA, 12)));
            document.add(new Paragraph("Generated on: " +
                    new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault()).format(new Date()),
                    new Font(Font.FontFamily.HELVETICA, 11)));
            document.add(new Paragraph("\n"));

            // POND DETAILS
            addPondDetails(document, report, data);

            // EXPENSES SECTION
            JSONObject expenses = report.optJSONObject("expenses");
            if (expenses != null) {
                addFingerlingsSection(document, expenses);
                addFeedsSection(document, expenses);
                addOtherExpensesSection(document, expenses);
                addSalarySection(document, expenses);
                addTotalCapital(document, expenses);
            }

            document.add(new Paragraph("\n\n--- End of Report ---"));
            document.close();
            Log.d("PDF_DEBUG", "PDF generated successfully: " + pdfFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("PDF_ERROR", "Error generating report PDF: " + e.getMessage());
            return null;
        }

        return pdfFile;
    }

    private static String formatPeso(double value) {
        return "P " + String.format("%,.2f", value);
    }

    private static void addPondDetails(Document document, JSONObject report, JSONObject rootData) throws Exception {
        JSONObject pondDetails = report.optJSONObject("pond_details");
        if (pondDetails != null) {
            document.add(new Paragraph("POND DETAILS",
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            document.add(new Paragraph("\n"));

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 12);

            for (Iterator<String> it = pondDetails.keys(); it.hasNext();) {
                String key = it.next();
                String value = pondDetails.optString(key, "-");

                Paragraph line = new Paragraph(key + ": ", labelFont);
                line.add(new Phrase(value, valueFont));
                line.setSpacingAfter(4f);
                document.add(line);
            }

            String action = rootData.optString("action", "REPORT");
            if (action.equalsIgnoreCase("EMERGENCY_HARVEST")) {
                String reason = rootData.optString("emergency_reason", "No reason provided");
                Log.d("PDF_DEBUG_REASON", "Emergency reason: " + reason); // <-- debug
                Paragraph reasonParagraph = new Paragraph("Emergency Reason: ", labelFont);
                reasonParagraph.add(new Phrase(reason, valueFont));
                reasonParagraph.setSpacingAfter(4f);
                document.add(reasonParagraph);
            }

            document.add(new Paragraph("\n"));
        }
    }



    private static void addFingerlingsSection(Document document, JSONObject expenses) throws Exception {
        JSONObject fingerlings = expenses.optJSONObject("Fingerlings");
        if (fingerlings != null) {
            document.add(new Paragraph("FINGERLINGS",
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Description"));
            table.addCell(headerCell("Quantity"));
            table.addCell(headerCell("Cost per Unit"));
            table.addCell(headerCell("Amount"));

            JSONArray details = fingerlings.optJSONArray("details");
            if (details != null && details.length() > 0) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject item = details.getJSONObject(i);
                    table.addCell(item.optString("description", "-"));
                    table.addCell(item.optString("quantity", "0"));
                    table.addCell(formatPeso(item.optDouble("cost_per_unit", 0)));
                    table.addCell(formatPeso(item.optDouble("amount", 0)));
                }
            } else {
                PdfPCell noData = new PdfPCell(new Phrase("No fingerlings data available"));
                noData.setColspan(4);
                noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noData);
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("Total: " + formatPeso(fingerlings.optDouble("total_cost", 0))));
            totalCell.setColspan(4);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalCell);

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }

    private static void addFeedsSection(Document document, JSONObject expenses) throws Exception {
        JSONObject feeds = expenses.optJSONObject("Feeds");
        if (feeds != null) {
            document.add(new Paragraph("ACCUMULATED FEED COST",
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Description"));
            table.addCell(headerCell("Amount"));

            JSONArray details = feeds.optJSONArray("details");
            if (details != null && details.length() > 0) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject item = details.getJSONObject(i);
                    table.addCell(item.optString("description", "-"));

                    PdfPCell amountCell = new PdfPCell(new Phrase(formatPeso(item.optDouble("amount", 0))));
                    amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(amountCell);
                }
            } else {
                PdfPCell noData = new PdfPCell(new Phrase("No feed cost data available"));
                noData.setColspan(2);
                noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noData);
            }

            double accumulatedFeedCost = feeds.optDouble("total_cost", 0);
            double blindFeedCost = feeds.optDouble("blind_feed_cost", 0);
            double combinedFeedTotal = accumulatedFeedCost + blindFeedCost;

            if (blindFeedCost > 0) {
                PdfPCell blindLabel = new PdfPCell(new Phrase("Blind Feeding Breakdown:"));
                blindLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(blindLabel);

                PdfPCell blindValue = new PdfPCell(new Phrase(formatPeso(blindFeedCost)));
                blindValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(blindValue);
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total Feed Cost (Accumulated + Blind):"));
            totalLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase(formatPeso(combinedFeedTotal)));
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalValue);

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }

    private static void addOtherExpensesSection(Document document, JSONObject expenses) throws Exception {
        JSONObject others = expenses.optJSONObject("Other Expenses");
        if (others != null) {
            document.add(new Paragraph("OTHER EXPENSES",
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Type"));
            table.addCell(headerCell("Description"));
            table.addCell(headerCell("Amount"));

            JSONArray details = others.optJSONArray("details");
            if (details != null && details.length() > 0) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject item = details.getJSONObject(i);
                    table.addCell(item.optString("cost_type", "-"));
                    table.addCell(item.optString("description", "-"));
                    table.addCell(formatPeso(item.optDouble("amount", 0)));
                }
            } else {
                PdfPCell noData = new PdfPCell(new Phrase("No other expenses available"));
                noData.setColspan(3);
                noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noData);
            }

            // ✅ Correct total row
            PdfPCell totalCell = new PdfPCell(new Phrase("Total:" + formatPeso(others.optDouble("total_cost", 0))));
            totalCell.setColspan(3);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setPaddingTop(6f);
            totalCell.setPaddingBottom(6f);
            table.addCell(totalCell);

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }

    private static void addSalarySection(Document document, JSONObject expenses) throws Exception {
        JSONObject salary = expenses.optJSONObject("Salary");
        if (salary != null) {
            document.add(new Paragraph("CARETAKER SALARY",
                    new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Description"));
            table.addCell(headerCell("Amount"));

            JSONArray details = salary.optJSONArray("details");
            if (details != null && details.length() > 0) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject item = details.getJSONObject(i);
                    table.addCell(item.optString("description", "-"));

                    PdfPCell amountCell = new PdfPCell(new Phrase(formatPeso(item.optDouble("amount", 0))));
                    amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(amountCell);
                }
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("Total Salary:" + formatPeso(salary.optDouble("total_cost", 0))));totalCell.setColspan(2);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalCell);

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }

    private static void addTotalCapital(Document document, JSONObject expenses) throws Exception {
        double totalCapital = 0;

        if (expenses.optJSONObject("Fingerlings") != null) {
            totalCapital += expenses.optJSONObject("Fingerlings").optDouble("total_cost", 0);
        }

        if (expenses.optJSONObject("Feeds") != null) {
            double accumulatedFeedCost = expenses.optJSONObject("Feeds").optDouble("total_cost", 0);
            double blindFeedCost = expenses.optJSONObject("Feeds").optDouble("blind_feed_cost", 0);
            totalCapital += (accumulatedFeedCost + blindFeedCost);
        }

        if (expenses.optJSONObject("Other Expenses") != null) {
            totalCapital += expenses.optJSONObject("Other Expenses").optDouble("total_cost", 0);
        }

        if (expenses.optJSONObject("Salary") != null) {
            totalCapital += expenses.optJSONObject("Salary").optDouble("total_cost", 0);
        }

        document.add(new Paragraph("\nTOTAL CAPITAL / EXPENSES:" +
                formatPeso(totalCapital),
                new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));

    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

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

            // Draw border
            canvas.setLineWidth(1.5f);
            canvas.rectangle(left, bottom, right - left, top - bottom);
            canvas.stroke();

            // Add watermark based on action type
            if (action.equals("INACTIVE")) {
                Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 60, Font.BOLD,
                        new BaseColor(200, 200, 200, 70));
                Phrase watermark = new Phrase("INACTIVE POND", watermarkFont);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark,
                        rect.getWidth() / 2, rect.getHeight() / 2, 45);

            } else if (action.equals("EMERGENCY_HARVEST")) {
                Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 60, Font.BOLD,
                        new BaseColor(255, 0, 0, 70));
                Phrase watermark = new Phrase("EMERGENCY HARVEST", watermarkFont);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark,
                        rect.getWidth() / 2, rect.getHeight() / 2, 45);
            }
        }
    }
}