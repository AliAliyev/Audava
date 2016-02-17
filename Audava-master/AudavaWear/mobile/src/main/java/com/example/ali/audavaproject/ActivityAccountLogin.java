package com.example.ali.audavaproject;

import android.content.SharedPreferences;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;

public class ActivityAccountLogin extends ActionBarActivity {

    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String APP_KEY = "2muzb3gk4xshp43";
    private final static String APP_SECRET = "au54wyjshebq2g8";

    private boolean dropboxAuthenticationTry;
    private DropboxAPI<AndroidAuthSession> dropbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dropboxAuthenticationTry = false;
        dropboxAuthentication();
    }

    private void dropboxAuthentication() {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        session = new AndroidAuthSession(appKeys);
        dropbox = new DropboxAPI<>(session);
        dropbox.getSession().startAuthentication(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dropboxAuthenticationTry = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(dropboxAuthenticationTry) {
            AndroidAuthSession session = dropbox.getSession();
            if (session.authenticationSuccessful()) {
                try {
                    // Required to complete auth, sets the access token on the session
                    session.finishAuthentication();
                    TokenPair tokens = session.getAccessTokenPair();
                    SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(APP_KEY, tokens.key);
                    editor.putString(APP_SECRET, tokens.secret);
                    editor.apply();
                    Toast.makeText(getApplicationContext(), "Authentication successful",
                            Toast.LENGTH_LONG).show();
                } catch (IllegalStateException e) {
                    Log.i("DbAuthLog", "Error authenticating", e);
                    Toast.makeText(getApplicationContext(), "Error during Dropbox authentication",
                            Toast.LENGTH_SHORT).show();
                }
            }
            dropboxAuthenticationTry = false;
            NavUtils.navigateUpFromSameTask(this);
        }
    }
}
