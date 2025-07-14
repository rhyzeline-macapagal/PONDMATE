package com.example.pondmatev1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UserProfile extends Fragment {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private TextView fullNameText, addressText, userTypeText;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ShapeableImageView imgProfilePhoto;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        Button logoutButton = view.findViewById(R.id.btnLogout);
        logoutButton.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(requireContext(), login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

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

        return view;
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

            // save path to sharedpref
            SharedPreferences prefs = requireContext().getSharedPreferences("user_profile", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("profile_photo_path_" + sessionManager.getUsername(), file.getAbsolutePath());
            editor.apply();


            imgProfilePhoto.setImageURI(Uri.fromFile(file));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // pang load ng pis from internal storage
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
