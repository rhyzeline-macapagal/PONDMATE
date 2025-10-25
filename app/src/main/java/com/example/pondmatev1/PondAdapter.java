package com.example.pondmatev1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import android.content.SharedPreferences;
import com.google.gson.Gson;


public class PondAdapter extends RecyclerView.Adapter<PondAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<PondModel> pondList;
    private final String userType;
    private OnPondDeletedListener deleteListener;

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

        if ("owner".equalsIgnoreCase(userType)) {
            holder.stopIcon.setVisibility(View.VISIBLE);
            holder.stopIcon.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Stop Progress")
                        .setMessage("Choose what you want to do with this pond:")
                        .setNegativeButton("Set to Inactive", (dialog, which) -> setPondInactive(holder, pond))
                        .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
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

            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).overridePendingTransition(R.anim.fade_in, 0);
            }
        });
    }

    private void setPondInactive(ViewHolder holder, PondModel pond) {
        Context context = holder.itemView.getContext();

        new AlertDialog.Builder(context)
                .setTitle("Set Pond as Inactive")
                .setMessage("Are you sure you want to mark \"" + pond.getName() + "\" as INACTIVE?\n\n" +
                        "It will be removed from the active list and moved to Pond History.")
                .setPositiveButton("Yes", (dialog, which) -> {

                    // 🟢 Step 1: Fetch full report data first
                    PondSyncManager.fetchPondReportData(pond.getName(), new PondSyncManager.Callback() {
                        @Override
                        public void onSuccess(Object response) {
                            ((Activity) context).runOnUiThread(() -> {
                                try {
                                    String raw = String.valueOf(response);
                                    JSONObject json = new JSONObject(raw);

                                    // Add "INACTIVE" marker for PDF title/watermark
                                    json.put("action", "INACTIVE");

                                    // Ensure pond object exists inside JSON
                                    if (!json.has("pond") || json.optJSONObject("pond") == null) {
                                        JSONObject pondObj = new JSONObject();
                                        pondObj.put("id", pond.getId());
                                        pondObj.put("name", pond.getName());
                                        json.put("pond", pondObj);
                                    }

                                    // 🟢 Step 2: Generate full PDF from JSON
                                    File pdfFile = PondPDFGenerator.generatePDF(context, json, pond.getId());

                                    if (pdfFile != null && pdfFile.exists()) {
                                        // 🟢 Step 3: Upload & mark as inactive
                                        PondSyncManager.setPondInactive(pond, pdfFile, new PondSyncManager.Callback() {
                                            @Override
                                            public void onSuccess(Object resp) {
                                                ((Activity) context).runOnUiThread(() -> {
                                                    Toast.makeText(context, "Pond set as INACTIVE", Toast.LENGTH_SHORT).show();

                                                    int position = holder.getAdapterPosition();
                                                    if (position >= 0 && position < pondList.size()) {
                                                        pondList.remove(position);
                                                        notifyItemRemoved(position);
                                                    }

                                                    if (context instanceof PondDashboardActivity) {
                                                        ((PondDashboardActivity) context).loadHistory(null);
                                                    }

                                                    if (deleteListener != null) {
                                                        deleteListener.onPondDeleteRequest(pond, position);
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
                                    } else {
                                        Toast.makeText(context, "Failed to generate report PDF", Toast.LENGTH_SHORT).show();
                                    }

                                } catch (Exception e) {
                                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private File getExistingPdfForPond(String pondId) {
        // Search both cacheDir and filesDir/pond_pdfs for matching files
        File[] searchDirs = new File[] {
                context.getCacheDir(),
                new File(context.getFilesDir(), "pond_pdfs")
        };

        File best = null;
        for (File dir : searchDirs) {
            if (dir == null || !dir.exists()) continue;
            File[] files = dir.listFiles((d, name) -> name.startsWith("pond_" + pondId + "_") || name.contains("pond_" + pondId + "_"));
            if (files == null || files.length == 0) continue;
            for (File f : files) {
                if (best == null || f.lastModified() > best.lastModified()) best = f;
            }
        }
        return best;
    }

    private void saveAndDeletePond(ViewHolder holder, PondModel pond, File pdfFile) {
        // If we don't have a local file, ensure pond.getPdfPath() is available
        File pdfToUse = (pdfFile != null && pdfFile.exists()) ? pdfFile : null;

        // If no local file, ensure pond.pdfPath is set (maybe came from earlier server upload)
        String serverPdfPath = (pond.getPdfPath() != null && !pond.getPdfPath().isEmpty()) ? pond.getPdfPath() : "";

        // If no local file and no server path, then we will still call save but server will attempt to handle it
        PondSyncManager.savePondHistoryWithPDF(pond, "INACTIVE", pdfToUse, new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Pond set as INACTIVE", Toast.LENGTH_SHORT).show();

                    int position = holder.getAdapterPosition();
                    if (position >= 0 && position < pondList.size()) {
                        pondList.remove(position);
                        notifyItemRemoved(position);
                    }

                    PondDashboardActivity.refreshHistoryNow();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(context, "Failed to mark pond as INACTIVE: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void runOnUiThread(Runnable r) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(r);
        }
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
}
