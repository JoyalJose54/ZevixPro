import 'package:flutter/foundation.dart';
import '../models/video_item.dart';
import '../models/folder_item.dart';
import '../services/playback_preferences.dart';
import '../services/media_scanner_service.dart';
import '../services/trash_service.dart';

class MediaProvider extends ChangeNotifier {
  final PlaybackPreferences preferences;
  final TrashService trashService;
  late final MediaScannerService scanner;

  List<VideoItem> _allVideos = [];
  List<FolderItem> _allFolders = [];
  bool _isLoading = false;
  String _searchQuery = "";
  int _sortOrder = 0; // 0: Date Desc, 1: Date Asc, 2: Name Asc, 3: Name Desc, 4: Size Desc

  MediaProvider({
    required this.preferences,
    required this.trashService,
  }) {
    scanner = MediaScannerService(preferences);
    _sortOrder = preferences.sortOrder;
    loadMedia();
  }

  List<VideoItem> get allVideos => _allVideos;
  List<FolderItem> get allFolders => _allFolders;
  bool get isLoading => _isLoading;
  String get searchQuery => _searchQuery;
  int get sortOrder => _sortOrder;

  List<VideoItem> get recentVideos {
    // Return videos that have a saved position, or top 8 most recent
    final withPos = _allVideos.where((v) => preferences.getSavedPosition(v.path) > 0).toList();
    if (withPos.isNotEmpty) {
      return withPos.take(10).toList();
    }
    return _allVideos.take(8).toList();
  }

  List<VideoItem> get filteredVideos {
    var list = List<VideoItem>.from(_allVideos);
    if (_searchQuery.trim().isNotEmpty) {
      final q = _searchQuery.trim().toLowerCase();
      list = list.where((v) => v.title.toLowerCase().contains(q) || v.displayName.toLowerCase().contains(q)).toList();
    }

    switch (_sortOrder) {
      case 0: // Date Descending (Newest first)
        list.sort((a, b) => b.dateModified.compareTo(a.dateModified));
        break;
      case 1: // Date Ascending (Oldest first)
        list.sort((a, b) => a.dateModified.compareTo(b.dateModified));
        break;
      case 2: // Name Ascending (A-Z)
        list.sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));
        break;
      case 3: // Name Descending (Z-A)
        list.sort((a, b) => b.title.toLowerCase().compareTo(a.title.toLowerCase()));
        break;
      case 4: // Size Descending
        list.sort((a, b) => b.size.compareTo(a.size));
        break;
      default:
        list.sort((a, b) => b.dateModified.compareTo(a.dateModified));
    }
    return list;
  }

  List<VideoItem> getVideosInFolder(String folderPath) {
    return _allVideos.where((v) => v.folderPath.toLowerCase() == folderPath.toLowerCase()).toList();
  }

  Future<void> loadMedia() async {
    _isLoading = true;
    notifyListeners();

    try {
      _allVideos = await scanner.scanVideos();
      _allFolders = scanner.groupVideosByFolder(_allVideos);
    } catch (_) {}

    _isLoading = false;
    notifyListeners();
  }

  void setSearchQuery(String q) {
    _searchQuery = q;
    notifyListeners();
  }

  Future<void> setSortOrder(int order) async {
    _sortOrder = order;
    await preferences.setSortOrder(order);
    notifyListeners();
  }

  Future<bool> deleteVideo(VideoItem video) async {
    final success = await trashService.moveToTrash(video.path, isVideo: true);
    if (success) {
      _allVideos.removeWhere((v) => v.path == video.path);
      _allFolders = scanner.groupVideosByFolder(_allVideos);
      notifyListeners();
    }
    return success;
  }

  Future<void> addWatchedFolder(String folderPath) async {
    await preferences.addWatchedFolder(folderPath);
    await loadMedia();
  }

  Future<void> removeWatchedFolder(String folderPath) async {
    await preferences.removeWatchedFolder(folderPath);
    await loadMedia();
  }
}

