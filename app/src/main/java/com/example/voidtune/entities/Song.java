package com.example.voidtune.entities;

import java.io.Serializable;

public class Song implements Serializable {
    private String name;
    private String artist;
    private String duracion;
    private String genero;
    private String id;
    private String albumId;
    // Constructor vacío requerido por Firebase
    public Song() {
        // Constructor vacío requerido por Firebase
    }

    // Constructor
    public Song(String name, String artist, String duracion, String genero, String id, String albumId) {
        this.name = name;
        this.artist = artist;
        this.duracion = duracion;
        this.genero = genero;
        this.id = id;
        this.albumId = albumId;
    }

    // Getters y setters
    public String getName() {
        return name;
    }

    public void setName(String title) {
        this.name = title;
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