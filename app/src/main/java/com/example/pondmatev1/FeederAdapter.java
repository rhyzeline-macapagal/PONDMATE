package com.example.pondmatev1;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;



public class FeederAdapter extends FragmentStateAdapter {
    public FeederAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new ScheduleFeeder();
        } else if (position == 1) {
            return new ControlsFeeder();
        } else {
            // fallback in case position is invalid
            return new ScheduleFeeder();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
