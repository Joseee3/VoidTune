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

        // Configuración del RecyclerView para las bibliotecas (cuadrícula de 2 columnas)
        RecyclerView libraryRecyclerView = findViewById(R.id.libraryRecyclerView);
        libraryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Inicializar con datos temporales
        libraryAdapter = new LibraryListAdapter(this, albumItems);
        libraryRecyclerView.setAdapter(libraryAdapter);


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

        // Cargar datos de la API
        //obtenerAlbumesDeLaApi();
        // Cargar datos de Firebase
        obtenerAlbumesDesdeFirebase();
    }

    private void inicializarRecyclerViews() {
        // Configuración para Suggestions
        RecyclerView suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        suggestionsAdapter = new HomeListAdapter(this, new ArrayList<>());
        suggestionsAdapter.setOnItemClickListener(album -> {
            cargarAlbumYAbrirDetalle(album.getId());
        });
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);

        // Configuración para Most Played
        RecyclerView mostPlayedRecyclerView = findViewById(R.id.mostPlayedRecyclerView);
        mostPlayedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mostPlayedAdapter = new HomeListAdapter(this, new ArrayList<>());
        mostPlayedAdapter.setOnItemClickListener(album -> {
            cargarAlbumYAbrirDetalle(album.getId());
        });
        mostPlayedRecyclerView.setAdapter(mostPlayedAdapter);

        // Configuración para Recent Music
        RecyclerView recentMusicRecyclerView = findViewById(R.id.recentMusicRecyclerView);
        recentMusicRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentMusicAdapter = new HomeListAdapter(this, new ArrayList<>());
        recentMusicAdapter.setOnItemClickListener(album -> {
            cargarAlbumYAbrirDetalle(album.getId());
        });
        recentMusicRecyclerView.setAdapter(recentMusicAdapter);

        // Configuración para More of What You Like
        RecyclerView moreOfWhatYouLikeRecyclerView = findViewById(R.id.moreOfWhatYouLikeRecyclerView);
        moreOfWhatYouLikeRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        moreOfWhatYouLikeAdapter = new HomeListAdapter(this, new ArrayList<>());
        moreOfWhatYouLikeAdapter.setOnItemClickListener(album -> {
            cargarAlbumYAbrirDetalle(album.getId());
        });
        moreOfWhatYouLikeRecyclerView.setAdapter(moreOfWhatYouLikeAdapter);

        // Configuración para Made for You
        RecyclerView madeForYouRecyclerView = findViewById(R.id.madeForYouRecyclerView);
        madeForYouRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        madeForYouAdapter = new HomeListAdapter(this, new ArrayList<>());
        madeForYouAdapter.setOnItemClickListener(album -> {
            cargarAlbumYAbrirDetalle(album.getId());
        });
        madeForYouRecyclerView.setAdapter(madeForYouAdapter);
    }

    private void cargarAlbumYAbrirDetalle(String albumId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Album> call = apiService.getAlbumById(albumId);

        call.enqueue(new Callback<Album>() {
            @Override
            public void onResponse(Call<Album> call, Response<Album> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Album album = response.body();
                    abrirDetalle(album); // Llama a abrirDetalle con el álbum completo
                } else {
                    Log.e("API", "Error al cargar álbum: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Album> call, Throwable t) {
                Log.e("API", "Fallo al obtener álbum: " + t.getMessage());
            }
        });
    }

    private void obtenerAlbumesDeLaApi() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<Album>> call = apiService.getAlbums();

        call.enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Album> albums = response.body();
                    actualizarUIConAlbumes(albums);
                    Log.d("API", "Se cargaron con éxito " + albums.size() + " álbumes");
                } else {
                    Log.e("API", "Error en la respuesta de la API: " + response.code());
                    cargarDatosPorDefecto();
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                Log.e("API", "La llamada a la API falló: " + t.getMessage());
                cargarDatosPorDefecto();
            }
        });
    }

    private void actualizarAdaptador(HomeListAdapter adapter, List<Album> albums) {
        RecyclerView parent = null;

        if (adapter == suggestionsAdapter) {
            parent = findViewById(R.id.suggestionsRecyclerView);
        } else if (adapter == mostPlayedAdapter) {
            parent = findViewById(R.id.mostPlayedRecyclerView);
        } else if (adapter == recentMusicAdapter) {
            parent = findViewById(R.id.recentMusicRecyclerView);
        } else if (adapter == moreOfWhatYouLikeAdapter) {
            parent = findViewById(R.id.moreOfWhatYouLikeRecyclerView);
        } else if (adapter == madeForYouAdapter) {
            parent = findViewById(R.id.madeForYouRecyclerView);
        }

        if (parent != null) {
            HomeListAdapter newAdapter = new HomeListAdapter(this, albums);
            newAdapter.setOnItemClickListener(album -> abrirDetalle(album));
            parent.setAdapter(newAdapter);

            if (adapter == suggestionsAdapter) {
                suggestionsAdapter = newAdapter;
            } else if (adapter == mostPlayedAdapter) {
                mostPlayedAdapter = newAdapter;
            } else if (adapter == recentMusicAdapter) {
                recentMusicAdapter = newAdapter;
            } else if (adapter == moreOfWhatYouLikeAdapter) {
                moreOfWhatYouLikeAdapter = newAdapter;
            } else if (adapter == madeForYouAdapter) {
                madeForYouAdapter = newAdapter;
            }
        }
    }

    private void cargarDatosPorDefecto() {
        List<Album> defaultAlbums = new ArrayList<>();
        List<LibraryItem> placeholders = Arrays.asList(
            new LibraryItem(R.drawable.foto_carousel, "Biblioteca 1"),
            new LibraryItem(R.drawable.foto_carousel, "Biblioteca 2"),
            new LibraryItem(R.drawable.foto_carousel, "Biblioteca 3"),
            new LibraryItem(R.drawable.foto_carousel, "Biblioteca 4")
        );

        for (LibraryItem item : placeholders) {
            defaultAlbums.add(new Album("", item.getTitle(), "Artista Desconocido", item.getImageUrl(), new ArrayList<>()));
        }

        configurarRecyclerView(R.id.suggestionsRecyclerView, defaultAlbums);
        configurarRecyclerView(R.id.mostPlayedRecyclerView, defaultAlbums);
        configurarRecyclerView(R.id.recentMusicRecyclerView, defaultAlbums);
        configurarRecyclerView(R.id.moreOfWhatYouLikeRecyclerView, defaultAlbums);
        configurarRecyclerView(R.id.madeForYouRecyclerView, defaultAlbums);
    }




