package com.example.ali.audavaproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class AlbumItem {

    private String name;
    private String description;
    private ArrayList<TrackItem> tracksList;
    private String iconPicture;

    public AlbumItem(final String name, final String description) {
        this.name = name;

        if( description.equals("")) {
            this.description = "No description";
        } else {
            this.description = description;
        }

        this.iconPicture = null;
        tracksList = new ArrayList<>();

        new File(getDirPath(name)).mkdirs();
    }

    public String getName() {
        return name;
    }

    public String getDescription() { return description; }

    public String getIconPicture() { return iconPicture; }

    public void setDescription(String description) { this.description = description; }

    public void setIconPicture(String imagePath) {
        if(iconPicture != null) { new File(iconPicture).delete(); } //delete previous icon picture
        iconPicture = copyImageFile(imagePath);
    }

    private String copyImageFile(String imagePath) {
        //copies a resized version of the original image to the album folder
        File originalFile = new File(imagePath);
        File albumImageFile = new File("/storage/emulated/0/Audava/" + name + "/cover.jpg");

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        Bitmap picture = BitmapFactory.decodeFile(originalFile.getAbsolutePath(), bitmapOptions);
        Bitmap resizedPicture = Bitmap.createScaledBitmap(picture, 120, 120, true);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(albumImageFile);
            resizedPicture.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return albumImageFile.getAbsolutePath();
    }

    public boolean addTrack(File temp, String trackName, String description, String fileSize)
            throws IOException {
        try {
            getTrack(trackName);
            return false;
        } catch (IllegalArgumentException e) {
            tracksList.add(new TrackItem(temp, trackName, name, description, fileSize));
            return true;
        }
    }

    public ArrayList<TrackItem> getTracksList() {
        return tracksList;
    }

    public TrackItem getTrack(String trackName) throws IllegalArgumentException {
        for (TrackItem track : tracksList) {
            if (track.getTrackName().toUpperCase().equals(trackName.toUpperCase())) {
                return track;
            }
        }
        throw new IllegalArgumentException("Track not found");
    }

    public void removeTrack(TrackItem track) {
        track.remove();
        tracksList.remove(track);
    }

    public void removeDeletedTracks() {
        //removes tracks deleted externally from the list of tracks.
        Iterator<TrackItem> iter = tracksList.iterator();
        while (iter.hasNext()) {
            TrackItem track = iter.next();
            if(!track.fileExists()) {
                iter.remove();
            }
        }
    }

    private String getDirPath(String str) {
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        return fileName + "/Audava/" + str;
    }

    public void renameAlbum(String newName) {
        File oldDir = new File(getDirPath(name));
        File newDir = new File(getDirPath(newName));
        oldDir.renameTo(newDir);
        for(TrackItem track : tracksList) {
            track.changeAlbumName(newName);
        }
        name = newName;
    }

    public void addTrackToList(TrackItem track) { tracksList.add(track); }

    public void removeTrackFromList(TrackItem track) { tracksList.remove(track); }

    public void checkIfPictureDeleted() {
        if (iconPicture != null) {
            File picture = new File(iconPicture);
            if (!picture.exists()) {
                iconPicture = null;
            }
        }
    }

    public void deletePicture() { iconPicture = null; }
}
