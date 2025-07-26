package com.example.pondmatev1;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PondDashboardActivity extends AppCompatActivity {

    RecyclerView pondRecyclerView;
    PondAdapter pondAdapter;
    ArrayList<PondModel> pondList;
    String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pond_dashboard);

        userType = new SessionManager(this).getUsertype();

        ImageView adminIcon = findViewById(R.id.adminIcon);
        adminIcon.setOnClickListener(v -> {
            Intent intent = new Intent(PondDashboardActivity.this, CaretakerDashboardActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.drop_in, R.anim.fade_out);

        });

        ImageView profileIcon = findViewById(R.id.profileIcon);

        profileIcon.setOnClickListener(v -> {
            UserProfileDialogFragment dialog = new UserProfileDialogFragment();
            dialog.show(getSupportFragmentManager(), "UserProfileDialog");
        });


        if (!"owner".equalsIgnoreCase(userType)) {
            adminIcon.setVisibility(View.GONE);
        } else {
            adminIcon.setVisibility(View.VISIBLE);
        }

        pondRecyclerView = findViewById(R.id.pondRecyclerView);
        int spacing = getResources().getDimensionPixelSize(R.dimen.pond_card_spacing);
        pondRecyclerView.addItemDecoration(new SpacingItemDecoration(spacing));
        pondRecyclerView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        pondList = new ArrayList<>();
        pondAdapter = new PondAdapter(this, pondList, userType);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        pondRecyclerView.setLayoutManager(layoutManager);
        pondRecyclerView.setAdapter(pondAdapter);

        PondSyncManager.syncPondsFromServer(this);
        loadLocalPonds();  // Make sure this is called only once unless manually refreshed
    }

    private void loadLocalPonds() {
        pondList.clear();

        if ("owner".equalsIgnoreCase(userType)) {
            pondList.add(new PondModel("ADD_BUTTON"));
        }

        DatabaseHelper db = new DatabaseHelper(this);
        Cursor c = db.getAllPonds();

        if (c != null && c.moveToFirst()) {
            do {
                PondModel pond = new PondModel(
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("breed")),
                        c.getInt(c.getColumnIndexOrThrow("fish_count")),
                        c.getDouble(c.getColumnIndexOrThrow("cost_per_fish")),
                        c.getString(c.getColumnIndexOrThrow("date_started")),
                        c.getString(c.getColumnIndexOrThrow("date_harvest")),
                        "DATA"
                );

                pondList.add(pond);

            } while (c.moveToNext());
            c.close();
        }

        Log.d("POND_LOAD", "Loaded pond count: " + pondList.size());
        pondAdapter.notifyDataSetChanged();
    }
}