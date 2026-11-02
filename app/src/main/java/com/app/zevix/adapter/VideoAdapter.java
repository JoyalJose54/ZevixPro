package com.app.zevix.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.FormatUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoItem video, int position, List<VideoItem> currentList);
        void onMoreClick(VideoItem video, View anchorView);
    }

    public interface OnVideoLongClickListener {
        boolean onVideoLongClick(VideoItem video, int position);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private final Context context;
    private final PlaybackPreferences preferences;
    private List<VideoItem> videoList;
    private final OnVideoClickListener listener;
    private OnVideoLongClickListener longClickListener;
    private OnSelectionChangeListener selectionChangeListener;

    private boolean isMultiSelectMode = false;
    private boolean isGridMode = false;
    private final Set<Long> selectedIds = new HashSet<>();

    public VideoAdapter(Context context, List<VideoItem> videoList, OnVideoClickListener listener) {
        this.context = context;
        this.preferences = new PlaybackPreferences(context);
        this.videoList = videoList != null ? videoList : new ArrayList<>();
        this.listener = listener;
    }

    public void setOnVideoLongClickListener(OnVideoLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public void updateData(List<VideoItem> newList) {
        this.videoList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<VideoItem> getVideoList() {
        return videoList;
    }

    public void setGridMode(boolean isGrid) {
        this.isGridMode = isGrid;
        notifyDataSetChanged();
    }

    public boolean isGridMode() {
        return isGridMode;
    }

    public void setMultiSelectMode(boolean enabled) {
        this.isMultiSelectMode = enabled;
        if (!enabled) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedIds.size());
        }
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void selectItem(VideoItem video) {
        selectedIds.add(video.getId());
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedIds.size());
        }
    }

    public void selectAll() {
        selectedIds.clear();
        for (VideoItem video : videoList) {
            selectedIds.add(video.getId());
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedIds.size());
        }
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(0);
        }
    }

    public List<VideoItem> getSelectedVideos() {
        List<VideoItem> selected = new ArrayList<>();
        for (VideoItem video : videoList) {
            if (selectedIds.contains(video.getId())) {
                selected.add(video);
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public void toggleItemSelection(VideoItem video) {
        if (selectedIds.contains(video.getId())) {
            selectedIds.remove(video.getId());
        } else {
            selectedIds.add(video.getId());
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedIds.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? 1 : 0;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == 1) ? R.layout.item_video_grid : R.layout.item_video;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videoList.get(position);

        holder.tvTitle.setText(video.getTitle());
        holder.tvDuration.setText(FormatUtils.formatDuration(video.getDuration()));

        if (isGridMode) {
            holder.tvInfo.setText(FormatUtils.formatFileSize(video.getSize()));
        } else {
            String info = FormatUtils.formatFileSize(video.getSize());
            String date = FormatUtils.formatDate(video.getDateAdded());
            if (!date.isEmpty()) {
                info += " • " + date;
            }
            holder.tvInfo.setText(info);
        }

        String badge = video.getResolutionBadge();
        if (!badge.isEmpty()) {
            holder.tvBadge.setText(badge);
            holder.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(video.getUri())
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .placeholder(R.drawable.play_png)
                .error(R.drawable.play_png)
                .into(holder.ivThumbnail);

        if (holder.pbProgress != null) {
            long savedPos = preferences.getSavedPosition(video.getPath(), video.getId());
            if (video.getDuration() > 0 && savedPos > 0) {
                int progress = (int) ((savedPos * 1000) / video.getDuration());
                holder.pbProgress.setProgress(Math.min(1000, Math.max(15, progress)));
                holder.pbProgress.setVisibility(View.VISIBLE);
            } else {
                holder.pbProgress.setVisibility(View.GONE);
            }
        }

        if (isMultiSelectMode) {
            holder.btnMore.setVisibility(View.GONE);
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.cbSelect.setChecked(selectedIds.contains(video.getId()));

            holder.itemView.setOnClickListener(v -> toggleItemSelection(video));
            holder.itemView.setOnLongClickListener(null);
        } else {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.cbSelect.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoClick(video, holder.getAdapterPosition(), videoList);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onVideoLongClick(video, holder.getAdapterPosition());
                }
                return false;
            });

            holder.btnMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMoreClick(video, holder.btnMore);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvDuration;
        TextView tvBadge;
        TextView tvTitle;
        TextView tvInfo;
        ImageView btnMore;
        CheckBox cbSelect;
        ProgressBar pbProgress;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvBadge = itemView.findViewById(R.id.tv_badge_resolution);
            tvTitle = itemView.findViewById(R.id.tv_video_title);
            tvInfo = itemView.findViewById(R.id.tv_video_info);
            btnMore = itemView.findViewById(R.id.btn_more);
            cbSelect = itemView.findViewById(R.id.cb_select);
            pbProgress = itemView.findViewById(R.id.pb_video_progress);
        }
    }
}
