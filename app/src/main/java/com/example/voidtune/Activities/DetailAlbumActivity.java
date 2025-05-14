package com.example.voidtune.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.voidtune.entities.Song;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class DetailAlbumActivity extends AppCompatActivity {

   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.detail_album);

       // Recuperar datos del Intent
       String albumId = getIntent().getStringExtra("albumId");
       String albumName = getIntent().getStringExtra("albumName");
       String albumImage = getIntent().getStringExtra("albumImage");

       if (albumId == null || albumId.isEmpty()) {
           finish();
           return;
       }

       // Configurar vistas
       TextView albumTitle = findViewById(R.id.albumTitle);
       ImageView albumCover = findViewById(R.id.albumCover);
       RecyclerView songsRecyclerView = findViewById(R.id.songsRecyclerView);

       albumTitle.setText(albumName);
       Glide.with(this).load(albumImage).into(albumCover);

       // Configurar RecyclerView
       songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
       ArrayList<Song> songs = new ArrayList<>();
       SongAdapter songAdapter = new SongAdapter(this, songs);
       songsRecyclerView.setAdapter(songAdapter);

       // Cargar canciones desde Firebase
       FirebaseDatabase.getInstance().getReference("albums").child(albumId).child("songs")
           .get()
           .addOnCompleteListener(task -> {
               if (task.isSuccessful() && task.getResult() != null) {
                   for (DataSnapshot songSnapshot : task.getResult().getChildren()) {
                       String songId = songSnapshot.getValue(String.class); // Obtener el valor del índice
                       if (songId != null) {
                           FirebaseDatabase.getInstance().getReference("songs").child(songId)
                               .get()
                               .addOnCompleteListener(songTask -> {
                                   if (songTask.isSuccessful() && songTask.getResult() != null) {
                                       Song song = songTask.getResult().getValue(Song.class);
                                       if (song != null) {
                                           songs.add(song);
                                           songAdapter.notifyItemInserted(songs.size() - 1);
                                       }
                                   }
                               });
                       }
                   }
               } else {
                   Log.e("Firebase", "Error al cargar canciones: " + task.getException().getMessage());
               }
           });
   }
}