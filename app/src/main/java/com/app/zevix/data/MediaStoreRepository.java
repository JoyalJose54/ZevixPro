package com.app.zevix.data;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.app.zevix.model.FolderItem;
import com.app.zevix.model.MusicItem;
import com.app.zevix.model.VideoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MediaStoreRepository {

    public static final int SORT_DATE_DESC = 0;
    public static final int SORT_DATE_ASC = 1;
    public static final int SORT_NAME_ASC = 2;
    public static final int SORT_NAME_DESC = 3;
    public static final int SORT_SIZE_DESC = 4;
    public static final int SORT_SIZE_ASC = 5;
    public static final int SORT_LENGTH_DESC = 6;
    public static final int SORT_LENGTH_ASC = 7;

    public static List<VideoItem> getAllVideos(Context context, int sortOrder) {
        List<VideoItem> videoList = new ArrayList<>();
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        };

        String sortClause;
        switch (sortOrder) {
            case SORT_DATE_ASC:
                sortClause = MediaStore.Video.Media.DATE_ADDED + " ASC";
                break;
            case SORT_NAME_ASC:
                sortClause = MediaStore.Video.Media.DISPLAY_NAME + " ASC";
                break;
            case SORT_NAME_DESC:
                sortClause = MediaStore.Video.Media.DISPLAY_NAME + " DESC";
                break;
            case SORT_SIZE_DESC:
                sortClause = MediaStore.Video.Media.SIZE + " DESC";
                break;
            case SORT_SIZE_ASC:
                sortClause = MediaStore.Video.Media.SIZE + " ASC";
                break;
            case SORT_LENGTH_DESC:
                sortClause = MediaStore.Video.Media.DURATION + " DESC";
                break;
            case SORT_LENGTH_ASC:
                sortClause = MediaStore.Video.Media.DURATION + " ASC";
                break;
            case SORT_DATE_DESC:
            default:
                sortClause = MediaStore.Video.Media.DATE_ADDED + " DESC";
                break;
        }

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                null,
                null,
                sortClause
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
                int widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
                int heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
                int bucketColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String displayName = cursor.getString(nameColumn);
                    String path = cursor.getString(dataColumn);
                    long duration = cursor.getLong(durationColumn);
                    long size = cursor.getLong(sizeColumn);
                    long dateAdded = cursor.getLong(dateColumn);
                    int width = cursor.getInt(widthColumn);
                    int height = cursor.getInt(heightColumn);

                    String folderName = "Internal Storage";
                    if (bucketColumn != -1 && !cursor.isNull(bucketColumn)) {
                        folderName = cursor.getString(bucketColumn);
                    } else if (path != null) {
                        File file = new File(path);
                        if (file.getParentFile() != null) {
                            folderName = file.getParentFile().getName();
                        }
                    }

                    String folderPath = "";
                    if (path != null) {
                        File file = new File(path);
                        if (file.getParentFile() != null) {
                            folderPath = file.getParentFile().getAbsolutePath();
                        }
                    }

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                    if (size > 0 && path != null && new File(path).exists()) {
                        TrashManager tm = TrashManager.getInstance(context);
                        if (tm.isPathTrashed(path) || tm.isPathPermanentlyDeleted(path)) {
                            continue;
                        }
                        videoList.add(new VideoItem(id, title, displayName, path, contentUri,
                                duration, size, dateAdded, width, height, folderName, folderPath));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videoList;
    }

    public static List<FolderItem> getAllFolders(Context context, int sortOrder) {
        List<VideoItem> allVideos = getAllVideos(context, sortOrder);
        Map<String, FolderItem> folderMap = new LinkedHashMap<>();

        for (VideoItem video : allVideos) {
            String folderKey = video.getFolderPath();
            if (folderKey == null || folderKey.isEmpty()) {
                folderKey = video.getFolderName();
            }

            FolderItem folder = folderMap.get(folderKey);
            if (folder == null) {
                folder = new FolderItem(video.getFolderName(), video.getFolderPath());
                folderMap.put(folderKey, folder);
            }
            folder.addVideo(video);
        }

        List<FolderItem> folderList = new ArrayList<>(folderMap.values());
        sortFolders(folderList, sortOrder);
        return folderList;
    }

    public static void sortFolders(List<FolderItem> folders, int sortOrder) {
        if (folders == null || folders.isEmpty()) return;
        FolderItem recentAdded = null;
        if (!folders.isEmpty() && "Recent Added".equalsIgnoreCase(folders.get(0).getFolderName())) {
            recentAdded = folders.remove(0);
        }

        switch (sortOrder) {
            case SORT_NAME_DESC:
                Collections.sort(folders, (a, b) -> b.getFolderName().compareToIgnoreCase(a.getFolderName()));
                break;
            case SORT_SIZE_DESC:
                Collections.sort(folders, (a, b) -> Long.compare(b.getTotalSize(), a.getTotalSize()));
                break;
            case SORT_SIZE_ASC:
                Collections.sort(folders, (a, b) -> Long.compare(a.getTotalSize(), b.getTotalSize()));
                break;
            case SORT_NAME_ASC:
            default:
                Collections.sort(folders, (a, b) -> a.getFolderName().compareToIgnoreCase(b.getFolderName()));
                break;
        }

        if (recentAdded != null) {
            folders.add(0, recentAdded);
        }
    }

    public static void sortVideos(List<VideoItem> videos, int sortOrder) {
        if (videos == null || videos.isEmpty()) return;
        switch (sortOrder) {
            case SORT_DATE_ASC:
                Collections.sort(videos, (a, b) -> Long.compare(a.getDateAdded(), b.getDateAdded()));
                break;
            case SORT_NAME_ASC:
                Collections.sort(videos, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
            case SORT_NAME_DESC:
                Collections.sort(videos, (a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;
            case SORT_SIZE_DESC:
                Collections.sort(videos, (a, b) -> Long.compare(b.getSize(), a.getSize()));
                break;
            case SORT_SIZE_ASC:
                Collections.sort(videos, (a, b) -> Long.compare(a.getSize(), b.getSize()));
                break;
            case SORT_LENGTH_DESC:
                Collections.sort(videos, (a, b) -> Long.compare(b.getDuration(), a.getDuration()));
                break;
            case SORT_LENGTH_ASC:
                Collections.sort(videos, (a, b) -> Long.compare(a.getDuration(), b.getDuration()));
                break;
            case SORT_DATE_DESC:
            default:
                Collections.sort(videos, (a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
                break;
        }
    }

    public static List<MusicItem> getAllMusic(Context context) {
        List<MusicItem> musicList = new ArrayList<>();
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    String path = cursor.getString(dataCol);
                    long duration = cursor.getLong(durationCol);
                    long size = cursor.getLong(sizeCol);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    if (size > 0) {
                        musicList.add(new MusicItem(id, title, artist, album, path, contentUri, duration, size));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return musicList;
    }
}
