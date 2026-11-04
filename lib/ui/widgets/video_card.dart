import 'dart:io';
import 'package:flutter/material.dart';
import '../../models/video_item.dart';
import '../../theme/app_theme.dart';

class VideoCard extends StatelessWidget {
  final VideoItem video;
  final VoidCallback onTap;
  final VoidCallback? onAddToPlaylist;
  final VoidCallback? onDelete;
  final bool isSelected;
  final bool isSelectionMode;
  final ValueChanged<bool?>? onSelectChanged;

  const VideoCard({
    super.key,
    required this.video,
    required this.onTap,
    this.onAddToPlaylist,
    this.onDelete,
    this.isSelected = false,
    this.isSelectionMode = false,
    this.onSelectChanged,
  });

  @override
  Widget build(BuildContext context) {
    final badge = video.resolutionBadge;

    return InkWell(
      onTap: isSelectionMode ? () => onSelectChanged?.call(!isSelected) : onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: isSelected ? AppTheme.bgCardSelected : AppTheme.bgCard,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected ? AppTheme.accentColor : AppTheme.dividerLine,
            width: isSelected ? 1.5 : 1,
          ),
        ),
        child: Row(
          children: [
            if (isSelectionMode)
              Checkbox(
                value: isSelected,
                activeColor: AppTheme.accentColor,
                onChanged: onSelectChanged,
              ),
            // Video Thumbnail Box
            Stack(
              children: [
                Container(
                  width: 120,
                  height: 72,
                  decoration: BoxDecoration(
                    color: const Color(0xFF14171F),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.white12, width: 0.5),
                  ),
                  child: Center(
                    child: Icon(
                      Icons.play_arrow_rounded,
                      color: AppTheme.accentColor.withOpacity(0.9),
                      size: 38,
                    ),
                  ),
                ),
                // Resolution Badge (4K / FHD / HD)
                if (badge.isNotEmpty)
                  Positioned(
                    top: 6,
                    left: 6,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                      decoration: BoxDecoration(
                        color: badge == "4K" ? AppTheme.yellowBoost : AppTheme.accentColor,
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        badge,
                        style: const TextStyle(
                          color: Colors.black,
                          fontSize: 9,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                  ),
                // Duration Pill
                Positioned(
                  bottom: 6,
                  right: 6,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.85),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      video.formattedDuration,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 10,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(width: 14),
            // Title & Info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    video.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: AppTheme.textPrimary,
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      height: 1.25,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1.5),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.06),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          video.folderName,
                          style: const TextStyle(
                            color: AppTheme.textSecondary,
                            fontSize: 11,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        video.formattedSize,
                        style: const TextStyle(
                          color: AppTheme.textMuted,
                          fontSize: 11,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            // Popup Menu
            PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert, color: AppTheme.textSecondary, size: 20),
              color: AppTheme.bgPopup,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
                side: const BorderSide(color: AppTheme.dividerLine),
              ),
              onSelected: (action) {
                switch (action) {
                  case 'play':
                    onTap();
                    break;
                  case 'playlist':
                    onAddToPlaylist?.call();
                    break;
                  case 'explorer':
                    _openInExplorer(video.path);
                    break;
                  case 'delete':
                    onDelete?.call();
                    break;
                }
              },
              itemBuilder: (context) => [
                const PopupMenuItem(
                  value: 'play',
                  child: Row(
                    children: [
                      Icon(Icons.play_circle_outline, color: AppTheme.accentColor, size: 18),
                      SizedBox(width: 10),
                      Text('Play Video', style: TextStyle(fontSize: 13, color: Colors.white)),
                    ],
                  ),
                ),
                const PopupMenuItem(
                  value: 'playlist',
                  child: Row(
                    children: [
                      Icon(Icons.playlist_add, color: Colors.white70, size: 18),
                      SizedBox(width: 10),
                      Text('Add to Playlist', style: TextStyle(fontSize: 13, color: Colors.white)),
                    ],
                  ),
                ),
                const PopupMenuItem(
                  value: 'explorer',
                  child: Row(
                    children: [
                      Icon(Icons.folder_open, color: Colors.white70, size: 18),
                      SizedBox(width: 10),
                      Text('Show in File Explorer', style: TextStyle(fontSize: 13, color: Colors.white)),
                    ],
                  ),
                ),
                const PopupMenuDivider(height: 1),
                const PopupMenuItem(
                  value: 'delete',
                  child: Row(
                    children: [
                      Icon(Icons.delete_outline, color: Colors.redAccent, size: 18),
                      SizedBox(width: 10),
                      Text('Move to Trash', style: TextStyle(fontSize: 13, color: Colors.redAccent)),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _openInExplorer(String filePath) {
    if (Platform.isWindows) {
      Process.run('explorer.exe', ['/select,', filePath]);
    }
  }
}

