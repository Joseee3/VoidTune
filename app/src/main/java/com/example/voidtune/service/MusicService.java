package com.example.voidtune.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.voidtune.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;

    private String currentTitle;
    private String currentArtist;
    private String currentAlbumImageUrl;
    private String currentAudioUrl;

    private int currentPosition;

    private List<String> playlist = new ArrayList<>();
    private int currentSongIndex = 0;

    private String currentSong; // Canción actual
    private int loadAttempts = 0; // Contador de intentos de carga

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



    // Método para establecer la canción actual
    public void setCurrentSong(String song) {
        this.currentSong = song;
    }

    // Método para obtener la canción actual
    public String getCurrentSong() {
        return currentSong;
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
    if (audioUrl == null || audioUrl.isEmpty()) {
        Log.e("MusicService", "La URL del audio es nula o vacía.");
        return;
    }

    try {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else {
            mediaPlayer.reset();
        }

        mediaPlayer.setDataSource(audioUrl);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(mp -> {
            mp.start();
            Log.d("MusicService", "Reproducción iniciada.");
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e("MusicService", "Error en MediaPlayer: " + what + ", extra: " + extra);
            return true;
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d("MusicService", "Reproducción completada.");
            playNextSong(); // Reproduce la siguiente canción
        });

        mediaPlayer.prepareAsync();
        Log.d("MusicService", "MediaPlayer configurado con la URL: " + audioUrl);
    } catch (IOException e) {
        Log.e("MusicService", "Error al configurar el MediaPlayer: " + e.getMessage());
    }
}
    public void saveState() {
        if (mediaPlayer != null) {
            currentPosition = mediaPlayer.getCurrentPosition();
        }
    }

    public void resumeSong() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }



    public void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }



    public void setPlaylist(List<String> playlist) {
        if (playlist == null || playlist.isEmpty()) {
            Log.e("MusicService", "La lista de reproducción está vacía o es null.");
            this.playlist = new ArrayList<>();
            return;
        }
        this.playlist = playlist;
        currentSongIndex = 0; // Reinicia el índice
        Log.d("MusicService", "Lista de reproducción configurada con " + playlist.size() + " canciones.");
        // No iniciar reproducción automáticamente
    }

    private void fetchAndSaveSongData(String songId) {
        if (songId == null || songId.isEmpty()) {
            Log.e("MusicService", "El ID de la canción es inválido.");
            return;
        }

        Log.d("MusicService", "Recuperando datos para la canción con ID: " + songId);

        DatabaseReference songRef = FirebaseDatabase.getInstance().getReference("songs").child(songId);
        songRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                String title = snapshot.child("name").getValue(String.class);
                String artist = snapshot.child("artist").getValue(String.class);
                String audioUrl = snapshot.child("audioURL").getValue(String.class);
                String albumId = snapshot.child("albumID").getValue(String.class);

                if (title == null || artist == null || audioUrl == null || audioUrl.isEmpty()) {
                    Log.e("MusicService", "Datos incompletos para la canción con ID: " + songId);
                    return;
                }

                Log.d("MusicService", "Datos recuperados: " + title + ", " + artist + ", " + audioUrl);

                if (albumId != null && !albumId.isEmpty()) {
                    DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumId);
                    albumRef.child("imageURL").get().addOnCompleteListener(albumTask -> {
                        String albumImageUrl = albumTask.isSuccessful() ? albumTask.getResult().getValue(String.class) : null;
                        playSongWithDetails(title, artist, audioUrl, albumImageUrl);
                    });
                } else {
                    playSongWithDetails(title, artist, audioUrl, null);
                }
            } else {
                Log.e("MusicService", "Error al obtener datos de la canción: " + task.getException());
            }
        });
    }

  private void playSongWithDetails(String title, String artist, String audioUrl, String albumImageUrl) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }

        currentTitle = title;
        currentArtist = artist;
        currentAudioUrl = audioUrl;
        currentAlbumImageUrl = albumImageUrl;

        // Guardar los datos en SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("title", title);
        editor.putString("artist", artist);
        editor.putString("albumImageUrl", albumImageUrl);
        editor.putString("audioURL", audioUrl);
        editor.apply();
        Log.d("MusicService", "Datos guardados en SharedPreferences: " + title + ", " + artist + ", " + audioUrl);

        // Notificar al reproductor que los datos han cambiado
        notifyPlayerUpdate();

        // Reproducir la nueva canción
        if (audioUrl != null && !audioUrl.isEmpty()) {
            playSong(audioUrl);
        } else {
            Log.e("MusicService", "URL de audio no válida. No se puede reproducir la canción.");
            playNextSong(); // Intenta reproducir la siguiente canción
        }
    }

