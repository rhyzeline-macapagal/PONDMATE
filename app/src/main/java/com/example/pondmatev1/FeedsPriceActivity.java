package com.example.pondmatev1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FeedsPriceActivity extends AppCompatActivity {

    private TableLayout tableFeeds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

                            TableRow row = new TableRow(FeedsPriceActivity.this);
                            row.setPadding(4, 4, 4, 4);

                            // Alternate row background (zebra style)
                            if (i % 2 == 0) {
                                row.setBackgroundColor(0xFFF9F9F9); // light gray
                            } else {
                                row.setBackgroundColor(0xFFFFFFFF); // white
                            }

                            // Action (Edit icon button)
                            ImageButton btnEdit = new ImageButton(FeedsPriceActivity.this);
                            btnEdit.setImageResource(android.R.drawable.ic_menu_edit);
                            btnEdit.setBackgroundColor(0x00000000); // transparent background
                            btnEdit.setPadding(12, 8, 12, 8);
                            btnEdit.setBackgroundResource(R.drawable.cell_border);
                            TableRow.LayoutParams params = new TableRow.LayoutParams(
                                    TableRow.LayoutParams.WRAP_CONTENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                            );
                            btnEdit.setLayoutParams(params);
                            row.addView(btnEdit);


                            // Breed
                            TextView tvBreed = new TextView(FeedsPriceActivity.this);
                            tvBreed.setText(breed);
                            tvBreed.setPadding(12, 8, 12, 8);
                            tvBreed.setMinWidth(100);
                            tvBreed.setBackgroundResource(R.drawable.cell_border); // ✅ add border
                            tvBreed.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                            row.addView(tvBreed);

// Feed type
                            TextView tvType = new TextView(FeedsPriceActivity.this);
                            tvType.setText(feedType);
                            tvType.setPadding(12, 8, 12, 8);
                            tvType.setMinWidth(80);
                            tvType.setBackgroundResource(R.drawable.cell_border); // ✅ add border
                            tvType.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                            row.addView(tvType);

// Price
                            TextView tvPrice = new TextView(FeedsPriceActivity.this);
                            double priceVal = Double.parseDouble(price);
                            tvPrice.setText("₱" + String.format("%.2f", priceVal));
                            tvPrice.setPadding(12, 8, 12, 8);
                            tvPrice.setMinWidth(80);
                            tvPrice.setBackgroundResource(R.drawable.cell_border); // ✅ add border
                            tvPrice.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                            row.addView(tvPrice);

// Date updated
                            TextView tvUpdated = new TextView(FeedsPriceActivity.this);
                            tvUpdated.setText(updated);
                            tvUpdated.setPadding(12, 8, 12, 8);
                            tvUpdated.setMinWidth(150);
                            tvUpdated.setBackgroundResource(R.drawable.cell_border); // ✅ add border

                            row.addView(tvUpdated);




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
