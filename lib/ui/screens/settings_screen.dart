import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:provider/provider.dart';
import '../../providers/media_provider.dart';
import '../../services/playback_preferences.dart';
import '../../theme/app_theme.dart';
import '../widgets/equalizer_dialog.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late PlaybackPreferences _prefs;

  @override
  void initState() {
    super.initState();
    _prefs = context.read<MediaProvider>().preferences;
  }

  Future<void> _pickFolder() async {
    final selectedDir = await FilePicker.getDirectoryPath();
    if (selectedDir != null) {
      final media = context.read<MediaProvider>();
      await media.addWatchedFolder(selectedDir);
      setState(() {});
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Added '$selectedDir' to library"), backgroundColor: AppTheme.bgCard),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final media = context.watch<MediaProvider>();

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        // Playback Engine Section
        const Text(
          "CINEMA PLAYBACK ENGINE",
          style: TextStyle(color: AppTheme.accentColor, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1),
        ),
        const SizedBox(height: 12),

        Container(
          decoration: BoxDecoration(
            color: AppTheme.bgCard,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppTheme.dividerLine),
          ),
          child: Column(
            children: [
              ListTile(
                leading: const Icon(Icons.bolt, color: AppTheme.accentColor),
                title: const Text("Hardware Video Acceleration", style: TextStyle(color: Colors.white)),
                subtitle: const Text(
                  "Direct3D11 (GPU Decode) — Uses your PC's GPU for stutter-free 4K/HDR 60fps playback",
                  style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                ),
                trailing: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppTheme.accentColor.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: AppTheme.accentColor),
                  ),
                  child: const Text("D3D11 Active", style: TextStyle(color: AppTheme.accentColor, fontSize: 11, fontWeight: FontWeight.bold)),
                ),
              ),
              const Divider(),
              SwitchListTile(
                secondary: const Icon(Icons.history, color: Colors.white70),
                title: const Text("Resume Playback", style: TextStyle(color: Colors.white)),
                subtitle: const Text("Resume video from where you stopped", style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                activeColor: AppTheme.accentColor,
                value: _prefs.resumePlayback,
                onChanged: (val) {
                  setState(() => _prefs.setResumePlayback(val));
                },
              ),
              const Divider(),
              SwitchListTile(
                secondary: const Icon(Icons.skip_next, color: Colors.white70),
                title: const Text("Auto-Play Next Video", style: TextStyle(color: Colors.white)),
                subtitle: const Text("Automatically play the next media file in queue", style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                activeColor: AppTheme.accentColor,
                value: _prefs.autoPlayNext,
                onChanged: (val) {
                  setState(() => _prefs.setAutoPlayNext(val));
                },
              ),
            ],
          ),
        ),

        const SizedBox(height: 28),

        // Audio & Visual Section
        const Text(
          "AUDIO & EQUALIZER",
          style: TextStyle(color: AppTheme.accentColor, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1),
        ),
        const SizedBox(height: 12),

        Container(
          decoration: BoxDecoration(
            color: AppTheme.bgCard,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppTheme.dividerLine),
          ),
          child: ListTile(
            leading: const Icon(Icons.equalizer, color: AppTheme.accentColor),
            title: const Text("Audio Equalizer & Sound FX", style: TextStyle(color: Colors.white)),
            subtitle: const Text("10-Band EQ presets, Bass Boost, and 3D Virtualizer", style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
            trailing: const Icon(Icons.chevron_right, color: Colors.white70),
            onTap: () {
              showDialog(
                context: context,
                builder: (_) => EqualizerDialog(preferences: _prefs),
              );
            },
          ),
        ),

        const SizedBox(height: 28),

        // Watched Folders Library Section
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              "LIBRARY WATCHED FOLDERS",
              style: TextStyle(color: AppTheme.accentColor, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1),
            ),
            TextButton.icon(
              icon: const Icon(Icons.add, size: 18),
              label: const Text("Add Folder / Drive"),
              style: TextButton.styleFrom(foregroundColor: AppTheme.accentColor),
              onPressed: _pickFolder,
            ),
          ],
        ),
        const SizedBox(height: 8),

        Container(
          decoration: BoxDecoration(
            color: AppTheme.bgCard,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppTheme.dividerLine),
          ),
          child: Column(
            children: [
              const ListTile(
                leading: Icon(Icons.folder_special, color: Colors.white70),
                title: Text("Default PC Media Folders", style: TextStyle(color: Colors.white)),
                subtitle: Text("Videos, Downloads, Music", style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                trailing: Text("Default", style: TextStyle(color: AppTheme.textMuted, fontSize: 11)),
              ),
              if (_prefs.watchedFolders.isNotEmpty) const Divider(),
              ..._prefs.watchedFolders.map((fPath) {
                return ListTile(
                  leading: const Icon(Icons.folder, color: AppTheme.accentColor),
                  title: Text(fPath, style: const TextStyle(color: Colors.white, fontSize: 13)),
                  trailing: IconButton(
                    icon: const Icon(Icons.remove_circle_outline, color: Colors.redAccent, size: 20),
                    tooltip: "Remove folder",
                    onPressed: () async {
                      await media.removeWatchedFolder(fPath);
                      setState(() {});
                    },
                  ),
                );
              }),
            ],
          ),
        ),

        const SizedBox(height: 36),

        // About Zevix Pro for Windows
        Center(
          child: Column(
            children: [
              Image.asset('assets/logo.png', width: 48, height: 48, errorBuilder: (_, __, ___) => const Icon(Icons.play_circle_fill, color: AppTheme.accentColor, size: 48)),
              const SizedBox(height: 10),
              const Text("Zevix Pro for Windows", style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              const Text("Version 1.0.0 • Direct3D11 Accelerated • MPV Engine", style: TextStyle(color: AppTheme.textMuted, fontSize: 12)),
            ],
          ),
        ),
      ],
    );
  }
}
