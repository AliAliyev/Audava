package com.example.ali.audavaproject;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class UploadFileToDropbox extends AsyncTask<Void, Void, Boolean> {

    private DropboxAPI<?> dropbox;
    private String path;
    private File file;
    private Context context;

    public UploadFileToDropbox(Context context,DropboxAPI<?> dropbox, String path, File file) {
        this.context = context;
        this.file = file;
        this.dropbox = dropbox;
        this.path = "/" + path; // path =  album name
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            dropbox.putFile(path, fileInputStream, file.length(), null, null);
            return true;
        } catch (IOException | DropboxException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(context, "File Uploaded Successfully!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Failed to upload file", Toast.LENGTH_LONG).show();
        }
    }

}