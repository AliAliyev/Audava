package com.example.ali.audavaproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivitySettings extends ActionBarActivity {

    private static final String SETTING_PREF = "SettingPref";
    private static final String RECORD_PREF = "RecordPref";
    private static final String DROPBOX_UPLOAD_PREF = "DropboxUpload";
    private static final String DROPBOX_ACCOUNT_PREF = "DropboxAccount";
    private static final String SAMPLE_RATE_PREF = "SampleRate";
    private static final String[] SAMPLE_RATE_CHOICES = {"8kHz (Phone Quality)",
            "22kHz (FM radio Quality)", "44.1kHz(Best Quality)" };

    private SharedPreferences preferences;
    private CheckBox recordCheckbox, dropboxCheckbox;
    private Button dropboxButton;
    private TextView samplerateButton, sampleInfo;
    private LinearLayout dropboxLayout;
    private String dropboxText;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_page);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        recordCheckbox = (CheckBox) findViewById(R.id.RecordBox);
        dropboxCheckbox = (CheckBox) findViewById(R.id.dropboxCheckbox);
        dropboxButton = (Button) findViewById(R.id.dropboxSignin);
        dropboxLayout = (LinearLayout) findViewById(R.id.dropboxLayout);
        samplerateButton = (TextView) findViewById(R.id.sampleRatebtn);
        sampleInfo = (TextView) findViewById(R.id.sampleInfo);

        LoadPreferences();

        samplerateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleRateDialog();
            }
        });

        recordCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavePreferences(RECORD_PREF, recordCheckbox.isChecked());
            }
        });


        dropboxCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int value;
                if (dropboxCheckbox.isChecked()) {
                    value = 1;
                } else {
                    value = 0;
                }

                SavePreferences(DROPBOX_UPLOAD_PREF, value);
            }
        });

        dropboxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dropboxText.equals("Sign out")) {
                    confirmDialog();
                } else if (dropboxText.equals("Sign in") || dropboxText == null) {
                    startActivity(new Intent(getApplicationContext(), ActivityAccountLogin.class));
                }
            }
        });
    }

    private void sampleRateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sample rate (quality)");
        builder.setSingleChoiceItems(SAMPLE_RATE_CHOICES, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int sample = 44100;
                        Toast.makeText(ActivitySettings.this, SAMPLE_RATE_CHOICES[which] +
                                        " Selected", Toast.LENGTH_LONG).show();
                        if(which==0) { sample = 8000; }
                        else if(which==1) { sample = 22050; }
                        else if(which==2) { sample = 44100; }
                        SavePreferences(SAMPLE_RATE_PREF, sample);
                        LoadPreferences();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void LoadPreferences() {
        preferences = this.getSharedPreferences(SETTING_PREF, 0);
        boolean recordBoolean = preferences.getBoolean(RECORD_PREF, false);
        dropboxText = preferences.getString(DROPBOX_ACCOUNT_PREF, "Sign in");
        int dropboxValue = preferences.getInt(DROPBOX_UPLOAD_PREF, 0);
        int sampleRateText = preferences.getInt(SAMPLE_RATE_PREF, 44100);

        if(recordBoolean) {
            recordCheckbox.setChecked(true);
        } else {
            recordCheckbox.setChecked(false);
        }

        if(dropboxText.equals("Sign out")) {
            dropboxButton.setText(dropboxText);
            dropboxLayout.setVisibility(View.VISIBLE);
            if (dropboxValue == 1) {
                dropboxCheckbox.setChecked(true);
            } else {
                dropboxCheckbox.setChecked(false);
            }
        } else if(dropboxText.equals("Sign in")) {
            dropboxLayout.setVisibility(View.GONE);
            dropboxButton.setText("Sign in");
        }

        if(sampleRateText == 8000) {
            samplerateButton.setText("Sample rate: 8kHz (Phone Quality)");
            sampleInfo.setText("WARNING: Terrible sound quality! Only choose this when low on " +
                    "memory! \nFile size : 16KB/second");
        } else if(sampleRateText == 22050) {
            samplerateButton.setText("Sample rate: 22kHz (FM radio Quality)");
            sampleInfo.setText("Acceptable sound quality\nFile size : 44KB/second");
        } else if(sampleRateText == 44100) {
            samplerateButton.setText("Sample rate: 44.1kHz (Best Quality)");
            sampleInfo.setText("Best sound quality\nWarning: Larger file size! (88.2KB/second)");
        }
    }

    private void SavePreferences(String name, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    private void SavePreferences(String name, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(name, value);
        editor.apply();
    }

    private void SavePreferences(String name, String value){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(name, value);
        editor.apply();
    }

    private void confirmDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Sign out of Dropbox?");
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SavePreferences(DROPBOX_ACCOUNT_PREF, "Sign in");
                DropboxAccountManager.logout(ActivitySettings.this);
                dropboxLayout.setVisibility(View.GONE);
                LoadPreferences();
            }
        });
        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogConfirm = dialogBuilder.create();
        dialogConfirm.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setting_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
            case R.id.action_settings:
                LoadPreferences();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        if(DropboxAccountManager.isLoggedIn(this)) {
            SavePreferences(DROPBOX_ACCOUNT_PREF, "Sign out");
        }
        super.onResume();
    }

    @Override
    protected void onRestart() {
        LoadPreferences();
        super.onRestart();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            LoadPreferences();
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
