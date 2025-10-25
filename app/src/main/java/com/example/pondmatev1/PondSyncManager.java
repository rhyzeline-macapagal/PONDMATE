package com.example.pondmatev1;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

                // Prepare POST data (tailored to your new pond fields)
                String postData = "name=" + URLEncoder.encode(pond.getName(), "UTF-8") +
                        "&area=" + URLEncoder.encode(String.valueOf(pond.getPondArea()), "UTF-8") +
                        "&date_created=" + URLEncoder.encode(pond.getDateStarted(), "UTF-8") +
                        "&stocking_date=" + URLEncoder.encode(pond.getDateStocking(), "UTF-8") +
                        "&image=" + URLEncoder.encode(imageBase64, "UTF-8");

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                // Read server response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                // Parse JSON response
                String response = sb.toString();
                JSONObject json = new JSONObject(response);
                String status = json.optString("status", "error");
                String message = json.optString("message", "Unknown error");

                // Callback to main thread
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

    public static void fetchFeederTypeByName(String pondName, Callback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_feeder_type.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // 🔹 Send pond_name as form-data
                String postData = "pond_name=" + URLEncoder.encode(pondName, "UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes("UTF-8"));
                os.flush();
                os.close();

                // 🔹 Read response
                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String response = sb.toString();
                Log.d("Feedertype", "Code: " + responseCode + " | Body: " + response);

                if (callback != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equalsIgnoreCase(jsonResponse.optString("status"))) {
                            JSONObject pondObj = jsonResponse.getJSONObject("pond");

                            // Extract fields
                            String feederType = pondObj.optString("feeder_type", "N/A");
                            int pondAgeDays = pondObj.optInt("pond_age_days", 0);

                            JSONObject resultObj = new JSONObject();
                            resultObj.put("feeder_type", feederType);
                            resultObj.put("pond_age_days", pondAgeDays);

                            callback.onSuccess(resultObj);
                        } else {
                            callback.onError("Server error: " + jsonResponse.optString("message"));
                        }
                    } catch (Exception parseEx) {
                        callback.onError("Parse error: " + parseEx.getMessage() + "\nResponse: " + response);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) callback.onError("Request error: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
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



}
