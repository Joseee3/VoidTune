package com.example.voidtune.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voidtune.entities.Album;
import com.example.voidtune.Activities.DetailAlbumActivity;
import com.example.voidtune.R;

import java.util.ArrayList;
import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {

    private Context context;
    private List<Album> albumList;

    public LibraryAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_library, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albumList.get(position);
        holder.albumName.setText(album.getName());
        holder.artistName.setText(album.getArtist());

        Glide.with(context)
            .load(album.getImageUrl())
            .placeholder(R.drawable.img_album)
            .into(holder.albumImage);

        holder.itemView.setOnClickListener(v -> {
            if (album.getId() != null && album.getName() != null && album.getArtist() != null) {
                Intent intent = new Intent(context, DetailAlbumActivity.class);
                intent.putExtra("albumId", album.getId());
                intent.putExtra("albumName", album.getName());
                intent.putExtra("albumArtist", album.getArtist());
                intent.putExtra("albumImage", album.getImageUrl());
                intent.putStringArrayListExtra("songs", new ArrayList<>(album.getSongs() != null ? album.getSongs() : new ArrayList<>()));

                // Log para depurar
                android.util.Log.d("LibraryAdapter", "Datos enviados: " + intent.getExtras());

                context.startActivity(intent);
            } else {
                android.util.Log.e("LibraryAdapter", "Datos del álbum incompletos: " + album);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumImage;
        TextView albumName, artistName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumImage = itemView.findViewById(R.id.albumImage);
            albumName = itemView.findViewById(R.id.albumName);
            artistName = itemView.findViewById(R.id.artistName);
        }
    }
}