import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/playlist_item.dart';
import '../../models/video_item.dart';
import '../../providers/media_provider.dart';
import '../../providers/playlist_provider.dart';
import '../../theme/app_theme.dart';
import 'player/desktop_player_screen.dart';
import '../widgets/video_card.dart';

class PlaylistScreen extends StatefulWidget {
  const PlaylistScreen({super.key});

  @override
  State<PlaylistScreen> createState() => _PlaylistScreenState();
}

class _PlaylistScreenState extends State<PlaylistScreen> {
  PlaylistItem? _selectedPlaylist;

  void _showCreateDialog() {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.bgDialog,
        title: const Text("New Playlist", style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: controller,
          autofocus: true,
          style: const TextStyle(color: Colors.white),
          decoration: const InputDecoration(
            hintText: "Playlist Name",
            hintStyle: TextStyle(color: Colors.white38),
            enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white24)),
            focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: AppTheme.accentColor)),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text("Cancel", style: TextStyle(color: Colors.white70)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentColor, foregroundColor: Colors.black),
            onPressed: () {
              final name = controller.text.trim();
              if (name.isNotEmpty) {
                context.read<PlaylistProvider>().createPlaylist(name);
                Navigator.of(ctx).pop();
              }
            },
            child: const Text("Create"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final playlistProv = context.watch<PlaylistProvider>();
    final mediaProv = context.watch<MediaProvider>();
    final playlists = playlistProv.playlists;

    if (_selectedPlaylist != null) {
      // Find latest reference from provider
      final pl = playlists.firstWhere(
        (p) => p.id == _selectedPlaylist!.id,
        orElse: () => _selectedPlaylist!,
      );

      final playlistVideos = <VideoItem>[];
      for (final path in pl.itemPaths) {
        final match = mediaProv.allVideos.where((v) => v.path == path).firstOrNull;
        if (match != null) playlistVideos.add(match);
      }

      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.arrow_back, color: Colors.white),
                  onPressed: () => setState(() => _selectedPlaylist = null),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        pl.name,
                        style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      Text(
                        "${playlistVideos.length} videos",
                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                if (playlistVideos.isNotEmpty)
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentColor, foregroundColor: Colors.black),
                    icon: const Icon(Icons.play_arrow),
                    label: const Text("Play All"),
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => DesktopPlayerScreen(
                            video: playlistVideos.first,
                            playlist: playlistVideos,
                            preferences: mediaProv.preferences,
                          ),
                        ),
                      );
                    },
                  ),
                IconButton(
                  icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                  tooltip: "Delete Playlist",
                  onPressed: () {
                    playlistProv.deletePlaylist(pl.id);
                    setState(() => _selectedPlaylist = null);
                  },
                ),
              ],
            ),
          ),
          const Divider(),
          Expanded(
            child: playlistVideos.isEmpty
                ? const Center(
                    child: Text(
                      "This playlist is empty. Add videos from the Videos tab!",
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  )
                : ListView.builder(
                    itemCount: playlistVideos.length,
                    itemBuilder: (context, i) {
                      final v = playlistVideos[i];
                      return VideoCard(
                        video: v,
                        onTap: () {
                          Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (_) => DesktopPlayerScreen(
                                video: v,
                                playlist: playlistVideos,
                                preferences: mediaProv.preferences,
                              ),
                            ),
                          );
                        },
                        onDelete: () => playlistProv.removeFromPlaylist(pl.id, v.path),
                      );
                    },
                  ),
          ),
        ],
      );
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppTheme.accentColor,
        foregroundColor: Colors.black,
        icon: const Icon(Icons.add),
        label: const Text("New Playlist", style: TextStyle(fontWeight: FontWeight.bold)),
        onPressed: _showCreateDialog,
      ),
      body: playlists.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.playlist_play_rounded, size: 64, color: AppTheme.textMuted),
                  const SizedBox(height: 16),
                  const Text("No playlists yet", style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  const Text("Create playlists to organize your favorite movies & clips.", style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                  const SizedBox(height: 20),
                  ElevatedButton(
                    style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentColor, foregroundColor: Colors.black),
                    onPressed: _showCreateDialog,
                    child: const Text("Create Playlist"),
                  ),
                ],
              ),
            )
          : GridView.builder(
              padding: const EdgeInsets.all(16),
              gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                maxCrossAxisExtent: 320,
                mainAxisExtent: 88,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
              ),
              itemCount: playlists.length,
              itemBuilder: (context, i) {
                final pl = playlists[i];
                return InkWell(
                  onTap: () => setState(() => _selectedPlaylist = pl),
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: AppTheme.bgCard,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppTheme.dividerLine),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(
                            color: AppTheme.accentColor.withOpacity(0.12),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Icon(Icons.playlist_play, color: AppTheme.accentColor, size: 28),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text(
                                pl.name,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                "${pl.itemPaths.length} items",
                                style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                              ),
                            ],
                          ),
                        ),
                        const Icon(Icons.chevron_right, color: AppTheme.textMuted, size: 20),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}
