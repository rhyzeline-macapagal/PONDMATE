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

        for (int i = 0; i < activities.size(); i++) {
            JsonObject act = activities.get(i).getAsJsonObject();
            String actDate = act.get("date").getAsString();
            if (!actDate.equals(date)) continue;

            String title = act.get("title").getAsString();
            String description = act.get("description").getAsString();
            int dayNumber = act.get("day_number").getAsInt();
            String key = pondName + "_" + actDate + "_" + title;

            LinearLayout wrapper = new LinearLayout(getContext());
            wrapper.setOrientation(LinearLayout.VERTICAL);

            android.widget.CheckBox checkBox = new android.widget.CheckBox(getContext());
            checkBox.setText("Day " + dayNumber + ": " + title);
            checkBox.setChecked(prefs.getBoolean(key, false));
            checkBox.setEnabled(actDate.equals(todayStr));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && actDate.equals(todayStr)) {
                    editor.putBoolean(key, true).apply();
                    showToast("Marked as done ✅");

                    SessionManager session = new SessionManager(requireContext());
                    String username = session.getUsername();
                    String userId = session.getUserId(); // current user

                    // Local notification for the user who checked the box
                    NotificationHelper.showActivityDoneNotification(
                            requireContext(),
                            pondName,
                            title,
                            true,  // local
                            username
                    );

                    // Broadcast to all OTHER users only
                    addNotificationToServer(
                            pondId,
                            "", // empty title for broadcast
                            title + " completed by " + username, // full message
                            actDate,
                            userId // pass current user's ID so server can skip sending to them
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

        if (activities.size() == 0) {
            TextView empty = new TextView(getContext());
            empty.setText("No activities for this date.");
            activitiesLayout.addView(empty);
        }
    }

    private void addNotificationToServer(String pondId, String title, String message, String scheduledFor, String userId) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_notification.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("ponds_id", pondId);
                payload.put("title", title);
                payload.put("message", message);
                payload.put("scheduled_for", scheduledFor);
                payload.put("user_id", userId);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                conn.disconnect();

                Log.d("ADD_NOTIFICATION", "Response: " + response);

            } catch (Exception e) {
                Log.e("ADD_NOTIFICATION", "Error: " + e.getMessage(), e);
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
                            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                            if (json.get("status").getAsString().equals("success")) {
                                JsonArray data = json.getAsJsonArray("data");
                                boolean hasNew = false;

                                for (int i = 0; i < data.size(); i++) {
                                    JsonObject notif = data.get(i).getAsJsonObject();

                                    String notifKey = notif.get("id").getAsString();
                                    if (fetchedNotifications.contains(notifKey)) continue;

                                    fetchedNotifications.add(notifKey);
                                    hasNew = true;

                                    String notifMessage = notif.get("message").getAsString();

                                    // Display broadcast or user notifications (5 args)
                                    NotificationHelper.showActivityDoneNotification(
                                            requireContext(),
                                            pondName,
                                            notifMessage, // full message already includes username
                                            false,        // broadcast
                                            ""            // username ignored for broadcast
                                    );
                                }

                                if (hasNew) {
                                    CalendarDay selected = calendarView.getSelectedDate();
                                    if (selected != null) {
                                        String formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                                selected.getYear(), selected.getMonth() + 1, selected.getDay());
                                        showActivitiesForDate(formatted);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("NotifPolling", "Error parsing notifications: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("NotifPolling", "Error fetching notifications: " + error);
                    }
                });

                handler.postDelayed(this, pollInterval);
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
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }
}
