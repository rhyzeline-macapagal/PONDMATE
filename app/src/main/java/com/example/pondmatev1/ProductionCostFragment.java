package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProductionCostFragment extends Fragment {

    TextView tvfishbreed, tvfishamount, tvnumberoffish, tvfishcost, tvcapital;
    LinearLayout linlfeederscont, linlmaintenancecont, linlsalarycont, feederslist, maintenancelist, salarylist;
    Button btnedit, btnviewsummary, btnfeedertype, btnmaintenancetype,btnsalarydate;
    ImageButton btnaddfeedslist, btnaddmaintenancelist, btnaddsalarylist;
    EditText etfeedercost, etmaintenancecost, etsalarycost;

    public ProductionCostFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_production_cost, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvfishbreed = view.findViewById(R.id.fishbreedpcostdisplay);
        tvfishamount = view.findViewById(R.id.amtperpiece);
        tvnumberoffish = view.findViewById(R.id.numoffingerlings);
        tvfishcost = view.findViewById(R.id.amtoffingerlings);
        tvcapital = view.findViewById(R.id.capital);
        btnedit = view.findViewById(R.id.editpcost);
        btnviewsummary = view.findViewById(R.id.btnsummary);

        linlfeederscont = view.findViewById(R.id.feeders_container);
        btnfeedertype = view.findViewById(R.id.typeoffeeders);
        etfeedercost = view.findViewById(R.id.amtoffeeders);
        btnaddfeedslist = view.findViewById(R.id.addToFeedsbtn);
        feederslist = view.findViewById(R.id.feedersList);

        linlmaintenancecont = view.findViewById(R.id.maintenance_container);
        btnmaintenancetype = view.findViewById(R.id.initialMaintenanceType);
        etmaintenancecost = view.findViewById(R.id.initialMaintenanceCost);
        btnaddmaintenancelist = view.findViewById(R.id.addMaintenanceButton);
        maintenancelist = view.findViewById(R.id.maintenanceList);

        linlsalarycont = view.findViewById(R.id.salary_container);
        btnsalarydate = view.findViewById(R.id.initialSalaryDate);
        etsalarycost = view.findViewById(R.id.initialSalaryCost);
        btnaddsalarylist = view.findViewById(R.id.addSalaryButton);
        salarylist = view.findViewById(R.id.salaryList);

        etfeedercost.setEnabled(false);
        etmaintenancecost.setEnabled(false);
        etsalarycost.setEnabled(false);

        btnaddfeedslist.setEnabled(false);
        btnaddmaintenancelist.setEnabled(false);
        btnaddsalarylist.setEnabled(false);

        btnfeedertype.setEnabled(false);
        btnmaintenancetype.setEnabled(false);
        btnsalarydate.setEnabled(false);

        btnviewsummary.setEnabled(false);

        setupFeederTypeButton(btnfeedertype, etfeedercost);
        setupMaintenanceTypeButton(btnmaintenancetype, etmaintenancecost);
        setupSalaryDateButton(btnsalarydate);

        etfeedercost.addTextChangedListener(capitalWatcher);
        etmaintenancecost.addTextChangedListener(capitalWatcher);
        etsalarycost.addTextChangedListener(capitalWatcher);

        btnedit.setOnClickListener(new View.OnClickListener() {
            private boolean isEditing = false;

            @Override
            public void onClick(View v) {
                if (!isEditing) {
                    // Switch to editing mode
                    isEditing = true;
                    btnedit.setText("Save");

                    // Enable input fields
                    etfeedercost.setEnabled(true);
                    etfeedercost.setFocusableInTouchMode(true);

                    etmaintenancecost.setEnabled(true);
                    etmaintenancecost.setFocusableInTouchMode(true);

                    etsalarycost.setEnabled(true);
                    etsalarycost.setFocusableInTouchMode(true);

                    // Enable action buttons
                    btnaddfeedslist.setEnabled(true);
                    btnaddmaintenancelist.setEnabled(true);
                    btnaddsalarylist.setEnabled(true);

                    btnfeedertype.setEnabled(true);
                    btnmaintenancetype.setEnabled(true);
                    btnsalarydate.setEnabled(true);

                } else {
                    isEditing = false;
                    btnedit.setText("Edit");
                    // Disable input fields
                    etfeedercost.setEnabled(false);
                    etmaintenancecost.setEnabled(false);
                    etsalarycost.setEnabled(false);
                    // Disable action buttons
                    btnaddfeedslist.setEnabled(false);
                    btnaddmaintenancelist.setEnabled(false);
                    btnaddsalarylist.setEnabled(false);
                    btnfeedertype.setEnabled(false);
                    btnmaintenancetype.setEnabled(false);
                    btnsalarydate.setEnabled(false);

                    // TODO: Save logic (if needed)
                    // e.g., store values to DB, SharedPreferences, etc.
                }
            }
        });

        btnaddfeedslist.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View newRow = inflater.inflate(R.layout.row_feeds, feederslist, false);

            ImageButton btnRemove = newRow.findViewById(R.id.removeToFeedsbtn);
            btnRemove.setOnClickListener(removeView -> {
                feederslist.removeView(newRow);
                calculateTotalCapital();
            });

            Button dynamicFeederTypeBtn = newRow.findViewById(R.id.typeoffeeders);
            EditText dynamicAmount = newRow.findViewById(R.id.amtoffeeders);
            dynamicAmount.addTextChangedListener(capitalWatcher);

            dynamicAmount.setEnabled(true);
            dynamicAmount.setFocusableInTouchMode(true);
            dynamicAmount.setFocusable(true);
            dynamicAmount.setCursorVisible(true);

            setupFeederTypeButton(dynamicFeederTypeBtn, dynamicAmount);

            feederslist.addView(newRow);
        });

        btnaddmaintenancelist.setOnClickListener(v ->{
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View newRow = inflater.inflate(R.layout.row_maintenance, maintenancelist, false);

            Button dynamicType = newRow.findViewById(R.id.initialMaintenanceType);
            EditText dynamicCost = newRow.findViewById(R.id.initialMaintenanceCost);
            dynamicCost.addTextChangedListener(capitalWatcher);

            dynamicCost.setEnabled(true);
            dynamicCost.setFocusable(true);
            dynamicCost.setFocusableInTouchMode(true);
            dynamicCost.setCursorVisible(true);

            setupMaintenanceTypeButton(dynamicType, dynamicCost);

            ImageButton btnRemove = newRow.findViewById(R.id.removeMaintenanceButton);
            btnRemove.setOnClickListener(removeView ->{
                maintenancelist.removeView(newRow);
                calculateTotalCapital();
            });

            maintenancelist.addView(newRow);
        });

        btnaddsalarylist.setOnClickListener(v ->{
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View newRow = inflater.inflate(R.layout.row_salary, salarylist, false);

            ImageButton btnRemove = newRow.findViewById(R.id.removeSalaryButton);
            btnRemove.setOnClickListener(removeView ->{
                salarylist.removeView(newRow);
                calculateTotalCapital();
            });

            Button dynamicSalaryDateBtn = newRow.findViewById(R.id.initialSalaryDate);
            setupSalaryDateButton(dynamicSalaryDateBtn);

            EditText dynamicSalaryCost = newRow.findViewById(R.id.initialSalaryCost);
            dynamicSalaryCost.addTextChangedListener(capitalWatcher);

            dynamicSalaryCost.setEnabled(true);
            dynamicSalaryCost.setFocusableInTouchMode(true);

            salarylist.addView(newRow);
        });

        btnviewsummary.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.dialog_summary, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // Summary TextViews
            TextView tvFingerlings = dialogView.findViewById(R.id.value_fingerlings);
            TextView tvFeed = dialogView.findViewById(R.id.value_feed);
            TextView tvMaintenance = dialogView.findViewById(R.id.value_maintenance);
            TextView tvSalary = dialogView.findViewById(R.id.value_salary);
            TextView tvCapital = dialogView.findViewById(R.id.value_total_capital);

            // Breakdown Layouts
            LinearLayout layoutFeederBreakdown = dialogView.findViewById(R.id.layoutFeederBreakdown);
            LinearLayout layoutMaintenanceBreakdown = dialogView.findViewById(R.id.layoutMaintenanceBreakdown);
            LinearLayout layoutSalaryBreakdown = dialogView.findViewById(R.id.layoutSalaryBreakdown);

            // Source Fields
            TextView tvBreed = view.findViewById(R.id.fishbreedpcostdisplay);
            TextView tvAmountPerPiece = view.findViewById(R.id.amtperpiece);
            TextView tvNumberOfFish = view.findViewById(R.id.numoffingerlings);
            EditText tvFeederCost = view.findViewById(R.id.amtoffeeders);
            EditText tvMaintenanceCost = view.findViewById(R.id.initialMaintenanceCost);
            EditText tvSalaryCost = view.findViewById(R.id.initialSalaryCost);

            Button btnfeedertype = view.findViewById(R.id.typeoffeeders);
            Button btnmaintenancetype = view.findViewById(R.id.initialMaintenanceType);
            Button btnsalarydate = view.findViewById(R.id.initialSalaryDate);

            double amountPerPiece = parseDoubleOrZero(tvAmountPerPiece.getText().toString());
            double numberOfFish = parseDoubleOrZero(tvNumberOfFish.getText().toString());
            double fingerlingsCost = amountPerPiece * numberOfFish;

            double feedCost = parseDoubleOrZero(tvFeederCost.getText().toString());
            double maintenanceCost = parseDoubleOrZero(tvMaintenanceCost.getText().toString());
            double salaryCost = parseDoubleOrZero(tvSalaryCost.getText().toString());

            // Initial breakdowns
            addBreakdownRow(layoutFeederBreakdown, btnfeedertype.getText().toString(), String.format("₱%.2f", feedCost));
            addBreakdownRow(layoutMaintenanceBreakdown, btnmaintenancetype.getText().toString(), String.format("₱%.2f", maintenanceCost));
            addBreakdownRow(layoutSalaryBreakdown, btnsalarydate.getText().toString(), String.format("₱%.2f", salaryCost));

            // Dynamic Feeders
            double dynamicFeederTotal = 0;
            for (int i = 0; i < feederslist.getChildCount(); i++) {
                View row = feederslist.getChildAt(i);
                Button type = row.findViewById(R.id.typeoffeeders);
                EditText cost = row.findViewById(R.id.amtoffeeders);
                if (type != null && cost != null && !cost.getText().toString().isEmpty()) {
                    double value = parseDoubleOrZero(cost.getText().toString());
                    dynamicFeederTotal += value;
                    addBreakdownRow(layoutFeederBreakdown, type.getText().toString(), String.format("₱%.2f", value));
                }
            }

            // Dynamic Maintenance
            double dynamicMaintenanceTotal = 0;
            for (int i = 0; i < maintenancelist.getChildCount(); i++) {
                View row = maintenancelist.getChildAt(i);
                Button type = row.findViewById(R.id.initialMaintenanceType);
                EditText cost = row.findViewById(R.id.initialMaintenanceCost);
                if (type != null && cost != null && !cost.getText().toString().isEmpty()) {
                    double value = parseDoubleOrZero(cost.getText().toString());
                    dynamicMaintenanceTotal += value;
                    addBreakdownRow(layoutMaintenanceBreakdown, type.getText().toString(), String.format("₱%.2f", value));
                }
            }

            // Dynamic Salaries
            double dynamicSalaryTotal = 0;
            for (int i = 0; i < salarylist.getChildCount(); i++) {
                View row = salarylist.getChildAt(i);
                Button type = row.findViewById(R.id.initialSalaryDate);
                EditText cost = row.findViewById(R.id.initialSalaryCost);
                if (type != null && cost != null && !cost.getText().toString().isEmpty()) {
                    double value = parseDoubleOrZero(cost.getText().toString());
                    dynamicSalaryTotal += value;
                    addBreakdownRow(layoutSalaryBreakdown, type.getText().toString(), String.format("₱%.2f", value));
                }
            }

            double totalFeeder = feedCost + dynamicFeederTotal;
            double totalMaintenance = maintenanceCost + dynamicMaintenanceTotal;
            double totalSalary = salaryCost + dynamicSalaryTotal;
            double capital = fingerlingsCost + totalFeeder + totalMaintenance + totalSalary;

            tvFingerlings.setText(String.format("₱%.2f", fingerlingsCost));
            tvFeed.setText(String.format("₱%.2f", totalFeeder));
            tvMaintenance.setText(String.format("₱%.2f", totalMaintenance));
            tvSalary.setText(String.format("₱%.2f", totalSalary));
            tvCapital.setText(String.format("₱%.2f", capital));

            dialog.show();

            // ===== PDF GENERATION BUTTON INSIDE DIALOG =====
            Button btnGeneratePDF = dialogView.findViewById(R.id.btn_generate_pdf); // Add this button in dialog_summary.xml

            btnGeneratePDF.setOnClickListener(printView -> {
                View pdfContentView = inflater.inflate(R.layout.pdf_summary_layout, null);

                pdfContentView.measure(
                        View.MeasureSpec.makeMeasureSpec(595, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                pdfContentView.layout(0, 0, pdfContentView.getMeasuredWidth(), pdfContentView.getMeasuredHeight());

                Bitmap bitmap = Bitmap.createBitmap(
                        pdfContentView.getMeasuredWidth(),
                        pdfContentView.getMeasuredHeight(),
                        Bitmap.Config.ARGB_8888
                );
                Canvas canvasBitmap = new Canvas(bitmap);
                pdfContentView.draw(canvasBitmap);

                int pageWidth = 595;
                int pageHeight = 842;
                PdfDocument document = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float scale = 0.5f;
                float centerX = (pageWidth - (bitmap.getWidth() * scale)) / 2;
                float centerY = (pageHeight - (bitmap.getHeight() * scale)) / 2;
                canvas.save();
                canvas.translate(centerX, centerY);
                canvas.scale(scale, scale);
                canvas.drawBitmap(bitmap, 0, 0, null);
                canvas.restore();

                document.finishPage(page);

                String fileName = "Summary_" + System.currentTimeMillis() + ".pdf";
                File pdfFile = new File(requireContext().getExternalFilesDir(null), fileName);
                try {
                    FileOutputStream fos = new FileOutputStream(pdfFile);
                    document.writeTo(fos);
                    document.close();
                    fos.close();
                    Toast.makeText(getContext(), "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private final TextWatcher capitalWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            calculateTotalCapital();
        }
    };
    private void setupFeederTypeButton(Button btnfeedertype, EditText amountField) {
        String[] options = {"Starter", "Grower", "Finisher", "Others"};

        btnfeedertype.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Select Feed Type");
            builder.setItems(options, (dialog, which) -> {
                String selected = options[which];
                if (selected.equals("Others")) {
                    // Prompt user to enter custom feed type
                    EditText input = new EditText(requireContext());
                    input.setHint("Enter custom type");

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Custom Feed Type")
                            .setView(input)
                            .setPositiveButton("OK", (d, w) -> {
                                String customType = input.getText().toString().trim();
                                if (!customType.isEmpty()) {
                                    btnfeedertype.setText(customType);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    btnfeedertype.setText(selected);
                }
            });
            builder.show();
        });
    }

    private void setupMaintenanceTypeButton(Button btnmaintenancetype, EditText costField  ) {
        String[] options = {
                "Water Change", "Water Monitoring", "Waste Removal", "Algae Control",
                "Cleaning Ponds & Filters", "Leak Repair", "Inspection",
                "Pump & Pipe Maintenance", "Parasite Treatment", "Net or Screen Repair", "Others"
        };

        btnmaintenancetype.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Select Maintenance Type");
            builder.setItems(options, (dialog, which) -> {
                String selected = options[which];
                if (selected.equals("Others")) {
                    // Show input dialog
                    EditText input = new EditText(requireContext());
                    input.setHint("Enter custom type");
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Custom Maintenance Type")
                            .setView(input)
                            .setPositiveButton("OK", (d, w) -> {
                                String custom = input.getText().toString().trim();
                                if (!custom.isEmpty()) {
                                    btnmaintenancetype.setText(custom);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    btnmaintenancetype.setText(selected);
                }
            });
            builder.show();
        });
    }

    private void setupSalaryDateButton(Button btnSalaryDate) {
        btnSalaryDate.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String dateString = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                        btnSalaryDate.setText(dateString);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }

    private void calculateTotalCapital() {
        double capital = 0.0;
        // Fish cost from TextView
        try {
            capital += Double.parseDouble(tvfishcost.getText().toString());
        } catch (NumberFormatException ignored) {}
        // Initial feeder cost
        try {
            capital += Double.parseDouble(etfeedercost.getText().toString());
        } catch (NumberFormatException ignored) {}
        // Initial maintenance cost
        try {
            capital += Double.parseDouble(etmaintenancecost.getText().toString());
        } catch (NumberFormatException ignored) {}
        // Initial salary cost
        try {
            capital += Double.parseDouble(etsalarycost.getText().toString());
        } catch (NumberFormatException ignored) {}
        // Dynamic feeder rows
        for (int i = 0; i < feederslist.getChildCount(); i++) {
            View row = feederslist.getChildAt(i);
            EditText dynamicCost = row.findViewById(R.id.amtoffeeders);
            try {
                capital += Double.parseDouble(dynamicCost.getText().toString());
            } catch (NumberFormatException ignored) {}
        }
        // Dynamic maintenance rows
        for (int i = 0; i < maintenancelist.getChildCount(); i++) {
            View row = maintenancelist.getChildAt(i);
            EditText dynamicCost = row.findViewById(R.id.initialMaintenanceCost);
            try {
                capital += Double.parseDouble(dynamicCost.getText().toString());
            } catch (NumberFormatException ignored) {}
        }

        for (int i = 0; i < salarylist.getChildCount(); i++) {
            View row = salarylist.getChildAt(i);
            EditText dynamicCost = row.findViewById(R.id.initialSalaryCost);
            try {
                capital += Double.parseDouble(dynamicCost.getText().toString());
            } catch (NumberFormatException ignored) {}
        }
        // Set calculated capital to TextView
        tvcapital.setText(String.format("%.2f", capital));
        btnviewsummary.setEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);

        String breed = sharedPreferences.getString("fish_breed", "");
        String amount = sharedPreferences.getString("fish_amount", "0");
        String number = sharedPreferences.getString("number_fish", "0");

        tvfishbreed.setText(breed);
        tvfishamount.setText(amount);
        tvnumberoffish.setText(number);

        try {
            double amountPerPiece = Double.parseDouble(amount);
            int numberOfFish = Integer.parseInt(number);
            double totalCost = amountPerPiece * numberOfFish;

            tvfishcost.setText(String.format("%.2f", totalCost));
        } catch (NumberFormatException e) {
            tvfishcost.setText("0.00");
        }

        calculateTotalCapital();
    }

    private double parseDoubleOrZero(String value) {
        try {
            return Double.parseDouble(value.replace("₱", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void addBreakdownRow(LinearLayout container, String label, String value) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.row_breakdown, container, false);

        TextView tvLabel = row.findViewById(R.id.tvBreakdownLabel);
        TextView tvCost = row.findViewById(R.id.tvBreakdownCost);

        tvLabel.setText(label);
        tvCost.setText(value);

        container.addView(row);
    }



}
