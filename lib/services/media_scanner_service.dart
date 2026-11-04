import 'dart:io';
import 'package:path/path.dart' as p;
import '../models/video_item.dart';
import '../models/music_item.dart';
import '../models/folder_item.dart';
import 'playback_preferences.dart';

class MediaScannerService {
  static const Set<String> videoExtensions = {
    '.mp4', '.mkv', '.avi', '.mov', '.flv', '.wmv',
    '.webm', '.ts', '.m4v', '.3gp', '.rmvb', '.vob',
    '.mpg', '.mpeg', '.m2ts', '.ogv'
  };

  static const Set<String> musicExtensions = {
    '.mp3', '.flac', '.wav', '.aac', '.m4a',
    '.ogg', '.wma', '.opus', '.alac', '.aiff'
  };

  final PlaybackPreferences preferences;

  MediaScannerService(this.preferences);

  Future<List<Directory>> getDefaultSearchDirectories() async {
    final dirs = <Directory>[];

    // Standard User folders on Windows
    final userProfile = Platform.environment['USERPROFILE'];
    if (userProfile != null) {
      final videos = Directory(p.join(userProfile, 'Videos'));
      if (videos.existsSync()) dirs.add(videos);

      final downloads = Directory(p.join(userProfile, 'Downloads'));
      if (downloads.existsSync()) dirs.add(downloads);

      final music = Directory(p.join(userProfile, 'Music'));
      if (music.existsSync()) dirs.add(music);

      final desktop = Directory(p.join(userProfile, 'Desktop'));
      if (desktop.existsSync()) dirs.add(desktop);
    }

    // Custom watched folders from preferences
    for (final customPath in preferences.watchedFolders) {
      final dir = Directory(customPath);
      if (dir.existsSync() && !dirs.any((d) => d.path.toLowerCase() == dir.path.toLowerCase())) {
        dirs.add(dir);
      }
    }

    return dirs;
  }

  Future<List<VideoItem>> scanVideos() async {
    final searchDirs = await getDefaultSearchDirectories();
    final List<VideoItem> videos = [];
    final Set<String> visitedPaths = {};

    for (final dir in searchDirs) {
      await _scanDirectoryForVideos(dir, videos, visitedPaths, maxDepth: 4);
    }

    return videos;
  }

  Future<void> _scanDirectoryForVideos(
    Directory dir,
    List<VideoItem> result,
    Set<String> visited, {
    int maxDepth = 4,
    int currentDepth = 0,
  }) async {
    if (currentDepth > maxDepth) return;

    try {
      final entities = dir.listSync(followLinks: false);
      for (final entity in entities) {
        if (entity is File) {
          final ext = p.extension(entity.path).toLowerCase();
          if (videoExtensions.contains(ext) && !visited.contains(entity.path.toLowerCase())) {
            visited.add(entity.path.toLowerCase());
            try {
              final stat = entity.statSync();
              final fileName = p.basename(entity.path);
              final parentDir = p.dirname(entity.path);
              final parentName = p.basename(parentDir);

              // Estimate basic resolution hint from filename if present (e.g. 4K, 2160p, 1080p, 720p)
              int width = 0;
              int height = 0;
              final upper = fileName.toUpperCase();
              if (upper.contains("4K") || upper.contains("2160P") || upper.contains("UHD")) {
                width = 3840; height = 2160;
              } else if (upper.contains("1080P") || upper.contains("FHD")) {
                width = 1920; height = 1080;
              } else if (upper.contains("720P") || upper.contains("HD")) {
                width = 1280; height = 720;
              }

              result.add(VideoItem(
                id: entity.path.hashCode,
                title: p.basenameWithoutExtension(fileName),
                displayName: fileName,
                path: entity.path,
                duration: const Duration(seconds: 0), // populated on load or playback
                size: stat.size,
                dateModified: stat.modified,
                width: width,
                height: height,
                folderName: parentName.isEmpty ? "Videos" : parentName,
                folderPath: parentDir,
              ));
            } catch (_) {}
          }
        } else if (entity is Directory) {
          final dirName = p.basename(entity.path);
          // Skip hidden and system directories
          if (!dirName.startsWith('.') &&
              !dirName.startsWith('\$') &&
              dirName != 'AppData' &&
              dirName != 'Windows' &&
              dirName != 'node_modules' &&
              dirName != '.gradle' &&
              dirName != 'build') {
            await _scanDirectoryForVideos(entity, result, visited, maxDepth: maxDepth, currentDepth: currentDepth + 1);
          }
        }
      }
    } catch (_) {}
  }

  Future<List<MusicItem>> scanMusic() async {
    final searchDirs = await getDefaultSearchDirectories();
    final List<MusicItem> music = [];
    final Set<String> visitedPaths = {};

    for (final dir in searchDirs) {
      await _scanDirectoryForMusic(dir, music, visitedPaths, maxDepth: 4);
    }

    return music;
  }

  Future<void> _scanDirectoryForMusic(
    Directory dir,
    List<MusicItem> result,
    Set<String> visited, {
    int maxDepth = 4,
    int currentDepth = 0,
  }) async {
    if (currentDepth > maxDepth) return;

    try {
      final entities = dir.listSync(followLinks: false);
      for (final entity in entities) {
        if (entity is File) {
          final ext = p.extension(entity.path).toLowerCase();
          if (musicExtensions.contains(ext) && !visited.contains(entity.path.toLowerCase())) {
            visited.add(entity.path.toLowerCase());
            try {
              final stat = entity.statSync();
              final fileName = p.basename(entity.path);
              final parentName = p.basename(p.dirname(entity.path));

              result.add(MusicItem(
                id: entity.path.hashCode,
                title: p.basenameWithoutExtension(fileName),
                artist: parentName.isEmpty ? "Unknown Artist" : parentName,
                album: "Local Music",
                path: entity.path,
                duration: const Duration(seconds: 0),
                size: stat.size,
              ));
            } catch (_) {}
          }
        } else if (entity is Directory) {
          final dirName = p.basename(entity.path);
          if (!dirName.startsWith('.') &&
              !dirName.startsWith('\$') &&
              dirName != 'AppData' &&
              dirName != 'Windows') {
            await _scanDirectoryForMusic(entity, result, visited, maxDepth: maxDepth, currentDepth: currentDepth + 1);
          }
        }
      }
    } catch (_) {}
  }

  List<FolderItem> groupVideosByFolder(List<VideoItem> videos) {
    final Map<String, List<VideoItem>> map = {};
    for (final v in videos) {
      map.putIfAbsent(v.folderPath, () => []).add(v);
    }

    final folders = <FolderItem>[];
    for (final entry in map.entries) {
      final folderVideos = entry.value;
      final totalSize = folderVideos.fold<int>(0, (sum, item) => sum + item.size);
      folders.add(FolderItem(
        name: folderVideos.first.folderName,
        path: entry.key,
        videoCount: folderVideos.length,
        totalSize: totalSize,
      ));
    }

    // Sort folders by name
    folders.sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
    return folders;
  }
}

