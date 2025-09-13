package com.example.pondmatev1;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class UserProfileDialogFragment extends DialogFragment {

    private SessionManager sessionManager;

    private TextView fullNameText, addressText, userTypeText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.CENTER);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        sessionManager = new SessionManager(requireContext());

        Button logoutButton = view.findViewById(R.id.btnLogout);
        logoutButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        sessionManager.clearSession();
                        Intent intent = new Intent(requireContext(), login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        TextView closeCaretaker = view.findViewById(R.id.closeCaretaker);
        closeCaretaker.setOnClickListener(v -> dismiss());

        // Profile info textviews
        fullNameText = view.findViewById(R.id.txtFullNameValue);
        addressText = view.findViewById(R.id.txtAddressValue);
        userTypeText = view.findViewById(R.id.txtPositionValue);

        // Load user info
        fullNameText.setText(sessionManager.getFullName());
        addressText.setText(sessionManager.getAddress());
        userTypeText.setText(sessionManager.getUsertype());
    }
}
