package com.example.voidtune;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.voidtune.Activities.DetailAlbumActivity;
import com.example.voidtune.VIewModel.MusicViewModel;
import com.example.voidtune.service.MusicService;
import com.example.voidtune.service.PlayerService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

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
        if (isServiceBound && musicService != null) {
            if (musicService.isPlaying()) {
                musicService.pauseSong();
            } else {
                musicService.resumeSong();
            }
            // Actualizar el estado del botón inmediatamente
            updatePlayPauseButton(playPauseButton);
        }
    });

    // Configurar el botón de "Next"
    // Botones de navegación
    view.findViewById(R.id.nextButton).setOnClickListener(v -> playNextSong());
    view.findViewById(R.id.previousButton).setOnClickListener(v -> playPreviousSong());

    return view;
}

 private void playNextSong() {
     if (isServiceBound && musicService != null) {
         Log.d("FloatingPlayerFragment", "Reproduciendo la siguiente canción...");
         musicService.playNextSong();

         String nextSongId = musicService.getCurrentSong(); // Obtén el ID de la siguiente canción
         if (nextSongId != null) {
             Log.d("FloatingPlayerFragment", "ID de la siguiente canción: " + nextSongId);
             updatePlayer(nextSongId); // Actualiza la UI con la nueva canción
         } else {
             Log.e("FloatingPlayerFragment", "No se pudo obtener el ID de la siguiente canción.");
         }
     } else {
         Log.e("FloatingPlayerFragment", "El servicio no está enlazado o es null.");
     }
 }

   private void playPreviousSong() {
       if (isServiceBound && musicService != null) {
           musicService.playPreviousSong();
           String previousSongId = musicService.getCurrentSong(); // Obtén el ID de la canción anterior
           if (previousSongId != null) {
               updatePlayer(previousSongId); // Actualiza la UI con la nueva canción
           } else {
               Log.e("FloatingPlayerFragment", "No se pudo obtener el ID de la canción anterior.");
           }
       } else {
           Log.e("FloatingPlayerFragment", "El servicio no está enlazado o es null.");
       }
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
             updatePlayerUI(); // Actualizar la interfaz del reproductor
             ImageButton playPauseButton = getView().findViewById(R.id.miniPlayPauseButton);
             if (playPauseButton != null) {
                 updatePlayPauseButton(playPauseButton); // Actualizar el botón de play/pause
             } else {
                 Log.e("FloatingPlayerFragment", "playPauseButton es null");
             }
         } else {
             Log.e("FloatingPlayerFragment", "getView() es null, no se puede actualizar la UI");
         }

         // Actualizar el reproductor flotante con la canción actual
         if (musicService != null && musicService.getCurrentSong() != null) {
             updatePlayer(musicService.getCurrentSong());
         } else {
             Log.e("FloatingPlayerFragment", "No hay canción actual en el servicio.");
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

       Log.d("FloatingPlayerFragment", "Cargando datos para la canción con ID: " + songId);


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

       // Obtener datos del servicio
       String title = musicService.getCurrentTitle();
       String artist = musicService.getCurrentArtist();
       String albumImageUrl = musicService.getCurrentAlbumImageUrl();

       // Validar y actualizar el título
       if (title != null && songTitle != null) {
           songTitle.setText(title);
       } else {
           Log.e("FloatingPlayerFragment", "Título no disponible o songTitle es null.");
       }

       // Validar y actualizar el artista
       if (artist != null && songArtist != null) {
           songArtist.setText(artist);
       } else {
           Log.e("FloatingPlayerFragment", "Artista no disponible o songArtist es null.");
       }

       // Validar y actualizar la imagen del álbum
       if (albumImageUrl != null && albumImage != null) {
           Glide.with(this)
               .load(albumImageUrl)
               .placeholder(R.drawable.img_album)
               .into(albumImage);
       } else {
           Log.e("FloatingPlayerFragment", "URL de la imagen del álbum no disponible o albumImage es null.");
       }

       // Actualizar el botón de play/pause
       ImageButton playPauseButton = getView() != null ? getView().findViewById(R.id.miniPlayPauseButton) : null;
       if (playPauseButton != null) {
           updatePlayPauseButton(playPauseButton);
       } else {
           Log.e("FloatingPlayerFragment", "playPauseButton es null.");
       }
   }

    public void updatePlayerUI(String autioUrl,String title, String artist, String albumImageUrl) {
        if (songTitle != null) songTitle.setText(title);
        if (songArtist != null) songArtist.setText(artist);
        if (albumImage != null) {
            Glide.with(this)
                .load(albumImageUrl)
                .placeholder(R.drawable.img_album)
                .into(albumImage);
        }
        Log.d("PlayerFragment", "UI updated with: " + title + ", " + artist);
    }

