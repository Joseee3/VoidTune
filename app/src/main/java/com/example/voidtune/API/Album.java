package com.example.voidtune.API;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Album {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("artist")
    private String artist;

    @SerializedName("image")
    private String imageUrl;

    @SerializedName("song")
    private List<Song> songs; // Lista de canciones asociadas al álbum

    // Constructor
       public Album(String id, String name, String artist, String imageUrl, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.songs = songs;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<Song> getSongs() {
        return songs;
    }
}