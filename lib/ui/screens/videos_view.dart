import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/video_item.dart';
import '../../providers/media_provider.dart';
import '../../providers/playlist_provider.dart';
import '../../theme/app_theme.dart';
import 'player/desktop_player_screen.dart';
import '../widgets/video_card.dart';

class VideosView extends StatefulWidget {
  const VideosView({super.key});

  @override
  State<VideosView> createState() => _VideosViewState();
}

class _VideosViewState extends State<VideosView> {
  bool _isSelectionMode = false;
  final Set<String> _selectedPaths = {};

  void _openPlayer(VideoItem video, List<VideoItem> playlist) {
    final media = context.read<MediaProvider>();
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => DesktopPlayerScreen(
          video: video,
          playlist: playlist,
          preferences: media.preferences,
        ),
      ),
    );
  }

  void _showSortMenu(BuildContext context) {
    final media = context.read<MediaProvider>();
    showMenu<int>(
      context: context,
      position: const RelativeRect.fromLTRB(1000, 80, 20, 0),
      color: AppTheme.bgPopup,
      items: [
        _buildSortItem(0, "Date (Newest first)", media.sortOrder),
        _buildSortItem(1, "Date (Oldest first)", media.sortOrder),
        _buildSortItem(2, "Name (A-Z)", media.sortOrder),
        _buildSortItem(3, "Name (Z-A)", media.sortOrder),
        _buildSortItem(4, "Size (Largest first)", media.sortOrder),
      ],
    ).then((val) {
      if (val != null) media.setSortOrder(val);
    });
  }

  PopupMenuItem<int> _buildSortItem(int value, String text, int currentSort) {
    return PopupMenuItem<int>(
      value: value,
      child: Row(
        children: [
          Text(text, style: const TextStyle(color: Colors.white, fontSize: 13)),
          const Spacer(),
          if (value == currentSort) const Icon(Icons.check, color: AppTheme.accentColor, size: 16),
        ],
      ),
    );
  }

  void _showAddToPlaylistDialog(VideoItem video) {
    final playlistProv = context.read<PlaylistProvider>();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.bgDialog,
        title: const Text("Add to Playlist", style: TextStyle(color: Colors.white)),
        content: SizedBox(
          width: 320,
          child: playlistProv.playlists.isEmpty
              ? const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text("No playlists yet. Create one in the Playlists tab!", style: TextStyle(color: Colors.white70)),
                )
              : ListView.builder(
                  shrinkWrap: true,
                  itemCount: playlistProv.playlists.length,
                  itemBuilder: (_, i) {
                    final pl = playlistProv.playlists[i];
                    return ListTile(
                      leading: const Icon(Icons.playlist_play, color: AppTheme.accentColor),
                      title: Text(pl.name, style: const TextStyle(color: Colors.white)),
                      subtitle: Text("${pl.itemPaths.length} items", style: const TextStyle(color: Colors.white54, fontSize: 11)),
                      onTap: () {
                        playlistProv.addToPlaylist(pl.id, video.path);
                        Navigator.of(ctx).pop();
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text("Added to ${pl.name}"), backgroundColor: AppTheme.bgCard),
                        );
                      },
                    );
                  },
                ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text("Cancel", style: TextStyle(color: Colors.white70)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final media = context.watch<MediaProvider>();
    final videos = media.filteredVideos;
    final recents = media.recentVideos;

    if (media.isLoading) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(color: AppTheme.accentColor),
            SizedBox(height: 16),
            Text("Scanning media on PC...", style: TextStyle(color: AppTheme.textSecondary)),
          ],
        ),
      );
    }

    if (videos.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.video_library_outlined, size: 64, color: AppTheme.textMuted),
            const SizedBox(height: 16),
            const Text(
              "No videos found in your standard folders",
              style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              "Click 'Add Folder' to scan your movie drives or folders.",
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
            ),
            const SizedBox(height: 20),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.accentColor,
                foregroundColor: Colors.black,
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              ),
              icon: const Icon(Icons.refresh),
              label: const Text("Refresh Media", style: TextStyle(fontWeight: FontWeight.bold)),
              onPressed: () => media.loadMedia(),
            ),
          ],
        ),
      );
    }

    return CustomScrollView(
      slivers: [
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Row(
              children: [
                if (_isSelectionMode) ...[
                  Text(
                    "${_selectedPaths.length} selected",
                    style: const TextStyle(color: AppTheme.accentColor, fontSize: 15, fontWeight: FontWeight.bold),
                  ),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                    tooltip: "Move selected to Trash",
                    onPressed: () async {
                      for (final path in _selectedPaths) {
                        final v = videos.firstWhere((x) => x.path == path);
                        await media.deleteVideo(v);
                      }
                      setState(() {
                        _selectedPaths.clear();
                        _isSelectionMode = false;
                      });
                    },
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white),
                    tooltip: "Cancel",
                    onPressed: () => setState(() {
                      _selectedPaths.clear();
                      _isSelectionMode = false;
                    }),
                  ),
                ] else ...[
                  Text(
                    "${videos.length} Videos",
                    style: const TextStyle(
                      color: AppTheme.textPrimary,
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const Spacer(),
                  TextButton.icon(
                    style: TextButton.styleFrom(foregroundColor: AppTheme.textSecondary),
                    icon: const Icon(Icons.sort, size: 18),
                    label: const Text("Sort"),
                    onPressed: () => _showSortMenu(context),
                  ),
                  IconButton(
                    icon: const Icon(Icons.checklist, color: AppTheme.textSecondary, size: 20),
                    tooltip: "Select Multiple",
                    onPressed: () => setState(() => _isSelectionMode = true),
                  ),
                  IconButton(
                    icon: const Icon(Icons.refresh, color: AppTheme.textSecondary, size: 20),
                    tooltip: "Deep Refresh",
                    onPressed: () => media.loadMedia(),
                  ),
                ],
              ],
            ),
          ),
        ),

        if (recents.isNotEmpty && !_isSelectionMode && media.searchQuery.isEmpty) ...[
          const SliverToBoxAdapter(
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Text(
                "Recent Videos",
                style: TextStyle(
                  color: AppTheme.textSecondary,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5,
                ),
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: SizedBox(
              height: 130,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                itemCount: recents.length,
                itemBuilder: (context, i) {
                  final v = recents[i];
                  return InkWell(
                    onTap: () => _openPlayer(v, videos),
                    borderRadius: BorderRadius.circular(10),
                    child: Container(
                      width: 140,
                      margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: AppTheme.bgCard,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: AppTheme.dividerLine),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            height: 60,
                            decoration: BoxDecoration(
                              color: const Color(0xFF14171F),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: const Center(
                              child: Icon(Icons.play_circle_fill, color: AppTheme.accentColor, size: 28),
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            v.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: Colors.white),
                          ),
                          Text(
                            v.formattedSize,
                            style: const TextStyle(fontSize: 10, color: AppTheme.textMuted),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 8)),
        ],

        SliverList(
          delegate: SliverChildBuilderDelegate(
            (context, index) {
              final video = videos[index];
              final isSelected = _selectedPaths.contains(video.path);

              return VideoCard(
                video: video,
                isSelected: isSelected,
                isSelectionMode: _isSelectionMode,
                onSelectChanged: (checked) {
                  setState(() {
                    if (checked == true) {
                      _selectedPaths.add(video.path);
                    } else {
                      _selectedPaths.remove(video.path);
                    }
                  });
                },
                onTap: () => _openPlayer(video, videos),
                onAddToPlaylist: () => _showAddToPlaylistDialog(video),
                onDelete: () async {
                  final ok = await media.deleteVideo(video);
                  if (ok && mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text("Moved '${video.title}' to Trash"),
                        backgroundColor: AppTheme.bgCard,
                      ),
                    );
                  }
                },
              );
            },
            childCount: videos.length,
          ),
        ),
        const SliverToBoxAdapter(child: SizedBox(height: 80)),
      ],
    );
  }
}
