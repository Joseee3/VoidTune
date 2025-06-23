package com.example.voidtune.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.BaseActivity;
import com.example.voidtune.FloatingPlayerFragment;
import com.example.voidtune.Fragments.PlaylistOptionsBottomSheet;
import com.example.voidtune.R;
import com.example.voidtune.VIewModel.MusicViewModel;
import com.example.voidtune.adapter.SongAdapter;
import com.example.voidtune.entities.Song;
import com.example.voidtune.service.MusicService;
import com.example.voidtune.service.PlayerService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.palette.graphics.Palette;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DetailPlaylistActivity extends BaseActivity {

    private SongAdapter songAdapter;
    private ArrayList<Song> songs;

    private MusicService musicService;
    private boolean isServiceBound = false;

    private boolean isPlaying = false;

    private String currentAudioUrl;
    private String currentAlbumId; // Variable para almacenar el ID del álbum actual

    private MusicViewModel musicViewModel;

    private List<String> playlist = new ArrayList<>(); // Lista para almacenar los IDs de las canciones de la playlist



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainedInstance(true);
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        setContentView(R.layout.detail_album);




//        // Inicializar MusicViewModel
//        musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);
//        // Observar cambios en el ID de la canción actual
//        musicViewModel.getCurrentSongId().observe(this, songId -> {
//            if (songId != null) {
//                updateFloatingPlayer(songId); // Actualiza el reproductor flotante
//            }
//        });
//
//        // Observar el estado de reproducción
//        musicViewModel.getIsPlaying().observe(this, isPlaying -> {
//            // Actualiza la UI según el estado de reproducción
//        });


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

        ImageView moreOptionsButton = findViewById(R.id.moreOptionsButton);
        String type = getIntent().getStringExtra("type");


        if ("likeSong".equals(type)) {

            titleTextView.setText("Tus me gusta");
            setContentView(R.layout.detail_likesong);

            playlistImageView.setImageResource(R.drawable.likesong);
            loadPlaylistSongs("likeSong");




        } else if ("playlist".equals(type)) {
            titleTextView.setText(playlistName != null ? playlistName : "Playlist sin nombre");
            moreOptionsButton.setVisibility(View.VISIBLE); // Show for playlists

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



         moreOptionsButton = findViewById(R.id.moreOptionsButton);
        moreOptionsButton.setOnClickListener(v -> {
            PlaylistOptionsBottomSheet bottomSheet = new PlaylistOptionsBottomSheet();
            bottomSheet.setOnOptionSelectedListener(new PlaylistOptionsBottomSheet.OnOptionSelectedListener() {
                @Override
                public void onEditPlaylist() {
                    String playlistId = getIntent().getStringExtra("playlistId");
                    String playlistName = getIntent().getStringExtra("playlistName");

                    if (playlistId != null && playlistName != null) {
                        showEditPlaylistDialog(playlistId, playlistName);
                    } else {
                        Toast.makeText(DetailPlaylistActivity.this, "No se pudo cargar la información de la playlist.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDeletePlaylist() {
                    // Lógica para eliminar la playlist
                    deletePlaylist(playlistId);
                }
            });
            bottomSheet.show(getSupportFragmentManager(), "PlaylistOptionsBottomSheet");
        });



        //Abrir Library
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_home) {
                    startActivity(new Intent(DetailPlaylistActivity.this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.menu_search) {
                    // Acción para el menú Search
                    startActivity(new Intent(DetailPlaylistActivity.this, SearchActivity.class));
                    return true;
                } else if (itemId == R.id.menu_library) {
                    startActivity(new Intent(DetailPlaylistActivity.this, LibraryActivity.class));
                    return true;
                }
                return false;
            }
        });


//        songAdapter.setOnSongClickListener(song -> {
//            Log.d("SongClick", "Song ID: " + song.getId());
//
//            DatabaseReference albumRef = FirebaseDatabase.getInstance()
//                    .getReference("albums")
//                    .child(song.getAlbumId());
//
//            albumRef.child("imageURL").get().addOnCompleteListener(task -> {
//                String albumImageUrl = null;
//                if (task.isSuccessful() && task.getResult() != null) {
//                    albumImageUrl = task.getResult().getValue(String.class);
//                }
//
//                // Save all song data for the floating player
//                SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                ArrayList<String> playlistIds = new ArrayList<>();
//                for (Song s : songs) playlistIds.add(s.getId());
//                editor.putStringSet("playlist", new HashSet<>(playlistIds));
//                editor.putString("currentAudioUrl", song.getAudioURL());
//                editor.putString("title", song.getName());
//                editor.putString("artist", song.getArtist());
//                editor.putString("albumImageUrl", albumImageUrl);
//                editor.apply();
//
//                // Start playback
//                if (musicService != null && isServiceBound) {
//                    musicService.replaceAndPlaySong(song.getId(), playlistIds);
//                } else {
//                    Toast.makeText(this, "El servicio de música no está disponible.", Toast.LENGTH_SHORT).show();
//                }
//
//                // Update the floating player (it will read from SharedPreferences)
//                loadFloatingPlayer();
//            });
//        });

       songAdapter.setOnSongClickListener(song -> {
           String albumId = song.getAlbumID();
           DatabaseReference albumRef = FirebaseDatabase.getInstance()
               .getReference("albums")
               .child(albumId);

           albumRef.get().addOnCompleteListener(task -> {
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
              // Save all song and album data for the floating player
              SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
              SharedPreferences.Editor editor = sharedPreferences.edit();
              ArrayList<String> playlistIds = new ArrayList<>();
              for (Song s : songs) playlistIds.add(s.getId());
              editor.putStringSet("playlist", new HashSet<>(playlistIds));
              editor.putString("currentAudioUrl", song.getAudioURL());
              editor.putString("title", song.getName());
              editor.putString("artist", song.getArtist());
              editor.putString("albumImageUrl", albumImageUrl != null ? albumImageUrl : "");
              editor.putString("albumName", albumName != null ? albumName : "");
              editor.putString("albumArtist", albumArtist != null ? albumArtist : "");
              editor.apply();

               // After editor.apply();
               // After saving to SharedPreferences
               FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
                   getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);

               if (floatingPlayerFragment != null && floatingPlayerFragment.isAdded()) {
                   floatingPlayerFragment.updatePlayer(song.getAudioURL());
                   floatingPlayerFragment.updateAlbumImage(albumImageUrl);
                   findViewById(R.id.floatingPlayerContainer).setVisibility(View.VISIBLE);
               }

               // Start playback
               if (musicService != null && isServiceBound) {
                   musicService.replaceAndPlaySong(song.getId(), playlistIds);
               } else {
                   Toast.makeText(DetailPlaylistActivity.this, "El servicio de música no está disponible.", Toast.LENGTH_SHORT).show();
               }

               updateFloatingPlayer(song.getId());
           });
       });
    }



    private void setRetainedInstance(boolean b) {
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
                    String songId = songSnapshot.getValue(String.class);
                    if (songId != null) {
                        songIds.add(songId);
                    }
                }

                if (songIds.isEmpty()) {
                    Log.e("DetailPlaylist", "La lista de reproducción está vacía.");
                } else {
                    Log.d("DetailPlaylist", "Lista de reproducción cargada con " + songIds.size() + " canciones.");

                    // Iniciar el servicio MusicService con la lista de reproducción
                    Intent intent = new Intent(this, MusicService.class);
                    intent.putStringArrayListExtra("playlist", new ArrayList<>(songIds));
                    intent.putExtra("sourceType", "playlist");
                    intent.putExtra("shouldStartPlayback", false); // Evitar reproducción automática
                    startService(intent);
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
                        if (task.getResult().hasChild("albumID")) {
                            song.setAlbumID(task.getResult().child("albumID").getValue(String.class)); // Asigna el albumId
                        }
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


    private void deletePlaylist(String playlistId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference playlistRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("playlists")
                .child(playlistId);

            playlistRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Playlist eliminada correctamente.", Toast.LENGTH_SHORT).show();
                    // Notificar a la actividad anterior
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("playlistDeleted", true);
                    resultIntent.putExtra("deletedPlaylistId", playlistId);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Cierra la actividad actual
                } else {
                    Log.e("DeletePlaylist", "Error al eliminar la playlist: " + task.getException());
                    Toast.makeText(this, "Error al eliminar la playlist.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditPlaylistDialog(String playlistId, String currentName) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        dialog.setView(dialogView);

        EditText input = dialogView.findViewById(R.id.playlistNameInput);
        Button createButton = dialogView.findViewById(R.id.createPlaylistButton);

        // Configurar el diálogo para edición
        input.setText(currentName);
        createButton.setText("Actualizar");

        createButton.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updatePlaylistName(playlistId, newName);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "El nombre de la playlist no puede estar vacío.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updatePlaylistName(String playlistId, String newName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference playlistNameRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("playlists")
                .child(playlistId)
                .child("name");

            playlistNameRef.setValue(newName).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Nombre de la playlist actualizado.", Toast.LENGTH_SHORT).show();
                    // Notificar a la actividad anterior
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("playlistUpdated", true);
                    resultIntent.putExtra("updatedPlaylistId", playlistId);
                    resultIntent.putExtra("updatedPlaylistName", newName);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Cierra la actividad actual
                } else {
                    Toast.makeText(this, "Error al actualizar el nombre: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show();
        }
    }



    private void adaptBackgroundToImage(String imageUrl) {
        ImageView playlistImageView = findViewById(R.id.albumCover);
        View backgroundView = findViewById(R.id.backgroundView); // Cambia al ID de tu fondo

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        playlistImageView.setImageBitmap(resource);

                        // Extraer colores con Palette
                        Palette.from(resource).generate(palette -> {
                            if (palette != null) {
                                int vibrantColor = palette.getVibrantColor(Color.BLACK); // Color vibrante
                                backgroundView.setBackgroundColor(vibrantColor); // Aplica el color al fondo
                            }
                        });
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Manejo si es necesario
                    }
                });
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
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
    currentAudioUrl = sharedPreferences.getString("currentAudioUrl", null);
    int savedPosition = sharedPreferences.getInt("currentPosition", 0);
    boolean isPlaying = sharedPreferences.getBoolean("isPlaying", false);
    // Recupera la playlist
    List<String> playlist = new ArrayList<>(sharedPreferences.getStringSet("playlist", new HashSet<>()));

    if (isServiceBound && musicService != null && currentAudioUrl != null) {
        musicService.restoreState();
        musicService.setPlaylist(playlist); // Asegúrate de que tu servicio tenga este método
        musicService.seekTo(savedPosition);
        if (isPlaying) {
            musicService.playSong(currentAudioUrl);
        } else {
            musicService.pauseSong();
        }
    }

    loadFloatingPlayer();
}

    @Override
    protected void onStop() {
        super.onStop();

        if (isServiceBound && musicService != null) {
            // Guarda el estado del reproductor en SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("currentAudioUrl", musicService.getCurrentAudioUrl());
            editor.putInt("currentPosition", musicService.getCurrentPosition());
            editor.putBoolean("isPlaying", musicService.isPlaying());
            editor.apply();

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



//  private void updateFloatingPlayer(String songId) {
//      currentAudioUrl = songId;
//
//      FirebaseDatabase.getInstance().getReference("songs").child(songId)
//          .get()
//          .addOnCompleteListener(task -> {
//              if (task.isSuccessful() && task.getResult() != null) {
//                  String audioUrl = task.getResult().child("audioURL").getValue(String.class);
//                  String title = task.getResult().child("title").getValue(String.class);
//                  String artist = task.getResult().child("artist").getValue(String.class);
//                  String albumImageUrl = task.getResult().child("albumImage").getValue(String.class);
//
//                  if (audioUrl != null && title != null && artist != null && albumImageUrl != null) {
//                      // Guardar los datos en SharedPreferences
//                      saveSongToSharedPreferences(audioUrl, title, artist, albumImageUrl);
//
//                      // Actualizar el reproductor flotante directamente
//                      FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
//                          getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);
//
//                      if (floatingPlayerFragment != null && floatingPlayerFragment.isAdded()) {
//                          floatingPlayerFragment.updatePlayer(audioUrl);
//                          findViewById(R.id.floatingPlayerContainer).setVisibility(View.VISIBLE);
//                      } else {
//                          Log.e("updateFloatingPlayer", "FloatingPlayerFragment no está disponible.");
//                      }
//                  } else {
//                      Log.e("updateFloatingPlayer", "Datos incompletos para la canción.");
//                  }
//              } else {
//                  Log.e("updateFloatingPlayer", "Error al cargar los datos de la canción: " + task.getException());
//              }
//          });
//  }

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

   private void saveSongToSharedPreferences(String audioUrl, String title, String artist, String albumImageUrl) {
       SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
       SharedPreferences.Editor editor = sharedPreferences.edit();
       editor.putString("currentAudioUrl", audioUrl);
       editor.putString("title", title);
       editor.putString("artist", artist);
       editor.putString("albumImageUrl", albumImageUrl);
       editor.apply();
   }

   private void iniciarReproduccion(List<String> canciones, String sourceType) {
       Intent musicServiceIntent = new Intent(this, MusicService.class);
       musicServiceIntent.putStringArrayListExtra("playlist", new ArrayList<>(canciones));
       musicServiceIntent.putExtra("sourceType", sourceType); // "album" o "playlist"
       startService(musicServiceIntent);
   }

    @Override
    protected void onResume () {
        super.onResume();
        FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
                getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);

        if (floatingPlayerFragment != null) {
            floatingPlayerFragment.updatePlayer(currentAudioUrl); // Pass the songId or audioUrl
        }
    }





}