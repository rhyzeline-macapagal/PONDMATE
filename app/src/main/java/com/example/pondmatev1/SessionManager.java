package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_USERID = "loggedInUserId";
    private static final String KEY_USERNAME = "loggedInUsername";
    private static final String KEY_USERTYPE = "loggedInUsertype";
    private static final String KEY_FULLNAME = "loggedInFullname";
    private static final String KEY_ADDRESS = "loggedInAddress";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUserId(String userId) {
        editor.putString(KEY_USERID, userId);
        editor.apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USERID, null);
    }

    // --- Username ---
    public void saveUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    // --- Usertype ---
    public void saveUsertype(String usertype) {
        editor.putString(KEY_USERTYPE, usertype);
        editor.apply();
    }

    public String getUsertype() {
        return prefs.getString(KEY_USERTYPE, null);
    }

    // --- Full Name ---
    public void saveFullName(String fullName) {
        editor.putString(KEY_FULLNAME, fullName);
        editor.apply();
    }

    public String getFullName() {
        return prefs.getString(KEY_FULLNAME, "");
    }

    // --- Address ---
    public void saveAddress(String address) {
        editor.putString(KEY_ADDRESS, address);
        editor.apply();
    }

    public String getAddress() {
        return prefs.getString(KEY_ADDRESS, "");
    }

    // --- Clear session ---
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
