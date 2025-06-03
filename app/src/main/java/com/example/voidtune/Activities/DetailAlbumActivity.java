package com.example.voidtune.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.BaseActivity;
import com.example.voidtune.FloatingPlayerFragment;
import com.example.voidtune.R;
import com.example.voidtune.adapter.SongAdapter;
import com.example.voidtune.entities.Song;
import com.example.voidtune.service.MusicService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class DetailAlbumActivity extends BaseActivity {

    private boolean isServiceBound = false;
    private MusicService musicService;
    private RecyclerView recyclerView;
    private ArrayList<Song> songs = new ArrayList<>();
    private String currentAudioUrl;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };
    @Override
    protected void onStart() {
        super.onStart();
        // Vincula el servicio de música
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // No reiniciar el reproductor si ya hay una canción en reproducción
        if (isServiceBound && musicService != null) {
            musicService.restoreState();
        }

        // Carga los datos del reproductor desde SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
        currentAudioUrl = sharedPreferences.getString("currentAudioUrl", null);

        // Asegúrate de que el reproductor flotante esté visible
        loadFloatingPlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            // Guarda los datos del reproductor en SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("currentAudioUrl", currentAudioUrl);
            editor.apply();

            // Desvincula el servicio de música
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    protected void loadFloatingPlayer() {
        FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
                getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);

        if (floatingPlayerFragment == null) {
            floatingPlayerFragment = new FloatingPlayerFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.floatingPlayerContainer, floatingPlayerFragment)
                    .commitNow();
        }

        if (currentAudioUrl != null) {
            floatingPlayerFragment.updatePlayer(currentAudioUrl);
            findViewById(R.id.floatingPlayerContainer).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.floatingPlayerContainer).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_album);


        // Configurar RecyclerView
        recyclerView = findViewById(R.id.songsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SongAdapter songAdapter = new SongAdapter(this, songs);
        recyclerView.setAdapter(songAdapter);

        // Configurar listener de clics en canciones
        songAdapter.setOnSongClickListener(song -> {
            Log.d("SongClick", "Song ID: " + song.getId());
            updateFloatingPlayer(song.getId());

            if (musicService != null && isServiceBound) {
                musicService.playSong(song.getAudioURL());
            } else {
                Toast.makeText(this, "Music service is not available.", Toast.LENGTH_SHORT).show();
            }
        });

        // Recuperar datos del Intent
        String albumId = getIntent().getStringExtra("albumId");
        String albumName = getIntent().getStringExtra("albumName");
        String albumImage = getIntent().getStringExtra("albumImage");

        if (albumId == null || albumId.isEmpty()) {
            finish();
            return;
        }

        // Configurar vistas
        TextView albumTitle = findViewById(R.id.albumTitle);
        ImageView albumCover = findViewById(R.id.albumCover);

        albumTitle.setText(albumName);
        Glide.with(this).load(albumImage).into(albumCover);

        // Cargar canciones desde Firebase
        FirebaseDatabase.getInstance().getReference("albums").child(albumId).child("songs")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DataSnapshot songSnapshot : task.getResult().getChildren()) {
                        String songId = songSnapshot.getValue(String.class);
                        if (songId != null) {
                            FirebaseDatabase.getInstance().getReference("songs").child(songId)
                                .get()
                                .addOnCompleteListener(songTask -> {
                                    if (songTask.isSuccessful() && songTask.getResult() != null) {
                                        Song song = songTask.getResult().getValue(Song.class);
                                        if (song != null) {
                                            song.setId(songId);
                                            songs.add(song);
                                            songAdapter.notifyItemInserted(songs.size() - 1);
                                        }
                                    }
                                });
                        }
                    }
                } else {
                    Log.e("Firebase", "Error al cargar canciones: " + task.getException().getMessage());
                }
            });
    }

    private void updateFloatingPlayer(String songId) {
        currentAudioUrl = songId;

        FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
                getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);

        if (floatingPlayerFragment == null) {
            floatingPlayerFragment = new FloatingPlayerFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.floatingPlayerContainer, floatingPlayerFragment)
                    .commitNow();
        }

        if (floatingPlayerFragment.isAdded()) {
            floatingPlayerFragment.updatePlayer(songId);
        } else {
            Log.e("DetailAlbumActivity", "FloatingPlayerFragment no está disponible.");
        }
    }

}