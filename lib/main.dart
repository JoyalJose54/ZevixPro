import 'dart:io';
import 'package:flutter/material.dart';
import 'package:media_kit/media_kit.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:window_manager/legacy.dart';

import 'providers/media_provider.dart';
import 'providers/music_provider.dart';
import 'providers/playlist_provider.dart';
import 'services/playback_preferences.dart';
import 'services/trash_service.dart';
import 'theme/app_theme.dart';
import 'ui/screens/splash_screen.dart';

String? _parseInitialFilePath(List<String> args) {
  for (final raw in args) {
    var candidate = raw.trim();
    if (candidate.startsWith('"') && candidate.endsWith('"') && candidate.length > 1) {
      candidate = candidate.substring(1, candidate.length - 1).trim();
    }
    if (candidate.startsWith("'") && candidate.endsWith("'") && candidate.length > 1) {
      candidate = candidate.substring(1, candidate.length - 1).trim();
    }
    if (candidate.startsWith('file://')) {
      try {
        candidate = Uri.parse(candidate).toFilePath();
      } catch (_) {}
    }
    if (candidate.isNotEmpty && !candidate.startsWith('-')) {
      final file = File(candidate);
      if (file.existsSync()) {
        return file.absolute.path;
      }
    }
  }
  return null;
}

Future<void> main(List<String> args) async {
  WidgetsFlutterBinding.ensureInitialized();

  final initialFilePath = _parseInitialFilePath(args);

  // Initialize MediaKit audio/video playback engine
  MediaKit.ensureInitialized();

  // Initialize Desktop Window Manager on Windows
  if (Platform.isWindows) {
    await windowManager.ensureInitialized();

    const windowOptions = WindowOptions(
      size: Size(1280, 800),
      minimumSize: Size(960, 600),
      center: true,
      backgroundColor: Colors.black,
      skipTaskbar: false,
      titleBarStyle: TitleBarStyle.hidden, // Frameless window with custom sleek titlebar
      title: "Zevix Pro",
    );

    await windowManager.waitUntilReadyToShow(windowOptions, () async {
      await windowManager.show();
      await windowManager.focus();
    });
  }

  // Load persistence services
  final sp = await SharedPreferences.getInstance();
  final prefs = PlaybackPreferences(sp);
  final trashService = await TrashService.create();
  final playlistProv = PlaylistProvider(sp);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => MediaProvider(
            preferences: prefs,
            trashService: trashService,
          ),
        ),
        ChangeNotifierProvider(
          create: (_) => MusicProvider(preferences: prefs),
        ),
        ChangeNotifierProvider.value(value: playlistProv),
      ],
      child: ZevixProApp(
        initialFilePath: initialFilePath,
        preferences: prefs,
      ),
    ),
  );
}

class ZevixProApp extends StatelessWidget {
  final String? initialFilePath;
  final PlaybackPreferences preferences;

  const ZevixProApp({
    super.key,
    this.initialFilePath,
    required this.preferences,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zevix Pro',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      home: SplashScreen(
        initialFilePath: initialFilePath,
        preferences: preferences,
      ),
    );
  }
}
