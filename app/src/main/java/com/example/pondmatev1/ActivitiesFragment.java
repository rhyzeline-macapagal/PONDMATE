package com.example.pondmatev1;

import static com.example.pondmatev1.PondActionHelper.loadingDialog;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private JsonArray completedActivitiesJson;

    private boolean activitiesLoaded = false;
    private boolean completedLoaded = false;
    private String lastRenderedDate = null;

    private JsonObject fetchedActivities;
    private ArrayList<String> fetchedNotifications = new ArrayList<>();
    private int lastNotifId = 0; // Track the latest notification ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable android.os.Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activities, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        calendarView.addDecorator(new TodayDecorator());
        SessionManager s = new SessionManager(requireContext());
        Log.d("SESSION_TEST", "Loaded username = " + s.getUsername());

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

    private void showLoadingDialog(String message) {
        requireActivity().runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                TextView loadingText = loadingDialog.findViewById(R.id.loadingText);
                if (loadingText != null) loadingText.setText(message);
                return;
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
                    fetchCompletedActivitiesFromServer();

                    String dateCreated = null;

                    // If PHP does not send "pond", skip it
                    if (fetchedActivities.has("pond")) {
                        JsonObject pondInfo = fetchedActivities.getAsJsonObject("pond");
                        if (pondInfo != null && pondInfo.has("date_created")) {
                            dateCreated = pondInfo.get("date_created").getAsString();
                        }
                    }

                    // Fallback: use the FIRST activity date
                    if (dateCreated == null) {
                        JsonArray data = fetchedActivities.getAsJsonArray("data");
                        dateCreated = data.get(0).getAsJsonObject().get("date").getAsString();
                    }

                    Log.d("DATE_DEBUG", "date_created (final) = " + dateCreated);

                    String finalDateCreated = dateCreated;
                    requireActivity().runOnUiThread(() -> {
                        setCalendarRange(calendarView, finalDateCreated, fetchedActivities);
                        activitiesLoaded = true;
                    });
                }

            } catch (Exception e) {
                Log.e("FetchActivities", e.getMessage(), e);
            }
        }).start();
    }

    private void setCalendarRange(MaterialCalendarView calendarView, String dateCreatedStr, JsonObject json) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(dateCreatedStr);

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(startDate);
            endCal.add(Calendar.DAY_OF_YEAR, 193);

            calendarView.state().edit()
                    .setMinimumDate(CalendarDay.from(startCal))
                    .setMaximumDate(CalendarDay.from(endCal))
                    .commit();

            calendarView.setShowOtherDates(MaterialCalendarView.SHOW_NONE);

            Calendar todayCal = Calendar.getInstance();
            String defaultDateStr;

            if (!todayCal.before(startCal) && !todayCal.after(endCal)) {
                calendarView.setSelectedDate(CalendarDay.from(todayCal));
                defaultDateStr = sdf.format(todayCal.getTime());
                Log.d("DATE_DEBUG", "default selected date = " + defaultDateStr);

            } else {
                calendarView.setSelectedDate(CalendarDay.from(startCal));
                defaultDateStr = sdf.format(startCal.getTime());
                Log.d("DATE_DEBUG", "default selected date = " + defaultDateStr);

            }
            showActivitiesForDate(defaultDateStr);

            calendarView.setOnDateChangedListener((widget, date, selected) -> {
                String formatted = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d",
                        date.getYear(),
                        date.getMonth() + 1,
                        date.getDay());
                showActivitiesForDate(formatted);
            });

        } catch (Exception e) {
            Log.e("CALENDAR_RANGE", "Error: " + e.getMessage());
        }
    }
    private void showActivitiesForDate(String date) {
        Log.d("ShowActivities", "Rendering activities for date=" + date);

        if (fetchedActivities == null) {
            Log.e("ShowActivities", "ERROR: fetchedActivities is NULL");
            return;
        }
        activitiesLayout.removeAllViews();

        JsonArray activities = fetchedActivities.getAsJsonArray("data");
        Log.d("ShowActivities", "Total activities fetched = " + activities.size());

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
            boolean isDoneOnServer = isActivityDoneOnServer(actDate, title);
            checkBox.setChecked(isDoneOnServer);
            checkBox.setEnabled(!isDoneOnServer && actDate.equals(todayStr));

            Log.d("DATE_DEBUG", "showActivitiesForDate called for = " + date);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && actDate.equals(todayStr)) {

                    // Show confirmation dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Mark as Done")
                            .setMessage("Are you sure you want to mark \"" + title + "\" as completed?")
                            .setPositiveButton("Yes", (dialog, which) -> {


                                checkBox.setEnabled(false);
                                showToast("Marked as done ✅");

                                // Prepare notification values
                                SessionManager session = new SessionManager(requireContext());
                                String username = session.getUsername();
                                String notifMessage = title + " completed by " + username;
                                String scheduledFor = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                                sendActivityDoneToServer(pondId, username, actDate, title);


// ✅ ADD HERE ⬇⬇⬇
                                if (completedActivitiesJson == null) {
                                    completedActivitiesJson = new JsonArray();
                                }

                                JsonObject done = new JsonObject();
                                done.addProperty("activity_date", actDate);
                                done.addProperty("activity_title", title);
                                completedActivitiesJson.add(done);


                                // Store local notification
                                NotificationStore.NotificationItem notifItem =
                                        new NotificationStore.NotificationItem(pondName, notifMessage, scheduledFor);
                                NotificationStore.addNotification(requireContext(), notifItem);

                                // Show banner
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
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                // Undo checkbox check
                                checkBox.setChecked(false);
                            })
                            .setCancelable(false)
                            .show();
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

    private void sendActivityDoneToServer(String pondId, String username, String date, String title) {
        new Thread(() -> {
            try {
                Log.d("SendDone", "Sending to server: user=" + username +
                        ", pond=" + pondId + ", date=" + date + ", title=" + title);
                URL url = new URL("https://pondmate.alwaysdata.net/mark_activity_done.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String post = "username=" + username +
                        "&pond_id=" + pondId +
                        "&activity_date=" + date +
                        "&activity_title=" + title;
                Log.d("SendDone", "POST Body: " + post);

                OutputStream os = conn.getOutputStream();
                os.write(post.getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                Log.d("SendDone", "Response code = " + code);

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code == 200 ? conn.getInputStream() : conn.getErrorStream()
                ));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                Log.d("SendDone", "Server response = " + response.toString());
                conn.disconnect();




            } catch (Exception e) {
                Log.e("SendDone", e.getMessage());
            }
        }).start();
    }

    private boolean isActivityDoneOnServer(String date, String title) {
        if (completedActivitiesJson == null) {
            Log.w("IsDone", "completedActivitiesJson is NULL");
            return false;
        }

        for (int i = 0; i < completedActivitiesJson.size(); i++) {
            JsonObject obj = completedActivitiesJson.get(i).getAsJsonObject();
            Log.d("IsDone", "Checking server item: " +
                    obj.get("activity_date").getAsString() + " / " +
                    obj.get("activity_title").getAsString());

            if (obj.get("activity_date").getAsString().equals(date)
                    && obj.get("activity_title").getAsString().equals(title)) {
                Log.d("IsDone", "MATCH FOUND for " + title);
                return true;
            }
        }
        return false;
    }


    private void fetchCompletedActivitiesFromServer() {
        Log.d("CompletedFetch", "Fetching completed activities for pond=" + pondId);

        new Thread(() -> {
            try {
                URL url = new URL(
                        "https://pondmate.alwaysdata.net/get_completed_activities.php?pond_id=" + pondId
                );

                Log.d("CompletedFetch", "GET URL = " + url.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                Log.d("CompletedFetch", "RAW SERVER RESPONSE = " + response);

                completedActivitiesJson =
                        JsonParser.parseString(response.toString()).getAsJsonArray();

                requireActivity().runOnUiThread(() -> {
                    CalendarDay selected = calendarView.getSelectedDate();
                    if (selected != null) {
                        String formatted = String.format(Locale.getDefault(),
                                "%04d-%02d-%02d",
                                selected.getYear(),
                                selected.getMonth() + 1,
                                selected.getDay());
                        showActivitiesForDate(formatted);
                    }
                });

            } catch (Exception e) {
                Log.e("CompletedFetch", e.getMessage(), e);
            }
        }).start();
    }




    public static void sendNotification(String pondsId, String title, String message, String scheduledFor, String username, PondSyncManager.Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_notification.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // ✅ Send as URL-encoded form data
                String data = "ponds_id=" + URLEncoder.encode(pondsId, "UTF-8")
                        + "&title=" + URLEncoder.encode(title, "UTF-8")
                        + "&message=" + URLEncoder.encode(message, "UTF-8")
                        + "&username=" + URLEncoder.encode(username, "UTF-8")
                        + "&scheduled_for=" + URLEncoder.encode(scheduledFor, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes("UTF-8"));
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
                            JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
                            JsonArray data = root.getAsJsonArray("notifications");

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
                                    fetchCompletedActivitiesFromServer();
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
        fetchCompletedActivitiesFromServer();
        handler.post(notificationPollRunnable);

    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(notifReceiver, new IntentFilter("POND_NOTIFICATION_UPDATED"));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(notifReceiver);
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private BroadcastReceiver notifReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the new notifications sent by NotificationPoller
            ArrayList<JsonObject> newNotifs = (ArrayList<JsonObject>) intent.getSerializableExtra("new_notifications");
            if (newNotifs == null || newNotifs.isEmpty()) return;

            for (JsonObject notif : newNotifs) {
                String title = notif.get("title").getAsString();
                String message = notif.get("message").getAsString();
                String pondName = ""; // optional: fetch if you store pond info
                int notifId = notif.get("id").getAsInt();

                // Avoid duplicate banners
                if (notifId <= lastNotifId) continue;
                lastNotifId = Math.max(lastNotifId, notifId);

                // Show banner
                NotificationHelper.showActivityDoneNotification(
                        requireContext(),
                        pondName,
                        message,
                        true,
                        "",
                        message.hashCode()
                );
            }

            // Update UI silently
            fetchCompletedActivitiesFromServer();
            CalendarDay selected = calendarView.getSelectedDate();
            if (selected != null) {
                String formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        selected.getYear(), selected.getMonth() + 1, selected.getDay());
                showActivitiesForDate(formatted);
            }
        }
    };



}
