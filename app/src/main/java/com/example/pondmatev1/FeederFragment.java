package com.example.pondmatev1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class FeederFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FeederAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feeder, container, false);

        tabLayout = view.findViewById(R.id.tablayout_feeders);
        viewPager = view.findViewById(R.id.viewPager);

        adapter = new FeederAdapter(getChildFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);


        // Link ViewPager2 with TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Controls");
            } else if (position == 1) {
                tab.setText("Schedule Feeder");
            }
        }).attach();

        return view;
    }
}
