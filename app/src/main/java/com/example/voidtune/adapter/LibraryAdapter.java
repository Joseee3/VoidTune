package com.example.voidtune.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.entities.Album;
import com.example.voidtune.entities.LibraryItem;
import com.example.voidtune.entities.Song;
import com.example.voidtune.Activities.DetailAlbumActivity;
import com.example.voidtune.R;

import java.util.ArrayList;
import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {

    private Context context;
    private List<LibraryItem> libraryItems;

    public LibraryAdapter(Context context, List<LibraryItem> libraryItems) {
        this.context = context;
        this.libraryItems = libraryItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_library, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibraryItem item = libraryItems.get(position);

        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());

        Glide.with(context)
            .load(item.getImageUrl())
            .placeholder(R.drawable.img_album)
            .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            switch (item.getType()) {
                case "album":
                    openAlbumDetail(item);
                    break;
                case "song":
                    playSong(item);
                    break;
                case "playlist":
                    openPlaylist(item);
                    break;
                default:
                    android.util.Log.e("LibraryAdapter", "Unknown item type: " + item.getType());
            }
        });
    }

    @Override
    public int getItemCount() {
        return libraryItems.size();
    }

    private void openAlbumDetail(LibraryItem item) {
        Intent intent = new Intent(context, DetailAlbumActivity.class);
        intent.putExtra("albumId", item.getId());
        intent.putExtra("albumName", item.getTitle());
        intent.putExtra("albumImage", item.getImageUrl());
        context.startActivity(intent);
    }

    private void playSong(LibraryItem item) {
        // Lógica para reproducir la canción
        android.util.Log.d("LibraryAdapter", "Playing song: " + item.getTitle());
    }

    private void openPlaylist(LibraryItem item) {
        // Lógica para abrir la playlist
        android.util.Log.d("LibraryAdapter", "Opening playlist: " + item.getTitle());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, subtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.albumImage);
            title = itemView.findViewById(R.id.albumName);
            subtitle = itemView.findViewById(R.id.artistName);
        }
    }
}