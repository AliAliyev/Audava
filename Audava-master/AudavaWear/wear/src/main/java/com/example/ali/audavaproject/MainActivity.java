package com.example.ali.audavaproject;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.mariux.teleport.lib.TeleportClient;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

   // TeleportClient mTeleportClient;

   // private static final String LOG_TAG = "AudioRecordTest";
    private ImageButton record;
    private Chronometer mChronometer;
    private Boolean recordOn;
    private Animation animation;
    private AudioRecord aRecorder;
    //private DataOutputStream dataOutputStream;
    //short[] audioData;

    public static final int SAMPLE_RATE = 44100;
    private int bufferSize;
    private short[] mBuffer;
    private boolean mIsRecording = false;
    private ProgressBar mProgressBar;
    private AudioRecord mRecorder;
    private File tempRaw;

    private static final String TAG = MainActivity.class.getName();
  //  private static final int SPEECH_REQUEST_CODE = 1;

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    //private int bufferSize = 0;
    private Thread recordingThread = null;
    private GoogleApiClient mGoogleApiClient;

    private static final String COUNT_KEY = "com.example.key.count";
    private int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRecorder();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                record = (ImageButton) findViewById(R.id.imageButton);
                mChronometer = (Chronometer) findViewById(R.id.chronometer);
                animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
                animation.setDuration(500); // duration - half a second
                animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
                animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
                animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
                recordOn= false;

                setTitle("Audava");

            record.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   // counter++;
                   toggleAudioRecord();

                // increaseCounter();
                  //  sendMessage(v);
                 //   syncDataItem(v);
                            //toggleRecord();
                            //toggleFlash(v);
                            //toggleTimer();
                            //recordOn = !recordOn;
                    }

            });
            }
        });
       // bufferSize =
             //   AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                 //       RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
      //  mTeleportClient = new TeleportClient(this);
    }

/*
    private void showToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }
*/

    private void initRecorder() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
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
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    mProgressBar.setProgress(0);
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private File getTempFile(final String suffix) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"+ "temp" + "." + suffix);
    }

    private void toggleAudioRecord() {

        if (!mIsRecording) {
            tempRaw = getTempFile("raw");
            //tempCopyWav = new File("/storage/emulated/0/Audava/tempCopyWav.wav");
            // starts recording
          //  mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
            mChronometer.start();
            record.startAnimation(animation);
            mIsRecording = true;
            mRecorder.startRecording();
            startBufferedWrite(tempRaw);
        } else {
            // pauses recording
            mRecorder.stop();
            mChronometer.stop();
            record.clearAnimation();
            mIsRecording = false;

            Toast.makeText(getApplicationContext(), "Data changed.",
                    Toast.LENGTH_LONG).show();
            Asset asset = createAssetFromFile(tempRaw);
            PutDataMapRequest dataMap = PutDataMapRequest.create("/audio");
            dataMap.getDataMap().putAsset("profileImage", asset);
            dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
            PutDataRequest request = dataMap.asPutDataRequest();
            //PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                   // .putDataItem(mGoogleApiClient, request);
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
       // mTeleportClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
      //  mTeleportClient.disconnect();
    }
    /*
    @Override
    protected void onPause() {
        super.onPause();
        if (aRecorder != null) {
            aRecorder.release();
            aRecorder = null;
        }
    }
*/

    public void syncDataItem(View v) {

        //set the AsyncTask to execute when the Data is Synced
     //   mTeleportClient.setOnSyncDataItemTask(new ShowToastOnSyncDataItemTask());

        //Let's sync a String!
      //  mTeleportClient.syncString("string", String.valueOf(counter++));
     //mTeleportClient.syncAsset();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    public class ShowToastOnSyncDataItemTask extends TeleportClient.OnSyncDataItemTask {

        @Override
        protected void onPostExecute(DataMap dataMap) {

            String s = dataMap.getString("string");

            Toast.makeText(getApplicationContext(),"DataItem - "+s,Toast.LENGTH_SHORT).show();

            //let's reset the task (otherwise it will be executed only once)
         //   mTeleportClient.setOnSyncDataItemTask(new ShowToastOnSyncDataItemTask());
        }
    }

    public void sendMessage(View v) {

      //  mTeleportClient.setOnGetMessageTask(new ShowToastFromOnGetMessageTask());

     //   mTeleportClient.sendMessage("ogranc", null);
    }

    public class ShowToastFromOnGetMessageTask extends TeleportClient.OnGetMessageTask {

        @Override
        protected void onPostExecute(String path) {


            Toast.makeText(getApplicationContext(), "Message - " + path, Toast.LENGTH_SHORT).show();

            //let's reset the task (otherwise it will be executed only once)
        //    mTeleportClient.setOnGetMessageTask(new ShowToastFromOnGetMessageTask());
        }
    }
    private void toggleRecord() {
        if (!recordOn) {
            startRecord();
        } else {

            stopRecord();
        }
    }

    private void startRecord() {

       // audioData = new short[bufferSize];

        Log.v(TAG, "Starting audio capture");
        aRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        if (aRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
            aRecorder.startRecording();
           // recordOn= true;
            Log.v(TAG, "Successfully started recording");

          //  while(recordOn){
             //   int numberOfShort = aRecorder.read(audioData, 0, bufferSize);
               // for(int i = 0; i < numberOfShort; i++)
                  //  dataOutputStream.writeShort(audioData[i]);  }

           recordingThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    processRawAudioData();
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
        }
        else Log.v(TAG, "Failed to started recording");

    }

    private void stopRecord() {
        Log.v(TAG, "Stop audio capture");
       // recordOn=false;
        this.aRecorder.stop();
      //  aRecorder.reset();
        this.aRecorder.release();
      //  aRecorder = null;
    }

    private void processRawAudioData() {
        byte data[] = new byte[bufferSize];
        int read;
        while(recordOn) {
            read = aRecorder.read(data, 0, bufferSize);

            if(AudioRecord.ERROR_INVALID_OPERATION != read)
                Log.v(TAG, "Successfully read " + data.length + " bytes of audio");
        }

    }

    private  Asset createAssetFromFile(File file) {
        //final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        //bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
       // byteStream.write(data, 0, data.length);
       // return Asset.createFromBytes(byteStream.toByteArray());
return Asset.createFromUri(Uri.fromFile(file));
    }

    private void toggleFlash(View v) {
        if(!recordOn) {
            record.startAnimation(animation);
        } else {
            v.clearAnimation();
        }
    }

    private void toggleTimer() {
        mChronometer.setBase(SystemClock.elapsedRealtime());
        if(!recordOn) {
            mChronometer.start();
        } else {
            mChronometer.stop();
        }
    }

    private void increaseCounter() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
        putDataMapReq.getDataMap().putInt(COUNT_KEY, count++);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }


}
