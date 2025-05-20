package com.example.voidtune.Activities;

    import android.content.Intent;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.MenuItem;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.GridLayoutManager;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

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
    import com.google.android.material.bottomnavigation.BottomNavigationView;
    import com.google.firebase.analytics.FirebaseAnalytics;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;

    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;

    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;

    public class MainActivity extends AppCompatActivity {

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

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_home);

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

//        private void cargarAlbumYAbrirDetalle(String albumId) {
//            if (albumId == null || albumId.isEmpty()) {
//                Log.e("Firebase", "El albumId es nulo o está vacío");
//                return;
//            }
//
//            Log.d("Firebase", "Cargando detalles del álbum con ID: " + albumId);
//
//            // Cargar detalles del álbum desde Firebase
//            databaseReference.child("albums").child(albumId).get().addOnCompleteListener(task -> {
//                if (task.isSuccessful() && task.getResult() != null) {
//                    DataSnapshot albumSnapshot = task.getResult();
//                    String albumName = albumSnapshot.child("name").getValue(String.class);
//                    String albumImage = albumSnapshot.child("imageURL").getValue(String.class);
//                    List<Song> songs = new ArrayList<>();
//
//                    // Recuperar los IDs de las canciones
//                    for (DataSnapshot songSnapshot : albumSnapshot.child("songs").getChildren()) {
//                        String songId = songSnapshot.getValue(String.class);
//                        if (songId != null && !songId.isEmpty()) {
//                            // Recuperar detalles de la canción desde el nodo "songs"
//                            databaseReference.child("songs").child(songId).get().addOnCompleteListener(songTask -> {
//                                if (songTask.isSuccessful() && songTask.getResult() != null) {
//                                    DataSnapshot songDetails = songTask.getResult();
//                                    String songName = songDetails.child("name").getValue(String.class);
//                                    String songArtist = songDetails.child("artist").getValue(String.class);
//                                    String songAudioURL = songDetails.child("audioURL").getValue(String.class);
//                                    String songDuration = songDetails.child("duration").getValue(String.class);
//
//                                    // Crear un objeto Song y agregarlo a la lista
//                                    songs.add(new Song(songId, songName, songArtist, songAudioURL, songDuration));
//
//                                    // Si se han cargado todas las canciones, abrir la actividad
//                                    if (songs.size() == albumSnapshot.child("songs").getChildrenCount()) {
//                                        abrirDetalleAlbum(albumId, albumName, albumImage, songs);
//                                    }
//                                } else {
//                                    Log.e("Firebase", "Error al cargar detalles de la canción: " + songTask.getException().getMessage());
//                                }
//                            });
//                        } else {
//                            Log.e("Firebase", "ID de canción nulo o vacío en el álbum " + albumId);
//                        }
//                    }
//                } else {
//                    Log.e("Firebase", "Error al cargar detalles del álbum: " + task.getException().getMessage());
//                }
//            });
//        }
//
//        private void abrirDetalleAlbum(String albumId, String albumName, String albumImage, List<Song> songs) {
//            Intent intent = new Intent(MainActivity.this, DetailAlbumActivity.class);
//            intent.putExtra("albumId", albumId);
//            intent.putExtra("albumName", albumName);
//            intent.putExtra("albumImage", albumImage);
//            intent.putParcelableArrayListExtra("songs", new ArrayList<>(songs));
//            startActivity(intent);
//        }
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

            // Initialize the list
            List<LibraryItem> libraryItems = new ArrayList<>();

            // Bind libraryRecyclerView
            RecyclerView libraryRecyclerView = findViewById(R.id.libraryRecyclerView);
            if (libraryRecyclerView == null) {
                Log.e("Error", "libraryRecyclerView no está definido en el diseño.");
                return;
            }
            libraryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

         userRef.child("playlists").get().addOnCompleteListener(task -> {
             if (task.isSuccessful() && task.getResult() != null) {
                 for (DataSnapshot playlistSnapshot : task.getResult().getChildren()) {
                     String playlistId = playlistSnapshot.getKey();
                     String name = playlistSnapshot.child("name").getValue(String.class);
                     String imageUrl = playlistSnapshot.child("imageURL").getValue(String.class);

                     if (imageUrl == null || imageUrl.isEmpty()) {
                         imageUrl = "url_to_default_playlist_image";
                     }

                     libraryItems.add(new LibraryItem(playlistId, name, imageUrl, "playlist"));
                 }

                 // Actualizar adaptador
                 PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, libraryItems);
                 libraryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columnas
                 libraryRecyclerView.setAdapter(playlistAdapter);

                 playlistAdapter.setOnItemClickListener(item -> {
                     Intent intent = new Intent(this, DetailPlaylistActivity.class);
                     intent.putExtra("playlistId", item.getId());
                     intent.putExtra("playlistName", item.getTitle());
                     intent.putExtra("playlistImage", item.getImageUrl());
                     intent.putExtra("type", "playlist");
                     startActivity(intent);
                 });

                 playlistAdapter.notifyDataSetChanged();
             } else {
                 Log.e("Firebase", "Error al cargar playlists: " + task.getException());
             }
         });

        }
    }