import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:media_kit/media_kit.dart';

enum PlayerMenuPage {
  main,
  subtitles,
  subtitleSettings,
  audio,
  playback,
  modifyColors,
  sleepTimer,
}

class PlayerControlsMenu extends StatefulWidget {
  final Player player;
  final PlayerMenuPage initialPage;
  final VoidCallback onClose;
  final VoidCallback onUploadSubtitle;

  // Playback & Zoom
  final double playbackSpeed;
  final ValueChanged<double> onSpeedChanged;
  final String zoomMode;
  final ValueChanged<String> onZoomChanged;
  final double volumeBoost;
  final ValueChanged<double> onVolumeBoostChanged;
  final double quickBrightness;
  final ValueChanged<double> onQuickBrightnessChanged;

  // Colors
  final double brightness;
  final ValueChanged<double> onBrightnessChanged;
  final double contrast;
  final ValueChanged<double> onContrastChanged;
  final double saturation;
  final ValueChanged<double> onSaturationChanged;
  final double hue;
  final ValueChanged<double> onHueChanged;
  final VoidCallback onResetColors;

  // Subtitles
  final bool subtitlesEnabled;
  final ValueChanged<bool> onToggleSubtitles;
  final List<SubtitleTrack> subtitleTracks;
  final SubtitleTrack selectedSubtitleTrack;
  final ValueChanged<SubtitleTrack> onSelectSubtitleTrack;

  final bool useNativeSubtitles;
  final ValueChanged<bool> onToggleNativeSubtitles;
  final bool pastelMode;
  final ValueChanged<bool> onTogglePastelMode;
  final double subtitleDelay;
  final ValueChanged<double> onSubtitleDelayChanged;
  final String subtitleFont;
  final ValueChanged<String> onSubtitleFontChanged;
  final double subtitleSize;
  final ValueChanged<double> onSubtitleSizeChanged;
  final double subtitlePosition;
  final ValueChanged<double> onSubtitlePositionChanged;
  final bool subtitleBackground;
  final ValueChanged<bool> onToggleSubtitleBackground;
  final Color subtitleColor;
  final ValueChanged<Color> onSubtitleColorChanged;

  // Audio Tracks
  final List<AudioTrack> audioTracks;
  final AudioTrack selectedAudioTrack;
  final ValueChanged<AudioTrack> onSelectAudioTrack;

  // Sleep Timer
  final int sleepMinutesRemaining;
  final bool sleepAtEndOfShow;
  final void Function(int minutes, bool endOfShow) onSetSleepTimer;

  // Quality label
  final String qualityLabel;

  const PlayerControlsMenu({
    super.key,
    required this.player,
    this.initialPage = PlayerMenuPage.main,
    required this.onClose,
    required this.onUploadSubtitle,
    required this.playbackSpeed,
    required this.onSpeedChanged,
    required this.zoomMode,
    required this.onZoomChanged,
    required this.volumeBoost,
    required this.onVolumeBoostChanged,
    required this.quickBrightness,
    required this.onQuickBrightnessChanged,
    required this.brightness,
    required this.onBrightnessChanged,
    required this.contrast,
    required this.onContrastChanged,
    required this.saturation,
    required this.onSaturationChanged,
    required this.hue,
    required this.onHueChanged,
    required this.onResetColors,
    required this.subtitlesEnabled,
    required this.onToggleSubtitles,
    required this.subtitleTracks,
    required this.selectedSubtitleTrack,
    required this.onSelectSubtitleTrack,
    required this.useNativeSubtitles,
    required this.onToggleNativeSubtitles,
    required this.pastelMode,
    required this.onTogglePastelMode,
    required this.subtitleDelay,
    required this.onSubtitleDelayChanged,
    required this.subtitleFont,
    required this.onSubtitleFontChanged,
    required this.subtitleSize,
    required this.onSubtitleSizeChanged,
    required this.subtitlePosition,
    required this.onSubtitlePositionChanged,
    required this.subtitleBackground,
    required this.onToggleSubtitleBackground,
    required this.subtitleColor,
    required this.onSubtitleColorChanged,
    required this.audioTracks,
    required this.selectedAudioTrack,
    required this.onSelectAudioTrack,
    required this.sleepMinutesRemaining,
    required this.sleepAtEndOfShow,
    required this.onSetSleepTimer,
    required this.qualityLabel,
  });

