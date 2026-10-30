package com.app.zevix.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.model.MusicItem;
import com.app.zevix.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    public interface OnMusicClickListener {
        void onMusicClick(MusicItem music, int position, List<MusicItem> list);
        void onMusicMore(MusicItem music, View anchor);
    }

    private final Context context;
    private List<MusicItem> musicList;
    private final OnMusicClickListener listener;

    public MusicAdapter(Context context, List<MusicItem> musicList, OnMusicClickListener listener) {
        this.context = context;
        this.musicList = musicList != null ? musicList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<MusicItem> newList) {
        this.musicList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicItem music = musicList.get(position);

        holder.tvTitle.setText(music.getTitle());
        String subtitle = music.getArtist();
        if (music.getDuration() > 0) {
            subtitle += " • " + FormatUtils.formatDuration(music.getDuration());
        }
        holder.tvArtist.setText(subtitle);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMusicClick(music, holder.getAdapterPosition(), musicList);
            }
        });

        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMusicMore(music, holder.btnMore);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvArtist;
        ImageView btnMore;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_music_thumb);
            tvTitle = itemView.findViewById(R.id.tv_music_title);
            tvArtist = itemView.findViewById(R.id.tv_music_artist);
            btnMore = itemView.findViewById(R.id.btn_music_more);
        }
    }
}

