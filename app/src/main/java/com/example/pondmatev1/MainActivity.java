package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nafis.bottomnavigation.NafisBottomNavigation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {


    NafisBottomNavigation bottomNavigation;
    DatabaseHelper dbHelper;
    AtomicBoolean hasNewUsers = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.getUsername() == null) {
            // No user logged in, redirect to login
            Intent intent = new Intent(this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView profileIcon = findViewById(R.id.profileIcon);
        SessionManager session = new SessionManager(this);
        String usertype = session.getUsertype();

        if (usertype != null && !usertype.equalsIgnoreCase("owner")) {
            profileIcon.setVisibility(View.GONE);
        }

        bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.add(new NafisBottomNavigation.Model(1, R.drawable.home1));
        bottomNavigation.add(new NafisBottomNavigation.Model(2, R.drawable.select2));
        bottomNavigation.add(new NafisBottomNavigation.Model(3, R.drawable.productioncost3));
        bottomNavigation.add(new NafisBottomNavigation.Model(4, R.drawable.feeder4));
        bottomNavigation.add(new NafisBottomNavigation.Model(5, R.drawable.activity5));




        bottomNavigation.setOnClickMenuListener(new Function1<NafisBottomNavigation.Model, Unit>() {
            @Override
            public Unit invoke(NafisBottomNavigation.Model model) {
                switch (model.getId()) {
                    case 1:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.con, new pcostroi())
                                .commit();
                        break;
                    case 2:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.con, new pcostroi())
                                .commit();
                        break;
                    case 3:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.con, new pcostroi())
                                .commit();
                        break;
                    case 4:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.con, new pcostroi())
                                .commit();
                        break;
                    case 5:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.con, new pcostroi())
                                .commit();
                        break;
                }
                return null;
            }
        });

        dbHelper = new DatabaseHelper(this);

        //sync only if internet is availavle
        if (isInternetAvailable()) {
            SyncManager.syncUsersToServer(this, dbHelper);
            syncUsersFromServer();  // ⬅️ This function will download all users and save to SQLite
        } else {
            Toast.makeText(this, "No internet connection. Sync skipped.", Toast.LENGTH_SHORT).show();
        }



    }
    private void syncUsersFromServer() {
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/get_users.php"); // your API URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }

                reader.close();
                conn.disconnect();

                String jsonResponse = jsonBuilder.toString();
                JSONArray usersArray = new JSONArray(jsonResponse);

                AtomicBoolean hasNewUsers = new AtomicBoolean(false);

                for (int i = 0; i < usersArray.length(); i++) {
                    JSONObject user = usersArray.getJSONObject(i);

                    String username = user.getString("username");
                    String password = user.getString("password");
                    String fullname = user.getString("fullname");
                    String address = user.getString("address");
                    String usertype = user.getString("usertype");

                    if (!dbHelper.isUserExistsByUsername(username)) {
                        dbHelper.addUser(username, password, fullname, address, usertype);
                        hasNewUsers.set(true);  // ✅ update safely
                    }
                }

                runOnUiThread(() -> {
                    if (hasNewUsers.get()) {
                        Toast.makeText(MainActivity.this, "✅ Users synced from server", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "✔️ No new users to sync", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "⚠️ Failed to sync users", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }



    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }
}



