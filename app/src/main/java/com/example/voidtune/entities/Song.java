package com.example.voidtune.entities;

public class Song  {
    private String name;
    private String artist;
    private String duracion;
    private String genero;
    private String id;
    public String audioURL;
    private String albumID;

    // Constructor vacío requerido por Firebase
    public Song() {
    }

    public Song(String id, String name, String artist, String audioURL, String duration) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.audioURL = audioURL;
        this.duracion = duration;
    }

    public Song(String name, String artist, String duracion, String genero, String id, String albumId) {
        this.name = name;
        this.artist = artist;
        this.duracion = duracion;
        this.genero = genero;
        this.id = id;
        this.albumID = albumId;
    }

    public Song(String id, String name, String artist) {
        this.id = id;
        this.name = name;
        this.artist = artist;
    }

     public Song(String songID, String albumID, String artist, String audioURL, String duration, String name, boolean isFull) {
         this.id = songID;
         this.albumID = albumID;
         this.artist = artist;
         this.audioURL = audioURL;
         this.duracion = duration;
         this.name = name;
     }

    public Song(String id) {
        this.id = id;
    }

    // Getters and setters
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

    public String getAlbumID() {
        return albumID;
    }

    public void setAlbumID(String albumId) {
        this.albumID = albumId;
    }

    public String getAudioURL() {
        return audioURL;
    }
    public void setAudioURL(String audioURL) {
        this.audioURL = audioURL;
    }

}