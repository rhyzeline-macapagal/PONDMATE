package com.example.pondmatev1;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;



import java.util.ArrayList;

public class CaretakerDashboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DatabaseHelper dbHelper;
    ArrayList<CaretakerModel> caretakers;
    CaretakerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_dashboard);

        recyclerView = findViewById(R.id.caretakerRecyclerView);
        dbHelper = new DatabaseHelper(this);

        caretakers = dbHelper.getCaretakersList();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CaretakerAdapter(this, caretakers, new CaretakerAdapter.OnItemClickListener() {
            @Override
            public void onEdit(CaretakerModel model) {

                Toast.makeText(CaretakerDashboardActivity.this, "Edit " + model.getUsername(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(CaretakerModel model) {
                // TODO: Show confirm dialog and delete from db
                Toast.makeText(CaretakerDashboardActivity.this, "Delete " + model.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
    }
}
