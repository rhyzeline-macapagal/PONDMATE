package com.example.pondmatev1;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class PondSyncManager {

    public static void postRequest(String endpoint, String postParams, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/" + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Send POST data
                OutputStream os = conn.getOutputStream();
                os.write(postParams.getBytes());
                os.flush();
                os.close();

                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String response = sb.toString();

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onSuccess(response);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    // 🔹 Accepts Map<String,String> and converts it to POST params
    public static void postData(String endpoint, Map<String, String> params, Callback callback) {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (postData.length() != 0) postData.append("&");
            postData.append(entry.getKey()).append("=").append(entry.getValue());
        }
        postRequest(endpoint, postData.toString(), callback);
    }


    public static void uploadPondToServer(Context context, PondModel pond, String imageBase64, Callback callback) {
        new Thread(() -> {
            try {
                // ✅ Prepare connection
                URL url = new URL("https://pondmate.alwaysdata.net/add_pond.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // ✅ Get logged-in username
                SessionManager session = new SessionManager(context);
                String ownerUsername = session.getUsername();

                // ✅ Build POST data
                String postData = "name=" + URLEncoder.encode(pond.getName(), "UTF-8") +
                        "&area=" + URLEncoder.encode(String.valueOf(pond.getPondArea()), "UTF-8") +
                        "&date_created=" + URLEncoder.encode(pond.getDateStarted(), "UTF-8") +
                        "&stocking_date=" + URLEncoder.encode(pond.getDateStocking(), "UTF-8") +
                        "&image=" + URLEncoder.encode(imageBase64, "UTF-8") +
                        "&owner_username=" + URLEncoder.encode(ownerUsername, "UTF-8"); // 🟩 Added

                // ✅ Send request
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                // ✅ Read server response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                // ✅ Parse JSON response
                String response = sb.toString();
                Log.d("ServerResponse", "Response: " + response);
                JSONObject json = new JSONObject(response);
                String status = json.optString("status", "error");
                String message = json.optString("message", "Unknown error");

                // ✅ Handle response on main thread
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if ("success".equalsIgnoreCase(status)) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError(message);
                        }
                    });
                }

            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Error: " + e.getMessage()));
                }
            }
        }).start();
    }

    public static void fetchAllPondsActivities(Callback callback) {
        new Thread(() -> {
            try {
                // ✅ Automatically filters by today and next 7 days
                String urlString = "https://pondmate.alwaysdata.net/get_all_pond_date_created.php";
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoInput(true);

                int responseCode = conn.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    if (callback != null) callback.onSuccess(response.toString());
                } else {
                    if (callback != null)
                        callback.onError("Server returned code: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                if (callback != null)
                    callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    // 🔹 Update existing feeding schedule
    public static void updateFeedingScheduleOnServer(String pondName,
                                                     String schedOne,
                                                     String schedTwo,
                                                     String schedThree,
                                                     double feedAmount,
                                                     float fishWeight,
                                                     float feedPrice,
                                                     Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_feeding_schedule.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                Log.d("UPDATE_FEED", "Updating pond=" + pondName +
                        ", sched1=" + schedOne +
                        ", sched2=" + schedTwo +
                        ", sched3=" + schedThree +
                        ", amount=" + feedAmount +
                        ", fish_weight=" + fishWeight +
                        ", feed_price=" + feedPrice);

                // 🔑 POST data (pond_name is identifier for update)
                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8") +
                        "&sched_one=" + URLEncoder.encode(schedOne != null ? schedOne : "", "UTF-8") +
                        "&sched_two=" + URLEncoder.encode(schedTwo != null ? schedTwo : "", "UTF-8") +
                        "&sched_three=" + URLEncoder.encode(schedThree != null ? schedThree : "", "UTF-8") +
                        "&feed_amount=" + feedAmount +
                        "&fish_weight=" + fishWeight +
                        "&feed_price=" + feedPrice;

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

                    runOnUiThreadSafe(() -> callback.onSuccess(response.toString()));
                } else {
                    runOnUiThreadSafe(() -> callback.onError("Server returned code: " + responseCode));
                }

            } catch (Exception e) {
                runOnUiThreadSafe(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }


    public static void stockFingerlingsOnServer(String pondName,
                                                String species,
                                                int fishCount,
                                                double costPerFish,
                                                String mortalityRate,
                                                String harvestDate,
                                                Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/stock_fingerlings.php"); // ✅ your PHP endpoint
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                Log.d("STOCK_FINGERLINGS", "Updating pond=" + pondName +
                        ", species=" + species +
                        ", fishCount=" + fishCount +
                        ", costPerFish=" + costPerFish +
                        ", mortalityRate=" + mortalityRate +
                        ", harvestDate=" + harvestDate);

                // 🔑 Prepare POST data
                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8") +
                        "&species=" + URLEncoder.encode(species, "UTF-8") +
                        "&fish_count=" + fishCount +
                        "&cost_per_fish=" + costPerFish +
                        "&mortality_rate=" + URLEncoder.encode(mortalityRate, "UTF-8") +
                        "&date_harvest=" + URLEncoder.encode(harvestDate, "UTF-8"); // ✅ removed stocking_date


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

                    runOnUiThreadSafe(() -> callback.onSuccess(response.toString()));
                } else {
                    runOnUiThreadSafe(() -> callback.onError("Server returned code: " + responseCode));
                }

            } catch (Exception e) {
                runOnUiThreadSafe(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public static void uploadBlindFeedLog(String pondName,
                                          String feedType,
                                          double quantity,
                                          double cost,
                                          String feedingDate,
                                          String recordAt, // 👈 added this
                                          Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_blind_feeding.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8") +
                        "&feed_type=" + URLEncoder.encode(feedType, "UTF-8") +
                        "&quantity=" + quantity +
                        "&cost=" + cost +
                        "&feeding_date=" + URLEncoder.encode(feedingDate, "UTF-8") +
                        "&recorded_at=" + URLEncoder.encode(recordAt, "UTF-8"); // 👈 added

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

                    runOnUiThreadSafe(() -> callback.onSuccess(response.toString()));
                } else {
                    runOnUiThreadSafe(() -> callback.onError("Server returned code: " + responseCode));
                }

            } catch (Exception e) {
                runOnUiThreadSafe(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public static void fetchPondActivities(String pondName, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_activities.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8");
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

                conn.disconnect();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void fetchPondDateCreated(String pondName, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_date_created.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8");
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
                conn.disconnect();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }



    public static void fetchBlindFeedLogs(String pondName, Callback callback) {
        new Thread(() -> {
            try {
                Log.d("FETCH_FEED_LOGS", "Fetching feed logs for pond=" + pondName);

                URL url = new URL("https://pondmate.alwaysdata.net/get_blind_feed_logs.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); // can use POST or GET; POST is safer
                conn.setDoOutput(true);

                // 🧩 Send the pond_name parameter
                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8");
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

                    Log.d("FETCH_FEED_LOGS", "Response: " + response);
                    callback.onSuccess(response.toString());
                } else {
                    callback.onError("Server returned code: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e("FETCH_FEED_LOGS", "Error: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        }).start();
    }


    public static void uploadSamplingRecord(String pondId, int daysOfCulture, String growthStage, int totalStocks,
                                            double mortalityRate, String feedingOne, String feedingTwo, double abw, double feedingRate,
                                            double survivalRate, double dfr, double dfrFeed, double dailyFeedCost,
                                            String createdAt, String updatedAt, String nextSamplingDate, Callback callback) {

        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_sampling.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        "pond_id=" + URLEncoder.encode(pondId, "UTF-8") +
                                "&days_of_culture=" + daysOfCulture +
                                "&growth_stage=" + URLEncoder.encode(growthStage, "UTF-8") +
                                "&total_stocks=" + totalStocks +
                                "&mortality_rate=" + mortalityRate +
                                "&feeding_one=" + URLEncoder.encode(feedingOne, "UTF-8") +
                                "&feeding_two=" + URLEncoder.encode(feedingTwo, "UTF-8") +
                                "&abw=" + abw +
                                "&feeding_rate=" + feedingRate +
                                "&survival_rate=" + survivalRate +
                                "&dfr=" + dfr +
                                "&dfr_feed=" + dfrFeed +
                                "&daily_feed_cost=" + dailyFeedCost +
                                "&created_at=" + URLEncoder.encode(createdAt, "UTF-8") +
                                "&updated_at=" + URLEncoder.encode(updatedAt, "UTF-8") +
                                "&next_sampling_date=" + URLEncoder.encode(nextSamplingDate, "UTF-8");

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

                    runOnUiThreadSafe(() -> callback.onSuccess(response.toString()));
                } else {
                    runOnUiThreadSafe(() -> callback.onError("Server returned code: " + responseCode));
                }

            } catch (Exception e) {
                runOnUiThreadSafe(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }


    public static void uploadMaintenanceToServer(String pond, String description, double cost, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_maintenance.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                Log.d("UPLOAD_MAINT", "Posting pond=" + pond + ", desc=" + description + ", amount=" + cost);
                String postData = "name=" + URLEncoder.encode(pond, "UTF-8") +
                        "&description=" + URLEncoder.encode(description, "UTF-8") +
                        "&amount=" + cost;

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

    public static void savePondHistoryWithPDF(PondModel pond, String action, File pdfFile, Callback callback) {
        new Thread(() -> {
            String boundary = "----PondBoundary" + System.currentTimeMillis();
            String LINE_FEED = "\r\n";

            try {
                URL url = new URL("https://pondmate.alwaysdata.net/savePondHistory.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                // ✅ Build a safe and unique filename if needed
                String safePondName = (pond.getName() != null && !pond.getName().isEmpty())
                        ? pond.getName().replaceAll("\\s+", "_")
                        : "unknown";
                String formattedDate = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String finalPdfName = "pond_" + safePondName + "_" + action + "_" + formattedDate + ".pdf";
                String pdfPathToSend = "uploads/" + finalPdfName;

                // ✅ If you already have a custom file (like from PDFGenerator), rename it for consistency
                File pdfToUpload;
                if (pdfFile != null && pdfFile.exists()) {
                    File renamed = new File(pdfFile.getParent(), finalPdfName);
                    if (pdfFile.renameTo(renamed)) {
                        pdfToUpload = renamed;
                    } else {
                        pdfToUpload = pdfFile;
                    }
                } else {
                    pdfToUpload = pdfFile;
                }

                // ✅ Write form data once (no duplicates)
                writeFormField(request, "pond_id", pond.getId(), boundary);
                writeFormField(request, "name", pond.getName(), boundary);
                writeFormField(request, "action", action != null ? action : "INACTIVE", boundary);
                writeFormField(request, "pdf_path", pdfPathToSend, boundary);

                // ✅ Upload PDF file if available
                if (pdfToUpload != null && pdfToUpload.exists()) {
                    writeFileField(request, "pdf_file", pdfToUpload, boundary);
                }

                request.writeBytes("--" + boundary + "--" + LINE_FEED);
                request.flush();
                request.close();

                int responseCode = conn.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode == 200) {
                    pond.setPdfPath(pdfPathToSend); // ✅ sync final path
                    callback.onSuccess(response.toString());
                } else {
                    callback.onError("HTTP Error: " + responseCode + " → " + response);
                }

                conn.disconnect();

            } catch (Exception e) {
                callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    public static void updatePondToServer(Context context, PondModel pond, String serverUrl, Callback callback) {
        new Thread(() -> {
            try {
                // ✅ Change this to your actual PHP endpoint
                String url = serverUrl.isEmpty()
                        ? "https://pondmate.alwaysdata.net/update_pond.php"
                        : serverUrl;

                URL endpoint = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Prepare POST data
                String postData = "name=" + URLEncoder.encode(pond.getName(), "UTF-8") +
                        "&species=" + URLEncoder.encode(pond.getBreed(), "UTF-8") +
                        "&fish_count=" + pond.getFishCount() +
                        "&cost_per_fish=" + pond.getCostPerFish() +
                        "&pond_area=" + pond.getPondArea() +
                        "&mortality_rate=" + pond.getMortalityRate() +
                        "&date_created=" + URLEncoder.encode(pond.getDateStarted(), "UTF-8") +
                        "&date_stocked=" + URLEncoder.encode(pond.getDateStocking(), "UTF-8") +
                        "&estimated_harvest=" + URLEncoder.encode(pond.getDateHarvest(), "UTF-8") +
                        "&assigned_caretakers=" + URLEncoder.encode(pond.getCaretakers(), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        if (responseCode == 200) {
                            callback.onSuccess(response.toString());
                        } else {
                            callback.onError("Server error: " + response);
                        }
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }


    public static void updatePondDetails(PondModel pond, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/updatePond.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData =
                        "pond_id=" + URLEncoder.encode(pond.getId(), "UTF-8") +
                                "&name=" + URLEncoder.encode(pond.getName(), "UTF-8") +
                                "&breed=" + URLEncoder.encode(pond.getBreed(), "UTF-8") +
                                "&fish_count=" + pond.getFishCount() +
                                "&cost_per_fish=" + pond.getCostPerFish() +
                                "&date_started=" + URLEncoder.encode(pond.getDateStarted(), "UTF-8") +
                                "&date_harvest=" + URLEncoder.encode(pond.getDateHarvest(), "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300
                                ? conn.getInputStream()
                                : conn.getErrorStream()
                ));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    if (callback != null) callback.onSuccess(response.toString());
                } else {
                    if (callback != null) callback.onError("Error: " + responseCode + " → " + response);
                }

                conn.disconnect();
            } catch (Exception e) {
                if (callback != null) callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    public static void fetchFarmgatePrice(String breed, Callback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/fetchFarmgatePrices.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // Build JSON body
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("breed", breed);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.toString().getBytes("UTF-8"));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("ROI_DEBUG", "Error fetching farmgate price: HTTP " + responseCode);
                    callback.onError("HTTP " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                Log.d("ROI_DEBUG", "Farmgate API Response: " + response);

                callback.onSuccess(response.toString());

            } catch (Exception e) {
                Log.e("ROI_DEBUG", "Exception fetching farmgate price", e);
                callback.onError(e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }



    public static void fetchPondReportData(String pondName, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_report.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                String data = "name=" + URLEncoder.encode(pondName, "UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                conn.disconnect();

                callback.onSuccess(result.toString());
            } catch (Exception e) {
                callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    public static void setPondInactive(PondModel pond, File pdfFile, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/setPondInactive.php");
                String boundary = "----PondBoundary" + System.currentTimeMillis();
                String LINE_FEED = "\r\n";

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                // --- Form fields ---
                writeFormField(request, "pond_id", pond.getId(), boundary);
                writeFormField(request, "name", pond.getName(), boundary);

                String pdfPath = pdfFile != null && pdfFile.exists()
                        ? "uploads/" + pdfFile.getName()
                        : (pond.getPdfPath() != null ? pond.getPdfPath() : "");

                writeFormField(request, "pdf_path", pdfPath, boundary);

                // --- File upload ---
                if (pdfFile != null && pdfFile.exists()) {
                    writeFileField(request, "pdf_file", pdfFile, boundary);
                }

                // --- End boundary ---
                request.writeBytes("--" + boundary + "--" + LINE_FEED);
                request.flush();
                request.close();

                // --- Read server response ---
                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                String resp = response.toString().trim();
                Log.d("SetInactivePond", "Server response: " + resp);

                if (resp.startsWith("\uFEFF")) {
                    resp = resp.substring(1);
                }

                JSONObject json;
                try {
                    json = new JSONObject(resp);
                } catch (JSONException ex) {
                    // Try to recover if PHP adds text before JSON
                    int start = resp.indexOf('{');
                    int end = resp.lastIndexOf('}');
                    if (start >= 0 && end >= 0) {
                        String cleanJson = resp.substring(start, end + 1);
                        json = new JSONObject(cleanJson);
                    } else {
                        throw ex;
                    }
                }

                boolean success = json.optBoolean("success", false);
                String message = json.optString("message", "Unknown response");

                if (success) {
                    runOnUiThreadSafe(() -> callback.onSuccess(message));
                } else {
                    runOnUiThreadSafe(() -> callback.onError("Error: " + message));
                }

                conn.disconnect();
            } catch (Exception e) {
                runOnUiThreadSafe(() ->
                        callback.onError("Exception: " + e.getMessage())
                );
            }
        }).start();
    }

    public static void fetchPondReport(String pondId, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_report.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_id=" + URLEncoder.encode(pondId, "UTF-8");
                conn.getOutputStream().write(postData.getBytes());
                conn.getOutputStream().flush();
                conn.getOutputStream().close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    runOnUiThreadSafe(() -> callback.onSuccess(jsonResponse));
                } else {
                    runOnUiThreadSafe(() -> callback.onError("Server returned: " + responseCode + " → " + response));
                }

            } catch (Exception e) {
                runOnUiThreadSafe(() -> callback.onError("Error: " + e.getMessage()));
            }
        }).start();
    }

    public static void fetchTotalPonds(Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_total_pond.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject obj = new JSONObject(response.toString());
                String status = obj.optString("status");

                if ("success".equalsIgnoreCase(status)) {
                    int pondCount = obj.optInt("count", 0);
                    runOnUiThreadSafe(() -> callback.onSuccess(pondCount));
                } else if ("empty".equalsIgnoreCase(status)) {
                    runOnUiThreadSafe(() -> callback.onSuccess(0));
                } else {
                    String message = obj.optString("message", "Unknown error");
                    runOnUiThreadSafe(() -> callback.onError(message));
                }

            } catch (Exception e) {
                runOnUiThreadSafe(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }


    public static void uploadFeedingScheduleToServer(String pondName,
                                                     String schedOne,
                                                     String schedTwo,
                                                     String schedThree,
                                                     double feedAmount,
                                                     float fishWeight,
                                                     float feedPrice,
                                                     Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/save_feeding_schedule.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                Log.d("UPLOAD_FEED", "Posting pond=" + pondName +
                        ", sched1=" + schedOne +
                        ", sched2=" + schedTwo +
                        ", sched3=" + schedThree +
                        ", amount=" + feedAmount +
                        ", fish_weight=" + fishWeight +
                        ", feed_price=" + feedPrice);

                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8") +
                        "&sched_one=" + URLEncoder.encode(schedOne, "UTF-8") +
                        "&sched_two=" + URLEncoder.encode(schedTwo != null ? schedTwo : "", "UTF-8") +
                        "&sched_three=" + URLEncoder.encode(schedThree != null ? schedThree : "", "UTF-8") +
                        "&feed_amount=" + feedAmount +
                        "&fish_weight=" + fishWeight +
                        "&feed_price=" + feedPrice;

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

    public static void fetchPondSchedule(String pondName, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_schedule.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "name=" + URLEncoder.encode(pondName, "UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                callback.onSuccess(result.toString());

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void fetchLatestSamplingRecord(String pondId, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL("https://pondmate.alwaysdata.net/fetchLatestSamplingRecord.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                // ✅ Send pond_id (NOT pond_name)
                String postData = "pond_id=" + URLEncoder.encode(pondId, "UTF-8");
                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    callback.onSuccess(response.toString());
                } else {
                    callback.onError("Server returned code: " + responseCode);
                }

            } catch (Exception e) {
                callback.onError(e.getMessage());
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (IOException ignored) {}
                }
                if (connection != null) connection.disconnect();
            }
        }).start();
    }



    public interface OnDataSyncListener {
        void onSuccess(String response);
        void onError(String error);
    }
    public static void deleteBlindFeedingLog(String logId, String pondName, OnDataSyncListener listener) {
        new Thread(() -> {
            try {
                // ✅ Replace with your actual full PHP URL
                URL url = new URL("https://pondmate.alwaysdata.net/delete_blind_feed_logs.php");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                // ✅ Prepare POST data
                String postData = "log_id=" + URLEncoder.encode(logId, "UTF-8") +
                        "&pond_name=" + URLEncoder.encode(pondName, "UTF-8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();
                os.close();

                // ✅ Read response
                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode == HttpURLConnection.HTTP_OK ? conn.getInputStream() : conn.getErrorStream()
                ));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.e("DELETE_FEED_LOG", "Response: " + response);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    listener.onSuccess(response.toString());
                } else {
                    listener.onError("Server returned: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("DELETE_FEED_LOG", "Error deleting log", e);
                listener.onError(e.getMessage());
            }
        }).start();
    }


    public static void updateBlindFeedingLog(
            String pondName,
            String feedType,
            double quantity,
            double cost,
            String feedingDate,
            String feedLogId,
            OnDataSyncListener listener
    ) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_blind_feeding_logs.php");

                JSONObject postData = new JSONObject();
                postData.put("pond_name", pondName);
                postData.put("feed_type", feedType);
                postData.put("quantity", quantity);
                postData.put("cost", cost);
                postData.put("feeding_date", feedingDate);
                postData.put("feed_log_id", feedLogId);

                Log.d("PondSyncManager", "Updating feed log with data: " + postData.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(postData.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                Log.d("PondSyncManager", "Server response (" + responseCode + "): " + response);

                if (responseCode == HttpURLConnection.HTTP_OK)
                    listener.onSuccess(response.toString());
                else
                    listener.onError("HTTP " + responseCode + ": " + response.toString());

            } catch (Exception e) {
                Log.e("PondSyncManager", "Error updating log", e);
                listener.onError(e.getMessage());
            }
        }).start();
    }






    public interface Callback {
        void onSuccess(Object result);
        void onError(String error);
    }

    private static void runOnUiThreadSafe(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }


    public static void fetchWeeklyScheduleByName(String pondName, Callback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/fetch_weekly_schedule.php");
                    conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // Create JSON body
                // Create JSON body
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("name", pondName);

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.close();

                // Read response
                int responseCode = conn.getResponseCode();
                InputStream is = responseCode == 200 ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String response = sb.toString();
                if (callback != null) callback.onSuccess((Object) response);

            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) callback.onError(e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start(); }

    private static void writeFormField(DataOutputStream request, String name, String value, String boundary) throws IOException {
        String LINE_FEED = "\r\n";
        request.writeBytes("--" + boundary + LINE_FEED);
        request.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED);
        request.writeBytes(LINE_FEED);
        request.writeBytes(value + LINE_FEED);
    }

    private static void writeFileField(DataOutputStream request, String fieldName, File file, String boundary) throws IOException {
        String LINE_FEED = "\r\n";
        request.writeBytes("--" + boundary + LINE_FEED);
        request.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"" + LINE_FEED);
        request.writeBytes("Content-Type: application/pdf" + LINE_FEED);
        request.writeBytes(LINE_FEED);

        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            request.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        request.writeBytes(LINE_FEED);
    }

    public static void fetchFeeds(Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_feeds.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write("".getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                callback.onSuccess(result.toString());

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // 🔹 Update feed price
    public static void updateFeedPrice(int feedId, double newPrice, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_feeds.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Prepare POST data
                String postData = "id=" + URLEncoder.encode(String.valueOf(feedId), "UTF-8") +
                        "&price_per_kg=" + URLEncoder.encode(String.valueOf(newPrice), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                callback.onSuccess(result.toString());

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void fetchFarmgatePrices(Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_farmgate.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write("".getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                callback.onSuccess(result.toString());

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void updateFarmgatePrice(int priceId, double newPrice, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/update_farmgate.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "id=" + URLEncoder.encode(String.valueOf(priceId), "UTF-8") +
                        "&price_per_kg=" + URLEncoder.encode(String.valueOf(newPrice), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                callback.onSuccess(result.toString());

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }


    public void createWeeklySchedule(String pondName,
                                     String time1, String time2, String time3,
                                     String feedAmount, String fishWeight,
                                     String feedPrice, String feederType,
                                     Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/save_schedule.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Data na ipapasa
                String data = "pond_name=" + URLEncoder.encode(pondName, "UTF-8")
                        + "&time1=" + URLEncoder.encode(time1, "UTF-8")
                        + "&time2=" + URLEncoder.encode(time2, "UTF-8")
                        + "&time3=" + URLEncoder.encode(time3, "UTF-8")
                        + "&feed_amount=" + URLEncoder.encode(feedAmount, "UTF-8")
                        + "&fish_weight=" + URLEncoder.encode(fishWeight, "UTF-8")
                        + "&feed_price=" + URLEncoder.encode(feedPrice, "UTF-8")
                        + "&feeder_type=" + URLEncoder.encode(feederType, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                if (callback != null) {
                    callback.onSuccess(response.toString());

                }

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }



    // ✅ Add Blind Feed Log
    public static void addBlindFeedLog(String pondName, String feedType, double quantity, double cost, String feedingDate, String recordedAt, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_blind_feed_log.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        "pond_name=" + URLEncoder.encode(pondName, "UTF-8") +
                                "&feed_type=" + URLEncoder.encode(feedType, "UTF-8") +
                                "&quantity=" + URLEncoder.encode(String.valueOf(quantity), "UTF-8") +
                                "&cost=" + URLEncoder.encode(String.valueOf(cost), "UTF-8") +
                                "&feeding_date=" + URLEncoder.encode(feedingDate, "UTF-8") +
                                "&recorded_at=" + URLEncoder.encode(recordedAt, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                callback.onSuccess(result.toString());
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }




    public static void fetchNotifications(Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_notifications.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                conn.disconnect();

                if (callback != null) callback.onSuccess(response.toString());
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        }).start();
    }




    // ✅ Update Blind Feed Log



    // ✅ Delete Blind Feed Log
//    public static void deleteBlindFeedLog(int id, Callback callback) {
//        new Thread(() -> {
//            try {
//                URL url = new URL("https://pondmate.alwaysdata.net/delete_blind_feed_log.php");
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("POST");
//                conn.setDoOutput(true);
//
//                String postData = "id=" + URLEncoder.encode(String.valueOf(id), "UTF-8");
//
//                OutputStream os = conn.getOutputStream();
//                os.write(postData.getBytes());
//                os.flush();
//                os.close();
//
//                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                StringBuilder result = new StringBuilder();
//                String line;
//
//                while ((line = reader.readLine()) != null) {
//                    result.append(line);
//                }
//
//                reader.close();
//                conn.disconnect();
//
//                callback.onSuccess(result.toString());
//            } catch (Exception e) {
//                callback.onError(e.getMessage());
//            }
//        }).start();
//    }



}
