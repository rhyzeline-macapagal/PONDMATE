package com.example.pondmatev1;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UserProfileDialogFragment extends DialogFragment {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private TextView fullNameText, addressText, userTypeText;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ShapeableImageView imgProfilePhoto;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
                        dismiss(); // close the dialog
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        TextView closeCaretaker = view.findViewById(R.id.closeCaretaker);
        closeCaretaker.setOnClickListener(v -> dismiss());

        imgProfilePhoto = view.findViewById(R.id.imgProfilePhoto);
        Button changePhotoButton = view.findViewById(R.id.btnChangePhoto);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            saveProfileImage(selectedImage);
                        }
                    }
                }
        );

        changePhotoButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        dbHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());

        fullNameText = view.findViewById(R.id.txtFullNameValue);
        addressText = view.findViewById(R.id.txtAddressValue);
        userTypeText = view.findViewById(R.id.txtPositionValue);

        String username = sessionManager.getUsername();
        if (username != null) {
            Cursor cursor = dbHelper.getUserInfo(username);
            if (cursor != null && cursor.moveToFirst()) {
                fullNameText.setText(cursor.getString(cursor.getColumnIndexOrThrow("fullname")));
                addressText.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
                userTypeText.setText(cursor.getString(cursor.getColumnIndexOrThrow("usertype")));
                cursor.close();
            }
        }

        loadSavedProfilePhoto();
    }

    private void saveProfileImage(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            File file = new File(requireContext().getFilesDir(), "profile_" + sessionManager.getUsername() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            SharedPreferences prefs = requireContext().getSharedPreferences("user_profile", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("profile_photo_path_" + sessionManager.getUsername(), file.getAbsolutePath());
            editor.apply();

            imgProfilePhoto.setImageURI(Uri.fromFile(file));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSavedProfilePhoto() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_profile", Activity.MODE_PRIVATE);
        String path = prefs.getString("profile_photo_path_" + sessionManager.getUsername(), null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                imgProfilePhoto.setImageURI(Uri.fromFile(file));
            }
        }
    }
    
}
