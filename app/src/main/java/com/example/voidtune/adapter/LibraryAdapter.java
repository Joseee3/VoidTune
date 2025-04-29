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
import com.example.voidtune.API.Album;
import com.example.voidtune.API.Song;
import com.example.voidtune.Activities.DetailAlbumActivity;
import com.example.voidtune.R;
import com.google.gson.Gson;

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

    if (album.getSongs() != null && !album.getSongs().isEmpty()) {
        holder.artistName.setText(album.getSongs().get(0).getArtist());
    } else {
        holder.artistName.setText("Artista desconocido");
    }

    Glide.with(context)
        .load(album.getImageUrl())
        .placeholder(R.drawable.img_album)
        .into(holder.albumImage);



    holder.itemView.setOnClickListener(v -> {
        Intent intent = new Intent(context, DetailAlbumActivity.class);
        intent.putExtra("albumName", album.getName());
        intent.putExtra("albumImage", album.getImageUrl());
        intent.putExtra("songs", (ArrayList<Song>) album.getSongs()); // Pasar la lista de canciones
        context.startActivity(intent);
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