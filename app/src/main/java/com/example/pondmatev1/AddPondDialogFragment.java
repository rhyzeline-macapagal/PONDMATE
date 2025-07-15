package com.example.pondmatev1;

import android.app.Dialog;
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

    private EditText etPondName, etBreed, etFishCount, etCostPerFish;
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
        etBreed = view.findViewById(R.id.etBreed);
        etFishCount = view.findViewById(R.id.etFishCount);
        etCostPerFish = view.findViewById(R.id.etCostPerFish);
        dateStarted = view.findViewById(R.id.dateStarted);
        tvDateHarvest = view.findViewById(R.id.tvDateHarvest);
        btnSave = view.findViewById(R.id.btnSavePond);

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

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    tvDateHarvest.setText("Harvest Date: " + sdf.format(harvestCalendar.getTime()));
                });

        btnSave.setOnClickListener(v -> {
            String name = etPondName.getText().toString().trim();
            String breed = etBreed.getText().toString().trim();
            int fishCount = Integer.parseInt(etFishCount.getText().toString().trim());
            double cost = Double.parseDouble(etCostPerFish.getText().toString().trim());


            String dateStartedStr = dateStarted.getYear() + "-" +
                    String.format(Locale.getDefault(), "%02d", (dateStarted.getMonth() + 1)) + "-" +
                    String.format(Locale.getDefault(), "%02d", dateStarted.getDayOfMonth());

            String dateHarvestStr = tvDateHarvest.getText().toString().replace("Harvest Date: ", "");

            PondModel pond = new PondModel(name, breed, fishCount, cost, dateStartedStr, dateHarvestStr, "DATA");

            // Save to SQLite
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            dbHelper.insertPond(pond);

            // Sync to server
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
