import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/music_provider.dart';
import '../../theme/app_theme.dart';
import '../screens/music_play_screen.dart';

class MusicMiniPlayer extends StatelessWidget {
  const MusicMiniPlayer({super.key});

  @override
  Widget build(BuildContext context) {
    final musicProvider = context.watch<MusicProvider>();
    final current = musicProvider.currentTrack;

    if (current == null) return const SizedBox.shrink();

    return InkWell(
      onTap: () {
        Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => const MusicPlayScreen()),
        );
      },
      child: Container(
        height: 64,
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        padding: const EdgeInsets.symmetric(horizontal: 14),
        decoration: BoxDecoration(
          color: const Color(0xFF14171F),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppTheme.accentColor.withOpacity(0.3), width: 1),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.5),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          children: [
            // Track Art Box
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: AppTheme.accentColor.withOpacity(0.15),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(Icons.music_note, color: AppTheme.accentColor, size: 24),
            ),
            const SizedBox(width: 12),
            // Title & Artist
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    current.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: AppTheme.textPrimary,
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    current.artist,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: AppTheme.textSecondary,
                      fontSize: 11,
                    ),
                  ),
                ],
              ),
            ),
            // Play / Pause Button
            IconButton(
              icon: Icon(
                musicProvider.isPlaying ? Icons.pause_circle_filled : Icons.play_circle_filled,
                color: AppTheme.accentColor,
                size: 34,
              ),
              onPressed: () => musicProvider.togglePlayPause(),
            ),
            // Close Button
            IconButton(
              icon: const Icon(Icons.close, color: AppTheme.textMuted, size: 20),
              onPressed: () => musicProvider.stop(),
            ),
          ],
        ),
      ),
    );
  }
}

