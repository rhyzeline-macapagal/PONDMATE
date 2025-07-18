package com.example.pondmatev1;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;



import java.util.ArrayList;

public class PondDashboardActivity extends AppCompatActivity {

    RecyclerView pondRecyclerView;
    PondAdapter pondAdapter;
    ArrayList<PondModel> pondList;
    String userType = "caretaker";


    private void loadLocalPonds() {
        pondList.clear();
        String userType = new SessionManager(this).getUsertype();
        if ("owner".equalsIgnoreCase(userType)) {
            pondList.add(0, new PondModel("ADD_BUTTON"));
        }



        DatabaseHelper db = new DatabaseHelper(this);
        Cursor c = db.getAllPonds();

        if (c != null && c.moveToFirst()) {
            do {
                pondList.add(new PondModel(
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("breed")),
                        c.getInt(c.getColumnIndexOrThrow("fish_count")),
                        c.getDouble(c.getColumnIndexOrThrow("cost_per_fish")),
                        c.getString(c.getColumnIndexOrThrow("date_started")),
                        c.getString(c.getColumnIndexOrThrow("date_harvest")),
                        "DATA"
                ));
            } while (c.moveToNext());
            c.close();
        }

        pondAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pond_dashboard);
        String userType = new SessionManager(this).getUsertype();

        ImageView adminIcon = findViewById(R.id.adminIcon);
        adminIcon.setOnClickListener(v -> {
            Intent intent = new Intent(PondDashboardActivity.this, CaretakerDashboardActivity.class);
            startActivity(intent);
        });



        if (!"owner".equalsIgnoreCase(userType)) {
            adminIcon.setVisibility(View.GONE);
        } else {
            adminIcon.setVisibility(View.VISIBLE);
        }

        pondRecyclerView = findViewById(R.id.pondRecyclerView);


        int spacing = getResources().getDimensionPixelSize(R.dimen.pond_card_spacing);
        pondRecyclerView.addItemDecoration(new SpacingItemDecoration(spacing));

        pondList = new ArrayList<>();
        pondList.add(new PondModel("ADD_BUTTON"));


        pondAdapter = new PondAdapter(this, pondList, userType);

        pondRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        pondRecyclerView.setAdapter(pondAdapter);

        loadLocalPonds();
    }

}
