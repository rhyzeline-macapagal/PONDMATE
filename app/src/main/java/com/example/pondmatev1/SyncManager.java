package com.example.pondmatev1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SyncManager {

    public static void syncUsersToServer(Context context, DatabaseHelper dbHelper) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users", null);

        if (cursor.getCount() == 0) {
            Toast.makeText(context, "No users to sync.", Toast.LENGTH_SHORT).show();
            cursor.close();
            return;
        }

        int totalUsers = cursor.getCount();
        final int[] syncedCount = {0};
        final int[] failedCount = {0};

        while (cursor.moveToNext()) {
            String id = String.valueOf(cursor.getInt(0));
            String username = cursor.getString(1);
            String password = cursor.getString(2);
            String fullname = cursor.getString(3);
            String address = cursor.getString(4);
            String usertype = cursor.getString(5);

            new Thread(() -> {
                try {
                    URL url = new URL("https://pondmate.alwaysdata.net/post_user.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    String postData = "id=" + URLEncoder.encode(id, "UTF-8") +
                            "&username=" + URLEncoder.encode(username, "UTF-8") +
                            "&password=" + URLEncoder.encode(password, "UTF-8") +
                            "&fullname=" + URLEncoder.encode(fullname, "UTF-8") +
                            "&address=" + URLEncoder.encode(address, "UTF-8") +
                            "&usertype=" + URLEncoder.encode(usertype, "UTF-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(postData.getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        synchronized (syncedCount) {
                            syncedCount[0]++;
                            checkIfAllDone(context, totalUsers, syncedCount[0], failedCount[0]);
                        }
                    } else {
                        Log.e("SyncError", "Failed for user: " + username + ", Code: " + responseCode);
                        synchronized (failedCount) {
                            failedCount[0]++;
                            checkIfAllDone(context, totalUsers, syncedCount[0], failedCount[0]);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    synchronized (failedCount) {
                        failedCount[0]++;
                        checkIfAllDone(context, totalUsers, syncedCount[0], failedCount[0]);
                    }
                }
            }).start();
        }

        cursor.close();
    }

    // 🔧 This helper method runs only once, when all users have been processed
    private static void checkIfAllDone(Context context, int total, int synced, int failed) {
        if (synced + failed == total) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "🔄 Sync finished: " + synced + " synced, " + failed + " failed.", Toast.LENGTH_SHORT).show();
            });
        }
    }

}
