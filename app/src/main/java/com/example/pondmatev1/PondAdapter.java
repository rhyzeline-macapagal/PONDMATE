package com.example.pondmatev1;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static com.example.pondmatev1.PondActionHelper.loadingDialog;
import static java.security.AccessController.getContext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.SharedPreferences;
import com.google.gson.Gson;


public class PondAdapter extends RecyclerView.Adapter<PondAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<PondModel> pondList;
    private final String userType;
    private OnPondDeletedListener deleteListener;
    private androidx.appcompat.app.AlertDialog loadingDialog;

    public PondAdapter(Context context, ArrayList<PondModel> pondList, String userType) {
        this.context = context;
        this.pondList = pondList;
        this.userType = userType;
    }

    public interface OnPondDeletedListener {
        void onPondDeleteRequest(PondModel pond, int position);
    }

    public void setOnPondDeletedListener(OnPondDeletedListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pond_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PondModel pond = pondList.get(position);

        // Handle "Add" card
        if ("ADD_BUTTON".equals(pond.getMode())) {
            if ("owner".equalsIgnoreCase(userType)) {
                holder.pondImage.setImageResource(R.drawable.ic_addpond);
                holder.pondName.setVisibility(View.GONE);
                holder.stopIcon.setVisibility(View.GONE);

                holder.itemView.setOnClickListener(v -> {
                    if (context instanceof AppCompatActivity) {
                        AddPondDialogFragment dialog = new AddPondDialogFragment();
                        dialog.setOnPondAddedListener(newPond -> {
                            pondList.add(1, newPond);
                            notifyItemInserted(1);
                        });
                        dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "AddPondDialog");
                    }
                });
            } else {
                holder.itemView.setVisibility(View.GONE);
            }
            return;
        }

        // Normal pond card
        if (pond.getImagePath() != null && !pond.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(pond.getImagePath())
                    .placeholder(R.drawable.pond)
                    .error(R.drawable.pond)
                    .centerCrop()
                    .dontAnimate()
                    .into(holder.pondImage);
        } else {
            holder.pondImage.setImageResource(R.drawable.pond);
        }

        holder.pondName.setText(pond.getName());
        holder.pondName.setVisibility(View.VISIBLE);
        holder.itemView.setVisibility(View.VISIBLE);

        // ✅ Highlight card green if it's harvest day or past harvest date
        try {
            if (pond.getDateHarvest() != null && !pond.getDateHarvest().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date harvestDate = sdf.parse(pond.getDateHarvest());
                Date today = new Date();

                if (harvestDate != null && !today.before(harvestDate)) {
                    // It's harvest day or later → make background green
                    holder.itemView.setBackgroundColor(Color.parseColor("#C8E6C9")); // light green
                } else {
                    // Not harvest day yet → normal background
                    holder.itemView.setBackgroundColor(Color.WHITE);
                }
            } else {
                // No harvest date set
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        if ("owner".equalsIgnoreCase(userType)) {
            holder.stopIcon.setVisibility(View.VISIBLE);
            holder.stopIcon.setOnClickListener(v -> showEmergencyHarvestDialog(holder, pond));

        } else {
            holder.stopIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            SharedPreferences prefs = context.getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            prefs.edit().putString("selected_pond", new Gson().toJson(pond)).apply();

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("pond_id", pond.getId());
            intent.putExtra("pond_name", pond.getName());
            intent.putExtra("breed", pond.getBreed());
            intent.putExtra("fish_count", pond.getFishCount());
            intent.putExtra("cost_per_fish", pond.getCostPerFish());
            intent.putExtra("date_started", pond.getDateStarted());
            intent.putExtra("date_harvest", pond.getDateHarvest());
            intent.putExtra("date_stocking", pond.getDateStocking());
            intent.putExtra("pond_area", pond.getPondArea());
            intent.putExtra("caretaker_name", pond.getCaretakerName());
            intent.putExtra("mortality_rate", pond.getMortalityRate());


            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).overridePendingTransition(R.anim.fade_in, 0);
            }
        });
    }


    @Override
    public int getItemCount() {
        return pondList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView pondImage;
        TextView pondName;
        ImageView stopIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            pondImage = itemView.findViewById(R.id.pondImage);
            pondName = itemView.findViewById(R.id.pondName);
            stopIcon = itemView.findViewById(R.id.stopIcon);
        }
    }

    private void showEmergencyHarvestDialog(ViewHolder holder, PondModel pond) {
        Context context = holder.itemView.getContext();

        // Create an EditText for the user to input the reason
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint("Enter reason for emergency harvest");

        new AlertDialog.Builder(context)
                .setTitle("Emergency Harvest")
                .setMessage("Provide a reason for the emergency harvest of \"" + pond.getName() + "\":")
                .setView(input)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(context, "Reason is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    processEmergencyHarvest(holder, pond, reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processEmergencyHarvest(ViewHolder holder, PondModel pond, String reason) {
        Context context = holder.itemView.getContext();

        // Use the stored PDF report data if available
        JSONObject json = pond.getPdfReportData();
        if (json == null) {
            // fallback: fetch data if not stored
            PondSyncManager.fetchPondReportData(pond.getName(), new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object response) {
                    ((Activity) context).runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(String.valueOf(response));
                            generateEmergencyPDF(holder, pond, reason, json);
                        } catch (Exception e) {
                            Toast.makeText(context, "Error parsing JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Error fetching data: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } else {
            generateEmergencyPDF(holder, pond, reason, json);
        }
    }

    private void generateEmergencyPDF(ViewHolder holder, PondModel pond, String reason, JSONObject json) {
        Context context = holder.itemView.getContext();
        showLoadingDialog(context, "Generating emergency harvest report...");
        try {
            // Mark as emergency harvest
            json.put("action", "EMERGENCY_HARVEST");
            json.put("emergency_reason", reason);

            // Ensure pond object exists
            if (!json.has("pond") || json.optJSONObject("pond") == null) {
                JSONObject pondObj = new JSONObject();
                pondObj.put("id", pond.getId());
                pondObj.put("name", pond.getName());
                json.put("pond", pondObj);
            }

            // Ensure report & expenses sections exist
            JSONObject report = json.optJSONObject("report");
            if (report == null) {
                report = new JSONObject();
                json.put("report", report);
            }

            JSONObject expenses = report.optJSONObject("expenses");
            if (expenses == null) {
                expenses = new JSONObject();
                report.put("expenses", expenses);
            }

            // Load updated PondModel from SharedPreferences to get the computed salary
            SharedPreferences prefs = context.getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson != null) {
                pond = new Gson().fromJson(pondJson, PondModel.class);
            }

            // Add caretaker salary from PondModel
            double totalSalary = pond.getSalaryCost();
            if (totalSalary > 0) {
                JSONObject salarySection = new JSONObject();
                JSONArray salaryDetails = new JSONArray();
                JSONObject salaryEntry = new JSONObject();

                salaryEntry.put("description", "Caretaker Salary");
                salaryEntry.put("amount", totalSalary);
                salaryDetails.put(salaryEntry);

                salarySection.put("details", salaryDetails);
                salarySection.put("total_cost", totalSalary);
                expenses.put("Salary", salarySection);
            }

            Log.d("PDF_DEBUG_JSON", json.toString(4));
            // Generate PDF
            File pdfFile = PondPDFGenerator.generatePDF(context, json, pond.getId());
            if (pdfFile == null || !pdfFile.exists()) {
                Toast.makeText(context, "Failed to generate report PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use final variables for lambdas
            final PondModel pondFinal = pond;
            final int position = holder.getAdapterPosition();
            final File pdfFileFinal = pdfFile;

            // Set pond inactive and update UI
            PondSyncManager.setPondInactive(pondFinal, pdfFileFinal, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object resp) {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Emergency Harvest Completed", Toast.LENGTH_SHORT).show();
                        hideLoadingDialog(context);

                        if (position >= 0 && position < pondList.size()) {
                            pondList.remove(position);
                            notifyItemRemoved(position);
                        }

                        if (context instanceof PondDashboardActivity) {
                            ((PondDashboardActivity) context).loadHistory(null);
                        }

                        if (deleteListener != null) {
                            deleteListener.onPondDeleteRequest(pondFinal, position);
                        }

                        if (context instanceof PondDashboardActivity) {
                            Intent i = new Intent(context, PondDashboardActivity.class);
                            i.putExtra("pond_name", pondFinal.getName());
                            i.putExtra("pond_id", pondFinal.getId());
                            context.startActivity(i);

                            ((Activity) context).overridePendingTransition(0, 0);
                            ((Activity) context).finish();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Error uploading PDF: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            });

        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void showLoadingDialog(Context context, String message) {

        if (loadingDialog != null && loadingDialog.isShowing()) return;

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(context);

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_loading, null);

        ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
        TextView loadingText = dialogView.findViewById(R.id.loadingText);
        loadingText.setText(message);

        Animation rotate = AnimationUtils.loadAnimation(context, R.anim.rotate);
        fishLoader.startAnimation(rotate);

        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> loadingDialog.show());
        } else {
            loadingDialog.show();
        }
    }


    private void hideLoadingDialog(Context context) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> loadingDialog.dismiss());
            } else {
                loadingDialog.dismiss();
            }
        }
    }


}

