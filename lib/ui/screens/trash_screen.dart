import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/trash_item.dart';
import '../../providers/media_provider.dart';
import '../../services/trash_service.dart';
import '../../theme/app_theme.dart';

class TrashScreen extends StatefulWidget {
  const TrashScreen({super.key});

  @override
  State<TrashScreen> createState() => _TrashScreenState();
}

class _TrashScreenState extends State<TrashScreen> {
  late final TrashService _trashService;
  List<TrashItem> _trashItems = [];

  @override
  void initState() {
    super.initState();
    _trashService = context.read<MediaProvider>().trashService;
    _loadTrash();
  }

  void _loadTrash() {
    setState(() {
      _trashItems = _trashService.getTrashItems();
    });
  }

  void _confirmEmptyTrash() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.bgDialog,
        title: const Text("Empty Trash Bin?", style: TextStyle(color: Colors.white)),
        content: const Text(
          "All files in Trash will be permanently erased. This cannot be undone.",
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text("Cancel", style: TextStyle(color: Colors.white70)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
            onPressed: () async {
              await _trashService.emptyTrash();
              _loadTrash();
              Navigator.of(ctx).pop();
            },
            child: const Text("Empty Trash"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Column(
        children: [
          // Header info
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                const Icon(Icons.delete_outline, color: AppTheme.accentColor, size: 28),
                const SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      "Smart Trash Bin",
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                    Text(
                      "${_trashItems.length} items • Files are auto-purged after 30 days",
                      style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                    ),
                  ],
                ),
                const Spacer(),
                if (_trashItems.isNotEmpty)
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
                    icon: const Icon(Icons.delete_forever),
                    label: const Text("Empty Trash"),
                    onPressed: _confirmEmptyTrash,
                  ),
              ],
            ),
          ),
          const Divider(),

          Expanded(
            child: _trashItems.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.delete_sweep_outlined, size: 64, color: AppTheme.textMuted.withOpacity(0.5)),
                        const SizedBox(height: 16),
                        const Text("Trash is empty", style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 6),
                        const Text("Deleted videos will appear here for 30 days before permanent erase.", style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                      ],
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    itemCount: _trashItems.length,
                    itemBuilder: (context, i) {
                      final item = _trashItems[i];
                      return Container(
                        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                        padding: const EdgeInsets.all(12),
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
                                color: Colors.redAccent.withOpacity(0.12),
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: const Icon(Icons.movie_outlined, color: Colors.redAccent, size: 24),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    item.title,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.bold),
                                  ),
                                  const SizedBox(height: 4),
                                  Row(
                                    children: [
                                      Text(item.formattedSize, style: const TextStyle(color: AppTheme.textMuted, fontSize: 12)),
                                      const SizedBox(width: 10),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1.5),
                                        decoration: BoxDecoration(
                                          color: Colors.amber.withOpacity(0.15),
                                          borderRadius: BorderRadius.circular(4),
                                        ),
                                        child: Text(
                                          "${item.daysRemaining} days left",
                                          style: const TextStyle(color: Colors.amber, fontSize: 11, fontWeight: FontWeight.bold),
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                            // Restore Button
                            IconButton(
                              icon: const Icon(Icons.restore, color: AppTheme.accentColor),
                              tooltip: "Restore to original location",
                              onPressed: () async {
                                final ok = await _trashService.restoreItem(item);
                                if (ok) {
                                  _loadTrash();
                                  context.read<MediaProvider>().loadMedia();
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(content: Text("Restored '${item.title}'"), backgroundColor: AppTheme.bgCard),
                                  );
                                }
                              },
                            ),
                            // Permanent delete button
                            IconButton(
                              icon: const Icon(Icons.delete_forever, color: Colors.redAccent),
                              tooltip: "Erase permanently",
                              onPressed: () async {
                                await _trashService.permanentDelete(item);
                                _loadTrash();
                              },
                            ),
                          ],
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}

