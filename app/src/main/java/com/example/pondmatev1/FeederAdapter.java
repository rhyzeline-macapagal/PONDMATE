package com.example.pondmatev1;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FeederAdapter extends FragmentStateAdapter {

    private final String pondId;

    public FeederAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, String pondId) {
        super(fragmentManager, lifecycle);
        this.pondId = pondId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = (position == 0) ? new ScheduleFeeder() : new ControlsFeeder();
        Bundle b = new Bundle();
        b.putString("pond_id", pondId);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}