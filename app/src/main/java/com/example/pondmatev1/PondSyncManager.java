package com.example.pondmatev1;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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




    public static void uploadMaintenanceToServer(String pond, String description, double cost, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/add_maintenance.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Send pond_name + maintenance details
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

    public static void fetchProductionReportByName(String pondName, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_pond_report.php");
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

    public static void savePondHistoryWithPDF(PondModel pond, String action, File pdfFile, Callback callback) {
        new Thread(() -> {
            String boundary = "----PondBoundary" + System.currentTimeMillis();
            String LINE_FEED = "\r\n";

            try {
                URL url = new URL("https://yourserver.com/savePondHistory.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                // --- Add text fields ---
                writeFormField(request, "pond_id", pond.getId(), boundary);
                writeFormField(request, "name", pond.getName(), boundary);
                writeFormField(request, "action", action, boundary);

                // ✅ Send existing path if no file is provided
                if (pdfFile == null) {
                    writeFormField(request, "pdf_path", pond.getPdfPath() != null ? pond.getPdfPath() : "", boundary);
                }

                // --- Add file if exists ---
                if (pdfFile != null && pdfFile.exists()) {
                    writeFileField(request, "pdf_file", pdfFile, boundary);
                }

                // End multipart
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

    // --- Helper methods ---
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


    public static void uploadFeedingScheduleToServer(String pondName,
                                                     String schedOne,
                                                     String schedTwo,
                                                     String schedThree,
                                                     double feedAmount,
                                                     Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/save_feeding_schedule.php"); // your PHP file
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Debug log
                Log.d("UPLOAD_FEED", "Posting pond=" + pondName +
                        ", sched1=" + schedOne +
                        ", sched2=" + schedTwo +
                        ", sched3=" + schedThree +
                        ", amount=" + feedAmount);

                // Build POST data
                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8") +
                        "&sched_one=" + URLEncoder.encode(schedOne, "UTF-8") +
                        "&sched_two=" + URLEncoder.encode(schedTwo != null ? schedTwo : "", "UTF-8") +
                        "&sched_three=" + URLEncoder.encode(schedThree != null ? schedThree : "", "UTF-8") +
                        "&feed_amount=" + feedAmount;

                // Send to server
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                // Handle response
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


    // Callback interface for async results
    public interface Callback {
        void onSuccess(Object result);
        void onError(String error);
    }

    public static void updatePondDetails(PondModel pond, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://yourserver.com/updatePond.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "pond_id=" + URLEncoder.encode(pond.getId(), "UTF-8") +
                        "&name=" + URLEncoder.encode(pond.getName(), "UTF-8") +
                        "&breed=" + URLEncoder.encode(pond.getBreed(), "UTF-8") +
                        "&fish_count=" + pond.getFishCount() +
                        "&cost_per_fish=" + pond.getCostPerFish() +
                        "&date_started=" + URLEncoder.encode(pond.getDateStarted(), "UTF-8") +
                        "&date_harvest=" + URLEncoder.encode(pond.getDateHarvest(), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);

                br.close();
                conn.disconnect();

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(response.toString()));
                }
            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }
}
