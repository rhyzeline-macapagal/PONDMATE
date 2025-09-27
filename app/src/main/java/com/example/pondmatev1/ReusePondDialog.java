package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReusePondDialog extends DialogFragment {

    private TextView tvPondName, tvDateHarvest, dateStarted, feedQuantityView;
    private EditText etFishCount, etCostPerFish, weightInput;
    private Spinner spinnerBreed;
    private Button btnSave;

    private String rawDateForDB = "";
    private String rawHarvestDateForDB = "";

    // Feeding times
    private final int DEFAULT_FEEDING1 = 7 * 60;
    private final int DEFAULT_FEEDING2 = 9 * 60;
    private final int DEFAULT_FEEDING3 = 15 * 60;

    private TextView timeoffeeding1, timeoffeeding2, timeoffeeding3;
    private String formattedTime1 = "", formattedTime2 = "", formattedTime3 = "";

    private PondModel existingPond;

    public void setPond(PondModel pond) {
        this.existingPond = pond;
    }

    public interface OnPondReusedListener {
        void onPondReused(PondModel updatedPond);
    }

    private OnPondReusedListener listener;

    public void setOnPondReusedListener(OnPondReusedListener listener) {
        this.listener = listener;
    }

    private final java.util.Map<String, Double> breedCosts = new java.util.HashMap<String, Double>() {{
        put("Tilapia", 1.50);
        put("Bangus", 2.50);
        put("Alimango", 3.0);
    }};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reusepond_dialog, container, false);

        tvPondName = view.findViewById(R.id.PName);
        spinnerBreed = view.findViewById(R.id.spinnerBreed);
        etFishCount = view.findViewById(R.id.etFishCount);
        etCostPerFish = view.findViewById(R.id.etCostPerFish);
        dateStarted = view.findViewById(R.id.tvDateStarted);
        tvDateHarvest = view.findViewById(R.id.tvDateHarvest);
        btnSave = view.findViewById(R.id.btnSavePond);
        TextView closeDialog = view.findViewById(R.id.btnClose);
        feedQuantityView = view.findViewById(R.id.tvFeedQuantity);
        weightInput = view.findViewById(R.id.etFishWeight);

        timeoffeeding1 = view.findViewById(R.id.timeoffeeding1);
        timeoffeeding2 = view.findViewById(R.id.timeoffeeding2);
        timeoffeeding3 = view.findViewById(R.id.timeoffeeding3);

        setDefaultFeedingTimes();

        if (existingPond != null) {
            tvPondName.setText(existingPond.getName());
        }

        etFishCount.addTextChangedListener(new SimpleTextWatcher(this::computeFeedQuantity));
        weightInput.addTextChangedListener(new SimpleTextWatcher(this::computeFeedQuantity));

        view.findViewById(R.id.btnselecttime1).setOnClickListener(v -> showTimePickerDialog(timeoffeeding1, 1));
        view.findViewById(R.id.btnselecttime2).setOnClickListener(v -> showTimePickerDialog(timeoffeeding2, 2));
        view.findViewById(R.id.btnselecttime3).setOnClickListener(v -> showTimePickerDialog(timeoffeeding3, 3));

        // Populate spinner
        String[] fishBreeds = {"Tilapia", "Bangus", "Alimango"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, fishBreeds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBreed.setAdapter(adapter);

        closeDialog.setOnClickListener(v -> dismiss());

        // Set today as date started
        Calendar today = Calendar.getInstance();
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        dateStarted.setText(displayFormat.format(today.getTime()));
        rawDateForDB = dbFormat.format(today.getTime());

        Runnable updateHarvestDate = () -> {
            String selectedBreed = spinnerBreed.getSelectedItem().toString();
            Calendar harvestCalendar = (Calendar) today.clone();
            switch (selectedBreed) {
                case "Tilapia": harvestCalendar.add(Calendar.DAY_OF_YEAR, 60); break;
                case "Bangus": harvestCalendar.add(Calendar.DAY_OF_YEAR, 90); break;
                case "Alimango": harvestCalendar.add(Calendar.DAY_OF_YEAR, 270); break;
            }
            rawHarvestDateForDB = dbFormat.format(harvestCalendar.getTime());
            tvDateHarvest.setText(displayFormat.format(harvestCalendar.getTime()));
        };

        spinnerBreed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                updateHarvestDate.run();
                if (breedCosts.containsKey(spinnerBreed.getSelectedItem().toString())) {
                    etCostPerFish.setText("₱" + breedCosts.get(spinnerBreed.getSelectedItem().toString()));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(v -> savePond());

        return view;
    }

    private void savePond() {
        String breed = spinnerBreed.getSelectedItem().toString();
        String fishCountStr = etFishCount.getText().toString().trim();
        String costStr = etCostPerFish.getText().toString().trim().replace("₱", "");

        if (breed.isEmpty() || fishCountStr.isEmpty() || costStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int fishCount;
        double cost;
        try {
            fishCount = Integer.parseInt(fishCountStr);
            cost = Double.parseDouble(costStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Save")
                .setMessage("Do you want to save this pond?")
                .setPositiveButton("Yes", (d, w) -> {
                    PondModel updatedPond = new PondModel(
                            existingPond.getId(),
                            existingPond.getName(),
                            breed,
                            fishCount,
                            cost,
                            rawDateForDB,
                            rawHarvestDateForDB,
                            existingPond.getImagePath(),
                            0f, 0f
                    );
                    updatedPond.setPdfPath(existingPond.getPdfPath());

                    // Just call listener; adapter handles all UI feedback & PDF/history
                    if (listener != null) listener.onPondReused(updatedPond);
                    dismiss();
                })
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void computeFeedQuantity() {
        String weightStr = weightInput.getText().toString().trim();
        String fishCountStr = etFishCount.getText().toString().trim();

        if (weightStr.isEmpty() || fishCountStr.isEmpty()) {
            feedQuantityView.setText("--");
            return;
        }

        try {
            int fishCount = Integer.parseInt(fishCountStr);
            float fishWeight = Float.parseFloat(weightStr);
            float feedPercentage = 0.03f;
            float totalFeed = feedPercentage * fishWeight * fishCount;
            float totalKg = totalFeed / 1000f;

            feedQuantityView.setText(String.format(Locale.getDefault(),
                    "%.2f kg (%.0f g)", totalKg, totalFeed));
        } catch (NumberFormatException e) {
            feedQuantityView.setText("--");
        }
    }

    private void showTimePickerDialog(TextView targetTextView, int feedingNumber) {
        TimePickerDialog picker = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                    int hour12 = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
                    targetTextView.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm));

                    String formatted24 = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    if (feedingNumber == 1) formattedTime1 = formatted24;
                    else if (feedingNumber == 2) formattedTime2 = formatted24;
                    else if (feedingNumber == 3) formattedTime3 = formatted24;
                }, 7, 0, false);
        picker.show();
    }

    private void setDefaultFeedingTimes() {
        timeoffeeding1.setText(formatMinutesTo12h(DEFAULT_FEEDING1));
        timeoffeeding2.setText(formatMinutesTo12h(DEFAULT_FEEDING2));
        timeoffeeding3.setText(formatMinutesTo12h(DEFAULT_FEEDING3));

        formattedTime1 = formatMinutesTo24h(DEFAULT_FEEDING1);
        formattedTime2 = formatMinutesTo24h(DEFAULT_FEEDING2);
        formattedTime3 = formatMinutesTo24h(DEFAULT_FEEDING3);
    }

    private String formatMinutesTo12h(int totalMinutes) {
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        String amPm = (hour >= 12) ? "PM" : "AM";
        int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);
    }

    private String formatMinutesTo24h(int totalMinutes) {
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable afterTextChanged;
        SimpleTextWatcher(Runnable afterTextChanged) { this.afterTextChanged = afterTextChanged; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { afterTextChanged.run(); }
    }
}
