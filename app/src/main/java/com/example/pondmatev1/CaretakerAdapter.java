package com.example.pondmatev1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CaretakerAdapter extends RecyclerView.Adapter<CaretakerAdapter.ViewHolder> {
    Context context;
    ArrayList<CaretakerModel> list;
    OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEdit(CaretakerModel model);
        void onDelete(CaretakerModel model);
    }

    public CaretakerAdapter(Context context, ArrayList<CaretakerModel> list, OnItemClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_caretaker, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CaretakerModel model = list.get(position);
        holder.username.setText(model.getUsername());

        // ✅ Mask password
        if (model.getPassword() != null) {
            String hiddenPassword = new String(new char[model.getPassword().length()]).replace("\0", "*");
            holder.password.setText(hiddenPassword);
        } else {
            holder.password.setText("********");
        }

        holder.fullname.setText(model.getFullname());
        holder.address.setText(model.getAddress());
        holder.salary.setText("₱" + model.getSalary());

        holder.edit.setOnClickListener(v -> listener.onEdit(model));
        holder.delete.setOnClickListener(v -> listener.onDelete(model));
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof CaretakerDashboardActivity) {
                ((CaretakerDashboardActivity) context).showEditDialog(model);
            }
        });
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView username, password, fullname, address, salary;
        ImageView edit, delete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.tvUsername);
            password = itemView.findViewById(R.id.tvPassword);
            fullname = itemView.findViewById(R.id.tvFullname);
            address = itemView.findViewById(R.id.tvAddress);
            salary = itemView.findViewById(R.id.tvSalary);
            edit = itemView.findViewById(R.id.btnEdit);
            delete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

