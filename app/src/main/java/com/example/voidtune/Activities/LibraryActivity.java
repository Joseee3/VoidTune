package com.example.voidtune.Activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.entities.Album;
import com.example.voidtune.API.ApiClient;
import com.example.voidtune.API.ApiService;
import com.example.voidtune.R;
import com.example.voidtune.adapter.LibraryAdapter;
import com.example.voidtune.entities.Song;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView albumsRecyclerView;
    private LibraryAdapter libraryAdapter;
    private List<Album> albumItems;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid(); // Obtén el UID del usuario actual
            FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("profileImage")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String profileImageUri = task.getResult().getValue(String.class);
                        if (profileImageUri != null && !profileImageUri.isEmpty()) {
                            ImageView userImage = findViewById(R.id.userImage);
                            Glide.with(this)
                                .load(profileImageUri)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(userImage);
                        }
                    } else {
                        Log.e("Firebase", "Error al obtener la imagen de perfil: " + task.getException().getMessage());
                    }
                });
        } else {
            Log.e("Auth", "No hay un usuario autenticado");
        }

        // Abrir Library
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_home) {
                    startActivity(new Intent(LibraryActivity.this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.menu_search) {
                    // Acción para el menú Search
                    return true;
                } else if (itemId == R.id.menu_library) {
                    startActivity(new Intent(LibraryActivity.this, LibraryActivity.class));
                    return true;
                }
                return false;
            }
        });

        // Configurar RecyclerView
        albumsRecyclerView = findViewById(R.id.albumsRecyclerView);
        albumsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar lista y adaptador
        albumItems = new ArrayList<>();
        libraryAdapter = new LibraryAdapter(this, albumItems);
        albumsRecyclerView.setAdapter(libraryAdapter);

        // Cargar datos
        cargarAlbumesDesdeFirebase();

        Button addPlaylistButton = findViewById(R.id.add_playlists_button);
        addPlaylistButton.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    private void cargarDatosDesdeAPI() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<Album>> call = apiService.getAlbums();

        call.enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Album> albums = response.body();
                    albumItems.clear();
                    albumItems.addAll(albums);
                    libraryAdapter.notifyDataSetChanged();
                    Log.d("API", "Se cargaron con éxito " + albums.size() + " álbumes");
                } else {
                    Log.e("API", "Error en la respuesta de la API: " + response.code());
                    Toast.makeText(LibraryActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                Log.e("API", "La llamada a la API falló: " + t.getMessage());
                Toast.makeText(LibraryActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }


   private void cargarAlbumesDesdeFirebase() {
       FirebaseDatabase.getInstance().getReference("albums")
           .get()
           .addOnCompleteListener(task -> {
               if (task.isSuccessful() && task.getResult() != null) {
                   albumItems.clear(); // Clear the list before adding new data
                   for (DataSnapshot albumSnapshot : task.getResult().getChildren()) {
                       String albumId = albumSnapshot.getKey();
                       String name = albumSnapshot.child("name").getValue(String.class);
                       String artist = albumSnapshot.child("artist").getValue(String.class);
                       String imageUrl = albumSnapshot.child("imageURL").getValue(String.class);
                       List<String> songs = new ArrayList<>();
                       for (DataSnapshot songSnapshot : albumSnapshot.child("songs").getChildren()) {
                           songs.add(songSnapshot.getValue(String.class));
                       }

                       Album album = new Album(albumId, name, artist, imageUrl, songs);
                       albumItems.add(album);
                   }
                   libraryAdapter.notifyDataSetChanged();
               } else {
                   Log.e("Firebase", "Error loading albums: " + task.getException().getMessage());
                   Toast.makeText(this, "Error loading albums", Toast.LENGTH_SHORT).show();
               }
           });
   }


   private void showCreatePlaylistDialog() {
       AlertDialog dialog = new AlertDialog.Builder(this).create();
       View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
       dialog.setView(dialogView);

       EditText input = dialogView.findViewById(R.id.playlistNameInput);
       Button createButton = dialogView.findViewById(R.id.createPlaylistButton);

       createButton.setOnClickListener(v -> {
           String playlistName = input.getText().toString().trim();
           if (!playlistName.isEmpty()) {
               createPlaylist(playlistName);
               dialog.dismiss();
           } else {
               Toast.makeText(this, "Playlist name cannot be empty.", Toast.LENGTH_SHORT).show();
           }
       });

       dialog.show();
   }


   private void createPlaylist(String playlistName) {
       FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
       if (currentUser != null) {
           String userId = currentUser.getUid(); // Obtén el UID del usuario actual
           DatabaseReference playlistsRef = FirebaseDatabase.getInstance()
               .getReference("users")
               .child(userId)
               .child("playlists"); // Nodo específico para playlists

           // Generar un ID único para la playlist
           String playlistId = playlistsRef.push().getKey();

           if (playlistId != null) {
               HashMap<String, Object> playlistData = new HashMap<>();
               playlistData.put("name", playlistName);
               playlistData.put("albums", new ArrayList<>()); // Lista vacía de álbumes

               playlistsRef.child(playlistId).setValue(playlistData)
                   .addOnCompleteListener(task -> {
                       if (task.isSuccessful()) {
                           Toast.makeText(this, "Playlist created successfully.", Toast.LENGTH_SHORT).show();
                       } else {
                           Toast.makeText(this, "Failed to create playlist: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                       }
                   });
           }
       } else {
           Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
       }
   }


}