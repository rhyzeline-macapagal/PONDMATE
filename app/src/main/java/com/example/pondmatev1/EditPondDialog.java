package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;



/**
 * EditPondDialog — updated to use a RecyclerView-based caretaker chooser instead of chips.
 */
public class EditPondDialog extends DialogFragment {

    private EditText etPondName, etPondArea, etFishCount, etCostPerFish, etMortalityRate;
    private EditText etDateCreated, etStockingDate, etHarvestDate;
    private Spinner spinnerSpecies;
    private Button btnSave, btnCancel;
    private TextView btnClose, tvCaretakers;

    private TextView tvTotalFingerlingsCost;

    private boolean hasFingerlingStock = false;

    private RecyclerView rvCaretakers;

    private PondModel pond; // existing pond data
    private OnPondUpdatedListener listener;

    private String rawDateCreatedForDB = "";
    private String rawStockingDateForDB = "";
    private String rawHarvestDateForDB = "";
    private final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()); // January 1, 2025

    private ArrayList<CaretakerModel> caretakerModels = new ArrayList<>();

    private static final String TAG = "EditPondDialog";

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private double rawPondArea = 0;

    // Adapter-backed state (checked booleans) used by the RecyclerView chooser
    private final List<Boolean> caretakerCheckedState = new ArrayList<>();

    private TextView tvCaretakerLabel;
    private Button btnSelectCaretakers;
    private FlexboxLayout caretakerChipContainer;

    private ArrayList<String> caretakerNames = new ArrayList<>();
    private ArrayList<String> caretakerIds = new ArrayList<>();
    private ArrayList<String> selectedCaretakerIds = new ArrayList<>();
    private Map<String, String> caretakerMap = new HashMap<>();
    private String caretakerDisplay;

    private List<Integer> assignedCaretakerIds = new ArrayList<>();
    private PondSyncManager pondSyncManager;

    public interface OnCaretakersUpdatedListener {
        void onCaretakersUpdated(ArrayList<String> selectedCaretakerIds);
    }

    private OnCaretakersUpdatedListener caretakersListener;
    public void setOnCaretakersUpdatedListener(OnCaretakersUpdatedListener listener) {
        this.caretakersListener = listener;
    }

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
        pondSyncManager = new PondSyncManager();

        hasFingerlingStock = pond.getFishCount() > 0 && pond.getCostPerFish() > 0;

        View view;
        if (hasFingerlingStock) {
            // FULL LAYOUT (with fish count, species, etc.)
            view = inflater.inflate(R.layout.dialog_edit_pond, null);
        } else {
            // MINIMAL LAYOUT (for ponds NOT yet stocked)
            view = inflater.inflate(R.layout.dialog_edit_pond_minimal, null);
        }
        Log.d("POND", "caretakerIds stored: " + pond.getCaretakerIds());

        initViews(view);

        setupAssignedCaretakers(pond.getId());
        if (assignedCaretakerIds != null && !assignedCaretakerIds.isEmpty()) {
            // Convert IDs to names if needed, then display
            String displayNames = getCaretakerNamesFromIds(assignedCaretakerIds);
            tvCaretakers.setText(displayNames);
        }

        caretakerChipContainer = view.findViewById(R.id.caretakerChipContainer);
        tvCaretakerLabel = view.findViewById(R.id.tvCaretakerLabel);
        btnSelectCaretakers = view.findViewById(R.id.btnSelectCaretakers);
        loadCaretakersAndDisplay();


        if (hasFingerlingStock && spinnerSpecies != null) {
            setupSpeciesSpinner();
        }
        populateFields();
        setupTextWatchers();
        setupDatePickers();

        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveChanges());

        btnSelectCaretakers.setEnabled(false);
        btnSelectCaretakers.setText("Loading caretakers...");

        loadCaretakersFromServer(() -> {
            // 1️⃣ Map all available caretakers
            populateCaretakerMap(caretakerIds, caretakerNames);

            // 3️⃣ Refresh chips
            refreshCaretakerChips();

            // 4️⃣ Enable button
            btnSelectCaretakers.setEnabled(true);
            btnSelectCaretakers.setText("Select Caretakers");

            // 5️⃣ Set selection dialog
            btnSelectCaretakers.setOnClickListener(v -> openCaretakerSelectionDialog());
        });


        builder.setView(view);
        return builder.create();
    }

    private void initViews(View v) {
        if (v == null) return;

        // --- Initialize all views first ---
        etPondName = v.findViewById(R.id.etPondName);
        etPondArea = v.findViewById(R.id.etPondArea);
        etFishCount = v.findViewById(R.id.etFishCount);
        etCostPerFish = v.findViewById(R.id.etCostPerFish);
        etMortalityRate = v.findViewById(R.id.etMortalityRate);
        etDateCreated = v.findViewById(R.id.etDateCreated);
        etStockingDate = v.findViewById(R.id.etStockingDate);
        etHarvestDate = v.findViewById(R.id.etHarvestDate);
        tvCaretakers = v.findViewById(R.id.etCaretakers);

        rvCaretakers = v.findViewById(R.id.rvCaretakers);
        spinnerSpecies = v.findViewById(R.id.spinnerBreed);
        btnSave = v.findViewById(R.id.btnSave);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnClose = v.findViewById(R.id.btnClose);
        tvTotalFingerlingsCost = v.findViewById(R.id.tvTotalFingerlingsCost);

        // --- Safe to use views now ---
        if (tvCaretakers != null) tvCaretakers.setText("Loading caretakers...");

        if (caretakerDisplay != null && !caretakerDisplay.isEmpty()) {
            tvCaretakers.setText(caretakerDisplay);
            tvCaretakers.setEnabled(false);
        }

        if (etPondName != null) etPondName.setText("");
        if (etPondArea != null) etPondArea.setText("");

        if (spinnerSpecies != null) {
            spinnerSpecies.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{ "Select species" }));
        }

        if (tvTotalFingerlingsCost != null) {
            tvTotalFingerlingsCost.setText("₱0.00");
        }
    }


    private String getCaretakerNamesFromIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return "";
        // Replace with your logic: Map<Integer, String> caretakerMap
        List<String> names = new ArrayList<>();
        for (Integer id : ids) {
            String name = caretakerMap.get(id); // Make sure caretakerMap exists
            if (name != null) names.add(name);
        }
        return String.join(", ", names);
    }



    private void populateFields() {
        if (pond == null) return;

        // --- Basic fields (exist in both layouts) ---
        if (etPondName != null)
            etPondName.setText(pond.getName() != null ? pond.getName() : "");

        if (etPondArea != null)
            etPondArea.setText(pond.getPondArea() > 0 ? String.valueOf(pond.getPondArea()) : "");

        if (tvCaretakers != null)
            tvCaretakers.setText(pond.getCaretakerName() != null ? pond.getCaretakerName() : "");

        // --- Species ---
        if (spinnerSpecies != null && pond.getBreed() != null && !pond.getBreed().isEmpty()) {
            setupSpeciesSpinner(); // will also select the breed
        }

        // --- Only populate fingerling-related fields if stocked ---
        if (hasFingerlingStock) {
            if (etFishCount != null) etFishCount.setText(String.valueOf(pond.getFishCount()));
            if (etCostPerFish != null) etCostPerFish.setText(String.valueOf(pond.getCostPerFish()));
            if (etMortalityRate != null) etMortalityRate.setText(String.valueOf(pond.getMortalityRate()));

            if (tvTotalFingerlingsCost != null) {
                double totalCost = pond.getFishCount() * pond.getCostPerFish();
                tvTotalFingerlingsCost.setText(String.format(Locale.getDefault(), "₱%.2f", totalCost));
            }
        }

        // --- Dates ---
        trySetDate(etDateCreated, pond.getDateStarted());
        trySetDate(etStockingDate, pond.getDateStocking());
        trySetDate(etHarvestDate, pond.getDateHarvest());
    }

    private void trySetDate(EditText et, String dbDate) {
        if (et != null && dbDate != null && !dbDate.isEmpty()) {
            try {
                Date date = dbFormat.parse(dbDate);
                et.setText(displayFormat.format(date));
            } catch (Exception e) {
                et.setText(dbDate); // fallback
            }
        }
    }

    private void setupSpeciesSpinner() {
        if (spinnerSpecies == null) return;
        String[] speciesList = getResources().getStringArray(R.array.fish_species);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, speciesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecies.setAdapter(adapter);

        // Pre-select if breed exists
        if (pond != null && pond.getBreed() != null && !pond.getBreed().isEmpty()) {
            for (int i = 0; i < speciesList.length; i++) {
                if (speciesList[i].equalsIgnoreCase(pond.getBreed())) {
                    spinnerSpecies.setSelection(i);
                    break;
                }
            }
        }

        spinnerSpecies.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUnitCostBasedOnSpecies();
                checkStockingDensityRealtime();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
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


    private void setupTextWatchers() {
        if (etFishCount != null) {
            etFishCount.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    computeTotalCost();
                    checkStockingDensityRealtime();
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }

        if (etCostPerFish != null) {
            etCostPerFish.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    computeTotalCost();
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }

        if (etPondArea != null) {
            etPondArea.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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
                    checkStockingDensityRealtime();
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }
    }
    private void setupDatePickers() {

        // Date Created picker
        setDatePicker(etDateCreated, selectedDate -> {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dbFormat.parse(selectedDate));

                // Save DB-format
                rawDateCreatedForDB = dbFormat.format(cal.getTime());

                // Display formatted
                if (etDateCreated != null)
                    etDateCreated.setText(displayFormat.format(cal.getTime()));

                // Stocking = DateCreated + 14 days
                Calendar calStocking = (Calendar) cal.clone();
                calStocking.add(Calendar.DAY_OF_MONTH, 14);
                rawStockingDateForDB = dbFormat.format(calStocking.getTime());
                if (etStockingDate != null) etStockingDate.setText(displayFormat.format(calStocking.getTime()));

                // Harvest = Stocking + 180 days
                Calendar calHarvest = (Calendar) calStocking.clone();
                calHarvest.add(Calendar.DAY_OF_MONTH, 180);
                rawHarvestDateForDB = dbFormat.format(calHarvest.getTime());
                if (etHarvestDate != null) etHarvestDate.setText(displayFormat.format(calHarvest.getTime()));

            } catch (Exception e) { e.printStackTrace(); }
        });

        // Stocking Date picker
        setDatePicker(etStockingDate, selectedDate -> {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dbFormat.parse(selectedDate));

                rawStockingDateForDB = dbFormat.format(cal.getTime());
                if (etStockingDate != null) etStockingDate.setText(displayFormat.format(cal.getTime()));

                // DateCreated = Stocking - 14 days
                Calendar calCreated = (Calendar) cal.clone();
                calCreated.add(Calendar.DAY_OF_MONTH, -14);
                rawDateCreatedForDB = dbFormat.format(calCreated.getTime());
                if (etDateCreated != null) etDateCreated.setText(displayFormat.format(calCreated.getTime()));

                // Harvest = Stocking + 180 days
                Calendar calHarvest = (Calendar) cal.clone();
                calHarvest.add(Calendar.DAY_OF_MONTH, 180);
                rawHarvestDateForDB = dbFormat.format(calHarvest.getTime());
                if (etHarvestDate != null) etHarvestDate.setText(displayFormat.format(calHarvest.getTime()));

            } catch (Exception e) { e.printStackTrace(); }
        });

        // Harvest Date picker
        setDatePicker(etHarvestDate, selectedDate -> {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dbFormat.parse(selectedDate));

                rawHarvestDateForDB = dbFormat.format(cal.getTime());
                if (etHarvestDate != null) etHarvestDate.setText(displayFormat.format(cal.getTime()));

                // Stocking = Harvest - 180 days
                Calendar calStocking = (Calendar) cal.clone();
                calStocking.add(Calendar.DAY_OF_MONTH, -180);
                rawStockingDateForDB = dbFormat.format(calStocking.getTime());
                if (etStockingDate != null) etStockingDate.setText(displayFormat.format(calStocking.getTime()));

                // DateCreated = Stocking - 14 days
                Calendar calCreated = (Calendar) calStocking.clone();
                calCreated.add(Calendar.DAY_OF_MONTH, -14);
                rawDateCreatedForDB = dbFormat.format(calCreated.getTime());
                if (etDateCreated != null) etDateCreated.setText(displayFormat.format(calCreated.getTime()));

            } catch (Exception e) { e.printStackTrace(); }
        });
    }


    private void setDatePicker(EditText target, OnDateSelectedListener listener) {
        if (target == null) return;
        final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

        target.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                Calendar pickedCal = Calendar.getInstance();
                pickedCal.set(year, month, day);
                String dbDate = dbFormat.format(pickedCal.getTime());           // To send to database
                String displayDate = displayFormat.format(pickedCal.getTime()); // For EditText display
                target.setText(displayDate);
                if (listener != null) listener.onDateSelected(dbDate);          // Send DB date to listener
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }


    private interface OnDateSelectedListener {
        void onDateSelected(String selectedDate);
    }

    private void saveChanges() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Save")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton("Yes", null)
                .setNegativeButton("No", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {

                // 1️⃣ Safely read EditText values
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

                // 2️⃣ Use internal DB-format dates instead of EditText text
                String dateCreated = (rawDateCreatedForDB != null && !rawDateCreatedForDB.isEmpty())
                        ? rawDateCreatedForDB
                        : pond.getDateStarted();

                String stockingDate = (rawStockingDateForDB != null && !rawStockingDateForDB.isEmpty())
                        ? rawStockingDateForDB
                        : pond.getDateStocking();

                String harvestDate = (rawHarvestDateForDB != null && !rawHarvestDateForDB.isEmpty())
                        ? rawHarvestDateForDB
                        : pond.getDateHarvest();

                // 3️⃣ Breed
                String selectedBreed = pond.getBreed();
                if (spinnerSpecies != null && spinnerSpecies.getVisibility() == View.VISIBLE) {
                    Object selectedItem = spinnerSpecies.getSelectedItem();
                    if (selectedItem != null) {
                        String spinnerValue = selectedItem.toString();
                        if (!spinnerValue.equalsIgnoreCase("Select species")) {
                            selectedBreed = spinnerValue;
                        }
                    }
                }


                // 4️⃣ Caretaker validation
                if (selectedCaretakerIds.isEmpty()) {
                    Toast.makeText(requireContext(), "Please assign at least one caretaker.", Toast.LENGTH_SHORT).show();
                    if (tvCaretakers != null) tvCaretakers.setError("Required");
                    return;
                }

                // 5️⃣ Stocking density check
                rawPondArea = pondArea;
                if (!checkStockingDensity()) return;

                // 6️⃣ Update pond model
                pond.setName(pondName);
                pond.setPondArea(pondArea);
                pond.setFishCount(fishCount);
                pond.setCostPerFish(costPerFish);
                pond.setMortalityRate(mortalityRate);
                pond.setDateStarted(dateCreated);
                pond.setDateStocking(stockingDate);
                pond.setDateHarvest(harvestDate);
                pond.setBreed(selectedBreed);
                pond.setCaretakerName(getSelectedCaretakerNames());
                pond.setCaretakerIds(new ArrayList<>(selectedCaretakerIds));

                // 7️⃣ Send to server
                updatePondOnServer(pond);

                dialog.dismiss();
            });
        });

        dialog.show();
    }


    private String getSelectedCaretakerNames() {
        if (selectedCaretakerIds.isEmpty() || caretakerMap.isEmpty()) {
            return "";
        }

        List<String> selectedNames = new ArrayList<>();
        for (String id : selectedCaretakerIds) {
            String name = caretakerMap.get(id);
            if (name != null && !name.trim().isEmpty()) {
                selectedNames.add(name);
            }
        }

        return String.join(", ", selectedNames);
    }


    private void populateCaretakerMap(List<String> ids, List<String> names) {
        caretakerMap.clear();
        if (ids != null && names != null && ids.size() == names.size()) {
            for (int i = 0; i < ids.size(); i++) {
                caretakerMap.put(ids.get(i), names.get(i));
            }
        }
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
                    paramsMap.put("estimated_harvest", pond.getDateHarvest());
                if (!selectedCaretakerIds.isEmpty())
                    paramsMap.put("assigned_caretakers", String.join(",", selectedCaretakerIds));

                paramsMap.put("id", String.valueOf(pond.getId()));

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

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }

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
        String fishCountStr = (etFishCount != null) ? etFishCount.getText().toString().trim() : "";

        if (fishCountStr.isEmpty() || rawPondArea <= 0) {
            if (etFishCount != null) etFishCount.setError(null);
            return;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double density = fishCount / rawPondArea;
            double minRecommended = 0, maxAllowed = 0;

            switch (breed) {
                case "Tilapia": minRecommended = 2.0; maxAllowed = 4.0; break;
                case "Bangus": minRecommended = 0.8; maxAllowed = 1.2; break;
            }

            double minFishCount = minRecommended * rawPondArea;
            double maxFishCount = maxAllowed * rawPondArea;

            if (density > maxAllowed && etFishCount != null)
                etFishCount.setError(String.format(Locale.getDefault(),"⚠️ Overstocked! Recommended: %.0f–%.0f fish.", minFishCount, maxFishCount));
            else if (density < minRecommended && etFishCount != null)
                etFishCount.setError(String.format(Locale.getDefault(),"Too few fish. Recommended: %.0f–%.0f fish.", minFishCount, maxFishCount));
            else if (etFishCount != null) etFishCount.setError(null);

        } catch (NumberFormatException e) {
            if (etFishCount != null) etFishCount.setError(null);
        }
    }

    private boolean checkStockingDensity() {
        String breed = spinnerSpecies.getSelectedItem() != null ? spinnerSpecies.getSelectedItem().toString() : "";
        String fishCountStr = (etFishCount != null) ? etFishCount.getText().toString().trim() : "";

        if (fishCountStr.isEmpty() || rawPondArea <= 0) {
            Toast.makeText(getContext(), "Missing data: check fish count or pond area.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double fishCount = Double.parseDouble(fishCountStr);
            double density = fishCount / rawPondArea;
            double minRecommended = 0, maxAllowed = 0;

            switch (breed) {
                case "Tilapia": minRecommended = 2.0; maxAllowed = 4.0; break;
                case "Bangus": minRecommended = 0.8; maxAllowed = 1.2; break;
            }

            if (density > maxAllowed) {
                Toast.makeText(getContext(),
                        String.format(Locale.getDefault(),"❌ Overstocked! Recommended: %.0f–%.0f fish.", minRecommended * rawPondArea, maxAllowed * rawPondArea),
                        Toast.LENGTH_LONG).show();
                return false;
            } else if (density < minRecommended) {
                Toast.makeText(getContext(),
                        String.format(Locale.getDefault(),"⚠️ Understocked! Recommended: %.0f–%.0f fish.", minRecommended * rawPondArea, maxAllowed * rawPondArea),
                        Toast.LENGTH_LONG).show();
                return false;
            }
            return true;

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(),"Invalid fish count format.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void loadCaretakersFromServer(Runnable onComplete) {
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
                Log.d("LOAD_CARETAKERS", "Caretaker IDs: " + caretakerIds);
                Log.d("LOAD_CARETAKERS", "Caretaker Names: " + caretakerNames);

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSelectCaretakers.setEnabled(true);
                        btnSelectCaretakers.setText("Select Caretakers");
                        btnSelectCaretakers.setOnClickListener(v -> openCaretakerSelectionDialog());

                        // Run your custom callback if provided
                        if (onComplete != null) onComplete.run();
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


    private void openCaretakerSelectionDialog() {
        boolean[] checkedItems = new boolean[caretakerIds.size()];

        for (int i = 0; i < caretakerIds.size(); i++) {
            checkedItems[i] = selectedCaretakerIds.contains(caretakerIds.get(i));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Caretakers")
                .setMultiChoiceItems(caretakerNames.toArray(new String[0]), checkedItems,
                        (dialog, which, isChecked) -> {
                            // Update selection immediately
                            String id = caretakerIds.get(which);
                            if (isChecked) {
                                if (!selectedCaretakerIds.contains(id)) selectedCaretakerIds.add(id);
                            } else {
                                selectedCaretakerIds.remove(id);
                            }
                        })
                .setPositiveButton("OK", (dialog, which) -> {
                    // Refresh summary and chips after selection is confirmed
                    refreshCaretakerChips();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupAssignedCaretakers(String pondId) {
        assignedCaretakerIds.clear();

        PondSyncManager.postRequest(
                "get_assigned_caretakers.php",
                "pond_id=" + pondId,
                new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        try {
                            String jsonString = response.toString();
                            JSONObject json = new JSONObject(jsonString);
                            String status = json.optString("status", "error");

                            if ("success".equalsIgnoreCase(status)) {
                                JSONArray dataArray = json.getJSONArray("data");
                                assignedCaretakerIds.clear();

                                for (int i = 0; i < dataArray.length(); i++) {
                                    assignedCaretakerIds.add(dataArray.getInt(i));
                                }

                                Log.d("CARETAKER_DEBUG", "Assigned caretakers: " + assignedCaretakerIds);

                                // ✅ Update UI on main thread safely
                                requireActivity().runOnUiThread(() -> {
                                    if (!isAdded()) return;

                                    if (tvCaretakers != null) {
                                        String displayNames = getCaretakerNamesFromIds(assignedCaretakerIds);
                                        tvCaretakers.setText(displayNames);
                                    }

                                    refreshCaretakerChips();
                                });

                            } else {
                                Log.d("CARETAKER_DEBUG", "No caretaker IDs found: " + json.optString("message"));
                                requireActivity().runOnUiThread(() -> {
                                    if (!isAdded()) return;

                                    if (tvCaretakers != null) {
                                        tvCaretakers.setText(""); // Clear field if none
                                    }

                                    refreshCaretakerChips();
                                });
                            }
                        } catch (JSONException e) {
                            Log.e("CARETAKER_DEBUG", "Failed to parse caretaker IDs: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("CARETAKER_DEBUG", "Failed to load assigned caretakers: " + error);
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;

                            if (tvCaretakers != null) {
                                tvCaretakers.setText(""); // Clear field on error
                            }

                            refreshCaretakerChips();
                        });
                    }
                }
        );
    }



    private class CaretakerAdapter extends RecyclerView.Adapter<CaretakerAdapter.CaretakerVH> {
        private final List<Boolean> localChecked;

        CaretakerAdapter() {
            // initialize localChecked to match caretakerIds size and pre-check from selectedCaretakerIds
            localChecked = new ArrayList<>();
            for (int i = 0; i < caretakerIds.size(); i++) {
                boolean checked = (i < caretakerCheckedState.size()) ? caretakerCheckedState.get(i)
                        : selectedCaretakerIds.contains(caretakerIds.get(i));
                localChecked.add(checked);
            }
        }

        boolean getChecked(int pos) {
            return pos >= 0 && pos < localChecked.size() && localChecked.get(pos);
        }

        @NonNull
        @Override
        public CaretakerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CheckBox checkBox = new CheckBox(parent.getContext());
            int pad = (int)(8 * parent.getContext().getResources().getDisplayMetrics().density);
            checkBox.setPadding(pad, pad, pad, pad);
            return new CaretakerVH(checkBox);
        }

        @Override
        public void onBindViewHolder(@NonNull CaretakerVH holder, int position) {
            String name = (position < caretakerNames.size()) ? caretakerNames.get(position) : ("Caretaker " + position);
            holder.checkBox.setText(name);

            // Avoid firing listener when setting checked state: remove listener, set checked, then re-attach
            holder.checkBox.setOnCheckedChangeListener(null);
            boolean checked = position < localChecked.size() ? localChecked.get(position) : false;
            holder.checkBox.setChecked(checked);

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localChecked.set(position, isChecked);
                // Immediately reflect selection in the central selection set so UI summary stays correct
                String id = caretakerIds.get(position);
                if (isChecked) selectedCaretakerIds.add(id);
                else selectedCaretakerIds.remove(id);

                // Update caretakers summary field
                if (tvCaretakers != null) tvCaretakers.setText(getSelectedCaretakerNames());
            });
        }

        @Override
        public int getItemCount() {
            return caretakerNames.size();
        }

        class CaretakerVH extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            CaretakerVH(@NonNull View itemView) {
                super(itemView);
                checkBox = (CheckBox) itemView;
            }
        }
    }

    private void loadCaretakersAndDisplay() {
        btnSelectCaretakers.setEnabled(false);
        btnSelectCaretakers.setText("Loading caretakers...");

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

                JSONArray array = new JSONArray(response.toString());
                caretakerNames.clear();
                caretakerIds.clear();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    caretakerIds.add(obj.getString("id"));
                    caretakerNames.add(obj.getString("fullname"));
                }

                // Populate map for name lookup
                caretakerMap.clear();
                for (int i = 0; i < caretakerIds.size(); i++) {
                    caretakerMap.put(caretakerIds.get(i), caretakerNames.get(i));
                }

                // Initialize selectedCaretakerIds from pond
                selectedCaretakerIds.clear();
                if (pond.getCaretakerIds() != null && !pond.getCaretakerIds().isEmpty()) {
                    for (String id : pond.getCaretakerIds()) {
                        if (caretakerIds.contains(id)) selectedCaretakerIds.add(id);
                    }
                }

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSelectCaretakers.setEnabled(true);
                        btnSelectCaretakers.setText("Select Caretakers");

                        // <-- Here is the fix: update tvCaretakers now
                        if (tvCaretakers != null)
                            tvCaretakers.setText(getSelectedCaretakerNames());

                        // Refresh chip container
                        refreshCaretakerChips();

                        btnSelectCaretakers.setOnClickListener(v -> openCaretakerSelectionDialog());
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


    private void refreshCaretakerChips() {
        caretakerChipContainer.removeAllViews();
        for (Integer id : assignedCaretakerIds) {
            String name = caretakerMap.get(id);
            if (name != null) {
                Chip chip = new Chip(requireContext());
                chip.setText(name);
                chip.setCloseIconVisible(false);
                caretakerChipContainer.addView(chip);
            }
        }
    }



}
