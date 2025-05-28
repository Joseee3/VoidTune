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
import com.example.voidtune.entities.Album;
import com.example.voidtune.R;

import java.util.ArrayList;
import java.util.List;

public class HomeListAdapter extends RecyclerView.Adapter<HomeListAdapter.HomeViewHolder> {

    private final Context context;
    private final List<Album> albums = new ArrayList<>(); // Aseguramos que sea mutable
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Album album);
    }

    public void updateData(List<Album> newAlbums) {
        albums.clear(); // Limpiamos la lista actual
        if (newAlbums != null) {
            albums.addAll(newAlbums); // Agregamos los nuevos datos
        }
        notifyDataSetChanged(); // Notificamos al adaptador que los datos han cambiado
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public HomeListAdapter(Context context, List<Album> initialAlbums) {
        this.context = context;
        if (initialAlbums != null) {
            this.albums.addAll(initialAlbums); // Inicializamos con los datos proporcionados
        }
    }

    @NonNull
    @Override
    public HomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home, parent, false);
        return new HomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.title.setText(album.getName());


        Glide.with(context)
                .load(album.getImageUrl())
                .placeholder(R.drawable.foto_carousel) // Imagen de carga
                .error(R.drawable.foto_carousel) // Imagen de error
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (album.getId() != null && !album.getId().isEmpty()) {
                    Log.d("Adapter", "Album seleccionado: " + album.getId());
                    listener.onItemClick(album);
                } else {
                    Log.e("Adapter", "El albumId es nulo o vacío");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class HomeViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;

        public HomeViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
        }
    }
}