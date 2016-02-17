package com.example.ali.audavaproject;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class DataManager {

    private static final String LOG_TAG = "DataManager";

    private String currentAlbum;
    private ArrayList<AlbumItem> listAlbums;
    private File saveFile;

    public DataManager() {
        currentAlbum = null;
        listAlbums = new ArrayList<>();

        String savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        savePath += "/Audava";
        new File(savePath).mkdirs();

        savePath += "/data.json";
        saveFile = new File(savePath);

        if(saveFile.exists()) { load(); }
        else {
            try { saveFile.createNewFile(); }
            catch (IOException e) {
                Log.e(LOG_TAG, "createNewFile() failed");
                e.printStackTrace();
            }
        }

        try { getAlbum("Default Story Album"); }
        catch (IllegalArgumentException e) {
            addAlbum("Default Story Album", "This is your Default Story Album, where you are " +
                    "prompted to save your tracks by default");
        }
    }

    private void save() {
        try {
            FileWriter file = new FileWriter(saveFile, false);
            SaveData toSave = new SaveData(currentAlbum, listAlbums);
            file.write(new Gson().toJson(toSave));
            file.flush();
            file.close();
        } catch ( IOException e ) {
            Log.e(LOG_TAG, "save() failed");
            e.printStackTrace();
        }
    }

    private void load() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(saveFile));
            SaveData getSave = new Gson().fromJson(br, SaveData.class);
            currentAlbum = getSave.getCurrentAlbum();
            listAlbums = getSave.getListAlbums();
        } catch ( IOException e ) {
            Log.e(LOG_TAG, "load() failed");
            e.printStackTrace();
        }
    }

    public boolean addAlbum(String name, String description) {
        try {
            getAlbum(name);
            return false;
        } catch (IllegalArgumentException e) {
            listAlbums.add(new AlbumItem(name, description));
            save();
            return true;
        }
    }

    public ArrayList<AlbumItem> getListAlbums() {
        return listAlbums;
    }

    public ArrayList<String> getListAlbumNames() {
        ArrayList<String> albumNames = new ArrayList<>();
        for (AlbumItem album : listAlbums) {
            albumNames.add(album.getName());
        }
        return albumNames;
    }

    private AlbumItem getAlbum(String name) throws IllegalArgumentException {
        for ( AlbumItem album : listAlbums ) {
            if ( album.getName().toUpperCase().equals(name.toUpperCase()) ) {
                return album;
            }
        }
        throw new IllegalArgumentException("Album not found");
    }

    public void removeAlbum(AlbumItem album) {
        for( TrackItem track : album.getTracksList() ) {
            track.remove();
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dir = new File(path + "/Audava/" + album.getName());
        dir.delete();
        listAlbums.remove(album);
        save();
    }

    public void setCurrentAlbum(String albumName) {
        currentAlbum = albumName;
        save();
    }

    public AlbumItem getCurrentAlbum() {
        return getAlbum(currentAlbum);
    }

    public void removeDeletedAlbums() {
        //removes album folders deleted externally from the list of albums.
        Iterator<AlbumItem> iter = listAlbums.iterator();
        while (iter.hasNext()) {
            AlbumItem album = iter.next();
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            File dir = new File(path + "/Audava/" + album.getName());
            if(!dir.exists()) {
                iter.remove();
            }
        }
        save();
    }

    public void checkAlbumPictures() {
        //check if any album pictures are deleted, and if they are, remove them (set to null)
        for(AlbumItem album : listAlbums) {
            album.checkIfPictureDeleted();
        }
        save();
    }

    public boolean renameAlbum(AlbumItem album, String newName) {
        try {
            getAlbum(newName);
            return false;
        } catch (IllegalArgumentException e) {
            album.renameAlbum(newName);
            save();
            return true;
        }
    }

    public void changeAlbumDescription(AlbumItem album, String description) {
        album.setDescription(description);
        save();
    }

    public void deleteAlbumPicture(AlbumItem album) {
        album.deletePicture();
        save();
    }

    public void setAlbumPicture(AlbumItem album, String iconPicture) {
        album.setIconPicture(iconPicture);
        save();
    }

    public boolean addTrack(File temp, String trackName, String albumName, String trackDescription,
                            String fileSize) throws IOException {
        if ( getAlbum(albumName).addTrack(temp, trackName, trackDescription, fileSize) ) {
            save();
            return true;
        } else {
            return false;
        }
    }

    public void removeDeletedTracks(AlbumItem album) {
        //removes tracks deleted externally from an album.
        album.removeDeletedTracks();
        save();
    }

    public void moveTrack(TrackItem track, String newAlbumName) {
        String oldAlbumName = track.getAlbumName();
        track.moveFile(newAlbumName);
        getAlbum(newAlbumName).addTrackToList(track);
        getAlbum(oldAlbumName).removeTrackFromList(track);
        save();
    }

    public void renameTrack(TrackItem track, String newName) {
        track.renameTrack(newName);
        save();
    }

    public void setTrackDescription(TrackItem track, String description) {
        track.setTrackDescription(description);
        save();
    }
}
