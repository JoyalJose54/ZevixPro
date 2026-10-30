package com.app.zevix.model;

import android.net.Uri;
import java.io.Serializable;

public class MusicItem implements Serializable {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final String path;
    private final String uriString;
    private final long duration;
    private final long size;

    public MusicItem(long id, String title, String artist, String album, String path, Uri uri, long duration, long size) {
        this.id = id;
        this.title = title != null ? title : "Unknown Track";
        this.artist = (artist != null && !artist.equals("<unknown>")) ? artist : "Unknown Artist";
        this.album = album != null ? album : "Unknown Album";
        this.path = path != null ? path : "";
        this.uriString = uri != null ? uri.toString() : "";
        this.duration = duration;
        this.size = size;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getPath() { return path; }
    public Uri getUri() { return uriString != null ? Uri.parse(uriString) : null; }
    public long getDuration() { return duration; }
    public long getSize() { return size; }
}