//    public void playNextSong() {
//        if (playlist != null && !playlist.isEmpty()) {
//            currentSongIndex = (currentSongIndex + 1) % playlist.size();
//            String nextSongId = playlist.get(currentSongIndex);
//            fetchAndSaveSongData(nextSongId);
//        } else {
//            Log.e("MusicService", "No hay canciones en la lista de reproducción.");
//        }
//    }

    public void playPreviousSong() {
        if (playlist != null && !playlist.isEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();
            String previousSongId = playlist.get(currentSongIndex);
            fetchAndSaveSongData(previousSongId);
            notifyPlayerUpdate(); // Notificar actualización
        } else {
            Log.e("MusicService", "No hay canciones en la lista de reproducción.");
        }
    }

    public void playNextSong() {
    if (playlist == null || playlist.isEmpty()) {
        Log.e("MusicService", "No hay canciones en la lista de reproducción.");
        return;
    }

    if (failedSongs.size() >= playlist.size()) {
        Log.e("MusicService", "Todas las canciones fallaron. Deteniendo reproducción.");
        Toast.makeText(this, "Error: No se pueden cargar canciones válidas.", Toast.LENGTH_SHORT).show();
        return;
    }

    do {
        currentSongIndex = (currentSongIndex + 1) % playlist.size();
    } while (failedSongs.contains(playlist.get(currentSongIndex)));

    String nextSongId = playlist.get(currentSongIndex);
    fetchAndSaveSongData(nextSongId);
    notifyPlayerUpdate(); // Notificar actualización
}



    private final List<String> failedSongs = new ArrayList<>(); // Lista de canciones fallidas

//   private void fetchAndSaveSongData(String songId) {
//        if (songId == null || songId.isEmpty()) {
//            Log.e("MusicService", "El ID de la canción es inválido.");
//            return;
//        }
//
//        if (failedSongs.contains(songId)) {
//            Log.e("MusicService", "La canción con ID: " + songId + " ya falló previamente. Saltando...");
//            playNeSxtSong();
//            return;
//        }
//
//        Log.d("MusicService", "Recuperando datos para la canción con ID: " + songId);
//
//        DatabaseReference songRef = FirebaseDatabase.getInstance().getReference("songs").child(songId);
//        songRef.get().addOnCompleteListener(task -> {
//            if (task.isSuccessful() && task.getResult() != null) {
//                DataSnapshot snapshot = task.getResult();
//
//                // Depurar los datos recuperados
//                Log.d("MusicService", "Datos recuperados: " + snapshot.getValue());
//
//                // Obtener datos de la canción
//                String title = snapshot.child("name").getValue(String.class); // Cambiado de "title" a "name"
//                String artist = snapshot.child("artist").getValue(String.class);
//                String audioUrl = snapshot.child("audioURL").getValue(String.class);
//                if (audioUrl == null || audioUrl.isEmpty()) {
//                    Log.e("FloatingPlayerFragment", "audioURL no encontrado o está vacío en Firebase.");
//                } else {
//                    Log.d("FloatingPlayerFragment", "audioURL obtenido de Firebase: " + audioUrl);
//                }
//                String albumId = snapshot.child("albumID").getValue(String.class);
//
//                if (title == null || artist == null || audioUrl == null) {
//                    Log.e("MusicService", "Datos incompletos para la canción con ID: " + songId);
//                    failedSongs.add(songId); // Agregar a la lista de fallos
//                    playNextSong(); // Intenta reproducir la siguiente canción
//                    return;
//                }
//
//                if (audioUrl == null || audioUrl.isEmpty()) {
//                    Log.e("MusicService", "audioURL no encontrado o está vacío en Firebase.");
//                    failedSongs.add(songId); // Agregar a la lista de fallos
//                    playNextSong(); // Intenta reproducir la siguiente canción
//                    return;
//                }
//
//                // Buscar la imagen del álbum si el albumId está disponible
//                if (albumId != null && !albumId.isEmpty()) {
//                    DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumId);
//                    albumRef.child("imageURL").get().addOnCompleteListener(albumTask -> {
//                        if (albumTask.isSuccessful() && albumTask.getResult() != null) {
//                            String albumImageUrl = albumTask.getResult().getValue(String.class);
//                            playSongWithDetails(title, artist, audioUrl, albumImageUrl);
//                        } else {
//                            Log.e("MusicService", "No se pudo obtener la imagen del álbum para el ID: " + albumId);
//                            playSongWithDetails(title, artist, audioUrl, null);
//                        }
//                    });
//                } else {
//                    playSongWithDetails(title, artist, audioUrl, null);
//                }
//            } else {
//                Log.e("MusicService", "Error al obtener datos de la canción: " + task.getException());
//                failedSongs.add(songId); // Agregar a la lista de fallos
//                playNextSong(); // Intenta reproducir la siguiente canción
//            }
//        });
//    }

