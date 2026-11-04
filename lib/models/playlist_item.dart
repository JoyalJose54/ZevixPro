class PlaylistItem {
  final String id;
  final String name;
  final List<String> itemPaths;
  final DateTime createdAt;

  PlaylistItem({
    required this.id,
    required this.name,
    required this.itemPaths,
    required this.createdAt,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'itemPaths': itemPaths,
    'createdAt': createdAt.toIso8601String(),
  };

  factory PlaylistItem.fromJson(Map<String, dynamic> json) => PlaylistItem(
    id: json['id'] as String? ?? '',
    name: json['name'] as String? ?? 'Untitled Playlist',
    itemPaths: List<String>.from(json['itemPaths'] as List? ?? []),
    createdAt: DateTime.tryParse(json['createdAt'] as String? ?? '') ?? DateTime.now(),
  );
}

