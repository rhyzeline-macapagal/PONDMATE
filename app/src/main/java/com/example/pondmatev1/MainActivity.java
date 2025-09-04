package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
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

import com.google.gson.Gson;
import com.nafis.bottomnavigation.NafisBottomNavigation;

public class MainActivity extends AppCompatActivity {

    NafisBottomNavigation bottomNavigation;
    private TextView pondNameLabel;
    private PondModel selectedPond; // ✅ store pond in memory instead of SharedPreferences

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
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
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

        adminIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CaretakerDashboardActivity.class);
            startActivity(intent);
        });

        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.add(new NafisBottomNavigation.Model(1, R.drawable.home1));
        bottomNavigation.add(new NafisBottomNavigation.Model(2, R.drawable.productioncost3));
        bottomNavigation.add(new NafisBottomNavigation.Model(3, R.drawable.feeder4));
        bottomNavigation.add(new NafisBottomNavigation.Model(4, R.drawable.activity5));

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
                    if (selectedPond == null) {
                        Toast.makeText(this, "Please select a pond first", Toast.LENGTH_SHORT).show();
                        return null;
                    }
                    ProductionCostFragment fragment = new ProductionCostFragment();
                    Bundle args = new Bundle();
                    args.putString("pond_id", selectedPond.getId());
                    args.putString("pond_name", selectedPond.getName());
                    fragment.setArguments(args);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.con, fragment)
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

        // ✅ If pond was passed via Intent
        if (getIntent() != null && getIntent().hasExtra("pond_id")) {
            PondModel pond = new PondModel(
                    getIntent().getStringExtra("pond_id"),
                    getIntent().getStringExtra("pond_name"),
                    getIntent().getStringExtra("breed"),
                    getIntent().getIntExtra("fish_count", 0),
                    getIntent().getDoubleExtra("cost_per_fish", 0.0),
                    getIntent().getStringExtra("date_started"),
                    getIntent().getStringExtra("date_harvest"),
                    null,
                    0f,
                    0f
            );
            openPondInfoFragment(pond);
        }
    }

    public void openPondInfoFragment(PondModel pond) {
        // ✅ keep pond in memory
        selectedPond = pond;

        if (pondNameLabel != null) {
            pondNameLabel.setText(pond.getName());
            pondNameLabel.setVisibility(View.VISIBLE);
        }

        PondInfoFragment fragment = new PondInfoFragment();
        Bundle args = new Bundle();
        args.putString("pond_id", pond.getId());
        args.putString("pond_name", pond.getName());
        args.putString("breed", pond.getBreed());
        args.putInt("fish_count", pond.getFishCount());
        args.putDouble("cost_per_fish", pond.getCostPerFish());
        args.putString("date_started", pond.getDateStarted());
        args.putString("date_harvest", pond.getDateHarvest());
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.con, fragment)
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
