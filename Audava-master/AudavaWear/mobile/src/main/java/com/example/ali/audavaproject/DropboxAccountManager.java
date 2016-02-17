package com.example.ali.audavaproject;

import android.content.Context;
import android.content.SharedPreferences;

public class DropboxAccountManager {

    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String APP_KEY = "2muzb3gk4xshp43";
    private final static String APP_SECRET = "au54wyjshebq2g8";

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(DROPBOX_NAME, 0);
        return prefs.getString(APP_KEY, null) != null && prefs.getString(APP_SECRET, null) != null;
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(DROPBOX_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(APP_KEY, null);
        editor.putString(APP_SECRET, null);
        editor.apply();
    }
}
