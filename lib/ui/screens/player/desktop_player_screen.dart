import 'dart:async';
import 'dart:io';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/gestures.dart';
import 'package:file_picker/file_picker.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:window_manager/legacy.dart';
import '../../../models/video_item.dart';
import '../../../services/playback_preferences.dart';
import '../../widgets/window_title_bar.dart';
import 'player_controls_menu.dart';
import 'color_filter_utils.dart';

enum VideoAspectRatioMode {
  fit,
  stretch,
  sixteenNine,
  fourThree,
  zoomCrop,
  original,
}

class DesktopPlayerScreen extends StatefulWidget {
  final VideoItem video;
  final List<VideoItem> playlist;
  final PlaybackPreferences preferences;

  const DesktopPlayerScreen({
    super.key,
    required this.video,
    required this.playlist,
    required this.preferences,
  });

  @override
  State<DesktopPlayerScreen> createState() => _DesktopPlayerScreenState();
}

class _DesktopPlayerScreenState extends State<DesktopPlayerScreen> with WindowListener {
  late final Player _player;
  late final VideoController _videoController;

  late VideoItem _currentVideo;
  late int _currentIndex;

  bool _isFullscreen = false;
  bool _showControls = true;
  Timer? _hideControlsTimer;

  Duration _position = Duration.zero;
  final ValueNotifier<Duration> _positionNotifier = ValueNotifier<Duration>(Duration.zero);
  final ValueNotifier<Duration> _bufferNotifier = ValueNotifier<Duration>(Duration.zero);
  Duration _duration = Duration.zero;
  Duration _buffer = Duration.zero;
  bool _isPlaying = false;
  double _volume = 100.0;
  bool _isMuted = false;
  double _playbackSpeed = 1.0;
  VideoAspectRatioMode _aspectRatioMode = VideoAspectRatioMode.fit;
  String _zoomMode = "Original";

  // End time & duration toggle
  bool _showRemainingTime = false;

  // Floating Menu Drawer State (matching screen recording)
  bool _isMenuOpen = false;
  PlayerMenuPage _menuPage = PlayerMenuPage.main;

  // Volume Boost (100% to 600%)
  double _volumeBoost = 100.0;

  // Color modifications (Brightness, Contrast, Saturation, Hue)
  double _quickBrightness = 100.0;
  double _brightness = 100.0; // 50% - 200%
  double _contrast = 100.0; // 50% - 200%
  double _saturation = 100.0; // 0% - 200%
  double _hue = 0.0; // 0° - 360°

  // Subtitles
  bool _subtitlesEnabled = true;
  SubtitleTrack _selectedSubtitle = SubtitleTrack.no();
  bool _useNativeSubtitles = false;
  bool _pastelMode = false;
  double _subtitleDelay = 0.0;
  String _subtitleFont = 'Montserrat';
  double _subtitleSize = 100.0;
  double _subtitlePosition = 100.0;
  bool _subtitleBackground = true;
  Color _subtitleColor = Colors.white;

  // Audio Tracks
  AudioTrack _selectedAudio = AudioTrack.auto();

  // Sleep Timer
  Timer? _sleepTimer;
  int _sleepMinutesRemaining = 0;
  bool _sleepAtEndOfShow = false;

  // Scrub bar drag state
  bool _isDraggingSeek = false;
  double _dragSeekPercent = 0.0;

  // Focus node for keyboard shortcuts
  final FocusNode _focusNode = FocusNode();

  StreamSubscription? _posSub;
  StreamSubscription? _durSub;
  StreamSubscription? _bufSub;
  StreamSubscription? _playSub;
  StreamSubscription? _compSub;
  StreamSubscription? _tracksSub;
  StreamSubscription? _trackSub;
  StreamSubscription? _videoParamsSub;

  // Press-and-hold Shift for 2x playback boost
  bool _isShiftBoostActive = false;
  double _preShiftRate = 1.0;

  // Bright Lime Green (#95FF50, RGB: 149, 255, 80)
  static const Color accentLimeGreen = Color(0xFF95FF50);

  @override
  void initState() {
    super.initState();
    windowManager.addListener(this);

    _currentVideo = widget.video;
    _currentIndex = widget.playlist.indexWhere((v) => v.path == _currentVideo.path);
    if (_currentIndex == -1) _currentIndex = 0;

    // Direct3D 11 GPU Hardware Acceleration Engine on Windows
    _player = Player(
      configuration: const PlayerConfiguration(
        title: "Zevix Pro Cinema Engine",
        bufferSize: 150 * 1024 * 1024,
      ),
    );

    _videoController = VideoController(
      _player,
      configuration: const VideoControllerConfiguration(
        enableHardwareAcceleration: true,
      ),
    );

    _initPlayerListeners();
    _loadVideo(_currentVideo);
    _resetHideControlsTimer();
  }

