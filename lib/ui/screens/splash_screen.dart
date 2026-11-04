import 'dart:io';
import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';
import 'package:provider/provider.dart';
import '../../models/video_item.dart';
import '../../providers/media_provider.dart';
import '../../services/playback_preferences.dart';
import '../widgets/window_title_bar.dart';
import 'main_screen.dart';
import 'player/desktop_player_screen.dart';

class SplashScreen extends StatefulWidget {
  final String? initialFilePath;
  final PlaybackPreferences? preferences;

  const SplashScreen({
    super.key,
    this.initialFilePath,
    this.preferences,
  });

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with SingleTickerProviderStateMixin {
  late AnimationController _animController;
  bool _isNavigated = false;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(vsync: this);
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  void _navigateToNext() {
    if (_isNavigated) return;
    _isNavigated = true;

    if (!mounted) return;

    final filePath = widget.initialFilePath;
    final hasFile = filePath != null && filePath.isNotEmpty && File(filePath).existsSync();

    if (hasFile) {
      final video = VideoItem.fromPath(filePath);
      final playlist = VideoItem.createPlaylistFromPath(filePath);
      final prefs = widget.preferences ?? context.read<MediaProvider>().preferences;

      Navigator.of(context).pushReplacement(
        PageRouteBuilder(
          transitionDuration: const Duration(milliseconds: 300),
          pageBuilder: (_, __, ___) => const MainScreen(),
        ),
      );

      Navigator.of(context).push(
        PageRouteBuilder(
          transitionDuration: const Duration(milliseconds: 300),
          pageBuilder: (_, __, ___) => DesktopPlayerScreen(
            video: video,
            playlist: playlist,
            preferences: prefs,
          ),
          transitionsBuilder: (_, animation, __, child) {
            return FadeTransition(
              opacity: animation,
              child: child,
            );
          },
        ),
      );
    } else {
      Navigator.of(context).pushReplacement(
        PageRouteBuilder(
          transitionDuration: const Duration(milliseconds: 400),
          pageBuilder: (context, animation, secondaryAnimation) => const MainScreen(),
          transitionsBuilder: (context, animation, secondaryAnimation, child) {
            return FadeTransition(
              opacity: animation,
              child: child,
            );
          },
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Column(
        children: [
          // Sleek custom titlebar for Windows window controls & dragging
          const WindowTitleBar(
            title: "Zevix Pro",
            isGreenBanner: false,
          ),

          // Splash Animation Area (Click anywhere to skip, matches Android SplashActivity)
          Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: _navigateToNext,
              child: Center(
                child: SizedBox(
                  width: 320,
                  height: 320,
                  child: Lottie.asset(
                    'assets/video_splash.json',
                    controller: _animController,
                    fit: BoxFit.contain,
                    onLoaded: (composition) {
                      _animController.duration = composition.duration;
                      _animController.forward().then((_) {
                        _navigateToNext();
                      });
                    },
                    errorBuilder: (context, error, stackTrace) {
                      // Fallback if Lottie asset fails to load
                      WidgetsBinding.instance.addPostFrameCallback((_) {
                        Future.delayed(const Duration(milliseconds: 800), _navigateToNext);
                      });
                      return Image.asset(
                        'assets/logo.png',
                        width: 140,
                        height: 140,
                        fit: BoxFit.contain,
                      );
                    },
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

