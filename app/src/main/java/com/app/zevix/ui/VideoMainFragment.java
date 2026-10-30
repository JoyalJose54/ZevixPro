package com.app.zevix.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.zevix.R;
import com.app.zevix.adapter.FolderAdapter;
import com.app.zevix.adapter.RecentVideoAdapter;
import com.app.zevix.adapter.VideoAdapter;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.data.PlaylistManager;
import com.app.zevix.data.PrivateVaultManager;
import com.app.zevix.data.TrashManager;
import com.app.zevix.model.FolderItem;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.FormatUtils;
import com.app.zevix.utils.PermissionHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VideoMainFragment extends Fragment implements VideoAdapter.OnVideoClickListener, FolderAdapter.OnFolderClickListener, RecentVideoAdapter.OnRecentVideoClickListener {

    public interface OnMultiSelectModeListener {
        void onMultiSelectChanged(boolean inSelectMode, int count);
    }

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private TextView tvEmpty;

    // Recent Videos Horizontal Carousel
    private View layoutRecentVideos;
    private RecyclerView rvRecentVideos;
    private RecentVideoAdapter recentAdapter;

    // Promoter Chips
    private View chipAllVideos;
    private ImageView ivChipAllVideos;
    private TextView tvChipAllVideos;
    private View chipPrivacy;
    private View chipTrash;

    // Section header
    private TextView tvSectionCount;
    private TextView tvSectionSubInfo;
    private ImageView btnSectionSort;
    private TextView btnDeepScan;

    private FolderAdapter folderAdapter;
    private VideoAdapter videoAdapter;
    private PlaybackPreferences preferences;
    private OnMultiSelectModeListener multiSelectListener;

    private boolean isAllVideosMode = false;
    private boolean isMultiSelectActive = false;
    private List<FolderItem> folderList = new ArrayList<>();
    private List<VideoItem> videoList = new ArrayList<>();
    private String currentSearchQuery = "";

    public void setOnMultiSelectModeListener(OnMultiSelectModeListener listener) {
        this.multiSelectListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_main, container, false);
        preferences = new PlaybackPreferences(requireContext());

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);

        layoutRecentVideos = view.findViewById(R.id.layout_recent_videos);
        rvRecentVideos = view.findViewById(R.id.rv_recent_videos);

        chipAllVideos = view.findViewById(R.id.chip_all_videos);
        ivChipAllVideos = view.findViewById(R.id.iv_chip_all_videos);
        tvChipAllVideos = view.findViewById(R.id.tv_chip_all_videos);
        chipPrivacy = view.findViewById(R.id.chip_privacy);
        chipTrash = view.findViewById(R.id.chip_trash);

        tvSectionCount = view.findViewById(R.id.tv_section_count);
        tvSectionSubInfo = view.findViewById(R.id.tv_section_subinfo);
        btnSectionSort = view.findViewById(R.id.btn_section_sort);
        btnDeepScan = view.findViewById(R.id.btn_deep_scan);

        // Setup horizontal recent videos
        rvRecentVideos.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new RecentVideoAdapter(getContext(), new ArrayList<>(), this);
        rvRecentVideos.setAdapter(recentAdapter);

        // Main recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        folderAdapter = new FolderAdapter(getContext(), new ArrayList<>(), this);
        videoAdapter = new VideoAdapter(getContext(), new ArrayList<>(), this);

        folderAdapter.setOnSelectionChangeListener(count -> {
            if (multiSelectListener != null) multiSelectListener.onMultiSelectChanged(true, count);
        });
        videoAdapter.setOnSelectionChangeListener(count -> {
            if (multiSelectListener != null) multiSelectListener.onMultiSelectChanged(true, count);
        });

        recyclerView.setAdapter(folderAdapter);

        swipeRefresh.setColorSchemeResources(R.color.accent_color);
        swipeRefresh.setOnRefreshListener(this::loadData);

        setupChips();
        btnSectionSort.setOnClickListener(this::showSortMenu);
        btnDeepScan.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Scanning device for video files...", Toast.LENGTH_SHORT).show();
            loadData();
        });

        loadData();
        return view;
    }

    private void setupChips() {
        chipAllVideos.setOnClickListener(v -> {
            if (isMultiSelectActive) exitMultiSelectMode();
            isAllVideosMode = !isAllVideosMode;
            updateModeUI();
        });

        chipPrivacy.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FileProtectionActivity.class);
            startActivity(intent);
        });

        chipTrash.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TrashActivity.class);
            startActivity(intent);
        });
    }

    private void updateModeUI() {
        if (isAllVideosMode) {
            chipAllVideos.setBackgroundResource(R.drawable.bg_chip_promoter_active);
            ivChipAllVideos.setImageResource(R.drawable.ic_folder);
            tvChipAllVideos.setText("All Folders");

            recyclerView.setAdapter(videoAdapter);
            int count = videoList.size();
            long totalSize = 0;
            for (VideoItem item : videoList) {
                totalSize += item.getSize();
            }
            tvSectionCount.setText(count + " VIDEOS");
            tvSectionSubInfo.setText(FormatUtils.formatFileSize(totalSize));
            tvSectionSubInfo.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(videoList.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            chipAllVideos.setBackgroundResource(R.drawable.bg_chip_promoter);
            ivChipAllVideos.setImageResource(R.drawable.ic_all_folders);
            tvChipAllVideos.setText("All Videos");

            recyclerView.setAdapter(folderAdapter);
            tvSectionCount.setText(folderList.size() + " FOLDERS");
            tvSectionSubInfo.setVisibility(View.GONE);
            tvEmpty.setVisibility(folderList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    public void loadData() {
        if (getContext() == null) return;
        swipeRefresh.setRefreshing(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            int sortOrder = preferences.getSortOrder();
            int folderSortOrder = preferences.getFolderSortOrder();
            List<VideoItem> videos = MediaStoreRepository.getAllVideos(requireContext(), sortOrder);
            List<FolderItem> folders = MediaStoreRepository.getAllFolders(requireContext(), folderSortOrder);

            // Add Recent Added synthetic folder if videos exist
            if (!videos.isEmpty()) {
                FolderItem recentFolder = new FolderItem("Recent Added", "");
                int maxRecent = Math.min(20, videos.size());
                for (int i = 0; i < maxRecent; i++) {
                    recentFolder.addVideo(videos.get(i));
                }
                folders.add(0, recentFolder);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                swipeRefresh.setRefreshing(false);
                videoList = videos;
                folderList = folders;

                folderAdapter.updateData(folderList);
                videoAdapter.updateData(videoList);

                // Update horizontal recent videos carousel
                if (!videos.isEmpty()) {
                    layoutRecentVideos.setVisibility(View.VISIBLE);
                    recentAdapter.updateData(videos);
                } else {
                    layoutRecentVideos.setVisibility(View.GONE);
                }

                updateModeUI();

                if (!currentSearchQuery.isEmpty()) {
                    filter(currentSearchQuery);
                }
            });
        });
    }

    public void enterMultiSelectMode() {
        isMultiSelectActive = true;
        if (isAllVideosMode) {
            videoAdapter.setMultiSelectMode(true);
        } else {
            folderAdapter.setMultiSelectMode(true);
        }
        if (multiSelectListener != null) {
            multiSelectListener.onMultiSelectChanged(true, 0);
        }
    }

    public void exitMultiSelectMode() {
        isMultiSelectActive = false;
        videoAdapter.setMultiSelectMode(false);
        folderAdapter.setMultiSelectMode(false);
        if (multiSelectListener != null) {
            multiSelectListener.onMultiSelectChanged(false, 0);
        }
    }

    public boolean isMultiSelectActive() {
        return isMultiSelectActive;
    }

    public void selectAll() {
        if (isAllVideosMode) {
            videoAdapter.selectAll();
        } else {
            folderAdapter.selectAll();
        }
    }

    public void clearSelection() {
        if (isAllVideosMode) {
            videoAdapter.clearSelection();
        } else {
            folderAdapter.clearSelection();
        }
    }

    public void deleteSelected() {
        int count = isAllVideosMode ? videoAdapter.getSelectedCount() : folderAdapter.getSelectedCount();
        if (count == 0) {
            Toast.makeText(getContext(), "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PermissionHelper.hasFullStorageAccess(requireContext())) {
            PermissionHelper.showFullStorageAccessDialog(requireActivity(), null);
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                .setTitle("Move to Trash")
                .setMessage("Move " + count + " selected item(s) to Trash? You can restore them within 30 days.")
                .setPositiveButton("Move to Trash", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (isAllVideosMode) {
                            List<VideoItem> selected = videoAdapter.getSelectedVideos();
                            for (VideoItem v : selected) {
                                TrashManager.getInstance(requireContext()).moveToTrash(v);
                            }
                        } else {
                            List<FolderItem> selected = folderAdapter.getSelectedFolders();
                            for (FolderItem f : selected) {
                                for (VideoItem v : f.getVideos()) {
                                    TrashManager.getInstance(requireContext()).moveToTrash(v);
                                }
                            }
                        }
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Moved to Trash", Toast.LENGTH_SHORT).show();
                            exitMultiSelectMode();
                            loadData();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void lockSelected() {
        int count = isAllVideosMode ? videoAdapter.getSelectedCount() : folderAdapter.getSelectedCount();
        if (count == 0) {
            Toast.makeText(getContext(), "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            if (isAllVideosMode) {
                List<VideoItem> selected = videoAdapter.getSelectedVideos();
                for (VideoItem v : selected) {
                    PrivateVaultManager.moveToVault(requireContext(), v);
                }
            } else {
                List<FolderItem> selected = folderAdapter.getSelectedFolders();
                for (FolderItem f : selected) {
                    for (VideoItem v : f.getVideos()) {
                        PrivateVaultManager.moveToVault(requireContext(), v);
                    }
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getContext(), "Locked " + count + " item(s) in Private Folder", Toast.LENGTH_SHORT).show();
                exitMultiSelectMode();
                loadData();
            });
        });
    }

    public void shareSelected() {
        List<Uri> uris = new ArrayList<>();
        if (isAllVideosMode) {
            for (VideoItem v : videoAdapter.getSelectedVideos()) {
                if (v.getUri() != null) uris.add(v.getUri());
            }
        } else {
            for (FolderItem f : folderAdapter.getSelectedFolders()) {
                for (VideoItem v : f.getVideos()) {
                    if (v.getUri() != null) uris.add(v.getUri());
                }
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(getContext(), "No items selected to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);
        share.setType("video/*");
        share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, (ArrayList<? extends android.os.Parcelable>) uris);
        startActivity(Intent.createChooser(share, "Share " + uris.size() + " videos"));
    }

    public void addSelectedToPlaylist() {
        List<VideoItem> items = new ArrayList<>();
        if (isAllVideosMode) {
            items.addAll(videoAdapter.getSelectedVideos());
        } else {
            for (FolderItem f : folderAdapter.getSelectedFolders()) {
                items.addAll(f.getVideos());
            }
        }

        if (items.isEmpty()) {
            Toast.makeText(getContext(), "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> playlists = PlaylistManager.getInstance(requireContext()).getPlaylistNames();
        List<String> options = new ArrayList<>();
        options.add("My Favorites");
        options.addAll(playlists);
        options.add("+ New Playlist");

        String[] optArray = options.toArray(new String[0]);
        new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                .setTitle("Add to Playlist")
                .setItems(optArray, (dialog, which) -> {
                    if (which == 0) {
                        for (VideoItem v : items) preferences.addFavorite(v.getId());
                        Toast.makeText(getContext(), "Added to My Favorites", Toast.LENGTH_SHORT).show();
                        exitMultiSelectMode();
                    } else if (which == optArray.length - 1) {
                        showCreatePlaylistAndAddDialog(items);
                    } else {
                        String name = optArray[which];
                        for (VideoItem v : items) {
                            PlaylistManager.getInstance(requireContext()).addVideoToPlaylist(name, v.getId());
                        }
                        Toast.makeText(getContext(), "Added to " + name, Toast.LENGTH_SHORT).show();
                        exitMultiSelectMode();
                    }
                })
                .show();
    }

    private void showCreatePlaylistAndAddDialog(List<VideoItem> items) {
        EditText et = new EditText(requireContext());
        et.setHint("Playlist name");
        et.setTextColor(getResources().getColor(R.color.white, requireContext().getTheme()));
        et.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                .setTitle("New Playlist")
                .setView(et)
                .setPositiveButton("CREATE", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty()) {
                        PlaylistManager.getInstance(requireContext()).createPlaylist(name);
                        for (VideoItem v : items) {
                            PlaylistManager.getInstance(requireContext()).addVideoToPlaylist(name, v.getId());
                        }
                        Toast.makeText(getContext(), "Created and added to " + name, Toast.LENGTH_SHORT).show();
                        exitMultiSelectMode();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    public void filter(String query) {
        this.currentSearchQuery = query != null ? query.trim().toLowerCase() : "";
        if (!isAllVideosMode) {
            isAllVideosMode = true;
            updateModeUI();
        }

        if (currentSearchQuery.isEmpty()) {
            videoAdapter.updateData(videoList);
            tvEmpty.setVisibility(videoList.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            List<VideoItem> filtered = new ArrayList<>();
            for (VideoItem video : videoList) {
                if (video.getTitle().toLowerCase().contains(currentSearchQuery)) {
                    filtered.add(video);
                }
            }
            videoAdapter.updateData(filtered);
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showSortMenu(View anchor) {
        if (isAllVideosMode) {
            SortDialogHelper.showSortDialog(requireContext(), false, preferences.getSortOrder(), (newSortOrder, toastLabel) -> {
                preferences.setSortOrder(newSortOrder);
                loadData();
            });
        } else {
            SortDialogHelper.showSortDialog(requireContext(), true, preferences.getFolderSortOrder(), (newSortOrder, toastLabel) -> {
                preferences.setFolderSortOrder(newSortOrder);
                MediaStoreRepository.sortFolders(folderList, newSortOrder);
                folderAdapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    public void onRecentVideoClick(VideoItem video, int position, List<VideoItem> list) {
        onVideoClick(video, position, list);
    }

    @Override
    public void onFolderClick(FolderItem folder) {
        Intent intent = new Intent(getContext(), FolderVideosActivity.class);
        intent.putExtra("folder", folder);
        startActivity(intent);
    }

    @Override
    public void onFolderMore(FolderItem folder, View anchor) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_folder, null);
        dialog.setContentView(sheet);

        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        TextView title = sheet.findViewById(R.id.sheet_title);
        title.setText(folder.getFolderName() + " (" + folder.getVideoCount() + " videos)");

        sheet.findViewById(R.id.action_lock).setOnClickListener(v -> {
            dialog.dismiss();
            Executors.newSingleThreadExecutor().execute(() -> {
                for (VideoItem item : folder.getVideos()) {
                    PrivateVaultManager.moveToVault(requireContext(), item);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Locked in Private Folder", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            });
        });

        sheet.findViewById(R.id.action_play).setOnClickListener(v -> {
            dialog.dismiss();
            if (!folder.getVideos().isEmpty()) {
                onVideoClick(folder.getVideos().get(0), 0, folder.getVideos());
            }
        });

        sheet.findViewById(R.id.action_add_to_playlist).setOnClickListener(v -> {
            dialog.dismiss();
            for (VideoItem item : folder.getVideos()) {
                preferences.addFavorite(item.getId());
            }
            Toast.makeText(getContext(), "Added folder videos to Favorites", Toast.LENGTH_SHORT).show();
        });

        sheet.findViewById(R.id.action_rename).setOnClickListener(v -> {
            dialog.dismiss();
            EditText et = new EditText(getContext());
            et.setText(folder.getFolderName());
            new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Rename Folder")
                    .setView(et)
                    .setPositiveButton("Rename", (d, which) -> {
                        Toast.makeText(getContext(), "Folder renamed to " + et.getText().toString(), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        sheet.findViewById(R.id.action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            if (!PermissionHelper.hasFullStorageAccess(requireContext())) {
                PermissionHelper.showFullStorageAccessDialog(requireActivity(), null);
                return;
            }
            new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Delete Folder")
                    .setMessage("Are you sure you want to move folder '" + folder.getFolderName() + "' and its " + folder.getVideoCount() + " videos to Trash?")
                    .setPositiveButton("Move to Trash", (d, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            for (VideoItem item : folder.getVideos()) {
                                TrashManager.getInstance(requireContext()).moveToTrash(item);
                            }
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(getContext(), "Moved to Trash", Toast.LENGTH_SHORT).show();
                                loadData();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        sheet.findViewById(R.id.action_share).setOnClickListener(v -> {
            dialog.dismiss();
            if (!folder.getVideos().isEmpty()) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("video/*");
                share.putExtra(Intent.EXTRA_STREAM, folder.getVideos().get(0).getUri());
                startActivity(Intent.createChooser(share, "Share from " + folder.getFolderName()));
            }
        });

        dialog.show();
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
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
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

        sheet.findViewById(R.id.action_lock).setOnClickListener(v -> {
            dialog.dismiss();
            Executors.newSingleThreadExecutor().execute(() -> {
                boolean moved = PrivateVaultManager.moveToVault(requireContext(), video);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (moved) {
                        Toast.makeText(getContext(), "Moved to Private Folder", Toast.LENGTH_SHORT).show();
                        loadData();
                    } else {
                        Toast.makeText(getContext(), "Failed to move video to Private Folder", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        sheet.findViewById(R.id.action_convert_mp3).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(getContext(), "Playing as Audio: " + video.getTitle(), Toast.LENGTH_SHORT).show();
            onVideoClick(video, videoList.indexOf(video), videoList);
        });

        sheet.findViewById(R.id.action_add_to_playlist).setOnClickListener(v -> {
            dialog.dismiss();
            preferences.addFavorite(video.getId());
            Toast.makeText(getContext(), "Added to My Favorites", Toast.LENGTH_SHORT).show();
        });

        sheet.findViewById(R.id.action_share).setOnClickListener(v -> {
            dialog.dismiss();
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("video/*");
            share.putExtra(Intent.EXTRA_STREAM, video.getUri());
            startActivity(Intent.createChooser(share, "Share Video"));
        });

        sheet.findViewById(R.id.action_rename).setOnClickListener(v -> {
            dialog.dismiss();
            EditText et = new EditText(getContext());
            et.setText(video.getTitle());
            new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Rename Video")
                    .setView(et)
                    .setPositiveButton("Rename", (d, which) -> {
                        Toast.makeText(getContext(), "Video renamed to " + et.getText().toString(), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        sheet.findViewById(R.id.action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            if (!PermissionHelper.hasFullStorageAccess(requireContext())) {
                PermissionHelper.showFullStorageAccessDialog(requireActivity(), null);
                return;
            }
            new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Move to Trash")
                    .setMessage("Move '" + video.getTitle() + "' to Trash? You can restore it within 30 days.")
                    .setPositiveButton("Move to Trash", (d, which) -> {
                        TrashManager.getInstance(requireContext()).moveToTrash(video);
                        Toast.makeText(getContext(), "Moved to Trash", Toast.LENGTH_SHORT).show();
                        loadData();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        sheet.findViewById(R.id.action_info).setOnClickListener(v -> {
            dialog.dismiss();
            showVideoInfoDialog(video);
        });

        dialog.show();
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
        loadData();
    }
}