  void _initPlayerListeners() {
    _posSub = _player.stream.position.listen((pos) {
      if (mounted) {
        _position = pos;
        if (!_isDraggingSeek) {
          _positionNotifier.value = pos;
        }

        // Periodically save resume position
        if (pos.inSeconds > 0 && pos.inSeconds % 5 == 0) {
          widget.preferences.savePosition(_currentVideo.path, pos.inMilliseconds);
        }
      }
    });

    _durSub = _player.stream.duration.listen((dur) {
      if (mounted) setState(() => _duration = dur);
    });

    _bufSub = _player.stream.buffer.listen((buf) {
      if (mounted) {
        _buffer = buf;
        _bufferNotifier.value = buf;
      }
    });

    _playSub = _player.stream.playing.listen((playing) {
      if (mounted) setState(() => _isPlaying = playing);
    });

    _tracksSub = _player.stream.tracks.listen((tracks) {
      if (mounted) setState(() {});
    });

    _videoParamsSub = _player.stream.videoParams.listen((params) {
      if (mounted) setState(() {});
    });

    _trackSub = _player.stream.track.listen((track) {
      if (mounted) {
        setState(() {
          _selectedSubtitle = track.subtitle;
          _selectedAudio = track.audio;
        });
      }
    });

    _compSub = _player.stream.completed.listen((completed) {
      if (completed && mounted) {
        if (_sleepAtEndOfShow) {
          _player.pause();
          _showHud(Icons.bedtime, "Playback stopped (Sleep Timer: End of Show)");
          return;
        }
        widget.preferences.clearPosition(_currentVideo.path);
        if (widget.preferences.autoPlayNext) {
          _playNext();
        }
      }
    });
  }

  Future<void> _loadVideo(VideoItem video) async {
    _currentVideo = video;
    _currentIndex = widget.playlist.indexWhere((v) => v.path == _currentVideo.path);

    await _player.open(Media(video.path));

    // Resume position check
    if (widget.preferences.resumePlayback) {
      final savedMs = widget.preferences.getSavedPosition(video.path);
      if (savedMs > 3000) {
        await _player.seek(Duration(milliseconds: savedMs));
        _showHud(Icons.history, "Resumed at ${_formatDuration(Duration(milliseconds: savedMs))}");
      }
    }

    _volume = widget.preferences.playerVolume;
    await _player.setVolume(_volume);
    await _player.play();

    _applyMpvColors();
    _applyMpvSubtitles();
  }

  void _setMpvProperty(String property, String value) {
    try {
      (_player.platform as dynamic).setProperty(property, value);
    } catch (_) {}
  }

  void _applyMpvColors() {
    final effectiveBrightness = ((_brightness - 100.0) + (_quickBrightness - 100.0)).clamp(-100.0, 100.0);
    _setMpvProperty('brightness', effectiveBrightness.round().toString());
    _setMpvProperty('contrast', ((_contrast - 100.0)).round().toString());
    _setMpvProperty('saturation', ((_saturation - 100.0)).round().toString());
    _setMpvProperty('hue', _hue.round().toString());
  }

  void _applyMpvSubtitles() {
    _setMpvProperty('sub-visibility', _subtitlesEnabled ? 'yes' : 'no');
    _setMpvProperty('sub-delay', _subtitleDelay.toStringAsFixed(2));
    _setMpvProperty('sub-font-size', (_subtitleSize * 0.55).toInt().toString());
    _setMpvProperty('sub-font', _subtitleFont);
    _setMpvProperty('sub-pos', (100 - _subtitlePosition * 0.2).toInt().toString());

    final hex = '#${_subtitleColor.toARGB32().toRadixString(16).padLeft(8, '0')}';
    _setMpvProperty('sub-color', hex);
    if (_subtitleBackground) {
      _setMpvProperty('sub-back-color', '#B0000000');
    } else {
      _setMpvProperty('sub-back-color', '#00000000');
    }
  }

  void _showHud(IconData icon, String message) {
    // Disabled as requested: no notifications or HUD in the middle of the screen
  }

  void _resetHideControlsTimer() {
    _hideControlsTimer?.cancel();
    if (!_showControls) {
      setState(() => _showControls = true);
    }
    // Do not auto-hide while the options drawer is open!
    if (_isMenuOpen) return;

    _hideControlsTimer = Timer(const Duration(seconds: 4), () {
      if (mounted && _isPlaying && !_isDraggingSeek && !_isMenuOpen) {
        setState(() => _showControls = false);
      }
    });
  }

  void _togglePlayPause() {
    _player.playOrPause();
    _showHud(
      _isPlaying ? Icons.pause_circle_filled : Icons.play_circle_filled,
      _isPlaying ? "Paused" : "Playing",
    );
    _resetHideControlsTimer();
  }

  void _seekRelative(int seconds) {
    final target = _position + Duration(seconds: seconds);
    final clamped = target < Duration.zero ? Duration.zero : (target > _duration ? _duration : target);
    _player.seek(clamped);
    _showHud(
      seconds > 0 ? Icons.forward_10 : Icons.replay_10,
      "${seconds > 0 ? '+' : ''}$seconds sec (${_formatDuration(clamped)})",
    );
    _resetHideControlsTimer();
  }

