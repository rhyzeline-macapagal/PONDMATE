package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class CaretakerDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CaretakerAdapter adapter;
    private ArrayList<CaretakerModel> caretakers = new ArrayList<>();
    private AlertDialog loadingDialog;

    private TextView tvTotalSalary, tvOverallSalary;

    private static final String BASE_URL = "https://pondmate.alwaysdata.net/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_dashboard);
        // ✅ Correct mapping
        tvOverallSalary = findViewById(R.id.tvTotalSalary);   // example id: tvOverallSalary
        tvTotalSalary   = findViewById(R.id.tvSalaryPerPond);

        findViewById(R.id.btnAddCaretaker).setOnClickListener(v -> showAddCaretakerDialog());

        TextView closeCaretaker = findViewById(R.id.closeCaretaker);
        if (closeCaretaker != null) {
            closeCaretaker.setOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.caretakerRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CaretakerAdapter(this, caretakers, new CaretakerAdapter.OnItemClickListener() {
            @Override
            public void onEdit(CaretakerModel model) {
                showEditDialog(model);
            }

            @Override
            public void onDelete(CaretakerModel model) {
                new AlertDialog.Builder(CaretakerDashboardActivity.this)
                        .setTitle("Confirm Delete")
                        .setMessage("Are you sure you want to delete this caretaker?")
                        .setPositiveButton("Yes", (dialogInterface, i) -> deleteCaretaker(model.getId()))
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);
        fetchCaretakers();
    }



    // ------------------ ADD CARETAKER ------------------
    private void showAddCaretakerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_caretaker);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText username = dialog.findViewById(R.id.editUsername);
        EditText password = dialog.findViewById(R.id.editPassword);
        EditText fullName = dialog.findViewById(R.id.editFullName);
        EditText address = dialog.findViewById(R.id.editAddress);
        TextView caretaker = dialog.findViewById(R.id.tvCaretaker);
        EditText salary = dialog.findViewById(R.id.editSalary);
        Button saveBtn = dialog.findViewById(R.id.saveCaretakerBtn);
        TextView closeDialog = dialog.findViewById(R.id.closeDialog);
        closeDialog.setOnClickListener(v -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String p = password.getText().toString().trim();
            String n = fullName.getText().toString().trim();
            String a = address.getText().toString().trim();
            String c = caretaker.getText().toString().trim();
            String s = salary.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty() || n.isEmpty() || a.isEmpty() || c.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            double salaryValue = 0.0;
            if (!s.isEmpty()) {
                try {
                    salaryValue = Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid salary", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            final double finalSalary = salaryValue;

            new AlertDialog.Builder(this)
                    .setTitle("Confirm Save")
                    .setMessage("Are you sure you want to add this caretaker?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        registerCaretaker(u, p, n, a, c, finalSalary);
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        dialog.show();
    }

    private void registerCaretaker(String u, String p, String n, String a, String c, double s) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "register_user.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "username=" + URLEncoder.encode(u, "UTF-8") +
                        "&password=" + URLEncoder.encode(p, "UTF-8") +
                        "&fullname=" + URLEncoder.encode(n, "UTF-8") +
                        "&address=" + URLEncoder.encode(a, "UTF-8") +
                        "&usertype=" + URLEncoder.encode(c, "UTF-8") +
                        "&salary=" + URLEncoder.encode(String.valueOf(s), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.readLine();
                in.close();

                runOnUiThread(() -> {
                    if ("success".equals(response)) {
                        Toast.makeText(this, "✅ Registration successful!", Toast.LENGTH_SHORT).show();
                        fetchCaretakers();
                    } else {
                        Toast.makeText(this, "❌ Server: " + response, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "🚫 Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // ------------------ FETCH CARETAKERS ------------------
    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
        TextView loadingText = dialogView.findViewById(R.id.loadingText);
        loadingText.setText("Loading Caretakers...");
        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        if (fishLoader != null) fishLoader.startAnimation(rotate);
        builder.setView(dialogView).setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    private void fetchCaretakers() {
        showLoadingDialog();
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "get_caretakers.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                runOnUiThread(() -> {
                    try {
                        caretakers.clear();
                        JSONArray arr = new JSONArray(response.toString());
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            double salary = obj.has("salary") && !obj.isNull("salary")
                                    ? obj.getDouble("salary") : 0.0;

                            CaretakerModel m = new CaretakerModel(
                                    obj.getString("username"),
                                    obj.getString("password"),
                                    obj.getString("fullname"),
                                    obj.getString("address"),
                                    obj.getString("usertype"),
                                    salary
                            );
                            m.setId(obj.getInt("id"));
                            caretakers.add(m);
                        }
                        adapter.notifyDataSetChanged();
                        updateSalaryTextViews();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                    }
                    hideLoadingDialog();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ------------------ SET SALARY TEXTVIEWS ------------------
    // ------------------ SET SALARY TEXTVIEWS ------------------
    private void updateSalaryTextViews() {
        double overallSalary = 0.0;
        for (CaretakerModel caretaker : caretakers) {
            overallSalary += caretaker.getSalary();
        }

        // ✅ Make it effectively final
        final double totalSalary = overallSalary;

        // ✅ Add label for overall salary
        tvOverallSalary.setText("Overall Caretakers Salary: " + formatCurrency(overallSalary));

        // Fetch pond count from DB and compute salary per pond
        PondSyncManager.fetchTotalPonds(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                int pondCount = (int) result;
                double salaryPerPond = (pondCount > 0) ? (totalSalary / pondCount) : 0.0;

                runOnUiThread(() -> {
                    // ✅ Add label for salary per pond
                    tvTotalSalary.setText("Salary per Pond: " + formatCurrency(salaryPerPond));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CaretakerDashboardActivity.this,
                            "Error fetching ponds: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    // ------------------ DELETE CARETAKER ------------------
    private void deleteCaretaker(int id) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "delete_caretaker.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String data = "id=" + URLEncoder.encode(String.valueOf(id), "UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String resp = in.readLine();
                in.close();

                runOnUiThread(() -> {
                    if ("success".equalsIgnoreCase(resp)) {
                        Toast.makeText(this, "✅ Deleted", Toast.LENGTH_SHORT).show();
                        fetchCaretakers();
                    } else {
                        Toast.makeText(this, "❌ Delete failed: " + resp, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "🚫 Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // ------------------ EDIT CARETAKER ------------------
    public void showEditDialog(CaretakerModel caretaker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_caretaker, null);
        builder.setView(view);

        EditText etFullname = view.findViewById(R.id.etEditFullname);
        EditText etUsername = view.findViewById(R.id.etEditUsername);
        EditText etPassword = view.findViewById(R.id.etEditPassword);
        EditText etAddress = view.findViewById(R.id.etEditAddress);
        EditText etSalary = view.findViewById(R.id.etEditSalary);
        Button btnSave = view.findViewById(R.id.btnSaveCaretaker);

        // Prefill existing data
        etFullname.setText(caretaker.getFullname());
        etUsername.setText(caretaker.getUsername());
        etPassword.setText(caretaker.getPassword());
        etAddress.setText(caretaker.getAddress());
        etSalary.setText(String.valueOf(caretaker.getSalary()));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSave.setOnClickListener(v -> {
            String newFullname = etFullname.getText().toString().trim();
            String newUsername = etUsername.getText().toString().trim();
            String newPassword = etPassword.getText().toString().trim();
            String newAddress = etAddress.getText().toString().trim();
            String sSalary = etSalary.getText().toString().trim();

            if (newFullname.isEmpty() || newUsername.isEmpty() || newPassword.isEmpty() || newAddress.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            double salaryValue = 0.0;
            if (!sSalary.isEmpty()) {
                try {
                    salaryValue = Double.parseDouble(sSalary);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid salary", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            final double finalSalary = salaryValue;

            new AlertDialog.Builder(this)
                    .setTitle("Confirm Save")
                    .setMessage("Are you sure you want to save changes to this caretaker?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        editCaretaker(
                                caretaker.getId(),
                                newUsername,
                                newPassword,
                                newFullname,
                                newAddress,
                                caretaker.getUsertype(),
                                finalSalary,
                                dialog
                        );
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }
    private String formatCurrency(double amount) {
        return "₱" + String.format("%,.2f", amount);
    }

    private void editCaretaker(int id, String newUsername, String newPassword,
                               String fullname, String address, String usertype, double salary, AlertDialog dialog) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "edit_caretaker.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData =
                        "id=" + URLEncoder.encode(String.valueOf(id), "UTF-8") +
                                "&username=" + URLEncoder.encode(newUsername, "UTF-8") +
                                "&password=" + URLEncoder.encode(newPassword, "UTF-8") +
                                "&fullname=" + URLEncoder.encode(fullname, "UTF-8") +
                                "&address=" + URLEncoder.encode(address, "UTF-8") +
                                "&usertype=" + URLEncoder.encode(usertype, "UTF-8") +
                                "&salary=" + URLEncoder.encode(String.valueOf(salary), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                runOnUiThread(() -> {
                    if (response.toString().contains("success")) {
                        Toast.makeText(this, "Caretaker updated!", Toast.LENGTH_SHORT).show();
                        fetchCaretakers();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "Update failed! " + response, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
