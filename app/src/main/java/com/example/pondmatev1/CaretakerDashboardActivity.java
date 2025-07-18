package com.example.pondmatev1;

import android.app.Dialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;



import java.util.ArrayList;

public class CaretakerDashboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DatabaseHelper dbHelper;
    ArrayList<CaretakerModel> caretakers;
    CaretakerAdapter adapter;

    Button btnaddcaretaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_dashboard);

        recyclerView = findViewById(R.id.caretakerRecyclerView);
        dbHelper = new DatabaseHelper(this);

        btnaddcaretaker = findViewById(R.id.btnAddCaretaker);
        btnaddcaretaker.setOnClickListener(v -> showAddCaretakerDialog());


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

    private void showAddCaretakerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_caretaker);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Close button
        TextView closeDialog = dialog.findViewById(R.id.closeDialog);
        closeDialog.setOnClickListener(v -> dialog.dismiss());

        // Input fields
        EditText username = dialog.findViewById(R.id.editUsername);
        EditText password = dialog.findViewById(R.id.editPassword);
        EditText fullName = dialog.findViewById(R.id.editFullName);
        EditText address = dialog.findViewById(R.id.editAddress);
        Button saveBtn = dialog.findViewById(R.id.saveCaretakerBtn);

        // Save button click
        saveBtn.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String p = password.getText().toString().trim();
            String n = fullName.getText().toString().trim();
            String a = address.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty() || n.isEmpty() || a.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            } else {
                // Save to DB logic here
                Toast.makeText(this, "Caretaker saved!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }


}
