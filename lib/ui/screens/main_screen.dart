import 'dart:io';
import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:provider/provider.dart';
import '../../models/video_item.dart';
import '../../providers/media_provider.dart';
import '../../theme/app_theme.dart';
import 'player/desktop_player_screen.dart';
import '../widgets/window_title_bar.dart';
import 'folders_view.dart';
import 'settings_screen.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final TextEditingController _searchController = TextEditingController();
  bool _isSearching = false;

  @override
  void initState() {
    super.initState();
    // 2 primary tabs: Folders, Settings
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(() {
      setState(() {});
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _pickFolder() async {
    final selectedDir = await FilePicker.getDirectoryPath();
    if (selectedDir != null && mounted) {
      final media = context.read<MediaProvider>();
      await media.addWatchedFolder(selectedDir);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Added folder '$selectedDir' to library"),
            backgroundColor: AppTheme.bgCard,
            duration: const Duration(seconds: 2),
          ),
        );
      }
    }
  }

  Future<void> _openSingleFile() async {
    final files = await FilePicker.pickFiles(
      type: FileType.video,
    );

    if (files.isNotEmpty && files.first.path != null && mounted) {
      final file = files.first;
      final path = file.path!;
      final media = context.read<MediaProvider>();
      final item = VideoItem(
        id: path.hashCode,
        title: file.name,
        displayName: file.name,
        path: path,
        duration: Duration.zero,
        size: File(path).existsSync() ? File(path).lengthSync() : 0,
        dateModified: DateTime.now(),
        folderName: "External",
        folderPath: "",
      );

      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => DesktopPlayerScreen(
            video: item,
            playlist: [item],
            preferences: media.preferences,
          ),
        ),
      );
    }
  }

  void _showNetworkStreamDialog() {
    final urlController = TextEditingController(
      text: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    );

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.bgDialog,
        title: const Text("Network Stream", style: TextStyle(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              "Enter video stream URL (HTTP, HTTPS, RTSP, HLS, etc.):",
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: urlController,
              autofocus: true,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                hintText: "https://...",
                hintStyle: TextStyle(color: Colors.white38),
                enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white24)),
                focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: AppTheme.accentColor)),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text("Cancel", style: TextStyle(color: Colors.white70)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.accentColor, foregroundColor: Colors.black),
            onPressed: () {
              final url = urlController.text.trim();
              if (url.isNotEmpty) {
                Navigator.of(ctx).pop();
                final media = context.read<MediaProvider>();
                final item = VideoItem(
                  id: url.hashCode,
                  title: url.split('/').last,
                  displayName: url,
                  path: url,
                  duration: Duration.zero,
                  size: 0,
                  dateModified: DateTime.now(),
                  folderName: "Stream",
                  folderPath: "",
                );

                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => DesktopPlayerScreen(
                      video: item,
                      playlist: [item],
                      preferences: media.preferences,
                    ),
                  ),
                );
              }
            },
            child: const Text("Play Stream"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final media = context.watch<MediaProvider>();

    return Scaffold(
      backgroundColor: AppTheme.bgMain,
      body: Column(
        children: [
          // Top Green Banner Title Bar (matching sample screenshot)
          const WindowTitleBar(
            title: "Zevix Pro",
            isGreenBanner: true,
          ),

          // Action Toolbar & Navigation Tabs Row
          Container(
            height: 48,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            decoration: const BoxDecoration(
              color: AppTheme.bgMain,
              border: Border(bottom: BorderSide(color: AppTheme.dividerLine)),
            ),
            child: Row(
              children: [
                // Navigation Tabs (Folders, Settings only)
                Expanded(
                  child: TabBar(
                    controller: _tabController,
                    isScrollable: true,
                    tabAlignment: TabAlignment.start,
                    indicatorColor: AppTheme.accentColor,
                    indicatorWeight: 3,
                    labelColor: AppTheme.accentColor,
                    unselectedLabelColor: AppTheme.textSecondary,
                    labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                    dividerColor: Colors.transparent,
                    onTap: (index) {
                      if (index == 0) {
                        _pickFolder();
                      }
                    },
                    tabs: const [
                      Tab(icon: Icon(Icons.folder_outlined, size: 18), text: "Folders"),
                      Tab(icon: Icon(Icons.settings_outlined, size: 18), text: "Settings"),
                    ],
                  ),
                ),

                // Search Bar or Search Button
                if (_isSearching)
                  Container(
                    width: 220,
                    height: 32,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    decoration: BoxDecoration(
                      color: AppTheme.bgCard,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: AppTheme.accentColor, width: 1),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.search, color: AppTheme.accentColor, size: 16),
                        const SizedBox(width: 6),
                        Expanded(
                          child: TextField(
                            controller: _searchController,
                            autofocus: true,
                            style: const TextStyle(color: Colors.white, fontSize: 12),
                            decoration: const InputDecoration(
                              hintText: "Search videos...",
                              hintStyle: TextStyle(color: AppTheme.textMuted, fontSize: 12),
                              border: InputBorder.none,
                              isDense: true,
                              contentPadding: EdgeInsets.zero,
                            ),
                            onChanged: (q) => media.setSearchQuery(q),
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.close, color: AppTheme.textMuted, size: 14),
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                          onPressed: () {
                            _searchController.clear();
                            media.setSearchQuery("");
                            setState(() => _isSearching = false);
                          },
                        ),
                      ],
                    ),
                  )
                else
                  IconButton(
                    icon: const Icon(Icons.search, color: Colors.white70, size: 20),
                    tooltip: "Search videos",
                    onPressed: () => setState(() => _isSearching = true),
                  ),

                const SizedBox(width: 8),

                // Select Folder Button (Primary button in Bright Lime Green)
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.accentColor,
                    foregroundColor: Colors.black,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
                  ),
                  icon: const Icon(Icons.folder_open, size: 15),
                  label: const Text("Select Folder", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                  onPressed: _pickFolder,
                ),

                const SizedBox(width: 8),

                // Open File Button
                OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.white,
                    side: const BorderSide(color: AppTheme.dividerLine),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
                  ),
                  icon: const Icon(Icons.file_open, size: 15, color: AppTheme.accentColor),
                  label: const Text("Open File", style: TextStyle(fontSize: 12)),
                  onPressed: _openSingleFile,
                ),

                const SizedBox(width: 8),

                // Network Stream Button
                OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.white,
                    side: const BorderSide(color: AppTheme.dividerLine),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
                  ),
                  icon: const Icon(Icons.stream, size: 15, color: AppTheme.accentColor),
                  label: const Text("Stream", style: TextStyle(fontSize: 12)),
                  onPressed: _showNetworkStreamDialog,
                ),
              ],
            ),
          ),

          // Main View Tabs (Folders, Settings)
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: const [
                FoldersView(),
                SettingsScreen(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
