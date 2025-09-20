package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class ScheduleFeederDialog extends DialogFragment {

    private TextView timeoffeeding1, timeoffeeding2, timeoffeeding3;
    private Button btnselecttime1, btnselecttime2, btnselecttime3;

    // Store selected times
    private Integer feeding1Minutes = null;
    private Integer feeding2Minutes = null;
    private Integer feeding3Minutes = null;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_schedule, null);

        timeoffeeding1 = view.findViewById(R.id.timeoffeeding3);
        timeoffeeding2 = view.findViewById(R.id.timeoffeeding1);
        timeoffeeding3 = view.findViewById(R.id.timeoffeeding2);

        btnselecttime1 = view.findViewById(R.id.btnselecttimes);
        btnselecttime2 = view.findViewById(R.id.btnselecttime1);
        btnselecttime3 = view.findViewById(R.id.btnselecttime2);

        btnselecttime1.setOnClickListener(v -> showTimePickerDialog(timeoffeeding1, 1));
        btnselecttime2.setOnClickListener(v -> showTimePickerDialog(timeoffeeding2, 2));
        btnselecttime3.setOnClickListener(v -> showTimePickerDialog(timeoffeeding3, 3));

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();
    }

    private void showTimePickerDialog(TextView targetTextView, int feedingNumber) {
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_timepicker, null);
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


                    // Save selected time
                    if (feedingNumber == 1) feeding1Minutes = totalMinutes;
                    if (feedingNumber == 2) feeding2Minutes = totalMinutes;
                    if (feedingNumber == 3) feeding3Minutes = totalMinutes;

                    // Format 12-hour time
                    String amPm = (hour >= 12) ? "PM" : "AM";
                    int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
                    String formatted = String.format("%02d:%02d %s", hour12, minute, amPm);
                    targetTextView.setText(formatted);

                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }



    private boolean isValidTime(int feedingNumber, int selectedMinutes) {
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        switch (feedingNumber) {
            case 1:
                // Feeding 1 must not be in the past
                if (selectedMinutes < currentMinutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 1 cannot be set in the past!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;

            case 2:
                if (feeding1Minutes == null) {
                    Toast.makeText(requireContext(),
                            "Please set Feeding 1 first!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (selectedMinutes <= feeding1Minutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 2 must be later than Feeding 1!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;

            case 3:
                if (feeding1Minutes == null || feeding2Minutes == null) {
                    Toast.makeText(requireContext(),
                            "Please set Feeding 1 and 2 first!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (selectedMinutes <= feeding1Minutes || selectedMinutes <= feeding2Minutes) {
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