  void _adjustVolume(double delta) {
    double newVol = (_volume + delta).clamp(0.0, 100.0);
    setState(() {
      _volume = newVol;
      _isMuted = false;
    });
    final boostedVolume = (_volume * (_volumeBoost / 100.0)).clamp(0.0, 600.0);
    _player.setVolume(boostedVolume);
    widget.preferences.setPlayerVolume(_volume);
    _showHud(
      _volume > 100 ? Icons.volume_up_outlined : (_volume > 0 ? Icons.volume_up : Icons.volume_off),
      "Volume: ${_volume.toInt()}%${_volumeBoost > 100 ? ' (Boost ${_volumeBoost.toInt()}%)' : ''}",
    );
    _resetHideControlsTimer();
  }

  void _toggleMute() {
    setState(() => _isMuted = !_isMuted);
    final boostedVolume = (_volume * (_volumeBoost / 100.0)).clamp(0.0, 600.0);
    _player.setVolume(_isMuted ? 0 : boostedVolume);
    _showHud(_isMuted ? Icons.volume_off : Icons.volume_up, _isMuted ? "Muted" : "Unmuted");
    _resetHideControlsTimer();
  }

  Future<void> _toggleFullscreen() async {
    final currentMax = await windowManager.isFullScreen();
    if (currentMax) {
      await windowManager.setFullScreen(false);
      setState(() => _isFullscreen = false);
    } else {
      await windowManager.setFullScreen(true);
      setState(() => _isFullscreen = true);
    }
    _showHud(
      _isFullscreen ? Icons.fullscreen : Icons.fullscreen_exit,
      _isFullscreen ? "Fullscreen ON" : "Fullscreen OFF",
    );
  }

  Future<void> _takeScreenshot() async {
    try {
      final picturesDir = await getApplicationDocumentsDirectory();
      final zevixDir = Directory(p.join(picturesDir.parent.path, "Pictures", "ZevixPro"));
      if (!zevixDir.existsSync()) zevixDir.createSync(recursive: true);

      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final screenshotFile = File(p.join(zevixDir.path, "Zevix_${_currentVideo.id}_$timestamp.jpg"));

      final bytes = await _player.screenshot();
      if (bytes != null) {
        await screenshotFile.writeAsBytes(bytes);
        _showHud(Icons.camera_alt, "Screenshot saved to Pictures/ZevixPro");
      }
    } catch (_) {
      _showHud(Icons.error_outline, "Screenshot failed");
    }
  }

  void _cycleAspectRatio() {
    final nextIndex = (_aspectRatioMode.index + 1) % VideoAspectRatioMode.values.length;
    setState(() {
      _aspectRatioMode = VideoAspectRatioMode.values[nextIndex];
      if (_aspectRatioMode == VideoAspectRatioMode.fit || _aspectRatioMode == VideoAspectRatioMode.original) {
        _zoomMode = "Original";
      } else if (_aspectRatioMode == VideoAspectRatioMode.zoomCrop) {
        _zoomMode = "Fill";
      } else if (_aspectRatioMode == VideoAspectRatioMode.stretch) {
        _zoomMode = "Stretch";
      }
    });

    final names = {
      VideoAspectRatioMode.fit: "Fit to Screen",
      VideoAspectRatioMode.stretch: "Stretch",
      VideoAspectRatioMode.sixteenNine: "16:9 Cinema",
      VideoAspectRatioMode.fourThree: "4:3 Classic",
      VideoAspectRatioMode.zoomCrop: "Fill (Center Crop)",
      VideoAspectRatioMode.original: "Original",
    };

    _showHud(Icons.aspect_ratio, names[_aspectRatioMode] ?? "Aspect Ratio");
  }

  void _setZoomMode(String mode) {
    setState(() {
      _zoomMode = mode;
      if (mode == "Original") {
        _aspectRatioMode = VideoAspectRatioMode.fit;
      } else if (mode == "Fill") {
        _aspectRatioMode = VideoAspectRatioMode.zoomCrop;
      } else if (mode == "Stretch") {
        _aspectRatioMode = VideoAspectRatioMode.stretch;
      }
    });
    _showHud(Icons.aspect_ratio, "Zoom: $mode");
  }

  void _playNext() {
    if (widget.playlist.isEmpty) return;
    final nextIdx = (_currentIndex + 1) % widget.playlist.length;
    _loadVideo(widget.playlist[nextIdx]);
    _showHud(Icons.skip_next, "Next: ${_currentVideo.title}");
  }

  void _playPrevious() {
    if (widget.playlist.isEmpty) return;
    if (_position.inSeconds > 3) {
      _player.seek(Duration.zero);
      return;
    }
    final prevIdx = (_currentIndex - 1 + widget.playlist.length) % widget.playlist.length;
    _loadVideo(widget.playlist[prevIdx]);
    _showHud(Icons.skip_previous, "Previous: ${_currentVideo.title}");
  }

  void _toggleMenu(PlayerMenuPage page) {
    setState(() {
      if (_isMenuOpen && _menuPage == page) {
        _isMenuOpen = false;
      } else {
        _isMenuOpen = true;
        _menuPage = page;
      }
    });
    _resetHideControlsTimer();
  }

