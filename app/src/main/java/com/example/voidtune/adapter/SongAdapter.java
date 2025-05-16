package com.example.voidtune.adapter;

        import android.app.AlertDialog;
        import android.content.Context;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ImageView;
        import android.widget.PopupMenu;
        import android.widget.TextView;
        import android.widget.Toast;

        import androidx.annotation.NonNull;
        import androidx.recyclerview.widget.RecyclerView;

        import com.example.voidtune.R;
        import com.example.voidtune.entities.Song;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.auth.FirebaseUser;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;

        import java.util.ArrayList;
        import java.util.List;

       public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

        private final Context context;
        private final ArrayList<Song> songs;

        public SongAdapter(Context context, ArrayList<Song> songs) {
            this.context = context;
            this.songs = songs;
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            Song song = songs.get(position);
            holder.songTitle.setText(song.getName()); // Cambiado de getTitle() a getName()
            holder.songArtist.setText(song.getArtist());

            holder.moreOptions.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, holder.moreOptions);
                popupMenu.inflate(R.menu.song_options_menu);

                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.add_to_playlist) {
                        showPlaylistSelectionDialog(song);
                        return true;
                    } else if (itemId == R.id.add_to_likesong) {
                        addToLikedSongs(song);
                        return true;
                    }
                    return false;
                });

                popupMenu.show();
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        public static class SongViewHolder extends RecyclerView.ViewHolder {
            TextView songTitle, songArtist;
            ImageView moreOptions;

            public SongViewHolder(@NonNull View itemView) {
                super(itemView);
                songTitle = itemView.findViewById(R.id.songTitle);
                songArtist = itemView.findViewById(R.id.songArtist);
                moreOptions = itemView.findViewById(R.id.moreOptions);
            }
        }


        // Add these methods to the SongAdapter class

        private void showPlaylistSelectionDialog(Song song) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference playlistsRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("playlists");

                playlistsRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> playlistNames = new ArrayList<>();
                        List<String> playlistIds = new ArrayList<>();

                        for (DataSnapshot snapshot : task.getResult().getChildren()) {
                            playlistNames.add(snapshot.child("name").getValue(String.class));
                            playlistIds.add(snapshot.getKey());
                        }

                        String[] playlistsArray = playlistNames.toArray(new String[0]);
                        new AlertDialog.Builder(context)
                            .setTitle("Select Playlist")
                            .setItems(playlistsArray, (dialog, which) -> {
                                String selectedPlaylistId = playlistIds.get(which);
                                addToPlaylist(song, selectedPlaylistId);
                            })
                            .show();
                    }
                });
            }
        }

        private void addToPlaylist(Song song, String playlistId) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference playlistRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("playlists")
                    .child(playlistId)
                    .child("songs"); // Cambiado de "albums" a "songs" para mayor claridad

                playlistRef.push().setValue(song) // Guarda el objeto Song completo
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Song added to playlist.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to add song: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }

        private void addToLikedSongs(Song song) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference likedSongsRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("likesong");

                likedSongsRef.push().setValue(song) // Guarda el objeto Song completo
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Song added to Liked Songs.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to add song: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }
    }