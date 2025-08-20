package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
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
    private DatePicker dateStarted;
    private TextView tvDateHarvest;
    private Button btnSave;
    private String rawHarvestDateForDB = "";
    private ImageView ivPondImage;
    private Button btnCaptureImage;
    private Bitmap capturedImageBitmap;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;

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
        ivPondImage = view.findViewById(R.id.ivPondImage);
        btnCaptureImage = view.findViewById(R.id.btnCaptureImage);

        //camera start
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
        dateStarted.init(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH),
                (view1, year, monthOfYear, dayOfMonth) -> {
                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.set(year, monthOfYear, dayOfMonth);

                    Calendar harvestCalendar = (Calendar) startCalendar.clone();
                    harvestCalendar.add(Calendar.DAY_OF_YEAR, 120);

                    // Format for DB
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    rawHarvestDateForDB = dbFormat.format(harvestCalendar.getTime());

                    // Format for display
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
                    String formattedDisplayDate = displayFormat.format(harvestCalendar.getTime());

                    tvDateHarvest.setText(formattedDisplayDate);
                }
        );


        btnSave.setOnClickListener(v -> {
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

            if (capturedImageBitmap == null) {
                Toast.makeText(getContext(), "Please capture a pond image.", Toast.LENGTH_SHORT).show();
                return;
            }

            String imageBase64 = bitmapToBase64(capturedImageBitmap);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Save")
                    .setMessage("Do you want to save this pond?")
                    .setPositiveButton("Yes", (dialogInterface, which) -> {

                        // Show loading dialog

                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        View loadingView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);
                        builder.setView(loadingView);
                        builder.setCancelable(false);
                        AlertDialog loadingDialog = builder.create();
                        loadingDialog.show();

                        String dateStartedStr = dateStarted.getYear() + "-" +
                                String.format(Locale.getDefault(), "%02d", (dateStarted.getMonth() + 1)) + "-" +
                                String.format(Locale.getDefault(), "%02d", dateStarted.getDayOfMonth());
                        String dateHarvestStr = rawHarvestDateForDB;

                        // Save minimal info to SharedPreferences
                        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("SharedData", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("fish_breed", breed);
                        editor.putString("fish_amount", costStr);
                        editor.putString("number_fish", fishCountStr);
                        editor.putString("date_started", dateStartedStr);
                        editor.apply();


                        PondModel pond = new PondModel(
                                null, // ID will be set after server responds
                                name,
                                breed,
                                fishCount,
                                cost,
                                dateStartedStr,
                                dateHarvestStr,
                                null, // imagePath will be set later
                                "DATA"
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
                                        loadingDialog.dismiss();
                                        return;
                                    }

                                    int pondId = json.getInt("pond_id");
                                    pond.setId(String.valueOf(pondId));

                                    // If API already returns full public URL, use it directly
                                    String serverImagePath = json.optString("image_path", "");
                                    if (!serverImagePath.isEmpty() && !serverImagePath.startsWith("http")) {
                                        serverImagePath = "https://pondmate.alwaysdata.net/" + serverImagePath;
                                    }
                                    pond.setImagePath(serverImagePath);

                                    if (listener != null) listener.onPondAdded(pond);

                                    Toast.makeText(getContext(), "Pond and fingerlings cost added successfully!", Toast.LENGTH_SHORT).show();
                                    loadingDialog.dismiss();
                                    dismiss();

                                } catch (Exception e) {
                                    Toast.makeText(getContext(), "Error parsing server response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.d("ServerResponse", "Raw result: [" + result.toString() + "]");
                                    loadingDialog.dismiss();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                                loadingDialog.dismiss();
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

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream); // compressed
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

}