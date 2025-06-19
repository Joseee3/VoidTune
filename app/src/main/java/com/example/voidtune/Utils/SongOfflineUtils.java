package com.example.voidtune.Utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class SongOfflineUtils {

    // 1. Download and save the song locally
    public static void downloadSong(String audioUrl, String fileName, Context context) {
        new Thread(() -> {
            try (InputStream in = new URL(audioUrl).openStream();
                 FileOutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 2. Check if device is offline
    public static boolean isOffline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork == null || !activeNetwork.isConnected();
    }

    // 3. Play local file if offline
    public static void playCurrentSong(Context context, String fileName, String onlineUrl) {
        String path;
        if (isOffline(context)) {
            File file = new File(context.getFilesDir(), fileName);
            if (file.exists()) {
                path = file.getAbsolutePath();
            } else {
                Toast.makeText(context, "No song available offline.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            path = onlineUrl;
            downloadSong(onlineUrl, fileName, context);
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}