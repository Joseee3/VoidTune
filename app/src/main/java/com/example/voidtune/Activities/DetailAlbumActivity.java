package com.example.voidtune.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.R;
import com.example.voidtune.adapter.SongAdapter;
import com.example.voidtune.API.Song;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class DetailAlbumActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_album);

        //Abrir Library
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_home) {
                    startActivity(new Intent(DetailAlbumActivity.this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.menu_search) {
                    // Acción para el menú Search
                    return true;
                } else if (itemId == R.id.menu_library) {
                    startActivity(new Intent(DetailAlbumActivity.this, LibraryActivity.class));
                    return true;
                }
                return false;
            }
        });

        // Obtener datos del Intent
        String albumName = getIntent().getStringExtra("albumName");
        String albumImage = getIntent().getStringExtra("albumImage");
        ArrayList<Song> songs = (ArrayList<Song>) getIntent().getSerializableExtra("songs");

        // Configurar vistas
        TextView albumTitle = findViewById(R.id.albumTitle);
        ImageView albumCover = findViewById(R.id.albumCover);
        RecyclerView songsRecyclerView = findViewById(R.id.songsRecyclerView);

        albumTitle.setText(albumName);
        Glide.with(this).load(albumImage).into(albumCover);

        // Configurar RecyclerView
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        SongAdapter songAdapter = new SongAdapter(this, songs);
        songsRecyclerView.setAdapter(songAdapter);
    }
}