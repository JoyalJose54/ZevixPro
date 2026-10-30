package com.app.zevix.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;

import com.app.zevix.model.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrashManager {

    public static class TrashItem {
        private final long id;
        private final String originalPath;
        private final String trashPath;
        private final String title;
        private final long duration;
        private final long size;
        private final long dateAdded;
        private final long trashedTimestamp;
        private final String originalUriString;

        public TrashItem(long id, String originalPath, String trashPath, String title,
                         long duration, long size, long dateAdded, long trashedTimestamp) {
            this(id, originalPath, trashPath, title, duration, size, dateAdded, trashedTimestamp, null);
        }

        public TrashItem(long id, String originalPath, String trashPath, String title,
                         long duration, long size, long dateAdded, long trashedTimestamp, String originalUriString) {
            this.id = id;
            this.originalPath = originalPath;
            this.trashPath = trashPath;
            this.title = title;
            this.duration = duration;
            this.size = size;
            this.dateAdded = dateAdded;
            this.trashedTimestamp = trashedTimestamp;
            this.originalUriString = originalUriString;
        }

        public long getId() { return id; }
        public String getOriginalPath() { return originalPath; }
        public String getTrashPath() { return trashPath; }
        public String getTitle() { return title; }
        public long getDuration() { return duration; }
        public long getSize() { return size; }
        public long getDateAdded() { return dateAdded; }
        public long getTrashedTimestamp() { return trashedTimestamp; }
        public String getOriginalUriString() { return originalUriString; }
        public Uri getOriginalUri() {
            return (originalUriString != null && !originalUriString.isEmpty()) ? Uri.parse(originalUriString) : null;
        }

        public int getDaysRemaining() {
            long elapsedMillis = System.currentTimeMillis() - trashedTimestamp;
            int daysPassed = (int) (elapsedMillis / (1000L * 60 * 60 * 24));
            return Math.max(0, 30 - daysPassed);
        }
    }

    private static final String PREF_NAME = "zevix_trash_pref";
    private static final String KEY_TRASH_ITEMS = "trash_items_json";
    private static final String KEY_PERM_DELETED = "perm_deleted_paths_set";
    private static TrashManager instance;
    private final Context context;
    private final SharedPreferences prefs;

    private TrashManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized TrashManager getInstance(Context context) {
        if (instance == null) {
            instance = new TrashManager(context);
        }
        return instance;
    }

    public File getTrashDirectory() {
        File dir = new File(context.getExternalFilesDir(null), "trash");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File nomedia = new File(dir, ".nomedia");
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile();
            } catch (Exception ignored) {}
        }
        return dir;
    }

    public synchronized boolean isPathTrashed(String path) {
        if (path == null) return false;
        List<TrashItem> items = loadItemsInternal();
        for (TrashItem item : items) {
            if (path.equalsIgnoreCase(item.getOriginalPath())) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isPathPermanentlyDeleted(String path) {
        if (path == null) return false;
        Set<String> set = prefs.getStringSet(KEY_PERM_DELETED, null);
        return set != null && set.contains(path.toLowerCase());
    }

    private synchronized void addPermanentlyDeletedPath(String path) {
        if (path == null) return;
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_PERM_DELETED, new HashSet<>()));
        set.add(path.toLowerCase());
        prefs.edit().putStringSet(KEY_PERM_DELETED, set).apply();
    }

    private synchronized void removePermanentlyDeletedPath(String path) {
        if (path == null) return;
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_PERM_DELETED, new HashSet<>()));
        if (set.remove(path.toLowerCase())) {
            prefs.edit().putStringSet(KEY_PERM_DELETED, set).apply();
        }
    }

    /**
     * Completely and permanently deletes a file from the device without leaving traces.
     */
    public static boolean forceDeleteFile(Context context, File file, Uri uri) {
        if (file == null && uri == null) return true;
        boolean deleted = false;

        // 1. Direct file deletion
        if (file != null && file.exists()) {
            try {
                deleted = file.delete();
            } catch (Exception ignored) {}
        }

        // 2. ContentResolver deletion by URI
        if (uri != null) {
            try {
                int count = context.getContentResolver().delete(uri, null, null);
                if (count > 0) deleted = true;
            } catch (Exception ignored) {}
        }

        // 3. ContentResolver deletion by path in Video MediaStore
        if (file != null) {
            try {
                int count = context.getContentResolver().delete(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{file.getAbsolutePath()}
                );
                if (count > 0) deleted = true;
            } catch (Exception ignored) {}

            // 4. Also delete from generic MediaStore.Files
            try {
                Uri filesUri = MediaStore.Files.getContentUri("external");
                int count = context.getContentResolver().delete(
                        filesUri,
                        MediaStore.Files.FileColumns.DATA + "=?",
                        new String[]{file.getAbsolutePath()}
                );
                if (count > 0) deleted = true;
            } catch (Exception ignored) {}
        }

        // 5. Truncate to 0 bytes and delete again if file still exists on disk
        if (file != null && file.exists()) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.getChannel().truncate(0);
            } catch (Exception ignored) {}
            try {
                deleted = file.delete();
            } catch (Exception ignored) {}
        }

        // 6. Notify MediaScanner to purge index from Android system gallery
        if (file != null) {
            try {
                android.media.MediaScannerConnection.scanFile(
                        context.getApplicationContext(),
                        new String[]{file.getAbsolutePath()},
                        null,
                        (scannedPath, scannedUri) -> {
                            if (scannedUri != null) {
                                try {
                                    context.getContentResolver().delete(scannedUri, null, null);
                                } catch (Exception ignored) {}
                            }
                        }
                );
            } catch (Exception ignored) {}
        }

        return deleted || (file != null && !file.exists());
    }

    public synchronized boolean moveToTrash(VideoItem video) {
        if (video == null || video.getPath() == null) return false;
        File sourceFile = new File(video.getPath());
        if (!sourceFile.exists()) return false;

        File trashDir = getTrashDirectory();
        File destFile = new File(trashDir, System.currentTimeMillis() + "_" + sourceFile.getName());

        boolean moved = false;
        try {
            moved = sourceFile.renameTo(destFile);
        } catch (Exception ignored) {}

        if (!moved) {
            boolean copied = copyFile(sourceFile, destFile);
            if (copied) {
                boolean deleted = forceDeleteFile(context, sourceFile, video.getUri());
                if (!deleted && sourceFile.exists()) {
                    // Critical: if source file could NOT be removed from public folder,
                    // cancel move and delete destFile to prevent leaving duplicate copies
                    destFile.delete();
                    return false;
                }
                moved = true;
            }
        } else {
            // renameTo succeeded, clean MediaStore entry for old location
            try {
                if (video.getUri() != null) {
                    context.getContentResolver().delete(video.getUri(), null, null);
                }
            } catch (Exception ignored) {}
            try {
                android.media.MediaScannerConnection.scanFile(context, new String[]{video.getPath()}, null, null);
            } catch (Exception ignored) {}
        }

        if (moved) {
            try {
                String jsonStr = prefs.getString(KEY_TRASH_ITEMS, "[]");
                JSONArray arr = new JSONArray(jsonStr);
                JSONObject obj = new JSONObject();
                obj.put("id", video.getId());
                obj.put("originalPath", video.getPath());
                obj.put("trashPath", destFile.getAbsolutePath());
                obj.put("title", video.getTitle());
                obj.put("duration", video.getDuration());
                obj.put("size", video.getSize());
                obj.put("dateAdded", video.getDateAdded());
                obj.put("trashedTimestamp", System.currentTimeMillis());
                if (video.getUri() != null) {
                    obj.put("originalUri", video.getUri().toString());
                }
                arr.put(obj);
                prefs.edit().putString(KEY_TRASH_ITEMS, arr.toString()).apply();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Checks all items in Trash and permanently deletes files that are older than 30 days.
     */
    public synchronized void cleanExpiredTrash() {
        long now = System.currentTimeMillis();
        long thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L;
        List<TrashItem> list = loadItemsInternal();
        List<TrashItem> expired = new ArrayList<>();
        for (TrashItem item : list) {
            if (now - item.getTrashedTimestamp() >= thirtyDaysMillis || item.getDaysRemaining() <= 0) {
                expired.add(item);
            }
        }
        for (TrashItem item : expired) {
            deletePermanently(item);
        }

        // Also clean up any orphan files in trash folder older than 30 days
        try {
            File trashDir = getTrashDirectory();
            File[] files = trashDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().equals(".nomedia")) continue;
                    if (now - f.lastModified() >= thirtyDaysMillis) {
                        forceDeleteFile(context, f, null);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Wipes any lingering residual files that were previously marked deleted but left on disk.
     */
    public synchronized void cleanupResidualPermDeletedFiles() {
        Set<String> set = prefs.getStringSet(KEY_PERM_DELETED, null);
        if (set != null && !set.isEmpty()) {
            Set<String> copy = new HashSet<>(set);
            Set<String> updated = new HashSet<>(set);
            for (String path : copy) {
                File f = new File(path);
                if (f.exists()) {
                    forceDeleteFile(context, f, null);
                }
                if (!f.exists()) {
                    updated.remove(path);
                }
            }
            prefs.edit().putStringSet(KEY_PERM_DELETED, updated).apply();
        }
    }

    private List<TrashItem> loadItemsInternal() {
        List<TrashItem> list = new ArrayList<>();
        try {
            String jsonStr = prefs.getString(KEY_TRASH_ITEMS, "[]");
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new TrashItem(
                        obj.optLong("id"),
                        obj.optString("originalPath"),
                        obj.optString("trashPath"),
                        obj.optString("title"),
                        obj.optLong("duration"),
                        obj.optLong("size"),
                        obj.optLong("dateAdded"),
                        obj.optLong("trashedTimestamp"),
                        obj.optString("originalUri", null)
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public synchronized List<TrashItem> getTrashItems() {
        cleanExpiredTrash();
        return loadItemsInternal();
    }

    public synchronized boolean restoreItem(TrashItem item) {
        if (item == null) return false;
        File trashFile = new File(item.getTrashPath());
        File origFile = new File(item.getOriginalPath());

        // Ensure parent directories exist
        if (origFile.getParentFile() != null && !origFile.getParentFile().exists()) {
            origFile.getParentFile().mkdirs();
        }

        boolean restored = false;
        try {
            restored = trashFile.renameTo(origFile);
        } catch (Exception ignored) {}

        if (!restored) {
            boolean copied = copyFile(trashFile, origFile);
            if (copied) {
                forceDeleteFile(context, trashFile, null);
                restored = true;
            }
        }

        if (restored) {
            removePermanentlyDeletedPath(item.getOriginalPath());
            try {
                android.media.MediaScannerConnection.scanFile(context, new String[]{origFile.getAbsolutePath()}, null, null);
            } catch (Exception ignored) {}
        }

        removeFromPreferences(item.getId());
        return restored;
    }

    /**
     * Permanently deletes a file from Trash and the entire device. Leaves no traces.
     */
    public synchronized void deletePermanently(TrashItem item) {
        if (item == null) return;

        // 1. Delete copy in trash directory
        if (item.getTrashPath() != null) {
            File trashFile = new File(item.getTrashPath());
            if (trashFile.exists()) {
                forceDeleteFile(context, trashFile, null);
            }
        }

        // 2. Delete original file on disk if it still exists
        if (item.getOriginalPath() != null) {
            File origFile = new File(item.getOriginalPath());
            if (origFile.exists()) {
                forceDeleteFile(context, origFile, item.getOriginalUri());
            }

            // 3. Remove from MediaStore
            try {
                context.getContentResolver().delete(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{item.getOriginalPath()}
                );
            } catch (Exception ignored) {}

            try {
                android.media.MediaScannerConnection.scanFile(context, new String[]{item.getOriginalPath()}, null, (path, uri) -> {
                    if (uri != null) {
                        try {
                            context.getContentResolver().delete(uri, null, null);
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}

            removePermanentlyDeletedPath(item.getOriginalPath());
        }

        removeFromPreferences(item.getId());
    }

    public synchronized void emptyTrash() {
        List<TrashItem> items = loadItemsInternal();
        for (TrashItem item : items) {
            deletePermanently(item);
        }
        // Also wipe any physical files in trash directory
        try {
            File trashDir = getTrashDirectory();
            File[] files = trashDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.getName().equals(".nomedia")) {
                        forceDeleteFile(context, f, null);
                    }
                }
            }
        } catch (Exception ignored) {}

        prefs.edit().putString(KEY_TRASH_ITEMS, "[]").apply();
    }

    private void removeFromPreferences(long id) {
        try {
            String jsonStr = prefs.getString(KEY_TRASH_ITEMS, "[]");
            JSONArray arr = new JSONArray(jsonStr);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.optLong("id") != id) {
                    newArr.put(obj);
                }
            }
            prefs.edit().putString(KEY_TRASH_ITEMS, newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private boolean copyFile(File src, File dst) {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
