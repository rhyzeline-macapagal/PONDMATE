package com.example.pondmatev1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedLogAdapter extends RecyclerView.Adapter<FeedLogAdapter.FeedLogViewHolder> {

    private final Context context;
    private final List<String> logList;

    public FeedLogAdapter(Context context, List<String> logList) {
        this.context = context;
        this.logList = logList;
    }

    @Override
    public FeedLogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feed_log, parent, false);
        return new FeedLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FeedLogViewHolder holder, int position) {
        String log = logList.get(position);
        holder.logText.setText(log);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    static class FeedLogViewHolder extends RecyclerView.ViewHolder {
        TextView logText;

        public FeedLogViewHolder(View itemView) {
            super(itemView);
            logText = itemView.findViewById(R.id.logText);
        }
    }
}
