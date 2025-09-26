package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;

import java.util.Calendar;
import java.util.Locale;

public class ScheduleFeederDialog extends DialogFragment {

    private TextView timeoffeeding1, timeoffeeding2, timeoffeeding3;
    private Button btnselecttime1, btnselecttime2, btnselecttime3, btnSave;

    // Store selected times
    private Integer feeding1Minutes = null;
    private Integer feeding2Minutes = null;
    private Integer feeding3Minutes = null;

    private String formattedTime1 = "";
    private String formattedTime2 = "";
    private String formattedTime3 = "";


    private TextView tvsFishCount, feedQuantityView;

    private EditText weightInput;

    private int fishCount = 0;

    private Runnable onDismissListener;

    public void setOnDismissListener(Runnable listener) {
        this.onDismissListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) {
            onDismissListener.run();
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_schedule, null);



        tvsFishCount = view.findViewById(R.id.tvsfishcount);
        weightInput = view.findViewById(R.id.weight);
        feedQuantityView = view.findViewById(R.id.feedquantity);

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            fishCount = pond.getFishCount();

            // Show fish count
            tvsFishCount.setText(String.valueOf(fishCount));
        }

        timeoffeeding1 = view.findViewById(R.id.timeoffeeding1);
        timeoffeeding2 = view.findViewById(R.id.timeoffeeding2);
        timeoffeeding3 = view.findViewById(R.id.timeoffeeding3);

        btnselecttime1 = view.findViewById(R.id.btnselecttime1);
        btnselecttime2 = view.findViewById(R.id.btnselecttime2);
        btnselecttime3 = view.findViewById(R.id.btnselecttime3);

        btnselecttime1.setOnClickListener(v -> showTimePickerDialog(timeoffeeding1, 1));
        btnselecttime2.setOnClickListener(v -> showTimePickerDialog(timeoffeeding2, 2));
        btnselecttime3.setOnClickListener(v -> showTimePickerDialog(timeoffeeding3, 3));

        btnSave = view.findViewById(R.id.createbtn);

        weightInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                computeFeedQuantity();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });



        btnSave.setOnClickListener(v -> confirmAndSaveSchedule());


        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();

    }


    private void computeFeedQuantity() {
        String weightStr = weightInput.getText().toString().trim();

        if (weightStr.isEmpty() || fishCount == 0) {
            feedQuantityView.setText("--");
            return;
        }

        try {
            float fishWeight = Float.parseFloat(weightStr); // in grams
            float feedPercentage = 0.03f; // 3%
            float computedFeedQty = feedPercentage * fishWeight * fishCount; // grams
            float computedKg = computedFeedQty / 1000f; // convert to kg

            // ✅ Show both KG and Grams
            feedQuantityView.setText(
                    String.format(Locale.getDefault(), "%.2f kg (%.0f g)", computedKg, computedFeedQty)
            );

        } catch (NumberFormatException e) {
            feedQuantityView.setText("--");
        }
    }


    private void showTimePickerDialog(TextView targetTextView, int feedingNumber) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timepicker, null);
        TimePicker timePicker = dialogView.findViewById(R.id.timePickerSpinner);
        timePicker.setIs24HourView(false);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Feeding Time " + feedingNumber)
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();

                    // Convert to minutes of the day
                    int totalMinutes = hour * 60 + minute;

                    if (!isValidTime(feedingNumber, totalMinutes)) {
                        return; // Don’t save invalid time
                    }

                    String amPm = (hour >= 12) ? "PM" : "AM";
                    int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
                    String formatted12 = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);
                    String formatted24 = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);

                    targetTextView.setText(formatted12); // ✅ Show 12h format in UI

                    if (feedingNumber == 1) {
                        feeding1Minutes = totalMinutes;
                        formattedTime1 = formatted24; // ✅ Save 24h format for upload
                    }
                    if (feedingNumber == 2) {
                        feeding2Minutes = totalMinutes;
                        formattedTime2 = formatted24;
                    }
                    if (feedingNumber == 3) {
                        feeding3Minutes = totalMinutes;
                        formattedTime3 = formatted24;
                    }

                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void confirmAndSaveSchedule() {
        if (formattedTime1.isEmpty() || formattedTime2.isEmpty() || formattedTime3.isEmpty()) {
            Toast.makeText(getContext(), "Please set all feeding times", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Feeding Schedule")
                .setMessage("Do you really want to save and upload this feeding schedule?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences prefss = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                    String pondJson = prefss.getString("selected_pond", null);
                    String pondName = "";

                    if (pondJson != null) {
                        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                        pondName = pond.getName();
                    }

                    if (pondName.isEmpty()) {
                        Toast.makeText(getContext(), "No pond selected!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String weightStr = weightInput.getText().toString().trim();
                    if (weightStr.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter fish weight", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    float fishWeight = Float.parseFloat(weightStr); // grams
                    float feedPercentage = 0.03f; // 3%
                    float computedFeedQty = feedPercentage * fishWeight * fishCount; // grams
                    float computedKg = computedFeedQty / 1000f;
                    float finalFeedAmount = computedKg; // ✅ send in KG


                    PondSyncManager.uploadFeedingScheduleToServer(
                            pondName,
                            formattedTime1,
                            formattedTime2,
                            formattedTime3,
                            finalFeedAmount,
                            fishWeight,
                            new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object result) {
                                    if (isAdded()) { // ✅ Check if fragment is still attached
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), "Uploaded to server: " + result, Toast.LENGTH_SHORT).show());
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    if (isAdded()) { // ✅ Check before calling requireActivity()
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show());
                                    }
                                }
                            }
                    );



                    Toast.makeText(getContext(), "Schedule saved locally", Toast.LENGTH_SHORT).show();
                    ScheduleFeederDialog.this.dismiss();



                })
                .setNegativeButton("No", (dialog, which) ->
                        Toast.makeText(getContext(), "Schedule not saved", Toast.LENGTH_SHORT).show())
                .show();
    }

    private boolean isValidTime(int feedingNumber, int selectedMinutes) {
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        switch (feedingNumber) {
            case 1:
                if (selectedMinutes < currentMinutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 1 cannot be set in the past!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 2:
                if (feeding1Minutes == null || selectedMinutes <= feeding1Minutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 2 must be later than Feeding 1!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 3:
                if (feeding1Minutes == null || feeding2Minutes == null ||
                        selectedMinutes <= feeding1Minutes || selectedMinutes <= feeding2Minutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 3 must be later than Feeding 1 and 2!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
        }
        return true;
    }

}
