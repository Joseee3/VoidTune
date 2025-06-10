package com.example.voidtune.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.BaseActivity;
import com.example.voidtune.FloatingPlayerFragment;
import com.example.voidtune.adapter.LibraryAdapter;
import com.example.voidtune.adapter.PlaylistAdapter;
import com.example.voidtune.entities.Song;
import com.example.voidtune.R;
import com.example.voidtune.adapter.CategoryAdapter;
import com.example.voidtune.adapter.HomeListAdapter;
import com.example.voidtune.adapter.LibraryListAdapter;
import com.example.voidtune.API.ApiClient;
import com.example.voidtune.API.ApiService;
import com.example.voidtune.entities.Album;
import com.example.voidtune.entities.LibraryItem;
import com.example.voidtune.service.MusicService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    private MusicService musicService;
    private boolean isServiceBound = false;

    private DatabaseReference databaseReference;

    private PlaylistAdapter playlistAdapter;

    private FirebaseAnalytics mFirebaseAnalytics;
    private List<Album> albumItems = new ArrayList<>();
    private LibraryListAdapter libraryAdapter;
    private HomeListAdapter suggestionsAdapter;
    private HomeListAdapter mostPlayedAdapter;
    private HomeListAdapter recentMusicAdapter;
    private HomeListAdapter moreOfWhatYouLikeAdapter;
    private HomeListAdapter madeForYouAdapter;

    private LinearLayout floatingPlayer;
    private TextView songTitle, artistName;
    private ImageView albumImage;
    private MediaPlayer mediaPlayer;

    private String currentAudioUrl;


    private static final int REQUEST_CODE_DETAIL_PLAYLIST = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        // Recuperar datos del Intent
        ArrayList<String> playlist = getIntent().getStringArrayListExtra("songs");
        int currentSongIndex = 0; // Índice inicial (puedes ajustarlo según sea necesario)

        if (playlist != null && !playlist.isEmpty()) {
            Log.d("DetailAlbumActivity", "Playlist recibida: " + playlist);

            // Verificar si es la primera o última canción
            if (currentSongIndex == 0) {
                Log.d("DetailAlbumActivity", "Es la primera canción.");
            } else if (currentSongIndex == playlist.size() - 1) {
                Log.d("DetailAlbumActivity", "Es la última canción.");
            } else {
                Log.d("DetailAlbumActivity", "Canción siguiente: " + playlist.get(currentSongIndex + 1));
            }
        } else {
            Log.e("DetailAlbumActivity", "No se recibieron canciones en el Intent.");
        }


        // Cargar el FloatingPlayerFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.floatingPlayerContainer, new FloatingPlayerFragment())
                .commit();


        TextView greetingText = findViewById(R.id.greetingText);

        // Get the current hour
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // Determine the greeting based on the time
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Buen día";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Buenas tardes";
        } else {
            greeting = "Buenas noches";
        }

        // Get the user's name from Firebase Realtime Database
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid(); // Get the user's UID
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            StringBuilder greetingBuilder = new StringBuilder(greeting);

            userRef.child("username").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String userName = task.getResult().getValue(String.class);
                    if (userName != null && !userName.isEmpty()) {
                        // Capitalize the first letter
                        String capitalizedUserName = userName.substring(0, 1).toUpperCase() + userName.substring(1).toLowerCase();
                        greetingBuilder.append(" ").append(capitalizedUserName); // Append the username
                        greetingText.setText(greetingBuilder.toString()); // Update the TextView
                    } else {
                        Log.e("Firebase", "Username is null or empty.");
                    }
                } else {
                    Log.e("Firebase", "Failed to fetch username: " + task.getException());
                }
            });
        } else {
            Log.e("Firebase", "User is not authenticated.");
        }

        // Set the greeting text
        greetingText.setText(greeting);

        // Configuración del RecyclerView para las categorías (horizontal)
        RecyclerView categoryRecyclerView = findViewById(R.id.carouselRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<String> categories = Arrays.asList("Música", "Podcasts", "Audiolibros", "Noticias", "Deportes");
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, categories);
        categoryRecyclerView.setAdapter(categoryAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        //Abrir Library
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_home) {
                    startActivity(new Intent(MainActivity.this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.menu_search) {
                    // Acción para el menú Search
                    return true;
                } else if (itemId == R.id.menu_library) {
                    startActivity(new Intent(MainActivity.this, LibraryActivity.class));
                    return true;
                }
                return false;
            }
        });

        // Configuración de los otros RecyclerView con item_home.xml
        inicializarRecyclerViews();

        // Cargar datos de Firebase
        cargarListasDinamicas();

        cargarLibraryItemsDesdeFirebase();

    }


    private void inicializarRecyclerViews() {
        suggestionsAdapter = configurarRecyclerViewDinamico(R.id.suggestionsRecyclerView);
        mostPlayedAdapter = configurarRecyclerViewDinamico(R.id.mostPlayedRecyclerView);
        recentMusicAdapter = configurarRecyclerViewDinamico(R.id.recentMusicRecyclerView);
        moreOfWhatYouLikeAdapter = configurarRecyclerViewDinamico(R.id.moreOfWhatYouLikeRecyclerView);
        madeForYouAdapter = configurarRecyclerViewDinamico(R.id.madeForYouRecyclerView);
    }

    private HomeListAdapter configurarRecyclerViewDinamico(int recyclerViewId) {
        RecyclerView recyclerView = findViewById(recyclerViewId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        HomeListAdapter adapter = new HomeListAdapter(this, new ArrayList<>());
        adapter.setOnItemClickListener(album -> cargarAlbumYAbrirDetalle(album.getId()));
        recyclerView.setAdapter(adapter);
        return adapter;
    }

    private void configurarRecyclerViewDinamico(int recyclerViewId, HomeListAdapter adapter) {
        RecyclerView recyclerView = findViewById(recyclerViewId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new HomeListAdapter(this, new ArrayList<>());
        adapter.setOnItemClickListener(album -> cargarAlbumYAbrirDetalle(album.getId()));
        recyclerView.setAdapter(adapter);
    }

    private void cargarListasDinamicas() {
        cargarListaDinamica("suggestion", suggestionsAdapter);
        cargarListaDinamica("mostplayed", mostPlayedAdapter);
        cargarListaDinamica("recentmusic", recentMusicAdapter);
        cargarListaDinamica("moreofwhatyoulike", moreOfWhatYouLikeAdapter);
        cargarListaDinamica("madeforyou", madeForYouAdapter);
    }

    private void cargarListaDinamica(String listaNombre, HomeListAdapter adapter) {
        databaseReference.child(listaNombre).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> albumIds = new ArrayList<>();
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String albumId = snapshot.getValue(String.class); // Leer el valor como String
                    if (albumId != null && !albumId.isEmpty()) {
                        albumIds.add(albumId);
                        Log.d("Firebase", "Album ID recuperado: " + albumId); // Log para depuración
                    } else {
                        Log.e("Firebase", "Album ID nulo o vacío en la lista " + listaNombre);
                    }
                }

                // Cargar detalles de los álbumes
                cargarDetallesDeAlbumes(albumIds, adapter);
            } else {
                Log.e("Firebase", "Error al cargar la lista " + listaNombre + ": " + task.getException().getMessage());
            }
        });
    }

    private void cargarDetallesDeAlbumes(List<String> albumIds, HomeListAdapter adapter) {
        List<Album> albums = new ArrayList<>();
        for (String albumId : albumIds) {
            databaseReference.child("albums").child(albumId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Album album = task.getResult().getValue(Album.class);
                    if (album != null) {
                        album.setId(albumId); // Asignar manualmente el ID
                        album.setImageUrl(task.getResult().child("imageURL").getValue(String.class)); // Asignar la URL de la imagen
                        albums.add(album);
                    } else {
                        Log.e("Firebase", "El álbum con ID " + albumId + " no existe en la base de datos");
                    }

                    // Actualizar el adaptador cuando se hayan cargado todos los álbumes
                    if (albums.size() == albumIds.size()) {
                        adapter.updateData(albums);
                    }
                } else {
                    Log.e("Firebase", "Error al cargar detalles del álbum con ID " + albumId + ": " + task.getException().getMessage());
                }
            });
        }
    }
    private void cargarAlbumYAbrirDetalle(String albumId) {
        if (albumId == null || albumId.isEmpty()) {
            Log.e("Firebase", "El albumId es nulo o está vacío");
            return;
        }

        Log.d("Firebase", "Cargando detalles del álbum con ID: " + albumId);

        // Cargar detalles del álbum desde Firebase
        databaseReference.child("albums").child(albumId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot albumSnapshot = task.getResult();
                String albumName = albumSnapshot.child("name").getValue(String.class);
                String albumImage = albumSnapshot.child("imageURL").getValue(String.class);
                List<String> songs = new ArrayList<>();
                for (DataSnapshot songSnapshot : albumSnapshot.child("songs").getChildren()) {
                    songs.add(songSnapshot.getValue(String.class));
                }

                // Iniciar reproducción
                iniciarReproduccion(songs, "album");

                // Crear un Intent para abrir DetailAlbumActivity
                Intent intent = new Intent(MainActivity.this, DetailAlbumActivity.class);
                intent.putExtra("albumId", albumId);
                intent.putExtra("albumName", albumName);
                intent.putExtra("albumImage", albumImage);
                intent.putStringArrayListExtra("songs", new ArrayList<>(songs));
                startActivity(intent);
            } else {
                Log.e("Firebase", "Error al cargar detalles del álbum: " + task.getException().getMessage());
            }
        });
    }
 private void abrirDetalle(Album album) {
        Intent intent = new Intent(MainActivity.this, DetailAlbumActivity.class);
        intent.putExtra("albumId", album.getId());
        intent.putExtra("albumName", album.getName());
        intent.putExtra("albumImage", album.getImageUrl()); // Pasar la URL de la imagen
        intent.putStringArrayListExtra("songs", new ArrayList<>(album.getSongs()));
        startActivity(intent);
    }

    private void cargarLibraryItemsDesdeFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("Firebase", "El usuario no está autenticado.");
            return;
        }

        String userId = currentUser.getUid();
        Log.d("Firebase", "ID del usuario autenticado: " + userId);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);


        // Inicializar el adaptador si no está inicializado
        if (playlistAdapter == null) {
            playlistAdapter = new PlaylistAdapter(this, new ArrayList<>());
        }

        // Configurar el RecyclerView si no está configurado
        RecyclerView libraryRecyclerView = findViewById(R.id.libraryRecyclerView);
        if (libraryRecyclerView == null) {
            Log.e("Error", "libraryRecyclerView no está definido en el diseño.");
            return;
        }
        if (libraryRecyclerView.getAdapter() == null) {
            libraryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columnas
            libraryRecyclerView.setAdapter(playlistAdapter);
        }

        libraryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columnas

        userRef.child("playlists").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<LibraryItem> libraryItems = new ArrayList<>(); // Limpiar lista antes de agregar nuevos datos
                for (DataSnapshot playlistSnapshot : task.getResult().getChildren()) {
                    String playlistId = playlistSnapshot.getKey();
                    String name = playlistSnapshot.child("name").getValue(String.class);
                    String imageUrl = playlistSnapshot.child("imageURL").getValue(String.class);

                    if ("likeSong".equals(playlistId)) {
                        imageUrl = "android.resource://" + getPackageName() + "/" + R.drawable.likesong;
                    } else if (imageUrl == null || imageUrl.isEmpty()) {
                        imageUrl = "url_to_default_playlist_image";
                    }

                    libraryItems.add(new LibraryItem(playlistId, name, imageUrl, "playlist"));
                }

                PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, libraryItems);
                libraryRecyclerView.setAdapter(playlistAdapter);

               playlistAdapter.setOnItemClickListener(item -> {
                   String playlistId = item.getId();
                   DatabaseReference playlistRef = userRef.child("playlists").child(playlistId).child("songs");

                   playlistRef.get().addOnCompleteListener(playlistTask -> {
                       if (playlistTask.isSuccessful() && playlistTask.getResult() != null) {
                           List<String> songIds = new ArrayList<>();
                           for (DataSnapshot songSnapshot : playlistTask.getResult().getChildren()) {
                               String songId = songSnapshot.getValue(String.class);
                               if (songId != null) {
                                   songIds.add(songId);
                               }
                           }

                           // Iniciar reproducción directamente
                           iniciarReproduccion(songIds, "playlist");

                           Intent intent = new Intent(this, DetailPlaylistActivity.class);
                           intent.putExtra("playlistId", playlistId);
                           intent.putExtra("playlistName", item.getTitle());
                           intent.putExtra("playlistImage", item.getImageUrl());
                           intent.putExtra("type", "playlist");
                           intent.putStringArrayListExtra("songs", new ArrayList<>(songIds));
                           startActivityForResult(intent, REQUEST_CODE_DETAIL_PLAYLIST);
                       } else {
                           Log.e("Firebase", "Error al cargar canciones de la playlist: " + playlistTask.getException());
                       }
                   });
               });
                playlistAdapter.notifyDataSetChanged();
            } else {
                Log.e("Firebase", "Error al cargar playlists: " + task.getException());
            }
        });
 }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DETAIL_PLAYLIST && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getBooleanExtra("playlistDeleted", false)) {
                    cargarLibraryItemsDesdeFirebase(); // Recargar datos al eliminar
                } else if (data.getBooleanExtra("playlistUpdated", false)) {
                    cargarLibraryItemsDesdeFirebase(); // Recargar datos al actualizar
                }
            }
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