  @override
  State<PlayerControlsMenu> createState() => _PlayerControlsMenuState();
}

class _PlayerControlsMenuState extends State<PlayerControlsMenu> {
  late PlayerMenuPage _currentPage;
  final TextEditingController _subtitleSearchController = TextEditingController();
  String _subtitleSearchQuery = "";

  // Bright Lime Green (#95FF50, RGB: 149, 255, 80)
  static const Color accentLimeGreen = Color(0xFF95FF50);
  // Glassmorphic translucent cards matching Image 2
  static const Color cardBg = Color(0x382C2C36);
  static const Color cardBgSelected = Color(0x55404050);
  static const Color textGrey = Color(0xFF8E8E93);

  @override
  void initState() {
    super.initState();
    _currentPage = widget.initialPage;
  }

  void _navigateTo(PlayerMenuPage page) {
    setState(() => _currentPage = page);
  }

  void _navigateBack() {
    if (_currentPage == PlayerMenuPage.subtitleSettings &&
        widget.initialPage == PlayerMenuPage.subtitles) {
      setState(() => _currentPage = PlayerMenuPage.subtitles);
    } else {
      setState(() => _currentPage = PlayerMenuPage.main);
    }
  }

  @override
  void dispose() {
    _subtitleSearchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Listener(
      // Absorb scroll events inside settings menu so they do NOT affect player volume
      behavior: HitTestBehavior.opaque,
      onPointerSignal: (_) {},
      child: Container(
        width: 350,
        constraints: const BoxConstraints(maxHeight: 520),
        decoration: BoxDecoration(
          color: const Color(0xF2141419), // Ultra-fast GPU-accelerated dark glass (lag-free)
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: Colors.white.withValues(alpha: 0.14),
            width: 1.0,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.7),
              blurRadius: 36,
              spreadRadius: 2,
              offset: const Offset(0, 12),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header (visible when on sub-pages)
            if (_currentPage != PlayerMenuPage.main) _buildHeader(),

            // Page content (smooth scrollable)
            Flexible(
              child: SingleChildScrollView(
                physics: const ClampingScrollPhysics(),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                child: _buildCurrentPage(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    String title = "";
    Widget? trailing;

    switch (_currentPage) {
      case PlayerMenuPage.subtitles:
        title = "Subtitles";
        trailing = GestureDetector(
          onTap: () => _navigateTo(PlayerMenuPage.subtitleSettings),
          child: const Padding(
            padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            child: Text(
              "Customize",
              style: TextStyle(
                color: Colors.white,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        );
        break;
      case PlayerMenuPage.subtitleSettings:
        title = "Subtitle settings";
        break;
      case PlayerMenuPage.audio:
        title = "Audio";
        break;
      case PlayerMenuPage.playback:
        title = "Playback";
        break;
      case PlayerMenuPage.modifyColors:
        title = "Modify colors";
        trailing = GestureDetector(
          onTap: widget.onResetColors,
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.restart_alt, color: Colors.white70, size: 16),
              SizedBox(width: 4),
              Text(
                "Reset",
                style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
              ),
            ],
          ),
        );
        break;
      case PlayerMenuPage.sleepTimer:
        title = "Sleep Timer";
        break;
      case PlayerMenuPage.main:
        break;
    }

    return Container(
      padding: const EdgeInsets.fromLTRB(8, 12, 16, 8),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: Colors.white.withValues(alpha: 0.08))),
      ),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white, size: 20),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
            onPressed: _navigateBack,
          ),
          const SizedBox(width: 4),
          Expanded(
            child: Text(
              title,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }

  Widget _buildCurrentPage() {
    switch (_currentPage) {
      case PlayerMenuPage.main:
        return _buildMainPage();
      case PlayerMenuPage.subtitles:
        return _buildSubtitlesPage();
      case PlayerMenuPage.subtitleSettings:
        return _buildSubtitleSettingsPage();
      case PlayerMenuPage.audio:
        return _buildAudioPage();
      case PlayerMenuPage.playback:
        return _buildPlaybackPage();
      case PlayerMenuPage.modifyColors:
        return _buildModifyColorsPage();
      case PlayerMenuPage.sleepTimer:
        return _buildSleepTimerPage();
    }
  }

  // -------------------------------------------------------------
  // 1. MAIN PAGE (Quality, Subtitles, Audio & Player Settings)
  // -------------------------------------------------------------
  Widget _buildMainPage() {
    final subName = !widget.subtitlesEnabled
        ? "Off"
        : (widget.selectedSubtitleTrack.title ??
            widget.selectedSubtitleTrack.language ??
            (widget.subtitleTracks.isNotEmpty ? "English" : "Off"));

    final audioName = widget.selectedAudioTrack.title ??
        widget.selectedAudioTrack.language ??
        (widget.audioTracks.isNotEmpty ? "ENGLISH 7.1" : "Stereo");

    final qualityText = widget.qualityLabel.isNotEmpty ? widget.qualityLabel : "1080p";

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Single row of 3 essential media cards (Quality, Subtitles, Audio)
        Row(
          children: [
            // Card 1: QUALITY (Non-clickable, dynamic display only)
            Expanded(
              child: _buildQualityCard(value: qualityText),
            ),
            const SizedBox(width: 8),
            // Card 2: SUBTITLES
            Expanded(
              child: _buildGridCard(
                leadingWidget: _buildBadge("CC"),
                title: "SUBTITLES",
                value: subName,
                onTap: () => _navigateTo(PlayerMenuPage.subtitles),
              ),
            ),
            const SizedBox(width: 8),
            // Card 3: AUDIO
            Expanded(
              child: _buildGridCard(
                icon: Icons.music_note,
                title: "AUDIO",
                value: audioName,
                onTap: () => _navigateTo(PlayerMenuPage.audio),
              ),
            ),
          ],
        ),

        const SizedBox(height: 12),

        // List Item 1: Sleep Timer
        _buildListTile(
          icon: Icons.bedtime,
          title: "Sleep Timer",
          trailingText: widget.sleepAtEndOfShow
              ? "End of Show"
              : (widget.sleepMinutesRemaining > 0 ? "${widget.sleepMinutesRemaining}m" : null),
          onTap: () => _navigateTo(PlayerMenuPage.sleepTimer),
        ),

        const SizedBox(height: 6),
        Divider(color: Colors.white.withValues(alpha: 0.08), height: 1),
        const SizedBox(height: 6),

        // List Item 2: Enable Subtitles (Toggle Switch)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 4),
          child: Row(
            children: [
              const SizedBox(
                width: 24,
                child: Icon(Icons.subtitles, color: Colors.white, size: 20),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text(
                  "Enable Subtitles",
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              CupertinoSwitch(
                value: widget.subtitlesEnabled,
                activeTrackColor: accentLimeGreen,
                inactiveTrackColor: const Color(0xFF333338),
                onChanged: widget.onToggleSubtitles,
              ),
            ],
          ),
        ),

        // List Item 3: Playback Settings
        _buildListTile(
          icon: Icons.speed,
          title: "Playback Settings",
          onTap: () => _navigateTo(PlayerMenuPage.playback),
        ),

        // List Item 4: Subtitle Settings
        _buildListTile(
          leadingWidget: _buildTtIcon(),
          title: "Subtitle Settings",
          onTap: () => _navigateTo(PlayerMenuPage.subtitleSettings),
        ),
      ],
    );
  }

  Widget _buildBadge(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 1),
      decoration: BoxDecoration(
        border: Border.all(color: textGrey, width: 1.2),
        borderRadius: BorderRadius.circular(2.5),
      ),
      child: Text(
        text,
        style: const TextStyle(
          color: textGrey,
          fontSize: 8.5,
          fontWeight: FontWeight.w800,
          letterSpacing: 0.5,
          height: 1.0,
        ),
      ),
    );
  }

