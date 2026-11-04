import 'package:flutter/material.dart';
import 'package:window_manager/legacy.dart';

class WindowTitleBar extends StatefulWidget {
  final String title;
  final bool isGreenBanner;

  const WindowTitleBar({
    super.key,
    this.title = "Zevix Pro",
    this.isGreenBanner = true,
  });

  @override
  State<WindowTitleBar> createState() => _WindowTitleBarState();
}

class _WindowTitleBarState extends State<WindowTitleBar> with WindowListener {
  bool _isMaximized = false;

  @override
  void initState() {
    super.initState();
    windowManager.addListener(this);
    _checkMaximized();
  }

  @override
  void dispose() {
    windowManager.removeListener(this);
    super.dispose();
  }

  Future<void> _checkMaximized() async {
    final max = await windowManager.isMaximized();
    if (mounted) setState(() => _isMaximized = max);
  }

  @override
  void onWindowMaximize() => setState(() => _isMaximized = true);

  @override
  void onWindowUnmaximize() => setState(() => _isMaximized = false);

  @override
  Widget build(BuildContext context) {
    // Top banner color: Bright Lime Green (#95FF50)
    const bannerColor = Color(0xFF95FF50);
    const contentColor = Color(0xFF0F1A11);

    return Container(
      height: 32,
      color: widget.isGreenBanner ? bannerColor : Colors.black,
      child: Row(
        children: [
          // Drag area with Logo and Title
          Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onPanStart: (details) => windowManager.startDragging(),
              onDoubleTap: () async {
                if (await windowManager.isMaximized()) {
                  windowManager.unmaximize();
                } else {
                  windowManager.maximize();
                }
              },
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10),
                child: Row(
                  children: [
                    Image.asset(
                      'assets/logo.png',
                      width: 16,
                      height: 16,
                      errorBuilder: (_, __, ___) => const Icon(
                        Icons.play_circle_fill,
                        color: contentColor,
                        size: 16,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        (widget.title.isEmpty || widget.title == "Zevix Pro")
                            ? "Zevix Pro"
                            : (widget.isGreenBanner ? "Zevix Pro" : widget.title),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: widget.isGreenBanner ? contentColor : Colors.white,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.3,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Caption Buttons (Minimize, Change Screen / Maximize, Close)
          _buildCaptionButton(
            icon: Icons.remove,
            iconColor: widget.isGreenBanner ? contentColor : Colors.white,
            onPressed: () => windowManager.minimize(),
          ),
          _buildCaptionButton(
            icon: _isMaximized ? Icons.filter_none : Icons.crop_square,
            iconSize: 13,
            iconColor: widget.isGreenBanner ? contentColor : Colors.white,
            onPressed: () async {
              if (await windowManager.isMaximized()) {
                windowManager.unmaximize();
              } else {
                windowManager.maximize();
              }
            },
          ),
          _buildCaptionButton(
            icon: Icons.close,
            iconColor: widget.isGreenBanner ? contentColor : Colors.white,
            isClose: true,
            onPressed: () => windowManager.close(),
          ),
        ],
      ),
    );
  }

  Widget _buildCaptionButton({
    required IconData icon,
    required VoidCallback onPressed,
    required Color iconColor,
    double iconSize = 15,
    bool isClose = false,
  }) {
    return SizedBox(
      width: 44,
      height: 32,
      child: IconButton(
        padding: EdgeInsets.zero,
        icon: Icon(icon, size: iconSize, color: iconColor),
        hoverColor: isClose ? const Color(0xFFE53935) : Colors.black.withValues(alpha: 0.12),
        splashRadius: 16,
        onPressed: onPressed,
      ),
    );
  }
}
