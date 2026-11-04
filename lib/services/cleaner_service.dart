import '../models/video_item.dart';
import 'playback_preferences.dart';
import 'trash_service.dart';

class CleanerScanResult {
  final List<VideoItem> watchedVideos;
  final List<VideoItem> largeVideos;
  final List<List<VideoItem>> duplicateGroups;

  final int watchedTotalBytes;
  final int largeTotalBytes;
  final int duplicateTotalBytes;

  CleanerScanResult({
    required this.watchedVideos,
    required this.largeVideos,
    required this.duplicateGroups,
    required this.watchedTotalBytes,
    required this.largeTotalBytes,
    required this.duplicateTotalBytes,
  });

  int get reclaimableBytes => watchedTotalBytes + duplicateTotalBytes;

  String formatBytes(int bytes) {
    if (bytes <= 0) return "0 B";
    const suffixes = ["B", "KB", "MB", "GB", "TB"];
    var i = 0;
    double dSize = bytes.toDouble();
    while (dSize >= 1024 && i < suffixes.length - 1) {
      dSize /= 1024;
      i++;
    }
    return "${dSize.toStringAsFixed(1)} ${suffixes[i]}";
  }
}

class CleanerService {
  final PlaybackPreferences preferences;
  final TrashService trashService;

  CleanerService({
    required this.preferences,
    required this.trashService,
  });

  CleanerScanResult analyzeVideos(List<VideoItem> videos) {
    final watched = <VideoItem>[];
    final large = <VideoItem>[];
    final Map<int, List<VideoItem>> sizeMap = {};

    int watchedBytes = 0;
    int largeBytes = 0;

    for (final v in videos) {
      // 1. Watched check
      final pos = preferences.getSavedPosition(v.path);
      if (pos > 0) {
        watched.add(v);
        watchedBytes += v.size;
      }

      // 2. Large files (> 100 MB)
      if (v.size >= 100 * 1024 * 1024) {
        large.add(v);
        largeBytes += v.size;
      }

      // 3. Potential duplicates by exact size
      sizeMap.putIfAbsent(v.size, () => []).add(v);
    }

    // Filter duplicate groups
    final duplicateGroups = <List<VideoItem>>[];
    int duplicateBytes = 0;

    for (final entry in sizeMap.entries) {
      if (entry.value.length > 1) {
        duplicateGroups.add(entry.value);
        // Bytes of copies beyond the first one
        duplicateBytes += (entry.key * (entry.value.length - 1));
      }
    }

    // Sort large files descending by size
    large.sort((a, b) => b.size.compareTo(a.size));

    return CleanerScanResult(
      watchedVideos: watched,
      largeVideos: large,
      duplicateGroups: duplicateGroups,
      watchedTotalBytes: watchedBytes,
      largeTotalBytes: largeBytes,
      duplicateTotalBytes: duplicateBytes,
    );
  }

  Future<int> batchMoveToTrash(List<VideoItem> items) async {
    int count = 0;
    for (final item in items) {
      final ok = await trashService.moveToTrash(item.path, isVideo: true);
      if (ok) count++;
    }
    return count;
  }
}

