package com.example.voidtune.entities;

import java.util.List;

public class User {
    private String id;
    private String username;
    private String email;
    private String profileImage;
    private List<String> playlist; // IDs de playlists
    private List<String> likeSong; // IDs de canciones favoritas

    public User() {
    }

    public User(String username, String email, String profileImage) {
        this.username = username;
        this.email = email;
        this.profileImage = profileImage;
    }

    public User(String id, String username, String email, String profileImage, List<String> playlist, List<String> likeSong) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.profileImage = profileImage;
        this.playlist = playlist;
        this.likeSong = likeSong;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public List<String> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(List<String> playlist) {
        this.playlist = playlist;
    }

    public List<String> getLikeSong() {
        return likeSong;
    }

    public void setLikeSong(List<String> likeSong) {
        this.likeSong = likeSong;
    }
}