package com.example.pondmatev1;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelectableCaretakerAdapter extends RecyclerView.Adapter<SelectableCaretakerAdapter.ViewHolder> {
    Context context;
    ArrayList<SelectableCaretaker> list;
    private static final String TAG = "SelectableCaretakerAdapter";


    // MAIN constructor
    public SelectableCaretakerAdapter(Context context, ArrayList<SelectableCaretaker> list) {
        this.context = context;
        this.list = list;
    }

    // FIXED constructor that converts raw data into SelectableCaretaker list
    public SelectableCaretakerAdapter(
            Context context,
            List<String> caretakerNames,
            List<String> caretakerIds,
            Set<String> selectedCaretakerIds,
            List<Boolean> caretakerCheckedState
    ) {
        this.context = context;
        this.list = new ArrayList<>();
        for (int i = 0; i < caretakerNames.size(); i++) {
            String name = caretakerNames.get(i);
            String id = caretakerIds.get(i);
            boolean isSelected = (caretakerCheckedState != null && i < caretakerCheckedState.size()) ? caretakerCheckedState.get(i) : false;
            this.list.add(new SelectableCaretaker(id, name, isSelected));
        }
        Log.d(TAG, "[SelectableCaretakerAdapter] Initialized with list: " + this.list);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_caretakers, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectableCaretaker model = list.get(position);
        holder.tvName.setText(model.getName());
        holder.checkBox.setChecked(model.isSelected());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            model.setSelected(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkCaretaker);
            tvName = itemView.findViewById(R.id.tvCaretakerName);
        }
    }

    public ArrayList<String> getSelectedIds() {
        ArrayList<String> selectedIds = new ArrayList<>();
        for (SelectableCaretaker c : list) {
            if (c.isSelected()) selectedIds.add(c.getId());
        }
        return selectedIds;
    }
}
