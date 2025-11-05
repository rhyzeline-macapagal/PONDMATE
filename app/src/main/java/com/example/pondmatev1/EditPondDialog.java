package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.flexbox.FlexboxLayout;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditPondDialog extends DialogFragment {

    private EditText etPondName, etPondArea, etFishCount, etCostPerFish, etMortalityRate;
    private EditText etDateCreated, etStockingDate, etHarvestDate, etCaretakers;
    private Spinner spinnerSpecies;
    private Button btnSave, btnCancel, btnSelectCaretakers;
    private TextView btnClose;
    private FlexboxLayout caretakerChipContainer;
    private TextView tvTotalFingerlingsCost;

    private boolean hasFingerlingStock = false;


    private PondModel pond; // existing pond data
    private OnPondUpdatedListener listener;

    private static final String TAG = "EditPondDialog";

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private double rawPondArea = 0;
    private String rawStockingDateForDB = "";

    private final List<String> caretakerNames = new ArrayList<>();
    private final List<String> caretakerIds = new ArrayList<>();
    private final Set<String> selectedCaretakerIds = new HashSet<>();

    public interface OnPondUpdatedListener {
        void onPondUpdated(PondModel updatedPond);
    }

    public EditPondDialog(PondModel pond, OnPondUpdatedListener listener) {
        this.pond = pond;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_pond, null);

        initViews(view);
        setupSpeciesSpinner();
        populateFields();
        setupTextWatchers();
        setupCaretakerButton();
        setupDatePickers();


        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveChanges());

        builder.setView(view);
        return builder.create();
    }

    private void initViews(View v) {
        Log.d(TAG, "initViews() called");
        etPondName = v.findViewById(R.id.etPondName);
        etPondArea = v.findViewById(R.id.etPondArea);
        etFishCount = v.findViewById(R.id.etFishCount);
        etCostPerFish = v.findViewById(R.id.etCostPerFish);
        etMortalityRate = v.findViewById(R.id.etMortalityRate);
        etDateCreated = v.findViewById(R.id.etDateCreated);
        etStockingDate = v.findViewById(R.id.etStockingDate);
        etHarvestDate = v.findViewById(R.id.etHarvestDate);
        spinnerSpecies = v.findViewById(R.id.spinnerSpecies);
        btnSave = v.findViewById(R.id.btnSave);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnClose = v.findViewById(R.id.btnClose);
        btnSelectCaretakers = v.findViewById(R.id.btnSelectCaretakers);
        caretakerChipContainer = v.findViewById(R.id.caretakerChipContainer);
        tvTotalFingerlingsCost = v.findViewById(R.id.tvTotalFingerlingsCost);
        Log.d(TAG, "Views initialized");
    }
    private void populateFields() {
        if (pond == null) {
            Log.d(TAG, "No pond data provided");
            return;
        }

        Log.d(TAG, "Pond data exists: " + pond.getName());

        // Basic pond info
        if (etPondName != null) etPondName.setText(pond.getName());
        if (etPondArea != null) {
            etPondArea.setText(String.valueOf(pond.getPondArea()));
            rawPondArea = pond.getPondArea();
        }
        if (etFishCount != null) etFishCount.setText(String.valueOf(pond.getFishCount()));
        if (etCostPerFish != null) etCostPerFish.setText(String.format(Locale.getDefault(), "%.2f", pond.getCostPerFish()));
        if (etMortalityRate != null) etMortalityRate.setText(String.valueOf(pond.getMortalityRate()));
        if (etDateCreated != null) etDateCreated.setText(pond.getDateStarted());
        if (etStockingDate != null) etStockingDate.setText(pond.getDateStocking());
        if (etHarvestDate != null) etHarvestDate.setText(pond.getDateHarvest());

        // Species spinner
        if (spinnerSpecies != null && pond.getBreed() != null) {
            String[] speciesList = getResources().getStringArray(R.array.fish_species);
            for (int i = 0; i < speciesList.length; i++) {
                if (speciesList[i].equalsIgnoreCase(pond.getBreed())) {
                    spinnerSpecies.setSelection(i);
                    break;
                }


            }
        }

        // Initialize caretakers
        // Determine if pond has fingerling stock
        hasFingerlingStock = pond.getFishCount() > 0 && pond.getCostPerFish() > 0;

// Apply edit restrictions if no fingerlings
        applyEditRestrictions();

        populateCaretakerIds();

        Log.d(TAG, "Fields populated");
        computeTotalCost(); // safe call
    }

    // This will parse pond's existing caretakers into selectedCaretakerIds
    private void populateCaretakerIds() {
        selectedCaretakerIds.clear();

        if (pond.getCaretakerIds() != null && !pond.getCaretakerIds().isEmpty()) {
            String[] ids = pond.getCaretakerIds().split(",");
            for (String id : ids) {
                selectedCaretakerIds.add(id.trim());
            }
            Log.d(TAG, "Parsed selectedCaretakerIds: " + selectedCaretakerIds);
        }

        refreshCaretakerChips();
    }


    private void applyEditRestrictions() {
        if (!hasFingerlingStock) {
            // Hide fields not yet relevant
            etFishCount.setVisibility(View.GONE);
            etCostPerFish.setVisibility(View.GONE);
            etMortalityRate.setVisibility(View.GONE);
            spinnerSpecies.setVisibility(View.GONE);
            etHarvestDate.setVisibility(View.GONE);

            // Keep stocking date visible
            etStockingDate.setVisibility(View.VISIBLE);

            // Optional: add info label
            TextView notice = new TextView(requireContext());
            notice.setText("No fingerlings stocked yet.\nYou can update pond info and set a stocking date.");
            notice.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            notice.setTextSize(14);
            notice.setGravity(Gravity.CENTER);
        } else {
            // Show all fields normally
            etFishCount.setVisibility(View.VISIBLE);
            etCostPerFish.setVisibility(View.VISIBLE);
            etMortalityRate.setVisibility(View.VISIBLE);
            spinnerSpecies.setVisibility(View.VISIBLE);
            etStockingDate.setVisibility(View.VISIBLE);
            etHarvestDate.setVisibility(View.VISIBLE);
        }

    }

    private void computeTotalCost() {

        if (tvTotalFingerlingsCost == null) {
            Log.d(TAG, "tvTotalFingerlingsCost is null, returning");
            return;
        }

        String fishCountStr = (etFishCount != null) ? etFishCount.getText().toString().trim() : "";
        String unitCostStr = (etCostPerFish != null) ? etCostPerFish.getText().toString().replace("₱", "").trim() : "";

        if (fishCountStr.isEmpty() || unitCostStr.isEmpty()) {
            tvTotalFingerlingsCost.setText("₱0.00");
            return;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double unitCost = Double.parseDouble(unitCostStr);
            double total = fishCount * unitCost;
            tvTotalFingerlingsCost.setText(String.format(Locale.getDefault(), "₱%.2f", total));
            Log.d(TAG, "Total cost computed: " + total);
        } catch (NumberFormatException e) {
            tvTotalFingerlingsCost.setText("₱0.00");
        }
    }


    private void setupSpeciesSpinner() {
        Log.d(TAG, "setupSpeciesSpinner() called");

        // Get species from resources
        String[] speciesList = getResources().getStringArray(R.array.fish_species);

        // Create an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,  // simple layout for items
                speciesList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach adapter to spinner
        spinnerSpecies.setAdapter(adapter);

        // Restore selected breed if pond exists
        if (pond != null && pond.getBreed() != null) {
            for (int i = 0; i < speciesList.length; i++) {
                if (speciesList[i].equalsIgnoreCase(pond.getBreed())) {
                    spinnerSpecies.setSelection(i);
                    break;
                }
            }
        }

        // Set listener
        spinnerSpecies.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Species selected: " + speciesList[position]);
                updateUnitCostBasedOnSpecies();
                checkStockingDensityRealtime();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private void setupTextWatchers() {
        etFishCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                computeTotalCost();
                checkStockingDensityRealtime();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        etCostPerFish.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                computeTotalCost();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        etPondArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String areaStr = s.toString().trim();
                if (!areaStr.isEmpty()) {
                    try {
                        rawPondArea = Double.parseDouble(areaStr);
                    } catch (NumberFormatException e) {
                        rawPondArea = 0;
                    }
                } else {
                    rawPondArea = 0;
                }
                // Call density check in real-times
                checkStockingDensityRealtime();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

    }


    private void setupDatePickers() {
        final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Date Created picker
        setDatePicker(etDateCreated, selectedDate -> {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dbFormat.parse(selectedDate));

                // Stocking = DateCreated + 14 days
                Calendar calStocking = (Calendar) cal.clone();
                calStocking.add(Calendar.DAY_OF_MONTH, 14);
                String stockingDate = dbFormat.format(calStocking.getTime());
                if (etStockingDate != null) etStockingDate.setText(stockingDate);

                // Harvest = Stocking + 180 days
                Calendar calHarvest = (Calendar) calStocking.clone();
                calHarvest.add(Calendar.DAY_OF_MONTH, 180);
                String harvestDate = dbFormat.format(calHarvest.getTime());
                if (etHarvestDate != null) etHarvestDate.setText(harvestDate);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Stocking Date picker
        setDatePicker(etStockingDate, selectedDate -> {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dbFormat.parse(selectedDate));

                // DateCreated = Stocking - 14 days
                Calendar calCreated = (Calendar) cal.clone();
                calCreated.add(Calendar.DAY_OF_MONTH, -14);
                String createdDate = dbFormat.format(calCreated.getTime());
                if (etDateCreated != null) etDateCreated.setText(createdDate);

                // Harvest = Stocking + 180 days
                Calendar calHarvest = (Calendar) cal.clone();
                calHarvest.add(Calendar.DAY_OF_MONTH, 180);
                String harvestDate = dbFormat.format(calHarvest.getTime());
                if (etHarvestDate != null) etHarvestDate.setText(harvestDate);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Harvest Date picker
        setDatePicker(etHarvestDate, selectedDate -> {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dbFormat.parse(selectedDate));

                // Stocking = Harvest - 180 days
                Calendar calStocking = (Calendar) cal.clone();
                calStocking.add(Calendar.DAY_OF_MONTH, -180);
                String stockingDate = dbFormat.format(calStocking.getTime());
                if (etStockingDate != null) etStockingDate.setText(stockingDate);

                // DateCreated = Stocking - 14 days
                Calendar calCreated = (Calendar) calStocking.clone();
                calCreated.add(Calendar.DAY_OF_MONTH, -14);
                String createdDate = dbFormat.format(calCreated.getTime());
                if (etDateCreated != null) etDateCreated.setText(createdDate);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void setDatePicker(EditText target, OnDateSelectedListener listener) {
        target.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                target.setText(date);
                if (listener != null) listener.onDateSelected(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private interface OnDateSelectedListener {
        void onDateSelected(String selectedDate);
    }


    private void saveChanges() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Save")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton("Yes", (dialog, which) -> {

                    // 1️⃣ Safely read EditText values, fallback to existing pond data
                    String pondName = etPondName.getText().toString().trim();
                    if (pondName.isEmpty()) pondName = pond.getName();

                    String pondAreaStr = etPondArea.getText().toString().trim();
                    double pondArea = pondAreaStr.isEmpty() ? pond.getPondArea() : Double.parseDouble(pondAreaStr);

                    int fishCount = pond.getFishCount();
                    double costPerFish = pond.getCostPerFish();
                    double mortalityRate = pond.getMortalityRate();

                    if (hasFingerlingStock) {
                        String fishCountStr = etFishCount.getText().toString().trim();
                        if (!fishCountStr.isEmpty()) fishCount = Integer.parseInt(fishCountStr);

                        String costStr = etCostPerFish.getText().toString().trim();
                        if (!costStr.isEmpty()) costPerFish = Double.parseDouble(costStr);

                        String mortalityStr = etMortalityRate.getText().toString().trim();
                        if (!mortalityStr.isEmpty()) mortalityRate = Double.parseDouble(mortalityStr);
                    }


                    String dateCreated = etDateCreated.getText().toString().trim();
                    if (dateCreated.isEmpty()) dateCreated = pond.getDateStarted();

                    String stockingDate = etStockingDate.getText().toString().trim();
                    if (stockingDate.isEmpty()) stockingDate = pond.getDateStocking();

                    String harvestDate = etHarvestDate.getText().toString().trim();
                    if (harvestDate.isEmpty()) harvestDate = pond.getDateHarvest();

                    String selectedBreed = pond.getBreed();

                    if (spinnerSpecies.getVisibility() == View.VISIBLE && spinnerSpecies.getSelectedItem() != null) {
                        String spinnerValue = spinnerSpecies.getSelectedItem().toString();
                        // only update if user actually picked something valid
                        if (!spinnerValue.equalsIgnoreCase("Select species")) {
                            selectedBreed = spinnerValue;
                        }
                    } else {
                        // Spinner hidden → no change to species
                        selectedBreed = pond.getBreed();
                    }



                    // 3️⃣ Caretakers
                    if (selectedCaretakerIds.isEmpty()) {
                        Toast.makeText(requireContext(), "Please assign at least one caretaker.", Toast.LENGTH_SHORT).show();
                        etCaretakers.setError("Required");
                        return;
                    } else {
                        etCaretakers.setError(null);
                    }


                    // 4️⃣ Stocking density check
                    rawPondArea = pondArea; // update raw pond area for validation
                    if (!checkStockingDensity()) return;

                    // 5️⃣ Update pond model
                    pond.setName(pondName);
                    pond.setPondArea(pondArea);
                    pond.setFishCount(fishCount);
                    pond.setCostPerFish(costPerFish);
                    pond.setMortalityRate(mortalityRate);
                    pond.setDateStarted(dateCreated);
                    pond.setDateStocking(stockingDate);
                    pond.setDateHarvest(harvestDate);
                    pond.setBreed(selectedBreed);

                    pond.setCaretakerName(getSelectedCaretakerNames()); // 🟢 store names for display
                    pond.setCaretakerIds(String.join(",", selectedCaretakerIds)); // 🟢 store IDs for server


                    // 6️⃣ Send to server
                    updatePondOnServer(pond);

                })
                .setNegativeButton("No", null)
                .show();
    }

    private String getSelectedCaretakerNames() {
        List<String> selectedNames = new ArrayList<>();
        for (int i = 0; i < caretakerIds.size(); i++) {
            if (selectedCaretakerIds.contains(caretakerIds.get(i))) {
                selectedNames.add(caretakerNames.get(i));
            }
        }
        return String.join(",", selectedNames);
    }



    // Validate required fields before saving
    private boolean validateInputs() {
        if (etPondName.getText().toString().trim().isEmpty() ||
                etPondArea.getText().toString().trim().isEmpty() ||
                etFishCount.getText().toString().trim().isEmpty() ||
                etCostPerFish.getText().toString().trim().isEmpty() ||
                etMortalityRate.getText().toString().trim().isEmpty() ||
                etDateCreated.getText().toString().trim().isEmpty() ||
                etStockingDate.getText().toString().trim().isEmpty() ||
                etHarvestDate.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all pond details.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedCaretakerIds.isEmpty()) {
            Toast.makeText(requireContext(), "Please assign at least one caretaker.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!checkStockingDensity()) {
            return false;

        }

        return true;
    }

    private void updatePondOnServer(PondModel pond) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_pond.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                // Build POST parameters dynamically
                Map<String, String> paramsMap = new HashMap<>();
                if (pond.getName() != null && !pond.getName().isEmpty())
                    paramsMap.put("name", pond.getName());
                if (pond.getBreed() != null && !pond.getBreed().isEmpty())
                    paramsMap.put("species", pond.getBreed());
                if (pond.getFishCount() > 0)
                    paramsMap.put("fish_count", String.valueOf(pond.getFishCount()));
                if (pond.getCostPerFish() >= 0)
                    paramsMap.put("cost_per_fish", String.valueOf(pond.getCostPerFish()));
                if (pond.getPondArea() > 0)
                    paramsMap.put("pond_area", String.valueOf(pond.getPondArea()));
                if (pond.getMortalityRate() >= 0)
                    paramsMap.put("mortality_rate", String.valueOf(pond.getMortalityRate()));
                if (pond.getDateStarted() != null && !pond.getDateStarted().isEmpty())
                    paramsMap.put("date_created", pond.getDateStarted());
                if (pond.getDateStocking() != null && !pond.getDateStocking().isEmpty())
                    paramsMap.put("stocking_date", pond.getDateStocking());
                if (pond.getDateHarvest() != null && !pond.getDateHarvest().isEmpty())
                if (pond.getDateHarvest() != null && !pond.getDateHarvest().isEmpty())
                    paramsMap.put("estimated_harvest", pond.getDateHarvest());
                if (!selectedCaretakerIds.isEmpty())
                    paramsMap.put("assigned_caretakers", String.join(",", selectedCaretakerIds));

                // Add pond name again for ID lookup on server (important)
                paramsMap.put("id", String.valueOf(pond.getId()));

                // Encode parameters
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                    if (postData.length() != 0) postData.append("&");
                    postData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    postData.append("=");
                    postData.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.toString().getBytes());
                    os.flush();
                }

                // Read server response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }

                // Handle response on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String resp = response.toString().trim();
                        try {
                            JSONObject json = new JSONObject(resp);
                            String status = json.optString("status");
                            String message = json.optString("message");

                            if (status.equalsIgnoreCase("success")) {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

                                if (listener != null) listener.onPondUpdated(pond);

                                // Dismiss the dialog first
                                dismiss();

                                // Reload PondDashboardActivity
                                if (getActivity() != null) {
                                    Intent intent = new Intent(getActivity(), PondDashboardActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed: " + message, Toast.LENGTH_LONG).show();
                                Log.d("UpdateFail", message);
                            }

                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Invalid server response.", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Error updating pond.", Toast.LENGTH_SHORT).show());
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }





    private void updateUnitCostBasedOnSpecies() {
        String species = spinnerSpecies.getSelectedItem() != null
                ? spinnerSpecies.getSelectedItem().toString()
                : "";
        double cost = species.equalsIgnoreCase("Tilapia") ? 0.35 : species.equalsIgnoreCase("Bangus") ? 0.50 : 0.0;
        etCostPerFish.setText(String.format(Locale.getDefault(), "%.2f", cost));
    }

    private void checkStockingDensityRealtime() {
        String breed = spinnerSpecies.getSelectedItem() != null ? spinnerSpecies.getSelectedItem().toString() : "";
        String fishCountStr = etFishCount.getText().toString().trim();

        if (fishCountStr.isEmpty() || rawPondArea <= 0) {
            etFishCount.setError(null);
            return;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double density = fishCount / rawPondArea;
            double minRecommended = 0, maxAllowed = 0;

            switch (breed) {
                case "Tilapia": minRecommended=2.0; maxAllowed=4.0; break;
                case "Bangus": minRecommended=0.8; maxAllowed=1.2; break;
            }

            double minFishCount = minRecommended * rawPondArea;
            double maxFishCount = maxAllowed * rawPondArea;

            if (density > maxAllowed) etFishCount.setError(
                    String.format(Locale.getDefault(),"⚠️ Overstocked! Recommended: %.0f–%.0f fish.", minFishCount,maxFishCount));
            else if (density < minRecommended) etFishCount.setError(
                    String.format(Locale.getDefault(),"Too few fish. Recommended: %.0f–%.0f fish.", minFishCount,maxFishCount));
            else etFishCount.setError(null);

        } catch (NumberFormatException e) {
            etFishCount.setError(null);
        }
    }

    private boolean checkStockingDensity() {
        String breed = spinnerSpecies.getSelectedItem() != null ? spinnerSpecies.getSelectedItem().toString() : "";
        String fishCountStr = etFishCount.getText().toString().trim();

        if (fishCountStr.isEmpty() || rawPondArea <= 0) {
            Toast.makeText(getContext(), "Missing data: check fish count or pond area.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double density = fishCount / rawPondArea;
            double minRecommended=0, maxAllowed=0;

            switch (breed) {
                case "Tilapia": minRecommended=2.0; maxAllowed=4.0; break;
                case "Bangus": minRecommended=0.8; maxAllowed=1.2; break;
            }

            if (density > maxAllowed) {
                Toast.makeText(getContext(),
                        String.format(Locale.getDefault(),"❌ Overstocked! Recommended: %.0f–%.0f fish.", minRecommended*rawPondArea, maxAllowed*rawPondArea),
                        Toast.LENGTH_LONG).show();
                return false;
            } else if (density < minRecommended) {
                Toast.makeText(getContext(),
                        String.format(Locale.getDefault(),"⚠️ Understocked! Recommended: %.0f–%.0f fish.", minRecommended*rawPondArea, maxAllowed*rawPondArea),
                        Toast.LENGTH_LONG).show();
                return false;
            }
            return true;

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(),"Invalid fish count format.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }



    private void refreshCaretakerChips() {
        Log.d(TAG, "refreshCaretakerChips() called");
        caretakerChipContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (String caretakerId : selectedCaretakerIds) {
            // Try to get the name from caretakerIds → caretakerNames mapping
            int index = caretakerIds.indexOf(caretakerId);
            String displayName = (index >= 0 && index < caretakerNames.size())
                    ? caretakerNames.get(index)
                    : caretakerId; // fallback if not found

            View chip = inflater.inflate(R.layout.item_caretaker_chip, caretakerChipContainer, false);
            TextView tvName = chip.findViewById(R.id.tvCaretakerName);
            TextView btnRemove = chip.findViewById(R.id.btnRemoveChip);

            tvName.setText(displayName);

            btnRemove.setOnClickListener(v -> {
                selectedCaretakerIds.remove(caretakerId);
                refreshCaretakerChips();
            });

            caretakerChipContainer.addView(chip);
        }
    }



    private void setupCaretakerButton() {
        loadCaretakersFromServer(); // no parameter
    }

    private void loadCaretakersFromServer() {
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

// 🟩 Once caretakers are loaded, re-populate selected IDs and refresh chips
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        populateCaretakerIds(); // 🔹 now we can map IDs to names
                        refreshCaretakerChips();
                    });
                }


                if (isAdded() && getActivity() != null && btnSelectCaretakers != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSelectCaretakers.setEnabled(true);
                        btnSelectCaretakers.setText("Assign Caretakers");

                        btnSelectCaretakers.setOnClickListener(v -> {
                            boolean[] checkedItems = new boolean[caretakerIds.size()];
                            for (int i = 0; i < caretakerIds.size(); i++)
                                checkedItems[i] = selectedCaretakerIds.contains(caretakerIds.get(i));

                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Select Caretakers")
                                    .setMultiChoiceItems(caretakerNames.toArray(new String[0]), checkedItems,
                                            (dialog, which, isChecked) -> {
                                                String id = caretakerIds.get(which);
                                                if (isChecked) selectedCaretakerIds.add(id);
                                                else selectedCaretakerIds.remove(id);
                                            })
                                    .setPositiveButton("OK", (dialog, which) -> refreshCaretakerChips())
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded() && getActivity() != null && btnSelectCaretakers != null) {
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




}
