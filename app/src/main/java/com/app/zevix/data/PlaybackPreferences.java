package com.app.zevix.data;

import android.content.Context;
import android.content.SharedPreferences;

public class PlaybackPreferences {
    private static final String PREF_NAME = "zevix_prefs";
    private static final String LEGACY_PREF_NAME = "legacy_app_prefs";
    private static final String KEY_RESUME_PREFIX = "resume_pos_";
    private static final String KEY_SORT_ORDER = "sort_order";
    private static final String KEY_FOLDER_SORT_ORDER = "folder_sort_order";
    private static final String KEY_FOLDER_GRID_MODE = "folder_grid_mode";
    private static final String KEY_ASPECT_RATIO = "aspect_ratio";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_EQ_ENABLED = "eq_enabled";
    private static final String KEY_EQ_PRESET = "eq_preset";
    private static final String KEY_BASS_BOOST = "bass_boost";
    private static final String KEY_PRIVATE_PIN = "private_pin";
    private static final String KEY_FAVORITES = "favorite_video_ids";
    private static final String KEY_THEME = "app_theme_index";

    private final SharedPreferences prefs;

    public PlaybackPreferences(Context context) {
        SharedPreferences oldPrefs = context.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE);
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (oldPrefs.getAll().size() > 0 && prefs.getAll().isEmpty()) {
            SharedPreferences.Editor editor = prefs.edit();
            for (java.util.Map.Entry<String, ?> entry : oldPrefs.getAll().entrySet()) {
                Object val = entry.getValue();
                if (val instanceof String) editor.putString(entry.getKey(), (String) val);
                else if (val instanceof Integer) editor.putInt(entry.getKey(), (Integer) val);
                else if (val instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) val);
                else if (val instanceof Long) editor.putLong(entry.getKey(), (Long) val);
                else if (val instanceof Float) editor.putFloat(entry.getKey(), (Float) val);
                else if (val instanceof java.util.Set) editor.putStringSet(entry.getKey(), (java.util.Set<String>) val);
            }
            editor.apply();
        }
    }

    private static String hashPin(String input) {
        if (input == null || input.isEmpty()) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return input;
        }
    }

    public void setPrivatePin(String pin) {
        prefs.edit().putString(KEY_PRIVATE_PIN, hashPin(pin)).apply();
    }

    public String getPrivatePin() {
        return prefs.getString(KEY_PRIVATE_PIN, "");
    }

    public boolean hasPrivatePin() {
        return !getPrivatePin().isEmpty();
    }

    public boolean checkPrivatePin(String inputPin) {
        if (inputPin == null) return false;
        String stored = getPrivatePin();
        if (stored.isEmpty()) return false;
        if (stored.equals(hashPin(inputPin))) {
            return true;
        }
        if (stored.equals(inputPin)) {
            setPrivatePin(inputPin); // upgrade to SHA-256
            return true;
        }
        return false;
    }

    public void addFavorite(long id) {
        java.util.Set<String> set = new java.util.HashSet<>(prefs.getStringSet(KEY_FAVORITES, new java.util.HashSet<>()));
        set.add(String.valueOf(id));
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply();
    }

    public void removeFavorite(long id) {
        java.util.Set<String> set = new java.util.HashSet<>(prefs.getStringSet(KEY_FAVORITES, new java.util.HashSet<>()));
        set.remove(String.valueOf(id));
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply();
    }

    public boolean isFavorite(long id) {
        java.util.Set<String> set = prefs.getStringSet(KEY_FAVORITES, java.util.Collections.emptySet());
        return set.contains(String.valueOf(id));
    }

    public boolean toggleFavorite(long id) {
        if (isFavorite(id)) {
            removeFavorite(id);
            return false;
        } else {
            addFavorite(id);
            return true;
        }
    }

    public boolean isSubtitleEnabled() {
        return prefs.getBoolean("subtitle_enabled", true);
    }

    public void setSubtitleEnabled(boolean enabled) {
        prefs.edit().putBoolean("subtitle_enabled", enabled).apply();
    }

    public int getThemeIndex() {
        return prefs.getInt(KEY_THEME, 0);
    }

    public void setThemeIndex(int index) {
        prefs.edit().putInt(KEY_THEME, index).apply();
    }

    public void savePosition(String path, long position) {
        if (path == null) return;
        prefs.edit().putLong(KEY_RESUME_PREFIX + path.hashCode(), position).apply();
    }

    public long getPosition(String path) {
        if (path == null) return 0;
        return prefs.getLong(KEY_RESUME_PREFIX + path.hashCode(), 0);
    }

    public void savePlaybackPosition(long id, long position) {
        prefs.edit().putLong(KEY_RESUME_PREFIX + id, position).apply();
    }

    public long getPlaybackPosition(long id) {
        return prefs.getLong(KEY_RESUME_PREFIX + id, 0);
    }

    public void setSortOrder(int order) {
        prefs.edit().putInt(KEY_SORT_ORDER, order).apply();
    }

    public int getSortOrder() {
        return prefs.getInt(KEY_SORT_ORDER, 0); // 0: Date Descending default
    }

    public void setFolderSortOrder(int order) {
        prefs.edit().putInt(KEY_FOLDER_SORT_ORDER, order).apply();
    }

    public int getFolderSortOrder() {
        return prefs.getInt(KEY_FOLDER_SORT_ORDER, 2); // 2: Name Ascending default
    }

    public void setFolderGridMode(boolean isGrid) {
        prefs.edit().putBoolean(KEY_FOLDER_GRID_MODE, isGrid).apply();
    }

    public boolean isFolderGridMode() {
        return prefs.getBoolean(KEY_FOLDER_GRID_MODE, true); // true: Grid default (matching screenshot)
    }

    public void setAspectRatio(int ratio) {
        prefs.edit().putInt(KEY_ASPECT_RATIO, ratio).apply();
    }

    public int getAspectRatio() {
        return prefs.getInt(KEY_ASPECT_RATIO, 0); // 0: Fit
    }

    public void setNightMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply();
    }

    public boolean isNightMode() {
        return prefs.getBoolean(KEY_NIGHT_MODE, false);
    }

    public void setEqualizerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_EQ_ENABLED, enabled).apply();
    }

    public boolean isEqualizerEnabled() {
        return prefs.getBoolean(KEY_EQ_ENABLED, false);
    }

    public void setEqualizerPreset(int preset) {
        prefs.edit().putInt(KEY_EQ_PRESET, preset).apply();
    }

    public int getEqualizerPreset() {
        return prefs.getInt(KEY_EQ_PRESET, 0);
    }

    public void setBassBoostStrength(int strength) {
        prefs.edit().putInt(KEY_BASS_BOOST, strength).apply();
    }

    public int getBassBoostStrength() {
        return prefs.getInt(KEY_BASS_BOOST, 0);
    }

    public void setVirtualizerStrength(int strength) {
        prefs.edit().putInt("virtualizer_strength", strength).apply();
    }

    public int getVirtualizerStrength() {
        return prefs.getInt("virtualizer_strength", 0);
    }

    public void setReverbPreset(int preset) {
        prefs.edit().putInt("reverb_preset", preset).apply();
    }

    public int getReverbPreset() {
        return prefs.getInt("reverb_preset", 0);
    }

    public void setEqualizerBands(int[] bands) {
        if (bands == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bands.length; i++) {
            sb.append(bands[i]);
            if (i < bands.length - 1) sb.append(",");
        }
        prefs.edit().putString("eq_custom_bands", sb.toString()).apply();
    }

    public int[] getEqualizerBands() {
        String saved = prefs.getString("eq_custom_bands", null);
        if (saved != null && !saved.isEmpty()) {
            String[] parts = saved.split(",");
            if (parts.length == 5) {
                int[] res = new int[5];
                try {
                    for (int i = 0; i < 5; i++) res[i] = Integer.parseInt(parts[i]);
                    return res;
                } catch (Exception ignored) {}
            }
        }
        return new int[]{1500, 1500, 1500, 1500, 1500};
    }

    public boolean isSoftwareDecoder() {
        return prefs.getBoolean("software_decoder", false);
    }

    public void setSoftwareDecoder(boolean sw) {
        prefs.edit().putBoolean("software_decoder", sw).apply();
    }

    public boolean isScreenshotButtonEnabled() {
        return prefs.getBoolean("screenshot_btn_enabled", true);
    }

    public void setScreenshotButtonEnabled(boolean enabled) {
        prefs.edit().putBoolean("screenshot_btn_enabled", enabled).apply();
    }

    public int getRepeatMode() {
        return prefs.getInt("player_repeat_mode", 2); // default: REPEAT_MODE_ALL = 2
    }

    public void setRepeatMode(int mode) {
        prefs.edit().putInt("player_repeat_mode", mode).apply();
    }

    public boolean isDirectRatioSwitchEnabled() {
        return prefs.getBoolean("direct_ratio_switch", false);
    }

    public void setDirectRatioSwitchEnabled(boolean enabled) {
        prefs.edit().putBoolean("direct_ratio_switch", enabled).apply();
    }

    public boolean isRememberRatioEnabled() {
        return prefs.getBoolean("remember_ratio", false);
    }

    public void setRememberRatioEnabled(boolean enabled) {
        prefs.edit().putBoolean("remember_ratio", enabled).apply();
    }
}

