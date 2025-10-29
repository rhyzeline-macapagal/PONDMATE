package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddPondDialogFragment extends DialogFragment {

    private AlertDialog loadingDialog;
    private EditText etPondName, etPondArea;
    private TextView tvDateStarted, tvDateStocking;
    private Button btnSave;
    private ImageView ivPondImage;
    private Button btnCaptureImage;
    private Bitmap capturedImageBitmap;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private String rawDateForDB = "";
    private String rawStockingDateForDB = "";
    public interface OnPondAddedListener { void onPondAdded(PondModel pondModel);}
    private OnPondAddedListener listener;
    public void setOnPondAddedListener(OnPondAddedListener listener) {
        this.listener = listener;
    }
    private ArrayList<String> caretakerNames = new ArrayList<>();
    private ArrayList<String> caretakerIds = new ArrayList<>();
    private ArrayList<String> selectedCaretakerIds = new ArrayList<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_add_pond, container, false);

        // ✅ Initialize UI components
        etPondName = view.findViewById(R.id.etPondName);
        etPondArea = view.findViewById(R.id.etPondArea);
        tvDateStarted = view.findViewById(R.id.tvDateStarted);
        tvDateStocking = view.findViewById(R.id.tvStocking); // renamed as stocking date
        btnSave = view.findViewById(R.id.btnSavePond);
        ivPondImage = view.findViewById(R.id.ivPondImage);
        btnCaptureImage = view.findViewById(R.id.btnCaptureImage);
        TextView btnClose = view.findViewById(R.id.btnClose);

        Button btnSelectCaretakers = view.findViewById(R.id.btnSelectCaretakers);
        TextView tvSelectedCaretakers = view.findViewById(R.id.tvSelectedCaretakers);

        btnSelectCaretakers.setEnabled(false);
        btnSelectCaretakers.setText("Loading caretakers...");

        loadCaretakersFromServer(btnSelectCaretakers, tvSelectedCaretakers);

        // ✅ Close dialog
        btnClose.setOnClickListener(v -> dismiss());

        // --- Date started
        Calendar today = Calendar.getInstance();
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.getDefault());
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String todayDisplay = displayFormat.format(today.getTime());
        tvDateStarted.setText(todayDisplay);

