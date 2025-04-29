package com.example.voidtune.entities;

public class LibraryItem {
    private final String imageUrl;
    private final String title;
    private final String subtitle;
    private final int imageResId;

    // Constructor para elementos con IDs de recursos locales (compatibilidad con versiones anteriores)
    public LibraryItem(int imageResId, String title) {
        this.imageUrl = null;
        this.imageResId = imageResId;
        this.title = title;
        this.subtitle = null;
    }

    // Constructor para elementos con URLs de imágenes de la API
    public LibraryItem(String imageUrl, String title, String subtitle) {
        this.imageUrl = imageUrl;
        this.imageResId = 0;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.isEmpty();
    }
}