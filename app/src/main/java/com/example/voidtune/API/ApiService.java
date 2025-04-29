package com.example.voidtune.API;
import com.example.voidtune.API.Album;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("album")
    Call<List<Album>> getAlbums();

    @GET("albums/{id}")
    Call<Album> getAlbumById(@Path("id") String albumId);
}