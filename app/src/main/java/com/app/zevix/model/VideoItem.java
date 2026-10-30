package com.app.zevix.model;

import android.net.Uri;
import java.io.Serializable;

public class VideoItem implements Serializable {
    private final long id;
    private final String title;
    private final String displayName;
    private final String path;
    private final String uriString;
    private final long duration;
    private final long size;
    private final long dateAdded;
    private final int width;
    private final int height;
    private final String folderName;
    private final String folderPath;

    public VideoItem(long id, String title, String displayName, String path, Uri uri,
                     long duration, long size, long dateAdded, int width, int height,
                     String folderName, String folderPath) {
        this.id = id;
        this.title = title != null ? title : "";
        this.displayName = displayName != null ? displayName : "";
        this.path = path != null ? path : "";
        this.uriString = uri != null ? uri.toString() : "";
        this.duration = duration;
        this.size = size;
        this.dateAdded = dateAdded;
        this.width = width;
        this.height = height;
        this.folderName = folderName != null ? folderName : "";
        this.folderPath = folderPath != null ? folderPath : "";
    }

    public long getId() { return id; }
    public String getTitle() { return (title != null && !title.isEmpty()) ? title : displayName; }
    public String getDisplayName() { return displayName; }
    public String getPath() { return path; }
    public Uri getUri() { return uriString != null ? Uri.parse(uriString) : null; }
    public String getUriString() { return uriString; }
    public long getDuration() { return duration; }
    public long getSize() { return size; }
    public long getDateAdded() { return dateAdded; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getFolderName() { return folderName; }
    public String getFolderPath() { return folderPath; }

    public String getResolutionBadge() {
        if (width >= 3840 || height >= 2160) return "4K";
        if (width >= 1920 || height >= 1080) return "FHD";
        if (width >= 1280 || height >= 720) return "HD";
        return "";
    }
}

