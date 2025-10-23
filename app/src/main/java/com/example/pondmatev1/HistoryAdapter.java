package com.example.pondmatev1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<HistoryModel> historyList;

    public HistoryAdapter(Context context, ArrayList<HistoryModel> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryModel history = historyList.get(position);

        holder.tvHistoryName.setText(history.getPondName());
        holder.tvHistoryDate.setText(history.getDate());

        if (history.getPdfPath() != null && !history.getPdfPath().isEmpty()) {
            holder.btnViewPdf.setVisibility(View.VISIBLE);
            holder.btnViewPdf.setOnClickListener(v -> {
                try {
                    String pdfUrl = "https://pondmate.alwaysdata.net/" + history.getPdfPath();
                    Intent intent = new Intent(context, PdfPreviewActivity.class);
                    intent.putExtra(PdfPreviewActivity.EXTRA_PDF_URL, pdfUrl); // send URL to activity
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Unable to open PDF", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.btnViewPdf.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryName, tvHistoryDate;
        Button btnViewPdf;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryName = itemView.findViewById(R.id.tvHistoryName);
            tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
            btnViewPdf = itemView.findViewById(R.id.btnViewPdf);
        }
    }
}
