package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

        TextView closecaretaker = findViewById(R.id.closeCaretaker);
        closecaretaker.setOnClickListener(v -> finish());


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


        EditText username = dialog.findViewById(R.id.editUsername);
        EditText password = dialog.findViewById(R.id.editPassword);
        EditText fullName = dialog.findViewById(R.id.editFullName);
        EditText address = dialog.findViewById(R.id.editAddress);
        TextView caretaker = dialog.findViewById(R.id.tvCaretaker);
        Button saveBtn = dialog.findViewById(R.id.saveCaretakerBtn);

        // Save button click
        saveBtn.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String p = password.getText().toString().trim();
            String n = fullName.getText().toString().trim();
            String a = address.getText().toString().trim();
            String c = caretaker.getText().toString().trim(); // "Caretaker"

            if (u.isEmpty() || p.isEmpty() || n.isEmpty() || a.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return; // Stop here if validation fails
            }

            // Show confirmation dialog
            new AlertDialog.Builder(CaretakerDashboardActivity.this)
                    .setTitle("Confirm Registration")
                    .setMessage("Are you sure you want to register this caretaker?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        dialog.dismiss(); // Dismiss the original registration dialog

                        // Proceed with registration
                        new Thread(() -> {
                            try {
                                URL url = new URL("https://pondmate.alwaysdata.net/register_user.php");
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setDoOutput(true);
                                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                                String postData = "username=" + URLEncoder.encode(u, "UTF-8") +
                                        "&password=" + URLEncoder.encode(p, "UTF-8") +
                                        "&fullname=" + URLEncoder.encode(n, "UTF-8") +
                                        "&address=" + URLEncoder.encode(a, "UTF-8") +
                                        "&usertype=" + URLEncoder.encode(c, "UTF-8");

                                OutputStream os = conn.getOutputStream();
                                os.write(postData.getBytes());
                                os.flush();
                                os.close();

                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String response = in.readLine();
                                in.close();

                                runOnUiThread(() -> {
                                    switch (response) {
                                        case "success":
                                            Toast.makeText(CaretakerDashboardActivity.this, "✅ Registration successful!", Toast.LENGTH_SHORT).show();
                                            dbHelper.addUser(u, p, n, a, c); // Optional SQLite insert
                                            break;

                                        case "exists":
                                            Toast.makeText(CaretakerDashboardActivity.this, "❗ Username already exists", Toast.LENGTH_LONG).show();
                                            break;

                                        case "missing":
                                            Toast.makeText(CaretakerDashboardActivity.this, "⚠️ Missing required data", Toast.LENGTH_LONG).show();
                                            break;

                                        default:
                                            Toast.makeText(CaretakerDashboardActivity.this, "❌ Server error: " + response, Toast.LENGTH_LONG).show();
                                            break;
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() ->
                                        Toast.makeText(CaretakerDashboardActivity.this, "🚫 Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                            }
                        }).start();

                    })
                    .setNegativeButton("No", null)
                    .show();
        });


        dialog.show();
    }





}
