package com.app.zevix.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.zevix.R;
import com.app.zevix.adapter.VideoAdapter;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.FormatUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VideosFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvVideos;
    private TextView tvEmpty;
    private VideoAdapter adapter;
    private PlaybackPreferences preferences;
    private List<VideoItem> fullVideoList = new ArrayList<>();
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos, container, false);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        rvVideos = view.findViewById(R.id.rv_videos);
        tvEmpty = view.findViewById(R.id.tv_empty);

        preferences = new PlaybackPreferences(requireContext());
        rvVideos.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new VideoAdapter(getContext(), new ArrayList<>(), this);
        rvVideos.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.accent_color);
        swipeRefresh.setOnRefreshListener(this::loadVideos);

        loadVideos();
        return view;
    }

    public void loadVideos() {
        if (getContext() == null) return;
        swipeRefresh.setRefreshing(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            int sortOrder = preferences.getSortOrder();
            List<VideoItem> videos = MediaStoreRepository.getAllVideos(requireContext(), sortOrder);
            new Handler(Looper.getMainLooper()).post(() -> {
                swipeRefresh.setRefreshing(false);
                fullVideoList = videos;
                applyFilter(currentQuery);
            });
        });
    }

    public void filter(String query) {
        this.currentQuery = query != null ? query.trim().toLowerCase() : "";
        applyFilter(currentQuery);
    }

    private void applyFilter(String query) {
        if (query.isEmpty()) {
            adapter.updateData(fullVideoList);
            tvEmpty.setVisibility(fullVideoList.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            List<VideoItem> filtered = new ArrayList<>();
            for (VideoItem video : fullVideoList) {
                if (video.getTitle().toLowerCase().contains(query)) {
                    filtered.add(video);
                }
            }
            adapter.updateData(filtered);
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onVideoClick(VideoItem video, int position, List<VideoItem> currentList) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_PLAYLIST, (Serializable) currentList);
        intent.putExtra(PlayerActivity.EXTRA_CURRENT_INDEX, position);
        startActivity(intent);
    }

    @Override
    public void onMoreClick(VideoItem video, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add(0, 1, 0, "Play");
        popup.getMenu().add(0, 2, 1, "Video Info");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                onVideoClick(video, fullVideoList.indexOf(video), fullVideoList);
                return true;
            } else if (item.getItemId() == 2) {
                showVideoInfoDialog(video);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showVideoInfoDialog(VideoItem video) {
        Dialog dialog = new Dialog(requireContext());
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

    @Override
    public void onResume() {
        super.onResume();
        loadVideos();
    }
}

