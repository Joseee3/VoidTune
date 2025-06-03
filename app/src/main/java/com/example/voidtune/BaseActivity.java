package com.example.voidtune;

import android.content.ComponentName;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.voidtune.service.PlayerService;


public class BaseActivity extends AppCompatActivity {
protected PlayerService playerService;
private boolean isBound = false;

private final ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
        playerService = binder.getService();
        isBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
    }
};

@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = new Intent(this, PlayerService.class);
    bindService(intent, connection, Context.BIND_AUTO_CREATE);
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (isBound) {
        unbindService(connection);
        isBound = false;
    }
}


    protected void loadFloatingPlayer() {
        // Cargar datos del caché
        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
        String title = sharedPreferences.getString("title", null);
        String artist = sharedPreferences.getString("artist", null);
        String albumImageUrl = sharedPreferences.getString("albumImageUrl", null);

        if (title != null && artist != null) {
            // Mostrar el flotante con los datos cargados
            TextView songTitle = findViewById(R.id.SongTitle);
            TextView songArtist = findViewById(R.id.ArtistName);
            ImageView albumImage = findViewById(R.id.AlbumImage);

            if (songTitle != null) songTitle.setText(title);
            if (songArtist != null) songArtist.setText(artist);
            if (albumImage != null && albumImageUrl != null) {
                Glide.with(this)
                        .load(albumImageUrl)
                        .placeholder(R.drawable.img_album)
                        .into(albumImage);
            }

            View floatingPlayer = findViewById(R.id.floatingPlayerContainer);
            if (floatingPlayer != null) {
                floatingPlayer.setVisibility(View.VISIBLE);
            }
        }
    }
}