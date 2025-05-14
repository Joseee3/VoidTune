package com.example.voidtune.adapter;

        import android.content.Context;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.TextView;

        import androidx.annotation.NonNull;
        import androidx.recyclerview.widget.RecyclerView;

        import com.example.voidtune.R;
        import com.example.voidtune.entities.Song;

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
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        public static class SongViewHolder extends RecyclerView.ViewHolder {
            TextView songTitle, songArtist;

            public SongViewHolder(@NonNull View itemView) {
                super(itemView);
                songTitle = itemView.findViewById(R.id.songTitle);
                songArtist = itemView.findViewById(R.id.songArtist);
            }
        }
    }