//    private void fetchAndPlaySong(String songId) {
//        DatabaseReference songRef = FirebaseDatabase.getInstance().getReference("songs").child(songId);
//
//        songRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    Song song = snapshot.getValue(Song.class);
//                    if (song != null) {
//                        updateFloatingPlayer(song.getName(), song.getArtist(), R.drawable.img_album, song.audioURL);
//                    }
//                } else {
//                    Toast.makeText(MainActivity.this, "The song does not exist.", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(MainActivity.this, "Error loading data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    private void saveSongToSharedPreferences(String audioUrl, String title, String artist, String albumImageUrl) {
        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("currentAudioUrl", audioUrl);
        editor.putString("title", title);
        editor.putString("artist", artist);
        editor.putString("albumImageUrl", albumImageUrl);
        editor.apply();
    }

    private void updateFloatingPlayer(String songId) {
        currentAudioUrl = songId;

        FirebaseDatabase.getInstance().getReference("songs").child(songId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String audioUrl = task.getResult().child("audioURL").getValue(String.class);
                    String title = task.getResult().child("title").getValue(String.class);
                    String artist = task.getResult().child("artist").getValue(String.class);
                    String albumImageUrl = task.getResult().child("albumImage").getValue(String.class);

                    if (audioUrl != null && title != null && artist != null && albumImageUrl != null) {
                        // Guardar los datos en SharedPreferences
                        saveSongToSharedPreferences(audioUrl, title, artist, albumImageUrl);

                        // Actualizar el reproductor flotante directamente
                        FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
                            getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);

                        if (floatingPlayerFragment != null && floatingPlayerFragment.isAdded()) {
                            floatingPlayerFragment.updatePlayer(audioUrl);
                            findViewById(R.id.floatingPlayerContainer).setVisibility(View.VISIBLE);
                        } else {
                            Log.e("updateFloatingPlayer", "FloatingPlayerFragment no está disponible.");
                        }
                    } else {
                        Log.e("updateFloatingPlayer", "Datos incompletos para la canción.");
                    }
                } else {
                    Log.e("updateFloatingPlayer", "Error al cargar los datos de la canción: " + task.getException());
                }
            });
    }

    private void iniciarReproduccion(List<String> canciones, String sourceType) {
        Intent musicServiceIntent = new Intent(this, MusicService.class);
        musicServiceIntent.putStringArrayListExtra("playlist", new ArrayList<>(canciones));
        musicServiceIntent.putExtra("sourceType", sourceType); // "album" o "playlist"
        startService(musicServiceIntent);
    }

}