package com.example.pondmatev1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ArrayList<NotificationModel> notifList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerView = findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Close button
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());

        loadNotifications();

        // Swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NotificationModel removed = notifList.get(position);
                notifList.remove(position);
                adapter.notifyItemRemoved(position);

                // Remove from SharedPreferences
                SharedPreferences prefs = getSharedPreferences("notifications", MODE_PRIVATE);
                Set<String> notifications = new HashSet<>(prefs.getStringSet("notif_list", new HashSet<>()));
                notifications.remove(removed.toString());
                prefs.edit().putStringSet("notif_list", notifications).apply();
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void loadNotifications() {
        SharedPreferences prefs = getSharedPreferences("notifications", MODE_PRIVATE);
        Set<String> notifications = prefs.getStringSet("notif_list", new HashSet<>());

        ArrayList<NotificationModel> allNotifications = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);

        for (String notif : notifications) {
            // Calendar activity format: PondName|ActivityName|Date
            if (notif.contains("|")) {
                String[] parts = notif.split("\\|");
                if (parts.length == 3) {
                    String pondName = parts[0];
                    String title = parts[1];
                    String date = parts[2];
                    allNotifications.add(new NotificationModel(pondName, title, date));
                    continue;
                }
            }

            // Otherwise, parse Title - Message (Date)
            int dateStart = notif.lastIndexOf('(');
            int dateEnd = notif.lastIndexOf(')');
            if (dateStart != -1 && dateEnd != -1) {
                String titleAndMessage = notif.substring(0, dateStart - 1);
                String dateStr = notif.substring(dateStart + 1, dateEnd);
                String[] parts = titleAndMessage.split(" - ", 2);
                String title = parts.length > 0 ? parts[0] : "";
                String pondName = parts.length > 1 ? parts[1] : "";
                allNotifications.add(new NotificationModel(pondName, title, dateStr));
            }
        }

        // Sort by date descending
        Collections.sort(allNotifications, (a, b) -> {
            try {
                Date d1 = sdf.parse(a.date);
                Date d2 = sdf.parse(b.date);
                return d2.compareTo(d1);
            } catch (ParseException e) {
                return b.date.compareTo(a.date);
            }
        });

        notifList = new ArrayList<>();
        boolean firstNewAdded = false;

        for (int i = 0; i < allNotifications.size(); i++) {
            NotificationModel n = allNotifications.get(i);
            if (!firstNewAdded) {
                notifList.add(new NotificationModel("HEADER", "New Notifications", ""));
                firstNewAdded = true;
            } else if (i == 3) {
                notifList.add(new NotificationModel("HEADER", "Earlier Notifications", ""));
            }
            notifList.add(n);
        }

        adapter = new NotificationAdapter(notifList);
        recyclerView.setAdapter(adapter);
    }

    // ===========================
    // Model Class
    // ===========================
    private static class NotificationModel {
        String pondName;
        String title;
        String date;

        NotificationModel(String pondName, String title, String date) {
            this.pondName = pondName;
            this.title = title;
            this.date = date;
        }

        @Override
        public String toString() {
            return title + " - " + pondName + " (" + date + ")";
        }
    }

    // ===========================
    // Adapter Class
    // ===========================
    private class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final ArrayList<NotificationModel> notifications;
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        NotificationAdapter(ArrayList<NotificationModel> notifications) {
            this.notifications = notifications;
        }

        @Override
        public int getItemViewType(int position) {
            return notifications.get(position).pondName.equals("HEADER") ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_notification_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_notification, parent, false);
                return new ItemViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            NotificationModel notif = notifications.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).headerText.setText(notif.title);
            } else {
                ItemViewHolder itemHolder = (ItemViewHolder) holder;
                itemHolder.tvPondName.setText(notif.pondName);
                itemHolder.tvTitle.setText(notif.title);
                itemHolder.tvDate.setText(notif.date);

                itemHolder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent();
                    intent.putExtra("selected_date", notif.date);
                    intent.putExtra("pond_name", notif.pondName);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView headerText;

            HeaderViewHolder(View itemView) {
                super(itemView);
                headerText = itemView.findViewById(R.id.tvSectionHeader);
            }
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView tvPondName, tvTitle, tvDate;

            ItemViewHolder(View itemView) {
                super(itemView);
                tvPondName = itemView.findViewById(R.id.tvNotificationPondName);
                tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
                tvDate = itemView.findViewById(R.id.tvNotificationDate);
            }
        }
    }
}
