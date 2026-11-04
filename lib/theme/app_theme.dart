import 'package:flutter/material.dart';

class AppTheme {
  // Brand colors: Bright Lime Green (#95FF50)
  static const Color accentColor = Color(0xFF95FF50);
  static const Color accentColorPressed = Color(0xFF7CE63B);
  static const Color bgMain = Color(0xFF000000); // Pure AMOLED Black
  static const Color bgCard = Color(0xFF0D0D0D);
  static const Color bgCardSelected = Color(0xFF1F2430);
  static const Color bgPopup = Color(0xFF181818);
  static const Color bgDialog = Color(0xFF121212);
  static const Color dividerLine = Color(0xFF1C1C1C);

  // Text & Icons
  static const Color textPrimary = Color(0xFFFFFFFF);
  static const Color textSecondary = Color(0xFF9AA0B0);
  static const Color textMuted = Color(0xFF666C80);
  static const Color yellowBoost = Color(0xFFFFAE00);

  // Player overlay
  static const Color overlayHudBg = Color(0xB3000000);
  static const Color playerProgressBuffered = Color(0x4DFFFFFF);
  static const Color playerProgressPlayed = Color(0xFF95FF50);

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: bgMain,
      colorScheme: const ColorScheme.dark(
        primary: accentColor,
        secondary: accentColor,
        surface: bgCard,
        surfaceContainerHighest: bgPopup,
        onPrimary: Color(0xFF0F1A11),
        onSurface: textPrimary,
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: bgDialog,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      cardTheme: CardThemeData(
        color: bgCard,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: dividerLine, width: 1),
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: bgMain,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: textPrimary,
          fontSize: 18,
          fontWeight: FontWeight.bold,
        ),
        iconTheme: IconThemeData(color: Colors.white),
      ),
      dividerTheme: const DividerThemeData(
        color: dividerLine,
        thickness: 1,
        space: 1,
      ),
      sliderTheme: SliderThemeData(
        activeTrackColor: accentColor,
        inactiveTrackColor: Colors.white24,
        thumbColor: accentColor,
        overlayColor: accentColor.withValues(alpha: 0.2),
        trackHeight: 3.5,
        thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
      ),
      tooltipTheme: TooltipThemeData(
        decoration: BoxDecoration(
          color: bgPopup,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: dividerLine),
        ),
        textStyle: const TextStyle(color: textPrimary, fontSize: 12),
      ),
    );
  }
}
