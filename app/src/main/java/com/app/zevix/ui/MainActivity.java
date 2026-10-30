package com.app.zevix.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.app.zevix.R;
import com.app.zevix.data.AppLockManager;
import com.app.zevix.data.MusicPlayerManager;
import com.app.zevix.data.TrashManager;
import com.app.zevix.model.MusicItem;
import com.app.zevix.utils.PermissionHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private View toolbarNormal;
    private TextView tvMainTitle;
    private ImageView btnCast;
    private ImageView btnSearch;
    private ImageView btnMainMore;
    private LinearLayout searchContainer;
    private EditText etSearch;
    private ImageView btnClearSearch;

    // Multi Select Bar
    private View toolbarMultiSelect;
    private ImageView btnCloseMulti;
    private TextView tvMultiCount;
    private ImageView btnMultiLock;
    private ImageView btnMultiDelete;
    private ImageView btnMultiMore;

    // Bottom Tabs
    private View tabVideo;
    private View tabMusic;
    private View tabPlaylist;
    private View ivTabVideo;
    private View ivTabMusic;
    private View ivTabPlaylist;
    private TextView tvTabVideo;
    private TextView tvTabMusic;
    private TextView tvTabPlaylist;

    // Mini Player
    private View miniPlayerContainer;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageView miniPlayerPlayPause;
    private ImageView miniPlayerClose;

    private VideoMainFragment videoFragment;
    private MusicMainFragment musicFragment;
    private PlaylistMainFragment playlistFragment;
    private int currentTab = 0; // 0: Video, 1: Music, 2: Playlist

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupBottomTabs();
        setupSearchAndToolbar();
        initAudioPlayer();

        // Start with Video tab
        switchTab(0);

        checkPermissionsInApp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppLockManager lockManager = AppLockManager.getInstance(this);
        if (lockManager.isAppLockEnabled() && !lockManager.isAppUnlocked()) {
            Intent intent = new Intent(this, AppLockVerifyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        // Background check for expired trash & residual deleted files
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                TrashManager tm = TrashManager.getInstance(this);
                tm.cleanExpiredTrash();
                tm.cleanupResidualPermDeletedFiles();
            } catch (Exception ignored) {}
        });
    }

    private void checkPermissionsInApp() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            showPermissionBottomSheet();
        } else if (!PermissionHelper.hasFullStorageAccess(this)) {
            showAllFilesAccessBottomSheet();
        }
    }

    private void showAllFilesAccessBottomSheet() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return;
        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Storage Access Required")
                .setMessage("To permanently delete files from your device, move items to Trash, and clean storage, please grant All Files Access.")
                .setPositiveButton("Grant Access", (dialog, which) -> {
                    PermissionHelper.requestFullStorageAccess(this);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void showPermissionBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_permission_request, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        Button btnGrant = view.findViewById(R.id.btn_grant_permission);
        Button btnCancel = view.findViewById(R.id.btn_cancel_permission);

        btnGrant.setOnClickListener(v -> {
            dialog.dismiss();
            PermissionHelper.requestStoragePermission(this);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (videoFragment != null) videoFragment.loadData();
                if (musicFragment != null) musicFragment.loadMusic();
                if (!PermissionHelper.hasFullStorageAccess(this)) {
                    showAllFilesAccessBottomSheet();
                }
            }
        }
    }

    private void initViews() {
        toolbarNormal = findViewById(R.id.toolbar);
        tvMainTitle = findViewById(R.id.tv_main_title);
        btnCast = findViewById(R.id.btn_cast);
        btnSearch = findViewById(R.id.btn_search);
        btnMainMore = findViewById(R.id.btn_main_more);
        searchContainer = findViewById(R.id.search_container);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        toolbarMultiSelect = findViewById(R.id.toolbar_multi_select);
        btnCloseMulti = findViewById(R.id.btn_close_multi);
        tvMultiCount = findViewById(R.id.tv_multi_count);
        btnMultiLock = findViewById(R.id.btn_multi_lock);
        btnMultiDelete = findViewById(R.id.btn_multi_delete);
        btnMultiMore = findViewById(R.id.btn_multi_more);

        tabVideo = findViewById(R.id.tab_video);
        tabMusic = findViewById(R.id.tab_music);
        tabPlaylist = findViewById(R.id.tab_playlist);

        ivTabVideo = findViewById(R.id.iv_tab_video);
        ivTabMusic = findViewById(R.id.iv_tab_music);
        ivTabPlaylist = findViewById(R.id.iv_tab_playlist);

        tvTabVideo = findViewById(R.id.tv_tab_video);
        tvTabMusic = findViewById(R.id.tv_tab_music);
        tvTabPlaylist = findViewById(R.id.tv_tab_playlist);

        miniPlayerContainer = findViewById(R.id.mini_player_container);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerPlayPause = findViewById(R.id.mini_player_play_pause);
        miniPlayerClose = findViewById(R.id.mini_player_close);

        videoFragment = new VideoMainFragment();
        musicFragment = new MusicMainFragment();
        playlistFragment = new PlaylistMainFragment();

        videoFragment.setOnMultiSelectModeListener((inSelectMode, count) -> {
            if (inSelectMode) {
                toolbarNormal.setVisibility(View.GONE);
                toolbarMultiSelect.setVisibility(View.VISIBLE);
                tvMultiCount.setText(count + " selected");
            } else {
                toolbarMultiSelect.setVisibility(View.GONE);
                toolbarNormal.setVisibility(View.VISIBLE);
            }
        });

        btnCloseMulti.setOnClickListener(v -> {
            if (videoFragment != null) videoFragment.exitMultiSelectMode();
        });

        btnMultiLock.setOnClickListener(v -> {
            if (videoFragment != null) videoFragment.lockSelected();
        });

        btnMultiDelete.setOnClickListener(v -> {
            if (videoFragment != null) videoFragment.deleteSelected();
        });

        btnMultiMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, btnMultiMore);
            popup.getMenu().add(0, 1, 0, "Select All");
            popup.getMenu().add(0, 2, 1, "Add to playlist");
            popup.getMenu().add(0, 3, 2, "Share");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        if (videoFragment != null) videoFragment.selectAll();
                        return true;
                    case 2:
                        if (videoFragment != null) videoFragment.addSelectedToPlaylist();
                        return true;
                    case 3:
                        if (videoFragment != null) videoFragment.shareSelected();
                        return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupBottomTabs() {
        tabVideo.setOnClickListener(v -> switchTab(0));
        tabMusic.setOnClickListener(v -> switchTab(1));
        tabPlaylist.setOnClickListener(v -> switchTab(2));
    }

    private void switchTab(int tabIndex) {
        if (currentTab == 0 && videoFragment != null && videoFragment.isMultiSelectActive()) {
            videoFragment.exitMultiSelectMode();
        }

        currentTab = tabIndex;
        Fragment selectedFragment;

        ivTabVideo.setSelected(tabIndex == 0);
        ivTabMusic.setSelected(tabIndex == 1);
        ivTabPlaylist.setSelected(tabIndex == 2);

        tvTabVideo.setSelected(tabIndex == 0);
        tvTabMusic.setSelected(tabIndex == 1);
        tvTabPlaylist.setSelected(tabIndex == 2);

        switch (tabIndex) {
            case 1:
                tvMainTitle.setText("Music");
                selectedFragment = musicFragment;
                break;
            case 2:
                tvMainTitle.setText("Playlist");
                selectedFragment = playlistFragment;
                break;
            case 0:
            default:
                tvMainTitle.setText("Video");
                selectedFragment = videoFragment;
                break;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, selectedFragment);
        transaction.commit();
    }

    private void setupSearchAndToolbar() {
        btnCast.setOnClickListener(v -> {
            Toast.makeText(this, "Scanning for Cast devices (Chromecast / Smart TV)...", Toast.LENGTH_SHORT).show();
        });

        btnSearch.setOnClickListener(v -> {
            if (searchContainer.getVisibility() == View.VISIBLE) {
                searchContainer.setVisibility(View.GONE);
                etSearch.setText("");
            } else {
                searchContainer.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
            }
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s != null ? s.toString() : "";
                if (currentTab == 0 && videoFragment != null) {
                    videoFragment.filter(q);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnMainMore.setOnClickListener(this::showMoreMenu);
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Select");
        popup.getMenu().add(0, 2, 1, "Network Stream");
        popup.getMenu().add(0, 4, 2, "Refresh");
        popup.getMenu().add(0, 5, 3, "Equalizer");
        popup.getMenu().add(0, 6, 4, "Settings");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    if (currentTab == 0 && videoFragment != null) {
                        videoFragment.enterMultiSelectMode();
                    }
                    return true;
                case 2:
                    showNetworkStreamDialog();
                    return true;
                case 4:
                    if (currentTab == 0 && videoFragment != null) videoFragment.loadData();
                    else if (currentTab == 1 && musicFragment != null) musicFragment.loadMusic();
                    Toast.makeText(this, "Media refreshed", Toast.LENGTH_SHORT).show();
                    return true;
                case 5:
                    startActivity(new Intent(this, EqualizerActivity.class));
                    return true;
                case 6:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void showNetworkStreamDialog() {
        EditText etUrl = new EditText(this);
        etUrl.setHint("http://, https://, rtsp://, hls url");
        etUrl.setText("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        etUrl.setSelectAllOnFocus(true);
        etUrl.setTextColor(getResources().getColor(R.color.white, getTheme()));
        etUrl.setPadding(32, 24, 32, 24);

        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Network Stream")
                .setMessage("Enter video stream URL to play online:")
                .setView(etUrl)
                .setPositiveButton("Play", (dialog, which) -> {
                    String url = etUrl.getText().toString().trim();
                    if (!url.isEmpty()) {
                        Intent intent = new Intent(this, PlayerActivity.class);
                        intent.setData(android.net.Uri.parse(url));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initAudioPlayer() {
        MusicPlayerManager manager = MusicPlayerManager.getInstance(this);
        manager.addListener(new MusicPlayerManager.OnMusicStateChangeListener() {
            @Override
            public void onSongChanged(MusicItem item) {
                if (item != null) {
                    miniPlayerTitle.setText(item.getTitle());
                    miniPlayerArtist.setText(item.getArtist());
                    miniPlayerContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
                miniPlayerPlayPause.setImageResource(isPlaying ? R.drawable.play_pause : R.drawable.play_play);
            }

            @Override
            public void onProgressUpdate(long currentPos, long duration) {}
        });

        miniPlayerPlayPause.setOnClickListener(v -> manager.togglePlayPause());

        miniPlayerClose.setOnClickListener(v -> {
            manager.stop();
            miniPlayerContainer.setVisibility(View.GONE);
        });

        miniPlayerContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, MusicPlayActivity.class);
            startActivity(intent);
        });
    }

    public void playMusic(MusicItem music) {
        if (music == null) return;
        List<MusicItem> list = new ArrayList<>();
        list.add(music);
        MusicPlayerManager.getInstance(this).playSong(list, 0);
        Intent intent = new Intent(this, MusicPlayActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (videoFragment != null && videoFragment.isMultiSelectActive()) {
            videoFragment.exitMultiSelectMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
