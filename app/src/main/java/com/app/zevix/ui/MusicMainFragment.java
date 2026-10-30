package com.app.zevix.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.zevix.R;
import com.app.zevix.adapter.MusicAdapter;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.model.MusicItem;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class MusicMainFragment extends Fragment implements MusicAdapter.OnMusicClickListener {

    private TabLayout musicTabs;
    private View btnShufflePlay;
    private TextView tvMusicCount;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvMusic;
    private MusicAdapter adapter;

    private List<MusicItem> musicList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_main, container, false);

        musicTabs = view.findViewById(R.id.music_tabs);
        btnShufflePlay = view.findViewById(R.id.btn_shuffle_play);
        tvMusicCount = view.findViewById(R.id.tv_music_count);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_music);
        rvMusic = view.findViewById(R.id.rv_music);

        musicTabs.addTab(musicTabs.newTab().setText("SONGS"));
        musicTabs.addTab(musicTabs.newTab().setText("FOLDERS"));
        musicTabs.addTab(musicTabs.newTab().setText("ALBUMS"));
        musicTabs.addTab(musicTabs.newTab().setText("ARTISTS"));

        rvMusic.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MusicAdapter(getContext(), new ArrayList<>(), this);
        rvMusic.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.accent_color);
        swipeRefresh.setOnRefreshListener(this::loadMusic);

        btnShufflePlay.setOnClickListener(v -> {
            if (!musicList.isEmpty()) {
                List<MusicItem> shuffled = new ArrayList<>(musicList);
                Collections.shuffle(shuffled);
                onMusicClick(shuffled.get(0), 0, shuffled);
            }
        });

        loadMusic();
        return view;
    }

    public void loadMusic() {
        if (getContext() == null) return;
        swipeRefresh.setRefreshing(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MusicItem> songs = MediaStoreRepository.getAllMusic(requireContext());
            new Handler(Looper.getMainLooper()).post(() -> {
                swipeRefresh.setRefreshing(false);
                musicList = songs;
                adapter.updateData(musicList);
                tvMusicCount.setText(String.valueOf(songs.size()));
            });
        });
    }

    @Override
    public void onMusicClick(MusicItem music, int position, List<MusicItem> list) {
        com.app.zevix.data.MusicPlayerManager.getInstance(requireContext()).playSong(list, position);
        android.content.Intent intent = new android.content.Intent(getContext(), MusicPlayActivity.class);
        startActivity(intent);
    }

    @Override
    public void onMusicMore(MusicItem music, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Play");
        popup.getMenu().add(0, 2, 1, "Add to Favorites");
        popup.getMenu().add(0, 3, 2, "Share");
        popup.getMenu().add(0, 4, 3, "Track Info");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                onMusicClick(music, musicList.indexOf(music), musicList);
                return true;
            } else if (item.getItemId() == 2) {
                new com.app.zevix.data.PlaybackPreferences(requireContext()).addFavorite(music.getId());
                Toast.makeText(getContext(), "Added to Favorites", Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == 3) {
                android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
                share.setType("audio/*");
                share.putExtra(android.content.Intent.EXTRA_STREAM, music.getUri());
                startActivity(android.content.Intent.createChooser(share, "Share Audio"));
                return true;
            } else if (item.getItemId() == 4) {
                Toast.makeText(getContext(), music.getTitle() + "\nArtist: " + music.getArtist() + "\nAlbum: " + music.getAlbum(), Toast.LENGTH_LONG).show();
                return true;
            }
            return false;
        });
        popup.show();
    }
}

