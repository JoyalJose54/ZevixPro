import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/folder_item.dart';
import '../../providers/media_provider.dart';
import '../../theme/app_theme.dart';
import 'player/desktop_player_screen.dart';
import '../widgets/folder_card.dart';
import '../widgets/video_card.dart';

class FoldersView extends StatefulWidget {
  const FoldersView({super.key});

  @override
  State<FoldersView> createState() => _FoldersViewState();
}

class _FoldersViewState extends State<FoldersView> {
  FolderItem? _selectedFolder;

  @override
  Widget build(BuildContext context) {
    final media = context.watch<MediaProvider>();

    if (media.isLoading) {
      return const Center(child: CircularProgressIndicator(color: AppTheme.accentColor));
    }

    if (_selectedFolder != null) {
      final folderVideos = media.getVideosInFolder(_selectedFolder!.path);
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.arrow_back, color: Colors.white),
                  onPressed: () => setState(() => _selectedFolder = null),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _selectedFolder!.name,
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
                      ),
                      Text(
                        "${folderVideos.length} videos • ${_selectedFolder!.formattedSize}",
                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const Divider(),
          Expanded(
            child: ListView.builder(
              itemCount: folderVideos.length,
              itemBuilder: (context, i) {
                final v = folderVideos[i];
                return VideoCard(
                  video: v,
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => DesktopPlayerScreen(
                          video: v,
                          playlist: folderVideos,
                          preferences: media.preferences,
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ],
      );
    }

    final folders = media.allFolders;

    if (folders.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.folder_open_outlined, size: 64, color: AppTheme.textMuted),
            const SizedBox(height: 16),
            const Text(
              "No video folders selected",
              style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              "Select a folder on your PC to view and play movies.",
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
            ),
            const SizedBox(height: 20),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.accentColor,
                foregroundColor: Colors.black,
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
              icon: const Icon(Icons.folder_open, size: 18),
              label: const Text("Select Folder", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              onPressed: () async {
                final selectedDir = await FilePicker.getDirectoryPath();
                if (selectedDir != null && context.mounted) {
                  await context.read<MediaProvider>().addWatchedFolder(selectedDir);
                }
              },
            ),
          ],
        ),
      );
    }

    return GridView.builder(
      padding: const EdgeInsets.all(16),
      gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
        maxCrossAxisExtent: 340,
        mainAxisExtent: 80,
        crossAxisSpacing: 12,
        mainAxisSpacing: 12,
      ),
      itemCount: folders.length + 1,
      itemBuilder: (context, i) {
        if (i == 0) {
          return Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: () async {
                final selectedDir = await FilePicker.getDirectoryPath();
                if (selectedDir != null && context.mounted) {
                  await context.read<MediaProvider>().addWatchedFolder(selectedDir);
                }
              },
              borderRadius: BorderRadius.circular(12),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: AppTheme.bgCard,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.accentColor.withValues(alpha: 0.5), width: 1.2),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        color: AppTheme.accentColor.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(Icons.create_new_folder_outlined, color: AppTheme.accentColor, size: 24),
                    ),
                    const SizedBox(width: 14),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            "Add Folder",
                            style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.bold),
                          ),
                          Text(
                            "Browse and scan folders",
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }
        final folder = folders[i - 1];
        return FolderCard(
          folder: folder,
          onTap: () => setState(() => _selectedFolder = folder),
        );
      },
    );
  }
}
