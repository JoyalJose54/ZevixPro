class FolderItem {
  final String name;
  final String path;
  final int videoCount;
  final int totalSize;

  FolderItem({
    required this.name,
    required this.path,
    required this.videoCount,
    required this.totalSize,
  });

  String get formattedSize {
    if (totalSize <= 0) return "0 B";
    const suffixes = ["B", "KB", "MB", "GB", "TB"];
    var i = 0;
    double dSize = totalSize.toDouble();
    while (dSize >= 1024 && i < suffixes.length - 1) {
      dSize /= 1024;
      i++;
    }
    return "${dSize.toStringAsFixed(1)} ${suffixes[i]}";
  }
}

