import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/playlist_item.dart';

class PlaylistProvider extends ChangeNotifier {
  static const String _keyPlaylists = "user_saved_playlists";
  final SharedPreferences _prefs;
  List<PlaylistItem> _playlists = [];

  PlaylistProvider(this._prefs) {
    _load();
  }

  static Future<PlaylistProvider> create() async {
    final sp = await SharedPreferences.getInstance();
    return PlaylistProvider(sp);
  }

  List<PlaylistItem> get playlists => _playlists;

  void _load() {
    final raw = _prefs.getStringList(_keyPlaylists) ?? [];
    _playlists = [];
    for (final s in raw) {
      try {
        _playlists.add(PlaylistItem.fromJson(jsonDecode(s)));
      } catch (_) {}
    }
    notifyListeners();
  }

  Future<void> _save() async {
    final raw = _playlists.map((p) => jsonEncode(p.toJson())).toList();
    await _prefs.setStringList(_keyPlaylists, raw);
    notifyListeners();
  }

  Future<PlaylistItem> createPlaylist(String name) async {
    final item = PlaylistItem(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      name: name.trim().isEmpty ? "New Playlist" : name.trim(),
      itemPaths: [],
      createdAt: DateTime.now(),
    );
    _playlists.add(item);
    await _save();
    return item;
  }

  Future<void> deletePlaylist(String id) async {
    _playlists.removeWhere((p) => p.id == id);
    await _save();
  }

  Future<void> addToPlaylist(String playlistId, String itemPath) async {
    final index = _playlists.indexWhere((p) => p.id == playlistId);
    if (index != -1) {
      if (!_playlists[index].itemPaths.contains(itemPath)) {
        _playlists[index].itemPaths.add(itemPath);
        await _save();
      }
    }
  }

  Future<void> removeFromPlaylist(String playlistId, String itemPath) async {
    final index = _playlists.indexWhere((p) => p.id == playlistId);
    if (index != -1) {
      _playlists[index].itemPaths.remove(itemPath);
      await _save();
    }
  }

  Future<void> renamePlaylist(String playlistId, String newName) async {
    final index = _playlists.indexWhere((p) => p.id == playlistId);
    if (index != -1) {
      _playlists[index] = PlaylistItem(
        id: _playlists[index].id,
        name: newName.trim(),
        itemPaths: _playlists[index].itemPaths,
        createdAt: _playlists[index].createdAt,
      );
      await _save();
    }
  }
}

