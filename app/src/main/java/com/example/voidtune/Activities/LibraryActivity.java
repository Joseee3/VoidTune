package com.example.voidtune.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voidtune.entities.Album;
import com.example.voidtune.API.ApiClient;
import com.example.voidtune.API.ApiService;
import com.example.voidtune.R;
import com.example.voidtune.adapter.LibraryAdapter;
import com.example.voidtune.entities.Song;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView albumsRecyclerView;
    private LibraryAdapter libraryAdapter;
    private List<Album> albumItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        //Abrir Library
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
        //cargarDatosDesdeAPI();
        // Descomentar si se desea cargar desde Firebase
        cargarAlbumesDesdeFirebase();

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
}