import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:media_kit/media_kit.dart';
import '../models/music_item.dart';
import '../services/media_scanner_service.dart';
import '../services/playback_preferences.dart';

class MusicProvider extends ChangeNotifier {
  final PlaybackPreferences preferences;
  late final MediaScannerService scanner;
  late final Player _player;

  List<MusicItem> _allTracks = [];
  List<MusicItem> _queue = [];
  int _currentIndex = -1;
  bool _isPlaying = false;
  bool _isLoading = false;
  Duration _position = Duration.zero;
  Duration _duration = Duration.zero;
  bool _isShuffle = false;
  bool _isRepeat = false;

  StreamSubscription? _posSub;
  StreamSubscription? _durSub;
  StreamSubscription? _playSub;
  StreamSubscription? _compSub;

  MusicProvider({required this.preferences}) {
    scanner = MediaScannerService(preferences);
    _player = Player(configuration: const PlayerConfiguration());

    _posSub = _player.stream.position.listen((pos) {
      _position = pos;
      notifyListeners();
    });

    _durSub = _player.stream.duration.listen((dur) {
      _duration = dur;
      notifyListeners();
    });

    _playSub = _player.stream.playing.listen((playing) {
      _isPlaying = playing;
      notifyListeners();
    });

    _compSub = _player.stream.completed.listen((completed) {
      if (completed) {
        if (_isRepeat) {
          _player.seek(Duration.zero);
          _player.play();
        } else {
          playNext();
        }
      }
    });

    loadTracks();
  }

  List<MusicItem> get allTracks => _allTracks;
  List<MusicItem> get queue => _queue;
  MusicItem? get currentTrack => (_currentIndex >= 0 && _currentIndex < _queue.length) ? _queue[_currentIndex] : null;
  bool get isPlaying => _isPlaying;
  bool get isLoading => _isLoading;
  Duration get position => _position;
  Duration get duration => _duration;
  bool get isShuffle => _isShuffle;
  bool get isRepeat => _isRepeat;
  Player get player => _player;

  Future<void> loadTracks() async {
    _isLoading = true;
    notifyListeners();

    try {
      _allTracks = await scanner.scanMusic();
    } catch (_) {}

    _isLoading = false;
    notifyListeners();
  }

  Future<void> playTrack(MusicItem track, {List<MusicItem>? fromList}) async {
    if (fromList != null) {
      _queue = List<MusicItem>.from(fromList);
      _currentIndex = _queue.indexWhere((m) => m.path == track.path);
    } else {
      if (!_queue.any((m) => m.path == track.path)) {
        _queue.add(track);
      }
      _currentIndex = _queue.indexWhere((m) => m.path == track.path);
    }

    if (_currentIndex < 0) _currentIndex = 0;

    await _player.open(Media(track.path));
    await _player.play();
    notifyListeners();
  }

  Future<void> togglePlayPause() async {
    await _player.playOrPause();
  }

  Future<void> seek(Duration pos) async {
    await _player.seek(pos);
  }

  Future<void> playNext() async {
    if (_queue.isEmpty) return;
    if (_isShuffle) {
      _currentIndex = (DateTime.now().millisecond % _queue.length);
    } else {
      _currentIndex = (_currentIndex + 1) % _queue.length;
    }
    final next = _queue[_currentIndex];
    await _player.open(Media(next.path));
    await _player.play();
    notifyListeners();
  }

  Future<void> playPrevious() async {
    if (_queue.isEmpty) return;
    if (_position.inSeconds > 3) {
      await _player.seek(Duration.zero);
      return;
    }
    _currentIndex = (_currentIndex - 1 + _queue.length) % _queue.length;
    final prev = _queue[_currentIndex];
    await _player.open(Media(prev.path));
    await _player.play();
    notifyListeners();
  }

  void toggleShuffle() {
    _isShuffle = !_isShuffle;
    notifyListeners();
  }

  void toggleRepeat() {
    _isRepeat = !_isRepeat;
    notifyListeners();
  }

  Future<void> stop() async {
    await _player.stop();
    _currentIndex = -1;
    notifyListeners();
  }

  @override
  void dispose() {
    _posSub?.cancel();
    _durSub?.cancel();
    _playSub?.cancel();
    _compSub?.cancel();
    _player.dispose();
    super.dispose();
  }
}

