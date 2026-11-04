import 'dart:io';

class TrashItem {
  final String id;
  final String title;
  final String originalPath;
  final String trashPath;
  final int size;
  final DateTime deletedAt;
  final bool isVideo;

  TrashItem({
    required this.id,
    required this.title,
    required this.originalPath,
    required this.trashPath,
    required this.size,
    required this.deletedAt,
    required this.isVideo,
  });

  int get daysRemaining {
    final expiry = deletedAt.add(const Duration(days: 30));
    final diff = expiry.difference(DateTime.now()).inDays;
    return diff < 0 ? 0 : diff;
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

  bool get exists => File(trashPath).existsSync();

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'originalPath': originalPath,
    'trashPath': trashPath,
    'size': size,
    'deletedAt': deletedAt.toIso8601String(),
    'isVideo': isVideo,
  };

  factory TrashItem.fromJson(Map<String, dynamic> json) => TrashItem(
    id: json['id'] as String? ?? '',
    title: json['title'] as String? ?? '',
    originalPath: json['originalPath'] as String? ?? '',
    trashPath: json['trashPath'] as String? ?? '',
    size: json['size'] as int? ?? 0,
    deletedAt: DateTime.tryParse(json['deletedAt'] as String? ?? '') ?? DateTime.now(),
    isVideo: json['isVideo'] as bool? ?? true,
  );
}