// --- Stocking date is 14 days after date started
        Calendar stockingCalendar = (Calendar) today.clone();
        stockingCalendar.add(Calendar.DAY_OF_MONTH, 14);  // add 14 days
        rawStockingDateForDB = dbFormat.format(stockingCalendar.getTime()); // "yyyy-MM-dd"
        tvDateStocking.setText(displayFormat.format(stockingCalendar.getTime())); // display "MMM. dd, yyyy"


        // ✅ Camera button
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

        // ✅ Save button
        btnSave.setOnClickListener(v -> {
            String name = etPondName.getText().toString().trim();
            String area = etPondArea.getText().toString().trim();

            if (name.isEmpty() || area.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
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
                    .setPositiveButton("Yes", (dialog, which) -> {
                        savePond(name, imageBase64);
                    })
                    .setNegativeButton("No", (dialog, which) ->
                            Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show()
                    )
                    .show();
        });

        return view;
    }

    private void savePond(String name, String imageBase64) {
        showLoadingDialog();

        double pondArea = 0;
        String areaText = etPondArea.getText().toString().trim();
        if (!areaText.isEmpty()) {
            try {
                pondArea = Double.parseDouble(areaText);
            } catch (NumberFormatException e) {
                pondArea = 0;
            }
        }

        if (rawDateForDB == null || rawDateForDB.isEmpty()) {
            rawDateForDB = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(Calendar.getInstance().getTime());
        }
        if (rawStockingDateForDB == null || rawStockingDateForDB.isEmpty()) {
            rawStockingDateForDB = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(Calendar.getInstance().getTime());
        }

        PondModel pond = new PondModel(
                null,                  // id
                name,                  // name
                null,                  // breed
                0,                     // fishCount
                0.0,                   // costPerFish
                rawDateForDB,          // dateStarted
                null,                  // dateHarvest
                rawStockingDateForDB,  // dateStocking
                pondArea,              // pondArea
                null,                  // imagePath
                null,                  // mode
                0f,                    // actualROI
                0f,                    // estimatedROI
                null,                  // pdfPath
                0.0                    // mortalityRate
        );

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        prefs.edit().putString("selected_pond", new Gson().toJson(pond)).apply();

        // ✅ Upload to server
        PondSyncManager.uploadPondToServer(requireContext(), pond, imageBase64, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                hideLoadingDialog();
                try {
                    JSONObject json = new JSONObject(result.toString());
                    String message = json.optString("message", "Unknown error");

                    if (!json.optString("status").equals("success")) {
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ✅ Update pond with server id & image path
                    int pondId = json.optInt("pond_id", -1);
                    pond.setId(String.valueOf(pondId));

                    String serverImagePath = json.optString("image_path", "");
                    if (!serverImagePath.isEmpty() && !serverImagePath.startsWith("http")) {
                        serverImagePath = "https://pondmate.alwaysdata.net/" + serverImagePath;
                    }
                    pond.setImagePath(serverImagePath);

                    // ✅ Save full pond again after server response
                    prefs.edit().putString("selected_pond", new Gson().toJson(pond)).apply();

                    // 🟩 Assign caretakers to this new pond
                    for (String caretakerId : selectedCaretakerIds) {
                        assignCaretakerToPond(String.valueOf(pondId), caretakerId);
                    }

                    // ✅ Notify listener
                    if (listener != null) listener.onPondAdded(pond);

                    Toast.makeText(getContext(), "Pond added successfully!", Toast.LENGTH_SHORT).show();
                    dismiss();

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Failed to parse response.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
        TextView loadingText = dialogView.findViewById(R.id.loadingText);
        loadingText.setText("Saving your pond... please wait...");
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

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void loadCaretakersFromServer(Button btnSelectCaretakers, TextView tvSelectedCaretakers) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_caretakers.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }

                JSONArray caretakersArray = new JSONArray(response.toString());

                caretakerNames.clear();
                caretakerIds.clear();

                for (int i = 0; i < caretakersArray.length(); i++) {
                    JSONObject obj = caretakersArray.getJSONObject(i);
                    caretakerNames.add(obj.getString("fullname"));
                    caretakerIds.add(obj.getString("id"));
                }

                // ✅ Safe UI update — only if fragment is attached
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSelectCaretakers.setEnabled(true);
                        btnSelectCaretakers.setText("Assign Caretakers");

                        btnSelectCaretakers.setOnClickListener(v -> {
                            boolean[] checkedItems = new boolean[caretakerNames.size()];

                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Select Caretakers")
                                    .setMultiChoiceItems(caretakerNames.toArray(new String[0]), checkedItems,
                                            (dialog, which, isChecked) -> {
                                                String id = caretakerIds.get(which);
                                                if (isChecked) selectedCaretakerIds.add(id);
                                                else selectedCaretakerIds.remove(id);
                                            })
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        if (selectedCaretakerIds.isEmpty()) {
                                            tvSelectedCaretakers.setText("No caretakers selected");
                                        } else {
                                            StringBuilder selectedNames = new StringBuilder();
                                            for (String id : selectedCaretakerIds) {
                                                int idx = caretakerIds.indexOf(id);
                                                if (idx >= 0)
                                                    selectedNames.append(caretakerNames.get(idx)).append(", ");
                                            }

                                            if (selectedNames.length() > 2)
                                                selectedNames.setLength(selectedNames.length() - 2); // remove last ", "

                                            tvSelectedCaretakers.setText("Selected: " + selectedNames);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSelectCaretakers.setEnabled(false);
                        btnSelectCaretakers.setText("Failed to load caretakers");
                        Toast.makeText(requireContext(), "Failed to load caretakers.", Toast.LENGTH_SHORT).show();
                    });
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void assignCaretakerToPond(String pondId, String caretakerId) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/assign_caretaker_to_pond.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_id=" + URLEncoder.encode(pondId, "UTF-8") +
                        "&caretaker_id=" + URLEncoder.encode(caretakerId, "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes());
                }

                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }

                String resp = response.toString().trim();
                JSONObject json = new JSONObject(resp);

                String status = json.optString("status", "error");
                int pondCount = json.optInt("pond_count", 0);

                // ✅ Safely update UI only if fragment is still attached
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if ("success".equalsIgnoreCase(status)) {
                            Toast.makeText(getActivity(),
                                    "Caretaker assigned! Now handling " + pondCount + " pond(s).",
                                    Toast.LENGTH_LONG).show();
                        } else if ("exists".equalsIgnoreCase(status)) {
                            Toast.makeText(getActivity(),
                                    "Caretaker already assigned to this pond.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(),
                                    "Failed to assign caretaker. Server said: " + status,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(),
                                    "Failed to assign caretaker: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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
            capturedImageBitmap = (Bitmap) extras.get("data");
            ivPondImage.setImageBitmap(capturedImageBitmap);
        }
    }
}
