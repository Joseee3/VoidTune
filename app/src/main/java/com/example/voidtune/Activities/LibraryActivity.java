package com.example.voidtune.Activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.adapter.PlaylistAdapter;
import com.example.voidtune.entities.Album;
import com.example.voidtune.API.ApiClient;
import com.example.voidtune.API.ApiService;
import com.example.voidtune.R;
import com.example.voidtune.adapter.LibraryAdapter;
import com.example.voidtune.entities.LibraryItem;
import com.example.voidtune.entities.Song;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
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

    private DrawerLayout drawerLayout;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        ProgressBar progressBar = findViewById(R.id.progressBar);

        //Configurar el DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        // Manejar el clic en el botón de navegación
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_settings) {
                // Abrir actividad de configuración
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (id == R.id.menu_logout) {
                progressBar.setVisibility(View.VISIBLE); // Mostrar el ProgressBar

                FirebaseAuth.getInstance().signOut();

                // Limpiar SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                // Redirigir al inicio de sesión y limpiar la pila de actividades
                Intent intent = new Intent(this, Login.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                progressBar.setVisibility(View.GONE); // Ocultar el ProgressBar
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        //Agregar el botón de navegación al DrawerLayout
        ImageView navigationButton = findViewById(R.id.userImage);
        navigationButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
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
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_home) {
                startActivity(new Intent(LibraryActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.menu_search) {
                return true;
            } else if (itemId == R.id.menu_library) {
                startActivity(new Intent(LibraryActivity.this, LibraryActivity.class));
                return true;
            }
            return false;
        });

        // Configurar RecyclerView
        albumsRecyclerView = findViewById(R.id.albumsRecyclerView);
        albumsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Cargar datos
        cargarLibraryItemsDesdeFirebase();

        Button addPlaylistButton = findViewById(R.id.add_playlists_button);
        addPlaylistButton.setOnClickListener(v -> showCreatePlaylistDialog());


        ImageView addIcon = findViewById(R.id.add_icon);
        addIcon.setOnClickListener(v -> showCreatePlaylistDialog());


        Button likeSongButton = findViewById(R.id.likeSongButton);
        likeSongButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailPlaylistActivity.class);
            intent.putExtra("type", "likeSong");
            startActivity(intent);
        });
    }


  private void cargarLibraryItemsDesdeFirebase() {
        // Inicializa el RecyclerView
        RecyclerView libraryRecyclerView = findViewById(R.id.albumsRecyclerView);

        if (libraryRecyclerView == null) {
            Log.e("LibraryActivity", "libraryRecyclerView is null. Check the XML layout.");
            return;
        }
        libraryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("Firebase", "User is not authenticated.");
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("playlists");

        List<LibraryItem> libraryItems = new ArrayList<>();

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DataSnapshot playlistSnapshot : task.getResult().getChildren()) {
                    String playlistId = playlistSnapshot.getKey();
                    String name = playlistSnapshot.child("name").getValue(String.class);
                    String imageUrl = playlistSnapshot.child("imageURL").getValue(String.class);

                    if (name != null && !name.isEmpty()) {
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = "url_to_default_playlist_image"; // Imagen por defecto
                        }
                        libraryItems.add(new LibraryItem(playlistId, name, imageUrl, "playlist"));
                    }
                }

                // Configura el adaptador con el RecyclerView correcto
                PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, libraryItems);
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
                Log.e("Firebase", "Error loading playlists: " + task.getException());
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
                           cargarLibraryItemsDesdeFirebase(); // Actualiza la lista
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