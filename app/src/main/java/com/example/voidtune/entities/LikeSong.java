package com.example.voidtune.entities;

public class LikeSong {
    private String songId;
    private String songName;
    private String artist;

    public LikeSong() {
    }

    public LikeSong(String songId, String songName, String artist) {
        this.songId = songId;
        this.songName = songName;
        this.artist = artist;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}