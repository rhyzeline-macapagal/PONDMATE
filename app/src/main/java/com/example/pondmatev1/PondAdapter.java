package com.example.pondmatev1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
            } else holder.itemView.setVisibility(View.GONE);
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
        } else holder.pondImage.setImageResource(R.drawable.pond);

        holder.pondName.setText(pond.getName());
        holder.pondName.setVisibility(View.VISIBLE);
        holder.itemView.setVisibility(View.VISIBLE);

        // Highlight if harvest day or past
        try {
            if (pond.getDateHarvest() != null && !pond.getDateHarvest().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date harvestDate = sdf.parse(pond.getDateHarvest());
                Date today = new Date();
                holder.itemView.setBackgroundColor((harvestDate != null && !today.before(harvestDate))
                        ? Color.parseColor("#C8E6C9") : Color.WHITE);
            } else holder.itemView.setBackgroundColor(Color.WHITE);
        } catch (Exception e) {
            e.printStackTrace();
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        // Emergency harvest icon for owner
        if ("owner".equalsIgnoreCase(userType)) {
            holder.stopIcon.setVisibility(View.VISIBLE);
            holder.stopIcon.setOnClickListener(v -> showEmergencyHarvestDialog(holder, pond, position));
        } else holder.stopIcon.setVisibility(View.GONE);

        // Normal pond click → open MainActivity
        holder.itemView.setOnClickListener(v -> {
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
            if (context instanceof Activity) ((Activity) context).overridePendingTransition(R.anim.fade_in, 0);
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

    private void showEmergencyHarvestDialog(ViewHolder holder, PondModel pond, int position) {
        Context context = holder.itemView.getContext();
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
                    processEmergencyHarvest(holder, pond, position, reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processEmergencyHarvest(ViewHolder holder, PondModel pond, int position, String reason) {
        Context context = holder.itemView.getContext();
        JSONObject json = pond.getPdfReportData();

        if (json == null) {
            PondSyncManager.fetchPondReportData(pond.getName(), new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object response) {
                    ((Activity) context).runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(String.valueOf(response));
                            generateEmergencyPDF(holder, pond, position, reason, json);
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
        } else generateEmergencyPDF(holder, pond, position, reason, json);
    }

    private void generateEmergencyPDF(ViewHolder holder, PondModel pond, int position, String reason, JSONObject json) {
        Context context = holder.itemView.getContext();
        showLoadingDialog(context, "Generating emergency harvest report...");

        try {
            json.put("action", "EMERGENCY_HARVEST");
            json.put("emergency_reason", reason);

            if (!json.has("pond") || json.optJSONObject("pond") == null) {
                JSONObject pondObj = new JSONObject();
                pondObj.put("id", pond.getId());
                pondObj.put("name", pond.getName());
                json.put("pond", pondObj);
            }

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

            SharedPreferences prefs = context.getSharedPreferences("POND_PREF", Context.MODE_PRIVATE);
            String pondJson = prefs.getString("selected_pond", null);
            if (pondJson != null) {
                pond = new Gson().fromJson(pondJson, PondModel.class);
            }

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

            File pdfFile = PondPDFGenerator.generatePDF(context, json, pond.getId());
            if (pdfFile == null || !pdfFile.exists()) {
                Toast.makeText(context, "Failed to generate report PDF", Toast.LENGTH_SHORT).show();
                hideLoadingDialog(context);
                return;
            }

            final PondModel pondFinal = pond;
            final int posFinal = position;
            final File pdfFileFinal = pdfFile;

            PondSyncManager.setPondInactive(pondFinal, pdfFileFinal, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object resp) {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Emergency Harvest Completed", Toast.LENGTH_SHORT).show();
                        hideLoadingDialog(context);

                        if (posFinal >= 0 && posFinal < pondList.size()) {
                            pondList.remove(posFinal);
                            notifyItemRemoved(posFinal);
                        }

                        if (context instanceof PondDashboardActivity) {
                            ((PondDashboardActivity) context).loadHistory(null);
                        }

                        if (deleteListener != null) {
                            deleteListener.onPondDeleteRequest(pondFinal, posFinal);
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
            hideLoadingDialog(context);
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadingDialog(Context context, String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);

        ImageView fishLoader = dialogView.findViewById(R.id.fishLoader);
        TextView loadingText = dialogView.findViewById(R.id.loadingText);
        loadingText.setText(message);

        Animation rotate = AnimationUtils.loadAnimation(context, R.anim.rotate);
        fishLoader.startAnimation(rotate);

        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
        if (context instanceof Activity) ((Activity) context).runOnUiThread(() -> loadingDialog.show());
        else loadingDialog.show();
    }

    private void hideLoadingDialog(Context context) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            if (context instanceof Activity) ((Activity) context).runOnUiThread(() -> loadingDialog.dismiss());
            else loadingDialog.dismiss();
        }
    }
}
