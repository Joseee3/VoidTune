package com.example.voidtune.VIewModel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MusicViewModel extends ViewModel {
    private final MutableLiveData<String> currentSongId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();

    public LiveData<String> getCurrentSongId() {
        return currentSongId;
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