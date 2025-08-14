package com.example.pondmatev1;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PondSyncManager {

    // Upload pond directly to server
    public static void uploadPondToServer(PondModel pond, String imageBase64, Callback callback) {
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
                        "&cost=" + pond.getCostPerFish() +
                        "&date_started=" + URLEncoder.encode(pond.getDateStarted(), "UTF-8") +
                        "&date_harvest=" + URLEncoder.encode(pond.getDateHarvest(), "UTF-8") +
                        "&image=" + URLEncoder.encode(imageBase64, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                // Parse JSON
                String response = sb.toString();
                JSONObject json = new JSONObject(response);
                String status = json.getString("status");
                String message = json.getString("message");

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if ("success".equalsIgnoreCase(status)) {
                            callback.onSuccess(response); // <-- send full JSON string
                        } else {
                            callback.onError(message);;
                        }
                    });
                }

            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }

    // Fetch ponds from MySQL server
    public static void fetchPondsFromServer(Callback callback) {
        new Thread(() -> {
            try {
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
                if (callback != null) callback.onSuccess(pondsArray);
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        }).start();
    }

    // Upload production cost directly
    public static void uploadProductionCostToServer(PondModel pond, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_production_cost.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Only send minimal info; PHP handles all three cost types
                String postData = "pond_id=" + pond.getId() +
                        "&fish_count=" + pond.getFishCount() +
                        "&cost_per_fish=" + pond.getCostPerFish();

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    callback.onSuccess(response.toString());
                } else {
                    callback.onError("Server returned code: " + responseCode);
                }

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }


    // Fetch production costs from server
    public static void fetchProductionCostsFromServer(Callback callback) {
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
                if (callback != null) callback.onSuccess(costsArray);
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        }).start();
    }

    // Callback interface for async results
    public interface Callback {
        void onSuccess(Object result);
        void onError(String error);
    }
}
