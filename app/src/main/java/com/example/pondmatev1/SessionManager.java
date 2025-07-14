package com.example.pondmatev1;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_USERNAME = "loggedInUsername";
    private static final String KEY_USERTYPE = "loggedInUsertype";  // 👈 Add this line

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    // 👇 Add these two methods for usertype
    public void saveUsertype(String usertype) {
        editor.putString(KEY_USERTYPE, usertype);
        editor.apply();
    }

    public String getUsertype() {
        return prefs.getString(KEY_USERTYPE, null);
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
