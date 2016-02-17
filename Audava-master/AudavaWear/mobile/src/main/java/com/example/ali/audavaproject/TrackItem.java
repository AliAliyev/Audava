package com.example.ali.audavaproject;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TrackItem {

    private String trackName;
    private File trackPath;
    private String albumName;
    private String description;
    private String fileSize;

    public TrackItem(File temp, final String trackName, final String albumName,
                     final String description, final String fileSize) throws IOException {
        this.trackName = trackName;
        this.albumName = albumName;
        if(description.equals("") ) {
            this.description = "No description";
        } else {
            this.description = description;
        }

        this.fileSize = fileSize;

        trackPath = new File(setFilename(trackName));
        copy(temp, trackPath);
    }

    private void copy(File src, File dst) throws IOException {
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

    private String setFilename(String name) {
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        return fileName + "/Audava/" + albumName + "/" + name + ".wav";
    }

    public File getTrackPath() {
        return trackPath;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getTrackDescription() { return description; }

    public void setTrackDescription(final String description) { this.description = description; }

    public String getTrackSize() { return fileSize; }

    //public void setTrackSize(final String fileSize) { this.fileSize = fileSize; }

    public boolean fileExists() {
        return trackPath.exists();
    }

    public void remove() {
        trackPath.delete();
    }

    public void changeAlbumName(String str) {
        //note: does not move file by itself (used for renaming albums)
        albumName = str;
        trackPath = new File(setFilename(trackName));
    }

    public void renameTrack(String newName) {
        File newPath = new File(setFilename(newName));
        trackPath.renameTo(newPath);
        trackPath = newPath;
        trackName = newName;
    }

    public void moveFile(String newAlbumName) {
        //note: does not remove file from original album's tracks list or add file to new album's
        //      tracks list by itself
        albumName = newAlbumName;
        File newPath = new File(setFilename(trackName));
        trackPath.renameTo(newPath);
        trackPath = newPath;
    }

    public String getAlbumName() { return albumName; }
}