package com.example.pondmatev1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FarmgatePriceActivity extends AppCompatActivity {

    private TableLayout tableFarmgate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_farmgate_price);

        tableFarmgate = findViewById(R.id.tableFarmgate);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        loadFarmgatePrices();
    }

    private void loadFarmgatePrices() {
        PondSyncManager.fetchFarmgatePrices(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(() -> {
                    try {
                        String response = result.toString();
                        Log.d("FarmgateResponse", response);

                        JSONObject json = new JSONObject(response);
                        if (!json.getString("status").equals("success")) {
                            Toast.makeText(FarmgatePriceActivity.this, "Failed to load farmgate prices", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray pricesArray = json.getJSONArray("farmgate");

                        tableFarmgate.removeViews(1, Math.max(0, tableFarmgate.getChildCount() - 1));

                        for (int i = 0; i < pricesArray.length(); i++) {
                            JSONObject item = pricesArray.getJSONObject(i);

                            int id = item.getInt("id");
                            String breed = item.getString("breed");
                            String price = item.getString("price_per_kg");
                            String updated = item.getString("updated_at");

                            TableRow row = new TableRow(FarmgatePriceActivity.this);
                            int cellHeight = (int) (56 * getResources().getDisplayMetrics().density);

                            // Edit Button
                            ImageButton btnEdit = new ImageButton(FarmgatePriceActivity.this);
                            btnEdit.setImageResource(android.R.drawable.ic_menu_edit);
                            TableRow.LayoutParams paramsBtn = new TableRow.LayoutParams(
                                    (int) (60 * getResources().getDisplayMetrics().density),
                                    cellHeight
                            );
                            paramsBtn.gravity = Gravity.CENTER;
                            btnEdit.setLayoutParams(paramsBtn);

                            // Breed
                            TextView tvBreed = new TextView(FarmgatePriceActivity.this);
                            tvBreed.setText(breed);
                            tvBreed.setGravity(Gravity.CENTER);
                            tvBreed.setHeight(cellHeight);

                            // Price
                            TextView tvPrice = new TextView(FarmgatePriceActivity.this);
                            double priceVal = Double.parseDouble(price);
                            tvPrice.setText("₱" + String.format("%.2f", priceVal));
                            tvPrice.setGravity(Gravity.CENTER);
                            tvPrice.setHeight(cellHeight);

                            // Date updated
                            TextView tvUpdated = new TextView(FarmgatePriceActivity.this);
                            tvUpdated.setText(updated);
                            tvUpdated.setGravity(Gravity.CENTER);
                            tvUpdated.setHeight(cellHeight);

                            // Borders
                            tvBreed.setBackgroundResource(R.drawable.cell_borders);
                            tvPrice.setBackgroundResource(R.drawable.cell_borders);
                            tvUpdated.setBackgroundResource(R.drawable.cell_borders);
                            btnEdit.setBackgroundResource(R.drawable.cell_borders);

                            row.addView(btnEdit);
                            row.addView(tvBreed);
                            row.addView(tvPrice);
                            row.addView(tvUpdated);

                            btnEdit.setOnClickListener(v -> showEditDialog(id, price, tvPrice));

                            tableFarmgate.addView(row);
                        }

                    } catch (JSONException e) {
                        Toast.makeText(FarmgatePriceActivity.this, "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(FarmgatePriceActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showEditDialog(int priceId, String oldPrice, TextView tvPrice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Farmgate Price");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(oldPrice);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String entered = input.getText().toString().trim();

            if (entered.isEmpty()) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }

            double newPrice;
            try {
                newPrice = Double.parseDouble(entered);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Confirm Update")
                    .setMessage("Are you sure you want to update the price to ₱" + String.format("%.2f", newPrice) + "?")
                    .setPositiveButton("Yes", (confirmDialog, w) -> {
                        PondSyncManager.updateFarmgatePrice(priceId, newPrice, new PondSyncManager.Callback() {
                            @Override
                            public void onSuccess(Object result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(FarmgatePriceActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                                    tvPrice.setText("₱" + String.format("%.2f", newPrice));
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() ->
                                        Toast.makeText(FarmgatePriceActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show()
                                );
                            }
                        });
                    })
                    .setNegativeButton("No", (confirmDialog, w) -> confirmDialog.dismiss())
                    .show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
