package com.example.voidtune.entities;

public class LibraryItem {
    private  String id;
    private  String imageUrl;
    private  String title;
    private  String subtitle;
    private  int imageResId;
    private String type;

    // Constructor para elementos con IDs de recursos locales (compatibilidad con versiones anteriores)
    public LibraryItem(int imageResId, String title) {
        this.imageUrl = null;
        this.imageResId = imageResId;
        this.title = title;
        this.subtitle = null;
    }
    public LibraryItem(String title, String type, int iconResId) {
        this.title = title;
        this.type = type;
        this.imageResId = iconResId;
    }

    public LibraryItem(String imageUrl, String title, String subtitle) {
        this.imageUrl = imageUrl;
        this.imageResId = 0;
        this.title = title;
        this.subtitle = subtitle;
    }

    public LibraryItem(String id, String imageUrl, String title, String subtitle, String type) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.imageResId = 0;
        this.title = title;
        this.subtitle = subtitle;
        this.type = type;
    }

    public LibraryItem(String id, String title, String imageUrl, String type) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.type = type;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
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

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}