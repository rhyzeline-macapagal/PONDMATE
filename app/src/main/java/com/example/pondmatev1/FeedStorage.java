package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FeedStorage {

    private static final String PREF_NAME = "FEED_LEVEL_PREF";

    public static float getRemainingFeed(Context context, String pondId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat("feed_remaining_" + pondId, 0f);
    }

    public static void setRemainingFeed(Context context, String pondId, float value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putFloat("feed_remaining_" + pondId, Math.max(value, 0)).apply();
    }

    public static void addFeed(Context context, String pondId, float amount) {
        float current = getRemainingFeed(context, pondId);
        float maxCapacity = 10000f; // 10kg = 10000 grams

        float newLevel = current + amount;

        // ✅ If new total exceeds capacity, notify user and stop
        if (newLevel > maxCapacity) {

            // How much can still be stored
            float spaceLeft = maxCapacity - current;

            new android.app.AlertDialog.Builder(context)
                    .setTitle("Max Capacity Reached")
                    .setMessage("The container can only hold up to 10,000g.\n\n" +
                            "Current level: " + current + "g\n" +
                            "You attempted to add: " + amount + "g\n\n" +
                            "Only " + spaceLeft + "g more can be stored.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();

            return; // ❗ DO NOT STORE ANYTHING
        }

        // ✅ Normal store (within limit)
        setRemainingFeed(context, pondId, newLevel);

        Log.d("FEED_STORAGE", "🔺 Added feed: " + amount + "g | New Level: " + newLevel + "g");

        logFeedAction(context, pondId, amount, "ADD", newLevel);
        sendLogToServer(context, pondId, amount, "ADD", newLevel);
        sendRemainingToServer(context, pondId, newLevel);

        context.sendBroadcast(new Intent("FEED_LEVEL_UPDATED"));
    }

    public static void deductFeed(Context context, String pondId, float amount) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        float current = prefs.getFloat("feed_remaining_" + pondId, 0f);

        Log.d("FEED_STORAGE", "🔻 Deduction triggered | pond=" + pondId);
        Log.d("FEED_STORAGE", "Current feed before deduction: " + current + "g");
        Log.d("FEED_STORAGE", "Scheduled deduction amount: " + amount + "g");

        float newLevel = Math.max(0, current - amount);

        // Use commit() here to synchronously write (optional if you pass newLevel)
        prefs.edit().putFloat("feed_remaining_" + pondId, newLevel).apply();

        Log.d("FEED_STORAGE", "✅ New feed level after deduction: " + newLevel + "g");

        // Log/send using the newLevel directly — avoids race
        logFeedAction(context, pondId, amount, "DEDUCT", newLevel);
        sendLogToServer(context, pondId, amount, "DEDUCT", newLevel);
        sendRemainingToServer(context, pondId, newLevel);

        context.sendBroadcast(new Intent("FEED_DEDUCTION_APPLIED"));
    }

    public static void logFeedAction(Context context, String pondId, float amount, String actionType, float remainingAfter) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String record = timestamp + " | " + actionType + " | " + amount + "g | remaining:" + remainingAfter + "g";

        SharedPreferences prefs = context.getSharedPreferences("FeedHistory_" + pondId, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // ✅ Clone the set to avoid reference issues
        Set<String> logs = new HashSet<>(prefs.getStringSet("records", new HashSet<>()));

        logs.add(record);

        editor.putStringSet("records", logs);
        editor.apply();

        Log.d("FEED_LOG", "📌 Logged feed action: " + record);
    }

    public static void sendLogToServer(Context context, String pondId, float amount, String actionType, float remainingAfter) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/log_feed_storage.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String data = "pond_id=" + URLEncoder.encode(pondId, "UTF-8") +
                        "&amount=" + URLEncoder.encode(String.valueOf(amount), "UTF-8") +
                        "&action_type=" + URLEncoder.encode(actionType, "UTF-8") +
                        "&remaining_after=" + URLEncoder.encode(String.valueOf(remainingAfter), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("FEED_LOG_DB", "HTTP Code: " + responseCode);

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                Log.d("FEED_LOG_DB", "Server reply: " + sb.toString());

                conn.disconnect();
            } catch (Exception e) {
                Log.e("FEED_LOG_DB", "❌ Failed: " + e.getMessage());
            }
        }).start();
    }


    public static void sendRemainingToServer(Context context, String pondId, float remaining) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_remaining_feed.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String data = "pond_id=" + URLEncoder.encode(pondId, "UTF-8") +
                        "&remaining=" + URLEncoder.encode(String.valueOf(remaining), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("FEED_LEVEL_DB", "HTTP Code: " + responseCode);

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(
                                responseCode >= 200 && responseCode < 300 ?
                                        conn.getInputStream() : conn.getErrorStream()
                        )
                );
                String line, response = "";
                while ((line = br.readLine()) != null) {
                    response += line;
                }
                br.close();

                Log.d("FEED_LEVEL_DB", "Server reply: " + response);

                conn.disconnect();

            } catch (Exception e) {
                Log.e("FEED_LEVEL_DB", "❌ Failed: " + e.getMessage());
            }
        }).start();
    }

    public static void fetchRemainingFeed(Context context, String pondId) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_feed_level.php?pond_id=" + pondId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String result = reader.readLine();
                reader.close();

                // Save to SharedPreferences for app UI to use
                setRemainingFeed(context, pondId, Float.parseFloat(result));

                Log.d("FEED_SYNC", "✅ Synced remaining feed: " + result + "g");

                // Notify UI to refresh if needed
                context.sendBroadcast(new Intent("FEED_LEVEL_UPDATED"));

            } catch (Exception e) {
                Log.e("FEED_SYNC", "❌ Sync failed: " + e.getMessage());
            }
        }).start();
    }

    public static void fetchFeedStorageLogs(Context context, String pondId, StorageLogsCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_feed_storage_logs.php?pond_id=" + pondId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                callback.onLogsFetched(response.toString());

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public interface StorageLogsCallback {
        void onLogsFetched(String json);
        void onError(String error);
    }


}
