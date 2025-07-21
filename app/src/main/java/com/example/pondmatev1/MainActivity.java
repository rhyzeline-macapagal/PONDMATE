package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.Gson;

import com.nafis.bottomnavigation.NafisBottomNavigation;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {

    NafisBottomNavigation bottomNavigation;
    private TextView pondNameLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        pondNameLabel = findViewById(R.id.pondNameLabel);
        pondNameLabel.setVisibility(View.GONE); // Hide it by default


        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.getUsername() == null) {
            Intent intent = new Intent(this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        ImageButton backBtn = findViewById(R.id.backToDashboardBtn);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PondDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);

            overridePendingTransition(R.anim.fade_in, R.anim.fade_out); // Only exit anim
            finish();
        });



        ImageView adminIcon = findViewById(R.id.adminIcon);
        String usertype = sessionManager.getUsertype();
        if (usertype != null && !usertype.equalsIgnoreCase("owner")) {
            adminIcon.setVisibility(View.GONE);
        }

        ImageView profileIcon = findViewById(R.id.profileIcon);

        profileIcon.setOnClickListener(v -> {
            bottomNavigation.setVisibility(View.GONE);
            backBtn.setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.con, new UserProfile())
                    .commit();
        });


        adminIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CaretakerDashboardActivity.class);
            startActivity(intent);
        });



        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.add(new NafisBottomNavigation.Model(1, R.drawable.home1));             // Home
        bottomNavigation.add(new NafisBottomNavigation.Model(2, R.drawable.productioncost3));    // Cost
        bottomNavigation.add(new NafisBottomNavigation.Model(3, R.drawable.feeder4));            // Feeder
        bottomNavigation.add(new NafisBottomNavigation.Model(4, R.drawable.activity5));          // Activity

        bottomNavigation.show(1, true);


        getSupportFragmentManager().beginTransaction()
                .replace(R.id.con, new PondInfoFragment())
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
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, new ProductionCostFragment())
                            .commit();
                    break;
                case 3:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, new FeederFragment())
                            .commit();
                    break;
                case 4:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, new ActivityFragment())
                            .commit();
                    break;
            }
            return null;
        });


        if (getIntent() != null && getIntent().hasExtra("pond_name")) {
            PondModel pond = new PondModel(
                    getIntent().getStringExtra("pond_name"),
                    getIntent().getStringExtra("breed"),
                    getIntent().getIntExtra("fish_count", 0),
                    getIntent().getDoubleExtra("cost_per_fish", 0.0),
                    getIntent().getStringExtra("date_started"),
                    getIntent().getStringExtra("date_harvest"),
                    "DATA"
            );
            openPondInfoFragment(pond);

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
