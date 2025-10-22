package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.util.Log;
import java.text.DecimalFormat;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddPondDialogFragment extends DialogFragment {

    private AlertDialog loadingDialog;

    private EditText etPondName, etFishCount, etCostPerFish, etPondArea;

    private Spinner spinnerBreed;

    private TextView tvDateHarvest, dateStarted, etFeedPrice;
    private Button btnSave;
    private String rawHarvestDateForDB = "";

    private String rawDateForDB = "";

    private ImageView ivPondImage;
    private Button btnCaptureImage;
    private Bitmap capturedImageBitmap;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;

    // Default feeding times in minutes since midnight
    private final int DEFAULT_FEEDING1 = 7 * 60;   // 7:00 AM
    private final int DEFAULT_FEEDING2 = 9 * 60;   // 9:00 AM
    private final int DEFAULT_FEEDING3 = 15 * 60;  // 3:00 PM

    // TextViews for feeding times
    private TextView timeoffeeding1, timeoffeeding2, timeoffeeding3, feedQuantityView, feedPriceView, feedPriceDayView, tvTotalFingerlingsCost;


    // Store selected times
    private Integer feeding1Minutes, feeding2Minutes, feeding3Minutes;
    private EditText weightInput;
    private int fishCount = 0;


    private String formattedTime1 = "", formattedTime2 = "", formattedTime3 = "";
    private int DEFAULT_FEEDING_1;
    private int DEFAULT_FEEDING_2;


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
        tvTotalFingerlingsCost = view.findViewById(R.id.tvTotalFingerlingsCost);
        etCostPerFish = view.findViewById(R.id.etCostPerFish);
        dateStarted = view.findViewById(R.id.tvDateStarted);
        tvDateHarvest = view.findViewById(R.id.tvDateHarvest);
        btnSave = view.findViewById(R.id.btnSavePond);
        TextView closeDialog = view.findViewById(R.id.btnClose);
        ivPondImage = view.findViewById(R.id.ivPondImage);
        btnCaptureImage = view.findViewById(R.id.btnCaptureImage);
        feedQuantityView = view.findViewById(R.id.tvFeedQuantity);
        weightInput = view.findViewById(R.id.etFishWeight);
        feedPriceView = view.findViewById(R.id.feedprice);
        feedPriceDayView = view.findViewById(R.id.feedpriceday);
        etPondArea = view.findViewById(R.id.etPondArea);



        timeoffeeding1 = view.findViewById(R.id.timeoffeeding1);
        timeoffeeding2 = view.findViewById(R.id.timeoffeeding2);
        timeoffeeding3 = view.findViewById(R.id.timeoffeeding3);



        setDefaultFeedingTimes();

        // 🟢 Fish Count listener
        etFishCount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkStockingDensityRealtime();
            }

            @Override public void afterTextChanged(Editable s) {
                computeFeedQuantity();
                computeTotalFingerlingsCost();
            }
        });

// 🟢 Cost per Fish listener
        etCostPerFish.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                computeTotalFingerlingsCost();
            }
        });

// 🟢 Pond Area listener (to recalc density when area changes)
        etPondArea.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkStockingDensityRealtime(); // ✅ Trigger when pond area changes
            }
            @Override public void afterTextChanged(Editable s) {}
        });

// 🟢 Fish Weight listener
        weightInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                computeFeedQuantity();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

