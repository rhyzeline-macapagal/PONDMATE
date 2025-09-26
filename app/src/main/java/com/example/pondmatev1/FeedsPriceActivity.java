package com.example.pondmatev1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
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

public class FeedsPriceActivity extends AppCompatActivity {

    private TableLayout tableFeeds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_feeds_price);

        tableFeeds = findViewById(R.id.tableFeeds);

        // Close button
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Load feeds from server
        loadFeeds();
    }

    private void loadFeeds() {
        PondSyncManager.fetchFeeds(new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(() -> {
                    try {
                        String response = result.toString(); // ✅ ensure it's parsed as String
                        Log.d("FeedsResponse", response);

                        JSONObject json = new JSONObject(response);
                        if (!json.getString("status").equals("success")) {
                            Toast.makeText(FeedsPriceActivity.this, "Failed to load feeds", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray feedsArray = json.getJSONArray("feeds");

                        // Clear old rows (except header)
                        tableFeeds.removeViews(1, Math.max(0, tableFeeds.getChildCount() - 1));

                        // Populate table
                        for (int i = 0; i < feedsArray.length(); i++) {
                            JSONObject feed = feedsArray.getJSONObject(i);

                            int id = feed.getInt("id");
                            String breed = feed.getString("breed");
                            String feedType = feed.getString("feed_type");
                            String price = feed.getString("price_per_kg");
                            String updated = feed.getString("updated_at");

                            // TableRow
                            TableRow row = new TableRow(FeedsPriceActivity.this);
                            row.setPadding(0, 0, 0, 0);

// Common cell height (e.g., 60dp)
                            int cellHeight = (int) (56 * getResources().getDisplayMetrics().density);

// Action (Edit icon button)
                            ImageButton btnEdit = new ImageButton(FeedsPriceActivity.this);
                            btnEdit.setImageResource(android.R.drawable.ic_menu_edit);
                            btnEdit.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
                            btnEdit.setAdjustViewBounds(true);

// ✅ Force same size as text cells
                            TableRow.LayoutParams paramsBtn = new TableRow.LayoutParams(
                                    (int) (60 * getResources().getDisplayMetrics().density), // width
                                    cellHeight // height
                            );
                            paramsBtn.gravity = Gravity.CENTER;
                            btnEdit.setLayoutParams(paramsBtn);

// Remove default background (so only your border shows)
                            row.addView(btnEdit);

// Breed
                            TextView tvBreed = new TextView(FeedsPriceActivity.this);
                            tvBreed.setText(breed);
                            tvBreed.setPadding(12, 8, 12, 8);
                            tvBreed.setMinWidth(100);
                            tvBreed.setGravity(Gravity.CENTER);
                            tvBreed.setHeight(cellHeight); // ✅ match row height
                            row.addView(tvBreed);

// Feed type
                            TextView tvType = new TextView(FeedsPriceActivity.this);
                            tvType.setText(feedType);
                            tvType.setPadding(12, 8, 12, 8);
                            tvType.setMinWidth(80);
                            tvType.setGravity(Gravity.CENTER);
                            tvType.setHeight(cellHeight); // ✅ match row height
                            row.addView(tvType);

// Price
                            TextView tvPrice = new TextView(FeedsPriceActivity.this);
                            double priceVal = Double.parseDouble(price);
                            tvPrice.setText("₱" + String.format("%.2f", priceVal));
                            tvPrice.setPadding(12, 8, 12, 8);
                            tvPrice.setMinWidth(80);
                            tvPrice.setGravity(Gravity.CENTER);
                            tvPrice.setHeight(cellHeight); // ✅ match row height
                            row.addView(tvPrice);

// Date updated
                            TextView tvUpdated = new TextView(FeedsPriceActivity.this);
                            tvUpdated.setText(updated);
                            tvUpdated.setPadding(12, 8, 12, 8);
                            tvUpdated.setMinWidth(150);
                            tvUpdated.setGravity(Gravity.CENTER);
                            tvUpdated.setHeight(cellHeight); // ✅ match row height
                            row.addView(tvUpdated);


                            tvBreed.setBackgroundResource(R.drawable.cell_borders);
                            tvType.setBackgroundResource(R.drawable.cell_borders);
                            tvPrice.setBackgroundResource(R.drawable.cell_borders);
                            tvUpdated.setBackgroundResource(R.drawable.cell_borders);
                            btnEdit.setBackgroundResource(R.drawable.cell_borders);



                            // Fix edit button action
                            btnEdit.setOnClickListener(v -> showEditDialog(id, price, tvPrice));

                            tableFeeds.addView(row);
                        }



                    } catch (JSONException e) {
                        Toast.makeText(FeedsPriceActivity.this, "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(FeedsPriceActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showEditDialog(int feedId, String oldPrice, TextView tvPrice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Price");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(oldPrice);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String entered = input.getText().toString().trim();

            // ✅ Check if input is empty or invalid
            if (entered.isEmpty() || entered.equals(".") || entered.equals(",")) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return; // don't proceed
            }

            double newPrice;
            try {
                newPrice = Double.parseDouble(entered);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Update")
                    .setMessage("Are you sure you want to update the price to ₱" + String.format("%.2f", newPrice) + "?")
                    .setPositiveButton("Yes", (confirmDialog, w) -> {
                        PondSyncManager.updateFeedPrice(feedId, newPrice, new PondSyncManager.Callback() {
                            @Override
                            public void onSuccess(Object result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(FeedsPriceActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                                    // ✅ formatted display
                                    tvPrice.setText("₱" + String.format("%.2f", newPrice));
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() ->
                                        Toast.makeText(FeedsPriceActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show()
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
