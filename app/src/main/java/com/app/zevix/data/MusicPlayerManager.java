package com.app.zevix.data;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.app.zevix.model.MusicItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicPlayerManager {

    public static final int LOOP_ALL = 0;
    public static final int LOOP_ONE = 1;
    public static final int LOOP_SHUFFLE = 2;

    public interface OnMusicStateChangeListener {
        void onSongChanged(MusicItem item);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressUpdate(long currentPos, long duration);
    }

    private static MusicPlayerManager instance;

    private ExoPlayer player;
    private List<MusicItem> originalQueue = new ArrayList<>();
    private List<MusicItem> playQueue = new ArrayList<>();
    private int currentIndex = -1;
    private int loopMode = LOOP_ALL;
    private float currentSpeed = 1.0f;

    private final List<OnMusicStateChangeListener> listeners = new ArrayList<>();

    public static synchronized MusicPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayerManager(context.getApplicationContext());
        }
        return instance;
    }

    private MusicPlayerManager(Context context) {
        player = new ExoPlayer.Builder(context).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    onSongEnded();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                for (OnMusicStateChangeListener l : listeners) {
                    l.onPlaybackStateChanged(isPlaying);
                }
            }
        });

        // Background progress tracker
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    long cur = player.getCurrentPosition();
                    long dur = player.getDuration();
                    for (OnMusicStateChangeListener l : listeners) {
                        l.onProgressUpdate(cur, dur);
                    }
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 500);
            }
        });
    }

    public void addListener(OnMusicStateChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        if (getCurrentSong() != null) {
            listener.onSongChanged(getCurrentSong());
            listener.onPlaybackStateChanged(isPlaying());
        }
    }

    public void removeListener(OnMusicStateChangeListener listener) {
        listeners.remove(listener);
    }

    public void playSong(List<MusicItem> list, int index) {
        if (list == null || list.isEmpty() || index < 0 || index >= list.size()) return;
        this.originalQueue = new ArrayList<>(list);
        this.playQueue = new ArrayList<>(list);
        this.currentIndex = index;

        if (loopMode == LOOP_SHUFFLE) {
            MusicItem current = originalQueue.get(index);
            Collections.shuffle(playQueue);
            this.currentIndex = playQueue.indexOf(current);
        }

        playCurrent();
    }

    private void playCurrent() {
        if (currentIndex < 0 || currentIndex >= playQueue.size()) return;
        MusicItem item = playQueue.get(currentIndex);
        player.setMediaItem(MediaItem.fromUri(item.getUri()));
        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
        player.prepare();
        player.play();

        for (OnMusicStateChangeListener l : listeners) {
            l.onSongChanged(item);
            l.onPlaybackStateChanged(true);
        }
    }

    private void onSongEnded() {
        if (loopMode == LOOP_ONE) {
            player.seekTo(0);
            player.play();
        } else {
            playNext();
        }
    }

    public void togglePlayPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    public void playNext() {
        if (playQueue.isEmpty()) return;
        if (currentIndex < playQueue.size() - 1) {
            currentIndex++;
        } else {
            currentIndex = 0; // Wrap around
        }
        playCurrent();
    }

    public void playPrev() {
        if (playQueue.isEmpty()) return;
        if (player.getCurrentPosition() > 3000) {
            player.seekTo(0);
        } else if (currentIndex > 0) {
            currentIndex--;
            playCurrent();
        } else {
            currentIndex = playQueue.size() - 1;
            playCurrent();
        }
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public void seekBy(long deltaMs) {
        long target = Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() + deltaMs));
        player.seekTo(target);
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public MusicItem getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < playQueue.size()) {
            return playQueue.get(currentIndex);
        }
        return null;
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    public int getAudioSessionId() {
        return player != null ? player.getAudioSessionId() : 0;
    }

    public int getLoopMode() {
        return loopMode;
    }

    public int cycleLoopMode() {
        loopMode = (loopMode + 1) % 3;
        return loopMode;
    }

    public float getSpeed() {
        return currentSpeed;
    }

    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        if (player != null) {
            player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
        }
    }

    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    public List<MusicItem> getPlayQueue() {
        return playQueue;
    }
}

