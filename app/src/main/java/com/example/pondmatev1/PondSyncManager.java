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

    public static void uploadProductionCostToServer(Context context,
                                                    int pondId,
                                                    String costType,
                                                    String description,
                                                    int quantity,
                                                    String unit,
                                                    double costPerUnit,
                                                    double amount,
                                                    String dateRecorded,
                                                    String createdAt) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_production_cost.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData =
                        "pond_id=" + pondId +
                                "&cost_type=" + URLEncoder.encode(costType, "UTF-8") +
                                "&description=" + URLEncoder.encode(description, "UTF-8") +
                                "&quantity=" + quantity +
                                "&unit=" + URLEncoder.encode(unit, "UTF-8") +
                                "&cost_per_unit=" + costPerUnit +
                                "&amount=" + amount +
                                "&date_recorded=" + URLEncoder.encode(dateRecorded, "UTF-8") +
                                "&created_at=" + URLEncoder.encode(createdAt, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                reader.close();

                Log.d("ProdCostUpload", "Server response: " + response);
            } catch (Exception e) {
                Log.e("ProdCostUpload", "Error uploading production cost: " + e.getMessage());
            }
        }).start();
    }


    public static void syncProductionCostsFromServer(Context context) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_production_costs.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray costsArray = new JSONArray(response.toString());

                DatabaseHelper db = new DatabaseHelper(context);
                db.clearProductionCostsTable();  // 🧹 clear old data

                for (int i = 0; i < costsArray.length(); i++) {
                    JSONObject cost = costsArray.getJSONObject(i);

                    int pondId = cost.getInt("pond_id");
                    String costType = cost.getString("cost_type");
                    String description = cost.getString("description");
                    int quantity = cost.getInt("quantity");
                    String unit = cost.getString("unit");
                    double costPerUnit = cost.getDouble("cost_per_unit");
                    double amount = cost.getDouble("amount");
                    String dateRecorded = cost.getString("date_recorded");
                    String createdAt = cost.getString("created_at");

                    db.insertProductionCost(pondId, costType, description, quantity, unit, costPerUnit, amount, dateRecorded, createdAt);
                }

                Log.d("SYNC_COSTS", "Production cost sync complete. Total: " + costsArray.length());

            } catch (Exception e) {
                Log.e("SYNC_COSTS", "Error syncing costs: " + e.getMessage());
            }
        }).start();
    }




}
