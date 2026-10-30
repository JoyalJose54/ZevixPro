package com.app.zevix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.zevix.R;
import com.app.zevix.adapter.FolderAdapter;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.model.FolderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FoldersFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvFolders;
    private TextView tvEmpty;
    private FolderAdapter adapter;
    private PlaybackPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folders, container, false);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        rvFolders = view.findViewById(R.id.rv_folders);
        tvEmpty = view.findViewById(R.id.tv_empty);

        preferences = new PlaybackPreferences(requireContext());
        rvFolders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FolderAdapter(getContext(), new ArrayList<>(), new FolderAdapter.OnFolderClickListener() {
            @Override
            public void onFolderClick(FolderItem folder) {
                Intent intent = new Intent(getContext(), FolderVideosActivity.class);
                intent.putExtra("folder", folder);
                startActivity(intent);
            }

            @Override
            public void onFolderMore(FolderItem folder, View anchor) {}
        });
        rvFolders.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.accent_color);
        swipeRefresh.setOnRefreshListener(this::loadFolders);

        loadFolders();
        return view;
    }

    public void loadFolders() {
        if (getContext() == null) return;
        swipeRefresh.setRefreshing(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            int sortOrder = preferences.getSortOrder();
            List<FolderItem> folders = MediaStoreRepository.getAllFolders(requireContext(), sortOrder);
            new Handler(Looper.getMainLooper()).post(() -> {
                swipeRefresh.setRefreshing(false);
                adapter.updateData(folders);
                if (folders.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFolders();
    }
}
