import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/music_provider.dart';
import '../../theme/app_theme.dart';
import 'music_play_screen.dart';

class MusicView extends StatelessWidget {
  const MusicView({super.key});

  @override
  Widget build(BuildContext context) {
    final music = context.watch<MusicProvider>();
    final tracks = music.allTracks;

    if (music.isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: AppTheme.accentColor),
      );
    }

    if (tracks.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.music_note_outlined, size: 64, color: AppTheme.textMuted),
            const SizedBox(height: 16),
            const Text(
              "No music tracks found",
              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              "Add audio tracks to your Music or Downloads folder.",
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
            ),
            const SizedBox(height: 20),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentColor, foregroundColor: Colors.black),
              icon: const Icon(Icons.refresh),
              label: const Text("Refresh Tracks"),
              onPressed: () => music.loadTracks(),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(vertical: 8),
      itemCount: tracks.length,
      itemBuilder: (context, i) {
        final track = tracks[i];
        final isCurrent = music.currentTrack?.path == track.path;

        return ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          leading: Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: isCurrent ? AppTheme.accentColor.withOpacity(0.2) : AppTheme.bgCard,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                color: isCurrent ? AppTheme.accentColor : AppTheme.dividerLine,
              ),
            ),
            child: Icon(
              isCurrent && music.isPlaying ? Icons.equalizer : Icons.music_note,
              color: isCurrent ? AppTheme.accentColor : Colors.white70,
              size: 22,
            ),
          ),
          title: Text(
            track.title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              color: isCurrent ? AppTheme.accentColor : Colors.white,
              fontWeight: isCurrent ? FontWeight.bold : FontWeight.w500,
              fontSize: 14,
            ),
          ),
          subtitle: Text(
            "${track.artist} • ${track.formattedSize}",
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(color: AppTheme.textMuted, fontSize: 12),
          ),
          trailing: IconButton(
            icon: Icon(
              isCurrent && music.isPlaying ? Icons.pause_circle_filled : Icons.play_circle_filled,
              color: AppTheme.accentColor,
              size: 28,
            ),
            onPressed: () {
              if (isCurrent) {
                music.togglePlayPause();
              } else {
                music.playTrack(track, fromList: tracks);
              }
            },
          ),
          onTap: () {
            music.playTrack(track, fromList: tracks);
            Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const MusicPlayScreen()),
            );
          },
        );
      },
    );
  }
}

