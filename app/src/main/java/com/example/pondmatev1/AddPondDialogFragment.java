package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddPondDialogFragment extends DialogFragment {

    private EditText etPondName, etFishCount, etCostPerFish;
    private Spinner spinnerBreed;

    private TextView tvDateHarvest, dateStarted;
    private Button btnSave;
    private String rawHarvestDateForDB = "";
    private ImageView ivPondImage;
    private Button btnCaptureImage;
    private Bitmap capturedImageBitmap;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;

    // Default feeding times in minutes since midnight
    private final int DEFAULT_FEEDING1 = 7 * 60;   // 7:00 AM
    private final int DEFAULT_FEEDING2 = 9 * 60;   // 9:00 AM
    private final int DEFAULT_FEEDING3 = 15 * 60;  // 3:00 PM

    // TextViews for feeding times
    private TextView timeoffeeding1, timeoffeeding2, timeoffeeding3, feedQuantityView;


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
        etCostPerFish = view.findViewById(R.id.etCostPerFish);
        dateStarted = view.findViewById(R.id.tvDateStarted);
        tvDateHarvest = view.findViewById(R.id.tvDateHarvest);
        btnSave = view.findViewById(R.id.btnSavePond);
        TextView closeDialog = view.findViewById(R.id.btnClose);
        ivPondImage = view.findViewById(R.id.ivPondImage);
        btnCaptureImage = view.findViewById(R.id.btnCaptureImage);
        feedQuantityView = view.findViewById(R.id.tvFeedQuantity);
        weightInput = view.findViewById(R.id.etFishWeight);

        timeoffeeding1 = view.findViewById(R.id.timeoffeeding1);
        timeoffeeding2 = view.findViewById(R.id.timeoffeeding2);
        timeoffeeding3 = view.findViewById(R.id.timeoffeeding3);

        setDefaultFeedingTimes();

        etFishCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                computeFeedQuantity();
            }
        });


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
        String[] fishBreeds = {"Tilapia", "Bangus", "Alimango"};
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

        String rawDateForDB = dbFormat.format(today.getTime());


        Runnable updateHarvestDate = () -> {
            String selectedBreed = spinnerBreed.getSelectedItem().toString();
            Calendar harvestCalendar = (Calendar) today.clone();

            switch (selectedBreed) {
                    case "Tilapia":
                        harvestCalendar.add(Calendar.DAY_OF_YEAR, 60);
                        break;
                    case "Bangus":
                        harvestCalendar.add(Calendar.DAY_OF_YEAR, 90);
                        break;
                    case "Alimango":
                        harvestCalendar.add(Calendar.DAY_OF_YEAR, 270);
                        break;
                }



            rawHarvestDateForDB = dbFormat.format(harvestCalendar.getTime());
            tvDateHarvest.setText(displayFormat.format(harvestCalendar.getTime()));
        };

        spinnerBreed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateHarvestDate.run();

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

            String imageBase64 = bitmapToBase64(capturedImageBitmap);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Save")
                    .setMessage("Do you want to save this pond?")
                    .setPositiveButton("Yes", (dialogInterface, which) -> {

                        // ✅ Use stored values
                        String dateStartedStr = rawDateForDB;      // from today
                        String dateHarvestStr = rawHarvestDateForDB; // from breed calculation

                        PondModel pond = new PondModel(
                                null,                // id
                                name,                // name
                                breed,               // breed
                                fishCount,           // fish count
                                cost,                // cost per fish
                                dateStartedStr,      // date started
                                dateHarvestStr,      // date harvest
                                null,                // imagePath
                                0f,                  // actualROI default
                                0f                   // estimatedROI default
                        );

                        // Upload pond to server
                        PondSyncManager.uploadPondToServer(pond, imageBase64, new PondSyncManager.Callback() {
                            @Override
                            public void onSuccess(Object result) {
                                try {
                                    JSONObject json = new JSONObject(result.toString());
                                    String message = json.optString("message", "Unknown error");

                                    if (!json.getString("status").equals("success")) {
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

                                    Toast.makeText(getContext(), "Pond and fingerlings cost added successfully!", Toast.LENGTH_SHORT).show();
                                    dismiss();

                                } catch (Exception e) {
                                    Toast.makeText(getContext(), "Error parsing server response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.d("ServerResponse", "Raw result: [" + result.toString() + "]");
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                            }
                        });

                    })
                    .setNegativeButton("No", (dialogInterface, which) -> dialogInterface.dismiss())
                    .show();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            capturedImageBitmap = (Bitmap) extras.get("data"); // This is a small thumbnail
            ivPondImage.setImageBitmap(capturedImageBitmap);
        }
    }
    private void computeFeedQuantity() {
        String weightStr = weightInput.getText().toString().trim(); // or use weight input if separate
        String fishCountStr = etFishCount.getText().toString().trim();

        if (weightStr.isEmpty() || fishCountStr.isEmpty()) {
            feedQuantityView.setText("--");
            return;
        }

        try {
            int fishCount = Integer.parseInt(fishCountStr);   // ✅ number of fish
            float fishWeight = Float.parseFloat(weightStr);   // in grams
            float feedPercentage = 0.03f;                     // 3%
            float totalFeed = feedPercentage * fishWeight * fishCount; // grams
            float totalKg = totalFeed / 1000f;

            feedQuantityView.setText(String.format(Locale.getDefault(),
                    "%.2f kg (%.0f g)", totalKg, totalFeed));

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
                // Feeding 1 cannot be in the past
                if (selectedMinutes < currentMinutes) {
                    Toast.makeText(requireContext(),
                            "Feeding 1 cannot be set in the past!",
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
        put("Tilapia", 1.50);
        put("Bangus", 2.50);
        put("Alimango", 3.0);
    }};

}