  Widget _buildTtIcon() {
    return SizedBox(
      width: 20,
      height: 20,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.baseline,
        textBaseline: TextBaseline.alphabetic,
        children: const [
          Text(
            'T',
            style: TextStyle(
              color: Colors.white,
              fontSize: 15,
              fontWeight: FontWeight.bold,
              height: 1.0,
            ),
          ),
          SizedBox(width: 1),
          Text(
            't',
            style: TextStyle(
              color: Colors.white,
              fontSize: 11,
              fontWeight: FontWeight.bold,
              height: 1.0,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGridCard({
    Widget? leadingWidget,
    IconData? icon,
    required String title,
    required String value,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: cardBg,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  leadingWidget ?? Icon(icon, size: 14, color: textGrey),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      title,
                      style: const TextStyle(
                        color: textGrey,
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0.5,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                value,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildListTile({
    Widget? leadingWidget,
    IconData? icon,
    required String title,
    String? trailingText,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 9, horizontal: 4),
          child: Row(
            children: [
              SizedBox(
                width: 24,
                child: leadingWidget ?? Icon(icon, color: Colors.white, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              if (trailingText != null) ...[
                Text(
                  trailingText,
                  style: const TextStyle(color: accentLimeGreen, fontSize: 13, fontWeight: FontWeight.bold),
                ),
                const SizedBox(width: 4),
              ],
              const Icon(Icons.chevron_right, color: Colors.white, size: 18),
            ],
          ),
        ),
      ),
    );
  }



  // Non-clickable dynamic quality display badge
  Widget _buildQualityCard({
    required String value,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _buildBadge("HQ"),
              const SizedBox(width: 6),
              const Expanded(
                child: Text(
                  "QUALITY",
                  style: TextStyle(
                    color: textGrey,
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.5,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 14,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // 2. SUBTITLES PAGE (Matches frame_007.png, frame_008.png)
  // -------------------------------------------------------------
  Widget _buildSubtitlesPage() {
    final filtered = widget.subtitleTracks.where((t) {
      if (_subtitleSearchQuery.isEmpty) return true;
      final name = (t.title ?? t.language ?? "Track ${t.id}").toLowerCase();
      return name.contains(_subtitleSearchQuery.toLowerCase());
    }).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Off Option
        _buildTrackOptionRow(
          title: "Off",
          isSelected: !widget.subtitlesEnabled || widget.selectedSubtitleTrack.id == 'no',
          onTap: () {
            widget.onToggleSubtitles(false);
            widget.onSelectSubtitleTrack(SubtitleTrack.no());
          },
        ),

        // Upload subtitle file
        Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: widget.onUploadSubtitle,
            borderRadius: BorderRadius.circular(8),
            child: const Padding(
              padding: EdgeInsets.symmetric(vertical: 11, horizontal: 8),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      "Upload subtitle file",
                      style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500),
                    ),
                  ),
                  Icon(Icons.file_upload_outlined, color: Colors.white70, size: 20),
                ],
              ),
            ),
          ),
        ),

        // Sync subtitle to audio
        Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () => _navigateTo(PlayerMenuPage.subtitleSettings),
            borderRadius: BorderRadius.circular(8),
            child: const Padding(
              padding: EdgeInsets.symmetric(vertical: 11, horizontal: 8),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      "Sync subtitle to audio",
                      style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500),
                    ),
                  ),
                  Icon(Icons.chevron_right, color: Colors.white38, size: 18),
                ],
              ),
            ),
          ),
        ),

        const SizedBox(height: 8),

        // Search Bar
        Container(
          height: 38,
          decoration: BoxDecoration(
            color: cardBg,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Row(
            children: [
              const Icon(Icons.search, color: textGrey, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: _subtitleSearchController,
                  style: const TextStyle(color: Colors.white, fontSize: 13),
                  decoration: const InputDecoration(
                    hintText: "Search",
                    hintStyle: TextStyle(color: textGrey, fontSize: 13),
                    border: InputBorder.none,
                    isDense: true,
                    contentPadding: EdgeInsets.zero,
                  ),
                  onChanged: (val) => setState(() => _subtitleSearchQuery = val),
                ),
              ),
              if (_subtitleSearchQuery.isNotEmpty)
                GestureDetector(
                  onTap: () {
                    _subtitleSearchController.clear();
                    setState(() => _subtitleSearchQuery = "");
                  },
                  child: const Icon(Icons.close, color: textGrey, size: 16),
                ),
            ],
          ),
        ),

        const SizedBox(height: 8),

        if (widget.subtitleTracks.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 24),
            child: Center(
              child: Text(
                "No embedded subtitles found.\nClick 'Upload subtitle file' above.",
                textAlign: TextAlign.center,
                style: TextStyle(color: textGrey, fontSize: 13),
              ),
            ),
          )
        else
          ...filtered.map((t) {
            final isSelected = widget.subtitlesEnabled && widget.selectedSubtitleTrack.id == t.id;
            final title = t.title ?? t.language ?? "Track ${t.id}";
            return _buildTrackOptionRow(
              title: title,
              isSelected: isSelected,
              onTap: () {
                widget.onToggleSubtitles(true);
                widget.onSelectSubtitleTrack(t);
              },
            );
          }),
      ],
    );
  }

  Widget _buildTrackOptionRow({
    required String title,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 8),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(
                    color: isSelected ? accentLimeGreen : Colors.white,
                    fontSize: 14,
                    fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                  ),
                ),
              ),
              if (isSelected)
                const Icon(Icons.check_circle, color: accentLimeGreen, size: 18),
            ],
          ),
        ),
      ),
    );
  }

  // -------------------------------------------------------------
  // 3. SUBTITLE SETTINGS PAGE (Matches frame_010.png, frame_026.png)
  // -------------------------------------------------------------
  Widget _buildSubtitleSettingsPage() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Use native subtitles
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    "Use native subtitles",
                    style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    "Render subtitles using device's built-in engine. Disables customization below.",
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            CupertinoSwitch(
              value: widget.useNativeSubtitles,
              activeTrackColor: accentLimeGreen,
              onChanged: widget.onToggleNativeSubtitles,
            ),
          ],
        ),

        const SizedBox(height: 14),

        // Pastel mode
        Row(
          children: [
            const Expanded(
              child: Text(
                "Pastel mode",
                style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
              ),
            ),
            CupertinoSwitch(
              value: widget.pastelMode,
              activeTrackColor: accentLimeGreen,
              onChanged: widget.onTogglePastelMode,
            ),
          ],
        ),

        const SizedBox(height: 16),

        // Subtitle delay
        const Text(
          "Subtitle delay",
          style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 4),
        SliderTheme(
          data: _greenSliderTheme(),
          child: Slider(
            value: widget.subtitleDelay.clamp(-5.0, 5.0),
            min: -5.0,
            max: 5.0,
            onChanged: (val) => widget.onSubtitleDelayChanged((val * 10).round() / 10),
          ),
        ),
        // Nudge Buttons: < Heard audio | 0 s | > Saw caption
        Row(
          children: [
            Expanded(
              child: _buildNudgeButton(
                label: "< Heard audio",
                onTap: () => widget.onSubtitleDelayChanged(
                  (widget.subtitleDelay - 0.5).clamp(-5.0, 5.0),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: cardBg,
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
              ),
              child: Text(
                "${widget.subtitleDelay >= 0 ? '+' : ''}${widget.subtitleDelay.toStringAsFixed(1)} s",
                style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _buildNudgeButton(
                label: "Saw caption >",
                onTap: () => widget.onSubtitleDelayChanged(
                  (widget.subtitleDelay + 0.5).clamp(-5.0, 5.0),
                ),
              ),
            ),
          ],
        ),

        const SizedBox(height: 18),

        // Font
        const Text(
          "Font",
          style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 8),
        Row(
          children: ["Cinejoy", "Netflix", "Montserrat"].map((font) {
            final isSel = widget.subtitleFont == font;
            return Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 3),
                child: _buildPillButton(
                  label: font,
                  isSelected: isSel,
                  onTap: () => widget.onSubtitleFontChanged(font),
                ),
              ),
            );
          }).toList(),
        ),

        const SizedBox(height: 16),

        // Text size
        Row(
          children: [
            const Text(
              "Text size",
              style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            Text(
              "${widget.subtitleSize.toInt()}%",
              style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        SliderTheme(
          data: _greenSliderTheme(),
          child: Slider(
            value: widget.subtitleSize.clamp(50.0, 200.0),
            min: 50.0,
            max: 200.0,
            onChanged: widget.onSubtitleSizeChanged,
          ),
        ),

        // Position
        Row(
          children: [
            const Text(
              "Position",
              style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            Text(
              widget.subtitlePosition == 100.0 ? "Default" : "${widget.subtitlePosition.toInt()}%",
              style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        SliderTheme(
          data: _greenSliderTheme(),
          child: Slider(
            value: widget.subtitlePosition.clamp(0.0, 100.0),
            min: 0.0,
            max: 100.0,
            onChanged: widget.onSubtitlePositionChanged,
          ),
        ),

        const SizedBox(height: 8),

        // Background
        Row(
          children: [
            const Expanded(
              child: Text(
                "Background",
                style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
              ),
            ),
            CupertinoSwitch(
              value: widget.subtitleBackground,
              activeTrackColor: accentLimeGreen,
              onChanged: widget.onToggleSubtitleBackground,
            ),
          ],
        ),

        const SizedBox(height: 16),

        // Color Swatches
        const Text(
          "Color",
          style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            _buildColorCircle(const Color(0xFFFFFFFF)),
            _buildColorCircle(const Color(0xFF93C5FD)),
            _buildColorCircle(const Color(0xFFFDE047)),
            _buildColorCircle(const Color(0xFF86EFAC)),
            // Custom edit button
            GestureDetector(
              onTap: () => widget.onSubtitleColorChanged(const Color(0xFFFF80DF)),
              child: Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: cardBg,
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white.withValues(alpha: 0.15)),
                ),
                child: const Icon(Icons.edit, color: Colors.white70, size: 16),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildNudgeButton({required String label, required VoidCallback onTap}) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(6),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: cardBg,
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
          ),
          child: Text(
            label,
            style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w600),
          ),
        ),
      ),
    );
  }

  Widget _buildColorCircle(Color color) {
    final isSelected = widget.subtitleColor.toARGB32() == color.toARGB32();
    return GestureDetector(
      onTap: () => widget.onSubtitleColorChanged(color),
      child: Container(
        margin: const EdgeInsets.only(right: 12),
        width: 32,
        height: 32,
        decoration: BoxDecoration(
          color: color,
          shape: BoxShape.circle,
          boxShadow: isSelected
              ? [BoxShadow(color: color.withValues(alpha: 0.4), blurRadius: 8, spreadRadius: 1)]
              : null,
        ),
        child: isSelected
            ? const Icon(Icons.check, color: Colors.black, size: 18)
            : null,
      ),
    );
  }

  // -------------------------------------------------------------
  // 4. AUDIO PAGE (Matches frame_014.png)
  // -------------------------------------------------------------
  Widget _buildAudioPage() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (widget.audioTracks.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 24),
            child: Center(
              child: Text(
                "No alternate audio tracks found",
                style: TextStyle(color: textGrey, fontSize: 13),
              ),
            ),
          )
        else
          ...widget.audioTracks.map((t) {
            final isSelected = widget.selectedAudioTrack.id == t.id;
            final title = t.title ?? t.language ?? "Track ${t.id}";
            return Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: () => widget.onSelectAudioTrack(t),
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                  child: Row(
                    children: [
                      const Icon(Icons.music_note, color: Colors.white70, size: 18),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          title,
                          style: TextStyle(
                            color: isSelected ? accentLimeGreen : Colors.white,
                            fontSize: 14,
                            fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                          ),
                        ),
                      ),
                      if (isSelected)
                        const Icon(Icons.check_circle, color: accentLimeGreen, size: 18),
                    ],
                  ),
                ),
              ),
            );
          }),
      ],
    );
  }

  // -------------------------------------------------------------
  // 5. PLAYBACK PAGE (Matches frame_016.png, frame_020.png)
  // -------------------------------------------------------------
  Widget _buildPlaybackPage() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Playback speed
        const Text(
          "Playback speed",
          style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 2),
        const Text(
          "Double-click center to customize",
          style: TextStyle(color: textGrey, fontSize: 11),
        ),
        const SizedBox(height: 8),
        Row(
          children: [0.25, 0.5, 1.0, 1.5, 2.0].map((s) {
            final isSel = (widget.playbackSpeed - s).abs() < 0.05;
            return Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2.5),
                child: _buildPillButton(
                  label: "${s}x",
                  isSelected: isSel,
                  onTap: () => widget.onSpeedChanged(s),
                ),
              ),
            );
          }).toList(),
        ),

        const SizedBox(height: 18),

        // Zoom (Aspect Ratio)
        const Text(
          "Zoom",
          style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 8),
        Row(
          children: ["Original", "Fill", "Stretch"].map((mode) {
            final isSel = widget.zoomMode == mode;
            return Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 3),
                child: _buildPillButton(
                  label: mode,
                  isSelected: isSel,
                  onTap: () => widget.onZoomChanged(mode),
                ),
              ),
            );
          }).toList(),
        ),

        const SizedBox(height: 18),

        // Volume Boost
        Row(
          children: [
            const Text(
              "Volume Boost",
              style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            Text(
              "${widget.volumeBoost.toInt()}%",
              style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        SliderTheme(
          data: _greenSliderTheme(),
          child: Slider(
            value: widget.volumeBoost.clamp(100.0, 600.0),
            min: 100.0,
            max: 600.0,
            onChanged: widget.onVolumeBoostChanged,
          ),
        ),
        const Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text("100%", style: TextStyle(color: textGrey, fontSize: 10)),
            Text("600%", style: TextStyle(color: textGrey, fontSize: 10)),
          ],
        ),

        const SizedBox(height: 14),

        // Brightness Quick Slider
        Row(
          children: [
            const Text(
              "Brightness",
              style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            Text(
              "${widget.quickBrightness.toInt()}%",
              style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold),
            ),
            const SizedBox(width: 6),
            GestureDetector(
              onTap: () => widget.onQuickBrightnessChanged(100.0),
              child: const Icon(Icons.restart_alt, color: textGrey, size: 16),
            ),
          ],
        ),
        SliderTheme(
          data: _greenSliderTheme(),
          child: Slider(
            value: widget.quickBrightness.clamp(50.0, 200.0),
            min: 50.0,
            max: 200.0,
            onChanged: widget.onQuickBrightnessChanged,
          ),
        ),

        const SizedBox(height: 14),
        Divider(color: Colors.white.withValues(alpha: 0.08), height: 1),
        const SizedBox(height: 8),

        // Modify colors
        _buildListTile(
          icon: Icons.tune,
          title: "Modify colors",
          trailingText: "On",
          onTap: () => _navigateTo(PlayerMenuPage.modifyColors),
        ),
      ],
    );
  }

  // -------------------------------------------------------------
  // 6. MODIFY COLORS PAGE (Matches frame_023.png)
  // -------------------------------------------------------------
  Widget _buildModifyColorsPage() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Brightness
        _buildColorSlider(
          label: "Brightness",
          value: widget.brightness,
          unit: "%",
          min: 50.0,
          max: 200.0,
          onChanged: widget.onBrightnessChanged,
          onReset: () => widget.onBrightnessChanged(100.0),
        ),
        const SizedBox(height: 14),

        // Contrast
        _buildColorSlider(
          label: "Contrast",
          value: widget.contrast,
          unit: "%",
          min: 50.0,
          max: 200.0,
          onChanged: widget.onContrastChanged,
          onReset: () => widget.onContrastChanged(100.0),
        ),
        const SizedBox(height: 14),

        // Saturation
        _buildColorSlider(
          label: "Saturation",
          value: widget.saturation,
          unit: "%",
          min: 0.0,
          max: 200.0,
          onChanged: widget.onSaturationChanged,
          onReset: () => widget.onSaturationChanged(100.0),
        ),
        const SizedBox(height: 14),

        // Hue
        _buildColorSlider(
          label: "Hue",
          value: widget.hue,
          unit: "°",
          min: 0.0,
          max: 360.0,
          onChanged: widget.onHueChanged,
          onReset: () => widget.onHueChanged(0.0),
        ),
      ],
    );
  }

  Widget _buildColorSlider({
    required String label,
    required double value,
    required String unit,
    required double min,
    required double max,
    required ValueChanged<double> onChanged,
    required VoidCallback onReset,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              label,
              style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            Text(
              "${value.toInt()}$unit",
              style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold),
            ),
            const SizedBox(width: 8),
            GestureDetector(
              onTap: onReset,
              child: const Icon(Icons.restart_alt, color: textGrey, size: 16),
            ),
          ],
        ),
        SliderTheme(
          data: _greenSliderTheme(),
          child: Slider(
            value: value.clamp(min, max),
            min: min,
            max: max,
            onChanged: onChanged,
          ),
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text("${min.toInt()}$unit", style: const TextStyle(color: textGrey, fontSize: 10)),
            Text("${max.toInt()}$unit", style: const TextStyle(color: textGrey, fontSize: 10)),
          ],
        ),
      ],
    );
  }

  // -------------------------------------------------------------
  // 7. SLEEP TIMER PAGE
  // -------------------------------------------------------------
  Widget _buildSleepTimerPage() {
    final options = [
      (0, false, "Off"),
      (15, false, "15 minutes"),
      (30, false, "30 minutes"),
      (45, false, "45 minutes"),
      (60, false, "60 minutes"),
      (0, true, "End of Show"),
    ];

    return Column(
      children: options.map((opt) {
        final isSelected = opt.$2
            ? widget.sleepAtEndOfShow
            : (!widget.sleepAtEndOfShow && widget.sleepMinutesRemaining == opt.$1);
        return Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () {
              widget.onSetSleepTimer(opt.$1, opt.$2);
            },
            borderRadius: BorderRadius.circular(8),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 8),
              child: Row(
                children: [
                  Icon(
                    opt.$2 ? Icons.movie_outlined : Icons.timer_outlined,
                    color: Colors.white70,
                    size: 18,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      opt.$3,
                      style: TextStyle(
                        color: isSelected ? accentLimeGreen : Colors.white,
                        fontSize: 14,
                        fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                      ),
                    ),
                  ),
                  if (isSelected)
                    const Icon(Icons.check_circle, color: accentLimeGreen, size: 18),
                ],
              ),
            ),
          ),
        );
      }).toList(),
    );
  }

  // -------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------
  Widget _buildPillButton({
    required String label,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 9),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: isSelected ? cardBgSelected : cardBg,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: isSelected ? accentLimeGreen : Colors.white.withValues(alpha: 0.08),
              width: isSelected ? 1.2 : 1,
            ),
          ),
          child: Text(
            label,
            style: TextStyle(
              color: isSelected ? Colors.white : Colors.white70,
              fontSize: 12,
              fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }

  SliderThemeData _greenSliderTheme() {
    return SliderTheme.of(context).copyWith(
      trackHeight: 3.5,
      activeTrackColor: accentLimeGreen,
      inactiveTrackColor: const Color(0xFF333338),
      thumbColor: accentLimeGreen,
      thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
      overlayColor: accentLimeGreen.withValues(alpha: 0.2),
      overlayShape: const RoundSliderOverlayShape(overlayRadius: 12),
    );
  }
}
