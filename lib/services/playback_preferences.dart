import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

class PlaybackPreferences {
  static const String _keyResumePrefix = "resume_pos_";
  static const String _keySortOrder = "sort_order";
  static const String _keyFolderSortOrder = "folder_sort_order";
  static const String _keyAspectRatio = "aspect_ratio";
  static const String _keyHwDec = "hw_acceleration";
  static const String _keyEqEnabled = "eq_enabled";
  static const String _keyEqPreset = "eq_preset";
  static const String _keyBassBoost = "bass_boost";
  static const String _keyVirtualizer = "virtualizer_strength";
  static const String _keyEqBands = "eq_custom_bands";
  static const String _keyFavorites = "favorite_paths";
  static const String _keyWatchedFolders = "watched_folders";
  static const String _keyResumePlayback = "resume_playback";
  static const String _keyAutoPlayNext = "auto_play_next";
  static const String _keyNightMode = "night_mode";
  static const String _keyPlayerVolume = "player_volume";

  final SharedPreferences _prefs;

  PlaybackPreferences(this._prefs);

  static Future<PlaybackPreferences> create() async {
    final sp = await SharedPreferences.getInstance();
    return PlaybackPreferences(sp);
  }

  // Resume positions
  int getSavedPosition(String path) {
    final key = _keyResumePrefix + path.hashCode.toString();
    return _prefs.getInt(key) ?? 0;
  }

  Future<void> savePosition(String path, int positionMs) async {
    final key = _keyResumePrefix + path.hashCode.toString();
    await _prefs.setInt(key, positionMs);
  }

  Future<void> clearPosition(String path) async {
    final key = _keyResumePrefix + path.hashCode.toString();
    await _prefs.remove(key);
  }

  // Sort orders (0: Date Desc, 1: Date Asc, 2: Name Asc, 3: Name Desc, 4: Size Desc, 5: Duration Desc)
  int get sortOrder => _prefs.getInt(_keySortOrder) ?? 0;
  Future<void> setSortOrder(int order) async => await _prefs.setInt(_keySortOrder, order);

  int get folderSortOrder => _prefs.getInt(_keyFolderSortOrder) ?? 2;
  Future<void> setFolderSortOrder(int order) async => await _prefs.setInt(_keyFolderSortOrder, order);

  // Aspect ratio (0: Fit, 1: Stretch, 2: 16:9, 3: 4:3, 4: Zoom/Crop, 5: Original)
  int get aspectRatio => _prefs.getInt(_keyAspectRatio) ?? 0;
  Future<void> setAspectRatio(int ratio) async => await _prefs.setInt(_keyAspectRatio, ratio);

  // Hardware acceleration: "d3d11va" (Direct3D 11 GPU), "auto-safe", "no"
  String get hwDecMode => _prefs.getString(_keyHwDec) ?? "d3d11va";
  Future<void> setHwDecMode(String mode) async => await _prefs.setString(_keyHwDec, mode);

  // Audio Equalizer
  bool get isEqEnabled => _prefs.getBool(_keyEqEnabled) ?? false;
  Future<void> setEqEnabled(bool enabled) async => await _prefs.setBool(_keyEqEnabled, enabled);

  int get eqPreset => _prefs.getInt(_keyEqPreset) ?? 0;
  Future<void> setEqPreset(int preset) async => await _prefs.setInt(_keyEqPreset, preset);

  int get bassBoost => _prefs.getInt(_keyBassBoost) ?? 0;
  Future<void> setBassBoost(int strength) async => await _prefs.setInt(_keyBassBoost, strength);

  int get virtualizer => _prefs.getInt(_keyVirtualizer) ?? 0;
  Future<void> setVirtualizer(int strength) async => await _prefs.setInt(_keyVirtualizer, strength);

  List<double> get eqBands {
    final raw = _prefs.getString(_keyEqBands);
    if (raw != null) {
      try {
        final list = (jsonDecode(raw) as List).map((e) => (e as num).toDouble()).toList();
        if (list.length == 10) return list;
      } catch (_) {}
    }
    return List.filled(10, 0.0); // 0 dB flat
  }

  Future<void> setEqBands(List<double> bands) async {
    await _prefs.setString(_keyEqBands, jsonEncode(bands));
  }

  // Favorites
  List<String> get favorites => _prefs.getStringList(_keyFavorites) ?? [];

  bool isFavorite(String path) => favorites.contains(path);

  Future<bool> toggleFavorite(String path) async {
    final current = List<String>.from(favorites);
    bool added;
    if (current.contains(path)) {
      current.remove(path);
      added = false;
    } else {
      current.add(path);
      added = true;
    }
    await _prefs.setStringList(_keyFavorites, current);
    return added;
  }

  // Watched Folders for scanner
  List<String> get watchedFolders => _prefs.getStringList(_keyWatchedFolders) ?? [];

  Future<void> addWatchedFolder(String path) async {
    final current = List<String>.from(watchedFolders);
    if (!current.contains(path)) {
      current.add(path);
      await _prefs.setStringList(_keyWatchedFolders, current);
    }
  }

  Future<void> removeWatchedFolder(String path) async {
    final current = List<String>.from(watchedFolders);
    current.remove(path);
    await _prefs.setStringList(_keyWatchedFolders, current);
  }

  // General Player Settings
  bool get resumePlayback => _prefs.getBool(_keyResumePlayback) ?? true;
  Future<void> setResumePlayback(bool resume) async => await _prefs.setBool(_keyResumePlayback, resume);

  bool get autoPlayNext => _prefs.getBool(_keyAutoPlayNext) ?? true;
  Future<void> setAutoPlayNext(bool next) async => await _prefs.setBool(_keyAutoPlayNext, next);

  bool get isNightMode => _prefs.getBool(_keyNightMode) ?? false;
  Future<void> setNightMode(bool night) async => await _prefs.setBool(_keyNightMode, night);

  double get playerVolume => _prefs.getDouble(_keyPlayerVolume) ?? 100.0;
  Future<void> setPlayerVolume(double volume) async => await _prefs.setDouble(_keyPlayerVolume, volume);
}

