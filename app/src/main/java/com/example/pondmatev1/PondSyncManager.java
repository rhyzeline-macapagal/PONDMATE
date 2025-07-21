package com.example.pondmatev1;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PondSyncManager {

    public static void uploadPondToServer(Context context, PondModel pond) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_pond.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "name=" + URLEncoder.encode(pond.getName(), "UTF-8") +
                        "&breed=" + URLEncoder.encode(pond.getBreed(), "UTF-8") +
                        "&fish_count=" + pond.getFishCount() +
                        "&cost_per_fish=" + pond.getCostPerFish() +
                        "&date_started=" + URLEncoder.encode(pond.getDateStarted(), "UTF-8") +
                        "&date_harvest=" + URLEncoder.encode(pond.getDateHarvest(), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                reader.close();

                Log.d("PondUpload", "Server response: " + response);
            } catch (Exception e) {
                Log.e("PondUpload", "Error uploading pond: " + e.getMessage());
            }
        }).start();
    }

    // ✅ Sync ponds from MySQL server and reset local table first
    public static void syncPondsFromServer(Context context) {
        new Thread(() -> {
            try {
                Log.d("SYNC_DEBUG", "Started syncing from server...");

                URL url = new URL("https://pondmate.alwaysdata.net/get_ponds.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray pondsArray = new JSONArray(response.toString());
                DatabaseHelper db = new DatabaseHelper(context);

                // 🧹 Wipe out local SQLite pond data first
                db.clearPondsTable();

                // ⬇️ Now insert fresh ponds from MySQL
                for (int i = 0; i < pondsArray.length(); i++) {
                    JSONObject pond = pondsArray.getJSONObject(i);

                    String name = pond.getString("name");
                    String breed = pond.getString("breed");
                    int fishCount = pond.getInt("fish_count");
                    double costPerFish = pond.getDouble("cost_per_fish");
                    String dateStarted = pond.getString("date_started");
                    String dateHarvest = pond.getString("date_harvest");

                    db.insertPond(name, breed, fishCount, costPerFish, dateStarted, dateHarvest);
                    Log.d("PondSync", "Inserted pond: " + name);
                }

                Log.d("SYNC_DEBUG", "Sync complete. Total ponds: " + pondsArray.length());

            } catch (Exception e) {
                Log.e("PondSync", "Error syncing ponds: " + e.getMessage());
            }
        }).start();
    }



}
