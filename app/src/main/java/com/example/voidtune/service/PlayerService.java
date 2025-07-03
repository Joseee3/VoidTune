package com.example.voidtune.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class PlayerService extends Service {
    private final IBinder binder = new PlayerBinder();
    private String currentTrack; // Información del track actual

    public class PlayerBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public void playTrack(String track) {
        currentTrack = track;
    }
    public String getCurrentTrack() {
        return currentTrack;
    }
}