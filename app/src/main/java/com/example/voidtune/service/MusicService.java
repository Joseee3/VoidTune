package com.example.voidtune.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;

    private String currentTitle;
    private String currentArtist;
    private String currentAlbumImageUrl;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public void setCurrentSong(String title, String artist, String albumImageUrl) {
        this.currentTitle = title;
        this.currentArtist = artist;
        this.currentAlbumImageUrl = albumImageUrl;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public String getCurrentArtist() {
        return currentArtist;
    }

    public String getCurrentAlbumImageUrl() {
        return currentAlbumImageUrl;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void playSong(String audioUrl) {
        if (mediaPlayer != null) {
            try {
                Log.d("MusicService", "Reproduciendo canción con URL: " + audioUrl);
                mediaPlayer.reset();
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                Log.e("MusicService", "Error al reproducir la canción: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.e("MusicService", "MediaPlayer no está inicializado.");
        }
    }

    public void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumeSong() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer(); // Libera el reproductor al destruir el servicio
    }
}