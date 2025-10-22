package com.example.pondmatev1;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ResetPondDialogFragment extends DialogFragment {

    private Spinner spinnerBreed;
    private EditText etFishCount, etCostPerFish;
    private DatePicker dateStarted;
    private TextView tvDateHarvest;
    private Button btnSave;
    private String rawHarvestDateForDB = "";
    private PondModel pond;

    public interface OnPondResetListener {
        void onPondReset(PondModel updatedPond);
    }

    private OnPondResetListener listener;

    public void setPond(PondModel pond) {
        this.pond = pond;
    }

    public void setOnPondResetListener(OnPondResetListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_reset_pond, container, false);

        spinnerBreed = view.findViewById(R.id.spinnerBreed);
        etFishCount = view.findViewById(R.id.etFishCount);
        etCostPerFish = view.findViewById(R.id.etCostPerFish);
        dateStarted = view.findViewById(R.id.dateStarted);
        tvDateHarvest = view.findViewById(R.id.tvDateHarvest);
        btnSave = view.findViewById(R.id.btnSavePond);
        TextView closeDialog = view.findViewById(R.id.btnClose);

        // Populate spinner
        String[] fishBreeds = {"Tilapia", "Bangus"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, fishBreeds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBreed.setAdapter(adapter);

        // Prefill existing pond data
        if (pond != null) {
            if (pond.getBreed() != null) {
                int pos = adapter.getPosition(pond.getBreed());
                if (pos >= 0) spinnerBreed.setSelection(pos);
            }
            etFishCount.setText(String.valueOf(pond.getFishCount()));
            etCostPerFish.setText(String.valueOf(pond.getCostPerFish()));
        }

        closeDialog.setOnClickListener(v -> dismiss());

        // Date Picker logic (similar to AddPondDialogFragment)
        Calendar today = Calendar.getInstance();
        dateStarted.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH),
                (view1, year, monthOfYear, dayOfMonth) -> {
                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.set(year, monthOfYear, dayOfMonth);
                    Calendar harvestCalendar = (Calendar) startCalendar.clone();

                    String selectedBreed = spinnerBreed.getSelectedItem().toString();
                    if (selectedBreed.equals("Tilapia")) {
                        harvestCalendar.add(Calendar.DAY_OF_YEAR, 60);
                    } else if (selectedBreed.equals("Bangus")) {
                        harvestCalendar.add(Calendar.DAY_OF_YEAR, 90);
                    }


                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    rawHarvestDateForDB = dbFormat.format(harvestCalendar.getTime());

                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
                    tvDateHarvest.setText(displayFormat.format(harvestCalendar.getTime()));
                });

        btnSave.setOnClickListener(v -> {
            String breed = spinnerBreed.getSelectedItem().toString();
            String fishCountStr = etFishCount.getText().toString().trim();
            String costStr = etCostPerFish.getText().toString().trim();

            if (fishCountStr.isEmpty() || costStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                pond.setBreed(breed);
                pond.setFishCount(Integer.parseInt(fishCountStr));
                pond.setCostPerFish(Double.parseDouble(costStr));
                pond.setDateStarted(dateStarted.getYear() + "-" +
                        String.format("%02d", dateStarted.getMonth() + 1) + "-" +
                        String.format("%02d", dateStarted.getDayOfMonth()));
                pond.setDateHarvest(rawHarvestDateForDB);

                PondSyncManager.updatePondDetails(pond, new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(getContext(), "Pond details updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onPondReset(pond);
                        dismiss();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