  Future<void> _pickAndLoadSubtitle() async {
    final files = await FilePicker.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['srt', 'vtt', 'ass', 'ssa', 'sub'],
    );
    if (files.isNotEmpty && files.first.path != null) {
      final track = SubtitleTrack.uri(files.first.path!, title: p.basename(files.first.path!));
      await _player.setSubtitleTrack(track);
      setState(() {
        _subtitlesEnabled = true;
        _selectedSubtitle = track;
      });
      _applyMpvSubtitles();
      _showHud(Icons.subtitles, "Loaded: ${p.basename(files.first.path!)}");
    }
  }

  void _setSleepTimer(int minutes, bool endOfShow) {
    _sleepTimer?.cancel();
    setState(() {
      _sleepMinutesRemaining = minutes;
      _sleepAtEndOfShow = endOfShow;
    });

    if (endOfShow) {
      _showHud(Icons.movie_outlined, "Sleep Timer: Stop at End of Show");
    } else if (minutes > 0) {
      _sleepTimer = Timer.periodic(const Duration(minutes: 1), (t) {
        if (!mounted) {
          t.cancel();
          return;
        }
        setState(() {
          _sleepMinutesRemaining--;
          if (_sleepMinutesRemaining <= 0) {
            t.cancel();
            _player.pause();
            _showHud(Icons.bedtime, "Sleep timer paused playback");
          }
        });
      });
      _showHud(Icons.timer, "Sleep timer set for $minutes minutes");
    } else {
      _showHud(Icons.timer_off, "Sleep timer disabled");
    }
  }

  String get _qualityLabel {
    final w = _player.state.videoParams.w ?? 0;
    final h = _player.state.videoParams.h ?? 0;
    if (w > 0 && h > 0) {
      final minDim = math.min(w, h);
      final maxDim = math.max(w, h);
      if (minDim >= 2100 || maxDim >= 3800) return "4K";
      if (minDim >= 1400 || maxDim >= 2500) return "1440p";
      if (minDim >= 1000 || maxDim >= 1900) return "1080p";
      if (minDim >= 700 || maxDim >= 1200) return "720p";
      if (minDim >= 450 || maxDim >= 800) return "480p";
      return "${minDim}p";
    }

    // Fallback based on video filename
    final lower = "${_currentVideo.title} ${_currentVideo.path}".toLowerCase();
    if (lower.contains("1080p") || lower.contains("fhd")) return "1080p";
    if (lower.contains("2160p") || lower.contains("4k") || lower.contains("uhd")) return "4K";
    if (lower.contains("1440p") || lower.contains("2k")) return "1440p";
    if (lower.contains("720p") || lower.contains("hd")) return "720p";
    if (lower.contains("480p") || lower.contains("sd")) return "480p";

    return "1080p";
  }

  String _formatEndTime(Duration position, Duration duration, double speed) {
    if (duration == Duration.zero) return "";
    final remaining = duration - position;
    if (remaining.isNegative) return "";
    final effectiveSpeed = speed > 0 ? speed : 1.0;
    final effectiveRemainingMs = (remaining.inMilliseconds / effectiveSpeed).round();
    final endTime = DateTime.now().add(Duration(milliseconds: effectiveRemainingMs));
    final hour = endTime.hour % 12 == 0 ? 12 : endTime.hour % 12;
    final minute = endTime.minute.toString().padLeft(2, '0');
    final ampm = endTime.hour >= 12 ? 'PM' : 'AM';
    return "$hour:$minute $ampm";
  }

  KeyEventResult _handleKeyEvent(FocusNode node, KeyEvent event) {
    final key = event.logicalKey;

    // Shift Key Hold: Instant 2x Fast-Forward Playback; release restores normal rate
    if (key == LogicalKeyboardKey.shiftLeft || key == LogicalKeyboardKey.shiftRight) {
      if (event is KeyDownEvent && !_isShiftBoostActive) {
        _isShiftBoostActive = true;
        _preShiftRate = _playbackSpeed;
        _player.setRate(2.0);
        setState(() => _playbackSpeed = 2.0);
        return KeyEventResult.handled;
      } else if (event is KeyUpEvent && _isShiftBoostActive) {
        _isShiftBoostActive = false;
        final targetRate = _preShiftRate > 0 ? _preShiftRate : 1.0;
        _player.setRate(targetRate);
        setState(() => _playbackSpeed = targetRate);
        return KeyEventResult.handled;
      }
    }

    if (event is! KeyDownEvent) return KeyEventResult.ignored;

    final isShift = HardwareKeyboard.instance.isShiftPressed;

    if (key == LogicalKeyboardKey.space || key == LogicalKeyboardKey.keyK) {
      _togglePlayPause();
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.arrowRight || key == LogicalKeyboardKey.keyL) {
      _seekRelative(isShift ? 10 : 5);
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.arrowLeft || key == LogicalKeyboardKey.keyJ) {
      _seekRelative(isShift ? -10 : -5);
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.arrowUp) {
      _adjustVolume(5);
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.arrowDown) {
      _adjustVolume(-5);
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.keyM) {
      _toggleMute();
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.keyF) {
      _toggleFullscreen();
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.keyS) {
      _takeScreenshot();
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.keyB) {
      _cycleAspectRatio();
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.keyN) {
      _playNext();
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.keyP) {
      _playPrevious();
      return KeyEventResult.handled;
    } else if (key == LogicalKeyboardKey.escape) {
      if (_isMenuOpen) {
        setState(() => _isMenuOpen = false);
        return KeyEventResult.handled;
      }
      if (_isFullscreen) {
        _toggleFullscreen();
      } else {
        _onExitPlayer();
      }
      return KeyEventResult.handled;
    }

    return KeyEventResult.ignored;
  }

  void _onExitPlayer() {
    if (_isFullscreen) {
      windowManager.setFullScreen(false);
    }
    widget.preferences.savePosition(_currentVideo.path, _position.inMilliseconds);
    Navigator.of(context).pop();
  }

  String _formatDuration(Duration d) {
    final hours = d.inHours;
    final minutes = d.inMinutes.remainder(60);
    final seconds = d.inSeconds.remainder(60);
    if (hours > 0) {
      return "${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
    }
    return "${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
  }

  @override
  void dispose() {
    windowManager.removeListener(this);
    _hideControlsTimer?.cancel();
    _sleepTimer?.cancel();
    _posSub?.cancel();
    _durSub?.cancel();
    _bufSub?.cancel();
    _playSub?.cancel();
    _compSub?.cancel();
    _tracksSub?.cancel();
    _trackSub?.cancel();
    _videoParamsSub?.cancel();
    _positionNotifier.dispose();
    _bufferNotifier.dispose();
    _player.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Focus(
      focusNode: _focusNode,
      autofocus: true,
      onKeyEvent: _handleKeyEvent,
      child: Scaffold(
        backgroundColor: Colors.black,
        body: Column(
          children: [
            // Top Green Banner: Shows only "Zevix Pro" without film title
            if (!_isFullscreen)
              const WindowTitleBar(
                title: "Zevix Pro",
                isGreenBanner: true,
              ),

            // Video Player Area
            Expanded(
              child: MouseRegion(
                onHover: (_) => _resetHideControlsTimer(),
                onExit: (_) {
                  if (_isPlaying && mounted && !_isMenuOpen) {
                    setState(() => _showControls = false);
                  }
                },
                child: GestureDetector(
                  onTap: () {
                    if (_isMenuOpen) {
                      setState(() => _isMenuOpen = false);
                      _resetHideControlsTimer();
                    } else {
                      _togglePlayPause();
                    }
                  },
                  onDoubleTap: _toggleFullscreen,
                  child: Listener(
                    onPointerSignal: (pointerSignal) {
                      if (pointerSignal is PointerScrollEvent) {
                        // Do NOT adjust volume when settings drawer is open so user can scroll settings!
                        if (_isMenuOpen) return;
                        if (pointerSignal.scrollDelta.dy < 0) {
                          _adjustVolume(3);
                        } else if (pointerSignal.scrollDelta.dy > 0) {
                          _adjustVolume(-3);
                        }
                      }
                    },
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        // Video Viewport with Real-Time GPU Color Correction
                        Center(
                          child: ColorFiltered(
                            colorFilter: ColorFilter.matrix(
                              ColorFilterUtils.calculateColorMatrix(
                                brightness: _brightness + (_quickBrightness - 100.0),
                                contrast: _contrast,
                                saturation: _saturation,
                                hueDegrees: _hue,
                              ),
                            ),
                            child: _buildVideoViewport(),
                          ),
                        ),

                        // Overlays (Top title bar & Bottom playback controls) matching sample screenshot
                        AnimatedOpacity(
                          opacity: _showControls || _isMenuOpen ? 1.0 : 0.0,
                          duration: const Duration(milliseconds: 220),
                          child: IgnorePointer(
                            ignoring: !_showControls && !_isMenuOpen,
                            child: Stack(
                              children: [
                                // Top Overlay: Back Button, Centered Movie Title, Cast Button
                                Positioned(
                                  top: 0,
                                  left: 0,
                                  right: 0,
                                  child: _buildTopOverlay(),
                                ),

                                // Floating Options Drawer (Matches Screen Recording)
                                if (_isMenuOpen)
                                  Positioned(
                                    bottom: 76,
                                    right: 16,
                                    child: RepaintBoundary(
                                      child: PlayerControlsMenu(
                                        player: _player,
                                      initialPage: _menuPage,
                                      qualityLabel: _qualityLabel,
                                      onClose: () => setState(() => _isMenuOpen = false),
                                      onUploadSubtitle: _pickAndLoadSubtitle,
                                      // Playback Speed
                                      playbackSpeed: _playbackSpeed,
                                      onSpeedChanged: (speed) {
                                        setState(() => _playbackSpeed = speed);
                                        _player.setRate(speed);
                                        _showHud(Icons.speed, "Speed: ${speed}x");
                                      },
                                      // Zoom
                                      zoomMode: _zoomMode,
                                      onZoomChanged: _setZoomMode,
                                      // Volume Boost
                                      volumeBoost: _volumeBoost,
                                      onVolumeBoostChanged: (boost) {
                                        setState(() => _volumeBoost = boost);
                                        _setMpvProperty('volume-max', '600');
                                        final boosted = (_volume * (_volumeBoost / 100.0)).clamp(0.0, 600.0);
                                        _player.setVolume(boosted);
                                      },
                                      // Quick Brightness
                                      quickBrightness: _quickBrightness,
                                      onQuickBrightnessChanged: (val) {
                                        setState(() => _quickBrightness = val);
                                        _applyMpvColors();
                                      },
                                      // Colors
                                      brightness: _brightness,
                                      onBrightnessChanged: (val) {
                                        setState(() => _brightness = val);
                                        _applyMpvColors();
                                      },
                                      contrast: _contrast,
                                      onContrastChanged: (val) {
                                        setState(() => _contrast = val);
                                        _applyMpvColors();
                                      },
                                      saturation: _saturation,
                                      onSaturationChanged: (val) {
                                        setState(() => _saturation = val);
                                        _applyMpvColors();
                                      },
                                      hue: _hue,
                                      onHueChanged: (val) {
                                        setState(() => _hue = val);
                                        _applyMpvColors();
                                      },
                                      onResetColors: () {
                                        setState(() {
                                          _brightness = 100.0;
                                          _contrast = 100.0;
                                          _saturation = 100.0;
                                          _hue = 0.0;
                                          _quickBrightness = 100.0;
                                        });
                                        _applyMpvColors();
                                        _showHud(Icons.restart_alt, "Colors reset to defaults");
                                      },
                                      // Subtitles
                                      subtitlesEnabled: _subtitlesEnabled,
                                      onToggleSubtitles: (enabled) {
                                        setState(() => _subtitlesEnabled = enabled);
                                        if (!enabled) {
                                          _player.setSubtitleTrack(SubtitleTrack.no());
                                        } else {
                                          if (_selectedSubtitle.id == 'no') {
                                            final available = _player.state.tracks.subtitle.where((t) => t.id != 'no').toList();
                                            if (available.isNotEmpty) {
                                              _selectedSubtitle = available.first;
                                            } else {
                                              _selectedSubtitle = SubtitleTrack.auto();
                                            }
                                          }
                                          _player.setSubtitleTrack(_selectedSubtitle);
                                        }
                                        _applyMpvSubtitles();
                                        _showHud(
                                          enabled ? Icons.subtitles : Icons.subtitles_off,
                                          enabled ? "Subtitles ON" : "Subtitles OFF",
                                        );
                                      },
                                      subtitleTracks: _player.state.tracks.subtitle,
                                      selectedSubtitleTrack: _selectedSubtitle,
                                      onSelectSubtitleTrack: (track) {
                                        _player.setSubtitleTrack(track);
                                        setState(() {
                                          _selectedSubtitle = track;
                                          _subtitlesEnabled = track.id != 'no';
                                        });
                                        _applyMpvSubtitles();
                                        _showHud(Icons.subtitles, "Subtitle: ${track.title ?? track.language ?? track.id}");
                                      },
                                      useNativeSubtitles: _useNativeSubtitles,
                                      onToggleNativeSubtitles: (val) {
                                        setState(() => _useNativeSubtitles = val);
                                        _showHud(Icons.subtitles, val ? "Native subtitles engine" : "Custom subtitle renderer");
                                      },
                                      pastelMode: _pastelMode,
                                      onTogglePastelMode: (val) => setState(() => _pastelMode = val),
                                      subtitleDelay: _subtitleDelay,
                                      onSubtitleDelayChanged: (val) {
                                        setState(() => _subtitleDelay = val);
                                        _applyMpvSubtitles();
                                        _showHud(Icons.timer, "Subtitle delay: ${val >= 0 ? '+' : ''}${val.toStringAsFixed(1)}s");
                                      },
                                      subtitleFont: _subtitleFont,
                                      onSubtitleFontChanged: (font) {
                                        setState(() => _subtitleFont = font);
                                        _applyMpvSubtitles();
                                      },
                                      subtitleSize: _subtitleSize,
                                      onSubtitleSizeChanged: (size) {
                                        setState(() => _subtitleSize = size);
                                        _applyMpvSubtitles();
                                      },
                                      subtitlePosition: _subtitlePosition,
                                      onSubtitlePositionChanged: (pos) {
                                        setState(() => _subtitlePosition = pos);
                                        _applyMpvSubtitles();
                                      },
                                      subtitleBackground: _subtitleBackground,
                                      onToggleSubtitleBackground: (bg) {
                                        setState(() => _subtitleBackground = bg);
                                        _applyMpvSubtitles();
                                      },
                                      subtitleColor: _subtitleColor,
                                      onSubtitleColorChanged: (c) {
                                        setState(() => _subtitleColor = c);
                                        _applyMpvSubtitles();
                                      },
                                      // Audio Tracks
                                      audioTracks: _player.state.tracks.audio,
                                      selectedAudioTrack: _selectedAudio,
                                      onSelectAudioTrack: (t) {
                                        _player.setAudioTrack(t);
                                        setState(() => _selectedAudio = t);
                                        _showHud(Icons.audiotrack, "Audio: ${t.title ?? t.language ?? t.id}");
                                      },
                                      // Sleep Timer
                                      sleepMinutesRemaining: _sleepMinutesRemaining,
                                      sleepAtEndOfShow: _sleepAtEndOfShow,
                                      onSetSleepTimer: _setSleepTimer,
                                    ),
                                  ),
                                ),

                                // Bottom Overlay: Seekbar + Control buttons with End-Time display
                                Positioned(
                                  bottom: 0,
                                  left: 0,
                                  right: 0,
                                  child: _buildBottomOverlay(),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // Passing controls: NoVideoControls prevents media_kit from injecting its default seekbar!
  // Disabling subtitleViewConfiguration removes duplicate Flutter subtitle overlay (MPV renders subtitles directly).
  Widget _buildVideoViewport() {
    BoxFit fit = BoxFit.contain;
    switch (_aspectRatioMode) {
      case VideoAspectRatioMode.fit:
        fit = BoxFit.contain;
        break;
      case VideoAspectRatioMode.stretch:
        fit = BoxFit.fill;
        break;
      case VideoAspectRatioMode.sixteenNine:
      case VideoAspectRatioMode.fourThree:
      case VideoAspectRatioMode.zoomCrop:
        fit = BoxFit.cover;
        break;
      case VideoAspectRatioMode.original:
        fit = BoxFit.none;
        break;
    }

    Widget videoWidget = Video(
      controller: _videoController,
      fit: fit,
      controls: NoVideoControls,
      subtitleViewConfiguration: const SubtitleViewConfiguration(visible: false),
    );

    if (_aspectRatioMode == VideoAspectRatioMode.sixteenNine) {
      videoWidget = AspectRatio(aspectRatio: 16 / 9, child: videoWidget);
    } else if (_aspectRatioMode == VideoAspectRatioMode.fourThree) {
      videoWidget = AspectRatio(aspectRatio: 4 / 3, child: videoWidget);
    }

    return RepaintBoundary(child: videoWidget);
  }

  // Top Overlay matching sample image: Left Arrow, Centered Title, Right Cast
  Widget _buildTopOverlay() {
    return Container(
      height: 64,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Colors.black87, Colors.transparent],
        ),
      ),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white, size: 24),
            tooltip: "Back (Esc)",
            onPressed: _onExitPlayer,
          ),
          const Spacer(),
          IconButton(
            icon: const Icon(Icons.cast, color: Colors.white, size: 22),
            tooltip: "Cast Screen",
            onPressed: () {
              _showHud(Icons.cast, "Scanning for Cast / Smart TV devices...");
            },
          ),
        ],
      ),
    );
  }

  // Bottom Overlay matching screen recording:
  // Seek bar on top, row of controls below it with dynamic Show End Time!
  Widget _buildBottomOverlay() {
    return ValueListenableBuilder<Duration>(
      valueListenable: _positionNotifier,
      builder: (context, pos, _) {
        final activePosition = _isDraggingSeek
            ? Duration(milliseconds: (_dragSeekPercent * _duration.inMilliseconds).toInt())
            : pos;

        final posText = _formatDuration(activePosition);
        final durText = _formatDuration(_duration);
        final remaining = _duration - activePosition;
        final remText = _formatDuration(remaining.isNegative ? Duration.zero : remaining);

        final progress = _duration.inMilliseconds > 0
            ? (activePosition.inMilliseconds / _duration.inMilliseconds).clamp(0.0, 1.0)
            : 0.0;
        final bufferProgress = _duration.inMilliseconds > 0
            ? (_buffer.inMilliseconds / _duration.inMilliseconds).clamp(0.0, 1.0)
            : 0.0;

        final endTimeStr = _formatEndTime(activePosition, _duration, _playbackSpeed);

        return Container(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.bottomCenter,
              end: Alignment.topCenter,
              colors: [Colors.black87, Colors.transparent],
            ),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // 1. Sleek Full-Width Custom Seek Bar (matching video)
              LayoutBuilder(
                builder: (context, constraints) {
                  final barWidth = constraints.maxWidth;
                  return GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onHorizontalDragStart: (details) {
                      setState(() {
                        _isDraggingSeek = true;
                        _dragSeekPercent = (details.localPosition.dx / barWidth).clamp(0.0, 1.0);
                      });
                    },
                    onHorizontalDragUpdate: (details) {
                      setState(() {
                        _dragSeekPercent = (details.localPosition.dx / barWidth).clamp(0.0, 1.0);
                      });
                    },
                    onHorizontalDragEnd: (details) {
                      final target = Duration(milliseconds: (_dragSeekPercent * _duration.inMilliseconds).toInt());
                      _player.seek(target);
                      setState(() {
                        _isDraggingSeek = false;
                        _position = target;
                      });
                      _positionNotifier.value = target;
                      _resetHideControlsTimer();
                    },
                    onTapDown: (details) {
                      final pct = (details.localPosition.dx / barWidth).clamp(0.0, 1.0);
                      final target = Duration(milliseconds: (pct * _duration.inMilliseconds).toInt());
                      _player.seek(target);
                      setState(() => _position = target);
                      _positionNotifier.value = target;
                      _resetHideControlsTimer();
                    },
                child: SizedBox(
                  height: 18,
                  child: Stack(
                    alignment: Alignment.centerLeft,
                    children: [
                      // Base unplayed track (Dark Grey)
                      Container(
                        height: 3.5,
                        width: double.infinity,
                        decoration: BoxDecoration(
                          color: const Color(0xFF333333),
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                      // Buffered track (Translucent White)
                      Container(
                        height: 3.5,
                        width: barWidth * bufferProgress,
                        decoration: BoxDecoration(
                          color: const Color(0x66FFFFFF),
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                      // Played track (Vibrant Light Green)
                      Container(
                        height: 3.5,
                        width: barWidth * progress,
                        decoration: BoxDecoration(
                          color: accentLimeGreen,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                      // Circular indicator at head
                      Positioned(
                        left: (barWidth * progress - 5).clamp(0.0, barWidth - 10),
                        child: Container(
                          width: 10,
                          height: 10,
                          decoration: const BoxDecoration(
                            color: accentLimeGreen,
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: Color(0x6695FF50),
                                blurRadius: 4,
                                spreadRadius: 1,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),

          const SizedBox(height: 4),

          // 2. Control Buttons Row with Dynamic End-Time Display
          Row(
            children: [
              // Play / Pause Button (White solid icon)
              IconButton(
                icon: Icon(
                  _isPlaying ? Icons.pause : Icons.play_arrow,
                  color: Colors.white,
                  size: 28,
                ),
                tooltip: "Play / Pause (Space)",
                onPressed: _togglePlayPause,
              ),

              // Replay 10s Button
              IconButton(
                icon: const Icon(Icons.replay_10, color: Colors.white, size: 23),
                tooltip: "Rewind 10s (Left Arrow)",
                onPressed: () => _seekRelative(-10),
              ),

              // Forward 10s Button
              IconButton(
                icon: const Icon(Icons.forward_10, color: Colors.white, size: 23),
                tooltip: "Forward 10s (Right Arrow)",
                onPressed: () => _seekRelative(10),
              ),

              const SizedBox(width: 4),

              // Volume Button & Sleek Slider
              IconButton(
                icon: Icon(
                  _isMuted || _volume == 0
                      ? Icons.volume_off
                      : (_volume < 50 ? Icons.volume_down : Icons.volume_up),
                  color: Colors.white,
                  size: 22,
                ),
                tooltip: "Mute / Unmute (M)",
                onPressed: _toggleMute,
              ),
              SizedBox(
                width: 76,
                child: SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    trackHeight: 3,
                    thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 5),
                    overlayShape: const RoundSliderOverlayShape(overlayRadius: 10),
                  ),
                  child: Slider(
                    value: _isMuted ? 0 : _volume.clamp(0.0, 100.0),
                    min: 0,
                    max: 100,
                    activeColor: accentLimeGreen,
                    inactiveColor: Colors.white24,
                    onChanged: (val) {
                      setState(() {
                        _volume = val;
                        _isMuted = false;
                      });
                      final boosted = (_volume * (_volumeBoost / 100.0)).clamp(0.0, 600.0);
                      _player.setVolume(boosted);
                    },
                  ),
                ),
              ),

              const SizedBox(width: 10),

              // Duration & Dynamic End-Time Indicator (Matches User Request)
              InkWell(
                onTap: () {
                  setState(() => _showRemainingTime = !_showRemainingTime);
                },
                borderRadius: BorderRadius.circular(4),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                  child: Text(
                    _showRemainingTime ? "$posText / -$remText" : "$posText / $durText",
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ),

              // End of Show Badge (Automatically adjusts to show length & speed!)
              if (_duration.inSeconds > 0 && endTimeStr.isNotEmpty) ...[
                const SizedBox(width: 8),
                Tooltip(
                  message: "Estimated end time of show (calculated with current playback speed)",
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.12), width: 0.8),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.schedule, size: 12, color: accentLimeGreen),
                        const SizedBox(width: 4),
                        Text(
                          "Ends at $endTimeStr",
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.85),
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],

              const Spacer(),

              // Settings Gear Button (Opens/Toggles main floating options drawer)
              IconButton(
                icon: Icon(
                  Icons.settings,
                  color: _isMenuOpen ? accentLimeGreen : Colors.white,
                  size: 21,
                ),
                tooltip: "Playback Settings",
                onPressed: () => _toggleMenu(PlayerMenuPage.main),
              ),

              // Fullscreen Toggle Button (Corners expand icon)
              IconButton(
                icon: Icon(
                  _isFullscreen ? Icons.fullscreen_exit : Icons.fullscreen,
                  color: Colors.white,
                  size: 24,
                ),
                tooltip: "Fullscreen (F)",
                onPressed: _toggleFullscreen,
              ),
            ],
          ),
        ],
      ),
    );
      },
    );
  }
}
