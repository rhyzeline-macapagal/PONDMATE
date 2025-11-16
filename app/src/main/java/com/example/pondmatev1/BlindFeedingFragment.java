package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BlindFeedingFragment extends DialogFragment {

    private TextView tvFeedType, etFeedCost; // ✅ replaced spinner with TextView
    private EditText etFeedQuantity, etFeedDate;
    private TableLayout tableFeedLogs;
    private Button btnAddFeed, btnCancelFeed;
    private Calendar selectedDate = Calendar.getInstance();
    private Map<String, Double> feedPriceMap = new HashMap<>();

    private boolean isLockedInFragment = false;
    private static final String PREF_LOCK_STATE = "LOCK_PREF";
    private static final String KEY_IS_LOCKED = "isLockedInBlindFeeding";





    private String pondName;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat mysqlFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final String TAG = "BlindFeedingFragment";

    private AlertDialog loadingDialog;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Dialog created");
        Dialog dialog = new Dialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_blind_feeding, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (getArguments() != null) {
            pondName = getArguments().getString("pondName");
        } else {
            // fallback to SharedPreferences if no argument passed
            SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson != null) {
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                pondName = pond.getName();
            } else {
                pondName = "Unknown Pond";
            }

        }


        SharedPreferences lockPrefs = requireContext().getSharedPreferences(PREF_LOCK_STATE, Context.MODE_PRIVATE);
        isLockedInFragment = lockPrefs.getBoolean(KEY_IS_LOCKED, false);

        if (isLockedInFragment) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⚠️ Locked")
                    .setMessage("You still need to complete all blind feeding logs before you can exit this section.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dlg, which) -> dlg.dismiss())
                    .show();
        }
        dialog.setCancelable(false);
        setCancelable(false);


        tvFeedType = view.findViewById(R.id.tvFeedType); // ✅ TextView now
        etFeedQuantity = view.findViewById(R.id.etFeedQuantity);
        etFeedCost = view.findViewById(R.id.etFeedCost);
        etFeedDate = view.findViewById(R.id.etFeedDate);
        tableFeedLogs = view.findViewById(R.id.tableFeedLogs);
        btnAddFeed = view.findViewById(R.id.btnAddFeed);
        btnCancelFeed = view.findViewById(R.id.btnCancelFeed);
        TextView btnClose = view.findViewById(R.id.btnClose);
        tvFeedType.setText("Frymash");

        etFeedDate.setOnClickListener(v -> showDateSelectionDialog());
        btnAddFeed.setOnClickListener(v -> addFeedLog());
        btnCancelFeed.setOnClickListener(v -> dismiss());
        hideLoadingDialog();
        btnClose.setOnClickListener(v -> dismiss());
        etFeedQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String quantityStr = s.toString().trim();
                if (quantityStr.isEmpty()) {
                    etFeedCost.setText("");
                    return;
                }

                double quantityInGrams;
                try {
                    quantityInGrams = Double.parseDouble(quantityStr);
                } catch (NumberFormatException e) {
                    etFeedCost.setText("");
                    return;
                }

                // Get pond breed
                SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                String pondJson = prefs.getString("selected_pond", null);
                if (pondJson == null) return;
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                String breed = pond.getBreed();

                // Fetch feed price from feeds list
                PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        try {
                            JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
                            JsonArray feeds = root.getAsJsonArray("feeds");
                            boolean found = false;
                            for (int i = 0; i < feeds.size(); i++) {
                                JsonObject feed = feeds.get(i).getAsJsonObject();
                                String feedBreed = feed.get("breed").getAsString();
                                String type = feed.get("feed_type").getAsString();

                                if (feedBreed.equalsIgnoreCase(breed) && type.equalsIgnoreCase("frymash")) {
                                    double pricePerKg = Double.parseDouble(feed.get("price_per_kg").getAsString());
                                    double cost = (quantityInGrams / 1000) * pricePerKg; // ✅ convert grams to kg
                                    etFeedCost.setText(String.format(Locale.getDefault(), "%.2f", cost));
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                etFeedCost.setText("0.00");
                                Toast.makeText(getContext(), "Feed price not found for this breed", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            etFeedCost.setText("0.00");
                            Toast.makeText(getContext(), "Error parsing feed price: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Cannot fetch feed price", e);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        etFeedCost.setText("0.00");
                        Toast.makeText(getContext(), "Cannot fetch feed price: " + error, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, error);
                    }
                });
            }

        });
        // Load
        loadFeedLogs(tableFeedLogs);
        checkIfLastBlindFeedingDay();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        requireActivity().getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isLockedInFragment) {
                            Toast.makeText(requireContext(),
                                    "You must complete all blind feeding logs before exiting.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                });
    }
    private void checkIfLastBlindFeedingDay() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String stockingDateStr = pond.getDateStocking();
        if (stockingDateStr == null || stockingDateStr.isEmpty()) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date stockingDate = sdf.parse(stockingDateStr);
            Calendar today = Calendar.getInstance();

            long diffMillis = today.getTimeInMillis() - stockingDate.getTime();
            int daysSinceStocking = (int) (diffMillis / (1000 * 60 * 60 * 24));

            Calendar lastBlindFeedingDay = Calendar.getInstance();
            lastBlindFeedingDay.setTime(stockingDate);
            lastBlindFeedingDay.add(Calendar.DAY_OF_YEAR, 30);

            Log.d("BlindFeedingDebug", "🗓 Stocking Date: " + stockingDateStr +
                    " | Today: " + sdf.format(today.getTime()) +
                    " | Last Blind Feeding Day: " + sdf.format(lastBlindFeedingDay.getTime()) +
                    " | Days Since Stocking: " + daysSinceStocking);

            // ✅ Fetch logs to decide what to do
            PondSyncManager.fetchBlindFeedLogs(pond.getName(), new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object response) {
                    try {
                        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) return;

                        JsonArray logs = json.getAsJsonArray("data");
                        int logCount = logs.size();

                        requireActivity().runOnUiThread(() -> {
                            SharedPreferences lockPrefs = requireContext().getSharedPreferences(PREF_LOCK_STATE, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = lockPrefs.edit();

                            if (daysSinceStocking == 30) {
                                // 🗓 It's the final blind feeding day
                                if (logCount < 30) {
                                    // 🔒 Lock until logs complete
                                    isLockedInFragment = true;
                                    editor.putBoolean(KEY_IS_LOCKED, true);
                                    editor.apply();

                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("⚠️ Final Day of Blind Feeding")
                                            .setMessage("You have only completed " + logCount + " out of 30 blind feeding logs.\n\nPlease finish all logs today before exiting.")
                                            .setCancelable(false)
                                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                            .show();

                                    Log.d("BlindFeedingDebug", "🔒 Final day and incomplete logs. Locked fragment.");
                                } else {
                                    // ✅ All logs complete
                                    isLockedInFragment = false;
                                    editor.putBoolean(KEY_IS_LOCKED, false);
                                    editor.apply();

                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("✅ Blind Feeding Complete")
                                            .setMessage("You have completed all 30 blind feeding logs.\n\nNo further entries are required.")
                                            .setCancelable(false)

                                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                            .show();

                                    // 🚫 Disable adding more logs
                                    if (btnAddFeed != null) btnAddFeed.setEnabled(false);
                                    Log.d("BlindFeedingDebug", "✅ All 30 logs complete on final day. Disabled log button.");
                                }

                            } else if (daysSinceStocking > 30) {
                                // ⛔ After blind feeding phase
                                isLockedInFragment = false;
                                editor.putBoolean(KEY_IS_LOCKED, false);
                                editor.apply();

                                new AlertDialog.Builder(requireContext())
                                        .setTitle("⛔ Blind Feeding Period Over")
                                        .setMessage("The 30-day blind feeding period has ended.\nYou can no longer add blind feeding logs.")
                                        .setCancelable(false)
                                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                        .show();

                                if (btnAddFeed != null) btnAddFeed.setEnabled(false);
                                Log.d("BlindFeedingDebug", "⛔ Day > 30. Blind feeding disabled.");

                            } else {
                                // 👌 Before day 30 — allow normal logging
                                isLockedInFragment = false;
                                editor.putBoolean(KEY_IS_LOCKED, false);
                                editor.apply();

                                Log.d("BlindFeedingDebug", "✅ Day " + daysSinceStocking + ": logging allowed.");
                            }
                        });

                    } catch (Exception e) {
                        Log.e("BlindFeedingDebug", "Error parsing logs: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("BlindFeedingDebug", "Error fetching blind feed logs: " + error);
                }
            });

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private void showDateSelectionDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String stockingDateStr = pond.getDateStocking();
        if (stockingDateStr == null || stockingDateStr.isEmpty()) return;

        String[] displayDates = generateFeedingDates(stockingDateStr);
        String[] serverDates = generateFeedingDatesForServer(stockingDateStr);

        if (displayDates.length == 0) return;

        final int[] selectedIndex = {0}; // default selection

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Feeding Date")
                .setSingleChoiceItems(displayDates, 0, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("OK", (dialog, which) -> etFeedDate.setText(displayDates[selectedIndex[0]])) // display friendly format
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())

                .show();
hideLoadingDialog();
        // Later, when sending to server, use: serverDates[selectedIndex[0]]
    }


    private String formatDateForDisplay(String dateStr) {
        try {
            Date date = mysqlFormat.parse(dateStr); // parse yyyy-MM-dd
            return displayFormat.format(date);       // format to MMM dd, yyyy
        } catch (ParseException e) {
            e.printStackTrace();
            return dateStr; // fallback to original if parsing fails
        }
    }

    private void fetchFeedPriceFromAllFeeds(String breed, String feedType, FeedPriceCallback callback) {
        PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                try {
                    JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
                    JsonArray feeds = root.getAsJsonArray("feeds"); // <-- use the "feeds" array
                    for (int i = 0; i < feeds.size(); i++) {
                        JsonObject feed = feeds.get(i).getAsJsonObject();
                        String feedBreed = feed.get("breed").getAsString();
                        String type = feed.get("feed_type").getAsString();

                        if (feedBreed.equalsIgnoreCase(breed) && type.equalsIgnoreCase(feedType)) {
                            double pricePerKg = Double.parseDouble(feed.get("price_per_kg").getAsString());
                            callback.onPriceFetched(pricePerKg);
                            return;
                        }
                    }
                    callback.onError("Feed price not found for this breed");
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    interface FeedPriceCallback {
        void onPriceFetched(double pricePerKg);
        void onError(String error);
    }
    private void addFeedLog() {
        final String feedType = "Frymash";
        final String quantityStr = etFeedQuantity.getText().toString().trim();
        final String dateStr = etFeedDate.getText().toString().trim();

        if (quantityStr.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        final String feedingDateMysql;
        try {
            Date selected = displayFormat.parse(dateStr);
            feedingDateMysql = mysqlFormat.format(selected);
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) {
            Toast.makeText(getContext(), "Pond not selected", Toast.LENGTH_SHORT).show();
            return;
        }
        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        final String pondName = pond.getName();

        computeFeedCost(feedType, quantity, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object costObj) {
                final double cost = (double) costObj;

                // Check for duplicates
                PondSyncManager.fetchBlindFeedLogs(pondName, new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        try {
                            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                            JsonArray logs = json.getAsJsonArray("data");
                            boolean dateExists = false;
                            for (int i = 0; i < logs.size(); i++) {
                                JsonObject log = logs.get(i).getAsJsonObject();
                                String existingDate = log.has("feeding_date") && !log.get("feeding_date").isJsonNull()
                                        ? log.get("feeding_date").getAsString() : "";
                                if (existingDate.equals(feedingDateMysql)) {
                                    dateExists = true;
                                    break;
                                }
                            }

                            if (dateExists) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    String displayDate;
                                    try {
                                        Date d = mysqlFormat.parse(feedingDateMysql);
                                        displayDate = displayFormat.format(d);
                                    } catch (ParseException e) {
                                        displayDate = feedingDateMysql;
                                    }
                                    Toast.makeText(
                                            getContext(),
                                            "A feeding log for " + displayDate + " already exists.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                            } else {
                                new Handler(Looper.getMainLooper()).post(() -> showConfirmAddDialog(quantity, cost, feedType, dateStr, feedingDateMysql, pondName));
                            }

                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> showConfirmAddDialog(quantity, cost, feedType, dateStr, feedingDateMysql, pondName));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> showConfirmAddDialog(quantity, cost, feedType, dateStr, feedingDateMysql, pondName));
                    }
                });
            }

            @Override
            public void onError(String error) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Error computing cost: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    private void showConfirmAddDialog(double quantity, double cost, String feedType, String dateStr, String feedingDateMysql, String pondName) {
        if (!isAdded() || getContext() == null) return;

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Feeding Log")
                .setMessage("Add feeding log?\n\nFeed Type: " + feedType +
                        "\nQuantity: " + quantity + " kg" +
                        "\nCost: ₱" + String.format(Locale.getDefault(), "%.2f", cost) +
                        "\nDate: " + dateStr)
                .setPositiveButton("Yes", (dialog, which) -> {
                    showLoadingDialog("Uploading feeding log...");
                    String recordAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    PondSyncManager.uploadBlindFeedLog(
                            pondName, feedType, quantity, cost, feedingDateMysql, recordAt,
                            new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object response) {
                                    hideLoadingDialog();
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        Toast.makeText(getContext(), "Feed log added!", Toast.LENGTH_SHORT).show();
                                        loadFeedLogs(tableFeedLogs);
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    hideLoadingDialog();
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                    );
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private String[] generateFeedingDates(String stockingDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date stockingDate = sdf.parse(stockingDateStr);

            Calendar cal = Calendar.getInstance();
            cal.setTime(stockingDate);

            int totalDays = 30; // adjust number of selectable dates

            // We'll keep two arrays: one for server format, one for display
            String[] displayDates = new String[totalDays];

            for (int i = 0; i < totalDays; i++) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                displayDates[i] = displayFormat.format(cal.getTime()); // MMM dd, yyyy
            }

            return displayDates;
        } catch (ParseException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
    private String[] generateFeedingDatesForServer(String stockingDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date stockingDate = sdf.parse(stockingDateStr);

            Calendar cal = Calendar.getInstance();
            cal.setTime(stockingDate);

            int totalDays = 30;
            String[] serverDates = new String[totalDays];

            for (int i = 0; i < totalDays; i++) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                serverDates[i] = mysqlFormat.format(cal.getTime()); // yyyy-MM-dd
            }

            return serverDates;
        } catch (ParseException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
    private void updateFeedLog(String pondName, String feedType, double quantity, double cost, String feedingDate, String feedLogId) {
        showLoadingDialog("Updating feeding log...");

        PondSyncManager.updateBlindFeedingLog(
                pondName, feedType, quantity, cost, feedingDate, feedLogId,
                new PondSyncManager.OnDataSyncListener() {
                    @Override
                    public void onSuccess(String response) {
                        hideLoadingDialog();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded() || getContext() == null) return;

                            try {
                                JSONObject json = new JSONObject(response);
                                if (json.getString("status").equals("success")) {
                                    Toast.makeText(getContext(), "Feed log updated!", Toast.LENGTH_SHORT).show();
                                    tableFeedLogs.removeViews(1, tableFeedLogs.getChildCount() - 1);
                                    loadFeedLogs(tableFeedLogs);
                                } else {
                                    Toast.makeText(getContext(), json.getString("message"), Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        hideLoadingDialog();
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(getContext(), "Error updating: " + error, Toast.LENGTH_LONG).show()
                        );
                    }
                }
        );
    }
    private void loadFeedLogs(TableLayout tableFeedLogs) {
        if (tableFeedLogs == null) {
            Log.e(TAG, "TableLayout is null. Cannot load feed logs.");
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        showLoadingDialog("Loading feed records...");

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String pondName = pond.getName();

        PondSyncManager.fetchBlindFeedLogs(pondName, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                hideLoadingDialog();
                requireActivity().runOnUiThread(() -> {
                    try {
                        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) {
                            Toast.makeText(getContext(), "No logs found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JsonArray logs = json.getAsJsonArray("data");
                        int childCount = tableFeedLogs.getChildCount();
                        if (childCount > 1) tableFeedLogs.removeViews(1, childCount - 1);

                        for (int i = 0; i < logs.size(); i++) {
                            JsonObject log = logs.get(i).getAsJsonObject();
                            String logId = log.get("id").getAsString();
                            String date = log.get("feeding_date").getAsString();
                            String feedType = log.get("feed_type").getAsString();
                            String quantity = log.get("quantity").getAsString();
                            String cost = log.get("cost").getAsString();

                            TableRow row = new TableRow(getContext());
                            row.setPadding(4, 4, 4, 4);

                            // Actions layout
                            LinearLayout actionLayout = new LinearLayout(getContext());
                            actionLayout.setOrientation(LinearLayout.HORIZONTAL);
                            actionLayout.setGravity(Gravity.CENTER_VERTICAL);
                            actionLayout.setPadding(8, 4, 8, 4);

                            ImageView editIcon = new ImageView(getContext());
                            editIcon.setImageResource(android.R.drawable.ic_menu_edit);
                            editIcon.setPadding(8, 0, 8, 0);
                            editIcon.setOnClickListener(v -> showEditDialog(logId, date, feedType, quantity, cost));

                            ImageView deleteIcon = new ImageView(getContext());
                            deleteIcon.setImageResource(android.R.drawable.ic_menu_delete);
                            deleteIcon.setPadding(8, 0, 8, 0);
                            deleteIcon.setOnClickListener(v -> confirmDeleteLog(logId));

                            actionLayout.addView(editIcon);
                            actionLayout.addView(deleteIcon);

                            row.addView(actionLayout);
                            addCell(row, formatDateForDisplay(date));
                            addCell(row, feedType);
                            addCell(row, quantity);
                            addCell(row, "₱" + cost);

                            tableFeedLogs.addView(row);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing feed logs: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(getContext(), "Error loading logs: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingDialog(String message) {
        requireActivity().runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                TextView loadingText = loadingDialog.findViewById(R.id.loadingText);
                if (loadingText != null) loadingText.setText(message);
                return; // already showing
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);

            ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
            TextView loadingText = dialogView.findViewById(R.id.loadingText);
            loadingText.setText(message);

            Animation rotate = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate);
            if (fishLoader != null) fishLoader.startAnimation(rotate);

            builder.setView(dialogView);
            builder.setCancelable(false);
            loadingDialog = builder.create();
            loadingDialog.show();
        });
    }

    private void hideLoadingDialog() {
        requireActivity().runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
                loadingDialog = null;
            }
        });
    }
    private void computeFeedCost(String feedType, double quantityInGrams, PondSyncManager.Callback callback) {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) {
            callback.onError("Pond not selected");
            return;
        }
        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        String pondBreed = pond.getBreed();

        PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                try {
                    JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject(); // root object
                    JsonArray feeds = root.getAsJsonArray("feeds"); // ✅ correct

                    double pricePerKg = -1;
                    for (int i = 0; i < feeds.size(); i++) {
                        JsonObject feed = feeds.get(i).getAsJsonObject();
                        String breed = feed.get("breed").getAsString();
                        String type = feed.get("feed_type").getAsString();
                        if (breed.equalsIgnoreCase(pondBreed) && type.equalsIgnoreCase(feedType)) {
                            pricePerKg = feed.get("price_per_kg").getAsDouble();
                            break;
                        }
                    }

                    if (pricePerKg < 0) {
                        callback.onError("Price not found for " + feedType + " (" + pondBreed + ")");
                        return;
                    }

                    double cost = (quantityInGrams / 1000.0) * pricePerKg; // grams -> kg
                    callback.onSuccess(cost);

                } catch (Exception e) {
                    callback.onError("Error parsing feeds: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    private void showEditDialog(String logId, String date, String feedType, String quantity, String cost) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_feed, null);
        EditText etQuantity = dialogView.findViewById(R.id.etEditQuantity);
        EditText etCost = dialogView.findViewById(R.id.etEditCost);

        etQuantity.setText(quantity);
        etCost.setText(cost);

        etQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double qtyInGrams = Double.parseDouble(s.toString());
                    SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                    String pondJson = prefs.getString("selected_pond", null);
                    if (pondJson == null) {
                        etCost.setText("0.00");
                        return;
                    }
                    PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                    String breed = pond.getBreed();

                    PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
                        @Override
                        public void onSuccess(Object response) {
                            try {
                                JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
                                JsonArray feeds = root.getAsJsonArray("feeds");
                                boolean found = false;
                                for (int i = 0; i < feeds.size(); i++) {
                                    JsonObject feed = feeds.get(i).getAsJsonObject();
                                    String feedBreed = feed.get("breed").getAsString();
                                    String type = feed.get("feed_type").getAsString();

                                    if (feedBreed.equalsIgnoreCase(breed) && type.equalsIgnoreCase(feedType)) {
                                        double pricePerKg = Double.parseDouble(feed.get("price_per_kg").getAsString());
                                        double cost = qtyInGrams * pricePerKg / 1000.0; // if input in grams
                                        etCost.setText(String.format(Locale.getDefault(), "%.2f", cost));
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) etCost.setText("0.00");
                            } catch (Exception e) {
                                etCost.setText("0.00");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            etCost.setText("0.00");
                        }
                    });

                } catch (NumberFormatException e) {
                    etCost.setText("0.00");
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });


        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                double newQuantity = Double.parseDouble(etQuantity.getText().toString());
                double newCost = Double.parseDouble(etCost.getText().toString());

                SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
                String pondJson = prefs.getString("selected_pond", null);
                PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
                String pondName = pond.getName();

                updateFeedLog(pondName, feedType, newQuantity, newCost, date, logId);

            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid input: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss() );
        hideLoadingDialog();
        builder.show();
    }
    private void confirmDeleteLog(String logId) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this feeding log?\nThis action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteFeedLog(logId))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void deleteFeedLog(String logId) {
        showLoadingDialog("Deleting feeding log...");
        PondSyncManager.deleteBlindFeedingLog(logId, pondName, new PondSyncManager.OnDataSyncListener() {
            @Override
            public void onSuccess(String response) {
                hideLoadingDialog();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Feed log deleted!", Toast.LENGTH_SHORT).show();
                    loadFeedLogs(tableFeedLogs);
                    hideLoadingDialog();
                });
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Error deleting: " + error, Toast.LENGTH_SHORT).show()
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
    @Override
    public void dismiss() {
        if (isLockedInFragment) {
            Toast.makeText(requireContext(),
                    "You cannot close this until all blind feeding logs are complete.",
                    Toast.LENGTH_SHORT).show();
        } else {
            super.dismiss();
        }
    }

    @Override
    public void dismissAllowingStateLoss() {
        if (isLockedInFragment) {
            Toast.makeText(requireContext(),
                    "You cannot close this until all blind feeding logs are complete.",
                    Toast.LENGTH_SHORT).show();
        } else {
            super.dismissAllowingStateLoss();
        }
    }

}
