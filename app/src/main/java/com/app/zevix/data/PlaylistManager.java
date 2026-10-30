package com.app.zevix.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.zevix.model.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PlaylistManager {

    private static final String PREF_NAME = "zevix_playlists_pref";
    private static final String KEY_PLAYLISTS = "custom_playlists_json";
    private static PlaylistManager instance;
    private final SharedPreferences prefs;

    private PlaylistManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PlaylistManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlaylistManager(context);
        }
        return instance;
    }

    public List<String> getPlaylistNames() {
        List<String> list = new ArrayList<>();
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                list.add(keys.next());
            }
        } catch (Exception ignored) {}
        return list;
    }

    public boolean createPlaylist(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has(name)) return false; // already exists
            obj.put(name, new JSONArray());
            prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void deletePlaylist(String name) {
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            obj.remove(name);
            prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
        } catch (Exception ignored) {}
    }

    public void renamePlaylist(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) return;
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has(oldName)) {
                JSONArray items = obj.getJSONArray(oldName);
                obj.remove(oldName);
                obj.put(newName, items);
                prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public void addVideoToPlaylist(String playlistName, long videoId) {
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray arr = obj.has(playlistName) ? obj.getJSONArray(playlistName) : new JSONArray();
            // check duplicate
            boolean exists = false;
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getLong(i) == videoId) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                arr.put(videoId);
                obj.put(playlistName, arr);
                prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public void removeVideoFromPlaylist(String playlistName, long videoId) {
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has(playlistName)) {
                JSONArray arr = obj.getJSONArray(playlistName);
                JSONArray newArr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.getLong(i) != videoId) {
                        newArr.put(arr.getLong(i));
                    }
                }
                obj.put(playlistName, newArr);
                prefs.edit().putString(KEY_PLAYLISTS, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public Set<Long> getVideoIdsForPlaylist(String playlistName) {
        Set<Long> ids = new HashSet<>();
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has(playlistName)) {
                JSONArray arr = obj.getJSONArray(playlistName);
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getLong(i));
                }
            }
        } catch (Exception ignored) {}
        return ids;
    }

    public int getVideoCount(String playlistName) {
        try {
            String jsonStr = prefs.getString(KEY_PLAYLISTS, "{}");
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has(playlistName)) {
                return obj.getJSONArray(playlistName).length();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}

