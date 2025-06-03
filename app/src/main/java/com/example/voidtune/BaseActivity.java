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

@Override
protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
    String title = sharedPreferences.getString("title", null);
    String artist = sharedPreferences.getString("artist", null);
    String albumImageUrl = sharedPreferences.getString("albumImageUrl", null);

    outState.putString("title", title);
    outState.putString("artist", artist);
    outState.putString("albumImageUrl", albumImageUrl);
}

@Override
protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    String title = savedInstanceState.getString("title");
    String artist = savedInstanceState.getString("artist");
    String albumImageUrl = savedInstanceState.getString("albumImageUrl");

    if (title != null && artist != null) {
        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("title", title);
        editor.putString("artist", artist);
        editor.putString("albumImageUrl", albumImageUrl);
        editor.apply();
    }

    loadFloatingPlayer(); // Recarga el reproductor flotante
}


   protected void loadFloatingPlayer() {
        FloatingPlayerFragment floatingPlayerFragment = (FloatingPlayerFragment)
                getSupportFragmentManager().findFragmentById(R.id.floatingPlayerContainer);

        if (floatingPlayerFragment == null) {
            floatingPlayerFragment = new FloatingPlayerFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.floatingPlayerContainer, floatingPlayerFragment)
                    .commitNow();
        }

        SharedPreferences sharedPreferences = getSharedPreferences("FloatingPlayerCache", MODE_PRIVATE);
        String currentAudioUrl = sharedPreferences.getString("currentAudioUrl", null);

        if (currentAudioUrl != null) {
            floatingPlayerFragment.updatePlayer(currentAudioUrl);
            findViewById(R.id.floatingPlayerContainer).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.floatingPlayerContainer).setVisibility(View.GONE);
        }
    }
}