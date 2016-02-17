package com.example.ali.audavaproject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ActivityTracksListView extends ActionBarActivity {

    private final static String DROPBOX_PREF = "dropbox_prefs";
    private final static String APP_KEY = "2muzb3gk4xshp43";
    private final static String APP_SECRET = "au54wyjshebq2g8";

    private DataManager data;
    private TrackItem selected;
    private String albumChoice;

    private MediaPlayer mediaPlayer;
    private Handler myHandler = new Handler();
    private int progressValue;

    private TextView track_text, current_duration, track_duration;
    private ImageButton play_button, pause_button, stop_button;
    private SeekBar trackSeekBar;

    private DropboxAPI<AndroidAuthSession> dropbox;
    private boolean dropboxAuthenticationTry;

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
        setContentView(R.layout.tracks_list_view);

        data = new DataManager();
        setTitle(data.getCurrentAlbum().getName());
        dropboxAuthenticationTry = false;

        track_text = (TextView) findViewById(R.id.track_name);
        play_button = (ImageButton) findViewById(R.id.imageButton);
        pause_button = (ImageButton) findViewById(R.id.imageButton2);
        stop_button = (ImageButton) findViewById(R.id.imageButton3);
        trackSeekBar = (SeekBar) findViewById(R.id.track_seekbar);
        current_duration = (TextView) findViewById(R.id.current_track_duration);
        track_duration = (TextView) findViewById(R.id.total_track_duration);
        trackSeekBar.setFocusableInTouchMode(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        data.removeDeletedTracks(data.getCurrentAlbum());
        populateListView();
    }

    private void populateListView() {
        ArrayAdapter<TrackItem> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.tracklistView);
        list.setAdapter(adapter);
        registerForContextMenu(list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_default, menu);
        return true;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        menu.add(0, v.getId(), 0, "Move");
        menu.add(0, v.getId(), 0, "Rename");
        menu.add(0, v.getId(), 0, "Set Description");
        menu.add(0, v.getId(), 0, "Upload Track to Dropbox");
        menu.add(0, v.getId(), 0, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle() == "Move") {
            moveDialog();
        }
        if(item.getTitle() == "Rename") {
            renameDialog();
        }
        if(item.getTitle() == "Set Description") {
            descriptionDialog();
        }
        if(item.getTitle() == "Upload Track to Dropbox") {
            Toast.makeText(ActivityTracksListView.this, "Upload " + selected.getTrackName() +
                            " to Dropbox", Toast.LENGTH_SHORT).show();
            if(dropboxAuthentication()) {
                uploadToDropbox();
            }
        }
        if(item.getTitle() == "Delete") {
            deleteDialog();
        }
        return false;
    }

    private boolean dropboxAuthentication() {
        dropboxAuthenticationTry = true;
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        SharedPreferences prefs = getSharedPreferences(DROPBOX_PREF, 0);
        String key = prefs.getString(APP_KEY, null);
        String secret = prefs.getString(APP_SECRET, null);

        if (key != null && secret != null) {
            AccessTokenPair token = new AccessTokenPair(key, secret);
            session = new AndroidAuthSession(appKeys, token);
            dropbox = new DropboxAPI<>(session);
            return true;

        } else {
            session = new AndroidAuthSession(appKeys);
            dropbox = new DropboxAPI<>(session);
            dropbox.getSession().startAuthentication(ActivityTracksListView.this);
            return false;
        }
    }

    private void uploadToDropbox() {
        Toast.makeText(ActivityTracksListView.this, "Uploading " + selected.getTrackName() +
                        " to Dropbox", Toast.LENGTH_SHORT).show();
        UploadFileToDropbox upload = new UploadFileToDropbox(ActivityTracksListView.this, dropbox,
                selected.getAlbumName() + "/" + selected.getTrackName() + ".wav",
                selected.getTrackPath());
        upload.execute();
    }

     @Override
    protected void onResume() {
         super.onResume();
         //Toast.makeText(ActivityTracksListView.this, "After onResume", Toast.LENGTH_SHORT).show();
         if(dropboxAuthenticationTry) {
             AndroidAuthSession session = dropbox.getSession();
             if (session.authenticationSuccessful()) {
                 try {
                     // Required to complete auth, sets the access token on the session
                     session.finishAuthentication();
                     TokenPair tokens = session.getAccessTokenPair();
                     //String accessToken = dropbox.getSession().getOAuth2AccessToken();
                     SharedPreferences prefs = getSharedPreferences(DROPBOX_PREF, 0);
                     SharedPreferences.Editor editor = prefs.edit();
                     editor.putString(APP_KEY, tokens.key);
                     editor.putString(APP_SECRET, tokens.secret);
                     editor.apply();
                     Toast.makeText(ActivityTracksListView.this, "Authentication successful",
                             Toast.LENGTH_LONG).show();
                     //dropboxLoggedIn = true;
                     uploadToDropbox();
                 } catch (IllegalStateException e) {
                     Log.i("DbAuthLog", "Error authenticating", e);
                     Toast.makeText(getApplicationContext(), "Error during Dropbox authentication",
                             Toast.LENGTH_SHORT).show();
                 }
             }
             dropboxAuthenticationTry = false;
         }
    }

    private void moveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Move to..");

        final Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data.getListAlbumNames());
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0,
                                       View arg1, int position, long arg3) {
                albumChoice = spinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }
        });

        builder.setView(spinner);

        builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                albumChoice = null;
            }
        });
        builder.setNegativeButton("Move", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                data.moveTrack(selected, albumChoice);
                populateListView();
                Toast.makeText(ActivityTracksListView.this, "Track moved to " + albumChoice,
                        Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void renameDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final EditText textInput = new EditText(this);
        textInput.setText(selected.getTrackName());
        textInput.setSelectAllOnFocus(true);

        textInput.requestFocus();
        textInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(textInput, 0);
            }
        }, 100);

        dialogBuilder.setTitle("Rename track");
        dialogBuilder.setView(textInput);
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String rename = textInput.getText().toString();

                File file = new File(Environment.getExternalStorageDirectory() + "/Audava/" +
                        data.getCurrentAlbum().getName() + "/" + rename + ".wav");
                if(!file.exists()) {
                    data.renameTrack(selected, rename);
                    Toast.makeText(ActivityTracksListView.this, "Name changed to " + rename,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ActivityTracksListView.this, "File with the same name exists",
                            Toast.LENGTH_SHORT).show();
                }
                populateListView();
            }
        });
        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogRename = dialogBuilder.create();
        dialogRename.show();
    }

    private void descriptionDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final EditText textInput = new EditText(this);

        textInput.setText(selected.getTrackDescription());
        textInput.setSelectAllOnFocus(true);

        textInput.requestFocus();
        textInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(textInput, 0);
            }
        }, 100);

        dialogBuilder.setTitle("Set Description (max 3 lines)");
        dialogBuilder.setView(textInput);
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String description = textInput.getText().toString();
                data.setTrackDescription(selected, description);
                Toast.makeText(ActivityTracksListView.this, "Description changed!" ,
                        Toast.LENGTH_SHORT).show();
                populateListView();
            }
        });
        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogRename = dialogBuilder.create();
        dialogRename.show();
    }

    private void deleteDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Delete track?");
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                data.getCurrentAlbum().removeTrack(selected);
                data.removeDeletedTracks(data.getCurrentAlbum());
                Toast.makeText(ActivityTracksListView.this, "Track Deleted",
                        Toast.LENGTH_SHORT).show();
                populateListView();
            }
        });

        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogDelete = dialogBuilder.create();
        dialogDelete.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), ActivitySettings.class));
                return true;
            case android.R.id.home:
                if(mediaPlayer!=null) {
                    mediaPlayer.release();
                    mediaPlayer=null;
                }
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    private class MyListAdapter extends ArrayAdapter<TrackItem> {

        public MyListAdapter() {
            super(ActivityTracksListView.this, R.layout.tracks_list_view,
                data.getCurrentAlbum().getTracksList());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.tracks_item_view, parent, false);
            }

            //Find the album to work with
            final TrackItem currentTrack = data.getCurrentAlbum().getTracksList().get(position);

            //Make
            final TextView sizeText = (TextView) itemView.findViewById(R.id.size);
            sizeText.setText(currentTrack.getTrackSize());
            final TextView makeText = (TextView) itemView.findViewById(R.id.trackTextView);
            final TextView makeDescription = (TextView) itemView.
                    findViewById(R.id.track_description);
            final ImageView makeimage = (ImageView) itemView.
                    findViewById(R.id.imageView);
            makeText.setText(currentTrack.getTrackName());
            makeText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopPlayer();
                    selected = currentTrack;
                    track_text.setText(currentTrack.getTrackName());
                    startPlayer(selected);
                }
            });
            makeText.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    selected = currentTrack;
                    return false;
                }
            });

            //Make Description

            makeDescription.setText(currentTrack.getTrackDescription());
            makeDescription.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopPlayer();
                    selected = currentTrack;
                    track_text.setText(currentTrack.getTrackName());
                    startPlayer(selected);
                }
            });
            makeDescription.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    selected = currentTrack;
                    return false;
                }
            });

            makeDescription.setText(currentTrack.getTrackDescription());
            makeimage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopPlayer();
                    selected = currentTrack;
                    track_text.setText(currentTrack.getTrackName());
                    startPlayer(selected);
                }
            });
            makeimage.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    selected = currentTrack;
                    return false;
                }
            });

            play_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mediaPlayer!=null)
                        mediaPlayer.start();
                     else
                        if(selected!=null)
                        startPlayer(selected);
                    
                }
            });

            pause_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mediaPlayer!=null) {
                        mediaPlayer.pause();
                    }
                }
            });

            stop_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopPlayer();
                    trackSeekBar.setProgress(0);
                    current_duration.setText("00:00");
                }
            });

            trackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    if (fromUser && seekBar.isInTouchMode()) {
                        progressValue = progress;
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(progressValue);
                    } else {
                        trackSeekBar.setProgress(progressValue);
                    }
                }
            });

            return itemView;
        }

    }

    private void startPlayer(TrackItem currentTrack){
        Uri uri = Uri.fromFile(currentTrack.getTrackPath()); // Load track location
        final double duration;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            //mediaPlayer.reset();
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.seekTo(progressValue);
            duration = mediaPlayer.getDuration();
            track_duration.setText(String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) duration),
                    TimeUnit.MILLISECONDS.toSeconds((long) duration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.
                                    MILLISECONDS.toMinutes((long) duration))));

            trackSeekBar.setMax((int) duration);
            //trackSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            myHandler.postDelayed(UpdateSongTime,10);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    trackSeekBar.setProgress(trackSeekBar.getMax());
                    current_duration.setText(String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes((long) duration),
                            TimeUnit.MILLISECONDS.toSeconds((long) duration) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.
                                            MILLISECONDS.toMinutes((long) duration))));
                    progressValue = 0;
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        progressValue = 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlayer();
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            double startTime;
            if(mediaPlayer!=null) {
                startTime = mediaPlayer.getCurrentPosition();
                current_duration.setText(String.format("%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                toMinutes((long) startTime)))
                );

                trackSeekBar.setProgress((int) startTime);
                myHandler.postDelayed(this, 5);
            }
        }
    };
}
