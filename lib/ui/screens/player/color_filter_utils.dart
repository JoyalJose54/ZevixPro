import 'dart:math' as math;

/// Utility to generate a 4x5 ColorFilter matrix for Brightness, Contrast, Saturation, and Hue
class ColorFilterUtils {
  static List<double> calculateColorMatrix({
    required double brightness, // 50% to 200%, default 100%
    required double contrast,   // 50% to 200%, default 100%
    required double saturation, // 0% to 200%, default 100%
    required double hueDegrees, // 0 to 360, default 0
  }) {
    if ((brightness - 100.0).abs() < 0.1 &&
        (contrast - 100.0).abs() < 0.1 &&
        (saturation - 100.0).abs() < 0.1 &&
        hueDegrees.abs() < 0.1) {
      return const [
        1, 0, 0, 0, 0,
        0, 1, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 0, 1, 0,
      ];
    }

    final b = brightness / 100.0;
    final c = contrast / 100.0;
    final s = saturation / 100.0;
    final rad = hueDegrees * math.pi / 180.0;
    final cosVal = math.cos(rad);
    final sinVal = math.sin(rad);

    // Luminance constants (ITU-R BT.709)
    const lumR = 0.2126;
    const lumG = 0.7152;
    const lumB = 0.0722;

    // Saturation matrix coefficients
    final sr1 = (1.0 - s) * lumR + s;
    final sr2 = (1.0 - s) * lumG;
    final sr3 = (1.0 - s) * lumB;

    final sg1 = (1.0 - s) * lumR;
    final sg2 = (1.0 - s) * lumG + s;
    final sg3 = (1.0 - s) * lumB;

    final sb1 = (1.0 - s) * lumR;
    final sb2 = (1.0 - s) * lumG;
    final sb3 = (1.0 - s) * lumB + s;

    // Hue rotation matrix
    final hr1 = lumR + cosVal * (1.0 - lumR) + sinVal * (-lumR);
    final hr2 = lumG + cosVal * (-lumG) + sinVal * (-lumG);
    final hr3 = lumB + cosVal * (-lumB) + sinVal * (1.0 - lumB);

    final hg1 = lumR + cosVal * (-lumR) + sinVal * 0.143;
    final hg2 = lumG + cosVal * (1.0 - lumG) + sinVal * 0.140;
    final hg3 = lumB + cosVal * (-lumB) + sinVal * (-0.283);

    final hb1 = lumR + cosVal * (-lumR) + sinVal * (-(1.0 - lumR));
    final hb2 = lumG + cosVal * (-lumG) + sinVal * lumG;
    final hb3 = lumB + cosVal * (1.0 - lumB) + sinVal * lumB;

    final scale = c * b;
    final offset = (1.0 - c) * 128.0;

    return [
      scale * (sr1 * hr1 + sr2 * hg1 + sr3 * hb1),
      scale * (sr1 * hr2 + sr2 * hg2 + sr3 * hb2),
      scale * (sr1 * hr3 + sr2 * hg3 + sr3 * hb3),
      0,
      offset,

      scale * (sg1 * hr1 + sg2 * hg1 + sg3 * hb1),
      scale * (sg1 * hr2 + sg2 * hg2 + sg3 * hb2),
      scale * (sg1 * hr3 + sg2 * hg3 + sg3 * hb3),
      0,
      offset,

      scale * (sb1 * hr1 + sb2 * hg1 + sb3 * hb1),
      scale * (sb1 * hr2 + sb2 * hg2 + sb3 * hb2),
      scale * (sb1 * hr3 + sb2 * hg3 + sb3 * hb3),
      0,
      offset,

      0, 0, 0, 1, 0,
    ];
  }
}

