package com.example.pondmatev1;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.pondmatev1.ControlsFeeder;
import com.example.pondmatev1.ScheduleFeeder;


public class FeederAdapter extends FragmentStateAdapter {
    public FeederAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new ControlsFeeder();
        } else if (position == 1) {
            return new ScheduleFeeder();
        } else {
            // fallback in case position is invalid
            return new ControlsFeeder();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
