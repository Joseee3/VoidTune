package com.example.voidtune.adapter;

    import android.content.Context;
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

    import java.util.List;

    public class HomeListAdapter extends RecyclerView.Adapter<HomeListAdapter.HomeViewHolder> {

        private final Context context;
        private final List<Album> albums;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(Album album);
        }

        public void updateData(List<Album> newAlbums) {
            this.albums.clear();
            this.albums.addAll(newAlbums);
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        public HomeListAdapter(Context context, List<Album> albums) {
            this.context = context;
            this.albums = albums;
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
                    .placeholder(R.drawable.foto_carousel)
                    .error(R.drawable.foto_carousel)
                    .into(holder.image);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(album);
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