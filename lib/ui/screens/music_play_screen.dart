import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/music_provider.dart';
import '../../theme/app_theme.dart';
import '../widgets/equalizer_dialog.dart';

class MusicPlayScreen extends StatefulWidget {
  const MusicPlayScreen({super.key});

  @override
  State<MusicPlayScreen> createState() => _MusicPlayScreenState();
}

class _MusicPlayScreenState extends State<MusicPlayScreen> with SingleTickerProviderStateMixin {
  late AnimationController _discController;

  @override
  void initState() {
    super.initState();
    _discController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 18),
    )..repeat();
  }

  @override
  void dispose() {
    _discController.dispose();
    super.dispose();
  }

  String _formatDuration(Duration d) {
    final minutes = d.inMinutes;
    final seconds = d.inSeconds.remainder(60);
    return "${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
  }

  @override
  Widget build(BuildContext context) {
    final music = context.watch<MusicProvider>();
    final track = music.currentTrack;

    if (music.isPlaying) {
      if (!_discController.isAnimating) _discController.repeat();
    } else {
      if (_discController.isAnimating) _discController.stop();
    }

    if (track == null) {
      return Scaffold(
        backgroundColor: AppTheme.bgMain,
        appBar: AppBar(backgroundColor: Colors.transparent),
        body: const Center(
          child: Text("No track selected", style: TextStyle(color: Colors.white70)),
        ),
      );
    }

    final pos = music.position;
    final dur = music.duration;
    final progress = dur.inMilliseconds > 0
        ? (pos.inMilliseconds / dur.inMilliseconds).clamp(0.0, 1.0)
        : 0.0;

    return Scaffold(
      backgroundColor: AppTheme.bgMain,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        leading: IconButton(
          icon: const Icon(Icons.keyboard_arrow_down, size: 28),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text("Now Playing", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.equalizer, color: AppTheme.accentColor),
            tooltip: "Audio Equalizer & FX",
            onPressed: () {
              showDialog(
                context: context,
                builder: (_) => EqualizerDialog(preferences: music.preferences),
              );
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Center(
        child: Container(
          constraints: const BoxConstraints(maxWidth: 540),
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Spacer(),
              // Vinyl Disc / Album Art
              RotationTransition(
                turns: _discController,
                child: Container(
                  width: 240,
                  height: 240,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: const Color(0xFF14171F),
                    border: Border.all(color: AppTheme.accentColor.withOpacity(0.4), width: 3),
                    boxShadow: [
                      BoxShadow(
                        color: AppTheme.accentColor.withOpacity(0.15),
                        blurRadius: 30,
                        spreadRadius: 4,
                      ),
                    ],
                  ),
                  child: Center(
                    child: Container(
                      width: 80,
                      height: 80,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.black,
                      ),
                      child: const Center(
                        child: Icon(Icons.music_note, color: AppTheme.accentColor, size: 40),
                      ),
                    ),
                  ),
                ),
              ),
              const Spacer(),

              // Track Title & Artist
              Text(
                track.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                track.artist,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: AppTheme.textSecondary,
                  fontSize: 14,
                ),
              ),
              const SizedBox(height: 24),

              // Progress Bar
              SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  trackHeight: 3.5,
                  thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
                  overlayShape: const RoundSliderOverlayShape(overlayRadius: 14),
                ),
                child: Slider(
                  value: progress,
                  activeColor: AppTheme.accentColor,
                  inactiveColor: Colors.white24,
                  onChanged: (val) {
                    final target = Duration(milliseconds: (val * dur.inMilliseconds).toInt());
                    music.seek(target);
                  },
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(_formatDuration(pos), style: const TextStyle(color: Colors.white54, fontSize: 11)),
                    Text(_formatDuration(dur), style: const TextStyle(color: Colors.white54, fontSize: 11)),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Control Buttons
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  IconButton(
                    icon: Icon(
                      Icons.shuffle,
                      color: music.isShuffle ? AppTheme.accentColor : Colors.white54,
                      size: 22,
                    ),
                    tooltip: "Shuffle",
                    onPressed: () => music.toggleShuffle(),
                  ),
                  IconButton(
                    icon: const Icon(Icons.skip_previous_rounded, color: Colors.white, size: 36),
                    tooltip: "Previous Track",
                    onPressed: () => music.playPrevious(),
                  ),
                  IconButton(
                    icon: Icon(
                      music.isPlaying ? Icons.pause_circle_filled : Icons.play_circle_filled,
                      color: AppTheme.accentColor,
                      size: 64,
                    ),
                    tooltip: "Play / Pause",
                    onPressed: () => music.togglePlayPause(),
                  ),
                  IconButton(
                    icon: const Icon(Icons.skip_next_rounded, color: Colors.white, size: 36),
                    tooltip: "Next Track",
                    onPressed: () => music.playNext(),
                  ),
                  IconButton(
                    icon: Icon(
                      Icons.repeat,
                      color: music.isRepeat ? AppTheme.accentColor : Colors.white54,
                      size: 22,
                    ),
                    tooltip: "Repeat",
                    onPressed: () => music.toggleRepeat(),
                  ),
                ],
              ),
              const Spacer(),
            ],
          ),
        ),
      ),
    );
  }
}

