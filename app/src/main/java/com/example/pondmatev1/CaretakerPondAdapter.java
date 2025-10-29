package com.example.pondmatev1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CaretakerPondAdapter extends RecyclerView.Adapter<CaretakerPondAdapter.ViewHolder> {
    private List<PondModel> pondList;
    private List<PondModel> ponds;


    public CaretakerPondAdapter(List<PondModel> pondList) {
        this.pondList = pondList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbPond;
        public ViewHolder(View view) {
            super(view);
            cbPond = view.findViewById(R.id.cbPond);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pond_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PondModel pond = pondList.get(position);

        // Prevent unwanted triggers when recycling rows
        holder.cbPond.setOnCheckedChangeListener(null);

        holder.cbPond.setText(pond.getName());
        holder.cbPond.setChecked(pond.isAssigned());

        // Save selection state
        holder.cbPond.setOnCheckedChangeListener((btn, checked) -> pond.setAssigned(checked));
    }

    @Override
    public int getItemCount() { return pondList.size(); }

    // ✅ Return selected pond IDs for saving to DB
    public List<PondModel> getSelectedPonds() {
        List<PondModel> selected = new ArrayList<>();
        for (PondModel p : ponds) {
            if (p.isAssigned()) {
                selected.add(p);
            }
        }
        return selected;
    }

}
