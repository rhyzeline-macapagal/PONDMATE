package com.example.pondmatev1.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pondmatev1.MainActivity;
import com.example.pondmatev1.PondDashboardActivity;
import com.example.pondmatev1.PondModel;
import com.example.pondmatev1.PondPDFGenerator;
import com.example.pondmatev1.PondSyncManager;
import com.example.pondmatev1.R;

import org.json.JSONObject;

import java.io.File;

public class PondActionHelper {

    private static AlertDialog loadingDialog;

    /** 🔹 Show custom loading dialog */
    public static void showLoading(Context context, String message) {
        if (context == null) return;

        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);

            ImageView fishLoader = view.findViewById(R.id.fishLoader);
            TextView loadingText = view.findViewById(R.id.loadingText);
            loadingText.setText(message);

            Animation rotate = AnimationUtils.loadAnimation(context, R.anim.rotate);
            fishLoader.startAnimation(rotate);

            builder.setView(view);
            builder.setCancelable(false);
            loadingDialog = builder.create();
            loadingDialog.show();
        });
    }

    /** 🔹 Hide the loading dialog safely */
    public static void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    /** 🔹 Perform automatic or emergency harvest */
    public static void harvestPond(Context context, PondModel pond, boolean isEmergency) {
        if (context == null || pond == null) return;
        showLoading(context, isEmergency ? "Emergency harvesting..." : "Processing harvest...");

        // Fetch report data from server
        PondSyncManager.fetchPondReportData(pond.getName(), new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object response) {
                ((Activity) context).runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(String.valueOf(response));

                        json.put("action", isEmergency ? "EMERGENCY_HARVEST" : "HARVEST");

                        // Ensure pond details exist
                        if (!json.has("pond") || json.optJSONObject("pond") == null) {
                            JSONObject pondObj = new JSONObject();
                            pondObj.put("id", pond.getId());
                            pondObj.put("name", pond.getName());
                            json.put("pond", pondObj);
                        }

                        // Generate report PDF
                        File pdfFile = PondPDFGenerator.generatePDF(context, json, pond.getId());
                        if (pdfFile == null || !pdfFile.exists()) {
                            hideLoading();
                            Toast.makeText(context, "Failed to generate report PDF", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Upload PDF + mark pond inactive
                        PondSyncManager.setPondInactive(pond, pdfFile, new PondSyncManager.Callback() {
                            @Override
                            public void onSuccess(Object resp) {
                                ((Activity) context).runOnUiThread(() -> {
                                    hideLoading();
                                    Toast.makeText(context, "Pond harvested successfully", Toast.LENGTH_SHORT).show();

                                    // ✅ Return to PondDashboardActivity
                                    Intent intent = new Intent(context, PondDashboardActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);

                                    if (context instanceof Activity) {
                                        ((Activity) context).finish();
                                        ((Activity) context).overridePendingTransition(R.anim.fade_in, 0);
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                ((Activity) context).runOnUiThread(() -> {
                                    hideLoading();
                                    Toast.makeText(context, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });

                    } catch (Exception e) {
                        hideLoading();
                        Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                ((Activity) context).runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(context, "Error fetching pond data: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
