package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SamplingDialog extends DialogFragment {

    private Button btnSelectTime1, btnSelectTime2;
    private TextView tvPondName, tvNextSampling, tvDaysOfCulture, tvLifeStage, tvTotalStocks, tvMortality, etSurvivalRate, tvDFRPerCycle;
    private TextView tvTimeFeeding1, tvTimeFeeding2, tvABWResult, tvDFRResult;
    private EditText etSampledWeight, etNumSamples, etFeedingRate;
    private int time1Minutes = -1, time2Minutes = -1;
    private String formattedTime1, formattedTime2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_sampling, container, false);

        // Close button
        TextView btnClose = view.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        // Initialize TextViews
        tvPondName = view.findViewById(R.id.tvPondName);
        tvNextSampling = view.findViewById(R.id.tvNextSampling);
        tvDFRPerCycle =  view.findViewById(R.id.tvDFRPerCycle);
        tvDaysOfCulture = view.findViewById(R.id.tvDaysOfCulture);
        tvLifeStage = view.findViewById(R.id.tvLifeStage);
        tvTotalStocks = view.findViewById(R.id.tvTotalStocks);
        tvMortality = view.findViewById(R.id.tvMortality);
        tvTimeFeeding1 = view.findViewById(R.id.timeoffeeding1);
        tvTimeFeeding2 = view.findViewById(R.id.timeoffeeding2);
        tvABWResult = view.findViewById(R.id.tvABWResult);
        tvDFRResult = view.findViewById(R.id.tvDFRResult);

        // Initialize EditTexts
        etSampledWeight = view.findViewById(R.id.etSampledWeight);
        etNumSamples = view.findViewById(R.id.etNumSamples);
        etFeedingRate = view.findViewById(R.id.etFeedingRate);
        etSurvivalRate = view.findViewById(R.id.etSurvivalRate);

        btnSelectTime1 = view.findViewById(R.id.btnselecttime1);
        btnSelectTime2 = view.findViewById(R.id.btnselecttime2);

        loadPondData();
        setDefaultFeedingTimes();

        Button btnSave = view.findViewById(R.id.btnSaveSampling);
        btnSave.setOnClickListener(v -> validateAndSave());


        btnSelectTime1.setOnClickListener(v -> showTimePickerDialog(tvTimeFeeding1, 1));
        btnSelectTime2.setOnClickListener(v -> showTimePickerDialog(tvTimeFeeding2, 2));

        // ✅ Add live computation watchers
        addTextWatchers();

        return view;
    }

    // 🐟 Load pond info
    // 🐟 Load pond info
    private void loadPondData() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            if (pond != null) {
                tvPondName.setText(pond.getName());

                // Compute next sampling date (15 days from today)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 15);
                tvNextSampling.setText("Next Sampling: " + sdf.format(calendar.getTime()));


                // Compute days of culture
                // 🧮 Compute days of culture and life stage based on species
                if (pond.getDateStocking() != null && !pond.getDateStocking().isEmpty()) {
                    try {
                        Date stockingDate = sdf.parse(pond.getDateStocking());
                        long days = (new Date().getTime() - stockingDate.getTime()) / (1000 * 60 * 60 * 24);
                        if (days < 1) days = 1; // ensure starts at Day 1

                        tvDaysOfCulture.setText(days + " days");

                        String species = pond.getBreed() != null ? pond.getBreed().toLowerCase() : "";

                        String lifeStage;

                        if (species.contains("tilapia")) {
                            if (days <= 60) lifeStage = "Fingerling";
                            else if (days <= 90) lifeStage = "Juvenile";
                            else if (days <= 180) lifeStage = "Sub-adult / Adult (Harvest)";
                            else lifeStage = "Post-harvest";
                        }
                        else if (species.contains("milkfish") || species.contains("bangus")) {
                            if (days <= 70) lifeStage = "Fingerling";
                            else if (days <= 90) lifeStage = "Juvenile";
                            else if (days <= 180) lifeStage = "Sub-adult / Adult (Harvest)";
                            else lifeStage = "Post-harvest";
                        }
                        else {
                            lifeStage = "Unknown Species";
                        }

                        tvLifeStage.setText(lifeStage);

                    } catch (ParseException e) {
                        tvDaysOfCulture.setText("N/A");
                        tvLifeStage.setText("N/A");
                    }
                }

                tvTotalStocks.setText(String.valueOf(pond.getFishCount()));
                tvMortality.setText(pond.getMortalityRate() + "%");

                // 🧮 Compute Survival Rate = 100 - Mortality
                double mortality = pond.getMortalityRate();
                double survivalRate = 100 - mortality;
                etSurvivalRate.setText(String.format(Locale.getDefault(), "%.2f%%", survivalRate));
            }
        }
    }

    // ✅ Validate fields before saving
    private void validateAndSave() {
        String sampledWeight = etSampledWeight.getText().toString().trim();
        String numSamples = etNumSamples.getText().toString().trim();
        String feedingRates = etFeedingRate.getText().toString().trim();
        String survivalRates = etSurvivalRate.getText().toString().trim();
        String feedingTime1 = tvTimeFeeding1.getText().toString().trim();
        String feedingTime2 = tvTimeFeeding2.getText().toString().trim();

        if (sampledWeight.isEmpty() || numSamples.isEmpty() ||
                feedingRates.isEmpty() || survivalRates.isEmpty() ||
                feedingTime1.isEmpty() || feedingTime2.isEmpty()) {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Incomplete Fields")
                    .setMessage("Please complete all required fields before saving.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        // Additional numeric validation
        try {
            double sw = Double.parseDouble(sampledWeight);
            double ns = Double.parseDouble(numSamples);
            double fr = Double.parseDouble(feedingRates);

            if (sw <= 0 || ns <= 0 || fr <= 0) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Invalid Input")
                        .setMessage("Values must be greater than zero.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
                return;
            }
        } catch (NumberFormatException e) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Invalid Format")
                    .setMessage("Please enter valid numeric values.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Save")
                .setMessage("Do you want to save this sampling record?")
                .setPositiveButton("Save", (dialog, which) -> {
                    SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                    String pondJson = prefs.getString("selected_pond", null);
                    PondModel pond = new Gson().fromJson(pondJson, PondModel.class);

                    String pondId = pond.getId();
                    int daysOfCulture = Integer.parseInt(tvDaysOfCulture.getText().toString().replace(" days", ""));
                    String growthStage = tvLifeStage.getText().toString();
                    int totalStocks = pond.getFishCount();
                    double mortalityRate = pond.getMortalityRate();

                    double abw = parseDouble(tvABWResult.getText().toString().replace(" g", ""));
                    double feedingRate = parseDouble(etFeedingRate.getText().toString());
                    double survivalRate = parseDouble(etSurvivalRate.getText().toString().replace("%", ""));
                    double dfr = parseDouble(tvDFRResult.getText().toString().replace(" g", ""));
                    double dfrFeed = parseDouble(tvDFRPerCycle.getText().toString().replace(" g", ""));

                    String feedingOne = formattedTime1; // use 24-hour version
                    String feedingTwo = formattedTime2;

                    String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    String nextSamplingDate = tvNextSampling.getText().toString()
                            .replace("Next Sampling: ", "")
                            .trim();

                    PondSyncManager.uploadSamplingRecord(
                            pond.getId(), daysOfCulture, growthStage, totalStocks, mortalityRate,
                            feedingOne, feedingTwo, abw, feedingRate, survivalRate, dfr, dfrFeed,
                            now, now, nextSamplingDate,
                            new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object response) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Sampling record saved!", Toast.LENGTH_SHORT).show();
                                        if (getParentFragment() instanceof ProductionCostFragment) {
                                            ((ProductionCostFragment) getParentFragment()).updateSamplingButtonState(pond.getId());
                                        }
                                        dismiss();
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
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
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

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Feeding Time " + timeNumber)
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    String amPm = (hour >= 12) ? "PM" : "AM";
                    int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
                    String formatted12 = String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm);
                    targetTextView.setText(formatted12);

                    // 🕒 Save 24-hour format for database
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


    // ✅ Add watchers for live computation
    private void addTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { computeValues(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        etSampledWeight.addTextChangedListener(watcher);
        etNumSamples.addTextChangedListener(watcher);
        etFeedingRate.addTextChangedListener(watcher);
        etSurvivalRate.addTextChangedListener(watcher);
    }

    // 🧠 Compute ABW and DFR
    private void computeValues() {
        try {
            double sampledWeight = parseDouble(etSampledWeight.getText().toString());
            double numSamples = parseDouble(etNumSamples.getText().toString());
            double feedingRate = parseDouble(etFeedingRate.getText().toString());

            // Extract numeric survival rate from text (e.g. "90.00%")
            String survivalText = etSurvivalRate.getText().toString().replace("%", "").trim();
            double survivalRate = parseDouble(survivalText);

            // 🧮 Compute ABW (Average Body Weight)
            double abw = 0;
            if (numSamples > 0) {
                abw = sampledWeight / numSamples;
            }

            // Prevent NaN or Infinity
            if (Double.isNaN(abw) || Double.isInfinite(abw)) abw = 0;

            tvABWResult.setText(String.format(Locale.getDefault(), "%.2f g", abw));

            // Convert % to decimal
            feedingRate /= 100.0;
            survivalRate /= 100.0;

            // 🧮 Compute DFR (Daily Feed Requirement)
            double totalStocks = parseDouble(tvTotalStocks.getText().toString());
            double dfr = feedingRate * survivalRate * abw * totalStocks;

            // Prevent NaN or Infinity
            if (Double.isNaN(dfr) || Double.isInfinite(dfr)) dfr = 0;

            tvDFRResult.setText(String.format(Locale.getDefault(), "%.2f g", dfr));

            // 🧮 Compute DFR per cycle (half of total DFR)
            double dfrPerCycle = dfr / 2;
            if (Double.isNaN(dfrPerCycle) || Double.isInfinite(dfrPerCycle)) dfrPerCycle = 0;

            tvDFRPerCycle.setText(String.format(Locale.getDefault(), "%.2f g", dfrPerCycle));

        } catch (Exception e) {
            tvABWResult.setText("0 g");
            tvDFRResult.setText("0 g");

            tvDFRPerCycle.setText("0 g");
        }
    }






    private double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0.0; }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
