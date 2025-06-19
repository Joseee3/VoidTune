package com.example.voidtune.adapter;

            import android.content.Context;
            import android.util.Log;
            import android.view.LayoutInflater;
            import android.view.View;
            import android.view.ViewGroup;
            import android.widget.ImageView;
            import android.widget.TextView;

            import androidx.annotation.NonNull;
            import androidx.recyclerview.widget.RecyclerView;

            import com.bumptech.glide.Glide;
            import com.example.voidtune.R;
            import com.example.voidtune.entities.Song;
            import com.google.firebase.database.FirebaseDatabase;

            import java.util.ArrayList;
            import java.util.List;

            public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SongViewHolder> {

                private final Context context;
                private final ArrayList<Song> songs;
                private boolean isPlaylistContext; // Nuevo argumento
                private SongAdapter.OnSongClickListener onSongClickListener;




                public SearchAdapter(Context context, ArrayList<Song> songs, boolean isPlaylistContext) {
                    this.context = context;
                    this.songs = songs; // Use ArrayList
                    this.isPlaylistContext = isPlaylistContext;
                }

                public SearchAdapter(Context context, ArrayList<Song> songs) {
                    this.context = context;
                    this.songs = songs;
                }

                public void setSongs(List<Song> songs) {
                    this.songs.clear();
                    this.songs.addAll(songs);
                    notifyDataSetChanged();
                }

                public List<Song> getSongs() {
                    return songs; // or the variable holding your song list
                }

                @NonNull
                @Override
                public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(context).inflate(R.layout.item_search, parent, false);
                    return new SongViewHolder(view);
                }

                @Override
                public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
                    Song song = songs.get(position);
                    holder.songTitle.setText(song.getName());
                    holder.songArtist.setText(song.getArtist());

                    String albumID = song.getAlbumID();
                    Log.d("SearchAdapter", "albumID: " + albumID); // <-- Agrega aquí

                    if (albumID != null && !albumID.isEmpty()) {
                        FirebaseDatabase.getInstance()
                            .getReference("albums")
                            .child(albumID)
                            .child("imageURL")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                String imageUrl = snapshot.getValue(String.class);
                                Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_music_note)
                                    .into(holder.songImage);
                            })
                            .addOnFailureListener(e -> {
                                Glide.with(context)
                                    .load(R.drawable.ic_music_note)
                                    .into(holder.songImage);
                            });
                    } else {
                        Glide.with(context)
                            .load(R.drawable.ic_music_note)
                            .into(holder.songImage);
                    }

                    holder.itemView.setOnClickListener(v -> {
                        if (onSongClickListener != null) {
                            onSongClickListener.onSongClick(song);
                        }
                    });
                }

                @Override
                public int getItemCount() {
                    return songs.size();
                }

                public static class SongViewHolder extends RecyclerView.ViewHolder {
                    TextView songTitle, songArtist;
                    ImageView songImage;

                    public SongViewHolder(@NonNull View itemView) {
                        super(itemView);
                        songTitle = itemView.findViewById(R.id.songTitle);
                        songArtist = itemView.findViewById(R.id.songArtist);
                        songImage = itemView.findViewById(R.id.songImage);
                    }
                }

                public void setOnSongClickListener(SongAdapter.OnSongClickListener listener) {
                    this.onSongClickListener = listener;
                }


            }