//
//   private void saveSongDataToCache(String title, String artist, String albumImageUrl, String audioUrl) {
//       if (getActivity() != null) {
//           SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
//           SharedPreferences.Editor editor = sharedPreferences.edit();
//           editor.putString("title", title);
//           editor.putString("artist", artist);
//           editor.putString("albumImageUrl", albumImageUrl);
//           editor.putString("audioUrl", audioUrl); // Asegúrate de usar la misma clave
//           editor.apply(); // Guarda los datos de forma asíncrona
//           Log.d("FloatingPlayerFragment", "Datos guardados en SharedPreferences: " + title + ", " + artist + ", " + albumImageUrl + ", " + audioUrl);
//       } else {
//           Log.e("FloatingPlayerFragment", "getActivity() es null, no se pueden guardar los datos en SharedPreferences.");
//       }
//   }
//
//  private void loadSongDataFromCache() {
//      if (getActivity() != null) {
//          SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
//          String title = sharedPreferences.getString("title", null);
//          String artist = sharedPreferences.getString("artist", null);
//          String albumImageUrl = sharedPreferences.getString("albumImageUrl", null);
//          String audioUrl = sharedPreferences.getString("audioUrl", null); // Recuperar la URL del audio
//
//          if (title != null && songTitle != null) songTitle.setText(title);
//          if (artist != null && songArtist != null) songArtist.setText(artist);
//          if (albumImageUrl != null && albumImage != null) {
//              Glide.with(requireContext())
//                  .load(albumImageUrl)
//                  .placeholder(R.drawable.img_album)
//                  .into(albumImage);
//          }
//
//          Log.d("FloatingPlayerFragment", "Datos cargados de SharedPreferences: " + title + ", " + artist + ", " + albumImageUrl + ", " + audioUrl);
//      } else {
//          Log.e("FloatingPlayerFragment", "getActivity() es null, no se pueden cargar los datos desde SharedPreferences.");
//      }
//  }
//private final BroadcastReceiver updatePlayerReceiver = new BroadcastReceiver() {
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
//        String title = sharedPreferences.getString("title", null);
//        String artist = sharedPreferences.getString("artist", null);
//        String audioUrl = sharedPreferences.getString("audioUrl", null);
//
//        if (title != null && artist != null && audioUrl != null) {
//            updatePlayerUI(title, artist, audioUrl);
//        } else {
//            Log.e("FloatingPlayerFragment", "Datos incompletos en SharedPreferences.");
//        }
//    }
//};

//   @Override
//public void onResume() {
//    super.onResume();
//    IntentFilter filter = new IntentFilter("com.example.voidtune.UPDATE_PLAYER");
//       ContextCompat.registerReceiver(requireActivity(), updatePlayerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
//}
//
//@Override
//public void onPause() {
//    super.onPause();
//    requireActivity().unregisterReceiver(updatePlayerReceiver);
//}

private void saveSongDataToCache(String title, String artist, String albumImageUrl, String audioUrl) {
    if (getActivity() != null) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("title", title);
        editor.putString("artist", artist);
        editor.putString("albumImageUrl", albumImageUrl);
        editor.putString("audioURL", audioUrl); // Clave consistente
        editor.apply();
        Log.d("FloatingPlayerFragment", "Datos guardados en SharedPreferences: " + title + ", " + artist + ", " + albumImageUrl + ", " + audioUrl);
    }
}

private void loadSongDataFromCache() {
    if (getActivity() != null) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FloatingPlayerCache", Context.MODE_PRIVATE);
        String title = sharedPreferences.getString("title", null);
        String artist = sharedPreferences.getString("artist", null);
        String albumImageUrl = sharedPreferences.getString("albumImageUrl", null);
        String audioUrl = sharedPreferences.getString("audioURL", null); // Clave consistente

        if (title != null && artist != null && audioUrl != null) {
            if (songTitle != null) songTitle.setText(title);
            if (songArtist != null) songArtist.setText(artist);
            if (albumImageUrl != null && albumImage != null) {
                Glide.with(requireContext())
                    .load(albumImageUrl)
                    .placeholder(R.drawable.img_album)
                    .into(albumImage);
            }
            Log.d("FloatingPlayerFragment", "Datos cargados de SharedPreferences: " + title + ", " + artist + ", " + albumImageUrl + ", " + audioUrl);

            // Mostrar el reproductor flotante
            View floatingPlayer = getView() != null ? getView().findViewById(R.id.floatingPlayerContainer) : null;
            if (floatingPlayer != null) {
                floatingPlayer.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e("FloatingPlayerFragment", "No se encontraron datos válidos en SharedPreferences.");

            // Ocultar el reproductor flotante si no hay datos
            View floatingPlayer = getView() != null ? getView().findViewById(R.id.floatingPlayerContainer) : null;
            if (floatingPlayer != null) {
                floatingPlayer.setVisibility(View.GONE);
            }
        }
    } else {
        Log.e("FloatingPlayerFragment", "getActivity() es null, no se pueden cargar los datos desde SharedPreferences.");
    }
}

private final BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        String albumImageUrl = intent.getStringExtra("albumImageUrl");

        Log.d("PlayerFragment", "Datos recibidos: " + title + ", " + artist + ", " + albumImageUrl);

        if (title != null && artist != null) {
            updatePlayerUI(title, artist, albumImageUrl);
        } else {
            Log.e("PlayerFragment", "Datos incompletos recibidos en el broadcast.");
        }
    }
};
@Override
public void onResume() {
    super.onResume();
    IntentFilter filter = new IntentFilter("com.example.voidtune.UPDATE_PLAYER");
    LocalBroadcastManager.getInstance(requireContext()).registerReceiver(playerUpdateReceiver, filter);
}

@Override
public void onPause() {
    super.onPause();
    LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(playerUpdateReceiver);
}

// In FloatingPlayerFragment.java
public void updateAlbumImage(String imageUrl) {
    ImageView albumImageView = getView().findViewById(R.id.AlbumImage);
    if (imageUrl != null && !imageUrl.isEmpty()) {
        Glide.with(getContext())
                .load(imageUrl)
                .placeholder(R.drawable.img_album)
                .into(albumImageView);
    } else {
        albumImageView.setImageResource(R.drawable.img_album);
    }
}

public void updatePlayerUI(String title, String artist, String albumImageUrl) {
    if (songTitle != null) {
        songTitle.setText(title);
    }
    if (songArtist != null) {
        songArtist.setText(artist);
    }
    if (albumImage != null) {
        Glide.with(this)
            .load(albumImageUrl)
            .placeholder(R.drawable.img_album)
            .into(albumImage);
    }
    Log.d("PlayerFragment", "UI actualizada con: " + title + ", " + artist);
}

}