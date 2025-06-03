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
import android.widget.Button;
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


    boolean isPlaying = false; // Variable para rastrear el estado de reproducción


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
    albumImage = view.findViewById(R.id.AlbumImage);

    // Configurar el botón de play/pause
    ImageButton playPauseButton = view.findViewById(R.id.miniPlayPauseButton);
   playPauseButton.setOnClickListener(v -> {
        Log.d("FloatingPlayerFragment", "Botón de play/pause clickeado");

        if (isServiceBound && musicService != null) {
            if (musicService.isPlaying()) {
                Log.d("FloatingPlayerFragment", "El servicio está reproduciendo, pausando la canción");
                musicService.pauseSong();
            } else {
                Log.d("FloatingPlayerFragment", "El servicio está pausado, reanudando la canción");
                musicService.resumeSong();
            }
            updatePlayPauseButton(playPauseButton);
        } else {
            Log.e("FloatingPlayerFragment", "Service not bound or null");
        }
    });

    // Actualizar el estado del botón al cargar la vista
    updatePlayPauseButton(playPauseButton);

    return view;
}


private void updatePlayPauseButton(ImageButton playPauseButton) {
    if (isServiceBound && musicService != null) {
        playPauseButton.setImageResource(
            musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
        );
    }
}



  private final ServiceConnection serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
          MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
          musicService = binder.getService();
          isServiceBound = true;

          Log.d("FloatingPlayerFragment", "Servicio conectado correctamente");

          // Validar que la vista no sea null antes de actualizar la UI
          if (getView() != null) {
              updatePlayerUI();
              ImageButton playPauseButton = getView().findViewById(R.id.miniPlayPauseButton);
              if (playPauseButton != null) {
                  updatePlayPauseButton(playPauseButton);
              } else {
                  Log.e("FloatingPlayerFragment", "playPauseButton es null");
              }
          } else {
              Log.e("FloatingPlayerFragment", "getView() es null, no se puede actualizar la UI");
          }
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
          isServiceBound = false;
          Log.d("FloatingPlayerFragment", "Servicio desconectado");
      }

    };



  @Override
  public void onStart() {
      super.onStart();

      loadSongDataFromCache();

      // Cargar datos del caché
      SharedPreferences sharedPreferences = requireContext().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
      String title = sharedPreferences.getString("title", null);
      String artist = sharedPreferences.getString("artist", null);
      String albumImageUrl = sharedPreferences.getString("albumImageUrl", null);

      if (title != null && artist != null) {
          // Actualizar la UI con los datos del caché
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
      } else {
          // Si no hay datos en el caché, ocultar el reproductor
          View floatingPlayer = getView().findViewById(R.id.floatingPlayerContainer);
          if (floatingPlayer != null) {
              floatingPlayer.setVisibility(View.GONE);
          }
          Log.e("FloatingPlayerFragment", "No hay datos en el caché. Verifica si se están guardando correctamente.");
      }

      // Enlazar el servicio
      Log.d("FloatingPlayerFragment", "Intentando enlazar el servicio...");
      Intent intent = new Intent(getActivity(), MusicService.class);
      boolean bound = getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
      Log.d("FloatingPlayerFragment", "¿Servicio enlazado?: " + bound);

      // Actualizar el estado del botón de play/pause
      if (isServiceBound && musicService != null) {
          ImageButton playPauseButton = getView().findViewById(R.id.miniPlayPauseButton);
          if (playPauseButton != null) {
              updatePlayPauseButton(playPauseButton);
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

            // Validar que el songId no contenga caracteres prohibidos
            if (songId.contains(".") || songId.contains("#") || songId.contains("$") || songId.contains("[") || songId.contains("]")) {
                Log.e("FloatingPlayerFragment", "songId contiene caracteres no permitidos.");
                return;
            }

            DatabaseReference songRef = FirebaseDatabase.getInstance()
                    .getReference("songs")
                    .child(songId);

            songRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) {
                        Log.e("FloatingPlayerFragment", "El fragmento no está adjunto a un contexto.");
                        return;
                    }

                    if (!snapshot.exists()) {
                        Log.e("FloatingPlayerFragment", "Song not found in Firebase.");
                        return;
                    }

                    // Recuperar los datos de la canción
                    String title = snapshot.child("name").getValue(String.class);
                    String artist = snapshot.child("artist").getValue(String.class);
                    String audioUrl = snapshot.child("audioURL").getValue(String.class);
                    String albumId = snapshot.child("albumID").getValue(String.class);

                    if (title == null || artist == null || audioUrl == null) {
                        Log.e("FloatingPlayerFragment", "Incomplete song data in Firebase.");
                        return;
                    }

                    // Actualizar las vistas del reproductor
                    if (songTitle != null) songTitle.setText(title);
                    if (songArtist != null) songArtist.setText(artist);

                    if (albumId != null && !albumId.isEmpty()) {
                        DatabaseReference albumRef = FirebaseDatabase.getInstance()
                                .getReference("albums")
                                .child(albumId);

                        albumRef.child("imageURL").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String albumImageUrl = task.getResult().getValue(String.class);
                                if (albumImageUrl != null && albumImage != null) {
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
                        saveSongDataToCache(title, artist, null, audioUrl);
                    }

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
       if (musicService == null) {
           Log.e("FloatingPlayerFragment", "musicService no está enlazado.");
           return;
       }

       String title = musicService.getCurrentTitle();
       String artist = musicService.getCurrentArtist();
       String albumImageUrl = musicService.getCurrentAlbumImageUrl();

       if (title != null && songTitle != null) {
           songTitle.setText(title);
       } else {
           Log.e("FloatingPlayerFragment", "Título no disponible o songTitle es null.");
       }

       if (artist != null && songArtist != null) {
           songArtist.setText(artist);
       } else {
           Log.e("FloatingPlayerFragment", "Artista no disponible o songArtist es null.");
       }

       if (albumImageUrl != null && albumImage != null) {
           Glide.with(this)
               .load(albumImageUrl)
               .placeholder(R.drawable.img_album)
               .into(albumImage);
       } else {
           Log.e("FloatingPlayerFragment", "URL de la imagen del álbum no disponible o albumImage es null.");
       }

       ImageButton playPauseButton = getView() != null ? getView().findViewById(R.id.miniPlayPauseButton) : null;
       if (playPauseButton != null) {
           updatePlayPauseButton(playPauseButton);
       } else {
           Log.e("FloatingPlayerFragment", "playPauseButton es null.");
       }
   }

   private void saveSongDataToCache(String title, String artist, String albumImageUrl, String audioUrl) {
       if (getActivity() != null) {
           SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
           SharedPreferences.Editor editor = sharedPreferences.edit();
           editor.putString("title", title);
           editor.putString("artist", artist);
           editor.putString("albumImageUrl", albumImageUrl);
           editor.putString("audioUrl", audioUrl); // Asegúrate de usar la misma clave
           editor.apply(); // Guarda los datos de forma asíncrona
           Log.d("FloatingPlayerFragment", "Datos guardados en SharedPreferences: " + title + ", " + artist + ", " + albumImageUrl + ", " + audioUrl);
       } else {
           Log.e("FloatingPlayerFragment", "getActivity() es null, no se pueden guardar los datos en SharedPreferences.");
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
      } else {
          Log.e("FloatingPlayerFragment", "getActivity() es null, no se pueden cargar los datos desde SharedPreferences.");
      }
  }
}