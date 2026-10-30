package com.app.zevix.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;

import com.app.zevix.R;
import com.app.zevix.data.MusicPlayerManager;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.model.MusicItem;
import com.app.zevix.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayActivity extends AppCompatActivity implements MusicPlayerManager.OnMusicStateChangeListener {

    private ImageView btnBack;
    private ImageView btnEqualizer;
    private ImageView btnMore;
    private ImageView ivDiscArt;
    private ImageView btnTimer;
    private TextView tvSongTitle;
    private TextView tvSongArtist;
    private ImageView btnFavorite;

    private TextView tvTimeCurrent;
    private SeekBar musicSeekBar;
    private TextView tvTimeTotal;

    private ImageView btnLoopMode;
    private ImageView btnPrevious;
    private ImageView btnPlayPause;
    private ImageView btnNext;
    private ImageView btnPlaylistQueue;

    private ImageView btnFb10;
    private TextView btnSpeed;
    private ImageView btnFf10;

    private MusicPlayerManager playerManager;
    private PlaybackPreferences preferences;
    private ObjectAnimator discAnimator;
    private boolean isUserSeeking = false;
    private final Handler sleepTimerHandler = new Handler();

    private Equalizer audioEqualizer;
    private BassBoost bassBoost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);

        preferences = new PlaybackPreferences(this);
        playerManager = MusicPlayerManager.getInstance(this);

        initViews();
        setupDiscAnimation();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEqualizer = findViewById(R.id.btn_equalizer);
        btnMore = findViewById(R.id.btn_more);
        ivDiscArt = findViewById(R.id.iv_disc_art);
        btnTimer = findViewById(R.id.btn_timer);
        tvSongTitle = findViewById(R.id.tv_song_title);
        tvSongArtist = findViewById(R.id.tv_song_artist);
        btnFavorite = findViewById(R.id.btn_favorite);

        tvTimeCurrent = findViewById(R.id.tv_time_current);
        musicSeekBar = findViewById(R.id.music_seek_bar);
        tvTimeTotal = findViewById(R.id.tv_time_total);

        btnLoopMode = findViewById(R.id.btn_loop_mode);
        btnPrevious = findViewById(R.id.btn_previous);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        btnPlaylistQueue = findViewById(R.id.btn_playlist_queue);

        btnFb10 = findViewById(R.id.btn_fb10);
        btnSpeed = findViewById(R.id.btn_speed);
        btnFf10 = findViewById(R.id.btn_ff10);

        tvSongTitle.setSelected(true);
        updateLoopModeIcon(playerManager.getLoopMode());
        btnSpeed.setText(String.format(java.util.Locale.US, "%.1fX", playerManager.getSpeed()));
    }

    private void setupDiscAnimation() {
        discAnimator = ObjectAnimator.ofFloat(ivDiscArt, "rotation", 0f, 360f);
        discAnimator.setDuration(12000);
        discAnimator.setInterpolator(new LinearInterpolator());
        discAnimator.setRepeatCount(ValueAnimator.INFINITE);
        discAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnEqualizer.setOnClickListener(v -> showEqualizerDialog());
        btnMore.setOnClickListener(this::showMoreMenu);
        btnTimer.setOnClickListener(v -> showSleepTimerDialog());

        btnFavorite.setOnClickListener(v -> {
            MusicItem song = playerManager.getCurrentSong();
            if (song != null) {
                boolean isFav = preferences.isFavorite(song.getId());
                if (isFav) {
                    preferences.removeFavorite(song.getId());
                    btnFavorite.setImageResource(R.drawable.ic_favorite);
                    btnFavorite.setColorFilter(0x88ffffff);
                    Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                } else {
                    preferences.addFavorite(song.getId());
                    btnFavorite.setImageResource(R.drawable.ic_menu_favorite_s);
                    btnFavorite.setColorFilter(getResources().getColor(R.color.accent_color, getTheme()));
                    Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPlayPause.setOnClickListener(v -> playerManager.togglePlayPause());
        btnPrevious.setOnClickListener(v -> playerManager.playPrev());
        btnNext.setOnClickListener(v -> playerManager.playNext());

        btnFb10.setOnClickListener(v -> playerManager.seekBy(-10000));
        btnFf10.setOnClickListener(v -> playerManager.seekBy(10000));
        btnSpeed.setOnClickListener(v -> showSpeedDialog());

        btnLoopMode.setOnClickListener(v -> {
            int newMode = playerManager.cycleLoopMode();
            updateLoopModeIcon(newMode);
            String modeName = newMode == MusicPlayerManager.LOOP_ALL ? "Repeat All" :
                    (newMode == MusicPlayerManager.LOOP_ONE ? "Repeat Current" : "Shuffle");
            Toast.makeText(this, modeName, Toast.LENGTH_SHORT).show();
        });

        btnPlaylistQueue.setOnClickListener(v -> showQueueDialog());

        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playerManager.getDuration() > 0) {
                    long target = (playerManager.getDuration() * progress) / 1000;
                    tvTimeCurrent.setText(FormatUtils.formatDuration(target));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (playerManager.getDuration() > 0) {
                    long target = (playerManager.getDuration() * seekBar.getProgress()) / 1000;
                    playerManager.seekTo(target);
                }
                isUserSeeking = false;
            }
        });
    }

    private void updateLoopModeIcon(int mode) {
        if (mode == MusicPlayerManager.LOOP_ONE) {
            btnLoopMode.setImageResource(R.drawable.ic_repeat_repeat);
        } else if (mode == MusicPlayerManager.LOOP_SHUFFLE) {
            btnLoopMode.setImageResource(R.drawable.ic_repeat_shuffle);
        } else {
            btnLoopMode.setImageResource(R.drawable.ic_repeat_order);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerManager.addListener(this);
        initEqualizer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerManager.removeListener(this);
    }

    @Override
    public void onSongChanged(MusicItem item) {
        if (item == null) return;
        tvSongTitle.setText(item.getTitle());
        tvSongArtist.setText(item.getArtist());
        tvTimeTotal.setText(FormatUtils.formatDuration(item.getDuration()));

        boolean isFav = preferences.isFavorite(item.getId());
        if (isFav) {
            btnFavorite.setImageResource(R.drawable.ic_menu_favorite_s);
            btnFavorite.setColorFilter(getResources().getColor(R.color.accent_color, getTheme()));
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite);
            btnFavorite.setColorFilter(0x88ffffff);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        if (isPlaying) {
            if (discAnimator.isPaused()) {
                discAnimator.resume();
            } else if (!discAnimator.isStarted()) {
                discAnimator.start();
            }
        } else {
            discAnimator.pause();
        }
    }

    @Override
    public void onProgressUpdate(long currentPos, long duration) {
        if (isUserSeeking) return;
        tvTimeCurrent.setText(FormatUtils.formatDuration(currentPos));
        if (duration > 0) {
            tvTimeTotal.setText(FormatUtils.formatDuration(duration));
            int progress = (int) ((currentPos * 1000) / duration);
            musicSeekBar.setProgress(progress);
        }
    }

    private void showSpeedDialog() {
        String[] speeds = new String[]{"0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x"};
        float[] speedValues = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        int selected = 2;
        for (int i = 0; i < speedValues.length; i++) {
            if (Math.abs(speedValues[i] - playerManager.getSpeed()) < 0.05f) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Playback Speed")
                .setSingleChoiceItems(speeds, selected, (dialog, which) -> {
                    playerManager.setSpeed(speedValues[which]);
                    btnSpeed.setText(String.format(java.util.Locale.US, "%.2fX", speedValues[which]).replace(".00", ""));
                    dialog.dismiss();
                })
                .show();
    }

    private void showSleepTimerDialog() {
        String[] options = new String[]{"Turn Off", "15 Minutes", "30 Minutes", "60 Minutes"};
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Sleep Timer")
                .setItems(options, (dialog, which) -> {
                    sleepTimerHandler.removeCallbacksAndMessages(null);
                    if (which == 0) {
                        Toast.makeText(this, "Sleep timer turned off", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        sleepTimerHandler.postDelayed(() -> playerManager.stop(), 15 * 60 * 1000);
                        Toast.makeText(this, "Music will stop in 15 minutes", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        sleepTimerHandler.postDelayed(() -> playerManager.stop(), 30 * 60 * 1000);
                        Toast.makeText(this, "Music will stop in 30 minutes", Toast.LENGTH_SHORT).show();
                    } else if (which == 3) {
                        sleepTimerHandler.postDelayed(() -> playerManager.stop(), 60 * 60 * 1000);
                        Toast.makeText(this, "Music will stop in 60 minutes", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showQueueDialog() {
        List<MusicItem> queue = playerManager.getPlayQueue();
        if (queue.isEmpty()) return;

        String[] titles = new String[queue.size()];
        int selectedIndex = 0;
        MusicItem current = playerManager.getCurrentSong();

        for (int i = 0; i < queue.size(); i++) {
            titles[i] = (i + 1) + ". " + queue.get(i).getTitle();
            if (current != null && queue.get(i).getId() == current.getId()) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Now Playing Queue (" + queue.size() + ")")
                .setSingleChoiceItems(titles, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    playerManager.playSong(queue, which);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Sleep Timer");
        popup.getMenu().add(0, 2, 1, "Track Properties");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showSleepTimerDialog();
                return true;
            } else if (item.getItemId() == 2) {
                MusicItem song = playerManager.getCurrentSong();
                if (song != null) {
                    Toast.makeText(this, song.getTitle() + "\nArtist: " + song.getArtist() + "\nAlbum: " + song.getAlbum() + "\nSize: " + FormatUtils.formatFileSize(song.getSize()), Toast.LENGTH_LONG).show();
                }
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void initEqualizer() {
        try {
            int audioSessionId = playerManager.getAudioSessionId();
            if (audioSessionId != 0) {
                if (audioEqualizer != null) audioEqualizer.release();
                if (bassBoost != null) bassBoost.release();

                audioEqualizer = new Equalizer(0, audioSessionId);
                bassBoost = new BassBoost(0, audioSessionId);

                boolean eqEnabled = preferences.isEqualizerEnabled();
                audioEqualizer.setEnabled(eqEnabled);
                bassBoost.setEnabled(eqEnabled);

                int preset = preferences.getEqualizerPreset();
                if (preset < audioEqualizer.getNumberOfPresets()) {
                    audioEqualizer.usePreset((short) preset);
                }

                int strength = preferences.getBassBoostStrength();
                if (bassBoost.getStrengthSupported()) {
                    bassBoost.setStrength((short) strength);
                }
            }
        } catch (Exception ignored) {}
    }

    private void showEqualizerDialog() {
        if (audioEqualizer == null) {
            initEqualizer();
        }
        if (audioEqualizer == null) {
            Toast.makeText(this, "Equalizer initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.Dialog dialog = new android.app.Dialog(this, R.style.Theme_ZevixPro_Dialog);
        dialog.setContentView(R.layout.dialog_equalizer);

        SwitchCompat switchEq = dialog.findViewById(R.id.switch_eq);
        Spinner spinnerPresets = dialog.findViewById(R.id.spinner_presets);
        TextView tvBassLabel = dialog.findViewById(R.id.tv_bass_boost_label);
        SeekBar sbBass = dialog.findViewById(R.id.sb_bass_boost);
        LinearLayout layoutBands = dialog.findViewById(R.id.layout_bands);
        Button btnDone = dialog.findViewById(R.id.btn_close_eq);

        switchEq.setChecked(preferences.isEqualizerEnabled());
        switchEq.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setEqualizerEnabled(isChecked);
            audioEqualizer.setEnabled(isChecked);
            if (bassBoost != null) bassBoost.setEnabled(isChecked);
        });

        // Presets
        short numPresets = audioEqualizer.getNumberOfPresets();
        List<String> presetNames = new ArrayList<>();
        for (short i = 0; i < numPresets; i++) {
            presetNames.add(audioEqualizer.getPresetName(i));
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, presetNames);
        spinnerPresets.setAdapter(spinnerAdapter);
        spinnerPresets.setSelection(Math.min(preferences.getEqualizerPreset(), numPresets - 1));

        spinnerPresets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                preferences.setEqualizerPreset(position);
                audioEqualizer.usePreset((short) position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Bass boost
        if (bassBoost != null && bassBoost.getStrengthSupported()) {
            int currentStrength = preferences.getBassBoostStrength();
            sbBass.setProgress(currentStrength);
            tvBassLabel.setText("Bass Boost: " + (currentStrength / 10) + "%");

            sbBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tvBassLabel.setText("Bass Boost: " + (progress / 10) + "%");
                        bassBoost.setStrength((short) progress);
                        preferences.setBassBoostStrength(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Bands
        layoutBands.removeAllViews();
        short numBands = audioEqualizer.getNumberOfBands();
        final short minLevel = audioEqualizer.getBandLevelRange()[0];
        final short maxLevel = audioEqualizer.getBandLevelRange()[1];

        for (short i = 0; i < numBands; i++) {
            final short bandIndex = i;
            int freqHz = audioEqualizer.getCenterFreq(bandIndex) / 1000;
            String freqStr = freqHz >= 1000 ? (freqHz / 1000) + " kHz" : freqHz + " Hz";

            LinearLayout bandRow = new LinearLayout(this);
            bandRow.setOrientation(LinearLayout.HORIZONTAL);
            bandRow.setPadding(0, 8, 0, 8);

            TextView tvBand = new TextView(this);
            tvBand.setText(freqStr);
            tvBand.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            tvBand.setWidth(140);

            SeekBar sbBand = new SeekBar(this);
            sbBand.setMax(maxLevel - minLevel);
            sbBand.setProgress(audioEqualizer.getBandLevel(bandIndex) - minLevel);
            sbBand.setProgressDrawable(getResources().getDrawable(R.drawable.player_seekbar_progress, getTheme()));
            sbBand.setThumb(getResources().getDrawable(R.drawable.player_seekbar_thumb, getTheme()));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            sbBand.setLayoutParams(lp);

            sbBand.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        audioEqualizer.setBandLevel(bandIndex, (short) (progress + minLevel));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            bandRow.addView(tvBand);
            bandRow.addView(sbBand);
            layoutBands.addView(bandRow);
        }

        btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (discAnimator != null) discAnimator.cancel();
        if (audioEqualizer != null) audioEqualizer.release();
        if (bassBoost != null) bassBoost.release();
        sleepTimerHandler.removeCallbacksAndMessages(null);
    }
}

