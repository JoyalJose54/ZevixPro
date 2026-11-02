package com.app.zevix.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.FormatUtils;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class RecentVideoAdapter extends RecyclerView.Adapter<RecentVideoAdapter.RecentViewHolder> {

    public interface OnRecentVideoClickListener {
        void onRecentVideoClick(VideoItem video, int position, List<VideoItem> list);
    }

    private final Context context;
    private final List<VideoItem> videos = new ArrayList<>();
    private final OnRecentVideoClickListener listener;
    private final PlaybackPreferences preferences;

    public RecentVideoAdapter(Context context, List<VideoItem> videos, OnRecentVideoClickListener listener) {
        this.context = context;
        if (videos != null) {
            this.videos.addAll(videos);
        }
        this.listener = listener;
        this.preferences = new PlaybackPreferences(context);
    }

    public void updateData(List<VideoItem> newVideos) {
        videos.clear();
        if (newVideos != null) {
            videos.addAll(newVideos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_recent, parent, false);
        return new RecentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        VideoItem video = videos.get(position);
        holder.tvTitle.setText(video.getTitle());
        holder.tvDuration.setText(FormatUtils.formatDuration(video.getDuration()));

        long savedPos = preferences.getSavedPosition(video.getPath(), video.getId());
        if (video.getDuration() > 0 && savedPos > 0) {
            int progress = (int) ((savedPos * 100) / video.getDuration());
            holder.progressBar.setProgress(Math.min(100, Math.max(1, progress)));
            holder.progressBar.setVisibility(View.VISIBLE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(video.getUri())
                .placeholder(R.drawable.play_png)
                .centerCrop()
                .into(holder.ivThumb);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecentVideoClick(video, holder.getAdapterPosition(), videos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(15, videos.size());
    }

    static class RecentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvDuration;
        ProgressBar progressBar;
        TextView tvTitle;

        RecentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_recent_thumb);
            tvDuration = itemView.findViewById(R.id.tv_recent_duration);
            progressBar = itemView.findViewById(R.id.pb_recent_progress);
            tvTitle = itemView.findViewById(R.id.tv_recent_title);
        }
    }
}
