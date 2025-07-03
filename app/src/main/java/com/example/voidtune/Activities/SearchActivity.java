
package com.example.voidtune.Activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import com.example.voidtune.FloatingPlayerFragment;
import com.example.voidtune.R;
import com.example.voidtune.VIewModel.MusicViewModel;
import com.example.voidtune.adapter.SongAdapter;
import com.example.voidtune.adapter.SearchAdapter;
import com.example.voidtune.entities.Song;
import com.example.voidtune.service.MusicService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private SongAdapter songAdapter;
    private MusicViewModel musicViewModel;

    private MusicService musicService;
    private boolean isServiceBound = false;

    public SearchAdapter searchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);

        SearchView searchView = findViewById(R.id.search_view);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);


    SearchAdapter songAdapter = new SearchAdapter(this, new ArrayList<>(), false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);

        //Abrir Library
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_home) {
                    startActivity(new Intent(SearchActivity.this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.menu_search) {
                    // Acción para el menú Search
                    startActivity(new Intent(SearchActivity.this, SearchActivity.class));
                    return true;
                } else if (itemId == R.id.menu_library) {
                    startActivity(new Intent(SearchActivity.this, LibraryActivity.class));
                    return true;
                }
                return false;
            }
        });

        fetchAllSongsFromFirebase(new OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<Song> songs) {
                musicViewModel.setAllSongs(songs);
            }
        });

        // Observe filtered songs
        musicViewModel.getFilteredSongs().observe(this, songs -> {
            songAdapter.setSongs(songs);
        });

        // Search as user types
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.isEmpty()) {
                    musicViewModel.searchSongs(query);
                } else {
                    songAdapter.setSongs(new ArrayList<>()); // Clear list
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.isEmpty()) {
                    musicViewModel.searchSongs(newText);
                } else {
                    songAdapter.setSongs(new ArrayList<>()); // Clear list
                }
                return true;
            }
        });

        songAdapter.setOnSongClickListener(selectedSong -> {
            String albumId = selectedSong.getAlbumID() != null ? selectedSong.getAlbumID() : "";

            FirebaseDatabase.getInstance()
                .getReference("albums")
                .child(albumId)
                .get()
                .addOnCompleteListener(task -> {
                    String albumImageUrl = "";
                    String albumName = "";
                    String albumArtist = "";
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot albumSnapshot = task.getResult();
                        albumImageUrl = albumSnapshot.child("imageURL").getValue(String.class);
                        albumName = albumSnapshot.child("name").getValue(String.class);
                        albumArtist = albumSnapshot.child("artist").getValue(String.class);
                    }

                    // Save all song and album data for the floating player
                    SharedPreferences prefs = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("currentAudioUrl", selectedSong.getAudioURL());
                    editor.putString("title", selectedSong.getName());
                    editor.putString("artist", selectedSong.getArtist());
                    editor.putString("albumImageUrl", albumImageUrl != null ? albumImageUrl : "");
                    editor.putString("albumName", albumName != null ? albumName : "");
                    editor.putString("albumArtist", albumArtist != null ? albumArtist : "");
                    editor.apply();

                    // Always replace and commit the fragment immediately
                    // Actualiza el fragmento del reproductor flotante
                    FloatingPlayerFragment floatingPlayerFragment = new FloatingPlayerFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.floatingPlayerContainer, floatingPlayerFragment)
                            .commitNow();

                    floatingPlayerFragment.updatePlayer(selectedSong.getAudioURL());
                    floatingPlayerFragment.updateAlbumImage(albumImageUrl);
                    findViewById(R.id.floatingPlayerContainer).setVisibility(View.VISIBLE);

                    // Start playback logic
                    if (musicService != null && isServiceBound) {
                        musicService.playSong(selectedSong.getAudioURL());
                    } else {
                        Intent intent = new Intent(SearchActivity.this, MusicService.class);
                        intent.putExtra("audioUrl", selectedSong.getAudioURL());
                        intent.putExtra("sourceType", "search");
                        startService(intent);
                    }
                    updateFloatingPlayer(selectedSong.getId());
                });
        });
    }

    private void fetchAllSongsFromFirebase(OnSongsFetchedListener listener) {
        DatabaseReference songsRef = FirebaseDatabase.getInstance().getReference("songs");
        songsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Song> songs = new ArrayList<>();
                for (DataSnapshot songSnapshot : dataSnapshot.getChildren()) {
                    Song song = songSnapshot.getValue(Song.class);
                    if (song != null) {
                        songs.add(song);
                    }
                }
                listener.onSongsFetched(songs);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onSongsFetched(new ArrayList<>());
            }
        });
    }

    private void loadFloatingPlayer() {
        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
        String currentAudioUrl = sharedPreferences.getString("currentAudioUrl", null);

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
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        loadFloatingPlayer();
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    public interface OnSongsFetchedListener {
        void onSongsFetched(List<Song> songs);
    }

    public void updateFloatingPlayer(String songId) {
        if (songId == null || songId.isEmpty()) {
            Log.e("DetailAlbumActivity", "songId is null or empty. Cannot update player.");
            return;
        }

        FirebaseDatabase.getInstance().getReference("songs").child(songId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot songSnapshot = task.getResult();
                        String audioUrl = songSnapshot.child("audioURL").getValue(String.class);
                        String title = songSnapshot.child("name").getValue(String.class);
                        String artist = songSnapshot.child("artist").getValue(String.class);
                        String albumId = songSnapshot.child("albumID").getValue(String.class);

                        if (albumId != null) {
                            FirebaseDatabase.getInstance().getReference("albums").child(albumId)
                                    .get()
                                    .addOnCompleteListener(albumTask -> {
                                        String albumImageUrl = "";
                                        String albumName = "";
                                        String albumArtist = "";
                                        if (albumTask.isSuccessful() && albumTask.getResult() != null) {
                                            DataSnapshot albumSnapshot = albumTask.getResult();
                                            albumImageUrl = albumSnapshot.child("imageURL").getValue(String.class);
                                            albumName = albumSnapshot.child("name").getValue(String.class);
                                            albumArtist = albumSnapshot.child("artist").getValue(String.class);
                                        }

                                        // Recupera la playlist actual de SharedPreferences
                                        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
                                        HashSet<String> playlistSet = new HashSet<>(sharedPreferences.getStringSet("playlist", new HashSet<>()));

                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putStringSet("playlist", playlistSet);
                                        editor.putString("currentAudioUrl", audioUrl);
                                        editor.putString("title", title);
                                        editor.putString("artist", artist);
                                        editor.putString("albumImageUrl", albumImageUrl != null ? albumImageUrl : "");
                                        editor.putString("albumName", albumName != null ? albumName : "");
                                        editor.putString("albumArtist", albumArtist != null ? albumArtist : "");
                                        editor.apply();

                                        FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
                                                getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);

                                        if (floatingPlayerFragment == null) {
                                            floatingPlayerFragment = new FloatingPlayerFragment();
                                            getSupportFragmentManager().beginTransaction()
                                                    .replace(R.id.floatingPlayerContainer, floatingPlayerFragment)
                                                    .commitNow();
                                        }

                                        if (floatingPlayerFragment.isAdded()) {
                                            floatingPlayerFragment.updatePlayer(audioUrl);
                                            floatingPlayerFragment.updateAlbumImage(albumImageUrl);
                                            findViewById(R.id.floatingPlayerContainer).setVisibility(View.VISIBLE);
                                            if (musicService != null && isServiceBound) {
                                                musicService.playSong(audioUrl);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}