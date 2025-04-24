package com.example.voidtune.Activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voidtune.R;
import com.example.voidtune.adapter.CategoryAdapter;
import com.example.voidtune.adapter.HomeListAdapter;
import com.example.voidtune.adapter.LibraryListAdapter;
import com.example.voidtune.entities.LibraryItem;
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        // Inicializar Firebase Analytics
        FirebaseApp.initializeApp(this);
        Log.d("FirebaseTest", "FirebaseApp inicializado correctamente");

        // Configuración de Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //Enviar un evento de inicio de sesión
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        mFirebaseAnalytics.logEvent("login_event", bundle);

        Log.d("FirebaseTest", "Evento de inicio de sesión enviado a Firebase");

        //Log de evento de inicio de sesión
        mFirebaseAnalytics.logEvent("login_event", null);

        // Configuración del RecyclerView para las categorías (horizontal)
        RecyclerView categoryRecyclerView = findViewById(R.id.carouselRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<String> categories = Arrays.asList("Música", "Podcasts", "Audiolibros", "Noticias", "Deportes");
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, categories);
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Configuración del RecyclerView para las bibliotecas (cuadrícula de 2 columnas)
        RecyclerView libraryRecyclerView = findViewById(R.id.libraryRecyclerView);
        libraryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        List<LibraryItem> libraryItems = Arrays.asList(
                new LibraryItem(R.drawable.foto_carousel, "Biblioteca 1"),
                new LibraryItem(R.drawable.foto_carousel, "Biblioteca 2"),
                new LibraryItem(R.drawable.foto_carousel, "Biblioteca 3"),
                new LibraryItem(R.drawable.foto_carousel, "Biblioteca 4")
        );

        LibraryListAdapter libraryAdapter = new LibraryListAdapter(this, libraryItems);
        libraryRecyclerView.setAdapter(libraryAdapter);

        // Configuración de los otros RecyclerView con item_home.xml
        setupRecyclerView(R.id.suggestionsRecyclerView, getSampleData());
        setupRecyclerView(R.id.mostPlayedRecyclerView, getSampleData());
        setupRecyclerView(R.id.recentMusicRecyclerView, getSampleData());
        setupRecyclerView(R.id.moreOfWhatYouLikeRecyclerView, getSampleData());
        setupRecyclerView(R.id.madeForYouRecyclerView, getSampleData());
    }

    private void setupRecyclerView(int recyclerViewId, List<LibraryItem> data) {
        RecyclerView recyclerView = findViewById(recyclerViewId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new HomeListAdapter(this, data));
    }

    private List<LibraryItem> getSampleData() {
        return Arrays.asList(
                new LibraryItem(R.drawable.foto_carousel, "Elemento 1"),
                new LibraryItem(R.drawable.foto_carousel, "Elemento 2"),
                new LibraryItem(R.drawable.foto_carousel, "Elemento 3"),
                new LibraryItem(R.drawable.foto_carousel, "Elemento 4")
        );
    }
}