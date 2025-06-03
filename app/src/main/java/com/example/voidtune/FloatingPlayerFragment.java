package com.example.voidtune;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.voidtune.VIewModel.MusicViewModel;
import com.example.voidtune.service.MusicService;
import com.example.voidtune.service.PlayerService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FloatingPlayerFragment extends Fragment {
    private TextView songTitle;
    private TextView songArtist;
    private ImageView albumImage;

    private MusicService musicService;
    private boolean isServiceBound = false;

    private MusicViewModel musicViewModel;

    private PlayerService playerService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Compartir el ViewModel con la actividad
        musicViewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);

        // Observar cambios en el ID de la canción
        musicViewModel.getCurrentSongId().observe(this, songId -> {
            if (songId != null) {
                updatePlayer(songId); // Actualiza la UI del reproductor
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_floating_player, container, false);
        songTitle = view.findViewById(R.id.SongTitle);
        songArtist = view.findViewById(R.id.ArtistName);
        albumImage = view.findViewById(R.id.AlbumImage); // Asegúrate de usar el ID correcto
        return view;
    }


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
            updatePlayerUI(); // Actualizar la interfaz con los datos actuales
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };



   @Override
    public void onStart() {
        super.onStart();

        // Cargar datos del caché
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
        String title = sharedPreferences.getString("title", null);
        String artist = sharedPreferences.getString("artist", null);
        String albumImageUrl = sharedPreferences.getString("albumImageUrl", null);

        if (title != null && artist != null) {
            // Mostrar el flotante con los datos cargados
            if (songTitle != null) songTitle.setText(title);
            if (songArtist != null) songArtist.setText(artist);
            if (albumImage != null && albumImageUrl != null) {
                Glide.with(requireContext())
                    .load(albumImageUrl)
                    .placeholder(R.drawable.img_album)
                    .into(albumImage);
            }

            View floatingPlayer = getView().findViewById(R.id.floatingPlayerContainer);
            if (floatingPlayer != null) {
                floatingPlayer.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isServiceBound) {
            getActivity().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    public void updatePlayer(String songId) {
            if (songId == null || songId.isEmpty()) {
                Log.e("FloatingPlayerFragment", "Invalid songId provided.");
                return;
            }

            DatabaseReference songRef = FirebaseDatabase.getInstance()
                    .getReference("songs")
                    .child(songId);

            songRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Log.e("FloatingPlayerFragment", "Song not found in Firebase.");
                        return;
                    }

                    // Recuperar los datos de la canción
                    String title = snapshot.child("name").getValue(String.class);
                    String artist = snapshot.child("artist").getValue(String.class);
                    String audioUrl = snapshot.child("audioURL").getValue(String.class);
                    String albumId = snapshot.child("albumID").getValue(String.class);


                    // Validar datos recuperados
                    if (title == null || artist == null || audioUrl == null) {
                        Log.e("FloatingPlayerFragment", "Incomplete song data in Firebase.");
                        return;
                    }


                    // Actualizar las vistas del reproductor
                    if (songTitle != null) songTitle.setText(title);
                    if (songArtist != null) songArtist.setText(artist);

                    // Recuperar y actualizar la imagen del álbum
                    if (albumId != null && !albumId.isEmpty()) {
                        DatabaseReference albumRef = FirebaseDatabase.getInstance()
                                .getReference("albums")
                                .child(albumId);

                        albumRef.child("imageURL").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String albumImageUrl = task.getResult().getValue(String.class);
                                if (albumImageUrl != null && albumImage != null) {
                                    // Actualizar la imagen del álbum
                                    Glide.with(requireContext())
                                            .load(albumImageUrl)
                                            .placeholder(R.drawable.img_album)
                                            .into(albumImage);

                                    // Guardar los datos en el caché
                                    saveSongDataToCache(title, artist, albumImageUrl, audioUrl);
                                }
                            } else {
                                Log.e("FloatingPlayerFragment", "Failed to fetch album image.");
                            }
                        });
                    } else {
                        // Guardar los datos en el caché sin la URL de la imagen
                        saveSongDataToCache(title, artist, null, audioUrl);
                    }

                    // Enviar la URL de audio al servicio de música
                    if (isServiceBound && musicService != null) {
                        musicService.playSong(audioUrl);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FloatingPlayerFragment", "Error fetching song: " + error.getMessage());
                }
            });
        }


   private void updatePlayerUI() {
       if (musicService != null) {
           if (songTitle != null) {
               songTitle.setText(musicService.getCurrentTitle());
           } else {
               Log.e("FloatingPlayerFragment", "songTitle is null");
           }

           if (songArtist != null) {
               songArtist.setText(musicService.getCurrentArtist());
           } else {
               Log.e("FloatingPlayerFragment", "songArtist is null");
           }

           if (albumImage != null) {
               Glide.with(this)
                       .load(musicService.getCurrentAlbumImageUrl())
                       .placeholder(R.drawable.img_album)
                       .into(albumImage);
           } else {
               Log.e("FloatingPlayerFragment", "albumImage is null");
           }
       }
   }


   private void saveSongDataToCache(String title, String artist, String albumImageUrl, String audioUrl) {
       if (getActivity() != null) {
           SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
           SharedPreferences.Editor editor = sharedPreferences.edit();
           editor.putString("title", title);
           editor.putString("artist", artist);
           editor.putString("albumImageUrl", albumImageUrl);
           editor.putString("audioURL", audioUrl);
           editor.apply();
       }
   }

  private void loadSongDataFromCache() {
    if (getActivity() != null) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
        String title = sharedPreferences.getString("title", null);
        String artist = sharedPreferences.getString("artist", null);
        String albumImageUrl = sharedPreferences.getString("albumImageUrl", null);
        String audioUrl = sharedPreferences.getString("audioUrl", null); // Recuperar la URL del audio

        if (title != null && songTitle != null) songTitle.setText(title);
        if (artist != null && songArtist != null) songArtist.setText(artist);
        if (albumImageUrl != null && albumImage != null) {
            Glide.with(requireContext())
                .load(albumImageUrl)
                .placeholder(R.drawable.img_album)
                .into(albumImage);
        }

        Log.d("FloatingPlayerFragment", "Datos cargados de SharedPreferences: " + title + ", " + artist + ", " + albumImageUrl + ", " + audioUrl);
    }
}
}