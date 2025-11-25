package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ActivitiesFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private LinearLayout activitiesLayout;
    private String pondName;
    private String pondId;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable notificationPollRunnable;
    private int pollInterval = 5000;

    private JsonObject fetchedActivities;
    private ArrayList<String> fetchedNotifications = new ArrayList<>();
    private int lastNotifId = 0; // Track the latest notification ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable android.os.Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activities, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        activitiesLayout = view.findViewById(R.id.activitiesLayout);

        SharedPreferences prefs = requireContext().getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
        String pondJson = prefs.getString("selected_pond", null);

        if (pondJson != null) {
            PondModel pond = new Gson().fromJson(pondJson, PondModel.class);
            pondName = pond.getName();
            pondId = pond.getId();
        } else {
            showToast("No pond selected.");
        }

        fetchActivitiesFromServer();
        startNotificationPolling();
        return view;
    }

    private void fetchActivitiesFromServer() {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_activities.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_name=" + pondName;
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                conn.disconnect();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (json.get("status").getAsString().equals("success")) {
                    fetchedActivities = json;

                    // Show today's activities immediately
                    String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    handler.post(() -> showActivitiesForDate(todayStr));
                    handler.post(this::setupCalendarListener);
                }

            } catch (Exception e) {
                Log.e("FetchActivities", e.getMessage(), e);
            }
        }).start();
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            String formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    date.getYear(), date.getMonth() + 1, date.getDay());
            showActivitiesForDate(formatted);
        });
    }

    private void showActivitiesForDate(String date) {
        if (fetchedActivities == null) return;
        activitiesLayout.removeAllViews();

        JsonArray activities = fetchedActivities.getAsJsonArray("data");
        SharedPreferences prefs = requireContext().getSharedPreferences("ACTIVITY_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        boolean hasActivities = false;

        for (int i = 0; i < activities.size(); i++) {
            JsonObject act = activities.get(i).getAsJsonObject();
            String actDate = act.get("date").getAsString();
            if (!actDate.equals(date)) continue;

            hasActivities = true;

            String title = act.get("title").getAsString();
            String description = act.get("description").getAsString();
            int dayNumber = act.get("day_number").getAsInt();
            String key = pondName + "_" + actDate + "_" + title;

            LinearLayout wrapper = new LinearLayout(getContext());
            wrapper.setOrientation(LinearLayout.VERTICAL);

            android.widget.CheckBox checkBox = new android.widget.CheckBox(getContext());
            checkBox.setText("Day " + dayNumber + ": " + title);
            checkBox.setChecked(prefs.getBoolean(key, false));
            checkBox.setEnabled(!prefs.getBoolean(key, false) && actDate.equals(todayStr));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && actDate.equals(todayStr)) {
                    editor.putBoolean(key, true).apply();
                    checkBox.setEnabled(false);
                    showToast("Marked as done ✅");

                    // Send notification/banner ONLY when user checks the box
                    SessionManager session = new SessionManager(requireContext());
                    String username = session.getUsername();
                    String notifMessage = title + " completed by " + username;
                    String scheduledFor = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                    NotificationStore.NotificationItem notifItem =
                            new NotificationStore.NotificationItem(pondName, notifMessage, scheduledFor);
                    NotificationStore.addNotification(requireContext(), notifItem);

                    // Show the banner now
                    NotificationHelper.showActivityDoneNotification(
                            requireContext(),
                            pondName,
                            notifMessage,
                            true,
                            "",
                            notifMessage.hashCode()
                    );

                    // Send to server
                    ActivitiesFragment.sendNotification(
                            pondId, title, notifMessage, scheduledFor, username, null
                    );
                }
            });

            TextView descView = new TextView(getContext());
            descView.setText(description);
            descView.setPadding(30, 0, 0, 0);

            wrapper.addView(checkBox);
            wrapper.addView(descView);
            activitiesLayout.addView(wrapper);
        }

        if (!hasActivities) {
            TextView empty = new TextView(getContext());
            empty.setText("No activities for this date.");
            activitiesLayout.addView(empty);
        }
    }

    public static void sendNotification(String pondId, String title, String message, String scheduledFor, String username, PondSyncManager.Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_notification.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("pond_id", pondId);
                json.put("title", title);
                json.put("message", message);
                json.put("username", username);
                json.put("scheduled_for", scheduledFor);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    if (callback != null) callback.onSuccess(response.toString());
                } else {
                    if (callback != null) callback.onError("Server error: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                if (callback != null) callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    private void startNotificationPolling() {
        notificationPollRunnable = new Runnable() {
            @Override
            public void run() {
                PondSyncManager.fetchNotifications(new PondSyncManager.Callback() {
                    @Override
                    public void onSuccess(Object response) {
                        try {
                            JsonArray data = JsonParser.parseString(response.toString()).getAsJsonArray();
                            ArrayList<JsonObject> newNotifs = new ArrayList<>();

                            for (int i = 0; i < data.size(); i++) {
                                JsonObject notif = data.get(i).getAsJsonObject();
                                int notifId = notif.get("id").getAsInt();

                                if (notifId <= lastNotifId) continue;

                                lastNotifId = Math.max(lastNotifId, notifId);
                                newNotifs.add(notif);
                            }

                            if (!newNotifs.isEmpty() && isAdded()) {
                                Handler mainHandler = new Handler(Looper.getMainLooper());

                                // Only update the UI silently
                                mainHandler.post(() -> {
                                    CalendarDay selected = calendarView.getSelectedDate();
                                    if (selected != null) {
                                        String formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                                selected.getYear(), selected.getMonth() + 1, selected.getDay());
                                        showActivitiesForDate(formatted);
                                    }
                                });
                            }

                        } catch (Exception e) {
                            Log.e("NotifPolling", "Error parsing notifications: " + e.getMessage(), e);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("NotifPolling", "Error fetching notifications: " + error);
                    }
                });

                if (isAdded()) handler.postDelayed(this, pollInterval);
            }
        };
        handler.post(notificationPollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startNotificationPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (notificationPollRunnable != null) handler.removeCallbacks(notificationPollRunnable);
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }
}
