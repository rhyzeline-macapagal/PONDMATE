package com.example.pondmatev1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CaretakerDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CaretakerAdapter adapter;
    CaretakerPondAdapter pondAdapter;
    private ArrayList<CaretakerModel> caretakers = new ArrayList<>();
    private AlertDialog loadingDialog;

    private TextView tvOverallSalary;

    private static final String BASE_URL = "https://pondmate.alwaysdata.net/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_dashboard);
        // ✅ Correct mapping
        tvOverallSalary = findViewById(R.id.tvTotalSalary);   // example id: tvOverallSalary

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
        EditText salary = dialog.findViewById(R.id.editSalary);
        Button saveBtn = dialog.findViewById(R.id.saveCaretakerBtn);
        TextView closeDialog = dialog.findViewById(R.id.closeDialog);
        closeDialog.setOnClickListener(v -> dialog.dismiss());

        // ✅ Multi-select ponds
        Button btnSelectPonds = dialog.findViewById(R.id.btnSelectPonds);
        TextView selectedPondsLabel = dialog.findViewById(R.id.tvSelectedPonds);

        ArrayList<String> pondNames = new ArrayList<>();
        ArrayList<Integer> pondIds = new ArrayList<>();
        Set<String> selectedCaretakerIds = new HashSet<>();
        ArrayList<Integer> selectedPondIds = new ArrayList<>();

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "get_active_ponds.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                JSONArray ponds = new JSONArray(response.toString());
                pondNames.clear();
                pondIds.clear();

                for (int i = 0; i < ponds.length(); i++) {
                    JSONObject obj = ponds.getJSONObject(i);
                    pondNames.add(obj.getString("name"));
                    pondIds.add(obj.getInt("id"));
                }

                runOnUiThread(() -> btnSelectPonds.setEnabled(true));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to fetch ponds: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();

        btnSelectPonds.setOnClickListener(v -> {
            boolean[] checkedItems = new boolean[pondNames.size()];

            // ✅ Mark already selected ponds as checked
            for (int i = 0; i < pondIds.size(); i++) {
                checkedItems[i] = selectedPondIds.contains(pondIds.get(i));
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Ponds");
            builder.setMultiChoiceItems(pondNames.toArray(new String[0]), checkedItems,
                    (dialog1, which, isChecked) -> {
                        int id = pondIds.get(which);
                        if (isChecked) {
                            if (!selectedPondIds.contains(id)) {
                                selectedPondIds.add(id); // ✅ no duplicates now
                            }
                        } else {
                            selectedPondIds.remove(Integer.valueOf(id));
                        }
                    });

            builder.setPositiveButton("OK", (d, w) -> {
                if (selectedPondIds.isEmpty()) {
                    selectedPondsLabel.setText("No ponds selected");
                } else {
                    StringBuilder names = new StringBuilder();
                    for (int i = 0; i < selectedPondIds.size(); i++) {
                        int id = selectedPondIds.get(i);
                        names.append(pondNames.get(pondIds.indexOf(id)));
                        if (i < selectedPondIds.size() - 1) names.append(", ");
                    }
                    selectedPondsLabel.setText("Selected: " + names);
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        saveBtn.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String p = password.getText().toString().trim();
            String n = fullName.getText().toString().trim();
            String a = address.getText().toString().trim();
            String s = salary.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty() || n.isEmpty() || a.isEmpty() || s.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double salaryValue;
            try {
                salaryValue = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid salary value", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedPondIds.isEmpty()) {
                Toast.makeText(this, "Please select at least one pond", Toast.LENGTH_SHORT).show();
                return;
            }

            String joinedPondIds = TextUtils.join(",", selectedPondIds);

            new AlertDialog.Builder(this)
                    .setTitle("Confirm Save")
                    .setMessage("Add this caretaker?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        new Thread(() -> {
                            try {
                                URL url = new URL(BASE_URL + "register_user.php");
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setDoOutput(true);
                                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                                String postData =
                                        "username=" + URLEncoder.encode(u, "UTF-8") +
                                                "&password=" + URLEncoder.encode(p, "UTF-8") +
                                                "&fullname=" + URLEncoder.encode(n, "UTF-8") +
                                                "&address=" + URLEncoder.encode(a, "UTF-8") +
                                                "&usertype=Caretaker" +
                                                "&salary=" + URLEncoder.encode(String.valueOf(salaryValue), "UTF-8") +
                                                "&pond_ids=" + URLEncoder.encode(joinedPondIds, "UTF-8");

                                OutputStream os = conn.getOutputStream();
                                os.write(postData.getBytes());
                                os.flush();
                                os.close();

                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String result = in.readLine();
                                in.close();

                                runOnUiThread(() -> {
                                    if (result.contains("success")) {
                                        Toast.makeText(this, "Caretaker added!", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        fetchCaretakers();
                                    } else if (result.contains("exists")) {
                                        Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "Error: " + result, Toast.LENGTH_LONG).show();
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() ->
                                        Toast.makeText(this, "Request failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                            }
                        }).start();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        dialog.show();
    }

    private void registerCaretaker(String u, String p, String n, String a, String c, double s, String pondIds) {
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
                        "&salary=" + URLEncoder.encode(String.valueOf(s), "UTF-8") +
                        "&pond_ids=" + URLEncoder.encode(pondIds, "UTF-8");

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

                            if (obj.has("pond_count") && !obj.isNull("pond_count")) {
                                m.setPondCount(obj.getInt("pond_count"));
                            }

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
    private void updateSalaryTextViews() {
        double overallSalary = 0.0;
        for (CaretakerModel caretaker : caretakers) {
            overallSalary += caretaker.getSalary();
        }

        // ✅ Make it effectively final
        final double totalSalary = overallSalary;

        // ✅ Add label for overall salary
        tvOverallSalary.setText("Overall Caretakers Salary: " + formatCurrency(overallSalary));
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
        RecyclerView rvPonds = view.findViewById(R.id.rvCaretakerPonds);
        Button btnSaveInfo = view.findViewById(R.id.btnSaveCaretaker);
        Button btnSavePonds = view.findViewById(R.id.btnSavePondAssignments);

        // Prefill
        etFullname.setText(caretaker.getFullname());
        etUsername.setText(caretaker.getUsername());
        etPassword.setText(caretaker.getPassword());
        etAddress.setText(caretaker.getAddress());
        etSalary.setText(String.valueOf(caretaker.getSalary()));

        AlertDialog dialog = builder.create();
        dialog.show();

        // ✅ Load ponds list into recycler
        loadAssignedPonds(caretaker.getId(), rvPonds);

        // ✅ Save caretaker info (NO changes to pond assignment here)
        btnSaveInfo.setOnClickListener(v -> {
            editCaretaker(
                    caretaker.getId(),
                    etUsername.getText().toString().trim(),
                    etPassword.getText().toString().trim(),
                    etFullname.getText().toString().trim(),
                    etAddress.getText().toString().trim(),
                    caretaker.getUsertype(),
                    Double.parseDouble(etSalary.getText().toString().trim()),
                    dialog
            );
        });

        // ✅ Save pond assignments only
        btnSavePonds.setOnClickListener(v -> saveCaretakerPonds(caretaker.getId()));
    }

    private String formatCurrency(double amount) {
        return "₱" + String.format("%,.2f", amount);
    }

    private void loadAssignedPonds(int caretakerId, RecyclerView rv) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "get_all_ponds_with_assignment.php?caretaker_id=" + caretakerId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                JSONArray arr = new JSONArray(response.toString());
                ArrayList<PondModel> ponds = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    PondModel p = new PondModel(o.getString("name")); // <-- Exists in your constructors
                    p.setId(String.valueOf(o.getInt("id"))); // Store ID as String
                    p.setAssigned(o.getBoolean("assigned"));
                    ponds.add(p);
                }

                runOnUiThread(() -> {
                    pondAdapter = new CaretakerPondAdapter(ponds);
                    rv.setLayoutManager(new LinearLayoutManager(this));
                    rv.setAdapter(pondAdapter);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

    private void saveCaretakerPonds(int caretakerId) {

        List<PondModel> selected = pondAdapter.getSelectedPonds();
        StringBuilder ids = new StringBuilder();

        for (PondModel p : selected) {
            if (ids.length() > 0) ids.append(",");
            ids.append(p.getId());
        }

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "update_caretaker_ponds.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String data = "caretaker_id=" + caretakerId + "&pond_ids=" + ids;
                conn.getOutputStream().write(data.getBytes());

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.readLine();
                in.close();

                runOnUiThread(() -> Toast.makeText(this, response, Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
