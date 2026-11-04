import 'package:flutter/material.dart';
import '../../services/playback_preferences.dart';
import '../../theme/app_theme.dart';

class EqualizerDialog extends StatefulWidget {
  final PlaybackPreferences preferences;
  final Function(List<double> bands, int bassBoost, int virtualizer)? onApply;

  const EqualizerDialog({
    super.key,
    required this.preferences,
    this.onApply,
  });

  @override
  State<EqualizerDialog> createState() => _EqualizerDialogState();
}

class _EqualizerDialogState extends State<EqualizerDialog> {
  static const List<String> _presets = [
    "Flat", "Rock", "Pop", "Jazz", "Classical", "Heavy Metal", "Dance", "Bass Boost"
  ];

  static const List<List<double>> _presetBands = [
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],         // Flat
    [4, 3, 2, 0, -1, 1, 3, 4, 4, 4],       // Rock
    [-1, 1, 3, 4, 3, 0, -1, -1, 1, 2],     // Pop
    [3, 2, 0, 2, -1, -1, 0, 2, 3, 4],      // Jazz
    [4, 3, 2, 1, -1, -1, 0, 2, 3, 3],      // Classical
    [5, 4, 1, 2, -1, 0, 3, 5, 5, 4],       // Heavy Metal
    [4, 3, 1, 0, 0, -1, 2, 3, 3, 2],       // Dance
    [7, 6, 4, 2, 0, 0, 0, 0, 0, 0],        // Bass Boost
  ];

  static const List<String> _freqLabels = [
    "31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
  ];

  late bool _enabled;
  late int _selectedPreset;
  late List<double> _bands;
  late int _bassBoost;
  late int _virtualizer;

  @override
  void initState() {
    super.initState();
    _enabled = widget.preferences.isEqEnabled;
    _selectedPreset = widget.preferences.eqPreset;
    _bands = List<double>.from(widget.preferences.eqBands);
    _bassBoost = widget.preferences.bassBoost;
    _virtualizer = widget.preferences.virtualizer;
  }

  void _applyPreset(int index) {
    setState(() {
      _selectedPreset = index;
      _bands = List<double>.from(_presetBands[index]);
    });
    _save();
  }

  void _save() {
    widget.preferences.setEqEnabled(_enabled);
    widget.preferences.setEqPreset(_selectedPreset);
    widget.preferences.setEqBands(_bands);
    widget.preferences.setBassBoost(_bassBoost);
    widget.preferences.setVirtualizer(_virtualizer);
    widget.onApply?.call(_bands, _bassBoost, _virtualizer);
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: AppTheme.bgDialog,
      insetPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Container(
        width: 720,
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header with Switch
            Row(
              children: [
                const Icon(Icons.equalizer, color: AppTheme.accentColor, size: 28),
                const SizedBox(width: 12),
                const Text(
                  "Audio Equalizer & FX",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
                ),
                const Spacer(),
                Switch(
                  value: _enabled,
                  activeColor: AppTheme.accentColor,
                  onChanged: (val) {
                    setState(() => _enabled = val);
                    _save();
                  },
                ),
              ],
            ),
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 16),

            // Presets chips
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: List.generate(_presets.length, (i) {
                  final isSelected = _selectedPreset == i;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(_presets[i]),
                      selected: isSelected,
                      selectedColor: AppTheme.accentColor,
                      backgroundColor: AppTheme.bgCard,
                      labelStyle: TextStyle(
                        color: isSelected ? Colors.black : Colors.white,
                        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                        fontSize: 12,
                      ),
                      onSelected: _enabled ? (_) => _applyPreset(i) : null,
                    ),
                  );
                }),
              ),
            ),
            const SizedBox(height: 24),

            // 10-band graphic sliders
            SizedBox(
              height: 160,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: List.generate(10, (i) {
                  return Column(
                    children: [
                      Text(
                        "${_bands[i].toInt()}dB",
                        style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary),
                      ),
                      Expanded(
                        child: RotatedBox(
                          quarterTurns: -1,
                          child: Slider(
                            value: _bands[i],
                            min: -10,
                            max: 10,
                            divisions: 20,
                            activeColor: AppTheme.accentColor,
                            onChanged: _enabled
                                ? (val) {
                                    setState(() {
                                      _bands[i] = val;
                                      _selectedPreset = -1; // custom
                                    });
                                    _save();
                                  }
                                : null,
                          ),
                        ),
                      ),
                      Text(
                        _freqLabels[i],
                        style: const TextStyle(fontSize: 9, color: AppTheme.textMuted),
                      ),
                    ],
                  );
                }),
              ),
            ),
            const SizedBox(height: 24),

            // Bass Boost & 3D Virtualizer
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text("Bass Boost", style: TextStyle(color: Colors.white, fontSize: 13)),
                          Text("$_bassBoost%", style: const TextStyle(color: AppTheme.accentColor, fontSize: 13, fontWeight: FontWeight.bold)),
                        ],
                      ),
                      Slider(
                        value: _bassBoost.toDouble(),
                        min: 0,
                        max: 100,
                        activeColor: AppTheme.accentColor,
                        onChanged: _enabled
                            ? (val) {
                                setState(() => _bassBoost = val.toInt());
                                _save();
                              }
                            : null,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 24),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text("3D Virtualizer", style: TextStyle(color: Colors.white, fontSize: 13)),
                          Text("$_virtualizer%", style: const TextStyle(color: AppTheme.accentColor, fontSize: 13, fontWeight: FontWeight.bold)),
                        ],
                      ),
                      Slider(
                        value: _virtualizer.toDouble(),
                        min: 0,
                        max: 100,
                        activeColor: AppTheme.accentColor,
                        onChanged: _enabled
                            ? (val) {
                                setState(() => _virtualizer = val.toInt());
                                _save();
                              }
                            : null,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Close button
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text("Done", style: TextStyle(color: AppTheme.accentColor, fontSize: 14, fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

