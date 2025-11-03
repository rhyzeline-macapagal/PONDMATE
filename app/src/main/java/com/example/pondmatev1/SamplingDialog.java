package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

// For OkHttp networking
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// For JSON construction
import org.json.JSONException;
import org.json.JSONObject;

// For logging
import android.util.Log;

// For Toast
import android.widget.Toast;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class SamplingDialog extends DialogFragment {

    private static final String TAG = "SamplingDialog";

    private Button btnSelectTime1, btnSelectTime2, btnSave;
    private TextView tvPondName, tvNextSampling, tvDaysOfCulture, tvLifeStage, tvTotalStocks, tvMortality, tvDFRPerCycle;
    private TextView tvTimeFeeding1, tvTimeFeeding2, tvABWResult, tvDFRResult, tvFeedType, tvFeedCost, tvRemainingFeed;
    private EditText etSampledWeight, etNumSamples, etFeedingRate;
    private TextView tvSurvivalRate; // display-only
    private int time1Minutes = -1, time2Minutes = -1;
    private String formattedTime1, formattedTime2;
    private String species = "";
    private long daysOfCulture = 1;
    private Double currentPricePerKg = null;
    private String pondId = "";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_add_sampling, container, false);
        // Close button
        TextView btnClose = view.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        // Initialize TextViews
        tvPondName = view.findViewById(R.id.tvPondName);
        tvNextSampling = view.findViewById(R.id.tvNextSampling);
        tvDFRPerCycle = view.findViewById(R.id.tvDFRPerCycle);
        tvDaysOfCulture = view.findViewById(R.id.tvDaysOfCulture);
        tvLifeStage = view.findViewById(R.id.tvLifeStage);
        tvTotalStocks = view.findViewById(R.id.tvTotalStocks);
        tvMortality = view.findViewById(R.id.tvMortality);
        tvTimeFeeding1 = view.findViewById(R.id.timeoffeeding1);
        tvTimeFeeding2 = view.findViewById(R.id.timeoffeeding2);
        tvABWResult = view.findViewById(R.id.tvABWResult);
        tvDFRResult = view.findViewById(R.id.tvDFRResult);
        tvFeedType = view.findViewById(R.id.tvFeedType);
        tvFeedCost = view.findViewById(R.id.tvFeedCost);

        // Initialize EditTexts and Survival Rate (display-only TextView)
        etSampledWeight = view.findViewById(R.id.etSampledWeight);
        etNumSamples = view.findViewById(R.id.etNumSamples);
        etFeedingRate = view.findViewById(R.id.etFeedingRate);
        tvSurvivalRate = view.findViewById(R.id.etSurvivalRate); // you said this is a TextView (id kept)

        btnSelectTime1 = view.findViewById(R.id.btnselecttime1);
        btnSelectTime2 = view.findViewById(R.id.btnselecttime2);

        // Load pond data and setup UI
        loadPondData();
        setDefaultFeedingTimes();
        addTextWatchers();

        tvRemainingFeed = view.findViewById(R.id.tvRemainingFeed);
        btnSave = view.findViewById(R.id.btnSaveSampling);
        btnSave.setOnClickListener(v -> validateAndSave());
        refreshFeedLevel();

        btnSelectTime1.setOnClickListener(v -> showTimePickerDialog(tvTimeFeeding1, 1));
        btnSelectTime2.setOnClickListener(v -> showTimePickerDialog(tvTimeFeeding2, 2));

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                feedUpdateReceiver,
                new IntentFilter("FEED_LEVEL_UPDATED")
        );
        Log.d("FEED_UI", "SamplingDialog receiver registered");

        Log.d("FEED_TEST", "SamplingDialog onViewCreated: Calling refreshRemainingFeedUI()");
        refreshRemainingFeedUI();

        return view;
    }

    private void loadPondData() {
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", android.content.Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);
        if (pondJson == null) return;

        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
        pondId = pond.getId();
        Log.d("FEED_TEST", "Loaded pondId for SamplingDialog = " + pondId);
        if (pond == null) return;

        tvPondName.setText(pond.getName());

        // next sampling date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 15);
        tvNextSampling.setText("Next Sampling: " + sdf.format(calendar.getTime()));

        // compute daysOfCulture (class-level)
        if (pond.getDateStocking() != null && !pond.getDateStocking().isEmpty()) {
            try {
                Date stockingDate = sdf.parse(pond.getDateStocking());
                daysOfCulture = (new Date().getTime() - stockingDate.getTime()) / (1000L * 60 * 60 * 24);
            } catch (Exception e) {
                daysOfCulture = 1;
            }
        } else {
            daysOfCulture = 1;
        }
        if (daysOfCulture < 1) daysOfCulture = 1;
        tvDaysOfCulture.setText(daysOfCulture + " days");

        // normalize breed
        species = pond.getBreed() != null ? pond.getBreed().toLowerCase(Locale.ROOT) : "";
        String breedClean = (species.contains("bangus") || species.contains("milkfish")) ? "bangus" : "tilapia";

        // life stage
        String lifeStage;
        if (breedClean.equals("tilapia")) {
            if (daysOfCulture <= 60) lifeStage = "Fingerling";
            else if (daysOfCulture <= 90) lifeStage = "Juvenile";
            else if (daysOfCulture <= 180) lifeStage = "Sub-adult / Adult (Harvest)";
            else lifeStage = "Post-harvest";
        } else {
            if (daysOfCulture <= 70) lifeStage = "Fingerling";
            else if (daysOfCulture <= 90) lifeStage = "Juvenile";
            else if (daysOfCulture <= 180) lifeStage = "Sub-adult / Adult (Harvest)";
            else lifeStage = "Post-harvest";
        }
        tvLifeStage.setText(lifeStage);

        // fill other UI
        tvTotalStocks.setText(String.valueOf(pond.getFishCount()));
        tvMortality.setText(pond.getMortalityRate() + "%");
        tvSurvivalRate.setText(String.format(Locale.getDefault(), "%.2f%%", (100 - pond.getMortalityRate())));

        // fetch feed type and price for this pond/day
        Log.d(TAG, "Loading feed price for " + breedClean + " days=" + daysOfCulture);
        fetchFeedPrice(breedClean, daysOfCulture);

        // watch DFR changes (DFR text updates happen in computeValues())
        tvDFRResult.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateFeedCost(); }
        });
    }

    private void fetchFeedPrice(String breedClean, long days) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_feed_price_by_day.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String postData = "breed=" + URLEncoder.encode(breedClean, "UTF-8")
                        + "&days=" + URLEncoder.encode(String.valueOf(days), "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes("UTF-8"));
                    os.flush();
                }

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + code);
                }

                StringBuilder sb = new StringBuilder();
                try (InputStream is = conn.getInputStream();
                     BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }

                Log.d(TAG, "fetchFeedPrice response: " + sb.toString());

                JSONObject obj = new JSONObject(sb.toString());
                final String feedType = obj.optString("feed_type", "No Feed");
                final double pricePerKg = obj.optDouble("price_per_kg", 0);

                requireActivity().runOnUiThread(() -> {
                    currentPricePerKg = pricePerKg;
                    tvFeedType.setText(feedType);
                    tvFeedType.setTag(pricePerKg);
                    Log.d(TAG, "FeedType=" + feedType + " price=" + pricePerKg);
                    updateFeedCost(); // recalc now that price is present
                });

            } catch (Exception e) {
                Log.e(TAG, "fetchFeedPrice error", e);
                requireActivity().runOnUiThread(() -> {
                    currentPricePerKg = null;
                    tvFeedType.setText("N/A");
                    tvFeedCost.setText("₱00.00");
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void updateFeedCost() {
        if (currentPricePerKg == null) {
            tvFeedCost.setText("₱00.00");
            return;
        }

        // ✅ Get TOTAL DFR per day (not per feeding)
        String dfrText = tvDFRResult.getText().toString().replace(" g", "").trim();
        double dfrGrams = parseDouble(dfrText);

        // ✅ Convert grams → kilograms
        double dfrKgPerDay = dfrGrams / 1000.0;

        // ✅ Compute daily feed cost
        double dailyCost = dfrKgPerDay * currentPricePerKg;

        // ✅ Display formatted
        tvFeedCost.setText("₱" + String.format(Locale.getDefault(), "%.2f", dailyCost));

        Log.d(TAG, "updateFeedCost — DFR(g)=" + dfrGrams +
                ", DFR(kg/day)=" + dfrKgPerDay +
                ", price/kg=" + currentPricePerKg +
                ", dailyCost=" + dailyCost);
    }

    private void validateAndSave() {
        SharedPreferences prefs = requireContext().getSharedPreferences("FEED_LEVEL_PREF", Context.MODE_PRIVATE);
        float remainingFeed = prefs.getFloat("feed_remaining_" + pondId, 0f);

        if (remainingFeed <= 0) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Feed Container is Empty")
                    .setMessage("You must store feeds in the container before you can save a sampling.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        double feedPerCycle = parseDouble(tvDFRPerCycle.getText().toString().replace(" g", ""));
        double requiredFeed = feedPerCycle * 30; // 2x/day for 15 days

        if (remainingFeed < requiredFeed) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Insufficient Feed")
                    .setMessage("The remaining feed in the container is not enough to support the next 15 days of feeding.\n\n" +
                            "Store additional feed before proceeding.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        String sampledWeight = etSampledWeight.getText().toString().trim();
        String numSamples = etNumSamples.getText().toString().trim();
        String feedingRates = etFeedingRate.getText().toString().trim();
        String survivalRates = tvSurvivalRate.getText().toString().trim(); // display-only

        String feedingTime1 = tvTimeFeeding1.getText().toString().trim();
        String feedingTime2 = tvTimeFeeding2.getText().toString().trim();

        if (sampledWeight.isEmpty() || numSamples.isEmpty() ||
                feedingRates.isEmpty() || survivalRates.isEmpty() ||
                feedingTime1.isEmpty() || feedingTime2.isEmpty()) {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Incomplete Fields")
                    .setMessage("Please complete all required fields before saving.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        try {
            double sw = Double.parseDouble(sampledWeight);
            double ns = Double.parseDouble(numSamples);
            double fr = Double.parseDouble(feedingRates);

            if (sw <= 0 || ns <= 0 || fr <= 0) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Invalid Input")
                        .setMessage("Values must be greater than zero.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
                return;
            }
        } catch (NumberFormatException e) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Invalid Format")
                    .setMessage("Please enter valid numeric values.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        SharedPreferences pondPrefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = pondPrefs.getString("selected_pond", null);
        PondModel pond = new Gson().fromJson(pondJson, PondModel.class);

        pondId = pond.getId();
        int daysOfCultureVal = Integer.parseInt(tvDaysOfCulture.getText().toString().replace(" days", ""));
        String growthStage = tvLifeStage.getText().toString();
        int totalStocks = pond.getFishCount();
        double mortalityRate = pond.getMortalityRate();

        double abw = parseDouble(tvABWResult.getText().toString().replace(" g", ""));
        double feedingRate = parseDouble(etFeedingRate.getText().toString());
        double survivalRate = parseDouble(tvSurvivalRate.getText().toString().replace("%", ""));
        double dfr = parseDouble(tvDFRResult.getText().toString().replace(" g", ""));
        double dfrFeed = parseDouble(tvDFRResult.getText().toString().replace(" g", ""));
        double dailyFeedCost = parseDouble(tvFeedCost.getText().toString().replace("₱", "").trim());


        String feedingOne = formattedTime1; // use 24-hour version
        String feedingTwo = formattedTime2;

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        String nextSamplingDate = tvNextSampling.getText().toString()
                .replace("Next Sampling: ", "")
                .trim();

        PondSyncManager.uploadSamplingRecord(
                pond.getId(), daysOfCultureVal, growthStage, totalStocks, mortalityRate,
                feedingOne, feedingTwo, abw, feedingRate, survivalRate, dfr, dfrFeed, dailyFeedCost,
                now, now, nextSamplingDate,
                new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Sampling record saved!", Toast.LENGTH_SHORT).show();

                            float feedPerCycle = (float) parseDouble(tvDFRPerCycle.getText().toString().replace(" g", ""));
                            float totalToDeduct = feedPerCycle * 2f; // 2 feedings for the current day
                            Log.d("SAMPLING_FEED", "Feed deduction computed: perCycle=" + feedPerCycle + ", totalDeduct=" + totalToDeduct);

                            FeedStorage.deductFeed(requireContext(), pondId, totalToDeduct);

                            float updated  = FeedStorage.getRemainingFeed(requireContext(), pondId);
                            Log.d("SAMPLING_FEED", "After deduction, remaining feed = " + updated );
                            tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g", updated ));

                            refreshFeedLevel();

                            // Notify ControlsFeeder (so it updates UI too)
                            LocalBroadcastManager.getInstance(requireContext())
                                    .sendBroadcast(new Intent("FEED_LEVEL_UPDATED"));

                            // ✅ Upload to Adafruit using feed per cycle per schedule
                            uploadDFRToAdafruit(pond.getName(), formattedTime1, formattedTime2, feedPerCycle);

                            // Refresh UI on parent fragment if needed
                            if (getParentFragment() instanceof ProductionCostFragment) {
                                ((ProductionCostFragment) getParentFragment()).updateSamplingButtonState(pond.getId());
                            }

                            dismiss();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show()
                        );
                    }
                }
        );
    }

    private void refreshRemainingFeedUI() {
        float remaining = FeedStorage.getRemainingFeed(requireContext(), pondId);
        Log.d("FEED_TEST", "refreshRemainingFeedUI() -> remaining feed = " + remaining);
        tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g", remaining));
    }

    private void refreshFeedLevel() {
        SharedPreferences prefs = requireContext().getSharedPreferences("FEED_LEVEL_PREF", Context.MODE_PRIVATE);
        float remainingFeed = FeedStorage.getRemainingFeed(requireContext(), pondId);
        tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g", remainingFeed));

        if (remainingFeed <= 0) {
            btnSave.setEnabled(false);
            btnSave.setAlpha(0.5f);
        } else {
            btnSave.setEnabled(true);
            btnSave.setAlpha(1f);
        }
    }

    private final BroadcastReceiver feedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshFeedLevel();
        }
    };

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(feedUpdateReceiver);
        super.onDestroyView();
    }


    private void setDefaultFeedingTimes() {
        formattedTime1 = "08:00:00";
        formattedTime2 = "16:00:00";
        tvTimeFeeding1.setText("8:00 AM");
        tvTimeFeeding2.setText("4:00 PM");
    }

    private void showTimePickerDialog(TextView targetTextView, int timeNumber) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timepicker, null);
        TimePicker timePicker = dialogView.findViewById(R.id.timePickerSpinner);
        timePicker.setIs24HourView(false);
        timePicker.setHour(timeNumber == 1 ? 8 : 16);
        timePicker.setMinute(0);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Feeding Time " + timeNumber)
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    String amPm = (hour >= 12) ? "PM" : "AM";
                    int hour12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
                    String formatted12 = String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm);
                    targetTextView.setText(formatted12);

                    // Save 24-hour format for database
                    String formatted24 = String.format(Locale.getDefault(), "%02d:%02d:00", hour, minute);
                    if (timeNumber == 1) {
                        formattedTime1 = formatted24;
                    } else {
                        formattedTime2 = formatted24;
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    // -------------------------
    // watchers for live computation (do NOT add survivalRate watcher; it's display-only)
    // -------------------------
    private void addTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { computeValues(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        etSampledWeight.addTextChangedListener(watcher);
        etNumSamples.addTextChangedListener(watcher);
        etFeedingRate.addTextChangedListener(watcher);
        // DO NOT add tvSurvivalRate here — it's not editable by user
    }

    // -------------------------
    // Compute ABW and DFR (keeps grams in UI)
    // DFR formula: dfr (g) = feedingRateDecimal * survivalDecimal * ABW(g) * totalStocks
    // -------------------------
    private void computeValues() {
        try {
            String sampledWeightStr = etSampledWeight.getText().toString().trim();
            String numSamplesStr = etNumSamples.getText().toString().trim();
            String feedingRateStr = etFeedingRate.getText().toString().trim();
            String survivalText = tvSurvivalRate.getText().toString().replace("%", "").trim();

            if (sampledWeightStr.isEmpty() || numSamplesStr.isEmpty() || feedingRateStr.isEmpty() || survivalText.isEmpty()) {
                Log.d(TAG, "computeValues: missing inputs");
                tvABWResult.setText("0.00 g");
                tvDFRResult.setText("0.00 g");
                tvDFRPerCycle.setText("0.00 g");
                updateFeedCost();
                return;
            }

            double sampledWeight = parseDouble(sampledWeightStr);
            double numSamples = parseDouble(numSamplesStr);
            double feedingRate = parseDouble(feedingRateStr);
            double survivalRate = parseDouble(survivalText);

            double abw = (numSamples > 0) ? (sampledWeight / numSamples) : 0;
            if (Double.isNaN(abw) || Double.isInfinite(abw)) abw = 0;
            tvABWResult.setText(String.format(Locale.getDefault(), "%.2f g", abw));
            Log.d(TAG, "ABW (g): " + abw);

            double totalStocks = parseDouble(tvTotalStocks.getText().toString());
            Log.d(TAG, "Total Stocks: " + totalStocks);

            double feedingRateDecimal = feedingRate / 100.0;
            double survivalDecimal = survivalRate / 100.0;

            double dfrGrams = feedingRateDecimal * survivalDecimal * abw * totalStocks;
            if (Double.isNaN(dfrGrams) || Double.isInfinite(dfrGrams)) dfrGrams = 0;

            tvDFRResult.setText(String.format(Locale.getDefault(), "%.2f g", dfrGrams));
            tvDFRPerCycle.setText(String.format(Locale.getDefault(), "%.2f g", (dfrGrams / 2.0)));
            Log.d(TAG, "DFR (g): " + dfrGrams);

            // Recalculate cost now that DFR changed
            updateFeedCost();

        } catch (Exception e) {
            Log.e(TAG, "computeValues error", e);
            tvABWResult.setText("0.00 g");
            tvDFRResult.setText("0.00 g");
            tvDFRPerCycle.setText("0.00 g");
            updateFeedCost();
        }
    }

    private double parseDouble(String s) {
        if (s == null) return 0.0;
        s = s.trim();
        if (s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0.0; }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void uploadDFRToAdafruit(String pondName, String feedingOne24, String feedingTwo24, double dfrFeedGrams) {
        OkHttpClient client = new OkHttpClient();

        String feedKey = "schedule"; // your Adafruit feed key
        String username = getString(R.string.adafruit_username);
        String aioKey = getString(R.string.adafruit_aio_key);

        // Divide by 10
        int reducedGrams = (int) Math.round(dfrFeedGrams / 10);

        // Value format: PondName|feedingOne,feedingTwo|reducedGrams
        String value = pondName + "|" + feedingOne24 + "," + feedingTwo24 + "|" + reducedGrams + "g";

        try {
            JSONObject json = new JSONObject();
            json.put("value", value);

            RequestBody body = RequestBody.create(json.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            String url = "https://io.adafruit.com/api/v2/" + username + "/feeds/" + feedKey + "/data";

            Log.d(TAG, "Posting to Adafruit: " + value);
            Log.d(TAG, "URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-AIO-Key", aioKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Adafruit request failed", e);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Failed to post to Adafruit: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String bodyStr = (response.body() != null) ? response.body().string() : "";
                    Log.d(TAG, "Adafruit response code: " + response.code());
                    Log.d(TAG, "Adafruit response body: " + bodyStr);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "DFR posted to Adafruit!", Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (JSONException ex) {
            Log.e(TAG, "JSON error", ex);
            Toast.makeText(getContext(), "JSON error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver feedUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float updated = FeedStorage.getRemainingFeed(context, pondId);
            Log.d("FEED_UI", "SamplingDialog updated feed display = " + updated);
            tvRemainingFeed.setText(String.format(Locale.getDefault(), "Remaining Feed: %.2f g", updated));
        }
    };

}
