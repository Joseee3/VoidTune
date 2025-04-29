package com.example.voidtune.API;

import java.io.Serializable;

public class Song implements Serializable {
    private String title;
    private String artist;
    private String duracion;
    private String genero;
    private String id;
    private String albumId;

    // Constructor
    public Song(String title, String artist, String duracion, String genero, String id, String albumId) {
        this.title = title;
        this.artist = artist;
        this.duracion = duracion;
        this.genero = genero;
        this.id = id;
        this.albumId = albumId;
    }

    // Getters y setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }
}