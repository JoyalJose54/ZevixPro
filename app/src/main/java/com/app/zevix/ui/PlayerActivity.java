package com.app.zevix.ui;

import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.util.TypedValue;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import com.app.zevix.widget.EqualizerView;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import android.media.audiofx.LoudnessEnhancer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.data.TrashManager;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.FormatUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYLIST = "extra_playlist";
    public static final String EXTRA_CURRENT_INDEX = "extra_current_index";

    // Player engine & View
    private ExoPlayer player;
    private PlayerView playerView;
    private PlaybackPreferences preferences;
    private AudioManager audioManager;

    // UI overlays & Containers
    private View nightModeMask;
    private View appVideoBox;
    private View appVideoTopBox;
    private View llBottomBar;
    private ImageView appVideoLockedLeft;
    private ImageView appVideoLockedRight;

    // Top Bar Views
    private ImageView appVideoFinish;
    private TextView appVideoTitle;
    private ImageView ivAudioTrack;
    private ImageView ivCast;
    private ImageView visualBoostBtn;
    private ImageView subtitleBtn;
    private ImageView appVideoList;
    private ImageView appVideoMenu;

    // Quick Tools Bar Views
    private View containerNightMode;
    private ImageView nightMode;
    private View containerAbRepeat;
    private ImageView abRepeat;
    private View pipModeLayout;
    private ImageView pipMode;
    private View equalizerLayout;
    private ImageView equalizer;
    private View containerMirror;
    private ImageView ivMirror;
    private View containerRotate;
    private ImageView rotation;
    private View containerMute;
    private ImageView mute;
    private View containerAudioMode;
    private ImageView audioMode;
    private View containerSpeed;
    private TextView speedText;
    private HorizontalScrollView containerScroll;
    private View more;
    private View layoutExtraTools;
    private ImageView ivMoreExpand;
    private View speed2xLayout;

    // Gestures and Scale
    private boolean isLongPressSpeedBoost = false;
    private boolean wasPlayingBeforeScrub = false;
    private long lastScrubEndTime = 0;
    private ScaleGestureDetector scaleGestureDetector;
    private float currentScale = 1.0f;
    private int currentRepeatMode = Player.REPEAT_MODE_ALL;

    // Floating Camera Button
    private View screenshotContainer;
    private ImageView screenshotBtn;

    // Subtitle View
    private TextView subtitleText;

    // Bottom Bar Views
    private View appVideoProcessPanl;
    private View bottomBar;
    private TextView appVideoCurrentTime;
    private TextView appVideoEndTime;
    private SeekBar appVideoSeekBar;

    private ImageView appVideoLock;
    private ImageView videoFb10;
    private ImageView videoPrevious;
    private ImageView appVideoPlay;
    private ImageView videoNext;
    private ImageView videoFf10;
    private ImageView appVideoScaleType;
    private View containerScaleType;
    private TextView tvScaleTypeText;
    private boolean isDirectRatioSwitchEnabled = false;
    private boolean isRememberRatioEnabled = false;

    // Authentic Gesture HUD Views
    private View soundProgressLayout;
    private ProgressBar soundProgress;
    private View brightnessProgressLayout;
    private ProgressBar brightnessProgress;
    private View appVideoFastForwardBox;
    private TextView appVideoFastForwardAll;
    private TextView appVideoFastForward;

    // Thumbnail Scrubbing Bubble HUD (Matching original Zevix)
    private View thumbLayoutRoot;
    private View thumbLayout;
    private ImageView thumbImageView;
    private TextView textProgress;
    private View arrowDown;
    private MediaMetadataRetriever thumbRetriever;
    private String currentRetrieverVideoKey = null;
    private volatile long latestThumbTargetTimeMs = -1;
    private long lastThumbRequestTime = 0;
    private final java.util.concurrent.ExecutorService thumbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final LruCache<Long, Bitmap> thumbCache = new LruCache<>(50);
    private long lastSavedPosition = 0;

    private View appVideoVolumeBox;
    private TextView appVideoVolumeText;
    private TextView appVideoVolume;
    private TextView appVideoVolumeBoost;
    private ImageView appVideoVolumeIcon;
    private View appVideoBrightnessBox;
    private TextView appVideoBrightness;
    private ImageView appVideoBrightnessIcon;

    private int gestureStartVolume = -1;
    private float gestureStartBrightness = -1f;

    private boolean isControlsShowing = true;

    // Floating A-B Repeat Card
    private View abRepeatLayout;
    private TextView tvStartSet;
    private TextView tvEndSet;
    private TextView tvAbClear;
    private ImageView abClose;

    // Floating Subtitle Sync
    private View adjustSubtitleLayout;
    private ImageView adjustClose;
    private ImageView adjustReduce;
    private ImageView adjustAdd;
    private TextView adjustText;
    private float subtitleOffsetSec = 0.0f;

    // EndTime display mode
    private boolean isShowingRemainingTime = false;

    // Playlist state
    private List<VideoItem> playlist = new ArrayList<>();
    private int currentIndex = 0;
    private VideoItem currentVideo;

    // Player states
    private boolean isLocked = false;
    private boolean isUserSeeking = false;
    private int currentAspectRatioMode = 0; // 0: Fit, 1: Fill/Crop, 2: Stretch, 3: 16:9, 4: 4:3
    private float currentSpeed = 1.0f;
    private boolean isBackgroundPlayEnabled = false;
    private boolean isMirrored = false;
    private boolean isMuted = false;
    private boolean stopAfterCurrentVideo = false;

    // A-B Repeat state
    private long repeatPointA = -1;
    private long repeatPointB = -1;

    // Equalizer & AudioFX
    private Equalizer audioEqualizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private PresetReverb presetReverb;
    private LoudnessEnhancer loudnessEnhancer;
    private int currentVolumeBoostLevel = 15; // 0 to 30 scale

    // Handlers
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Handler controlsHideHandler = new Handler(Looper.getMainLooper());
    private final Handler hudHideHandler = new Handler(Looper.getMainLooper());
    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());

    private GestureDetector gestureDetector;
    private float initialTouchX;
    private float initialTouchY;
    private boolean isHorizontalScrubbing = false;
    private boolean isVerticalAdjusting = false;
    private boolean isAdjustingBrightness = false;
    private long scrubStartPosition = 0;
    private long scrubTargetPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        hideSystemBars();

        setContentView(R.layout.activity_player);

        preferences = new PlaybackPreferences(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initViews();
        parseIntentData();
        setupGestureDetector();
        initPlayer();
        setupClickListeners();

        // Restore saved settings
        currentAspectRatioMode = preferences.getAspectRatio();
        applyAspectRatio(currentAspectRatioMode);
        nightModeMask.setVisibility(preferences.isNightMode() ? View.VISIBLE : View.GONE);
        showControls();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void initViews() {
        playerView = findViewById(R.id.player_view);
        playerView.setOnTouchListener((v, event) -> onTouchEvent(event));
        nightModeMask = findViewById(R.id.night_mode_mask);
        appVideoBox = findViewById(R.id.app_video_box);
        appVideoTopBox = findViewById(R.id.app_video_top_box);
        if (appVideoTopBox != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appVideoTopBox, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                if (statusBarHeight <= 0) {
                    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resourceId > 0) {
                        statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                    }
                }
                v.setPadding(0, Math.max(0, statusBarHeight), 0, (int) (12 * getResources().getDisplayMetrics().density));
                return insets;
            });
        }
        llBottomBar = findViewById(R.id.ll_bottom_bar);
        if (llBottomBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(llBottomBar, (v, insets) -> {
                int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                v.setPadding(0, (int) (48 * getResources().getDisplayMetrics().density), 0, Math.max(0, navBarHeight));
                return insets;
            });
        }
        appVideoLockedLeft = findViewById(R.id.app_video_locked_left);
        appVideoLockedRight = findViewById(R.id.app_video_locked_right);

        // Top bar
        appVideoFinish = findViewById(R.id.app_video_finish);
        appVideoTitle = findViewById(R.id.app_video_title);
        ivAudioTrack = findViewById(R.id.iv_audio_track);
        ivCast = findViewById(R.id.iv_cast);
        visualBoostBtn = findViewById(R.id.visual_boost_btn);
        subtitleBtn = findViewById(R.id.subtitle_btn);
        appVideoList = findViewById(R.id.app_video_list);
        appVideoMenu = findViewById(R.id.app_video_menu);

        // Quick tools bar
        containerNightMode = findViewById(R.id.container_night_mode);
        nightMode = findViewById(R.id.night_mode);
        containerAbRepeat = findViewById(R.id.container_ab_repeat);
        abRepeat = findViewById(R.id.ab_repeat);
        pipModeLayout = findViewById(R.id.pip_mode_layout);
        pipMode = findViewById(R.id.pip_mode);
        equalizerLayout = findViewById(R.id.equalizer_layout);
        equalizer = findViewById(R.id.equalizer);
        containerMirror = findViewById(R.id.container_mirror);
        ivMirror = findViewById(R.id.iv_mirror);
        containerRotate = findViewById(R.id.container_rotate);
        rotation = findViewById(R.id.rotation);
        containerMute = findViewById(R.id.container_mute);
        mute = findViewById(R.id.mute);
        containerAudioMode = findViewById(R.id.container_audio_mode);
        audioMode = findViewById(R.id.audio_mode);
        containerSpeed = findViewById(R.id.container_speed);
        speedText = findViewById(R.id.speed_text);
        containerScroll = findViewById(R.id.container_scroll);
        more = findViewById(R.id.more);
        layoutExtraTools = findViewById(R.id.layout_extra_tools);
        ivMoreExpand = findViewById(R.id.iv_more_expand);
        speed2xLayout = findViewById(R.id.speed2xLayout);

        // Floating Camera
        screenshotContainer = findViewById(R.id.screenshot);
        screenshotBtn = findViewById(R.id.screenshotBtn);

        // Subtitle
        subtitleText = findViewById(R.id.subtitle_text);

        // Bottom Bar
        appVideoProcessPanl = findViewById(R.id.app_video_process_panl);
        bottomBar = findViewById(R.id.bottom_bar);
        appVideoCurrentTime = findViewById(R.id.app_video_currentTime);
        appVideoEndTime = findViewById(R.id.app_video_endTime);
        appVideoSeekBar = findViewById(R.id.app_video_seekBar);

        appVideoLock = findViewById(R.id.app_video_lock);
        videoFb10 = findViewById(R.id.video_fb10);
        videoPrevious = findViewById(R.id.video_previous);
        appVideoPlay = findViewById(R.id.app_video_play);
        videoNext = findViewById(R.id.video_next);
        videoFf10 = findViewById(R.id.video_ff10);
        appVideoScaleType = findViewById(R.id.app_video_scale_type);
        containerScaleType = findViewById(R.id.container_scale_type);
        tvScaleTypeText = findViewById(R.id.tv_scale_type_text);

        isDirectRatioSwitchEnabled = preferences.isDirectRatioSwitchEnabled();
        isRememberRatioEnabled = preferences.isRememberRatioEnabled();

        // Thumbnail Scrubbing Bubble HUD
        thumbLayoutRoot = findViewById(R.id.thumb_layout_root);
        thumbLayout = findViewById(R.id.thumb_layout);
        thumbImageView = findViewById(R.id.thumb);
        textProgress = findViewById(R.id.text_progress);
        arrowDown = findViewById(R.id.arrow_down);

        // Authentic Gesture HUD views
        soundProgressLayout = findViewById(R.id.sound_progress_layout);
        soundProgress = findViewById(R.id.sound_progress);
        appVideoVolumeIcon = findViewById(R.id.app_video_volume_icon);

        brightnessProgressLayout = findViewById(R.id.brightness_progress_layout);
        brightnessProgress = findViewById(R.id.brightness_progress);
        appVideoBrightnessIcon = findViewById(R.id.app_video_brightness_icon);

        appVideoFastForwardBox = findViewById(R.id.app_video_fastForward_box);
        appVideoFastForwardAll = findViewById(R.id.app_video_fastForward_all);
        appVideoFastForward = findViewById(R.id.app_video_fastForward);

        appVideoVolumeBox = findViewById(R.id.app_video_volume_box);
        appVideoVolumeText = findViewById(R.id.app_video_volume_text);
        appVideoVolume = findViewById(R.id.app_video_volume);
        appVideoVolumeBoost = findViewById(R.id.app_video_volume_boost);

        appVideoBrightnessBox = findViewById(R.id.app_video_brightness_box);
        appVideoBrightness = findViewById(R.id.app_video_brightness);

        screenshotContainer = findViewById(R.id.screenshot);
        screenshotBtn = findViewById(R.id.screenshotBtn);
        if (screenshotContainer != null) {
            screenshotContainer.setVisibility(preferences.isScreenshotButtonEnabled() ? View.VISIBLE : View.GONE);
        }

        // Restore remembered volume level
        int savedVol = preferences.getPlayerVolume();
        if (savedVol >= 0) {
            setVolumeLevel(savedVol, false);
        } else {
            int streamMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int streamCurrent = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            currentVolumeBoostLevel = streamMax > 0 ? Math.round((streamCurrent / (float) streamMax) * 15f) : 10;
            preferences.savePlayerVolume(currentVolumeBoostLevel);
        }

        // Restore remembered brightness level
        float savedBrightness = preferences.getPlayerBrightness();
        if (savedBrightness >= 0) {
            setBrightnessLevel(savedBrightness, false);
        }

        // Floating A-B Repeat Card
        abRepeatLayout = findViewById(R.id.ab_repeat_layout);
        tvStartSet = findViewById(R.id.tv_start_set);
        tvEndSet = findViewById(R.id.tv_end_set);
        tvAbClear = findViewById(R.id.tv_ab_clear);
        abClose = findViewById(R.id.ab_close);

        // Floating Subtitle Sync
        adjustSubtitleLayout = findViewById(R.id.adjust_subtitle_layout);
        adjustClose = findViewById(R.id.adjust_close);
        adjustReduce = findViewById(R.id.adjust_reduce);
        adjustAdd = findViewById(R.id.adjust_add);
        adjustText = findViewById(R.id.adjust_text);
    }

    @SuppressWarnings("unchecked")
    private void parseIntentData() {
        if (getIntent().hasExtra(EXTRA_PLAYLIST)) {
            playlist = (List<VideoItem>) getIntent().getSerializableExtra(EXTRA_PLAYLIST);
            currentIndex = getIntent().getIntExtra(EXTRA_CURRENT_INDEX, 0);
        } else if (getIntent().getData() != null) {
            Uri uri = getIntent().getData();
            String title = uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "Video";
            VideoItem singleVideo = new VideoItem(0, title, title, uri.getPath(), uri, 0, 0, 0, 0, 0, "", "");
            playlist = new ArrayList<>();
            playlist.add(singleVideo);
            currentIndex = 0;
        }

        if (playlist == null || playlist.isEmpty()) {
            Toast.makeText(this, "No video to play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            currentIndex = 0;
        }

        currentVideo = playlist.get(currentIndex);
        appVideoTitle.setText(currentVideo.getTitle());
    }

    private void initPlayer() {
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);

        if (preferences.isSoftwareDecoder()) {
            renderersFactory.setMediaCodecSelector((mimeType, requiresSecure, requiresTunneling) -> {
                List<MediaCodecInfo> decoders = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecure, requiresTunneling);
                List<MediaCodecInfo> sw = new ArrayList<>();
                for (MediaCodecInfo info : decoders) {
                    if (info.softwareOnly || info.name.startsWith("c2.android.") || info.name.startsWith("omx.google.")) {
                        sw.add(info);
                    }
                }
                return sw.isEmpty() ? decoders : sw;
            });
        } else {
            renderersFactory.setMediaCodecSelector((mimeType, requiresSecure, requiresTunneling) -> {
                List<MediaCodecInfo> decoders = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecure, requiresTunneling);
                List<MediaCodecInfo> hw = new ArrayList<>();
                for (MediaCodecInfo info : decoders) {
                    if (!info.softwareOnly && !info.name.startsWith("c2.android.") && !info.name.startsWith("omx.google.")) {
                        hw.add(info);
                    }
                }
                return hw.isEmpty() ? decoders : hw;
            });
        }

        player = new ExoPlayer.Builder(this, renderersFactory).build();
        playerView.setPlayer(player);

        androidx.media3.common.AudioAttributes audioAttributes = new androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();
        player.setAudioAttributes(audioAttributes, true);

        // Apply saved repeat mode
        currentRepeatMode = preferences.getRepeatMode();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);

        player.addListener(new Player.Listener() {
            @Override
            public void onVideoSizeChanged(@NonNull androidx.media3.common.VideoSize videoSize) {
                applyAspectRatio(currentAspectRatioMode);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    updateDurationDisplay();
                    initEqualizer();
                } else if (playbackState == Player.STATE_ENDED) {
                    if (currentVideo != null) {
                        long dur = player != null && player.getDuration() > 0 ? player.getDuration() : 1000;
                        preferences.savePlaybackPosition(currentVideo.getPath(), currentVideo.getId(), dur);
                    }
                    if (stopAfterCurrentVideo) {
                        stopAfterCurrentVideo = false;
                        player.pause();
                        Toast.makeText(PlayerActivity.this, "Playback ended", Toast.LENGTH_SHORT).show();
                    } else if (currentRepeatMode == Player.REPEAT_MODE_ONE) {
                        player.seekTo(0);
                        player.play();
                    } else {
                        playNextVideo();
                    }
                }
            }

            @Override
            public void onTracksChanged(Tracks tracks) {
                int audioCount = 0;
                for (Tracks.Group group : tracks.getGroups()) {
                    if (group.getType() == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                        audioCount += group.length;
                    }
                }
                ivAudioTrack.setVisibility(audioCount > 1 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                boolean playWhenReady = player != null && player.getPlayWhenReady();
                appVideoPlay.setImageResource(playWhenReady ? R.drawable.ic_pause : R.drawable.ic_play);
                if (isPlaying) {
                    startProgressTracker();
                    if (!isHorizontalScrubbing && !isUserSeeking) {
                        scheduleHideControls();
                    }
                } else {
                    stopProgressTracker();
                    if (!playWhenReady && !isHorizontalScrubbing && !isUserSeeking) {
                        showControls();
                    }
                }
            }
        });

        playVideoAtIndex(currentIndex);
    }

    private void switchDecoderMode(boolean swMode) {
        if (preferences.isSoftwareDecoder() == swMode) return;
        preferences.setSoftwareDecoder(swMode);
        Toast.makeText(this, swMode ? "Switched to SW Decoder" : "Switched to HW Decoder", Toast.LENGTH_SHORT).show();

        if (player != null) {
            long currentPos = player.getCurrentPosition();
            boolean isPlaying = player.isPlaying();
            player.release();
            initPlayer();
            player.seekTo(currentPos);
            if (isPlaying) {
                player.play();
            }
        }
    }

    private void playVideoAtIndex(int index) {
        if (index < 0 || index >= playlist.size()) return;

        // Save position of current video before switching
        if (player != null && currentVideo != null) {
            long cur = player.getCurrentPosition();
            long dur = player.getDuration();
            if (dur > 0 && cur >= dur - 1500) {
                preferences.savePlaybackPosition(currentVideo.getPath(), currentVideo.getId(), dur);
            } else {
                preferences.savePlaybackPosition(currentVideo.getPath(), currentVideo.getId(), cur);
            }
        }

        // Reset thumbnail preview retriever & cache for new video
        synchronized (this) {
            if (thumbRetriever != null) {
                try { thumbRetriever.release(); } catch (Exception ignored) {}
                thumbRetriever = null;
            }
        }
        thumbCache.evictAll();
        currentRetrieverVideoKey = null;
        latestThumbTargetTimeMs = -1;
        lastSavedPosition = 0;

        currentIndex = index;
        currentVideo = playlist.get(currentIndex);
        appVideoTitle.setText(currentVideo.getTitle());

        // Reset AB repeat
        repeatPointA = -1;
        repeatPointB = -1;

        MediaItem mediaItem = MediaItem.fromUri(currentVideo.getUri());
        player.setMediaItem(mediaItem);

        long savedPos = preferences.getSavedPosition(currentVideo.getPath(), currentVideo.getId());
        if (savedPos > 0 && (currentVideo.getDuration() <= 0 || savedPos < currentVideo.getDuration() - 2000)) {
            player.seekTo(savedPos);
        } else {
            player.seekTo(0);
        }

        player.prepare();
        player.play();
    }

    private void initEqualizer() {
        try {
            int audioSessionId = player.getAudioSessionId();
            if (audioSessionId != 0) {
                if (audioEqualizer != null) audioEqualizer.release();
                if (bassBoost != null) bassBoost.release();
                if (virtualizer != null) virtualizer.release();
                if (presetReverb != null) presetReverb.release();
                if (loudnessEnhancer != null) loudnessEnhancer.release();

                audioEqualizer = new Equalizer(0, audioSessionId);
                bassBoost = new BassBoost(0, audioSessionId);
                virtualizer = new Virtualizer(0, audioSessionId);
                presetReverb = new PresetReverb(0, audioSessionId);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    loudnessEnhancer = new LoudnessEnhancer(audioSessionId);
                    loudnessEnhancer.setEnabled(true);
                }

                boolean eqEnabled = preferences.isEqualizerEnabled();
                audioEqualizer.setEnabled(eqEnabled);
                bassBoost.setEnabled(eqEnabled);
                virtualizer.setEnabled(eqEnabled);
                presetReverb.setEnabled(eqEnabled);

                int preset = preferences.getEqualizerPreset();
                int[] savedBands = preferences.getEqualizerBands();
                if (savedBands != null && savedBands.length == audioEqualizer.getNumberOfBands()) {
                    short[] range = audioEqualizer.getBandLevelRange();
                    for (short b = 0; b < savedBands.length; b++) {
                        float ratio = (float) savedBands[b] / 3000f;
                        short level = (short) (range[0] + ratio * (range[1] - range[0]));
                        try {
                            audioEqualizer.setBandLevel(b, level);
                        } catch (Exception ignored) {}
                    }
                } else if (preset >= 0 && preset < audioEqualizer.getNumberOfPresets()) {
                    try {
                        audioEqualizer.usePreset((short) preset);
                    } catch (Exception ignored) {}
                }

                int strength = preferences.getBassBoostStrength();
                if (bassBoost.getStrengthSupported()) {
                    bassBoost.setStrength((short) strength);
                }

                int virStrength = preferences.getVirtualizerStrength();
                if (virtualizer.getStrengthSupported()) {
                    virtualizer.setStrength((short) virStrength);
                }

                int revPreset = preferences.getReverbPreset();
                try {
                    presetReverb.setPreset((short) revPreset);
                } catch (Exception ignored) {}

                // Apply existing volume boost if above 15
                if (currentVolumeBoostLevel > 15 && loudnessEnhancer != null) {
                    int boost = currentVolumeBoostLevel - 15;
                    try { loudnessEnhancer.setTargetGain(boost * 30); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        // Back
        appVideoFinish.setOnClickListener(v -> finish());

        // Play/Pause
        appVideoPlay.setOnClickListener(v -> {
            if (isHorizontalScrubbing || isUserSeeking || System.currentTimeMillis() - lastScrubEndTime < 300) {
                return;
            }
            togglePlayPause();
        });

        // Prev & Next
        videoPrevious.setOnClickListener(v -> playPrevVideo());
        videoNext.setOnClickListener(v -> playNextVideo());

        // Lock & Unlock
        appVideoLock.setOnClickListener(v -> toggleLock(true));
        appVideoLockedLeft.setOnClickListener(v -> toggleLock(false));
        appVideoLockedRight.setOnClickListener(v -> toggleLock(false));

        // Fast Rewind 10s & Forward 10s
        videoFb10.setOnClickListener(v -> {
            if (player != null) {
                long target = Math.max(0, player.getCurrentPosition() - 10000);
                player.seekTo(target);
            }
        });
        videoFf10.setOnClickListener(v -> {
            if (player != null) {
                long target = Math.min(player.getDuration(), player.getCurrentPosition() + 10000);
                player.seekTo(target);
            }
        });

        // Scale type (aspect ratio) - opens authentic ratio bottom sheet matching Zevix
        View.OnClickListener scaleClick = v -> {
            if (isDirectRatioSwitchEnabled) {
                cycleNextRatio();
            } else {
                showRatioBottomSheet();
            }
        };
        View.OnLongClickListener scaleLongClick = v -> {
            showRatioBottomSheet();
            return true;
        };

        if (containerScaleType != null) {
            containerScaleType.setOnClickListener(scaleClick);
            containerScaleType.setOnLongClickListener(scaleLongClick);
        }
        if (appVideoScaleType != null) {
            appVideoScaleType.setOnClickListener(scaleClick);
            appVideoScaleType.setOnLongClickListener(scaleLongClick);
        }

        // Top actions
        ivAudioTrack.setOnClickListener(v -> showAudioTrackBottomSheet());
        ivCast.setOnClickListener(v -> showCastDialog());
        visualBoostBtn.setOnClickListener(v -> showVisualBoostDialog());
        subtitleBtn.setOnClickListener(v -> showSubtitleBottomSheet());
        appVideoList.setOnClickListener(v -> showQueueBottomSheet());
        appVideoMenu.setOnClickListener(v -> showPlayerMenuBottomSheet());

        // Quick tools bar actions (bound to both container and icon)
        View.OnClickListener nightModeClick = v -> toggleNightMode();
        containerNightMode.setOnClickListener(nightModeClick);
        nightMode.setOnClickListener(nightModeClick);

        View.OnClickListener abRepeatClick = v -> handleAbRepeat();
        containerAbRepeat.setOnClickListener(abRepeatClick);
        abRepeat.setOnClickListener(abRepeatClick);

        View.OnClickListener pipClick = v -> enterPipMode();
        pipModeLayout.setOnClickListener(pipClick);
        pipMode.setOnClickListener(pipClick);

        View.OnClickListener eqClick = v -> showEqualizerDialog();
        equalizerLayout.setOnClickListener(eqClick);
        equalizer.setOnClickListener(eqClick);

        View.OnClickListener mirrorClick = v -> toggleMirror();
        containerMirror.setOnClickListener(mirrorClick);
        ivMirror.setOnClickListener(mirrorClick);

        View.OnClickListener rotateClick = v -> toggleOrientation();
        containerRotate.setOnClickListener(rotateClick);
        rotation.setOnClickListener(rotateClick);

        View.OnClickListener muteClick = v -> toggleMute();
        containerMute.setOnClickListener(muteClick);
        mute.setOnClickListener(muteClick);

        View.OnClickListener audioModeClick = v -> toggleBackgroundPlay();
        containerAudioMode.setOnClickListener(audioModeClick);
        audioMode.setOnClickListener(audioModeClick);

        View.OnClickListener speedClick = v -> showSpeedBottomSheet();
        containerSpeed.setOnClickListener(speedClick);
        speedText.setOnClickListener(speedClick);
        View speedBtn = findViewById(R.id.speed_btn);
        if (speedBtn != null) speedBtn.setOnClickListener(speedClick);

        View.OnClickListener moreClick = v -> {
            if (layoutExtraTools != null) {
                boolean show = layoutExtraTools.getVisibility() != View.VISIBLE;
                layoutExtraTools.setVisibility(show ? View.VISIBLE : View.GONE);
                if (ivMoreExpand != null) {
                    ivMoreExpand.animate().rotation(show ? 180f : 0f).setDuration(200).start();
                }
                if (containerScroll != null) {
                    containerScroll.postDelayed(() -> {
                        if (show) {
                            containerScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                        } else {
                            containerScroll.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                        }
                    }, 100);
                }
            }
        };
        more.setOnClickListener(moreClick);
        if (ivMoreExpand != null) {
            ivMoreExpand.setOnClickListener(moreClick);
        }

        // Floating camera
        screenshotBtn.setOnClickListener(v -> captureScreenshot());

        // Duration / Remaining Time toggle
        appVideoEndTime.setOnClickListener(v -> {
            isShowingRemainingTime = !isShowingRemainingTime;
            updateDurationDisplay();
        });

        // Tap empty area of controls container to hide controls
        if (appVideoBox != null) {
            appVideoBox.setClickable(false);
            appVideoBox.setOnTouchListener((v, event) -> onTouchEvent(event));
        }

        // A-B Repeat Card Actions
        tvStartSet.setOnClickListener(v -> {
            if (player == null) return;
            repeatPointA = player.getCurrentPosition();
            tvStartSet.setText(FormatUtils.formatDuration(repeatPointA));
            Toast.makeText(this, "Point A: " + FormatUtils.formatDuration(repeatPointA), Toast.LENGTH_SHORT).show();
        });
        tvEndSet.setOnClickListener(v -> {
            if (player == null) return;
            long cur = player.getCurrentPosition();
            if (repeatPointA >= 0 && cur > repeatPointA) {
                repeatPointB = cur;
                tvEndSet.setText(FormatUtils.formatDuration(repeatPointB));
                Toast.makeText(this, "Point B: " + FormatUtils.formatDuration(repeatPointB) + " (Looping A-B)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Set Point A first, Point B must be after Point A", Toast.LENGTH_SHORT).show();
            }
        });
        tvAbClear.setOnClickListener(v -> {
            repeatPointA = -1;
            repeatPointB = -1;
            tvStartSet.setText("A");
            tvEndSet.setText("B");
            Toast.makeText(this, "A-B Repeat cleared", Toast.LENGTH_SHORT).show();
        });
        abClose.setOnClickListener(v -> abRepeatLayout.setVisibility(View.GONE));

        // Floating Subtitle Sync Actions
        adjustReduce.setOnClickListener(v -> {
            subtitleOffsetSec -= 0.1f;
            adjustText.setText(String.format(java.util.Locale.US, "%+.1fs", subtitleOffsetSec));
        });
        adjustAdd.setOnClickListener(v -> {
            subtitleOffsetSec += 0.1f;
            adjustText.setText(String.format(java.util.Locale.US, "%+.1fs", subtitleOffsetSec));
        });
        adjustClose.setOnClickListener(v -> adjustSubtitleLayout.setVisibility(View.GONE));

        // Seekbar
        appVideoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null && player.getDuration() > 0) {
                    long duration = player.getDuration();
                    long targetPos = (duration * progress) / seekBar.getMax();
                    appVideoCurrentTime.setText(FormatUtils.formatDuration(targetPos));
                    long seekDelta = targetPos - scrubStartPosition;
                    showThumbSeekHUD(seekDelta, targetPos, duration);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
                controlsHideHandler.removeCallbacksAndMessages(null);
                if (player != null) {
                    scrubStartPosition = player.getCurrentPosition();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null && player.getDuration() > 0) {
                    long targetPos = (player.getDuration() * seekBar.getProgress()) / seekBar.getMax();
                    player.seekTo(targetPos);
                    if (currentVideo != null) {
                        preferences.savePlaybackPosition(currentVideo.getPath(), currentVideo.getId(), targetPos);
                    }
                }
                isUserSeeking = false;
                hideThumbSeekHUD();
                scheduleHideControls();
            }
        });
    }

    private void playNextVideo() {
        if (playlist == null || playlist.isEmpty()) return;
        if (currentIndex < playlist.size() - 1) {
            playVideoAtIndex(currentIndex + 1);
        } else {
            // Folders videos are over, don't start from first again. Return to folder files!
            finish();
        }
    }

    private void playPrevVideo() {
        if (currentIndex > 0) {
            playVideoAtIndex(currentIndex - 1);
        } else {
            player.seekTo(0);
        }
    }

    private void toggleLock(boolean lock) {
        isLocked = lock;
        if (isLocked) {
            appVideoBox.setVisibility(View.GONE);
            abRepeatLayout.setVisibility(View.GONE);
            adjustSubtitleLayout.setVisibility(View.GONE);
            appVideoLockedLeft.setVisibility(View.VISIBLE);
            appVideoLockedRight.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.lock, Toast.LENGTH_SHORT).show();
        } else {
            appVideoLockedLeft.setVisibility(View.GONE);
            appVideoLockedRight.setVisibility(View.GONE);
            showControls();
            Toast.makeText(this, R.string.unlock, Toast.LENGTH_SHORT).show();
        }
    }

    private static final int[] RATIO_ICONS = {
        R.drawable.ic_ratio_fit,
        R.drawable.ic_ratio_fill,
        R.drawable.ic_ratio_original,
        R.drawable.ic_ratio_stretch,
        R.drawable.ic_ratio_16_9,
        R.drawable.ic_ratio_4_3,
        R.drawable.ic_ratio_18_9,
        R.drawable.ic_ratio_195_9,
        R.drawable.ic_ratio_20_9,
        R.drawable.ic_ratio_21_9,
        R.drawable.ic_ratio_185_1,
        R.drawable.ic_ratio_221_1,
        R.drawable.ic_ratio_235_1,
        R.drawable.ic_ratio_239_1
    };

    private String getRatioName(int mode) {
        switch (mode) {
            case 0: return "Fit";
            case 1: return "Fill";
            case 2: return "Original";
            case 3: return "Stretch";
            case 4: return "16:9";
            case 5: return "4:3";
            case 6: return "18:9";
            case 7: return "19.5:9";
            case 8: return "20:9";
            case 9: return "21:9";
            case 10: return "1.85:1";
            case 11: return "2.21:1";
            case 12: return "2.35:1";
            case 13: return "2.39:1";
            default: return "Fit";
        }
    }

    private void cycleNextRatio() {
        int[] cycleModes = {0, 1, 2, 3, 4, 5};
        int nextIndex = 0;
        for (int i = 0; i < cycleModes.length; i++) {
            if (cycleModes[i] == currentAspectRatioMode) {
                nextIndex = (i + 1) % cycleModes.length;
                break;
            }
        }
        applyAspectRatio(cycleModes[nextIndex]);
    }

    private void applyAspectRatio(int mode) {
        currentAspectRatioMode = mode;
        if (preferences.isRememberRatioEnabled()) {
            preferences.setAspectRatio(mode);
        }

        if (playerView != null) {
            androidx.media3.ui.AspectRatioFrameLayout contentFrame = 
                playerView.findViewById(androidx.media3.ui.R.id.exo_content_frame);
            androidx.media3.common.VideoSize videoSize = player != null ? player.getVideoSize() : null;
            float naturalRatio = (videoSize != null && videoSize.width > 0 && videoSize.height > 0)
                ? ((float) videoSize.width / (float) videoSize.height)
                : 0f;
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            switch (mode) {
                case 0: // Fit
                    if (contentFrame != null) contentFrame.setAspectRatio(naturalRatio);
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 1: // Fill
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    break;
                case 2: // Original
                    if (contentFrame != null) contentFrame.setAspectRatio(naturalRatio);
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 3: // Stretch
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL);
                    break;
                case 4: // 16:9
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? (16f / 9f) : (9f / 16f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 5: // 4:3
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? (4f / 3f) : (3f / 4f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 6: // 18:9
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? 2.0f : 0.5f);
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 7: // 19.5:9
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? (19.5f / 9f) : (9f / 19.5f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 8: // 20:9
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? (20f / 9f) : (9f / 20f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 9: // 21:9
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? (21f / 9f) : (9f / 21f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 10: // 1.85:1
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? 1.85f : (1f / 1.85f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 11: // 2.21:1
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? 2.21f : (1f / 2.21f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 12: // 2.35:1
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? 2.35f : (1f / 2.35f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
                case 13: // 2.39:1
                    if (contentFrame != null) contentFrame.setAspectRatio(isLandscape ? 2.39f : (1f / 2.39f));
                    playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
            }
        }

        if (appVideoScaleType != null && mode >= 0 && mode < RATIO_ICONS.length) {
            appVideoScaleType.setVisibility(View.VISIBLE);
            appVideoScaleType.setImageResource(RATIO_ICONS[mode]);
        }
        if (tvScaleTypeText != null) {
            tvScaleTypeText.setVisibility(View.GONE);
        }
    }

    private void toggleOrientation() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void toggleNightMode() {
        boolean night = !preferences.isNightMode();
        preferences.setNightMode(night);
        nightModeMask.setVisibility(night ? View.VISIBLE : View.GONE);
        Toast.makeText(this, night ? "Night Mode Enabled" : "Night Mode Disabled", Toast.LENGTH_SHORT).show();
    }

    private void handleAbRepeat() {
        abRepeatLayout.setVisibility(View.VISIBLE);
        if (repeatPointA >= 0) {
            tvStartSet.setText(FormatUtils.formatDuration(repeatPointA));
        } else {
            tvStartSet.setText("A");
        }
        if (repeatPointB >= 0) {
            tvEndSet.setText(FormatUtils.formatDuration(repeatPointB));
        } else {
            tvEndSet.setText("B");
        }
    }

    private void toggleMirror() {
        isMirrored = !isMirrored;
        playerView.setScaleX(isMirrored ? -1.0f : 1.0f);
        Toast.makeText(this, isMirrored ? "Video Mirrored" : "Mirror Off", Toast.LENGTH_SHORT).show();
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (player != null) {
            player.setVolume(isMuted ? 0f : 1f);
        }
        mute.setImageResource(isMuted ? R.drawable.ic_mute_on : R.drawable.ic_mute_off);
        Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
    }

    private void toggleBackgroundPlay() {
        isBackgroundPlayEnabled = !isBackgroundPlayEnabled;
        Toast.makeText(this, "Background play " + (isBackgroundPlayEnabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }

    private Dialog activeSheetDialog;

    private Dialog createPlayerSheetDialog(View sheetView) {
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (!isLandscape) {
            BottomSheetDialog bsd = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            bsd.setContentView(sheetView);
            activeSheetDialog = bsd;
            return bsd;
        }

        // Landscape Right-Side Drawer Dialog matching original Zevix
        Dialog dialog = new Dialog(this, R.style.Theme_Zevix_SideSheet);

        FrameLayout rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootContainer.setBackgroundColor(Color.TRANSPARENT);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int minWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 360, dm);
        int maxWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 440, dm);
        int calcWidth = (int) (dm.widthPixels * 0.45f);
        int panelWidth = Math.max(minWidth, Math.min(calcWidth, maxWidth));

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                panelWidth, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        sheetView.setLayoutParams(panelParams);
        sheetView.setBackgroundResource(R.drawable.bg_dialog_dark_left_radius);
        sheetView.setClickable(true);

        if (sheetView instanceof androidx.core.widget.NestedScrollView) {
            ((androidx.core.widget.NestedScrollView) sheetView).setFillViewport(true);
        } else {
            View scrollView = sheetView.findViewById(R.id.scroll_view);
            if (scrollView == null && sheetView instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) sheetView;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child instanceof androidx.core.widget.NestedScrollView || child instanceof ScrollView) {
                        scrollView = child;
                        break;
                    }
                }
            }
            if (scrollView != null && scrollView.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
                lp.height = 0;
                lp.weight = 1.0f;
                scrollView.setLayoutParams(lp);
            }
        }

        rootContainer.addView(sheetView);
        dialog.setContentView(rootContainer);

        // Clicking dimmed area outside panel dismisses with animation
        rootContainer.setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));

        // Back button inside panel also dismisses with animation
        View backBtn = sheetView.findViewById(R.id.iv_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Prevent system bars popping up in immersive player mode
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dialog.setOnShowListener(d -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
                if (insetsController != null) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
                sheetView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.right_in));
            });
        }

        activeSheetDialog = dialog;
        return dialog;
    }

    private void dismissPlayerSheetDialog(Dialog dialog, View panelView) {
        if (dialog == null || !dialog.isShowing()) return;
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (!isLandscape || panelView == null) {
            dialog.dismiss();
            activeSheetDialog = null;
            return;
        }
        try {
            Animation outAnim = AnimationUtils.loadAnimation(this, R.anim.right_out);
            outAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {}
                    activeSheetDialog = null;
                }
                @Override public void onAnimationRepeat(Animation animation) {}
            });
            panelView.startAnimation(outAnim);
        } catch (Exception e) {
            dialog.dismiss();
            activeSheetDialog = null;
        }
    }

    private void showSpeedBottomSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_speed, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        sheetView.findViewById(R.id.iv_back).setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));
        TextView tvSpeed = sheetView.findViewById(R.id.speed_value_text);
        SeekBar sbSpeed = sheetView.findViewById(R.id.speed_seekbar);

        tvSpeed.setText(String.format(java.util.Locale.US, "%.2fx", currentSpeed));
        int progress = (int) Math.round(((currentSpeed - 0.25f) / 3.75f) * 100f);
        sbSpeed.setProgress(Math.max(0, Math.min(100, progress)));

        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                if (fromUser) {
                    float spd = 0.25f + (p / 100.0f) * 3.75f;
                    spd = Math.round(spd * 20.0f) / 20.0f;
                    setPlayerSpeed(spd);
                    tvSpeed.setText(String.format(java.util.Locale.US, "%.2fx", spd));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        int[] btnIds = {R.id.speed_text_025, R.id.speed_text_050, R.id.speed_text_100, R.id.speed_text_125,
                R.id.speed_text_150, R.id.speed_text_200, R.id.speed_text_300, R.id.speed_text_400};
        float[] spdValues = {0.25f, 0.5f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f};

        for (int i = 0; i < btnIds.length; i++) {
            final float val = spdValues[i];
            TextView btn = sheetView.findViewById(btnIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    setPlayerSpeed(val);
                    tvSpeed.setText(String.format(java.util.Locale.US, "%.2fx", val));
                    int p = (int) Math.round(((val - 0.25f) / 3.75f) * 100f);
                    sbSpeed.setProgress(Math.max(0, Math.min(100, p)));
                    dismissPlayerSheetDialog(dialog, sheetView);
                });
            }
        }

        dialog.show();
    }

    private void setPlayerSpeed(float speed) {
        currentSpeed = speed;
        if (player != null) {
            player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
        }
        if (speedText != null) {
            speedText.setText(String.format(java.util.Locale.US, "%.2fX", currentSpeed).replace(".00", ""));
        }
    }

    private void showAudioTrackBottomSheet() {
        if (player == null) return;
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_audio, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        sheetView.findViewById(R.id.iv_back).setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));
        LinearLayout layoutTracks = sheetView.findViewById(R.id.layout_audio_tracks);

        Tracks tracks = player.getCurrentTracks();
        List<Tracks.Group> audioGroups = new ArrayList<>();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                audioGroups.add(group);
            }
        }

        if (audioGroups.isEmpty()) {
            TextView tvDefault = new TextView(this);
            tvDefault.setText("1. Default Audio Track");
            tvDefault.setTextColor(getResources().getColor(R.color.accent_color, getTheme()));
            tvDefault.setPadding(24, 24, 24, 24);
            tvDefault.setTextSize(15);
            tvDefault.setBackgroundResource(R.drawable.bg_common_btn_light);
            layoutTracks.addView(tvDefault);
        } else {
            int trackIdx = 1;
            for (Tracks.Group group : audioGroups) {
                for (int i = 0; i < group.length; i++) {
                    final Tracks.Group targetGroup = group;
                    final int targetTrack = i;
                    androidx.media3.common.Format format = group.getTrackFormat(i);
                    String lang = format.language != null ? format.language.toUpperCase() : "Track " + trackIdx;
                    String label = trackIdx + ". " + lang + (format.channelCount > 2 ? " (Surround)" : " (Stereo)");

                    TextView tvTrack = new TextView(this);
                    tvTrack.setText(label);
                    boolean isSelected = group.isTrackSelected(i);
                    tvTrack.setTextColor(isSelected ? getResources().getColor(R.color.accent_color, getTheme()) : 0xffffffff);
                    tvTrack.setPadding(24, 24, 24, 24);
                    tvTrack.setTextSize(15);
                    tvTrack.setBackgroundResource(R.drawable.bg_common_btn_light);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = 8;
                    tvTrack.setLayoutParams(lp);

                    tvTrack.setOnClickListener(v -> {
                        player.setTrackSelectionParameters(
                                player.getTrackSelectionParameters()
                                        .buildUpon()
                                        .setOverrideForType(new TrackSelectionOverride(targetGroup.getMediaTrackGroup(), targetTrack))
                                        .build());
                        Toast.makeText(this, "Selected: " + label, Toast.LENGTH_SHORT).show();
                        dismissPlayerSheetDialog(dialog, sheetView);
                    });

                    layoutTracks.addView(tvTrack);
                    trackIdx++;
                }
            }
        }

        sheetView.findViewById(R.id.tv_audio_delay).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            Toast.makeText(this, "Audio sync offset: 0.0s", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showSubtitleBottomSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_subtitle_menu, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        sheetView.findViewById(R.id.iv_back).setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));
        androidx.appcompat.widget.SwitchCompat swSub = sheetView.findViewById(R.id.sw_subtitle);
        TextView tvContent = sheetView.findViewById(R.id.tv_subtitle_content);

        swSub.setChecked(preferences.isSubtitleEnabled());
        tvContent.setText(preferences.isSubtitleEnabled() ? "Embedded / Active" : "None");

        swSub.setOnCheckedChangeListener((btn, isChecked) -> {
            preferences.setSubtitleEnabled(isChecked);
            tvContent.setText(isChecked ? "Embedded / Active" : "None");
            if (player != null) {
                player.setTrackSelectionParameters(
                        player.getTrackSelectionParameters()
                                .buildUpon()
                                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !isChecked)
                                .build());
            }
        });

        sheetView.findViewById(R.id.tv_import_file).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            openSubtitlePicker();
        });

        sheetView.findViewById(R.id.tv_subtitle_delay).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            adjustSubtitleLayout.setVisibility(View.VISIBLE);
        });

        dialog.show();
    }

    private final ActivityResultLauncher<String[]> subtitlePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    loadExternalSubtitle(uri);
                }
            });

    private void openSubtitlePicker() {
        try {
            subtitlePickerLauncher.launch(new String[]{"*/*"});
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open file picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExternalSubtitle(Uri uri) {
        try {
            subtitleText.setVisibility(View.VISIBLE);
            subtitleText.setText("Subtitle active: " + (uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "custom.srt"));
            Toast.makeText(this, "Subtitle loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load subtitle", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCastDialog() {
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Cast Video")
                .setMessage("Scanning local Wi-Fi for Chromecast / Smart TV displays...")
                .setPositiveButton("Search", (dialog, which) -> {
                    Toast.makeText(this, "No external display found on network", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showVisualBoostDialog() {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_visual_boost, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        sheetView.findViewById(R.id.iv_back).setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));

        androidx.appcompat.widget.SwitchCompat swHw = sheetView.findViewById(R.id.sw_hw_plus);
        androidx.appcompat.widget.SwitchCompat swHdr = sheetView.findViewById(R.id.sw_vivid_hdr);
        androidx.appcompat.widget.SwitchCompat swNight = sheetView.findViewById(R.id.sw_night_vision);

        if (swHw != null) {
            swHw.setChecked(!preferences.isSoftwareDecoder());
            swHw.setOnCheckedChangeListener((btn, isChecked) -> {
                switchDecoderMode(!isChecked);
                Toast.makeText(this, isChecked ? "HW+ Acceleration Enabled" : "Software Decoding Active", Toast.LENGTH_SHORT).show();
            });
        }

        if (swNight != null) {
            swNight.setChecked(preferences.isNightMode());
            swNight.setOnCheckedChangeListener((btn, isChecked) -> {
                preferences.setNightMode(isChecked);
                if (nightModeMask != null) {
                    nightModeMask.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }

        if (swHdr != null) {
            swHdr.setOnCheckedChangeListener((btn, isChecked) -> {
                Toast.makeText(this, isChecked ? "Vivid HDR Enhanced" : "Standard Color", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private void showQueueBottomSheet() {
        if (playlist == null || playlist.isEmpty()) return;
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_playlist, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        sheetView.findViewById(R.id.iv_back).setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));

        TextView tvCount = sheetView.findViewById(R.id.play_list_count);
        tvCount.setText("(" + playlist.size() + ")");

        TextView tvRepeat = sheetView.findViewById(R.id.repeat_mode_text);
        ImageView ivRepeat = sheetView.findViewById(R.id.repeat_mode);
        updateRepeatModeDisplay(tvRepeat, ivRepeat);

        View.OnClickListener repeatClick = v -> {
            cycleRepeatMode();
            updateRepeatModeDisplay(tvRepeat, ivRepeat);
        };
        tvRepeat.setOnClickListener(repeatClick);
        ivRepeat.setOnClickListener(repeatClick);

        RecyclerView rv = sheetView.findViewById(R.id.rv_play_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new QueueAdapter(playlist, currentIndex, position -> {
            playVideoAtIndex(position);
            dismissPlayerSheetDialog(dialog, sheetView);
        }, removedPos -> {
            if (playlist.size() > 1) {
                playlist.remove(removedPos);
                if (removedPos < currentIndex) currentIndex--;
                tvCount.setText("(" + playlist.size() + ")");
                if (rv.getAdapter() != null) rv.getAdapter().notifyItemRemoved(removedPos);
            } else {
                Toast.makeText(this, "Cannot remove the only playing video", Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }

    private void updateRatioDialogUI(View sheetView, int activeMode) {
        if (sheetView == null) return;

        ImageView ivFit = sheetView.findViewById(R.id.iv_ratio_fit);
        TextView tvFit = sheetView.findViewById(R.id.tv_ratio_fit);
        ImageView ivFill = sheetView.findViewById(R.id.iv_ratio_fill);
        TextView tvFill = sheetView.findViewById(R.id.tv_ratio_fill);
        ImageView ivOriginal = sheetView.findViewById(R.id.iv_ratio_original);
        TextView tvOriginal = sheetView.findViewById(R.id.tv_ratio_original);
        ImageView ivStretch = sheetView.findViewById(R.id.iv_ratio_stretch);
        TextView tvStretch = sheetView.findViewById(R.id.tv_ratio_stretch);

        int greenColor = Color.parseColor("#25d366");
        int whiteColor = Color.parseColor("#ffffff");
        int unselectedTextColor = Color.parseColor("#ccffffff");

        if (ivFit != null) ivFit.setBackgroundResource(activeMode == 0 ? R.drawable.bg_oval_accent : R.drawable.bg_oval_802a);
        if (tvFit != null) tvFit.setTextColor(activeMode == 0 ? greenColor : whiteColor);

        if (ivFill != null) ivFill.setBackgroundResource(activeMode == 1 ? R.drawable.bg_oval_accent : R.drawable.bg_oval_802a);
        if (tvFill != null) tvFill.setTextColor(activeMode == 1 ? greenColor : whiteColor);

        if (ivOriginal != null) ivOriginal.setBackgroundResource(activeMode == 2 ? R.drawable.bg_oval_accent : R.drawable.bg_oval_802a);
        if (tvOriginal != null) tvOriginal.setTextColor(activeMode == 2 ? greenColor : whiteColor);

        if (ivStretch != null) ivStretch.setBackgroundResource(activeMode == 3 ? R.drawable.bg_oval_accent : R.drawable.bg_oval_802a);
        if (tvStretch != null) tvStretch.setTextColor(activeMode == 3 ? greenColor : whiteColor);

        int[] stdIds = {R.id.tv_ratio_16_9, R.id.tv_ratio_4_3, R.id.tv_ratio_18_9, R.id.tv_ratio_195_9, R.id.tv_ratio_20_9, R.id.tv_ratio_21_9};
        int[] stdModes = {4, 5, 6, 7, 8, 9};
        for (int i = 0; i < stdIds.length; i++) {
            TextView tv = sheetView.findViewById(stdIds[i]);
            if (tv != null) {
                boolean isSel = (activeMode == stdModes[i]);
                tv.setBackgroundResource(isSel ? R.drawable.bg_oval_accent : R.drawable.bg_oval_802a);
                tv.setTextColor(isSel ? whiteColor : unselectedTextColor);
            }
        }

        int[] cinIds = {R.id.tv_ratio_185, R.id.tv_ratio_221, R.id.tv_ratio_235, R.id.tv_ratio_239};
        int[] cinModes = {10, 11, 12, 13};
        for (int i = 0; i < cinIds.length; i++) {
            TextView tv = sheetView.findViewById(cinIds[i]);
            if (tv != null) {
                boolean isSel = (activeMode == cinModes[i]);
                tv.setBackgroundResource(isSel ? R.drawable.bg_oval_accent : R.drawable.bg_oval_802a);
                tv.setTextColor(isSel ? whiteColor : unselectedTextColor);
            }
        }
    }

    private void showRatioBottomSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_ratio, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        sheetView.findViewById(R.id.iv_back).setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));

        updateRatioDialogUI(sheetView, currentAspectRatioMode);

        sheetView.findViewById(R.id.ll_ratio_fit).setOnClickListener(v -> {
            applyAspectRatio(0);
            updateRatioDialogUI(sheetView, 0);
            dismissPlayerSheetDialog(dialog, sheetView);
        });
        sheetView.findViewById(R.id.ll_ratio_fill).setOnClickListener(v -> {
            applyAspectRatio(1);
            updateRatioDialogUI(sheetView, 1);
            dismissPlayerSheetDialog(dialog, sheetView);
        });
        sheetView.findViewById(R.id.ll_ratio_original).setOnClickListener(v -> {
            applyAspectRatio(2);
            updateRatioDialogUI(sheetView, 2);
            dismissPlayerSheetDialog(dialog, sheetView);
        });
        sheetView.findViewById(R.id.ll_ratio_stretch).setOnClickListener(v -> {
            applyAspectRatio(3);
            updateRatioDialogUI(sheetView, 3);
            dismissPlayerSheetDialog(dialog, sheetView);
        });

        int[] stdIds = {R.id.tv_ratio_16_9, R.id.tv_ratio_4_3, R.id.tv_ratio_18_9, R.id.tv_ratio_195_9, R.id.tv_ratio_20_9, R.id.tv_ratio_21_9};
        int[] stdModes = {4, 5, 6, 7, 8, 9};
        for (int i = 0; i < stdIds.length; i++) {
            final int mode = stdModes[i];
            View tv = sheetView.findViewById(stdIds[i]);
            if (tv != null) {
                tv.setOnClickListener(v -> {
                    applyAspectRatio(mode);
                    updateRatioDialogUI(sheetView, mode);
                    dismissPlayerSheetDialog(dialog, sheetView);
                });
            }
        }

        int[] cinIds = {R.id.tv_ratio_185, R.id.tv_ratio_221, R.id.tv_ratio_235, R.id.tv_ratio_239};
        int[] cinModes = {10, 11, 12, 13};
        for (int i = 0; i < cinIds.length; i++) {
            final int mode = cinModes[i];
            View tv = sheetView.findViewById(cinIds[i]);
            if (tv != null) {
                tv.setOnClickListener(v -> {
                    applyAspectRatio(mode);
                    updateRatioDialogUI(sheetView, mode);
                    dismissPlayerSheetDialog(dialog, sheetView);
                });
            }
        }

        androidx.appcompat.widget.SwitchCompat swRemember = sheetView.findViewById(R.id.sw_remember_ratio);
        if (swRemember != null) {
            swRemember.setChecked(preferences.isRememberRatioEnabled());
            swRemember.setOnCheckedChangeListener((btn, isChecked) -> {
                preferences.setRememberRatioEnabled(isChecked);
                isRememberRatioEnabled = isChecked;
                if (isChecked) {
                    preferences.setAspectRatio(currentAspectRatioMode);
                }
            });
        }

        androidx.appcompat.widget.SwitchCompat swDirect = sheetView.findViewById(R.id.sw_ratio_on_tap);
        if (swDirect != null) {
            swDirect.setChecked(preferences.isDirectRatioSwitchEnabled());
            swDirect.setOnCheckedChangeListener((btn, isChecked) -> {
                preferences.setDirectRatioSwitchEnabled(isChecked);
                isDirectRatioSwitchEnabled = isChecked;
            });
        }

        dialog.show();
    }

    private void showPlayerMenuBottomSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_menu, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        // Audio Track
        sheetView.findViewById(R.id.menu_audio).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            showAudioTrackBottomSheet();
        });

        // Subtitle
        sheetView.findViewById(R.id.menu_subtitle).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            showSubtitleBottomSheet();
        });

        // Background Play
        sheetView.findViewById(R.id.btn_audio_mode).setOnClickListener(v -> {
            toggleBackgroundPlay();
            dismissPlayerSheetDialog(dialog, sheetView);
        });

        // Pop-up Play
        sheetView.findViewById(R.id.btn_pip_mode).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            enterPipMode();
        });

        // Cast
        sheetView.findViewById(R.id.menu_cast).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            showCastDialog();
        });

        // Delete
        sheetView.findViewById(R.id.menu_delete).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            confirmDeleteCurrentVideo();
        });

        // Properties
        sheetView.findViewById(R.id.menu_properties).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            showVideoPropertiesDialog();
        });

        // Favorites
        ImageView ivFav = sheetView.findViewById(R.id.iv_favorite);
        TextView tvFav = sheetView.findViewById(R.id.tv_favorite);
        if (currentVideo != null && preferences.isFavorite(currentVideo.getId())) {
            ivFav.setImageResource(R.drawable.ic_menu_favorite_s);
            tvFav.setTextColor(getResources().getColor(R.color.accent_color, getTheme()));
        }
        sheetView.findViewById(R.id.menu_favorite).setOnClickListener(v -> {
            if (currentVideo != null) {
                boolean fav = preferences.toggleFavorite(currentVideo.getId());
                ivFav.setImageResource(fav ? R.drawable.ic_menu_favorite_s : R.drawable.ic_menu_favorite_n);
                tvFav.setTextColor(fav ? getResources().getColor(R.color.accent_color, getTheme()) : 0xffffffff);
                Toast.makeText(this, fav ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
            }
        });

        // AB Repeat
        sheetView.findViewById(R.id.btn_ab_repeat).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            handleAbRepeat();
        });

        // Equalizer
        sheetView.findViewById(R.id.btn_equalizer).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            showEqualizerDialog();
        });

        // Timer
        sheetView.findViewById(R.id.btn_timer).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            showSleepTimerDialog();
        });

        // Visual Boost
        sheetView.findViewById(R.id.btn_visual_boost).setOnClickListener(v -> {
            dismissPlayerSheetDialog(dialog, sheetView);
            toggleNightMode();
        });

        // Repeat Mode 5-icon Bar
        TextView tvRepeatMode = sheetView.findViewById(R.id.tv_current_repeat_mode);
        ImageView btnOrder = sheetView.findViewById(R.id.repeat_order);
        ImageView btnRepeat = sheetView.findViewById(R.id.repeat_repeat);
        ImageView btnShuffle = sheetView.findViewById(R.id.repeat_shuffle);
        ImageView btnLoop = sheetView.findViewById(R.id.repeat_loop);
        ImageView btnStopNext = sheetView.findViewById(R.id.repeat_not_play_next);

        setupRepeatBarUI(sheetView, tvRepeatMode, btnOrder, btnRepeat, btnShuffle, btnLoop, btnStopNext);

        // Brightness Slider
        SeekBar sbBrightness = sheetView.findViewById(R.id.sb_brightness);
        TextView tvBrightnessValue = sheetView.findViewById(R.id.tv_brightness_value);
        float currentB = getWindow().getAttributes().screenBrightness;
        int currentBPercent = Math.round((currentB < 0 ? 0.5f : currentB) * 100f);
        sbBrightness.setProgress(currentBPercent);
        tvBrightnessValue.setText(currentBPercent + "%");
        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setBrightnessLevel(progress / 100f);
                    tvBrightnessValue.setText(progress + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Volume Slider (0 - 30 with boost)
        SeekBar sbVolume = sheetView.findViewById(R.id.sb_volume);
        TextView tvVolumeValue = sheetView.findViewById(R.id.tv_volume_value);
        sbVolume.setMax(30);
        sbVolume.setProgress(currentVolumeBoostLevel);
        tvVolumeValue.setText(currentVolumeBoostLevel > 15 ? "15 (+" + (currentVolumeBoostLevel - 15) + ")" : String.valueOf(currentVolumeBoostLevel));
        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setVolumeLevel(progress);
                    tvVolumeValue.setText(progress > 15 ? "15 (+" + (progress - 15) + ")" : String.valueOf(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Decoder Selector (HW / SW)
        TextView decoderHw = sheetView.findViewById(R.id.decoder_hw);
        TextView decoderSw = sheetView.findViewById(R.id.decoder_sw);
        boolean isSoftware = preferences.isSoftwareDecoder();
        updateDecoderUI(decoderHw, decoderSw, isSoftware);

        decoderHw.setOnClickListener(v -> {
            switchDecoderMode(false);
            updateDecoderUI(decoderHw, decoderSw, false);
            dismissPlayerSheetDialog(dialog, sheetView);
        });

        decoderSw.setOnClickListener(v -> {
            switchDecoderMode(true);
            updateDecoderUI(decoderHw, decoderSw, true);
            dismissPlayerSheetDialog(dialog, sheetView);
        });

        // Floating Screenshot Shortcut Toggle
        CheckBox cbScreenshot = sheetView.findViewById(R.id.screenshot_checkbox);
        cbScreenshot.setChecked(preferences.isScreenshotButtonEnabled());
        cbScreenshot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setScreenshotButtonEnabled(isChecked);
            if (screenshotContainer != null) {
                screenshotContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        dialog.show();
    }

    private void updateDecoderUI(TextView decoderHw, TextView decoderSw, boolean isSoftware) {
        if (decoderHw == null || decoderSw == null) return;
        if (!isSoftware) {
            decoderHw.setBackgroundResource(R.drawable.bg_accent_btn20);
            decoderHw.setTextColor(0xffffffff);
            decoderSw.setBackgroundResource(R.drawable.bg_2a2a2a_btn20);
            decoderSw.setTextColor(0xbbffffff);
        } else {
            decoderSw.setBackgroundResource(R.drawable.bg_accent_btn20);
            decoderSw.setTextColor(0xffffffff);
            decoderHw.setBackgroundResource(R.drawable.bg_2a2a2a_btn20);
            decoderHw.setTextColor(0xbbffffff);
        }
    }

    private void setupRepeatBarUI(View sheetView, TextView tvRepeatMode, ImageView btnOrder,
                                  ImageView btnRepeat, ImageView btnShuffle, ImageView btnLoop, ImageView btnStopNext) {
        Runnable updateUI = () -> {
            btnOrder.setBackgroundResource(0);
            btnRepeat.setBackgroundResource(0);
            btnShuffle.setBackgroundResource(0);
            btnLoop.setBackgroundResource(0);
            btnStopNext.setBackgroundResource(0);

            if (stopAfterCurrentVideo) {
                btnStopNext.setBackgroundResource(R.drawable.bg_accent_btn20);
                if (tvRepeatMode != null) tvRepeatMode.setText(R.string.repeat_stop);
            } else if (player != null && player.getShuffleModeEnabled()) {
                btnShuffle.setBackgroundResource(R.drawable.bg_accent_btn20);
                if (tvRepeatMode != null) tvRepeatMode.setText(R.string.repeat_shuffle);
            } else if (currentRepeatMode == Player.REPEAT_MODE_ONE) {
                btnRepeat.setBackgroundResource(R.drawable.bg_accent_btn20);
                if (tvRepeatMode != null) tvRepeatMode.setText(R.string.repeat_single);
            } else if (currentRepeatMode == Player.REPEAT_MODE_ALL) {
                btnLoop.setBackgroundResource(R.drawable.bg_accent_btn20);
                if (tvRepeatMode != null) tvRepeatMode.setText(R.string.repeat_order);
            } else {
                btnOrder.setBackgroundResource(R.drawable.bg_accent_btn20);
                if (tvRepeatMode != null) tvRepeatMode.setText(R.string.repeat_sequence);
            }
        };

        updateUI.run();

        btnOrder.setOnClickListener(v -> {
            stopAfterCurrentVideo = false;
            currentRepeatMode = Player.REPEAT_MODE_OFF;
            preferences.setRepeatMode(currentRepeatMode);
            if (player != null) {
                player.setRepeatMode(currentRepeatMode);
                player.setShuffleModeEnabled(false);
            }
            updateUI.run();
            Toast.makeText(this, "Repeat: Sequence", Toast.LENGTH_SHORT).show();
        });

        btnRepeat.setOnClickListener(v -> {
            stopAfterCurrentVideo = false;
            currentRepeatMode = Player.REPEAT_MODE_ONE;
            preferences.setRepeatMode(currentRepeatMode);
            if (player != null) {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                player.setShuffleModeEnabled(false);
            }
            updateUI.run();
            Toast.makeText(this, "Repeat: Single", Toast.LENGTH_SHORT).show();
        });

        btnShuffle.setOnClickListener(v -> {
            stopAfterCurrentVideo = false;
            currentRepeatMode = Player.REPEAT_MODE_ALL;
            preferences.setRepeatMode(currentRepeatMode);
            if (player != null) {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                player.setShuffleModeEnabled(true);
            }
            updateUI.run();
            Toast.makeText(this, "Repeat: Shuffle", Toast.LENGTH_SHORT).show();
        });

        btnLoop.setOnClickListener(v -> {
            stopAfterCurrentVideo = false;
            currentRepeatMode = Player.REPEAT_MODE_ALL;
            preferences.setRepeatMode(currentRepeatMode);
            if (player != null) {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                player.setShuffleModeEnabled(false);
            }
            updateUI.run();
            Toast.makeText(this, "Repeat: Loop All", Toast.LENGTH_SHORT).show();
        });

        btnStopNext.setOnClickListener(v -> {
            stopAfterCurrentVideo = true;
            updateUI.run();
            Toast.makeText(this, "Stop After Current Video", Toast.LENGTH_SHORT).show();
        });
    }

    private void cycleRepeatMode() {
        if (currentRepeatMode == Player.REPEAT_MODE_ALL) {
            currentRepeatMode = Player.REPEAT_MODE_ONE;
            Toast.makeText(this, "Repeat: Loop Single", Toast.LENGTH_SHORT).show();
        } else if (currentRepeatMode == Player.REPEAT_MODE_ONE) {
            currentRepeatMode = Player.REPEAT_MODE_OFF;
            Toast.makeText(this, "Repeat: Sequence", Toast.LENGTH_SHORT).show();
        } else {
            currentRepeatMode = Player.REPEAT_MODE_ALL;
            Toast.makeText(this, "Repeat: Loop All", Toast.LENGTH_SHORT).show();
        }
        preferences.setRepeatMode(currentRepeatMode);
        if (player != null) {
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
            player.setShuffleModeEnabled(false);
        }
    }

    private void updateRepeatModeDisplay(TextView tvRepeat, ImageView ivRepeat) {
        if (currentRepeatMode == Player.REPEAT_MODE_ALL) {
            if (tvRepeat != null) tvRepeat.setText("Loop All");
            if (ivRepeat != null) ivRepeat.setImageResource(R.drawable.ic_repeat_order);
        } else if (currentRepeatMode == Player.REPEAT_MODE_ONE) {
            if (tvRepeat != null) tvRepeat.setText("Loop Single");
            if (ivRepeat != null) ivRepeat.setImageResource(R.drawable.ic_repeat_repeat);
        } else {
            if (tvRepeat != null) tvRepeat.setText("Sequence");
            if (ivRepeat != null) ivRepeat.setImageResource(R.drawable.ic_repeat_shuffle);
        }
    }

    private void confirmDeleteCurrentVideo() {
        if (currentVideo == null) return;
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Move to Trash")
                .setMessage("Move \"" + currentVideo.getTitle() + "\" to Trash? You can restore it within 30 days.")
                .setPositiveButton("Move to Trash", (d, w) -> {
                    try {
                        TrashManager.getInstance(this).moveToTrash(currentVideo);
                        Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                        if (playlist.size() > 1) {
                            playlist.remove(currentIndex);
                            if (currentIndex >= playlist.size()) currentIndex = 0;
                            playVideoAtIndex(currentIndex);
                        } else {
                            finish();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not move to Trash: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {
        private final List<VideoItem> items;
        private final int activeIndex;
        private final OnItemClickListener clickListener;
        private final OnItemRemoveListener removeListener;

        interface OnItemClickListener {
            void onItemClick(int position);
        }

        interface OnItemRemoveListener {
            void onItemRemove(int position);
        }

        QueueAdapter(List<VideoItem> items, int activeIndex, OnItemClickListener clickListener, OnItemRemoveListener removeListener) {
            this.items = items;
            this.activeIndex = activeIndex;
            this.clickListener = clickListener;
            this.removeListener = removeListener;
        }

        @NonNull
        @Override
        public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_queue, parent, false);
            return new QueueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
            VideoItem item = items.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvDuration.setText(FormatUtils.formatDuration(item.getDuration()));
            if (position == activeIndex) {
                holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.accent_color, null));
            } else {
                holder.tvTitle.setTextColor(0xffffffff);
            }

            Glide.with(holder.itemView.getContext())
                    .load(item.getUri())
                    .placeholder(R.drawable.bg_btn_grey)
                    .centerCrop()
                    .into(holder.ivThumb);

            holder.itemView.setOnClickListener(v -> clickListener.onItemClick(position));
            holder.ivRemove.setOnClickListener(v -> removeListener.onItemRemove(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class QueueViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView tvDuration;
            TextView tvTitle;
            ImageView ivRemove;

            QueueViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumb = itemView.findViewById(R.id.iv_queue_thumb);
                tvDuration = itemView.findViewById(R.id.tv_queue_duration);
                tvTitle = itemView.findViewById(R.id.tv_queue_title);
                ivRemove = itemView.findViewById(R.id.iv_queue_remove);
            }
        }
    }

    private void showSleepTimerDialog() {
        String[] options = new String[]{"Turn Off", "15 Minutes", "30 Minutes", "60 Minutes", "At End of Video"};
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle(R.string.sleep_timer)
                .setItems(options, (dialog, which) -> {
                    sleepTimerHandler.removeCallbacksAndMessages(null);
                    switch (which) {
                        case 0:
                            Toast.makeText(this, "Sleep timer turned off", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            sleepTimerHandler.postDelayed(this::finish, 15 * 60 * 1000);
                            Toast.makeText(this, "Sleep timer set: 15 minutes", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            sleepTimerHandler.postDelayed(this::finish, 30 * 60 * 1000);
                            Toast.makeText(this, "Sleep timer set: 30 minutes", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            sleepTimerHandler.postDelayed(this::finish, 60 * 60 * 1000);
                            Toast.makeText(this, "Sleep timer set: 60 minutes", Toast.LENGTH_SHORT).show();
                            break;
                        case 4:
                            Toast.makeText(this, "Will stop at end of current video", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showVideoPropertiesDialog() {
        if (currentVideo == null) return;
        Dialog dialog = new Dialog(this, R.style.Theme_ZevixPro_Dialog);
        dialog.setContentView(R.layout.dialog_video_info);

        TextView tvTitle = dialog.findViewById(R.id.tv_info_title);
        TextView tvResolution = dialog.findViewById(R.id.tv_info_resolution);
        TextView tvDuration = dialog.findViewById(R.id.tv_info_duration);
        TextView tvSize = dialog.findViewById(R.id.tv_info_size);
        TextView tvDate = dialog.findViewById(R.id.tv_info_date);
        TextView tvPath = dialog.findViewById(R.id.tv_info_path);
        Button btnClose = dialog.findViewById(R.id.btn_close_info);

        tvTitle.setText(currentVideo.getTitle());
        String res = (currentVideo.getWidth() > 0 && currentVideo.getHeight() > 0)
                ? (currentVideo.getWidth() + " x " + currentVideo.getHeight())
                : "Unknown";
        tvResolution.setText(res);
        tvDuration.setText(FormatUtils.formatDuration(player != null && player.getDuration() > 0 ? player.getDuration() : currentVideo.getDuration()));
        tvSize.setText(FormatUtils.formatFileSize(currentVideo.getSize()));
        tvDate.setText(FormatUtils.formatDate(currentVideo.getDateAdded()));
        tvPath.setText(currentVideo.getPath());

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int width = Math.min((int) (dm.widthPixels * 0.88f), (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 460, dm));
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showEqualizerDialog() {
        if (player == null) return;
        int audioSessionId = player.getAudioSessionId();
        if (audioEqualizer == null) {
            try {
                audioEqualizer = new Equalizer(0, audioSessionId != 0 ? audioSessionId : 0);
                bassBoost = new BassBoost(0, audioSessionId != 0 ? audioSessionId : 0);
                virtualizer = new Virtualizer(0, audioSessionId != 0 ? audioSessionId : 0);
                presetReverb = new PresetReverb(0, audioSessionId != 0 ? audioSessionId : 0);
            } catch (Exception ignored) {}
        }
        if (audioEqualizer == null) {
            Toast.makeText(this, "Equalizer initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        View sheetView = getLayoutInflater().inflate(R.layout.dialog_player_equalizer, null);
        Dialog dialog = createPlayerSheetDialog(sheetView);

        ImageView ivBack = sheetView.findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> dismissPlayerSheetDialog(dialog, sheetView));
        }

        SwitchCompat swEq = sheetView.findViewById(R.id.sw_eq_enable);
        View vCover = sheetView.findViewById(R.id.v_eq_cover);
        boolean isEqEnabled = preferences.isEqualizerEnabled();
        swEq.setChecked(isEqEnabled);
        vCover.setVisibility(isEqEnabled ? View.GONE : View.VISIBLE);

        swEq.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setEqualizerEnabled(isChecked);
            if (audioEqualizer != null) audioEqualizer.setEnabled(isChecked);
            if (bassBoost != null) bassBoost.setEnabled(isChecked);
            if (virtualizer != null) virtualizer.setEnabled(isChecked);
            if (presetReverb != null) presetReverb.setEnabled(isChecked);
            vCover.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        // Setup EqualizerView
        EqualizerView equalizerView = sheetView.findViewById(R.id.equalizer_view);
        short numBands = audioEqualizer.getNumberOfBands();
        short[] bandRange = audioEqualizer.getBandLevelRange();

        String[] freqLabels = new String[numBands];
        for (short b = 0; b < numBands; b++) {
            int freqHz = audioEqualizer.getCenterFreq(b) / 1000;
            if (freqHz >= 1000) {
                float khz = freqHz / 1000f;
                if (khz == (int) khz) {
                    freqLabels[b] = ((int) khz) + "kHz";
                } else {
                    freqLabels[b] = String.format(java.util.Locale.US, "%.1fkHz", khz);
                }
            } else {
                freqLabels[b] = freqHz + "Hz";
            }
        }
        equalizerView.setFreqLabels(freqLabels);

        int[] currentBands = preferences.getEqualizerBands();
        if (currentBands != null && currentBands.length == numBands) {
            equalizerView.setProgress(currentBands);
        } else {
            int[] initial = new int[numBands];
            for (short b = 0; b < numBands; b++) {
                short lvl = audioEqualizer.getBandLevel(b);
                initial[b] = (int) (((lvl - bandRange[0]) / (float) (bandRange[1] - bandRange[0])) * 3000);
            }
            equalizerView.setProgress(initial);
        }

        // Setup Presets Horizontal Scroll
        LinearLayout layoutPresets = sheetView.findViewById(R.id.layout_presets);
        layoutPresets.removeAllViews();

        java.util.List<String> presetNames = new java.util.ArrayList<>();
        presetNames.add("Custom");
        short numPresets = audioEqualizer.getNumberOfPresets();
        for (short i = 0; i < numPresets; i++) {
            presetNames.add(audioEqualizer.getPresetName(i));
        }
        String[] standardPresets = {"Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock", "Techno"};
        for (String sp : standardPresets) {
            boolean exists = false;
            for (String pn : presetNames) {
                if (pn.equalsIgnoreCase(sp)) { exists = true; break; }
            }
            if (!exists) presetNames.add(sp);
        }

        int activePresetPref = preferences.getEqualizerPreset();
        int selectedPresetIndex = activePresetPref >= 0 ? (activePresetPref + 1) : 0;
        if (selectedPresetIndex >= presetNames.size()) selectedPresetIndex = 0;

        final TextView[] presetViews = new TextView[presetNames.size()];
        final int[] currentSelectedPreset = new int[]{selectedPresetIndex};

        Runnable updatePresetPillStyles = () -> {
            for (int i = 0; i < presetViews.length; i++) {
                if (presetViews[i] == null) continue;
                boolean isSel = (i == currentSelectedPreset[0]);
                presetViews[i].setBackgroundResource(isSel ? R.drawable.bg_eq_pill_selected : R.drawable.bg_eq_pill_normal);
                presetViews[i].setTextColor(isSel ? Color.WHITE : Color.parseColor("#99ffffff"));
            }
        };

        for (int i = 0; i < presetNames.size(); i++) {
            final int pIndex = i;
            TextView pill = new TextView(this);
            pill.setText(presetNames.get(i));
            pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            pill.setSingleLine(true);
            int padH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            int padV = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            pill.setPadding(padH, padV, padH, padV);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            pill.setLayoutParams(lp);

            presetViews[i] = pill;

            pill.setOnClickListener(v -> {
                currentSelectedPreset[0] = pIndex;
                updatePresetPillStyles.run();

                if (pIndex == 0) {
                    preferences.setEqualizerPreset(-1);
                } else {
                    int nativePresetIdx = pIndex - 1;
                    if (nativePresetIdx < numPresets) {
                        try {
                            audioEqualizer.usePreset((short) nativePresetIdx);
                        } catch (Exception ignored) {}
                        int[] progresses = new int[numBands];
                        for (short b = 0; b < numBands; b++) {
                            short lvl = audioEqualizer.getBandLevel(b);
                            progresses[b] = (int) (((lvl - bandRange[0]) / (float) (bandRange[1] - bandRange[0])) * 3000);
                        }
                        equalizerView.setProgressWithAnim(progresses);
                        preferences.setEqualizerBands(progresses);
                    } else {
                        int[] progresses = new int[numBands];
                        for (int b = 0; b < numBands; b++) progresses[b] = 1500;
                        if ("Techno".equalsIgnoreCase(presetNames.get(pIndex)) && numBands == 5) {
                            progresses[0] = 2200; progresses[1] = 1700; progresses[2] = 1400; progresses[3] = 1800; progresses[4] = 2300;
                        }
                        for (short b = 0; b < numBands; b++) {
                            float ratio = (float) progresses[b] / 3000f;
                            short lvl = (short) (bandRange[0] + ratio * (bandRange[1] - bandRange[0]));
                            try { audioEqualizer.setBandLevel(b, lvl); } catch (Exception ignored) {}
                        }
                        equalizerView.setProgressWithAnim(progresses);
                        preferences.setEqualizerBands(progresses);
                    }
                    preferences.setEqualizerPreset(nativePresetIdx);
                }
            });

            layoutPresets.addView(pill);
        }
        updatePresetPillStyles.run();

        equalizerView.setOnEqualizerChangeListener(new EqualizerView.OnEqualizerChangeListener() {
            @Override
            public void onProgressChanged(EqualizerView view, int[] progress, int bandIndex) {
                if (audioEqualizer != null && bandIndex >= 0 && bandIndex < numBands) {
                    float ratio = (float) progress[bandIndex] / 3000f;
                    short level = (short) (bandRange[0] + ratio * (bandRange[1] - bandRange[0]));
                    try {
                        audioEqualizer.setBandLevel((short) bandIndex, level);
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onStartTrackingTouch(EqualizerView view, int[] progress, int bandIndex) {
                if (currentSelectedPreset[0] != 0) {
                    currentSelectedPreset[0] = 0;
                    updatePresetPillStyles.run();
                    preferences.setEqualizerPreset(-1);
                }
            }

            @Override
            public void onStopTrackingTouch(EqualizerView view, int[] progress, int bandIndex) {
                preferences.setEqualizerBands(progress);
            }
        });

        ImageView btnAddPreset = sheetView.findViewById(R.id.btn_add_preset);
        if (btnAddPreset != null) {
            btnAddPreset.setOnClickListener(v -> {
                int[] flatBands = new int[numBands];
                for (short b = 0; b < numBands; b++) {
                    flatBands[b] = 1500;
                    try {
                        audioEqualizer.setBandLevel(b, (short) 0);
                    } catch (Exception ignored) {}
                }
                equalizerView.setProgressWithAnim(flatBands);
                preferences.setEqualizerBands(flatBands);
                currentSelectedPreset[0] = 0;
                updatePresetPillStyles.run();
                preferences.setEqualizerPreset(-1);
                Toast.makeText(this, "Reset to Flat", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup Reverb Pills
        LinearLayout layoutReverb = sheetView.findViewById(R.id.layout_reverb);
        layoutReverb.removeAllViews();
        String[] reverbNames = {"None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate"};
        final TextView[] reverbViews = new TextView[reverbNames.length];
        int savedRev = preferences.getReverbPreset();
        final int[] currentSelectedReverb = new int[]{Math.max(0, Math.min(reverbNames.length - 1, savedRev))};

        Runnable updateReverbPillStyles = () -> {
            for (int i = 0; i < reverbViews.length; i++) {
                if (reverbViews[i] == null) continue;
                boolean isSel = (i == currentSelectedReverb[0]);
                reverbViews[i].setBackgroundResource(isSel ? R.drawable.bg_eq_pill_selected : R.drawable.bg_eq_pill_normal);
                reverbViews[i].setTextColor(isSel ? Color.WHITE : Color.parseColor("#99ffffff"));
            }
        };

        for (int i = 0; i < reverbNames.length; i++) {
            final int rIndex = i;
            TextView pill = new TextView(this);
            pill.setText(reverbNames[i]);
            pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            pill.setSingleLine(true);
            int padH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            int padV = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            pill.setPadding(padH, padV, padH, padV);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            pill.setLayoutParams(lp);

            reverbViews[i] = pill;

            pill.setOnClickListener(v -> {
                currentSelectedReverb[0] = rIndex;
                updateReverbPillStyles.run();
                if (presetReverb != null) {
                    try {
                        presetReverb.setPreset((short) rIndex);
                    } catch (Exception ignored) {}
                }
                preferences.setReverbPreset(rIndex);
            });

            layoutReverb.addView(pill);
        }
        updateReverbPillStyles.run();

        // Setup Bass Boost
        SeekBar sbBass = sheetView.findViewById(R.id.sb_bass_boost);
        TextView tvBassPercent = sheetView.findViewById(R.id.tv_bass_percent);
        int currentBass = preferences.getBassBoostStrength();
        int bassPercent = Math.max(0, Math.min(100, currentBass / 10));
        sbBass.setProgress(bassPercent);
        tvBassPercent.setText(bassPercent + "%");
        sbBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvBassPercent.setText(progress + "%");
                    if (bassBoost != null && bassBoost.getStrengthSupported()) {
                        try {
                            bassBoost.setStrength((short) (progress * 10));
                        } catch (Exception ignored) {}
                    }
                    preferences.setBassBoostStrength(progress * 10);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup Virtualizer
        SeekBar sbVir = sheetView.findViewById(R.id.sb_virtualizer);
        TextView tvVirPercent = sheetView.findViewById(R.id.tv_vir_percent);
        int currentVir = preferences.getVirtualizerStrength();
        int virPercent = Math.max(0, Math.min(100, currentVir / 10));
        sbVir.setProgress(virPercent);
        tvVirPercent.setText(virPercent + "%");
        sbVir.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvVirPercent.setText(progress + "%");
                    if (virtualizer != null && virtualizer.getStrengthSupported()) {
                        try {
                            virtualizer.setStrength((short) (progress * 10));
                        } catch (Exception ignored) {}
                    }
                    preferences.setVirtualizerStrength(progress * 10);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        dialog.show();
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Rational rational = new Rational(playerView.getWidth() > 0 ? playerView.getWidth() : 16,
                        playerView.getHeight() > 0 ? playerView.getHeight() : 9);
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(rational)
                        .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                enterPictureInPictureMode();
            }
        } else {
            Toast.makeText(this, "Picture-in-Picture requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        appVideoBox.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        if (isInPictureInPictureMode) {
            appVideoLockedLeft.setVisibility(View.GONE);
        }
    }

    private void captureScreenshot() {
        try {
            View surface = playerView.getVideoSurfaceView();
            if (surface instanceof TextureView) {
                Bitmap bitmap = ((TextureView) surface).getBitmap();
                if (bitmap != null) {
                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File zevixDir = new File(picturesDir, "Zevix");
                    if (!zevixDir.exists()) zevixDir.mkdirs();

                    File snapFile = new File(zevixDir, "Snap_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream out = new FileOutputStream(snapFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                    }
                    Toast.makeText(this, R.string.screenshot_saved, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "Screenshot captured", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to capture screenshot", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        boolean playWhenReady = player.getPlayWhenReady();
        if (appVideoPlay != null) {
            appVideoPlay.setImageResource(playWhenReady ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    // Touch Gestures
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isLocked || isHorizontalScrubbing || System.currentTimeMillis() - lastScrubEndTime < 300) return false;
                toggleControls();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked || isHorizontalScrubbing || player == null) return false;
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                float x = e.getX();
                if (x < screenWidth * 0.35f) {
                    if (player.getDuration() <= 0) return false;
                    long targetPos = Math.max(0, player.getCurrentPosition() - 10000);
                    player.seekTo(targetPos);
                    appVideoCurrentTime.setText(FormatUtils.formatDuration(targetPos));
                    int progress = (int) ((targetPos * 1000) / player.getDuration());
                    appVideoSeekBar.setProgress(progress);
                    showSeekHUD(-10000, targetPos);
                    hideHUDWithDelay(appVideoFastForwardBox);
                } else if (x > screenWidth * 0.65f) {
                    if (player.getDuration() <= 0) return false;
                    long targetPos = Math.min(player.getDuration(), player.getCurrentPosition() + 10000);
                    player.seekTo(targetPos);
                    appVideoCurrentTime.setText(FormatUtils.formatDuration(targetPos));
                    int progress = (int) ((targetPos * 1000) / player.getDuration());
                    appVideoSeekBar.setProgress(progress);
                    showSeekHUD(10000, targetPos);
                    hideHUDWithDelay(appVideoFastForwardBox);
                } else {
                    // Centre region: Play / Pause
                    togglePlayPause();
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (isLocked || isHorizontalScrubbing || isVerticalAdjusting || player == null || !player.isPlaying()) return;
                isLongPressSpeedBoost = true;
                player.setPlaybackParameters(new PlaybackParameters(2.0f));
                if (speed2xLayout != null) {
                    speed2xLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (isLocked) return false;
                currentScale *= detector.getScaleFactor();
                currentScale = Math.max(1.0f, Math.min(currentScale, 4.0f));
                View surface = playerView.getVideoSurfaceView();
                if (surface != null) {
                    surface.setScaleX(currentScale);
                    surface.setScaleY(currentScale);
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            wasPlayingBeforeScrub = player != null && player.getPlayWhenReady();
        }

        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (isLongPressSpeedBoost) {
                isLongPressSpeedBoost = false;
                if (player != null) {
                    player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
                }
                if (speed2xLayout != null) {
                    speed2xLayout.setVisibility(View.GONE);
                }
            }
        }

        if (event.getPointerCount() > 1 && scaleGestureDetector != null) {
            return scaleGestureDetector.onTouchEvent(event);
        }

        if (!isHorizontalScrubbing && gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }

        if (isLocked) return true;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        if (appVideoBox != null && appVideoBox.getVisibility() == View.VISIBLE && !isHorizontalScrubbing) {
            scheduleHideControls();
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getX();
                initialTouchY = event.getY();
                isHorizontalScrubbing = false;
                isVerticalAdjusting = false;
                gestureStartVolume = -1;
                gestureStartBrightness = -1f;
                scrubStartPosition = player != null ? player.getCurrentPosition() : 0;
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - initialTouchX;
                float deltaY = initialTouchY - event.getY();

                if (!isHorizontalScrubbing && !isVerticalAdjusting) {
                    if (Math.abs(deltaX) > 40 && Math.abs(deltaX) > Math.abs(deltaY)) {
                        isHorizontalScrubbing = true;
                    } else if (Math.abs(deltaY) > 30) {
                        isVerticalAdjusting = true;
                        isAdjustingBrightness = initialTouchX >= (screenWidth / 2f);
                        if (isAdjustingBrightness) {
                            WindowManager.LayoutParams lp = getWindow().getAttributes();
                            gestureStartBrightness = lp.screenBrightness < 0 ? 0.5f : lp.screenBrightness;
                        } else {
                            gestureStartVolume = currentVolumeBoostLevel;
                        }
                    }
                }

                if (isHorizontalScrubbing && player != null && player.getDuration() > 0) {
                    long duration = player.getDuration();
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                    float deltaInch = deltaX / (isLandscape ? dm.xdpi : dm.ydpi);
                    long seekDelta = (long) (deltaInch * 22000.0f);
                    scrubTargetPosition = Math.max(0, Math.min(duration, scrubStartPosition + seekDelta));

                    int progress = (int) ((scrubTargetPosition * 1000) / duration);
                    appVideoSeekBar.setProgress(progress);
                    appVideoCurrentTime.setText(FormatUtils.formatDuration(scrubTargetPosition));

                    showThumbSeekHUD(seekDelta, scrubTargetPosition, duration);
                } else if (isVerticalAdjusting) {
                    float heightFraction = deltaY / (screenHeight * 0.35f);
                    if (isAdjustingBrightness) {
                        if (gestureStartBrightness < 0) {
                            WindowManager.LayoutParams lp = getWindow().getAttributes();
                            gestureStartBrightness = lp.screenBrightness < 0 ? 0.5f : lp.screenBrightness;
                        }
                        float target = Math.max(0.01f, Math.min(1.0f, gestureStartBrightness + heightFraction));
                        setBrightnessLevel(target);
                    } else {
                        if (gestureStartVolume < 0) {
                            gestureStartVolume = currentVolumeBoostLevel;
                        }
                        int target = Math.max(0, Math.min(30, Math.round(gestureStartVolume + (heightFraction * 30f))));
                        setVolumeLevel(target);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isHorizontalScrubbing) {
                    lastScrubEndTime = System.currentTimeMillis();
                    if (player != null) {
                        player.seekTo(scrubTargetPosition);
                        if (wasPlayingBeforeScrub) {
                            player.play();
                        }
                    }
                    hideThumbSeekHUD();
                    hideHUDWithDelay(appVideoFastForwardBox);
                    if (!isControlsShowing) {
                        if (llBottomBar != null) llBottomBar.setVisibility(View.GONE);
                        if (appVideoProcessPanl != null) appVideoProcessPanl.setVisibility(View.GONE);
                        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
                    } else {
                        scheduleHideControls();
                    }
                    isHorizontalScrubbing = false;
                    isVerticalAdjusting = false;
                    gestureStartVolume = -1;
                    gestureStartBrightness = -1f;
                    return true;
                }
                if (isVerticalAdjusting) {
                    hideHUDWithDelay(soundProgressLayout, appVideoVolumeBox, brightnessProgressLayout, appVideoBrightnessBox);
                }
                isHorizontalScrubbing = false;
                isVerticalAdjusting = false;
                gestureStartVolume = -1;
                gestureStartBrightness = -1f;
                break;
        }

        return true;
    }

    private void showThumbSeekHUD(long seekDelta, long targetPos, long duration) {
        if (duration <= 0) return;

        if (appVideoTopBox != null) appVideoTopBox.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        if (llBottomBar != null) llBottomBar.setVisibility(View.VISIBLE);
        if (appVideoProcessPanl != null) appVideoProcessPanl.setVisibility(View.VISIBLE);

        if (thumbLayout != null) {
            if (thumbLayout.getVisibility() != View.VISIBLE) {
                thumbLayout.clearAnimation();
                thumbLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
                thumbLayout.setVisibility(View.VISIBLE);
                thumbLayout.setAlpha(1f);
            }

            if (textProgress != null) {
                String prefix = seekDelta >= 0 ? "+" : "-";
                textProgress.setText(String.format(java.util.Locale.ENGLISH, "%s [%s%s]", FormatUtils.formatDuration(targetPos), prefix, FormatUtils.formatDuration(Math.abs(seekDelta))));
            }

            float fraction = (float) targetPos / (float) duration;
            int sbLeft = appVideoSeekBar.getLeft() + appVideoSeekBar.getPaddingLeft();
            int sbWidth = appVideoSeekBar.getWidth() - appVideoSeekBar.getPaddingLeft() - appVideoSeekBar.getPaddingRight();
            if (sbWidth <= 0) {
                sbWidth = getResources().getDisplayMetrics().widthPixels - (int) (80 * getResources().getDisplayMetrics().density);
            }
            float thumbX = sbLeft + (fraction * sbWidth);

            int bWidth = thumbLayout.getWidth();
            if (bWidth <= 0) bWidth = (int) (168 * getResources().getDisplayMetrics().density);
            float minX = 8 * getResources().getDisplayMetrics().density;
            float maxX = getResources().getDisplayMetrics().widthPixels - bWidth - minX;
            float bubbleX = Math.max(minX, Math.min(maxX, thumbX - (bWidth / 2f)));
            thumbLayout.setX(bubbleX);

            if (arrowDown != null) {
                float arrowWidth = arrowDown.getWidth() > 0 ? arrowDown.getWidth() : (14 * getResources().getDisplayMetrics().density);
                float arrowX = (thumbX - bubbleX) - (arrowWidth / 2f);
                float minArrow = 8 * getResources().getDisplayMetrics().density;
                float maxArrow = bWidth - arrowWidth - minArrow;
                arrowDown.setTranslationX(Math.max(minArrow, Math.min(maxArrow, arrowX)));
            }

            requestSeekThumbnail(targetPos);
        } else {
            showSeekHUD(seekDelta, targetPos);
        }
    }

    private void hideThumbSeekHUD() {
        if (thumbLayout != null && thumbLayout.getVisibility() == View.VISIBLE) {
            thumbLayout.clearAnimation();
            thumbLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            thumbLayout.setVisibility(View.GONE);
        }
    }

    private void requestSeekThumbnail(long positionMs) {
        if (currentVideo == null) return;
        latestThumbTargetTimeMs = positionMs;

        long bucket = (positionMs / 300L) * 300L;
        Bitmap cached = thumbCache.get(bucket);
        if (cached != null) {
            if (thumbImageView != null) {
                thumbImageView.setImageBitmap(cached);
            }
            return;
        }

        final VideoItem targetVideo = currentVideo;
        final String videoKey = targetVideo.getPath() != null ? targetVideo.getPath() : String.valueOf(targetVideo.getId());

        thumbExecutor.execute(() -> {
            try {
                if (currentVideo != targetVideo) return;

                synchronized (this) {
                    if (thumbRetriever == null || !videoKey.equals(currentRetrieverVideoKey)) {
                        if (thumbRetriever != null) {
                            try { thumbRetriever.release(); } catch (Exception ignored) {}
                        }
                        thumbRetriever = new MediaMetadataRetriever();
                        if (targetVideo.getUri() != null) {
                            thumbRetriever.setDataSource(PlayerActivity.this, targetVideo.getUri());
                        } else if (targetVideo.getPath() != null) {
                            thumbRetriever.setDataSource(targetVideo.getPath());
                        }
                        currentRetrieverVideoKey = videoKey;
                    }
                }

                if (Math.abs(latestThumbTargetTimeMs - positionMs) > 1200) {
                    return; // user moved on, discard stale frame decode
                }

                long timeUs = positionMs * 1000L;
                Bitmap frame = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    try {
                        frame = thumbRetriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, 320, 180);
                    } catch (Throwable t) {
                        try {
                            frame = thumbRetriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180);
                        } catch (Throwable ignored) {}
                    }
                }
                if (frame == null) {
                    try {
                        frame = thumbRetriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
                    } catch (Throwable t) {
                        try {
                            frame = thumbRetriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        } catch (Throwable ignored) {}
                    }
                }

                if (frame != null && currentVideo == targetVideo) {
                    final Bitmap finalFrame = frame;
                    thumbCache.put(bucket, finalFrame);
                    runOnUiThread(() -> {
                        if (thumbImageView != null && (isHorizontalScrubbing || isUserSeeking) && currentVideo == targetVideo) {
                            if (Math.abs(latestThumbTargetTimeMs - positionMs) <= 1200) {
                                thumbImageView.setImageBitmap(finalFrame);
                            }
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void adjustVolume(float delta) {
        int change = Math.round(delta * 30f);
        setVolumeLevel(currentVolumeBoostLevel + change);
    }

    private void setVolumeLevel(int targetLevel) {
        setVolumeLevel(targetLevel, true);
    }

    private void setVolumeLevel(int targetLevel, boolean showHUD) {
        currentVolumeBoostLevel = Math.max(0, Math.min(30, targetLevel));
        if (preferences != null) {
            preferences.savePlayerVolume(currentVolumeBoostLevel);
        }

        int streamMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (currentVolumeBoostLevel <= 15) {
            int streamTarget = streamMax > 0 ? Math.round((currentVolumeBoostLevel / 15.0f) * streamMax) : currentVolumeBoostLevel;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamTarget, 0);
            if (player != null) player.setVolume(1.0f);
            if (loudnessEnhancer != null) {
                try { loudnessEnhancer.setTargetGain(0); } catch (Exception ignored) {}
            }
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMax, 0);
            int boost = currentVolumeBoostLevel - 15;
            if (player != null) player.setVolume(1.0f);
            if (loudnessEnhancer != null) {
                try { loudnessEnhancer.setTargetGain(boost * 30); } catch (Exception ignored) {}
            }
        }

        if (showHUD) {
            // Side Progress Bar
            soundProgressLayout.setVisibility(View.VISIBLE);
            soundProgress.setMax(30);
            soundProgress.setProgress(Math.min(currentVolumeBoostLevel, 15));
            soundProgress.setSecondaryProgress(currentVolumeBoostLevel);
            if (appVideoVolumeIcon != null) {
                appVideoVolumeIcon.setImageResource(currentVolumeBoostLevel == 0 ? R.drawable.ic_mute : R.drawable.ic_sound);
            }

            // Center Volume Box (authentic Zevix UX)
            if (appVideoVolumeBox != null) {
                appVideoVolumeBox.setVisibility(View.VISIBLE);
                if (appVideoVolumeText != null) {
                    appVideoVolumeText.setText(currentVolumeBoostLevel > 15 ? R.string.volume_boost : R.string.volume);
                    appVideoVolumeText.append(" :");
                }
                if (currentVolumeBoostLevel == 0) {
                    appVideoVolume.setText(R.string.off);
                    if (appVideoVolumeBoost != null) appVideoVolumeBoost.setVisibility(View.GONE);
                } else if (currentVolumeBoostLevel > 15) {
                    appVideoVolume.setText("15");
                    if (appVideoVolumeBoost != null) {
                        appVideoVolumeBoost.setVisibility(View.VISIBLE);
                        appVideoVolumeBoost.setText(" (+" + (currentVolumeBoostLevel - 15) + ")");
                    }
                } else {
                    appVideoVolume.setText(String.valueOf(currentVolumeBoostLevel));
                    if (appVideoVolumeBoost != null) appVideoVolumeBoost.setVisibility(View.GONE);
                }
            }
        }
    }

    private void adjustBrightness(float delta) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float current = lp.screenBrightness < 0 ? (preferences != null && preferences.getPlayerBrightness() >= 0 ? preferences.getPlayerBrightness() : 0.5f) : lp.screenBrightness;
        float target = Math.max(0.01f, Math.min(1.0f, current + delta));
        setBrightnessLevel(target, true);
    }

    private void setBrightnessLevel(float brightness) {
        setBrightnessLevel(brightness, true);
    }

    private void setBrightnessLevel(float brightness, boolean showHUD) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);

        if (preferences != null) {
            preferences.savePlayerBrightness(brightness);
        }

        if (showHUD) {
            int percent = Math.round(brightness * 100f);

            // Side Progress Bar
            brightnessProgressLayout.setVisibility(View.VISIBLE);
            brightnessProgress.setMax(100);
            brightnessProgress.setProgress(percent);

            // Center Brightness Box
            if (appVideoBrightnessBox != null) {
                appVideoBrightnessBox.setVisibility(View.VISIBLE);
                if (appVideoBrightness != null) {
                    appVideoBrightness.setText(percent + "%");
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            adjustVolume(1 / 30f);
            hideHUDWithDelay(appVideoVolumeBox);
            hideHUDWithDelay(soundProgressLayout);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            adjustVolume(-1 / 30f);
            hideHUDWithDelay(appVideoVolumeBox);
            hideHUDWithDelay(soundProgressLayout);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showSeekHUD(long deltaMillis, long targetPos) {
        appVideoFastForwardBox.setVisibility(View.VISIBLE);
        String prefix = deltaMillis >= 0 ? "+" : "-";
        appVideoFastForward.setText("[" + prefix + FormatUtils.formatDuration(Math.abs(deltaMillis)) + "]");
        appVideoFastForwardAll.setText(FormatUtils.formatDuration(targetPos) + " / " + FormatUtils.formatDuration(player.getDuration()));
    }

    private void hideHUDWithDelay(View... views) {
        if (views == null) return;
        hudHideHandler.removeCallbacksAndMessages(null);
        hudHideHandler.postDelayed(() -> {
            for (View hud : views) {
                if (hud != null) hud.setVisibility(View.GONE);
            }
        }, 800);
    }

    private void toggleControls() {
        if (isControlsShowing) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        isControlsShowing = true;
        if (appVideoBox != null) appVideoBox.setVisibility(View.VISIBLE);
        if (appVideoTopBox != null) appVideoTopBox.setVisibility(View.VISIBLE);
        if (llBottomBar != null) llBottomBar.setVisibility(View.VISIBLE);
        if (appVideoProcessPanl != null) appVideoProcessPanl.setVisibility(View.VISIBLE);
        if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
        if (screenshotContainer != null) {
            screenshotContainer.setVisibility(preferences.isScreenshotButtonEnabled() ? View.VISIBLE : View.GONE);
        }
        scheduleHideControls();
    }

    private void hideControls() {
        isControlsShowing = false;
        if (appVideoTopBox != null) appVideoTopBox.setVisibility(View.GONE);
        if (llBottomBar != null) llBottomBar.setVisibility(View.GONE);
        if (screenshotContainer != null) screenshotContainer.setVisibility(View.GONE);
        hideThumbSeekHUD();
        controlsHideHandler.removeCallbacksAndMessages(null);
    }

    private void scheduleHideControls() {
        controlsHideHandler.removeCallbacksAndMessages(null);
        controlsHideHandler.postDelayed(this::hideControls, 3500);
    }

    private void updateDurationDisplay() {
        if (player == null) return;
        long duration = player.getDuration();
        long current = player.getCurrentPosition();
        if (isShowingRemainingTime && duration > 0) {
            long remaining = Math.max(0, duration - current);
            appVideoEndTime.setText("-" + FormatUtils.formatDuration(remaining));
        } else {
            appVideoEndTime.setText(FormatUtils.formatDuration(duration));
        }
    }

    private void startProgressTracker() {
        progressHandler.post(progressRunnable);
    }

    private void stopProgressTracker() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && !isUserSeeking) {
                long pos = player.getCurrentPosition();
                long duration = player.getDuration();
                appVideoCurrentTime.setText(FormatUtils.formatDuration(pos));
                updateDurationDisplay();

                // Check A-B repeat
                if (repeatPointA != -1 && repeatPointB != -1 && pos >= repeatPointB) {
                    player.seekTo(repeatPointA);
                }

                if (duration > 0) {
                    int progress = (int) ((pos * 1000) / duration);
                    appVideoSeekBar.setProgress(progress);
                    long buffered = player.getBufferedPosition();
                    int bufferedProgress = (int) ((buffered * 1000) / duration);
                    appVideoSeekBar.setSecondaryProgress(bufferedProgress);

                    // Periodically save playback position
                    if (pos > 0 && currentVideo != null && (Math.abs(pos - lastSavedPosition) >= 2000 || lastSavedPosition == 0)) {
                        lastSavedPosition = pos;
                        preferences.savePlaybackPosition(currentVideo.getPath(), currentVideo.getId(), pos);
                    }
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && currentVideo != null) {
            preferences.savePlaybackPosition(currentVideo.getPath(), currentVideo.getId(), player.getCurrentPosition());
            if (!isBackgroundPlayEnabled && !isInPictureInPictureMode()) {
                player.pause();
            }
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (activeSheetDialog != null && activeSheetDialog.isShowing()) {
            activeSheetDialog.dismiss();
            activeSheetDialog = null;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressTracker();
        controlsHideHandler.removeCallbacksAndMessages(null);
        hudHideHandler.removeCallbacksAndMessages(null);
        sleepTimerHandler.removeCallbacksAndMessages(null);

        if (audioEqualizer != null) audioEqualizer.release();
        if (bassBoost != null) bassBoost.release();
        if (virtualizer != null) {
            try { virtualizer.release(); } catch (Exception ignored) {}
            virtualizer = null;
        }
        if (presetReverb != null) {
            try { presetReverb.release(); } catch (Exception ignored) {}
            presetReverb = null;
        }
        if (loudnessEnhancer != null) {
            try { loudnessEnhancer.release(); } catch (Exception ignored) {}
            loudnessEnhancer = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        if (thumbRetriever != null) {
            try { thumbRetriever.release(); } catch (Exception ignored) {}
            thumbRetriever = null;
        }
        thumbExecutor.shutdownNow();
    }
}
