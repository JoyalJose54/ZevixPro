package com.app.zevix.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.adapter.VideoAdapter;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.data.PlaylistManager;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.FormatUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class PlaylistDetailActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    public static final String EXTRA_PLAYLIST_TYPE = "extra_pl_type";
    public static final String EXTRA_PLAYLIST_NAME = "extra_pl_name";

    public static final int TYPE_FAVORITES = 0;
    public static final int TYPE_HISTORY = 1;
    public static final int TYPE_CUSTOM = 2;

    private ImageView btnBack;
    private TextView tvPlaylistTitle;
    private TextView tvPlaylistSub;
    private ImageView btnStyle;
    private ImageView btnAddVideo;
    private View btnPlayAll;
    private View btnShuffle;
    private RecyclerView rvPlaylistVideos;
    private View layoutEmpty;
    private Button btnAddVideosEmpty;

    private int playlistType;
    private String playlistName;
    private VideoAdapter adapter;
    private List<VideoItem> playlistVideos = new ArrayList<>();
    private PlaybackPreferences preferences;
    private boolean isGridMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        preferences = new PlaybackPreferences(this);
        playlistType = getIntent().getIntExtra(EXTRA_PLAYLIST_TYPE, TYPE_CUSTOM);
        playlistName = getIntent().getStringExtra(EXTRA_PLAYLIST_NAME);
        if (playlistName == null) playlistName = "Playlist";

        btnBack = findViewById(R.id.btn_back);
        tvPlaylistTitle = findViewById(R.id.tv_playlist_title);
        tvPlaylistSub = findViewById(R.id.tv_playlist_sub);
        btnStyle = findViewById(R.id.btn_style);
        btnAddVideo = findViewById(R.id.btn_add_video_to_pl);
        btnPlayAll = findViewById(R.id.btn_pl_play_all);
        btnShuffle = findViewById(R.id.btn_pl_shuffle);
        rvPlaylistVideos = findViewById(R.id.rv_playlist_videos);
        layoutEmpty = findViewById(R.id.layout_empty);
        btnAddVideosEmpty = findViewById(R.id.btn_add_videos_empty);

        tvPlaylistTitle.setText(playlistName);
        btnBack.setOnClickListener(v -> finish());
        btnStyle.setOnClickListener(v -> toggleLayoutStyle());

        if (playlistType == TYPE_HISTORY) {
            btnAddVideo.setVisibility(View.GONE);
            btnAddVideosEmpty.setVisibility(View.GONE);
        } else {
            btnAddVideo.setOnClickListener(v -> showAddVideosDialog());
            btnAddVideosEmpty.setOnClickListener(v -> showAddVideosDialog());
        }

        btnPlayAll.setOnClickListener(v -> {
            if (!playlistVideos.isEmpty()) {
                onVideoClick(playlistVideos.get(0), 0, playlistVideos);
            } else {
                Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show();
            }
        });

        btnShuffle.setOnClickListener(v -> {
            if (!playlistVideos.isEmpty()) {
                List<VideoItem> shuffled = new ArrayList<>(playlistVideos);
                Collections.shuffle(shuffled);
                onVideoClick(shuffled.get(0), 0, shuffled);
            }
        });

        rvPlaylistVideos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoAdapter(this, new ArrayList<>(), this);
        rvPlaylistVideos.setAdapter(adapter);

        loadPlaylistVideos();
    }

    private void toggleLayoutStyle() {
        isGridMode = !isGridMode;
        if (isGridMode) {
            btnStyle.setImageResource(R.drawable.ic_layout_list);
            rvPlaylistVideos.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            btnStyle.setImageResource(R.drawable.ic_layout_grid);
            rvPlaylistVideos.setLayoutManager(new LinearLayoutManager(this));
        }
        adapter.notifyDataSetChanged();
    }

    private void loadPlaylistVideos() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> allVideos = MediaStoreRepository.getAllVideos(this, preferences.getSortOrder());
            List<VideoItem> filtered = new ArrayList<>();

            if (playlistType == TYPE_FAVORITES) {
                for (VideoItem v : allVideos) {
                    if (preferences.isFavorite(v.getId())) {
                        filtered.add(v);
                    }
                }
            } else if (playlistType == TYPE_HISTORY) {
                // Return videos with saved playback position or first 20
                for (VideoItem v : allVideos) {
                    if (preferences.getPlaybackPosition(v.getId()) > 0) {
                        filtered.add(v);
                    }
                }
                if (filtered.isEmpty()) {
                    filtered.addAll(allVideos.subList(0, Math.min(10, allVideos.size())));
                }
            } else {
                Set<Long> ids = PlaylistManager.getInstance(this).getVideoIdsForPlaylist(playlistName);
                for (VideoItem v : allVideos) {
                    if (ids.contains(v.getId())) {
                        filtered.add(v);
                    }
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                playlistVideos = filtered;
                adapter.updateData(playlistVideos);
                int count = playlistVideos.size();
                tvPlaylistSub.setText(count + (count == 1 ? " video" : " videos"));
                layoutEmpty.setVisibility(playlistVideos.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showAddVideosDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> allVideos = MediaStoreRepository.getAllVideos(this, MediaStoreRepository.SORT_DATE_DESC);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (allVideos.isEmpty()) {
                    Toast.makeText(this, "No videos found on device", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] titles = new String[allVideos.size()];
                boolean[] checked = new boolean[allVideos.size()];

                Set<Long> existingIds = (playlistType == TYPE_CUSTOM)
                        ? PlaylistManager.getInstance(this).getVideoIdsForPlaylist(playlistName)
                        : null;

                for (int i = 0; i < allVideos.size(); i++) {
                    VideoItem v = allVideos.get(i);
                    titles[i] = v.getTitle();
                    if (playlistType == TYPE_FAVORITES) {
                        checked[i] = preferences.isFavorite(v.getId());
                    } else if (existingIds != null) {
                        checked[i] = existingIds.contains(v.getId());
                    }
                }

                new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                        .setTitle("Add Videos to " + playlistName)
                        .setMultiChoiceItems(titles, checked, (d, which, isChecked) -> checked[which] = isChecked)
                        .setPositiveButton("Save", (d, which) -> {
                            for (int i = 0; i < checked.length; i++) {
                                VideoItem v = allVideos.get(i);
                                if (checked[i]) {
                                    if (playlistType == TYPE_FAVORITES) {
                                        preferences.addFavorite(v.getId());
                                    } else {
                                        PlaylistManager.getInstance(this).addVideoToPlaylist(playlistName, v.getId());
                                    }
                                } else {
                                    if (playlistType == TYPE_FAVORITES) {
                                        preferences.removeFavorite(v.getId());
                                    } else {
                                        PlaylistManager.getInstance(this).removeVideoFromPlaylist(playlistName, v.getId());
                                    }
                                }
                            }
                            loadPlaylistVideos();
                            Toast.makeText(this, "Playlist updated", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    @Override
    public void onVideoClick(VideoItem video, int position, List<VideoItem> currentList) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_PLAYLIST, (Serializable) currentList);
        intent.putExtra(PlayerActivity.EXTRA_CURRENT_INDEX, position);
        startActivity(intent);
    }

    @Override
    public void onMoreClick(VideoItem video, View anchorView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_video, null);
        dialog.setContentView(sheet);

        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        TextView title = sheet.findViewById(R.id.sheet_title);
        title.setText(video.getTitle());

        // Replace "Add to playlist" with "Remove from playlist"
        TextView tvPlAction = sheet.findViewById(R.id.action_add_to_playlist);
        tvPlAction.setText("Remove from playlist");
        tvPlAction.setOnClickListener(v -> {
            dialog.dismiss();
            if (playlistType == TYPE_FAVORITES) {
                preferences.removeFavorite(video.getId());
            } else {
                PlaylistManager.getInstance(this).removeVideoFromPlaylist(playlistName, video.getId());
            }
            Toast.makeText(this, "Removed from " + playlistName, Toast.LENGTH_SHORT).show();
            loadPlaylistVideos();
        });

        sheet.findViewById(R.id.action_convert_mp3).setOnClickListener(v -> {
            dialog.dismiss();
            onVideoClick(video, playlistVideos.indexOf(video), playlistVideos);
        });

        sheet.findViewById(R.id.action_share).setOnClickListener(v -> {
            dialog.dismiss();
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("video/*");
            share.putExtra(Intent.EXTRA_STREAM, video.getUri());
            startActivity(Intent.createChooser(share, "Share Video"));
        });

        sheet.findViewById(R.id.action_info).setOnClickListener(v -> {
            dialog.dismiss();
            showVideoInfoDialog(video);
        });

        dialog.show();
    }

    private void showVideoInfoDialog(VideoItem video) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_video_info);

        TextView tvTitle = dialog.findViewById(R.id.tv_info_title);
        TextView tvResolution = dialog.findViewById(R.id.tv_info_resolution);
        TextView tvDuration = dialog.findViewById(R.id.tv_info_duration);
        TextView tvSize = dialog.findViewById(R.id.tv_info_size);
        TextView tvDate = dialog.findViewById(R.id.tv_info_date);
        TextView tvPath = dialog.findViewById(R.id.tv_info_path);
        TextView tvFormat = dialog.findViewById(R.id.tv_info_format);
        View btnClose = dialog.findViewById(R.id.btn_close_info);

        tvTitle.setText(video.getTitle());
        String res = (video.getWidth() > 0 && video.getHeight() > 0)
                ? (video.getWidth() + " x " + video.getHeight())
                : "Unknown";
        tvResolution.setText(res);
        tvDuration.setText(FormatUtils.formatDuration(video.getDuration()));
        tvSize.setText(FormatUtils.formatFileSize(video.getSize()));
        tvDate.setText(FormatUtils.formatDate(video.getDateAdded()));
        tvPath.setText(video.getPath());

        String format = "MP4";
        if (video.getPath() != null && video.getPath().contains(".")) {
            format = video.getPath().substring(video.getPath().lastIndexOf(".") + 1).toUpperCase();
        }
        if (tvFormat != null) tvFormat.setText(format);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}

