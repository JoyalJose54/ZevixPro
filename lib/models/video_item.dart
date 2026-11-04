import 'dart:io';
import 'package:path/path.dart' as p;

class VideoItem {
  final int id;
  final String title;
  final String displayName;
  final String path;
  final Duration duration;
  final int size;
  final DateTime dateModified;
  final int width;
  final int height;
  final String folderName;
  final String folderPath;

  VideoItem({
    required this.id,
    required this.title,
    required this.displayName,
    required this.path,
    required this.duration,
    required this.size,
    required this.dateModified,
    this.width = 0,
    this.height = 0,
    required this.folderName,
    required this.folderPath,
  });

  String get resolutionBadge {
    if (width >= 3840 || height >= 2160) return "4K";
    if (width >= 1920 || height >= 1080) return "FHD";
    if (width >= 1280 || height >= 720) return "HD";
    return "";
  }

  String get formattedSize {
    if (size <= 0) return "0 B";
    const suffixes = ["B", "KB", "MB", "GB", "TB"];
    var i = 0;
    double dSize = size.toDouble();
    while (dSize >= 1024 && i < suffixes.length - 1) {
      dSize /= 1024;
      i++;
    }
    return "${dSize.toStringAsFixed(1)} ${suffixes[i]}";
  }

  String get formattedDuration {
    if (duration.inSeconds <= 0) return "--:--";
    final hours = duration.inHours;
    final minutes = duration.inMinutes.remainder(60);
    final seconds = duration.inSeconds.remainder(60);
    if (hours > 0) {
      return "${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
    }
    return "${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
  }

  bool get exists => File(path).existsSync();

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'displayName': displayName,
    'path': path,
    'durationMs': duration.inMilliseconds,
    'size': size,
    'dateModified': dateModified.toIso8601String(),
    'width': width,
    'height': height,
    'folderName': folderName,
    'folderPath': folderPath,
  };

  factory VideoItem.fromJson(Map<String, dynamic> json) => VideoItem(
    id: json['id'] as int? ?? 0,
    title: json['title'] as String? ?? '',
    displayName: json['displayName'] as String? ?? '',
    path: json['path'] as String? ?? '',
    duration: Duration(milliseconds: json['durationMs'] as int? ?? 0),
    size: json['size'] as int? ?? 0,
    dateModified: DateTime.tryParse(json['dateModified'] as String? ?? '') ?? DateTime.now(),
    width: json['width'] as int? ?? 0,
    height: json['height'] as int? ?? 0,
    folderName: json['folderName'] as String? ?? '',
    folderPath: json['folderPath'] as String? ?? '',
  );

  factory VideoItem.fromPath(String filePath) {
    final file = File(filePath);
    final stat = file.existsSync() ? file.statSync() : null;
    final parentDir = p.dirname(filePath);
    final parentName = p.basename(parentDir);
    final fileName = p.basename(filePath);
    return VideoItem(
      id: filePath.hashCode,
      title: p.basenameWithoutExtension(fileName),
      displayName: fileName,
      path: filePath,
      duration: Duration.zero,
      size: stat?.size ?? 0,
      dateModified: stat?.modified ?? DateTime.now(),
      width: 0,
      height: 0,
      folderName: parentName.isEmpty ? "Videos" : parentName,
      folderPath: parentDir,
    );
  }

  static List<VideoItem> createPlaylistFromPath(String filePath) {
    final target = VideoItem.fromPath(filePath);
    try {
      final parentDir = Directory(p.dirname(filePath));
      if (parentDir.existsSync()) {
        const videoExtensions = {'.mp4', '.mkv', '.avi', '.mov', '.webm', '.flv', '.wmv', '.m4v', '.ts'};
        final files = parentDir
            .listSync()
            .whereType<File>()
            .where((f) => videoExtensions.contains(p.extension(f.path).toLowerCase()))
            .toList();

        files.sort((a, b) => p.basename(a.path).toLowerCase().compareTo(p.basename(b.path).toLowerCase()));

        final list = files.map((f) => VideoItem.fromPath(f.path)).toList();
        if (list.isNotEmpty) {
          return list;
        }
      }
    } catch (_) {}
    return [target];
  }
}

