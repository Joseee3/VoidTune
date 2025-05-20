package com.example.voidtune.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.R;
import com.example.voidtune.adapter.SongAdapter;
import com.example.voidtune.entities.Song;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DetailPlaylistActivity extends AppCompatActivity {

    private SongAdapter songAdapter;
    private ArrayList<Song> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_album);

        // Configurar RecyclerView
        RecyclerView songsRecyclerView = findViewById(R.id.songsRecyclerView);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        songs = new ArrayList<>();
        songAdapter = new SongAdapter(this, songs, true); // true porque es una playlist
        songsRecyclerView.setAdapter(songAdapter);


        String playlistId = getIntent().getStringExtra("playlistId");



        songAdapter.setOnDeleteClickListener((song, position) -> {
            removeSongFromPlaylist(playlistId, song, position);
        });

        // Configurar título
        TextView titleTextView = findViewById(R.id.albumTitle);
        ImageView playlistImageView = findViewById(R.id.albumCover);

        // Obtener datos del Intent

        String playlistName = getIntent().getStringExtra("playlistName");
        String playlistImage = getIntent().getStringExtra("playlistImage");
        String type = getIntent().getStringExtra("type");

        if ("likeSong".equals(type)) {
            titleTextView.setText("Tus me gusta");
            playlistImageView.setImageResource(R.drawable.img_album);
            loadPlaylistSongs("likeSong");
        } else if ("playlist".equals(type)) {
            titleTextView.setText(playlistName != null ? playlistName : "Playlist sin nombre");
            if (playlistImage != null && !playlistImage.isEmpty()) {
                Glide.with(this)
                    .load(playlistImage)
                    .placeholder(R.drawable.img_album)
                    .into(playlistImageView);
            } else {
                playlistImageView.setImageResource(R.drawable.img_album);
            }
            loadPlaylistSongs(playlistId);
        }
    }

    private void loadPlaylistSongs(String playlistId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("DetailPlaylist", "User not authenticated");
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference playlistSongsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("playlists")
                .child(playlistId)
                .child("songs");

        playlistSongsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> songIds = new ArrayList<>();
                for (DataSnapshot songSnapshot : task.getResult().getChildren()) {
                    String songId = songSnapshot.getValue(String.class); // Obtener el ID de la canción
                    if (songId != null) {
                        songIds.add(songId);
                    }
                }
                fetchSongsDetails(songIds); // Buscar detalles de las canciones
            } else {
                Log.e("DetailPlaylist", "Failed to load playlist songs: " + task.getException());
            }
        });
    }

    private void fetchSongsDetails(List<String> songIds) {
        DatabaseReference songsRef = FirebaseDatabase.getInstance().getReference("songs");

        for (String songId : songIds) {
            songsRef.child(songId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Song song = task.getResult().getValue(Song.class);
                    if (song != null) {
                        song.setId(songId); // Asigna el ID al objeto Song
                        songs.add(song); // Agregar la canción a la lista
                        songAdapter.notifyItemInserted(songs.size() - 1); // Actualizar adaptador
                    }
                } else {
                    Log.e("fetchSongsDetails", "Error fetching song data: " + task.getException());
                }
            });
        }
    }


   private void removeSongFromPlaylist(String playlistId, Song song, int position) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference playlistSongsRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("playlists")
                    .child(playlistId)
                    .child("songs");

                // Find the key for the song to delete
                playlistSongsRef.orderByValue().equalTo(song.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                // Remove the song using its key
                                child.getRef().removeValue().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        if (position >= 0 && position < songs.size()) {
                                            songs.remove(position); // Remove from local list
                                            songAdapter.notifyItemRemoved(position); // Notify adapter
                                            songAdapter.notifyItemRangeChanged(position, songs.size());
                                            Toast.makeText(DetailPlaylistActivity.this, "Song removed from playlist.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.e("DetailPlaylist", "Invalid position: " + position);
                                        }
                                    } else {
                                        Log.e("DetailPlaylist", "Failed to remove song from Firebase: " + task.getException());
                                        Toast.makeText(DetailPlaylistActivity.this, "Failed to remove song: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Log.e("DetailPlaylist", "Song ID not found in playlist.");
                            Toast.makeText(DetailPlaylistActivity.this, "Song not found in playlist.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DetailPlaylist", "Error querying song: " + error.getMessage());
                    }
                });
            } else {
                Toast.makeText(DetailPlaylistActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            }
        }

}