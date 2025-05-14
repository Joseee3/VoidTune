package com.example.voidtune.entities;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String name;
    private List<Album> albums;

    public Playlist(String name) {
        this.name = name;
        this.albums = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public void addAlbum(Album album) {
        this.albums.add(album);
    }

    public void removeAlbum(Album album) {
        this.albums.remove(album);
    }
}