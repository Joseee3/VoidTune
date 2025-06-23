package com.example.voidtune.VIewModel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.voidtune.entities.Song;

import java.util.ArrayList;
import java.util.List;

public class MusicViewModel extends ViewModel {



    private final MutableLiveData<String> currentSongId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();

    public LiveData<String> getCurrentSongId() {
        return currentSongId;
    }

    private final List<Song> allSongs = new ArrayList<>();
    private final MutableLiveData<List<Song>> filteredSongs = new MutableLiveData<>();

    public LiveData<List<Song>> getFilteredSongs() {
        return filteredSongs;
    }

    public void searchSongs(String query) {
        List<Song> result = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getName().toLowerCase().contains(query.toLowerCase()) ||
                song.getArtist().toLowerCase().contains(query.toLowerCase())) {
                result.add(song);
            }
        }
        filteredSongs.setValue(result);
    }

    // Java
    public void setAllSongs(List<Song> songs) {
        allSongs.clear();
        allSongs.addAll(songs);
        filteredSongs.setValue(new ArrayList<>(allSongs));
    }


    public void loadAllSongs() {
        filteredSongs.setValue(allSongs);
    }

    public void setCurrentSongId(String songId) {
        currentSongId.setValue(songId);
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(Boolean playing) {
        isPlaying.setValue(playing);
    }
}