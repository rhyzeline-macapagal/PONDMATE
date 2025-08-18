package com.example.pondmatev1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class PondAdapter extends RecyclerView.Adapter<PondAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<PondModel> pondList;
    private final String userType;

    public PondAdapter(Context context, ArrayList<PondModel> pondList, String userType) {
        this.context = context;
        this.pondList = pondList;
        this.userType = userType;
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

        // Handle Add Button card
        if ("ADD_BUTTON".equals(pond.getMode())) {
            if ("owner".equalsIgnoreCase(userType)) {
                holder.pondImage.setImageResource(R.drawable.ic_addpond);
                holder.pondName.setVisibility(View.GONE);
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

                holder.itemView.setOnClickListener(v -> {
                    if (context instanceof AppCompatActivity) {
                        AddPondDialogFragment dialog = new AddPondDialogFragment();
                        dialog.setOnPondAddedListener(newPond -> {
                            pondList.add(1, newPond); // Insert below button
                            notifyItemInserted(1);
                        });
                        dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "AddPondDialog");
                    }
                });
            } else {
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            }
            return;
        }

        // Load pond image directly from full URL
        if (pond.getImagePath() != null && !pond.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(pond.getImagePath()) // Already full public URL from PHP
                    .placeholder(R.drawable.pond) // While loading
                    .error(R.drawable.pond)       // On error
                    .into(holder.pondImage);
        } else {
            holder.pondImage.setImageResource(R.drawable.pond);
        }

        // Set pond name
        holder.pondName.setText(pond.getName());
        holder.pondName.setVisibility(View.VISIBLE);
        holder.itemView.setVisibility(View.VISIBLE);
        holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Item click to open MainActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
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

    @Override
    public int getItemCount() {
        return pondList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView pondImage;
        TextView pondName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            pondImage = itemView.findViewById(R.id.pondImage);
            pondName = itemView.findViewById(R.id.pondName);
        }
    }
}
