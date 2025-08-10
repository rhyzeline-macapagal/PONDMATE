package com.example.pondmatev1;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddPondDialogFragment extends DialogFragment {

    private EditText etPondName, etFishCount, etCostPerFish;
    private Spinner spinnerBreed;
    private DatePicker dateStarted;
    private TextView tvDateHarvest;
    private Button btnSave;

    public interface OnPondAddedListener {
        void onPondAdded(PondModel pondModel);
    }

    private OnPondAddedListener listener;

    public void setOnPondAddedListener(OnPondAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_pond, container, false);

        etPondName = view.findViewById(R.id.etPondName);
        spinnerBreed = view.findViewById(R.id.spinnerBreed);
        etFishCount = view.findViewById(R.id.etFishCount);
        etCostPerFish = view.findViewById(R.id.etCostPerFish);
        dateStarted = view.findViewById(R.id.dateStarted);
        tvDateHarvest = view.findViewById(R.id.tvDateHarvest);
        btnSave = view.findViewById(R.id.btnSavePond);
        TextView closeDialog = view.findViewById(R.id.btnClose);

        // Populate spinner with fish breeds
        String[] fishBreeds = {"Tilapia", "Bangus", "Alimango"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, fishBreeds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBreed.setAdapter(adapter);

        closeDialog.setOnClickListener(v -> dismiss());

        Calendar today = Calendar.getInstance();
        dateStarted.init(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH),
                (view1, year, monthOfYear, dayOfMonth) -> {
                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.set(year, monthOfYear, dayOfMonth);

                    Calendar harvestCalendar = (Calendar) startCalendar.clone();
                    harvestCalendar.add(Calendar.DAY_OF_YEAR, 120);

                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String rawHarvestDate = dbFormat.format(harvestCalendar.getTime());

                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
                    String formattedDisplayDate = displayFormat.format(harvestCalendar.getTime());

                    tvDateHarvest.setText(formattedDisplayDate);
                });

        btnSave.setOnClickListener(v ->  {
            String name = etPondName.getText().toString().trim();
            String breed = spinnerBreed.getSelectedItem().toString();
            String fishCountStr = etFishCount.getText().toString().trim();
            String costStr = etCostPerFish.getText().toString().trim();

            if (name.isEmpty() || breed.isEmpty() || fishCountStr.isEmpty() || costStr.isEmpty()) {
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

            // ✅ Format dates first
            String dateStartedStr = dateStarted.getYear() + "-" +
                    String.format(Locale.getDefault(), "%02d", (dateStarted.getMonth() + 1)) + "-" +
                    String.format(Locale.getDefault(), "%02d", dateStarted.getDayOfMonth());

            String dateHarvestStr = tvDateHarvest.getText().toString().replace("Harvest Date: ", "");

            // ✅ Now save to SharedPreferences
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("fish_breed", breed);
            editor.putString("fish_amount", costStr);
            editor.putString("number_fish", fishCountStr);
            editor.putString("date_started", dateStartedStr);
            editor.apply();

            // Continue with saving pond
            PondModel pond = new PondModel(name, breed, fishCount, cost, dateStartedStr, dateHarvestStr, "DATA");

            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            dbHelper.insertPond(pond);
            PondSyncManager.uploadPondToServer(getContext(), pond);

            if (listener != null) listener.onPondAdded(pond);

            dismiss();
        });


        return view;
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

}
