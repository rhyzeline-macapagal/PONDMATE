package com.example.pondmatev1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class login extends AppCompatActivity {

    EditText userName, passWord;
    Button loginButton;
    CheckBox rememberMeCheckBox;

    AlertDialog loadingDialog;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (!isInternetAvailable()) {
            showNoInternetDialog();
            return;
        }

        userName = findViewById(R.id.username_input);
        passWord = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        rememberMeCheckBox = findViewById(R.id.checkBox);

        // Password toggle
        final Drawable eyeOpen = ContextCompat.getDrawable(this, R.drawable.eye_open);
        final Drawable eyeClosed = ContextCompat.getDrawable(this, R.drawable.hidden);
        final Drawable lockIcon = ContextCompat.getDrawable(this, R.drawable.lock_icon);
        final boolean[] isVisible = {false};
        setPasswordEyeIcon(passWord, lockIcon, eyeClosed);

        passWord.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = passWord.getCompoundDrawables()[2];
                if (drawableEnd != null) {
                    int drawableWidth = drawableEnd.getBounds().width();
                    int clickAreaStart = passWord.getWidth() - passWord.getPaddingEnd() - drawableWidth;
                    if (event.getX() >= clickAreaStart) {
                        isVisible[0] = !isVisible[0];
                        int cursorPosition = passWord.getSelectionStart();
                        passWord.setInputType(isVisible[0] ?
                                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        setPasswordEyeIcon(passWord, lockIcon, isVisible[0] ? eyeOpen : eyeClosed);
                        passWord.setSelection(cursorPosition);
                        return true;
                    }
                }
            }
            return false;
        });

        passWord.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setPasswordEyeIcon(passWord, lockIcon, isVisible[0] ? eyeOpen : eyeClosed);
            }
            public void afterTextChanged(Editable s) {}
        });

        loadPreferences();

        loginButton.setOnClickListener(v -> {
            String username = userName.getText().toString().trim();
            String password = passWord.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            loginButton.setEnabled(false);
            loginOnline(username, password);
        });
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null); // your custom layout

        ImageView fishLoader = dialogView.findViewById(R.id.fishLoader); // <-- make sure this id exists in dialog_loading.xml
        TextView loadingText = dialogView.findViewById(R.id.loadingText);

        // Change the text dynamically
        loadingText.setText("Logging in...");

        // Apply animation
        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        if (fishLoader != null) {   // <-- prevent null crash
            fishLoader.startAnimation(rotate);
        }

        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.show();
    }


    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }


    private void loginOnline(String username, String password) {
        showLoadingDialog();
        new Thread(() -> {
            try {
                URL url = new URL("https://pondmate.alwaysdata.net/login_user.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "username=" + URLEncoder.encode(username, "UTF-8") +
                        "&password=" + URLEncoder.encode(password, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
                }
                in.close();

                String response = responseBuilder.toString();

                runOnUiThread(() -> {
                    hideLoadingDialog();
                    try {
                        JSONObject responseJson = new JSONObject(response);
                        String status = responseJson.getString("status");

                        if ("success".equals(status)) {
                            String returnedUsername = responseJson.getString("username");
                            String returnedUsertype = responseJson.getString("usertype");

                            SessionManager session = new SessionManager(this);
                            session.saveUsername(returnedUsername);
                            session.saveUsertype(returnedUsertype);

                            if (rememberMeCheckBox.isChecked()) {
                                savePreferences(returnedUsername, password);
                            } else {
                                clearPreferences();
                            }

                            startActivity(new Intent(this, PondDashboardActivity.class));
                            overridePendingTransition(R.anim.zoom_in, R.anim.fade_out);
                            finish();

                        } else {
                            Toast.makeText(this, "❌ Invalid credentials", Toast.LENGTH_SHORT).show();
                            loginButton.setEnabled(true);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "⚠️ Error parsing response", Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "⚠️ Network error", Toast.LENGTH_SHORT).show();
                    loginButton.setEnabled(true);
                });
            }
        }).start();
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNet = cm.getActiveNetworkInfo();
            return activeNet != null && activeNet.isConnected();
        }
        return false;
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("You need to connect to the internet to log in.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void savePreferences(String username, String password) {
        SharedPreferences sp = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putBoolean("rememberMe", true);
        editor.apply();
    }

    private void clearPreferences() {
        SharedPreferences sp = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    private void loadPreferences() {
        SharedPreferences sp = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (sp.getBoolean("rememberMe", false)) {
            userName.setText(sp.getString("username", ""));
            passWord.setText(sp.getString("password", ""));
            rememberMeCheckBox.setChecked(true);
        }
    }

    private void setPasswordEyeIcon(EditText editText, Drawable start, Drawable end) {
        editText.setCompoundDrawablesWithIntrinsicBounds(start, null, end, null);
    }
}
