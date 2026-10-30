package com.app.zevix.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.biometric.BiometricManager;

public class AppLockManager {

    private static final String PREF_NAME = "zevix_app_lock_prefs";
    private static final String KEY_APP_LOCK_ENABLED = "app_lock_enabled";
    private static final String KEY_LOCK_TYPE = "lock_type"; // "PIN" or "PATTERN"
    private static final String KEY_PIN = "app_lock_pin";
    private static final String KEY_PATTERN = "app_lock_pattern";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    public static final String LOCK_TYPE_PIN = "PIN";
    public static final String LOCK_TYPE_PATTERN = "PATTERN";

    private static AppLockManager instance;
    private final SharedPreferences prefs;

    // In-memory unlocked state for the current foreground session
    private boolean isAppUnlocked = false;

    private AppLockManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AppLockManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppLockManager(context);
        }
        return instance;
    }

    public boolean isAppLockEnabled() {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false) && (hasPin() || hasPattern());
    }

    public void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply();
    }

    public String getLockType() {
        return prefs.getString(KEY_LOCK_TYPE, LOCK_TYPE_PIN);
    }

    public void setLockType(String type) {
        prefs.edit().putString(KEY_LOCK_TYPE, type).apply();
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

    public String getPin() {
        return prefs.getString(KEY_PIN, "");
    }

    public void setPin(String pin) {
        prefs.edit().putString(KEY_PIN, hashPin(pin)).apply();
    }

    public boolean hasPin() {
        return !prefs.getString(KEY_PIN, "").isEmpty();
    }

    public boolean checkPin(String inputPin) {
        if (inputPin == null) return false;
        String stored = prefs.getString(KEY_PIN, "");
        if (stored.isEmpty()) return false;
        if (stored.equals(hashPin(inputPin))) {
            return true;
        }
        if (stored.equals(inputPin)) {
            setPin(inputPin);
            return true;
        }
        return false;
    }

    public String getPattern() {
        return prefs.getString(KEY_PATTERN, "");
    }

    public void setPattern(String pattern) {
        prefs.edit().putString(KEY_PATTERN, hashPin(pattern)).apply();
    }

    public boolean hasPattern() {
        return !prefs.getString(KEY_PATTERN, "").isEmpty();
    }

    public boolean checkPattern(String inputPattern) {
        if (inputPattern == null) return false;
        String stored = prefs.getString(KEY_PATTERN, "");
        if (stored.isEmpty()) return false;
        if (stored.equals(hashPin(inputPattern))) {
            return true;
        }
        if (stored.equals(inputPattern)) {
            setPattern(inputPattern);
            return true;
        }
        return false;
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricSupported(Context context) {
        BiometricManager bm = BiometricManager.from(context);
        int result = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public boolean isAppUnlocked() {
        return isAppUnlocked;
    }

    public void setAppUnlocked(boolean unlocked) {
        this.isAppUnlocked = unlocked;
    }

    public void lockApp() {
        this.isAppUnlocked = false;
    }
}

