package com.example.pondmatev1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

        initializeViews(view);
        setupViewPager();
        setupTabLayout();
        addAnimations();

        return view;
    }

    private void initializeViews(View view) {
        tabLayout = view.findViewById(R.id.tablayout_feeders);
        viewPager = view.findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        String pondId = getArguments() != null ? getArguments().getString("pond_id") : null;
        if (pondId == null) {
            Log.e("FEED_TEST", "FeederFragment: pondId is NULL!");
        } else {
            Log.d("FEED_TEST", "FeederFragment received pondId = " + pondId);
        }

        adapter = new FeederAdapter(getChildFragmentManager(), getLifecycle(), pondId);
        viewPager.setAdapter(adapter);
    }


    // Updated TabLayoutMediator setup without icons
    private void setupTabLayout() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Schedule");
                    break;
                case 1:
                    tab.setText("Controls");
                    break;
                default:
                    tab.setText("Tab " + (position + 1));
                    break;
            }
        }).attach();

        // Add tab selection listener for additional effects
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.view != null) {
                    tab.view.animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .setDuration(150)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.view != null) {
                    tab.view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Handle tab reselection if needed
            }
        });
    }

    private void addAnimations() {
        // Add entrance animation
        if (getView() != null) {
            getView().setAlpha(0f);
            getView().animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Add any refresh logic here
    }
}