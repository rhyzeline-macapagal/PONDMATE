package com.example.pondmatev1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

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
                        .setTitle("Stop Pond")
                        .setMessage("Choose what you want to do with this pond:")
                        .setPositiveButton("Reset Pond Details", (dialog, which) -> resetPond(holder, pond))
                        .setNegativeButton("Delete Pond", (dialog, which) -> deletePond(holder, pond))
                        .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        } else {
            holder.stopIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("pond_id", pond.getId());
            intent.putExtra("pond_name", pond.getName());
            intent.putExtra("breed", pond.getBreed());
            intent.putExtra("fish_count", pond.getFishCount());
            intent.putExtra("cost_per_fish", pond.getCostPerFish());
            intent.putExtra("date_started", pond.getDateStarted());
            intent.putExtra("date_harvest", pond.getDateHarvest());
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).overridePendingTransition(R.anim.fade_in, 0);
            }
        });
    }

    private void resetPond(ViewHolder holder, PondModel pond) {
        ResetPondDialogFragment resetDialog = new ResetPondDialogFragment();
        resetDialog.setPond(pond);
        resetDialog.setOnPondResetListener(updatedPond -> {
            PondSyncManager.updatePondDetails(updatedPond, new PondSyncManager.Callback() {
                @Override
                public void onSuccess(Object result) {
                    File existingPDF = (updatedPond.getPdfPath() != null && !updatedPond.getPdfPath().isEmpty())
                            ? new File(updatedPond.getPdfPath()) : null;

                    PondSyncManager.savePondHistoryWithPDF(updatedPond, "RESET", existingPDF, new PondSyncManager.Callback() {
                        @Override
                        public void onSuccess(Object result) {
                            int adapterPos = holder.getAdapterPosition();
                            if (adapterPos >= 0 && adapterPos < pondList.size()) {
                                pondList.set(adapterPos, updatedPond);
                                notifyItemChanged(adapterPos);
                            }
                            runOnUiThread(() -> Toast.makeText(context, "Pond reset successfully!", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(context, "History save failed: " + error, Toast.LENGTH_LONG).show());
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(context, "Update failed: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
        resetDialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "ResetPondDialog");
    }

    private void deletePond(ViewHolder holder, PondModel pond) {
        PondSyncManager.fetchPondReport(pond.getId(), new PondSyncManager.Callback() {
            @Override
            public void onSuccess(Object result) {
                try {
                    JSONObject pondReport = (JSONObject) result;
                    File generatedPDF = PondPDFGenerator.generatePDF(context, pondReport, "DELETED");
                    if (generatedPDF == null) throw new Exception("PDF generation failed");

                    PondSyncManager.savePondHistoryWithPDF(pond, "DELETED", generatedPDF, new PondSyncManager.Callback() {
                        @Override
                        public void onSuccess(Object result) {
                            PondSyncManager.deletePond(pond.getId(), new PondSyncManager.Callback() {
                                @Override
                                public void onSuccess(Object result) {
                                    int adapterPos = holder.getAdapterPosition();
                                    if (adapterPos >= 0 && adapterPos < pondList.size()) {
                                        pondList.remove(adapterPos);
                                        notifyItemRemoved(adapterPos);
                                    }
                                    if (context instanceof PondDashboardActivity) {
                                        runOnUiThread(() -> ((PondDashboardActivity) context).loadHistory(pond.getId()));
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> Toast.makeText(context, "Error deleting pond: " + error, Toast.LENGTH_SHORT).show());
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(context, "Error saving history: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(context, "Error fetching pond report: " + error, Toast.LENGTH_SHORT).show());
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