private void configurarRecyclerView(int recyclerViewId, List<Album> albums) {
    RecyclerView recyclerView = findViewById(recyclerViewId);
    recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    HomeListAdapter adapter = new HomeListAdapter(this, albums);
    adapter.setOnItemClickListener(album -> abrirDetalle(album));
    recyclerView.setAdapter(adapter);
}


   private void actualizarUIConAlbumes(List<Album> albums) {
        // Limpiar elementos existentes
        albumItems.clear();

        // Convertir álbumes a LibraryItems
        List<LibraryItem> libraryItems = new ArrayList<>();
        List<LibraryItem> homeItems = new ArrayList<>();

        for (Album album : albums) {
            LibraryItem item = new LibraryItem(album.getImageUrl(), album.getName(), album.getArtist());

            // Añadir algunos álbumes a la cuadrícula de biblioteca
            if (libraryItems.size() < 4) {
                libraryItems.add(item);
            }

            // Añadir todos los álbumes a los elementos de inicio
            homeItems.add(item);
        }

        // Actualizar cuadrícula de biblioteca
        albumItems.addAll(albums); // Usar directamente la lista de álbumes
        libraryAdapter.setOnItemClickListener(new LibraryListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Album album) {
                abrirDetalle(album);
            }
        });
        libraryAdapter.notifyDataSetChanged();



        // Actualizar todas las listas horizontales con diferentes subconjuntos de datos
        actualizarAdaptador(suggestionsAdapter, albums);
        actualizarAdaptador(mostPlayedAdapter, albums);
        actualizarAdaptador(recentMusicAdapter, albums);
        actualizarAdaptador(moreOfWhatYouLikeAdapter, albums);
        actualizarAdaptador(madeForYouAdapter, albums);
    }

    private void abrirDetalle(Album album) {
        Intent intent = new Intent(MainActivity.this, DetailAlbumActivity.class);
        intent.putExtra("albumId", album.getId()); // Pasar el ID del álbum
        intent.putExtra("albumName", album.getName());
        intent.putExtra("albumImage", album.getImageUrl());
        intent.putStringArrayListExtra("songs", new ArrayList<>(album.getSongs())); // Pasar lista de IDs de canciones
        startActivity(intent);
    }



    //FIREBASE


    private void obtenerAlbumesDesdeFirebase() {
        databaseReference.child("albums").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Album> albums = new ArrayList<>();
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String albumId = snapshot.getKey();
                    String name = snapshot.child("name").getValue(String.class);
                    String artist = snapshot.child("artist").getValue(String.class);
                    String imageUrl = snapshot.child("imageURL").getValue(String.class);
                    List<String> songs = new ArrayList<>();
                    for (DataSnapshot songSnapshot : snapshot.child("songs").getChildren()) {
                        songs.add(songSnapshot.getValue(String.class));
                    }

                    Album album = new Album(albumId, name, artist, imageUrl, songs);
                    albums.add(album);
                }
                actualizarUIConAlbumes(albums);
            } else {
                Log.e("Firebase", "Error al obtener álbumes: " + task.getException().getMessage());
            }
        });
    }

    private void cargarAlbumYAbrirDetalleFirebase(String albumId) {
        databaseReference.child("albums").child(albumId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Album album = task.getResult().getValue(Album.class);
                if (album != null) {
                    abrirDetalle(album);
                }
            } else {
                Log.e("Firebase", "Error al cargar álbum: " + task.getException().getMessage());
            }
        });
    }


}