package com.example.pondmatev1;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BlindFeedingFragment extends DialogFragment {

    private Spinner spinnerFeedType;
    private EditText etOtherFeedType, etFeedQuantity, etFeedCost, etFeedDate;
    private TableLayout tableFeedLogs;
    private Button btnAddFeed, btnCancelFeed;
    private Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat mysqlFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final String TAG = "BlindFeedingFragment";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Dialog created");
        Dialog dialog = new Dialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_blind_feeding, null);
        dialog.setContentView(view);

        spinnerFeedType = view.findViewById(R.id.spinnerFeedType);
        etOtherFeedType = view.findViewById(R.id.etOtherFeedType);
        etFeedQuantity = view.findViewById(R.id.etFeedQuantity);
        etFeedCost = view.findViewById(R.id.etFeedCost);
        etFeedDate = view.findViewById(R.id.etFeedDate);
        tableFeedLogs = view.findViewById(R.id.tableFeedLogs);
        btnAddFeed = view.findViewById(R.id.btnAddFeed);
        btnCancelFeed = view.findViewById(R.id.btnCancelFeed);
        TextView btnClose = view.findViewById(R.id.btnClose);

        // Spinner setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Frymash", "Crumble", "Other"}
        );
        spinnerFeedType.setAdapter(adapter);

        spinnerFeedType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                etOtherFeedType.setVisibility(
                        parent.getItemAtPosition(position).toString().equals("Other") ? View.VISIBLE : View.GONE
                );
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etFeedDate.setOnClickListener(v -> showDatePicker());
        btnAddFeed.setOnClickListener(v -> addFeedLog());
        btnCancelFeed.setOnClickListener(v -> dismiss());
        btnClose.setOnClickListener(v -> dismiss());

        // Load existing logs
        loadFeedLogs();

        return dialog;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    etFeedDate.setText(displayFormat.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void addFeedLog() {
        String feedType = spinnerFeedType.getSelectedItem().toString();
        if (feedType.equals("Other")) {
            feedType = etOtherFeedType.getText().toString().trim();
        }

        String quantityStr = etFeedQuantity.getText().toString().trim();
        String costStr = etFeedCost.getText().toString().trim();
        String dateStr = etFeedDate.getText().toString().trim();

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        String pondName = "";
        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
        }

        if (feedType.isEmpty() || quantityStr.isEmpty() || costStr.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double quantity = Double.parseDouble(quantityStr);
        double cost = Double.parseDouble(costStr);

        String feedingDateMysql;
        try {
            Date parsedDate = displayFormat.parse(dateStr);
            feedingDateMysql = mysqlFormat.format(parsedDate);
        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error: " + e.getMessage());
            feedingDateMysql = dateStr;
        }

        final String finalFeedType = feedType;
        final String finalPondName = pondName;
        final String finalFeedingDate = feedingDateMysql;
        final double finalQuantity = quantity;
        final double finalCost = cost;

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Feeding Log")
                .setMessage("Add this feeding log?\n\nFeed Type: " + finalFeedType +
                        "\nQuantity: " + finalQuantity + " kg\nCost: ₱" + finalCost + "\nDate: " + dateStr)
                .setPositiveButton("Yes", (dialog, which) -> {
                    String recordAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date());

                    PondSyncManager.uploadBlindFeedLog(
                            finalPondName,
                            finalFeedType,
                            finalQuantity,
                            finalCost,
                            finalFeedingDate,
                            recordAt,
                            new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object response) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Feed log added!", Toast.LENGTH_SHORT).show();
                                        loadFeedLogs(); // 🔁 Auto refresh table after upload
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                    );
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadFeedLogs() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String pondName = pond.getName();

        PondSyncManager.fetchBlindFeedLogs(pondName, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                requireActivity().runOnUiThread(() -> {
                    try {
                        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) {
                            Toast.makeText(getContext(), "No logs found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JsonArray logs = json.getAsJsonArray("data");

                        // 🔄 Keep header (index 0), remove all rows below it
                        int childCount = tableFeedLogs.getChildCount();
                        if (childCount > 1) tableFeedLogs.removeViews(1, childCount - 1);

                        for (int i = 0; i < logs.size(); i++) {
                            JsonObject log = logs.get(i).getAsJsonObject();
                            TableRow row = new TableRow(getContext());

                            addCell(row, log.get("feeding_date").getAsString());
                            addCell(row, log.get("feed_type").getAsString());
                            addCell(row, log.get("quantity").getAsString());
                            addCell(row, "₱" + log.get("cost").getAsString());


                            tableFeedLogs.addView(row);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing feed logs: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error loading logs: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }






    private void addCell(TableRow row, String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(16, 8, 16, 8);
        tv.setTextSize(14);
        row.addView(tv);
    }
}
