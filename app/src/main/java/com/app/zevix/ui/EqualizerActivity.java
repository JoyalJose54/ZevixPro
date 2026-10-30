package com.app.zevix.ui;

import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.app.zevix.R;

public class EqualizerActivity extends AppCompatActivity {

    private static final String PREF_EQ = "zevix_equalizer_pref";
    private SharedPreferences prefs;

    private ImageView btnBack;
    private SwitchCompat switchEnable;
    private LinearLayout layoutPresets;
    private LinearLayout layoutBandsContainer;
    private SeekBar seekBass;
    private TextView tvBassValue;
    private SeekBar seekVirtualizer;
    private TextView tvVirValue;
    private LinearLayout layoutReverbs;

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;

    private final String[] presets = new String[]{
            "Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock", "Custom"
    };

    private final String[] reverbs = new String[]{
            "None", "Small Room", "Medium Room", "Large Room", "Plate"
    };

    private int selectedPresetIndex = 0;
    private int selectedReverbIndex = 0;
    private final SeekBar[] bandSeekBars = new SeekBar[5];
    private final TextView[] bandValueTexts = new TextView[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        prefs = getSharedPreferences(PREF_EQ, MODE_PRIVATE);

        btnBack = findViewById(R.id.btn_back);
        switchEnable = findViewById(R.id.switch_eq_enable);
        layoutPresets = findViewById(R.id.layout_presets);
        layoutBandsContainer = findViewById(R.id.layout_bands_container);
        seekBass = findViewById(R.id.seek_bass);
        tvBassValue = findViewById(R.id.tv_bass_value);
        seekVirtualizer = findViewById(R.id.seek_virtualizer);
        tvVirValue = findViewById(R.id.tv_vir_value);
        layoutReverbs = findViewById(R.id.layout_reverbs);

        btnBack.setOnClickListener(v -> finish());

        initAudioEffects();
        setupPresetsUI();
        setupBandsUI();
        setupBassAndVirtualizerUI();
        setupReverbsUI();

        boolean enabled = prefs.getBoolean("eq_enabled", true);
        switchEnable.setChecked(enabled);
        setEffectsEnabled(enabled);

        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("eq_enabled", isChecked).apply();
            setEffectsEnabled(isChecked);
        });
    }

    private void initAudioEffects() {
        try {
            equalizer = new Equalizer(0, 0);
            equalizer.setEnabled(true);
        } catch (Exception ignored) {}

        try {
            bassBoost = new BassBoost(0, 0);
            bassBoost.setEnabled(true);
        } catch (Exception ignored) {}

        try {
            virtualizer = new Virtualizer(0, 0);
            virtualizer.setEnabled(true);
        } catch (Exception ignored) {}
    }

    private void setEffectsEnabled(boolean enabled) {
        try {
            if (equalizer != null) equalizer.setEnabled(enabled);
            if (bassBoost != null) bassBoost.setEnabled(enabled);
            if (virtualizer != null) virtualizer.setEnabled(enabled);
        } catch (Exception ignored) {}
    }

    private void setupPresetsUI() {
        selectedPresetIndex = prefs.getInt("preset_index", 0);
        layoutPresets.removeAllViews();

        for (int i = 0; i < presets.length; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(presets[i]);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(36, 16, 36, 16);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(16);
            chip.setLayoutParams(lp);

            updateChipStyle(chip, i == selectedPresetIndex);

            chip.setOnClickListener(v -> {
                selectedPresetIndex = index;
                prefs.edit().putInt("preset_index", index).apply();
                setupPresetsUI();
                applyPresetToBands(index);
            });

            layoutPresets.addView(chip);
        }
    }

    private void updateChipStyle(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_promoter_active);
            chip.setTextColor(getResources().getColor(R.color.white, getTheme()));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_promoter);
            chip.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
        }
    }

    private void setupBandsUI() {
        layoutBandsContainer.removeAllViews();
        short numBands = 5;
        if (equalizer != null) {
            try {
                numBands = equalizer.getNumberOfBands();
            } catch (Exception ignored) {}
        }
        numBands = (short) Math.min(5, Math.max(1, (int) numBands));

        final short minLevel = equalizer != null ? equalizer.getBandLevelRange()[0] : -1500;
        final short maxLevel = equalizer != null ? equalizer.getBandLevelRange()[1] : 1500;
        final int range = maxLevel - minLevel;

        int[] defaultFreqs = new int[]{60000, 230000, 910000, 3600000, 14000000};

        for (short b = 0; b < numBands; b++) {
            final short band = b;
            int freq = defaultFreqs[Math.min(b, defaultFreqs.length - 1)];
            if (equalizer != null) {
                try {
                    freq = equalizer.getCenterFreq(band);
                } catch (Exception ignored) {}
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 12, 0, 12);

            TextView tvFreq = new TextView(this);
            tvFreq.setLayoutParams(new LinearLayout.LayoutParams(140, LinearLayout.LayoutParams.WRAP_CONTENT));
            tvFreq.setText(formatFrequency(freq));
            tvFreq.setTextColor(getResources().getColor(R.color.white, getTheme()));
            tvFreq.setTextSize(13);

            SeekBar seek = new SeekBar(this);
            LinearLayout.LayoutParams lpSeek = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            seek.setLayoutParams(lpSeek);
            seek.setMax(range);
            seek.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.accent_color, getTheme())));
            seek.setThumbTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.accent_color, getTheme())));

            TextView tvVal = new TextView(this);
            tvVal.setLayoutParams(new LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT));
            tvVal.setGravity(Gravity.END);
            tvVal.setText("0 dB");
            tvVal.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            tvVal.setTextSize(12);

            int savedBandProgress = prefs.getInt("band_" + band, range / 2);
            seek.setProgress(savedBandProgress);
            short currentLevel = (short) (minLevel + savedBandProgress);
            tvVal.setText((currentLevel / 100) + " dB");

            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    short level = (short) (minLevel + progress);
                    tvVal.setText((level / 100) + " dB");
                    if (fromUser) {
                        prefs.edit().putInt("band_" + band, progress).apply();
                        if (equalizer != null) {
                            try {
                                equalizer.setBandLevel(band, level);
                            } catch (Exception ignored) {}
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            bandSeekBars[b] = seek;
            bandValueTexts[b] = tvVal;

            row.addView(tvFreq);
            row.addView(seek);
            row.addView(tvVal);
            layoutBandsContainer.addView(row);
        }
    }

    private String formatFrequency(int freqMilliHz) {
        int hz = freqMilliHz / 1000;
        if (hz >= 1000) {
            return (hz / 1000) + " kHz";
        } else {
            return hz + " Hz";
        }
    }

    private void applyPresetToBands(int presetIndex) {
        if (presetIndex == presets.length - 1) return; // Custom
        // Predefined relative dB values for presets:
        int[][] presetGains = new int[][]{
                {0, 0, 0, 0, 0},         // Normal
                {500, 300, -200, 400, 500}, // Classical
                {600, 200, 200, 400, 0},   // Dance
                {0, 0, 0, 0, 0},         // Flat
                {300, 0, 0, 200, -100},    // Folk
                {400, 100, 900, 300, 0},   // Heavy Metal
                {500, 300, 0, 200, 300},   // Hip Hop
                {400, 200, -200, 200, 500},// Jazz
                {-100, 200, 500, 100, -200},// Pop
                {500, 300, -100, 300, 500} // Rock
        };

        if (presetIndex < presetGains.length) {
            int[] gains = presetGains[presetIndex];
            final short minLevel = equalizer != null ? equalizer.getBandLevelRange()[0] : -1500;
            final short maxLevel = equalizer != null ? equalizer.getBandLevelRange()[1] : 1500;
            final int range = maxLevel - minLevel;

            for (int i = 0; i < 5 && i < gains.length; i++) {
                if (bandSeekBars[i] != null) {
                    int progress = (gains[i] - minLevel);
                    bandSeekBars[i].setProgress(Math.max(0, Math.min(range, progress)));
                    if (equalizer != null) {
                        try {
                            equalizer.setBandLevel((short) i, (short) gains[i]);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void setupBassAndVirtualizerUI() {
        int bass = prefs.getInt("bass_boost", 0);
        seekBass.setProgress(bass);
        tvBassValue.setText(bass + "%");

        seekBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvBassValue.setText(progress + "%");
                if (fromUser) {
                    prefs.edit().putInt("bass_boost", progress).apply();
                    if (bassBoost != null) {
                        try {
                            bassBoost.setStrength((short) (progress * 10));
                        } catch (Exception ignored) {}
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        int vir = prefs.getInt("virtualizer", 0);
        seekVirtualizer.setProgress(vir);
        tvVirValue.setText(vir + "%");

        seekVirtualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVirValue.setText(progress + "%");
                if (fromUser) {
                    prefs.edit().putInt("virtualizer", progress).apply();
                    if (virtualizer != null) {
                        try {
                            virtualizer.setStrength((short) (progress * 10));
                        } catch (Exception ignored) {}
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupReverbsUI() {
        selectedReverbIndex = prefs.getInt("reverb_index", 0);
        layoutReverbs.removeAllViews();

        for (int i = 0; i < reverbs.length; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(reverbs[i]);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(36, 16, 36, 16);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(16);
            chip.setLayoutParams(lp);

            updateChipStyle(chip, i == selectedReverbIndex);

            chip.setOnClickListener(v -> {
                selectedReverbIndex = index;
                prefs.edit().putInt("reverb_index", index).apply();
                setupReverbsUI();
                Toast.makeText(this, "Reverb: " + reverbs[index], Toast.LENGTH_SHORT).show();
            });

            layoutReverbs.addView(chip);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (equalizer != null) equalizer.release();
            if (bassBoost != null) bassBoost.release();
            if (virtualizer != null) virtualizer.release();
        } catch (Exception ignored) {}
    }
}

