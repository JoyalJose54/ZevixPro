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

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

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
      child: const ZevixProApp(),
    ),
  );
}

class ZevixProApp extends StatelessWidget {
  const ZevixProApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zevix Pro',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      home: const SplashScreen(),
    );
  }
}
