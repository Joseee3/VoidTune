package com.example.voidtune.entities;

public class LibraryItem {
    private final int imageResId;
    private final String title;

    public LibraryItem(int imageResId, String title) {
        this.imageResId = imageResId;
        this.title = title;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getTitle() {
        return title;
    }
}