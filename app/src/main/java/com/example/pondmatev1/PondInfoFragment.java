package com.example.pondmatev1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PondInfoFragment extends Fragment {

    private TextView tvPondName, tvBreed, tvFishCount, tvCostPerFish, tvDateStarted, tvHarvestDate;
    private Button btnEdit;

    private PondModel pond;

    public PondInfoFragment() {}

    public static PondInfoFragment newInstance(PondModel pond) {
        PondInfoFragment fragment = new PondInfoFragment();
        Bundle args = new Bundle();
        args.putString("name", pond.getName());
        args.putString("breed", pond.getBreed());
        args.putInt("fish_count", pond.getFishCount());
        args.putDouble("cost_per_fish", pond.getCostPerFish());
        args.putString("date_started", pond.getDateStarted());
        args.putString("date_harvest", pond.getDateHarvest());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pond_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPondName = view.findViewById(R.id.tvPondName);
        tvBreed = view.findViewById(R.id.tvBreed);
        tvFishCount = view.findViewById(R.id.tvFishCount);
        tvCostPerFish = view.findViewById(R.id.tvCostPerFish);
        tvDateStarted = view.findViewById(R.id.tvDateStarted);
        tvHarvestDate = view.findViewById(R.id.tvHarvestDate);
        btnEdit = view.findViewById(R.id.btnEditPond);

        if (getArguments() != null) {
            tvPondName.setText(getArguments().getString("name"));
            tvBreed.setText(getArguments().getString("breed"));
            tvFishCount.setText("Fish Count: " + getArguments().getInt("fish_count"));
            tvCostPerFish.setText("Cost per Fish: ₱" + getArguments().getDouble("cost_per_fish"));
            tvDateStarted.setText("Date Started: " + getArguments().getString("date_started"));
            tvHarvestDate.setText("Harvest Date: " + getArguments().getString("date_harvest"));
        }

        String userType = new SessionManager(requireContext()).getUsertype();
        if ("owner".equalsIgnoreCase(userType)) {
            btnEdit.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE);
        }

        btnEdit.setOnClickListener(v -> {
            // TODO: Add edit logic or open edit dialog
        });
    }
}
