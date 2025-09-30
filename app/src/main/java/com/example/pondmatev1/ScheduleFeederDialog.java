package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class ScheduleFeederDialog extends DialogFragment {

    private TextView timeoffeeding1, timeoffeeding2, timeoffeeding3;
    private Button btnselecttime1, btnselecttime2, btnselecttime3, btnSave;

    private Integer feeding1Minutes = null;
    private Integer feeding2Minutes = null;
    private Integer feeding3Minutes = null;

    private String formattedTime1 = "";
    private String formattedTime2 = "";
    private String formattedTime3 = "";

    private TextView tvsFishCount, feedQuantityView, tvFeedPrice;
    private EditText weightInput;
    private int fishCount = 0;
    private float feedPrice = 0.00F;

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
        tvFeedPrice = view.findViewById(R.id.feedprice);

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
        btnSave.setText("Update Schedule");

        weightInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { computeFeedQuantity(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnSave.setOnClickListener(v -> confirmAndUpdateSchedule());

        // 🔄 Fetch existing schedule when dialog opens
        fetchExistingSchedule();

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();
    }

    private void fetchExistingSchedule() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            String pondNameLocal = pond.getName();
            fishCount = pond.getFishCount();
            tvsFishCount.setText(String.valueOf(fishCount));

            PondSyncManager.fetchPondSchedule(pondNameLocal, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object result) {
                    String response = (String) result;

                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            JSONObject json = new JSONObject(response);

                            if ("success".equals(json.getString("status"))) {
                                JSONArray schedulesArray = json.optJSONArray("schedules");
                                if (schedulesArray != null && schedulesArray.length() > 0) {
                                    JSONObject scheduleObj = schedulesArray.getJSONObject(0);

                                    String schedOne = scheduleObj.optString("sched_one", "");
                                    String schedTwo = scheduleObj.optString("sched_two", "");
                                    String schedThree = scheduleObj.optString("sched_three", "");
                                    String feedQty = scheduleObj.optString("feed_amount", "0.0") + " kg";
                                    String feedPriceStr = "₱" + scheduleObj.optString("feed_price", "0.00");
                                    String fishWeight = scheduleObj.optString("fish_weight", "0.00");

                                    // ✅ Populate UI
                                    if (!schedOne.isEmpty()) {
                                        timeoffeeding1.setText(formatTime(schedOne));
                                        formattedTime1 = schedOne;
                                    }
                                    if (!schedTwo.isEmpty()) {
                                        timeoffeeding2.setText(formatTime(schedTwo));
                                        formattedTime2 = schedTwo;
                                    }
                                    if (!schedThree.isEmpty()) {
                                        timeoffeeding3.setText(formatTime(schedThree));
                                        formattedTime3 = schedThree;
                                    }

                                    feedQuantityView.setText(feedQty);
                                    tvFeedPrice.setText(feedPriceStr);
                                    weightInput.setText(fishWeight);

                                    try {
                                        feedPrice = Float.parseFloat(scheduleObj.optString("feed_price", "0.00"));
                                    } catch (Exception ignored) {}
                                }
                            } else {
                                Toast.makeText(getContext(), "Error: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Failed to fetch schedule: " + error, Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }
    }


    private void computeFeedQuantity() {
        String weightStr = weightInput.getText().toString().trim();

        if (weightStr.isEmpty() || fishCount == 0) {
            feedQuantityView.setText("--");
            return;
        }

        try {
            float fishWeight = Float.parseFloat(weightStr);
            float feedPercentage = 0.03f;
            float computedFeedQty = feedPercentage * fishWeight * fishCount;
            float computedKg = computedFeedQty / 1000f;

            feedQuantityView.setText(
                    String.format(Locale.getDefault(), "%.2f kg", computedKg, computedFeedQty)
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
                    int totalMinutes = hour * 60 + minute;

                    if (!isValidTime(feedingNumber, totalMinutes)) return;

                    String amPm = (hour >= 12) ? "PM" : "AM";
                    int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;

                    String formatted12 = String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm);
                    String formatted24 = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);

                    targetTextView.setText(formatted12);

                    if (feedingNumber == 1) { feeding1Minutes = totalMinutes; formattedTime1 = formatted24; }
                    if (feedingNumber == 2) { feeding2Minutes = totalMinutes; formattedTime2 = formatted24; }
                    if (feedingNumber == 3) { feeding3Minutes = totalMinutes; formattedTime3 = formatted24; }
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }


    private void confirmAndUpdateSchedule() {
        if (formattedTime1.isEmpty() || formattedTime2.isEmpty() || formattedTime3.isEmpty()) {
            Toast.makeText(getContext(), "Please set all feeding times", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔒 Validate order of times (must be 1 < 2 < 3)
        try {
            String[] parts1 = formattedTime1.split(":");
            String[] parts2 = formattedTime2.split(":");
            String[] parts3 = formattedTime3.split(":");

            int minutes1 = Integer.parseInt(parts1[0]) * 60 + Integer.parseInt(parts1[1]);
            int minutes2 = Integer.parseInt(parts2[0]) * 60 + Integer.parseInt(parts2[1]);
            int minutes3 = Integer.parseInt(parts3[0]) * 60 + Integer.parseInt(parts3[1]);

            if (minutes1 >= minutes2) {
                Toast.makeText(getContext(), "Feeding 1 must be earlier than Feeding 2!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (minutes2 >= minutes3) {
                Toast.makeText(getContext(), "Feeding 2 must be earlier than Feeding 3!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (minutes3 <= minutes1 || minutes3 <= minutes2) {
                Toast.makeText(getContext(), "Feeding 3 must be later than Feeding 1 and 2!", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error validating feeding times", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Update Feeding Schedule")
                .setMessage("Do you want to overwrite the existing feeding schedule?")
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

                    float fishWeight = Float.parseFloat(weightStr);
                    float feedPercentage = 0.03f;
                    float computedFeedQty = feedPercentage * fishWeight * fishCount;
                    float computedKg = computedFeedQty / 1000f;
                    float finalFeedAmount = computedKg;

                    PondSyncManager.updateFeedingScheduleOnServer(
                            pondName, formattedTime1, formattedTime2, formattedTime3,
                            finalFeedAmount, fishWeight, feedPrice,
                            new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object result) {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Schedule updated successfully!", Toast.LENGTH_SHORT).show();
                                            ScheduleFeederDialog.this.dismiss();
                                        });
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), "Update failed: " + error, Toast.LENGTH_SHORT).show());
                                    }
                                }
                            }
                    );
                })
                .setNegativeButton("No", (dialog1, which) -> dialog1.dismiss())
                .show();
    }

    private boolean isValidTime(int feedingNumber, int selectedMinutes) {
        switch (feedingNumber) {
            case 1:
                if ((feeding2Minutes != null && selectedMinutes >= feeding2Minutes) ||
                        (feeding3Minutes != null && selectedMinutes >= feeding3Minutes)) {
                    Toast.makeText(requireContext(), "Feeding 1 must be earlier than Feeding 2 and 3!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 2:
                if (feeding1Minutes == null || selectedMinutes <= feeding1Minutes) {
                    Toast.makeText(requireContext(), "Feeding 2 must be later than Feeding 1!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (feeding3Minutes != null && selectedMinutes >= feeding3Minutes) {
                    Toast.makeText(requireContext(), "Feeding 2 must be earlier than Feeding 3!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 3:
                if (feeding1Minutes == null || feeding2Minutes == null ||
                        selectedMinutes <= feeding1Minutes || selectedMinutes <= feeding2Minutes) {
                    Toast.makeText(requireContext(), "Feeding 3 must be later than Feeding 1 and 2!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
        }
        return true;
    }


    private String formatTime(String time24) {
        if (time24 == null || time24.isEmpty()) return "";
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String amPm = (hour >= 12) ? "PM" : "AM";
            int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
            return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);
        } catch (Exception e) {
            return time24;
        }
    }
}
