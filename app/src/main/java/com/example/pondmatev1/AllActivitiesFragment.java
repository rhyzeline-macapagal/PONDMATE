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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AllActivitiesFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private LinearLayout activitiesLayout;
    private TextView activitiesHeader, activitiesInstruction;
    private AlertDialog loadingDialog;

    private JsonArray allActivitiesArray = new JsonArray(); // Stores all ponds’ activities
    private Set<String> availableDates = new HashSet<>();

    public AllActivitiesFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_activities, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        activitiesLayout = view.findViewById(R.id.activitiesLayout);
        activitiesHeader = view.findViewById(R.id.activitiesHeader);
        activitiesInstruction = view.findViewById(R.id.activitiesInstruction);

        // Week mode for compact view
        calendarView.state().edit()
                .setCalendarDisplayMode(CalendarMode.WEEKS)
                .commit();

        calendarView.setTopbarVisible(true);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {
                String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        date.getYear(), date.getMonth() + 1, date.getDay());
                showActivitiesForDate(selectedDate);
            }
        });
        // Fetch activities from all ponds
        fetchAllPondActivities();

        return view;
    }

    private void showLoadingDialog(String message) {
        if (getContext() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) return;
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

    private void fetchAllPondActivities() {
        showLoadingDialog("Loading all activities...");

        PondSyncManager.fetchAllPondsActivities(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                hideLoadingDialog();
                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (json.get("status").getAsString().equals("success")) {

                        // ✅ Get the earliest and latest dates from server
                        String startDateStr = json.get("start_date").getAsString();
                        String endDateStr = json.get("end_date").getAsString();

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date startDate = sdf.parse(startDateStr);
                        Date endDate = sdf.parse(endDateStr);

                        Calendar startCal = Calendar.getInstance();
                        Calendar endCal = Calendar.getInstance();
                        startCal.setTime(startDate);
                        endCal.setTime(endDate);
                        requireActivity().runOnUiThread(() -> {
                            calendarView.state().edit()
                                    .setMinimumDate(CalendarDay.from(startCal))
                                    .setMaximumDate(CalendarDay.from(endCal))
                                    .setCalendarDisplayMode(CalendarMode.WEEKS)
                                    .commit();

                            // ✅ Focus today if inside range, otherwise start date
                            Calendar today = Calendar.getInstance();
                            CalendarDay todayDay = CalendarDay.from(today);
                            if (!today.before(startCal) && !today.after(endCal)) {
                                calendarView.setCurrentDate(todayDay);
                                calendarView.setSelectedDate(todayDay);
                            } else {
                                calendarView.setCurrentDate(CalendarDay.from(startCal));
                                calendarView.setSelectedDate(CalendarDay.from(startCal));
                            }

                            // Continue updating UI
                            highlightAvailableDates();
                            showActivitiesForDate(
                                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())
                            );
                        });
// ✅ Process ponds and activities as before
                        JsonArray pondsArray = json.getAsJsonArray("data");
                        availableDates.clear();
                        allActivitiesArray = new JsonArray();

                        for (int i = 0; i < pondsArray.size(); i++) {
                            JsonObject pond = pondsArray.get(i).getAsJsonObject();
                            String pondName = pond.get("pond_name").getAsString();
                            JsonArray activities = pond.getAsJsonArray("activities");

                            for (int j = 0; j < activities.size(); j++) {
                                JsonObject activity = activities.get(j).getAsJsonObject();
                                activity.addProperty("pond_name", pondName); // ✅ add missing key
                                String date = activity.get("date").getAsString();
                                availableDates.add(date);
                                allActivitiesArray.add(activity);
                            }
                        }

                        requireActivity().runOnUiThread(() -> {
                            highlightAvailableDates();
                            showActivitiesForDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(new Date())); // auto show today's activities
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error parsing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Failed to load: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }




    /**
     * Highlights all available activity dates on the calendar.
     */
    private void highlightAvailableDates() {
        if (getContext() == null || calendarView == null) return;

        // 🧹 Clear old decorations first
        calendarView.removeDecorators();

        // 🎨 Convert your availableDates (strings) into CalendarDay objects
        HashSet<CalendarDay> datesToHighlight = new HashSet<>();
        for (String dateStr : availableDates) {
            try {
                String[] parts = dateStr.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1; // zero-based month
                int day = Integer.parseInt(parts[2]);
                datesToHighlight.add(CalendarDay.from(year, month, day));
            } catch (Exception e) {
                Log.e("CALENDAR_HIGHLIGHT", "Invalid date: " + dateStr);
            }
        }

        // 🌕 Add a decorator that visually highlights the dates
        calendarView.addDecorator(new EventDecorator(
                requireContext(),
                datesToHighlight
        ));
    }


    private void showActivitiesForDate(String date) {
        activitiesLayout.removeAllViews();
        boolean found = false;

        SharedPreferences prefs = requireContext().getSharedPreferences("ALL_ACTIVITY_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        for (int i = 0; i < allActivitiesArray.size(); i++) {
            JsonObject act = allActivitiesArray.get(i).getAsJsonObject();
            String actDate = act.get("date").getAsString();

            if (actDate.equals(date)) {
                found = true;

                String pondName = act.get("pond_name").getAsString();
                String title = act.get("title").getAsString();
                String description = act.get("description").getAsString();

                String key = pondName + "_" + actDate + "_" + title;

                LinearLayout wrapper = new LinearLayout(getContext());
                wrapper.setOrientation(LinearLayout.VERTICAL);
                wrapper.setPadding(10, 10, 10, 10);

                CheckBox cb = new CheckBox(getContext());
                cb.setText(pondName + ": " + title);
                cb.setChecked(prefs.getBoolean(key, false));
                cb.setEnabled(actDate.equals(todayStr));

                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (actDate.equals(todayStr)) {
                        editor.putBoolean(key, isChecked);
                        editor.apply();
                        if (isChecked)
                            Toast.makeText(getContext(), "Marked as done ✅", Toast.LENGTH_SHORT).show();
                    }
                });

                TextView desc = new TextView(getContext());
                desc.setText(description);
                desc.setPadding(40, 0, 0, 0);

                wrapper.addView(cb);
                wrapper.addView(desc);
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
