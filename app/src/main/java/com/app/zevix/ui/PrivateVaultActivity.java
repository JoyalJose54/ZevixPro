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
import com.app.zevix.data.PrivateVaultManager;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.FormatUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class PrivateVaultActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private ImageView btnBack;
    private ImageView btnStyle;
    private ImageView btnAddPrivate;
    private RecyclerView rvPrivateVideos;
    private View layoutEmpty;
    private Button btnImportEmpty;

    private VideoAdapter adapter;
    private List<VideoItem> vaultVideos = new ArrayList<>();
    private boolean isGridMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_vault);

        btnBack = findViewById(R.id.btn_back);
        btnStyle = findViewById(R.id.btn_style);
        btnAddPrivate = findViewById(R.id.btn_add_private);
        rvPrivateVideos = findViewById(R.id.rv_private_videos);
        layoutEmpty = findViewById(R.id.layout_empty);
        btnImportEmpty = findViewById(R.id.btn_import_empty);

        btnBack.setOnClickListener(v -> finish());
        btnStyle.setOnClickListener(v -> toggleLayoutStyle());

        btnAddPrivate.setOnClickListener(v -> showImportVideoDialog());
        btnImportEmpty.setOnClickListener(v -> showImportVideoDialog());

        rvPrivateVideos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoAdapter(this, new ArrayList<>(), this);
        rvPrivateVideos.setAdapter(adapter);

        loadVaultVideos();
    }

    private void toggleLayoutStyle() {
        isGridMode = !isGridMode;
        if (isGridMode) {
            btnStyle.setImageResource(R.drawable.ic_layout_list);
            rvPrivateVideos.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            btnStyle.setImageResource(R.drawable.ic_layout_grid);
            rvPrivateVideos.setLayoutManager(new LinearLayoutManager(this));
        }
        adapter.notifyDataSetChanged();
    }

    private void loadVaultVideos() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> list = PrivateVaultManager.getVaultVideos(this);
            new Handler(Looper.getMainLooper()).post(() -> {
                vaultVideos = list;
                adapter.updateData(vaultVideos);
                layoutEmpty.setVisibility(vaultVideos.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showImportVideoDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> allVideos = MediaStoreRepository.getAllVideos(this, MediaStoreRepository.SORT_DATE_DESC);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (allVideos.isEmpty()) {
                    Toast.makeText(this, "No public videos found to import", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] titles = new String[allVideos.size()];
                boolean[] checked = new boolean[allVideos.size()];
                for (int i = 0; i < allVideos.size(); i++) {
                    titles[i] = allVideos.get(i).getTitle() + " (" + FormatUtils.formatFileSize(allVideos.get(i).getSize()) + ")";
                }

                new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                        .setTitle("Select Videos to Lock")
                        .setMultiChoiceItems(titles, checked, (d, which, isChecked) -> checked[which] = isChecked)
                        .setPositiveButton("Lock Selected", (d, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                int lockedCount = 0;
                                for (int i = 0; i < checked.length; i++) {
                                    if (checked[i]) {
                                        PrivateVaultManager.moveToVault(this, allVideos.get(i));
                                        lockedCount++;
                                    }
                                }
                                final int count = lockedCount;
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(this, "Locked " + count + " videos in Private Folder", Toast.LENGTH_SHORT).show();
                                    loadVaultVideos();
                                });
                            });
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

        // Lock button repurposed as "Unlock from Vault"
        TextView tvLock = sheet.findViewById(R.id.action_lock);
        tvLock.setText("Unlock to Movies Folder");
        tvLock.setOnClickListener(v -> {
            dialog.dismiss();
            Executors.newSingleThreadExecutor().execute(() -> {
                boolean restored = PrivateVaultManager.restoreFromVault(this, video);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (restored) {
                        Toast.makeText(this, "Video unlocked to Movies folder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to unlock video", Toast.LENGTH_SHORT).show();
                    }
                    loadVaultVideos();
                });
            });
        });

        sheet.findViewById(R.id.action_convert_mp3).setOnClickListener(v -> {
            dialog.dismiss();
            onVideoClick(video, vaultVideos.indexOf(video), vaultVideos);
        });

        sheet.findViewById(R.id.action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Delete Private Video")
                    .setMessage("Permanently delete '" + video.getTitle() + "'?")
                    .setPositiveButton("Delete", (d, w) -> {
                        PrivateVaultManager.deleteFromVault(this, video);
                        Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show();
                        loadVaultVideos();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
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

