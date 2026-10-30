package com.app.zevix.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FolderItem implements Serializable {
    private final String folderName;
    private final String folderPath;
    private final List<VideoItem> videos;

    public FolderItem(String folderName, String folderPath) {
        this.folderName = folderName != null ? folderName : "";
        this.folderPath = folderPath != null ? folderPath : "";
        this.videos = new ArrayList<>();
    }

    public String getFolderName() { return folderName; }
    public String getFolderPath() { return folderPath; }
    public List<VideoItem> getVideos() { return videos; }

    public void addVideo(VideoItem video) {
        videos.add(video);
    }

    public int getVideoCount() {
        return videos.size();
    }

    public long getTotalSize() {
        long total = 0;
        for (VideoItem item : videos) {
            total += item.getSize();
        }
        return total;
    }

    public VideoItem getFirstVideo() {
        return videos.isEmpty() ? null : videos.get(0);
    }
}

