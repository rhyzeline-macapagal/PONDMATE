package com.example.pondmatev1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CaretakerAdapter extends RecyclerView.Adapter<CaretakerAdapter.CaretakerViewHolder> {
    private List<Caretaker> caretakerList;

    public CaretakerAdapter(List<Caretaker> caretakerList) {
        this.caretakerList = caretakerList;
    }

    @NonNull
    @Override
    public CaretakerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.caretaker_item, parent, false);
        return new CaretakerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaretakerViewHolder holder, int position) {
        Caretaker caretaker = caretakerList.get(position);
        holder.txtFullName.setText(caretaker.getFullName());
        holder.txtAddress.setText(caretaker.getAddress());
        holder.txtUsername.setText("Username: " + caretaker.getUsername());
    }

    @Override
    public int getItemCount() {
        return caretakerList.size();
    }

    public static class CaretakerViewHolder extends RecyclerView.ViewHolder {
        TextView txtFullName, txtAddress, txtUsername;

        public CaretakerViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFullName = itemView.findViewById(R.id.txtFullName);
            txtAddress = itemView.findViewById(R.id.txtAddress);
            txtUsername = itemView.findViewById(R.id.txtUsername);
        }
    }
}

