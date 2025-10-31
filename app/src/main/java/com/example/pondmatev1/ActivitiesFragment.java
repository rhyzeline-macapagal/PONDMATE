package com.example.pondmatev1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ActivitiesFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private LinearLayout activitiesLayout;
    private ProgressBar progressBar;
    private TextView activitiesHeader, tvTitle;
    private AlertDialog loadingDialog;


    private String pondName;

    public ActivitiesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_activities, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        activitiesLayout = view.findViewById(R.id.activitiesLayout);
        activitiesHeader = view.findViewById(R.id.activitiesHeader);
        tvTitle = view.findViewById(R.id.tvTitle);

        tvTitle.setText("Pond Activities");

        calendarView.addDecorator(new TodayDecorator());
        // ✅ Load pond name from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();

            // Fetch pond date_created using the selected pond name
            fetchPondDateCreated(pondName);
        } else {
            Toast.makeText(getContext(), "No pond selected.", Toast.LENGTH_SHORT).show();
        }

        return view;
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


    private void fetchPondDateCreated(String pondName) {

        showLoadingDialog("Loading calendar...");

        PondSyncManager.fetchPondDateCreated(pondName, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                hideLoadingDialog();

                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (json.get("status").getAsString().equals("success")) {
                        String dateCreated = json.get("date_created").getAsString();
                        // ✅ Now fetch activities using the same pond
                        fetchActivities(pondName, dateCreated);

                    } else {
                        showToast("Failed to load pond date.");
                        hideLoadingDialog();

                    }

                } catch (Exception e) {
                    showToast("Error parsing response.");
                    hideLoadingDialog();

                }
            }

            @Override
            public void onError(String error) {
                showToast("Network error: " + error);
                hideLoadingDialog();
            }
        });
    }

    private void fetchActivities(String pondName, String dateCreated) {
        showLoadingDialog("Loading activities...");

        PondSyncManager.fetchPondActivities(pondName, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (json.get("status").getAsString().equals("success")) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            hideLoadingDialog();
                            setCalendarRange(calendarView, dateCreated, json);
                        });
                    }
                } catch (Exception e) {
                    showToast("Error parsing activities.");
                    hideLoadingDialog();
                }
            }

            @Override
            public void onError(String error) {
                showToast("Network error: " + error);
                hideLoadingDialog();
            }
        });
    }

    /**
     * Configure calendar to only show 194 days from date_created
     */
    private void setCalendarRange(MaterialCalendarView calendarView, String dateCreatedStr, JsonObject json) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(dateCreatedStr);

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(startDate);
            endCal.add(Calendar.DAY_OF_YEAR, 193); // 194 days total

            calendarView.state().edit()
                    .setMinimumDate(CalendarDay.from(startCal))
                    .setMaximumDate(CalendarDay.from(endCal))
                    .commit();

            calendarView.setShowOtherDates(MaterialCalendarView.SHOW_NONE);

// Default selection: today if in range, else start date
            Calendar todayCal = Calendar.getInstance();
            String defaultDateStr;
            if (!todayCal.before(startCal) && !todayCal.after(endCal)) {
                calendarView.setSelectedDate(CalendarDay.from(todayCal));
                defaultDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayCal.getTime());
            } else {
                calendarView.setSelectedDate(CalendarDay.from(startCal));
                defaultDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCal.getTime());
            }

// Show activities for the default date
            showActivitiesForDate(json, defaultDateStr);

// Handle date selection by user
            calendarView.setOnDateChangedListener((widget, date, selected) -> {
                String formattedDate = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d",
                        date.getYear(),
                        date.getMonth() + 1,
                        date.getDay());
                showActivitiesForDate(json, formattedDate);
            });


        } catch (Exception e) {
            Log.e("CALENDAR_RANGE", "Error: " + e.getMessage());
        }
    }

    /**
     * Example function to show activities for selected date
     */
    private void showActivitiesForDate(JsonObject json, String date) {
        activitiesLayout.removeAllViews();
        if (!json.has("data")) return;

        JsonArray activities = json.getAsJsonArray("data");

        boolean found = false;

        // Get today in yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        SharedPreferences prefs = requireContext().getSharedPreferences("ACTIVITY_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (int i = 0; i < activities.size(); i++) {
            JsonObject act = activities.get(i).getAsJsonObject();
            String actDate = act.get("date").getAsString();

            if (actDate.equals(date)) {
                found = true;

                String title = act.get("title").getAsString();
                String description = act.get("description").getAsString();
                int dayNumber = act.get("day_number").getAsInt();

                String key = pondName + "_" + actDate + "_" + title;

                LinearLayout itemLayout = new LinearLayout(getContext());
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setPadding(10, 10, 10, 10);

                android.widget.CheckBox checkBox = new android.widget.CheckBox(getContext());
                checkBox.setText("Day " + dayNumber + ": " + title);
                checkBox.setChecked(prefs.getBoolean(key, false));

                // ✅ Only allow checking if it's today
                checkBox.setEnabled(actDate.equals(todayStr));

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (actDate.equals(todayStr)) {
                        editor.putBoolean(key, isChecked);
                        editor.apply();
                        if (isChecked) {
                            Toast.makeText(getContext(), "Marked as done ✅", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                TextView descView = new TextView(getContext());
                descView.setText(description);
                descView.setPadding(30, 0, 0, 0);

                LinearLayout wrapper = new LinearLayout(getContext());
                wrapper.setOrientation(LinearLayout.VERTICAL);
                wrapper.addView(checkBox);
                wrapper.addView(descView);

                activitiesLayout.addView(wrapper);
            }
        }

        if (!found) {
            TextView empty = new TextView(getContext());
            empty.setText("No activities for this date.");
            activitiesLayout.addView(empty);
        }
    }



    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }
}
