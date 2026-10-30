package com.app.zevix.data;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import com.app.zevix.model.VideoItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PrivateVaultManager {

    private static final String VAULT_DIR_NAME = ".ZevixVault";
    private static final String LEGACY_VAULT_DIR_NAME = ".PrivateVault";

    public static File getVaultDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), VAULT_DIR_NAME);
        if (!dir.exists()) {
            File legacy = new File(context.getExternalFilesDir(null), LEGACY_VAULT_DIR_NAME);
            if (legacy.exists()) {
                legacy.renameTo(dir);
            } else {
                dir.mkdirs();
            }
        }
        File nomedia = new File(dir, ".nomedia");
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile();
            } catch (Exception ignored) {}
        }
        return dir;
    }

    public static boolean moveToVault(Context context, VideoItem video) {
        if (video == null || video.getPath() == null) return false;
        try {
            File src = new File(video.getPath());
            if (!src.exists()) return false;

            File vaultDir = getVaultDir(context);
            File dest = new File(vaultDir, src.getName());
            if (dest.exists()) {
                dest = new File(vaultDir, System.currentTimeMillis() + "_" + src.getName());
            }

            boolean renamed = false;
            try {
                renamed = src.renameTo(dest);
            } catch (Exception ignored) {}

            if (!renamed) {
                // Copy & Delete fallback
                try (FileInputStream in = new FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                boolean deleted = TrashManager.forceDeleteFile(context, src, video.getUri());
                if (!deleted && src.exists()) {
                    dest.delete();
                    return false;
                }
            } else {
                try {
                    if (video.getUri() != null) {
                        context.getContentResolver().delete(video.getUri(), null, null);
                    }
                } catch (Exception ignored) {}
                try {
                    android.media.MediaScannerConnection.scanFile(context, new String[]{video.getPath()}, null, null);
                } catch (Exception ignored) {}
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<VideoItem> getVaultVideos(Context context) {
        List<VideoItem> list = new ArrayList<>();
        File vaultDir = getVaultDir(context);
        File[] files = vaultDir.listFiles();
        if (files == null) return list;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        for (File file : files) {
            if (file.isDirectory() || file.getName().startsWith(".")) continue;
            String name = file.getName();
            long size = file.length();
            long date = file.lastModified() / 1000;
            long duration = 0;
            int width = 0;
            int height = 0;

            try {
                retriever.setDataSource(file.getAbsolutePath());
                String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durStr != null) duration = Long.parseLong(durStr);
                String wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                if (wStr != null) width = Integer.parseInt(wStr);
                String hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                if (hStr != null) height = Integer.parseInt(hStr);
            } catch (Exception ignored) {}

            Uri uri = Uri.fromFile(file);
            list.add(new VideoItem(file.hashCode(), name, name, file.getAbsolutePath(), uri,
                    duration, size, date, width, height, "Private Vault", vaultDir.getAbsolutePath()));
        }
        try {
            retriever.release();
        } catch (Exception ignored) {}

        return list;
    }

    public static boolean restoreFromVault(Context context, VideoItem video) {
        if (video == null || video.getPath() == null) return false;
        try {
            File src = new File(video.getPath());
            if (!src.exists()) return false;

            File moviesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES);
            if (!moviesDir.exists()) moviesDir.mkdirs();

            File dest = new File(moviesDir, src.getName());
            boolean renamed = false;
            try {
                renamed = src.renameTo(dest);
            } catch (Exception ignored) {}

            if (!renamed) {
                try (FileInputStream in = new FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                TrashManager.forceDeleteFile(context, src, null);
            }
            android.media.MediaScannerConnection.scanFile(context, new String[]{dest.getAbsolutePath()}, null, null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteFromVault(Context context, VideoItem video) {
        if (video == null || video.getPath() == null) return false;
        try {
            File src = new File(video.getPath());
            return TrashManager.forceDeleteFile(context, src, video.getUri());
        } catch (Exception e) {
            return false;
        }
    }
}

