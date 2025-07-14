package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nafis.bottomnavigation.NafisBottomNavigation;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {

    NafisBottomNavigation bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        // 🔙 Back button to return to Pond Dashboard
        ImageButton backBtn = findViewById(R.id.backToDashboardBtn);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PondDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 👤 Profile Icon visibility for owner only
        ImageView profileIcon = findViewById(R.id.profileIcon);
        String usertype = sessionManager.getUsertype();
        if (usertype != null && !usertype.equalsIgnoreCase("owner")) {
            profileIcon.setVisibility(View.GONE);
        }

        // ⬇️ Bottom Navigation Setup (Only 4 buttons now)
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.add(new NafisBottomNavigation.Model(1, R.drawable.home1));             // Home
        bottomNavigation.add(new NafisBottomNavigation.Model(2, R.drawable.productioncost3));    // Cost
        bottomNavigation.add(new NafisBottomNavigation.Model(3, R.drawable.feeder4));            // Feeder
        bottomNavigation.add(new NafisBottomNavigation.Model(4, R.drawable.activity5));          // Activity

        bottomNavigation.show(1, true);

        // 📌 Default: show Pond Info Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.con, new PondInfoFragment())
                .commit();

        // 🔁 Handle Nav Clicks
        bottomNavigation.setOnClickMenuListener(model -> {
            switch (model.getId()) {
                case 1:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, new PondInfoFragment())
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

        // 🎯 Open specific pond if passed from intent
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
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.con, PondInfoFragment.newInstance(pond))
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
