package com.example.voidtune.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voidtune.R;
import com.example.voidtune.entities.LibraryItem;

import java.util.List;

public class LibraryListAdapter extends RecyclerView.Adapter<LibraryListAdapter.LibraryViewHolder> {

    private final Context context;
    private final List<LibraryItem> items;

    public LibraryListAdapter(Context context, List<LibraryItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_librarys, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        LibraryItem item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.image.setImageResource(item.getImageResId());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LibraryViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
        }
    }
}