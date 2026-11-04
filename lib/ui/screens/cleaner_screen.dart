import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/video_item.dart';
import '../../providers/media_provider.dart';
import '../../services/cleaner_service.dart';
import '../../theme/app_theme.dart';
import '../widgets/video_card.dart';

class CleanerScreen extends StatefulWidget {
  const CleanerScreen({super.key});

  @override
  State<CleanerScreen> createState() => _CleanerScreenState();
}

class _CleanerScreenState extends State<CleanerScreen> {
  CleanerScanResult? _result;
  bool _isScanning = false;
  String? _selectedCategory; // "watched", "large", "duplicates"

  @override
  void initState() {
    super.initState();
    _runScan();
  }

  void _runScan() {
    setState(() => _isScanning = true);
    final media = context.read<MediaProvider>();
    final cleaner = CleanerService(
      preferences: media.preferences,
      trashService: media.trashService,
    );

    final res = cleaner.analyzeVideos(media.allVideos);
    setState(() {
      _result = res;
      _isScanning = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final media = context.watch<MediaProvider>();

    if (_isScanning || _result == null) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(color: AppTheme.accentColor),
            SizedBox(height: 16),
            Text("Analyzing storage usage on PC...", style: TextStyle(color: AppTheme.textSecondary)),
          ],
        ),
      );
    }

    if (_selectedCategory != null) {
      return _buildCategoryDetail(context, media);
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Storage Header Banner
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  AppTheme.accentColor.withOpacity(0.2),
                  const Color(0xFF14171F),
                ],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.accentColor.withOpacity(0.3)),
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppTheme.accentColor.withOpacity(0.2),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.cleaning_services, color: AppTheme.accentColor, size: 36),
                ),
                const SizedBox(width: 20),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        "Storage Cleaner & Optimizer",
                        style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        "Reclaimable Space: ${_result!.formatBytes(_result!.reclaimableBytes)}",
                        style: const TextStyle(color: AppTheme.accentColor, fontSize: 15, fontWeight: FontWeight.w700),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        "Safely removes watched videos and duplicate files into your Smart Trash.",
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.accentColor,
                    foregroundColor: Colors.black,
                    padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
                  ),
                  icon: const Icon(Icons.refresh),
                  label: const Text("Rescan", style: TextStyle(fontWeight: FontWeight.bold)),
                  onPressed: _runScan,
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // Categories Grid
          Row(
            children: [
              Expanded(
                child: _buildCleanerCard(
                  title: "Watched Videos",
                  subtitle: "${_result!.watchedVideos.length} videos",
                  size: _result!.formatBytes(_result!.watchedTotalBytes),
                  icon: Icons.history,
                  color: Colors.blueAccent,
                  onTap: () => setState(() => _selectedCategory = "watched"),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _buildCleanerCard(
                  title: "Large Files (>100MB)",
                  subtitle: "${_result!.largeVideos.length} files",
                  size: _result!.formatBytes(_result!.largeTotalBytes),
                  icon: Icons.disc_full_outlined,
                  color: AppTheme.yellowBoost,
                  onTap: () => setState(() => _selectedCategory = "large"),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _buildCleanerCard(
                  title: "Duplicate Videos",
                  subtitle: "${_result!.duplicateGroups.length} groups",
                  size: _result!.formatBytes(_result!.duplicateTotalBytes),
                  icon: Icons.copy,
                  color: Colors.purpleAccent,
                  onTap: () => setState(() => _selectedCategory = "duplicates"),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildCleanerCard({
    required String title,
    required String subtitle,
    required String size,
    required IconData icon,
    required Color color,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: AppTheme.bgCard,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppTheme.dividerLine),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: color.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(icon, color: color, size: 24),
                ),
                Text(
                  size,
                  style: TextStyle(color: color, fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              title,
              style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Text("View & Clean", style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600)),
                const SizedBox(width: 4),
                Icon(Icons.arrow_forward_ios, size: 10, color: color),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCategoryDetail(BuildContext context, MediaProvider media) {
    List<VideoItem> items = [];
    String title = "";

    if (_selectedCategory == "watched") {
      items = _result!.watchedVideos;
      title = "Watched Videos";
    } else if (_selectedCategory == "large") {
      items = _result!.largeVideos;
      title = "Large Files";
    } else if (_selectedCategory == "duplicates") {
      items = _result!.duplicateGroups.expand((g) => g.skip(1)).toList(); // duplicate copies
      title = "Duplicate Video Copies";
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
                onPressed: () => setState(() => _selectedCategory = null),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                    Text("${items.length} items found", style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                  ],
                ),
              ),
              if (items.isNotEmpty)
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
                  icon: const Icon(Icons.delete_sweep),
                  label: const Text("Clean All to Trash"),
                  onPressed: () async {
                    for (final item in items) {
                      await media.deleteVideo(item);
                    }
                    _runScan();
                    setState(() => _selectedCategory = null);
                  },
                ),
            ],
          ),
        ),
        const Divider(),
        Expanded(
          child: items.isEmpty
              ? const Center(child: Text("No files to clean in this category", style: TextStyle(color: AppTheme.textSecondary)))
              : ListView.builder(
                  itemCount: items.length,
                  itemBuilder: (context, i) {
                    final v = items[i];
                    return VideoCard(
                      video: v,
                      onTap: () {},
                      onDelete: () async {
                        await media.deleteVideo(v);
                        _runScan();
                      },
                    );
                  },
                ),
        ),
      ],
    );
  }
}

