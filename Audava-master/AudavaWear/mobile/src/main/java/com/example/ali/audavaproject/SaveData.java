package com.example.ali.audavaproject;

import java.util.ArrayList;

public class SaveData {

    private String currentAlbum;
    private ArrayList<AlbumItem> listAlbums;

    public SaveData(final String currentAlbum, final ArrayList<AlbumItem> listAlbums) {
        this.currentAlbum = currentAlbum;
        this.listAlbums = listAlbums;
    }

    public String getCurrentAlbum() { return currentAlbum; }

    public ArrayList<AlbumItem> getListAlbums() { return listAlbums; }
}
