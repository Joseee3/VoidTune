package com.example.voidtune.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.voidtune.R;

import java.io.IOException;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;

    private String currentTitle;
    private String currentArtist;
    private String currentAlbumImageUrl;
    private String currentAudioUrl;

    private int currentPosition;

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

    public void setCurrentAudioUrl(String audioUrl) {
        this.currentAudioUrl = audioUrl;
        Log.d("MusicService", "URL de audio actualizada: " + audioUrl);
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0; // Devuelve 0 si el MediaPlayer no está inicializado
    }


    public void play() {
        if (currentAudioUrl != null) {
            playSong(currentAudioUrl); // Reutiliza la lógica de playSong
        } else {
            Log.e("MusicService", "No hay una URL de audio cargada para reproducir.");
        }
    }

    public void playSong(String audioUrl) {
        if (mediaPlayer != null && audioUrl.equals(currentAudioUrl)) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start(); // Reanuda si ya está cargada
            }
        } else {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            currentAudioUrl = audioUrl;
            try {
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                Log.e("MusicService", "Error al reproducir: " + e.getMessage());
            }
        }
    }

    public void saveState() {
        if (mediaPlayer != null) {
            currentPosition = mediaPlayer.getCurrentPosition();
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

public void restoreState() {
    if (mediaPlayer != null && currentAudioUrl != null) {
        mediaPlayer.seekTo(currentPosition);
    }
}

public void seekTo(int position) {
    if (mediaPlayer != null) {
        mediaPlayer.seekTo(position);
    }
}

public String getCurrentAudioUrl() {
    return currentAudioUrl;
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


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel(); // Crear el canal de notificación
        Notification notification = new NotificationCompat.Builder(this, "MusicServiceChannel")
                .setContentTitle("Reproduciendo música") // Título de la notificación
                .setContentText("Tu canción está en reproducción") // Texto de la notificación
                .setSmallIcon(R.drawable.ic_music_note) // Icono de la notificación
                .build();
        startForeground(1, notification); // Ejecutar el servicio en primer plano
        return START_STICKY; // Asegura que el servicio se reinicie si es detenido
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "MusicServiceChannel", // ID del canal
                    "Music Service Channel", // Nombre del canal
                    NotificationManager.IMPORTANCE_DEFAULT // Importancia del canal
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel); // Registrar el canal
            }
        }
    }
}