// optional: if prefilled
        computeTotalFingerlingsCost();


        Button btnselecttime1 = view.findViewById(R.id.btnselecttime1);
        Button btnselecttime2 = view.findViewById(R.id.btnselecttime2);
        Button btnselecttime3 = view.findViewById(R.id.btnselecttime3);

        btnselecttime1.setOnClickListener(v -> showTimePickerDialog(timeoffeeding1, 1));
        btnselecttime2.setOnClickListener(v -> showTimePickerDialog(timeoffeeding2, 2));
        btnselecttime3.setOnClickListener(v -> showTimePickerDialog(timeoffeeding3, 3));

        // Camera button
        btnCaptureImage.setOnClickListener(v -> {
            if (requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } else {
                Toast.makeText(getContext(), "Camera not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Populate spinner with fish breeds
        String[] fishBreeds = {"Tilapia", "Bangus"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, fishBreeds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBreed.setAdapter(adapter);


        closeDialog.setOnClickListener(v -> dismiss());


        Calendar today = Calendar.getInstance();
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


        String todayDisplay = displayFormat.format(today.getTime());
        dateStarted.setText(todayDisplay);

        rawDateForDB = dbFormat.format(today.getTime());


        Runnable updateHarvestDate = () -> {
            String selectedBreed = spinnerBreed.getSelectedItem().toString();
            Calendar harvestCalendar = (Calendar) today.clone();

            switch (selectedBreed) {
                    case "Tilapia":
                        harvestCalendar.add(Calendar.DAY_OF_YEAR, 125);
                        break;
                    case "Bangus":
                        harvestCalendar.add(Calendar.DAY_OF_YEAR, 125);
                        break;

                }



            rawHarvestDateForDB = dbFormat.format(harvestCalendar.getTime());
            tvDateHarvest.setText(displayFormat.format(harvestCalendar.getTime()));
        };

        spinnerBreed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateHarvestDate.run();
                computeFeedQuantity();
                checkStockingDensityRealtime();

                String selectedBreed = spinnerBreed.getSelectedItem().toString();
                if (breedCosts.containsKey(selectedBreed)) {
                    double defaultCost = breedCosts.get(selectedBreed);
                    etCostPerFish.setText("₱" + defaultCost);
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etCostPerFish.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                String input = s.toString();

                // Remove existing peso sign if any
                if (input.startsWith("₱")) {
                    input = input.substring(1);
                }

                // If empty, just show ₱
                if (input.isEmpty()) {
                    etCostPerFish.setText("₱");
                    etCostPerFish.setSelection(etCostPerFish.getText().length());
                } else {
                    etCostPerFish.setText("₱" + input);
                    etCostPerFish.setSelection(etCostPerFish.getText().length());
                }

                isEditing = false;
            }
        });


        updateHarvestDate.run();
        String selectedBreed = spinnerBreed.getSelectedItem().toString();
        if (breedCosts.containsKey(selectedBreed)) {
            double defaultCost = breedCosts.get(selectedBreed);
            etCostPerFish.setText("₱" + defaultCost);
        }


        btnSave.setOnClickListener(v -> {
            String name = etPondName.getText().toString().trim();
            String breed = spinnerBreed.getSelectedItem().toString();
            String fishCountStr = etFishCount.getText().toString().trim();
            String costStr = etCostPerFish.getText().toString().trim().replace("₱", "");

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

            if (capturedImageBitmap == null) {
                Toast.makeText(getContext(), "Please capture a pond image.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🟢 Stocking density validation here
            if (!checkStockingDensity()) {
                return; // ❌ Stop saving if overstocked
            }

            String imageBase64 = bitmapToBase64(capturedImageBitmap);

            // 🟢 Compute feed per cycle (final variable for lambda)
            float tempFeedPerCycle;
            try {
                int fishCnt = Integer.parseInt(fishCountStr);
                String weightStr = weightInput.getText().toString().trim();
                float fishW = weightStr.isEmpty() ? 0f : Float.parseFloat(weightStr);
                float feedPct = 0.03f; // 3% of body weight
                float totalFeedGrams = feedPct * fishW * fishCnt;

                int cycleCount = 0;
                if (feeding1Minutes != null) cycleCount++;
                if (feeding2Minutes != null) cycleCount++;
                if (feeding3Minutes != null) cycleCount++;
                if (cycleCount == 0) cycleCount = 3;

                float perCycleGrams = (cycleCount > 0) ? totalFeedGrams / cycleCount : 0f;
                tempFeedPerCycle = perCycleGrams / 1000f; // convert grams → kg
            } catch (NumberFormatException e) {
                tempFeedPerCycle = 0f;
            }

// ✅ Now make it effectively final for lambda use
            final float computedFeedPerCycle = tempFeedPerCycle;


            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Save")
                    .setMessage("Do you want to save this pond?")
                    .setPositiveButton("Yes", (dialog1, which1) -> {
                        String confirmationMessage =
                                "Are you sure with the feeding schedule you inputted?\n\n" +
                                        "Feeding 1: " + convertTo12Hour(formattedTime1) + "\n" +
                                        "Feeding 2: " + convertTo12Hour(formattedTime2) + "\n" +
                                        "Feeding 3: " + convertTo12Hour(formattedTime3) + "\n\n" +
                                        "Feed per cycle: " + String.format(Locale.getDefault(), "%.2f kg", computedFeedPerCycle);

                        new AlertDialog.Builder(requireContext())
                                .setTitle("Confirm Feeding Schedule")
                                .setMessage(confirmationMessage)
                                .setPositiveButton("Yes", (dialog2, which2) -> {
                                    String feedPriceStr = feedPriceView.getText().toString().replace("₱", "").trim();
                                    if (feedPriceStr.isEmpty()) feedPriceStr = "0";


// ✅ Pass computedFeedPerCycle and feedPriceStr
                                    savePondAndSchedule(name, breed, fishCount, cost, imageBase64, computedFeedPerCycle, feedPriceStr);

                                })
                                .setNegativeButton("No", (dialog2, which2) -> {
                                    Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                                })
                                .show();

                    })
                    .setNegativeButton("No", (dialog1, which1) -> {
                        Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
        return view;
    }

    private void checkStockingDensityRealtime() {
        String breed = spinnerBreed.getSelectedItem() != null ? spinnerBreed.getSelectedItem().toString() : "";
        String fishCountStr = etFishCount.getText().toString().trim();
        String pondAreaStr = etPondArea.getText().toString().trim();

        if (fishCountStr.isEmpty() || pondAreaStr.isEmpty()) {
            etFishCount.setError(null);
            return;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double pondArea = Double.parseDouble(pondAreaStr);

            if (pondArea <= 0) {
                etPondArea.setError("Pond area must be > 0");
                return;
            }

            double density = fishCount / pondArea;
            double maxAllowed = 0;
            double minRecommended = 0;

            switch (breed) {
                case "Tilapia":
                    minRecommended = 3.0;
                    maxAllowed = 15.0;
                    break;
                case "Bangus":
                    minRecommended = 0.2;
                    maxAllowed = 3.0;
                    break;
            }

            // 🟡 Give visual feedback in real time
            if (density > maxAllowed) {
                etFishCount.setError(String.format(Locale.getDefault(),
                        "⚠️ Overstocked! %.2f fish/m² (max %.2f)", density, maxAllowed));
            } else if (density < minRecommended) {
                etFishCount.setError(String.format(Locale.getDefault(),
                        "Low density: %.2f fish/m² (min %.2f)", density, minRecommended));
            } else {
                etFishCount.setError(null); // ✅ Clear warning if within range
            }

        } catch (NumberFormatException e) {
            etFishCount.setError(null);
        }
    }

    private boolean checkStockingDensity() {
        String breed = spinnerBreed.getSelectedItem() != null ? spinnerBreed.getSelectedItem().toString() : "";
        String fishCountStr = etFishCount.getText().toString().trim();
        String pondAreaStr = etPondArea.getText().toString().trim();

        if (fishCountStr.isEmpty() || pondAreaStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter fish count and pond area.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double pondArea = Double.parseDouble(pondAreaStr);

            if (pondArea <= 0) {
                Toast.makeText(getContext(), "Pond area must be greater than 0.", Toast.LENGTH_SHORT).show();
                return false;
            }

            double density = fishCount / pondArea;
            double maxAllowed = 0;
            double minRecommended = 0;

            switch (breed) {
                case "Tilapia":
                    minRecommended = 3.0;
                    maxAllowed = 15.0;
                    break;
                case "Bangus":
                    minRecommended = 0.2;
                    maxAllowed = 3.0;
                    break;

            }

            if (density > maxAllowed) {
                Toast.makeText(getContext(),
                        String.format(Locale.getDefault(),
                                "❌ Overstocked! %.2f fish/m² (max %.2f)", density, maxAllowed),
                        Toast.LENGTH_LONG).show();
                return false; // ❌ prevent saving
            }

            return true; // ✅ within safe range

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid input format.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }



    private void computeTotalFingerlingsCost() {
        String fishCountStr = etFishCount.getText().toString().trim();
        String costPerFishStr = etCostPerFish.getText().toString().trim();

        // Remove any characters except digits and dot
        fishCountStr = fishCountStr.replaceAll("[^\\d.]", "");
        costPerFishStr = costPerFishStr.replaceAll("[^\\d.]", "");

        if (fishCountStr.isEmpty() || costPerFishStr.isEmpty()) {
            tvTotalFingerlingsCost.setText("₱0.00");
            return;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double costPerFish = Double.parseDouble(costPerFishStr);
            double totalCost = fishCount * costPerFish;

            DecimalFormat df = new DecimalFormat("#,##0.00"); // adds commas and 2 decimals
            tvTotalFingerlingsCost.setText("₱" + df.format(totalCost));
        } catch (NumberFormatException e) {
            tvTotalFingerlingsCost.setText("₱0.00");
        }
    }



    private void savePondAndSchedule(String name, String breed, int fishCount, double cost,
                                     String imageBase64, float computedFeedPerCycle, String feedPriceStr) {

        showLoadingDialog();

        String dateStartedStr = rawDateForDB;
        String dateHarvestStr = rawHarvestDateForDB;

        PondModel pond = new PondModel(
                null,
                name,
                breed,
                fishCount,
                cost,
                dateStartedStr,
                dateHarvestStr,
                null,
                0f,
                0f
        );

        PondSyncManager.uploadPondToServer(pond, imageBase64, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                try {
                    JSONObject json = new JSONObject(result.toString());
                    String message = json.optString("message", "Unknown error");

                    if (!json.getString("status").equals("success")) {
                        hideLoadingDialog();
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    int pondId = json.getInt("pond_id");
                    pond.setId(String.valueOf(pondId));

                    String serverImagePath = json.optString("image_path", "");
                    if (!serverImagePath.isEmpty() && !serverImagePath.startsWith("http")) {
                        serverImagePath = "https://pondmate.alwaysdata.net/" + serverImagePath;
                    }
                    pond.setImagePath(serverImagePath);
                    if (listener != null) listener.onPondAdded(pond);

                    hideLoadingDialog();
                    Toast.makeText(getContext(), "Pond added successfully!", Toast.LENGTH_SHORT).show();
                    confirmAndSaveSchedule(name);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date startDate = sdf.parse(dateStartedStr);
                    Date today = new Date();
                    long diffInMillis = today.getTime() - startDate.getTime();
                    int pondAgeDays = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                    int pondAgeMonths = pondAgeDays / 30; // approximate month
                    String feederType = getFeederType(breed, pondAgeMonths);


                    PondSyncManager syncManager = new PondSyncManager();
                    syncManager.createWeeklySchedule(
                            pond.getName(),
                            formattedTime1,
                            formattedTime2,
                            formattedTime3,
                            String.valueOf(computedFeedPerCycle),
                            weightInput.getText().toString().trim(),
                            feedPriceStr,
                            feederType,
                            new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object result) {
                                    if (!isAdded()) return; // fragment no longer attached, exit early

                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "Schedule Created!", Toast.LENGTH_LONG).show()
                                    );
                                }

                                @Override
                                public void onError(String error) {
                                    if (!isAdded()) return;

                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "Schedule error: " + error, Toast.LENGTH_LONG).show()
                                    );
                                    }
                                }

                    );
                    dismiss();

                } catch (Exception e) {
                    hideLoadingDialog();
                    Toast.makeText(getContext(), "Error parsing server response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.d("ServerResponse", "Raw result: [" + result.toString() + "]");
                }
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String convertTo12Hour(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf24.parse(time24);
            return sdf12.format(date);
        } catch (Exception e) {
            return time24; // fallback if parsing fails
        }
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



    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            capturedImageBitmap = (Bitmap) extras.get("data"); // This is a small thumbnail
            ivPondImage.setImageBitmap(capturedImageBitmap);
        }
    }
    private String getFeederType(String breed, int pondAgeMonths) {
        breed = breed.toLowerCase();
        if (breed.contains("tilapia") || breed.contains("bangus")) {
            if (pondAgeMonths < 1) return "starter";
            else if (pondAgeMonths <= 3) return "grower";
            else return "finisher";
        }
        return "auto"; // default
    }


    private void confirmAndSaveSchedule(String pondName) {
        pondName = etPondName.getText().toString().trim();
        if (pondName.isEmpty()) {
            Toast.makeText(getContext(), "Pond name is empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (formattedTime1.isEmpty() || formattedTime2.isEmpty() || formattedTime3.isEmpty()) {
            Toast.makeText(getContext(), "Please set all feeding times", Toast.LENGTH_SHORT).show();
            return;
        }


        String fishCountStr = etFishCount.getText().toString().trim();
        if (fishCountStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter fish count", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fishCount = Integer.parseInt(fishCountStr); // ⚠️ set the class field
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid fish count", Toast.LENGTH_SHORT).show();
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
        // ✅ Always divide by 3 (fixed cycles)
        float perCycleGrams = computedFeedQty / 3;
        // Convert to kg
        float finalFeedAmount = perCycleGrams / 1000f;

        String feedPriceText = feedPriceView.getText().toString().replace("₱", "").trim();
        double feedPrice = 0.0;
        try {
            if (feedPriceText.contains("(")) {
                feedPriceText = feedPriceText.split("\\(")[0].trim();
            }
            feedPrice = Double.parseDouble(feedPriceText);
        } catch (NumberFormatException e) {
            feedPrice = 0.0;
        }

        uploadFeedingTimesToAdafruit(pondName, formattedTime1, formattedTime2, formattedTime3, finalFeedAmount);

        // Upload to your backend server
        PondSyncManager.uploadFeedingScheduleToServer(
                pondName, formattedTime1, formattedTime2, formattedTime3,
                finalFeedAmount, fishWeight, (float) feedPrice,
                new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object result) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Uploaded to server: " + result, Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show());
                        }
                    }
                }
        );

        Toast.makeText(getContext(), "Schedule saved!", Toast.LENGTH_SHORT).show();

    }



    private void uploadFeedingTimesToAdafruit(String pondName, String t1, String t2, String t3, float feedAmountKg) {
        OkHttpClient client = new OkHttpClient();

        String feedKey = "schedule"; // your Adafruit feed key
        String username = getString(R.string.adafruit_username);
        String aioKey = getString(R.string.adafruit_aio_key);

        // Convert kg → grams (1 kg = 1000 g)
        int feedAmountGrams = Math.round(feedAmountKg * 1000);

// Divide by 10 for the value to send
        int reducedGrams = feedAmountGrams / 10;

// Value format: PondName|time1,time2,time3|feedAmountGrams
        String value = pondName + "|" + t1 + "," + t2 + "," + t3 + "|" + reducedGrams + "g";

        try {
            JSONObject json = new JSONObject();
            json.put("value", value);

            RequestBody body = RequestBody.create(json.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            String url = "https://io.adafruit.com/api/v2/" + username + "/feeds/" + feedKey + "/data";

            // 🔥 Log what you’re about to send
            android.util.Log.d("ScheduleFeeder", "Posting to Adafruit: " + value);
            android.util.Log.d("ScheduleFeeder", "URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-AIO-Key", aioKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Context context = getContext();
                        if (context != null) {
                            Toast.makeText(context, "Failed to post to Adafruit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Context context = getContext();
                        if (context != null) {
                            if (response.isSuccessful()) {
                                Toast.makeText(context, "Schedule saved to Adafruit!", Toast.LENGTH_LONG).show();
                                AddPondDialogFragment.this.dismiss();
                            } else {
                                Toast.makeText(context, "Failed to add schedule: " + response.code(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });

        } catch (JSONException ex) {
            android.util.Log.e("ScheduleFeeder", "JSON error", ex);
            Toast.makeText(getContext(), "JSON error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void computeFeedQuantity() {
        String weightStr = weightInput.getText().toString().trim();
        String fishCountStr = etFishCount.getText().toString().trim();

        if (weightStr.isEmpty() || fishCountStr.isEmpty()) {
            feedQuantityView.setText("--");
            feedPriceView.setText("--");
            return;
        }

        try {
            int fishCount = Integer.parseInt(fishCountStr);
            float fishWeight = Float.parseFloat(weightStr);
            float feedPercentage = 0.03f; // starter = 3%

            // daily feed in grams
            float totalFeed = feedPercentage * fishWeight * fishCount;

            // how many feeding cycles are set
            int cycleCount = 0;
            if (feeding1Minutes != null) cycleCount++;
            if (feeding2Minutes != null) cycleCount++;
            if (feeding3Minutes != null) cycleCount++;

            // per cycle
            float perCycleFeed = (cycleCount > 0) ? totalFeed / cycleCount : 0;
            float perCycleKg = perCycleFeed / 1000f;

            // update feed quantity
            feedQuantityView.setText(String.format(Locale.getDefault(),
                    "%.2f kg (%.0f g)", perCycleKg, perCycleFeed));

            // compute price separately
            computeFeedsPrice(perCycleKg);

        } catch (NumberFormatException e) {
            feedQuantityView.setText("--");
            feedPriceView.setText("--");
        }
    }


    private void computeFeedsPrice(float perCycleKg) {
        String selectedBreed = spinnerBreed.getSelectedItem().toString().toLowerCase();

        PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(result.toString());

                    if (!json.getString("status").equals("success")) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                feedPriceView.setText("Error: " + json.optString("message"));
                                feedPriceDayView.setText("--"); // reset
                            });
                        }
                        return;
                    }

                    org.json.JSONArray arr = json.getJSONArray("feeds");
                    double starterPrice = 0;

                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject obj = arr.getJSONObject(i);
                        String breed = obj.getString("breed");
                        String type = obj.getString("feed_type");
                        double price = obj.getDouble("price_per_kg");

                        if (breed.equalsIgnoreCase(selectedBreed) && type.equalsIgnoreCase("starter")) {
                            starterPrice = price;
                            break;
                        }
                    }

                    double perCycleCost = perCycleKg * starterPrice;
                    double perDayCost = perCycleCost * 3;
                    double finalStarterPrice = starterPrice;

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            feedPriceView.setText(String.format(Locale.getDefault(),
                                    "₱%.2f (Starter ₱%.2f/kg)",
                                    perCycleCost, finalStarterPrice));

                            feedPriceDayView.setText(String.format(Locale.getDefault(),
                                    "₱%.2f", perDayCost));
                        });
                    }

                } catch (Exception e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            feedPriceView.setText("Error parsing feeds");
                            feedPriceDayView.setText("--");
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        feedPriceView.setText("Error: " + error);
                        feedPriceDayView.setText("--");
                    });
                }
            }
        });
    }




    private void showLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
        TextView loadingText = dialogView.findViewById(R.id.loadingText);
        loadingText.setText("Loading your PONDS please wait...");
        Animation rotate = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate);
        if (fishLoader != null) fishLoader.startAnimation(rotate);
        builder.setView(dialogView).setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
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


    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream); // compressed
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void setDefaultFeedingTimes() {
        // Feeding 1
        feeding1Minutes = DEFAULT_FEEDING1;
        formattedTime1 = formatMinutesTo24h(DEFAULT_FEEDING1);
        timeoffeeding1.setText(formatMinutesTo12h(DEFAULT_FEEDING1));

        // Feeding 2
        feeding2Minutes = DEFAULT_FEEDING2;
        formattedTime2 = formatMinutesTo24h(DEFAULT_FEEDING2);
        timeoffeeding2.setText(formatMinutesTo12h(DEFAULT_FEEDING2));

        // Feeding 3
        feeding3Minutes = DEFAULT_FEEDING3;
        formattedTime3 = formatMinutesTo24h(DEFAULT_FEEDING3);
        timeoffeeding3.setText(formatMinutesTo12h(DEFAULT_FEEDING3));
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

    private boolean isValidTime(int feedingNumber, int selectedMinutes) {
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        switch (feedingNumber) {
            case 1:
                // Feeding 1 must be earlier than Feeding 2 and 3 (if already set)
                if (feeding2Minutes != null && selectedMinutes >= feeding2Minutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 1 must be earlier than Feeding 2!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (feeding3Minutes != null && selectedMinutes >= feeding3Minutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 1 must be earlier than Feeding 3!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                feeding1Minutes = selectedMinutes;
                return true;

            case 2:
                // Use default if Feeding 1 is not set
                int f1 = (feeding1Minutes != null) ? feeding1Minutes : DEFAULT_FEEDING_1;

                if (selectedMinutes <= f1) {
                    Toast.makeText(requireContext(),
                            "Feeding 2 must be later than Feeding 1!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                // Feeding 2 must be earlier than Feeding 3 (if already set)
                if (feeding3Minutes != null && selectedMinutes >= feeding3Minutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 2 must be earlier than Feeding 3!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                feeding2Minutes = selectedMinutes;
                return true;

            case 3:
                // Use default if Feeding 1 or 2 are not set
                f1 = (feeding1Minutes != null) ? feeding1Minutes : DEFAULT_FEEDING_1;
                int f2 = (feeding2Minutes != null) ? feeding2Minutes : DEFAULT_FEEDING_2;

                if (selectedMinutes <= f1 || selectedMinutes <= f2) {
                    Toast.makeText(requireContext(),
                            "Feeding 3 must be later than Feeding 1 and 2!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                feeding3Minutes = selectedMinutes;
                return true;

            default:
                return false;
        }

    }


    private final java.util.Map<String, Double> breedCosts = new java.util.HashMap<String, Double>() {{
        put("Tilapia", .20);
        put("Bangus", .50);

    }};

}
