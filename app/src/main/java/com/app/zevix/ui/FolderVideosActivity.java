package com.app.zevix.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.zevix.R;
import com.app.zevix.adapter.VideoAdapter;
import com.app.zevix.data.AppLockManager;
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

public class FolderVideosActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private View layoutNormalToolbar;
    private View layoutSelectToolbar;
    private ImageView btnBack;
    private TextView tvFolderTitle;
    private TextView tvFolderCount;
    private ImageView btnCast;
    private ImageView btnStyle;
    private ImageView btnMore;

    private ImageView btnCloseSelect;
    private TextView tvSelectCount;
    private ImageView btnSelectLock;
    private ImageView btnSelectDelete;
    private ImageView btnSelectAll;
    private ImageView btnSelectMore;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvVideos;
    private TextView tvEmpty;

    private FolderItem currentFolder;
    private VideoAdapter adapter;
    private PlaybackPreferences preferences;
    private List<VideoItem> videoList = new ArrayList<>();
    private boolean isGridMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_videos);

        currentFolder = (FolderItem) getIntent().getSerializableExtra("folder");
        if (currentFolder == null) {
            finish();
            return;
        }

        preferences = new PlaybackPreferences(this);
        isGridMode = preferences.isFolderGridMode();

        layoutNormalToolbar = findViewById(R.id.layout_normal_toolbar);
        layoutSelectToolbar = findViewById(R.id.layout_select_toolbar);

        btnBack = findViewById(R.id.btn_back);
        tvFolderTitle = findViewById(R.id.tv_folder_title);
        tvFolderCount = findViewById(R.id.tv_folder_count);
        btnCast = findViewById(R.id.btn_cast);
        btnStyle = findViewById(R.id.btn_style);
        btnMore = findViewById(R.id.btn_more);

        btnCloseSelect = findViewById(R.id.btn_close_select);
        tvSelectCount = findViewById(R.id.tv_select_count);
        btnSelectLock = findViewById(R.id.btn_select_lock);
        btnSelectDelete = findViewById(R.id.btn_select_delete);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnSelectMore = findViewById(R.id.btn_select_more);

        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvVideos = findViewById(R.id.rv_videos);
        tvEmpty = findViewById(R.id.tv_empty);

        tvFolderTitle.setText(currentFolder.getFolderName());
        btnBack.setOnClickListener(v -> finish());

        adapter = new VideoAdapter(this, new ArrayList<>(), this);
        adapter.setOnSelectionChangeListener(count -> tvSelectCount.setText(count + " selected"));
        adapter.setOnVideoLongClickListener((video, position) -> {
            if (!adapter.isMultiSelectMode()) {
                enterSelectMode();
                adapter.toggleItemSelection(video);
                return true;
            }
            return false;
        });
        rvVideos.setAdapter(adapter);

        updateLayoutMode();

        swipeRefresh.setColorSchemeResources(R.color.accent_color);
        swipeRefresh.setOnRefreshListener(this::loadVideos);

        btnCast.setOnClickListener(v -> Toast.makeText(this, "Scanning for Cast displays...", Toast.LENGTH_SHORT).show());
        btnStyle.setOnClickListener(v -> toggleLayoutStyle());
        btnMore.setOnClickListener(this::showFolderMenu);

        btnCloseSelect.setOnClickListener(v -> exitSelectMode());
        btnSelectAll.setOnClickListener(v -> {
            if (adapter.getSelectedVideos().size() == videoList.size()) {
                adapter.clearSelection();
            } else {
                adapter.selectAll();
            }
        });
        btnSelectDelete.setOnClickListener(v -> deleteSelectedVideos());
        btnSelectLock.setOnClickListener(v -> lockSelectedVideosToVault());
        btnSelectMore.setOnClickListener(this::showSelectMoreMenu);

        loadVideos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppLockManager lockManager = AppLockManager.getInstance(this);
        if (lockManager.isAppLockEnabled() && !lockManager.isAppUnlocked()) {
            Intent intent = new Intent(this, AppLockVerifyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return;
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.isMultiSelectMode()) {
            exitSelectMode();
            return;
        }
        super.onBackPressed();
    }

    private void updateLayoutMode() {
        if (isGridMode) {
            btnStyle.setImageResource(R.drawable.ic_layout_grid_menu);
            rvVideos.setLayoutManager(new GridLayoutManager(this, 3));
            adapter.setGridMode(true);
        } else {
            btnStyle.setImageResource(R.drawable.ic_layout_list_menu);
            rvVideos.setLayoutManager(new LinearLayoutManager(this));
            adapter.setGridMode(false);
        }
    }

    private void toggleLayoutStyle() {
        isGridMode = !isGridMode;
        preferences.setFolderGridMode(isGridMode);
        updateLayoutMode();
    }

    private void loadVideos() {
        swipeRefresh.setRefreshing(true);
        new Thread(() -> {
            List<FolderItem> folders = MediaStoreRepository.getAllFolders(this, preferences.getSortOrder());
            FolderItem updatedFolder = null;
            for (FolderItem f : folders) {
                if (f.getFolderPath() != null && f.getFolderPath().equals(currentFolder.getFolderPath())) {
                    updatedFolder = f;
                    break;
                }
            }
            final FolderItem target = updatedFolder;
            runOnUiThread(() -> {
                swipeRefresh.setRefreshing(false);
                if (target != null) {
                    currentFolder = target;
                    videoList = new ArrayList<>(target.getVideos());
                } else if (currentFolder != null && currentFolder.getVideos() != null) {
                    videoList = new ArrayList<>(currentFolder.getVideos());
                }
                applySorting();
                adapter.updateData(videoList);
                int count = videoList.size();
                long totalSize = 0;
                for (VideoItem v : videoList) totalSize += v.getSize();
                tvFolderCount.setText(count + " VIDEOS  " + FormatUtils.formatFileSize(totalSize));
                tvEmpty.setVisibility(videoList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void applySorting() {
        MediaStoreRepository.sortVideos(videoList, preferences.getSortOrder());
    }

    private void showFolderMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Select");
        popup.getMenu().add(0, 2, 1, "Sort by");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                enterSelectMode();
                return true;
            } else if (item.getItemId() == 2) {
                SortDialogHelper.showSortDialog(this, false, preferences.getSortOrder(), (newOrder, label) -> {
                    preferences.setSortOrder(newOrder);
                    applySorting();
                    adapter.notifyDataSetChanged();
                });
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void enterSelectMode() {
        layoutNormalToolbar.setVisibility(View.GONE);
        layoutSelectToolbar.setVisibility(View.VISIBLE);
        tvSelectCount.setText("0 selected");
        adapter.setMultiSelectMode(true);
    }

    private void exitSelectMode() {
        adapter.setMultiSelectMode(false);
        layoutSelectToolbar.setVisibility(View.GONE);
        layoutNormalToolbar.setVisibility(View.VISIBLE);
    }

    private void deleteSelectedVideos() {
        List<VideoItem> selected = adapter.getSelectedVideos();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!PermissionHelper.hasFullStorageAccess(this)) {
            PermissionHelper.showFullStorageAccessDialog(this, null);
            return;
        }
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Delete " + selected.size() + " video(s)?")
                .setMessage("Moved to Trash. Files will be deleted permanently after 30 days.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int count = 0;
                    for (VideoItem item : selected) {
                        if (TrashManager.getInstance(this).moveToTrash(item)) {
                            count++;
                            videoList.remove(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    long totalSize = 0;
                    for (VideoItem v : videoList) totalSize += v.getSize();
                    tvFolderCount.setText(videoList.size() + " VIDEOS  " + FormatUtils.formatFileSize(totalSize));
                    tvEmpty.setVisibility(videoList.isEmpty() ? View.VISIBLE : View.GONE);
                    Toast.makeText(this, count + " item(s) moved to Trash", Toast.LENGTH_SHORT).show();
                    exitSelectMode();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void lockSelectedVideosToVault() {
        List<VideoItem> selected = adapter.getSelectedVideos();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Move to Private Folder?")
                .setMessage("Move " + selected.size() + " video(s) to Private Folder? Files will be hidden and protected by PIN.")
                .setPositiveButton("Move", (dialog, which) -> {
                    int count = 0;
                    for (VideoItem item : selected) {
                        if (PrivateVaultManager.moveToVault(this, item)) {
                            count++;
                            videoList.remove(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    long totalSize = 0;
                    for (VideoItem v : videoList) totalSize += v.getSize();
                    tvFolderCount.setText(videoList.size() + " VIDEOS  " + FormatUtils.formatFileSize(totalSize));
                    tvEmpty.setVisibility(videoList.isEmpty() ? View.VISIBLE : View.GONE);
                    Toast.makeText(this, "Locked " + count + " video(s) in Private Folder", Toast.LENGTH_SHORT).show();
                    exitSelectMode();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSelectMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Add to playlist");
        popup.getMenu().add(0, 2, 1, "Share");
        popup.getMenu().add(0, 3, 2, "Properties");

        popup.setOnMenuItemClickListener(item -> {
            List<VideoItem> selected = adapter.getSelectedVideos();
            if (selected.isEmpty()) {
                Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
                return true;
            }

            int id = item.getItemId();
            if (id == 1) {
                // Add to playlist
                showAddToPlaylistDialog(selected);
                return true;
            } else if (id == 2) {
                // Share
                shareSelectedVideos(selected);
                return true;
            } else if (id == 3) {
                // Properties
                if (selected.size() == 1) {
                    showVideoInfoDialog(selected.get(0));
                } else {
                    showMultiVideoInfoDialog(selected);
                }
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showAddToPlaylistDialog(List<VideoItem> items) {
        List<String> playlists = PlaylistManager.getInstance(this).getPlaylistNames();
        List<String> options = new ArrayList<>();
        options.add("My Favorites");
        options.addAll(playlists);
        options.add("+ New Playlist");

        String[] optArray = options.toArray(new String[0]);
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Add to Playlist")
                .setItems(optArray, (dialog, which) -> {
                    if (which == 0) {
                        for (VideoItem v : items) preferences.addFavorite(v.getId());
                        Toast.makeText(this, "Added to My Favorites", Toast.LENGTH_SHORT).show();
                        exitSelectMode();
                    } else if (which == optArray.length - 1) {
                        showCreatePlaylistAndAddDialog(items);
                    } else {
                        String name = optArray[which];
                        for (VideoItem v : items) {
                            PlaylistManager.getInstance(this).addVideoToPlaylist(name, v.getId());
                        }
                        Toast.makeText(this, "Added to " + name, Toast.LENGTH_SHORT).show();
                        exitSelectMode();
                    }
                })
                .show();
    }

    private void showCreatePlaylistAndAddDialog(List<VideoItem> items) {
        EditText et = new EditText(this);
        et.setHint("Playlist name");
        et.setTextColor(getResources().getColor(R.color.white, getTheme()));
        et.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("New Playlist")
                .setView(et)
                .setPositiveButton("CREATE", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty()) {
                        PlaylistManager.getInstance(this).createPlaylist(name);
                        for (VideoItem v : items) {
                            PlaylistManager.getInstance(this).addVideoToPlaylist(name, v.getId());
                        }
                        Toast.makeText(this, "Created and added to " + name, Toast.LENGTH_SHORT).show();
                        exitSelectMode();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void shareSelectedVideos(List<VideoItem> selected) {
        List<Uri> uris = new ArrayList<>();
        for (VideoItem v : selected) {
            if (v.getUri() != null) uris.add(v.getUri());
        }
        if (uris.isEmpty()) {
            Toast.makeText(this, "No video files found to share", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);
        share.setType("video/*");
        share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, (ArrayList<? extends android.os.Parcelable>) uris);
        startActivity(Intent.createChooser(share, "Share " + uris.size() + " videos"));
    }

    private void showMultiVideoInfoDialog(List<VideoItem> selected) {
        long totalSize = 0;
        long totalDuration = 0;
        for (VideoItem v : selected) {
            totalSize += v.getSize();
            totalDuration += v.getDuration();
        }

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Selected Videos Info")
                .setMessage("Count: " + selected.size() + " videos\nTotal Size: " + FormatUtils.formatFileSize(totalSize) + "\nTotal Duration: " + FormatUtils.formatDuration(totalDuration))
                .setPositiveButton("OK", null)
                .show();
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

        sheet.findViewById(R.id.action_lock).setOnClickListener(v -> {
            dialog.dismiss();
            boolean moved = PrivateVaultManager.moveToVault(this, video);
            if (moved) {
                videoList.remove(video);
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Moved to Private Folder", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to move to Private Folder", Toast.LENGTH_SHORT).show();
            }
        });

        sheet.findViewById(R.id.action_convert_mp3).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Playing as Audio: " + video.getTitle(), Toast.LENGTH_SHORT).show();
            onVideoClick(video, videoList.indexOf(video), videoList);
        });

        sheet.findViewById(R.id.action_add_to_playlist).setOnClickListener(v -> {
            dialog.dismiss();
            preferences.addFavorite(video.getId());
            Toast.makeText(this, "Added to My Favorites", Toast.LENGTH_SHORT).show();
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
            EditText et = new EditText(this);
            et.setText(video.getTitle());
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Rename Video")
                    .setView(et)
                    .setPositiveButton("Rename", (d, which) -> {
                        Toast.makeText(this, "Video renamed to " + et.getText().toString(), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        sheet.findViewById(R.id.action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            if (!PermissionHelper.hasFullStorageAccess(this)) {
                PermissionHelper.showFullStorageAccessDialog(this, null);
                return;
            }
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Move to Trash")
                    .setMessage("Move '" + video.getTitle() + "' to Trash? You can restore it within 30 days.")
                    .setPositiveButton("Move to Trash", (d, which) -> {
                        boolean ok = TrashManager.getInstance(this).moveToTrash(video);
                        if (ok) {
                            videoList.remove(video);
                            adapter.notifyDataSetChanged();
                            tvFolderCount.setText(videoList.size() + " VIDEOS  " + FormatUtils.formatFileSize(currentFolder.getTotalSize()));
                            Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to move to Trash", Toast.LENGTH_SHORT).show();
                        }
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
