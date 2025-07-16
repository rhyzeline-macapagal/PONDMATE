package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProductionCostFragment extends Fragment {

    TextView tvfishbreed, tvfishamount, tvnumberoffish, tvfishcost, tvcapital;
    LinearLayout linlfeederscont, linlmaintenancecont, linlsalarycont, feederslist, maintenancelist, salarylist;
    Button btnedit, btngeneratereport, btnfeedertype, btnmaintenancetype,btnsalarydate;
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
        btngeneratereport = view.findViewById(R.id.generatereportpcostroi);

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

        btngeneratereport.setEnabled(false);

        setupFeederTypeButton(btnfeedertype, etfeedercost);
        setupMaintenanceTypeButton(btnmaintenancetype, etmaintenancecost);
        setupSalaryDateButton(btnsalarydate);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);

        String breed = sharedPreferences.getString("fish_breed", "");
        String amount = sharedPreferences.getString("fish_amount", "");
        String number = sharedPreferences.getString("number_fish", "");

        tvfishbreed.setText(breed);
        tvfishamount.setText(amount);
        tvnumberoffish.setText(number);

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
            });
            Button dynamicFeederTypeBtn = newRow.findViewById(R.id.typeoffeeders);
            EditText dynamicAmount = newRow.findViewById(R.id.amtoffeeders);

            dynamicAmount.setEnabled(true);
            dynamicAmount.setFocusableInTouchMode(true);
            dynamicAmount.setFocusable(true);
            dynamicAmount.setCursorVisible(true);

            setupFeederTypeButton(dynamicFeederTypeBtn, dynamicAmount);

            btnRemove.setOnClickListener(removeView -> feederslist.removeView(newRow));

            feederslist.addView(newRow);
        });

        btnaddmaintenancelist.setOnClickListener(v ->{
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View newRow = inflater.inflate(R.layout.row_maintenance, maintenancelist, false);

            Button dynamicType = newRow.findViewById(R.id.initialMaintenanceType);
            EditText dynamicCost = newRow.findViewById(R.id.initialMaintenanceCost);
            dynamicCost.setEnabled(true);
            dynamicCost.setFocusable(true);
            dynamicCost.setFocusableInTouchMode(true);
            dynamicCost.setCursorVisible(true);

            setupMaintenanceTypeButton(dynamicType, dynamicCost);

            ImageButton btnRemove = newRow.findViewById(R.id.removeMaintenanceButton);
            btnRemove.setOnClickListener(removeView ->{
                maintenancelist.removeView(newRow);
            });
            maintenancelist.addView(newRow);
        });

        btnaddsalarylist.setOnClickListener(v ->{
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View newRow = inflater.inflate(R.layout.row_salary, salarylist, false);

            ImageButton btnRemove = newRow.findViewById(R.id.removeSalaryButton);
            btnRemove.setOnClickListener(removeView ->{
                salarylist.removeView(newRow);
            });

            Button dynamicSalaryDateBtn = newRow.findViewById(R.id.initialSalaryDate);
            setupSalaryDateButton(dynamicSalaryDateBtn);

            EditText dynamicSalaryCost = newRow.findViewById(R.id.initialSalaryCost);
            dynamicSalaryCost.setEnabled(true);
            dynamicSalaryCost.setFocusableInTouchMode(true);

            salarylist.addView(newRow);
        });
    }
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


}
