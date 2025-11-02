
package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.nafis.bottomnavigation.NafisBottomNavigation;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    NafisBottomNavigation bottomNavigation;
    private TextView pondNameLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);



        pondNameLabel = findViewById(R.id.pondNameLabel);
        pondNameLabel.setVisibility(View.GONE); // Hide by default

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.getUsername() == null) {
            Intent intent = new Intent(this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        createNotificationChannel();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton backBtn = findViewById(R.id.backToDashboardBtn);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PondDashboardActivity.class);
            // optional: if you don’t want to stack multiple dashboard screens
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // close the current activity so it doesn’t stay in back stack
        });


        ImageView adminIcon = findViewById(R.id.adminIcon);
        String usertype = sessionManager.getUsertype();
        if (usertype != null && !usertype.equalsIgnoreCase("owner")) {
            adminIcon.setVisibility(View.GONE);
        }

        ImageView profileIcon = findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(v -> {
            Log.d("DEBUG", "Profile icon clicked");
            UserProfileDialogFragment dialog = new UserProfileDialogFragment();
            dialog.show(getSupportFragmentManager(), "UserProfileDialog");
        });

        ImageView notificationIcon = findViewById(R.id.notificationIcon);
        notificationIcon.bringToFront();

        notificationIcon.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("POND_PREF", MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);

            Intent intent = new Intent(this, NotificationActivity.class);

            if (pondJson != null) {
                intent.putExtra("pond_json", pondJson);
            } else {
                intent.putExtra("pond_json", ""); // or send a flag like "NO_POND"
            }

            startActivity(intent);
        });

        adminIcon.setOnClickListener(v -> {
            // Inflate the custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_options, null);

            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this, R.style.TransparentDialog) // optional style
                    .setView(dialogView)
                    .create();

            // Close button (the ✖ on top-right)
            TextView btnClose = dialogView.findViewById(R.id.btnClose);
            btnClose.setOnClickListener(v1 -> dialog.dismiss());

            // Caretaker Dashboard button
            ImageButton btnCaretaker = dialogView.findViewById(R.id.btnCaretaker);
            btnCaretaker.setOnClickListener(v1 -> {
                startActivity(new Intent(MainActivity.this, CaretakerDashboardActivity.class));
                overridePendingTransition(R.anim.drop_in, R.anim.fade_out);
                dialog.dismiss();
            });

            // Feeds Prices button
            ImageButton btnFeedsPrice = dialogView.findViewById(R.id.btnFeedsPrice);
            btnFeedsPrice.setOnClickListener(v1 -> {
                startActivity(new Intent(MainActivity.this, FeedsPriceActivity.class));
                dialog.dismiss();
            });

            ImageButton btnFarmgatePrice = dialogView.findViewById(R.id.btnFarmgatePrice);
            btnFarmgatePrice.setOnClickListener(v1 -> {
                startActivity(new Intent(MainActivity.this, FarmgatePriceActivity.class));
                dialog.dismiss();
            });

            // Show dialog
            dialog.show();
        });



        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.add(new NafisBottomNavigation.Model(1, R.drawable.home1));
        bottomNavigation.add(new NafisBottomNavigation.Model(2, R.drawable.productioncost3));
        bottomNavigation.add(new NafisBottomNavigation.Model(3, R.drawable.feeder4));
        bottomNavigation.add(new NafisBottomNavigation.Model(4, R.drawable.activity5));

        bottomNavigation.show(1, true);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.con, new PondInfoFragment())
                .addToBackStack(null)
                .commit();

        bottomNavigation.setOnClickMenuListener(model -> {
            switch (model.getId()) {
                case 1:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, new PondInfoFragment())
                            .addToBackStack(null)
                            .commit();
                    break;

                case 2:

                    ProductionCostFragment fragment = new ProductionCostFragment();

                    Bundle args = new Bundle();
                    args.putString("pond_id", getIntent().getStringExtra("pond_id"));  // ✅ pass pond_id
                    args.putString("pond_name", pondNameLabel.getText().toString());

                    fragment.setArguments(args);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, fragment)
                            .addToBackStack(null)
                            .commit();
                    break;

                case 3:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, new FeederFragment())
                            .addToBackStack(null)
                            .commit();
                    break;

                case 4:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, new ActivitiesFragment())
                            .addToBackStack(null)
                            .commit();
                    break;
            }
            return null;
        });

        if (getIntent() != null && getIntent().hasExtra("pond_id")) {
            PondModel pond = new PondModel(
                                getIntent().getStringExtra("pond_id"),
                                getIntent().getStringExtra("pond_name"),
                                getIntent().getStringExtra("breed"),
                                getIntent().getIntExtra("fish_count", 0),
                                getIntent().getDoubleExtra("cost_per_fish", 0.0),
                                getIntent().getStringExtra("date_started"),
                                getIntent().getStringExtra("date_harvest"),
                                getIntent().getStringExtra("date_stocking"),
                                getIntent().getDoubleExtra("pond_area", 0.0),
                                getIntent().getStringExtra("image_path"),
                                getIntent().getStringExtra("caretaker_name"), //
                                0f, // actual ROI
                                0f, // estimated ROI
                    getIntent().getStringExtra("pdf_path"), // mortality rate
                    getIntent().getDoubleExtra("mortality_rate", 0.0)
                        );
            pond.setCaretakerName(getIntent().getStringExtra("caretaker_name"));
            openPondInfoFragment(pond);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "pondmate_channel",
                    "PondMate Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for feeding, sampling, and pond activities");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }


    public void openPondInfoFragment(PondModel pond) {
        // Save pond as JSON in SharedPreferences
        SharedPreferences prefs = getSharedPreferences("POND_PREF", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String pondJson = new Gson().toJson(pond);
        editor.putString("selected_pond", pondJson);
        editor.apply();
        TextView pondNameLabel = findViewById(R.id.pondNameLabel);

        if (pondNameLabel != null) {
            pondNameLabel.setText(pond.getName()); // or pond.getName() depending on your model
            pondNameLabel.setVisibility(View.VISIBLE);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.con, new PondInfoFragment())
                .addToBackStack(null)
                .commit();
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