package com.example.ali.audavaproject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActivityRecord extends ActionBarActivity {

    private static final String SETTING_PREF = "SettingPref";
    private static final String RECORD_PREF = "RecordPref";
    private static final String SAMPLE_RATE_PREF = "SampleRate";
    private static final String LOG_TAG = "AudioRecord";

    private static final String DROPBOX_UPLOAD_PREF = "DropboxUpload";
    private static final String DROPBOX_PREF = "dropbox_prefs";
    private static final String APP_KEY = "2muzb3gk4xshp43";
    private static final String APP_SECRET = "au54wyjshebq2g8";

    private DropboxAPI<AndroidAuthSession> dropbox;
    private SharedPreferences preferences;
    private DataManager data;

    private Button cancelButton, albumsButton, finishButton;
    private ImageButton record;
    private TextView pauseText;
    private Animation animation;
    private Chronometer mChronometer;
    private long timeWhenStopped = 0;

    private AudioRecord mRecorder;
    private short[] mBuffer;
    private boolean mIsRecording;
    private ProgressBar mProgressBar;
    private int fileSize, sampleRate;
    private File tempRaw, tempWav, mergedWav, tempCopyWav;
    private String albumChoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_page);

        data = new DataManager();
        preferences = getSharedPreferences(SETTING_PREF, 0);
        sampleRate = preferences.getInt(SAMPLE_RATE_PREF, 44100);

        tempWav = getTempFile("wav");
        mergedWav = new File(Environment.getExternalStorageDirectory() + "/Audava/mergedWav.wav");

        initRecorder();
        record = (ImageButton) findViewById(R.id.record);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                toggleAudioRecord();
            }
        });
        setRecordAnimation();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);

        albumsButton = (Button) findViewById(R.id.albumButton);
        albumsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ActivityAlbumsListView.class));
            }
        });

        finishButton = (Button) findViewById(R.id.finish);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tempRaw != null) {
                    finishRecord();
                } else { toggleAudioRecord(); }
            }
        });

        cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tempRaw != null) {
                    cancelDialog();
                }
            }
        });

        pauseText = (TextView) findViewById(R.id.recordingText);

        if (preferences.getBoolean(RECORD_PREF, false)) {
            toggleAudioRecord();
        } else {
            finishButton.setText("Record Story");
        }
    }

    private void setRecordAnimation() {
        animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button
        // will fade back in
    }

    @Override
    public void onDestroy() {
        mRecorder.release();
        deleteTempFiles();
        super.onDestroy();
    }

    private void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mIsRecording = false;
    }

    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(
                            new FileOutputStream(file)));

                    while (mIsRecording) {
                        double sum = 0;
                        int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(mBuffer[i]);
                            sum += mBuffer[i] * mBuffer[i];
                        }

                        if (readSize > 0) {
                            final double amplitude = sum / readSize;
                            mProgressBar.setProgress((int) Math.sqrt(amplitude));
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(ActivityRecord.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    mProgressBar.setProgress(0);
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(ActivityRecord.this, e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(ActivityRecord.this, e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, sampleRate); // sample rate
            writeInt(output, sampleRate * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private File getTempFile(final String suffix) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Audava/temp" + "." + suffix);
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    private void toggleAudioRecord() {

        if (!mIsRecording) {
            albumsButton.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            pauseText.setVisibility(View.INVISIBLE);
            finishButton.setText("Finish/Save");
            tempRaw = getTempFile("raw");
            tempCopyWav = new File(Environment.getExternalStorageDirectory()
                    + "/Audava/tempCopyWav.wav");
            try {
                tempCopyWav.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // starts recording
            mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
            mChronometer.start();
            record.startAnimation(animation);
            mIsRecording = true;
            mRecorder.startRecording();
            startBufferedWrite(tempRaw);
        } else {
            // pauses recording
            mRecorder.stop();
            pauseText.setVisibility(View.VISIBLE);
            try {
                rawToWave(tempRaw, tempWav);
            } catch (IOException e) {
                e.printStackTrace();
            }
            CombineWaveFile(Environment.getExternalStorageDirectory() + "/Audava/tempCopyWav.wav",
                    Environment.getExternalStorageDirectory() + "/Audava/temp.wav");
            try {
                copy(mergedWav, tempCopyWav);
            } catch (IOException e) {
                e.printStackTrace();
            }
            timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
            mChronometer.stop();
            record.clearAnimation();
            mIsRecording = false;
        }
    }

    private void finishRecord() {
        mRecorder.stop();
        timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
        mChronometer.stop();
        record.clearAnimation();
        if (mIsRecording) {
            try {
                rawToWave(tempRaw, tempWav);
            } catch (IOException e) {
                Toast.makeText(ActivityRecord.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            CombineWaveFile(Environment.getExternalStorageDirectory() + "/Audava/tempCopyWav.wav",
                    Environment.getExternalStorageDirectory() + "/Audava/temp.wav");

            try {
                copy(mergedWav, tempCopyWav);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        fileSize = Integer.parseInt(String.valueOf(tempCopyWav.length() / 1024));
        mIsRecording = false;
        inputDialog();
    }

    private void cancelRecord() {
        albumsButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.INVISIBLE);
        pauseText.setVisibility(View.INVISIBLE);
        mChronometer.stop();
        mChronometer.setText("00:00");
        timeWhenStopped = 0;
        record.clearAnimation();

        mIsRecording = false;
        mRecorder.stop();

        tempRaw = null;
        deleteTempFiles();
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public void CombineWaveFile(String file1, String file2) {
        FileInputStream in1, in2;
        final int RECORDER_BPP = 16;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        int channels = 1;
        long byteRate = RECORDER_BPP * sampleRate * channels / 8;
        int bufferSize = 1024;
        byte[] data = new byte[bufferSize];

        try {
            in1 = new FileInputStream(file1);
            in2 = new FileInputStream(file2);
            out = new FileOutputStream(mergedWav);
            totalAudioLen = in1.getChannel().size() + in2.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    (long) sampleRate, channels, byteRate, RECORDER_BPP);

            while (in1.read(data) != -1)
                out.write(data);
            while (in2.read(data) != -1)
                out.write(data);
            out.close();
            in1.close();
            in2.close();
            out.close();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen,
                                     long longSampleRate, int channels, long byteRate,
                                     int RECORDER_BPP) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        header[34] = (byte) RECORDER_BPP;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_default, menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                if (mIsRecording) {
                    backDialog();
                } else {
                    startActivity(new Intent(getApplicationContext(), ActivitySettings.class));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void inputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save as..");

        LayoutInflater inflater = this.getLayoutInflater();
        final View inputDialogView = inflater.inflate(R.layout.record_input_dialog, null);
        builder.setView(inputDialogView);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        final String current = sdf.format(new Date());

        final EditText filenameInput = (EditText) inputDialogView.findViewById(R.id.filename);
        filenameInput.setText("Untitled " + current);
        filenameInput.setSelectAllOnFocus(true);

        filenameInput.requestFocus();
        filenameInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(filenameInput, 0);
            }
        }, 100);

        final Spinner spinner = (Spinner) inputDialogView.findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(inputDialogView.getContext(),
                android.R.layout.simple_spinner_dropdown_item, data.getListAlbumNames());
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0,
                                       View arg1, int position, long arg3) {
                albumChoice = spinner.getSelectedItem().toString(); // move to set pos btn, local
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }
        });

        final EditText descriptionInput = (EditText) inputDialogView.findViewById(R.id.description);

        builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String trackName = filenameInput.getText().toString();
                String trackDescription = descriptionInput.getText().toString();
                try {
                    if (data.addTrack(tempCopyWav, trackName, albumChoice, trackDescription,
                            String.valueOf(fileSize) + " KB\n" + mChronometer.getText())) {
                        showToast("File saved to album " + albumChoice);
                        AutoUpload(filenameInput.getText().toString());

                    } else {
                        data.addTrack(tempCopyWav, "Untitled " + current, albumChoice,
                                trackDescription, String.valueOf(fileSize) + " KB\n" +
                                        mChronometer.getText());
                        showToast("A file with the same name already exists. File saved with " +
                                "default name.");
                        AutoUpload("Untitled " + current);
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "copy() failed");
                    showToast("ERROR: cannot save file");
                }

                mChronometer.setText("00:00");
                deleteTempFiles();
                timeWhenStopped = 0;
                tempRaw = null;
                finishButton.setText("Record Story");
                albumsButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.INVISIBLE);
                pauseText.setVisibility(View.INVISIBLE);

                openAlbumDialog(albumChoice);
            }
        });

        builder.show().setCanceledOnTouchOutside(false);
    }

    private void deleteTempFiles() {
        if (tempRaw != null) {
            tempRaw.delete();
        }
        if (tempWav != null) {
            tempWav.delete();
        }
        if (tempCopyWav != null) {
            tempCopyWav.delete();
        }
        if (mergedWav != null) {
            mergedWav.delete();
        }
    }

    private void cancelDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Delete current recording?");
        dialogBuilder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelRecord();
                finishButton.setText("Record Story");
            }
        });
        dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogCancel = dialogBuilder.create();
        dialogCancel.show();
    }

    private void openAlbumDialog(final String albumChoice) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Go to album?");
        dialogBuilder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                data.setCurrentAlbum(albumChoice);
                startActivity(new Intent(getApplicationContext(), ActivityTracksListView.class));
            }
        });
        dialogBuilder.setPositiveButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogRename = dialogBuilder.create();
        dialogRename.show();
    }

    private void backDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Recording in progress...\nProceed to Settings?");
        dialogBuilder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //cancelRecord();
                startActivity(new Intent(getApplicationContext(), ActivitySettings.class));
                finish();
            }
        });
        dialogBuilder.setPositiveButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialogBack = dialogBuilder.create();
        dialogBack.show();
    }

    private boolean dropboxAuthentication() {
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
            return false;
        }
    }

    private void AutoUpload(String trackName) {
        int dropboxValue = preferences.getInt(DROPBOX_UPLOAD_PREF, 0);
        if (dropboxValue == 1) {
            if (dropboxAuthentication()) {
                UploadFileToDropbox upload = new UploadFileToDropbox(ActivityRecord.this, dropbox,
                        albumChoice + "/" + trackName + ".wav",
                        tempCopyWav);
                upload.execute();
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mIsRecording) {
            dialogOnBackPress();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void dialogOnBackPress() {

        new AlertDialog.Builder(this)
                .setMessage("Recording on progress...\nAre you sure you want to exit?")
                .setCancelable(false)
                .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityRecord.this.finish();
            }
        })
                .setPositiveButton("No", null)
                .show();
    }
}
