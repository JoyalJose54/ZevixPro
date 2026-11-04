import 'dart:io';

class MusicItem {
  final int id;
  final String title;
  final String artist;
  final String album;
  final String path;
  final Duration duration;
  final int size;

  MusicItem({
    required this.id,
    required this.title,
    required this.artist,
    required this.album,
    required this.path,
    required this.duration,
    required this.size,
  });

  String get formattedSize {
    if (size <= 0) return "0 B";
    const suffixes = ["B", "KB", "MB", "GB"];
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
    final minutes = duration.inMinutes;
    final seconds = duration.inSeconds.remainder(60);
    return "${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
  }

  bool get exists => File(path).existsSync();

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'artist': artist,
    'album': album,
    'path': path,
    'durationMs': duration.inMilliseconds,
    'size': size,
  };

  factory MusicItem.fromJson(Map<String, dynamic> json) => MusicItem(
    id: json['id'] as int? ?? 0,
    title: json['title'] as String? ?? 'Unknown Track',
    artist: json['artist'] as String? ?? 'Unknown Artist',
    album: json['album'] as String? ?? 'Unknown Album',
    path: json['path'] as String? ?? '',
    duration: Duration(milliseconds: json['durationMs'] as int? ?? 0),
    size: json['size'] as int? ?? 0,
  );
}