//   private void playSongWithDetails(String title, String artist, String audioUrl, String albumImageUrl) {
//        // Detener la canción actual si está en reproducción
//        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//            mediaPlayer.stop();
//            mediaPlayer.reset();
//        }
//
//        // Actualizar variables del servicio
//        currentTitle = title;
//        currentArtist = artist;
//        currentAudioUrl = audioUrl;
//        currentAlbumImageUrl = albumImageUrl;
//
//        // Guardar los datos en SharedPreferences
//        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("title", title);
//        editor.putString("artist", artist);
//        editor.putString("albumImageUrl", albumImageUrl);
//        editor.putString("audioURL", audioUrl);
//        editor.apply();
//        Log.d("MusicService", "Datos guardados en SharedPreferences: " + title + ", " + artist + ", " + audioUrl);
//
//        // Notificar al reproductor que los datos han cambiado
//        notifyPlayerUpdate();
//
//        // Reproducir la nueva canción
//        playSong(audioUrl);
//
//        Log.d("MusicService", "Reproduciendo: " + title + " - " + artist);
//    }

private void notifyPlayerUpdate() {
    Intent intent = new Intent("com.example.voidtune.UPDATE_PLAYER");
    intent.putExtra("title", currentTitle);
    intent.putExtra("artist", currentArtist);
    intent.putExtra("albumImageUrl", currentAlbumImageUrl);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    Log.d("MusicService", "LocalBroadcast enviado con datos: " + currentTitle + ", " + currentArtist);
}

    public void loadFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
        currentTitle = sharedPreferences.getString("title", null);
        currentArtist = sharedPreferences.getString("artist", null);
        currentAlbumImageUrl = sharedPreferences.getString("albumImageUrl", null);
        currentAudioUrl = sharedPreferences.getString("audioURL", null); // Cambiar "audioUrl" a "audioURL"

        if (currentTitle != null && currentArtist != null && currentAlbumImageUrl != null && currentAudioUrl != null) {
            Log.d("MusicService", "Datos cargados desde SharedPreferences: " + currentTitle);
        } else {
            Log.e("MusicService", "No se encontraron datos en SharedPreferences.");
        }
    }

public boolean isPlaying() {
    return mediaPlayer != null && mediaPlayer.isPlaying();
}

public void restoreState() {
    if (mediaPlayer != null && currentAudioUrl != null) {
        mediaPlayer.seekTo(currentPosition);
        if (!mediaPlayer.isPlaying()) {
            Log.d("MusicService", "El reproductor está en pausa, no se reanudará automáticamente.");
        } else {
            mediaPlayer.pause(); // Asegura que no se reanude automáticamente
        }
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


//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        createNotificationChannel(); // Crear el canal de notificación
//        Notification notification = new NotificationCompat.Builder(this, "MusicServiceChannel")
//                .setContentTitle("Reproduciendo música") // Título de la notificación
//                .setContentText("Tu canción está en reproducción") // Texto de la notificación
//                .setSmallIcon(R.drawable.ic_music_note) // Icono de la notificación
//                .build();
//        startForeground(1, notification); // Ejecutar el servicio en primer plano
//        return START_STICKY; // Asegura que el servicio se reinicie si es detenido
//    }

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

   private boolean isNotificationActive = false; // Variable para rastrear el estado de la notificación

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
       loadFromSharedPreferences();

       if (intent != null) {
           ArrayList<String> newPlaylist = intent.getStringArrayListExtra("playlist");
           String sourceType = intent.getStringExtra("sourceType");
           boolean shouldStartPlayback = intent.getBooleanExtra("shouldStartPlayback", false);

           if (newPlaylist != null) {
               if (isPlaying() && playlist != null && !playlist.isEmpty()) {
                   Log.d("MusicService", "Ya hay una canción en reproducción. No se sobrescribirá la lista.");
               } else {
                   setPlaylist(newPlaylist);
                   Log.d("MusicService", "Lista de reproducción configurada desde: " + sourceType);

                   if (shouldStartPlayback) {
                       fetchAndSaveSongData(newPlaylist.get(0));
                   }
               }
           }
       }

       if (!isNotificationActive) { // Solo crea la notificación si no está activa
           createNotificationChannel();
           Notification notification = new NotificationCompat.Builder(this, "MusicServiceChannel")
                   .setContentTitle("Reproduciendo música")
                   .setContentText("Tu canción está en reproducción")
                   .setSmallIcon(R.drawable.ic_music_note)
                   .build();
           startForeground(1, notification);
           isNotificationActive = true; // Marca la notificación como activa
       }

       return START_STICKY;
   }

}