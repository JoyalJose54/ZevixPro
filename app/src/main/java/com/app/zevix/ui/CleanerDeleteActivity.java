package com.app.zevix.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.adapter.CleanerAdapter;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.data.TrashManager;
import com.app.zevix.model.VideoItem;
import com.app.zevix.utils.PermissionHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CleanerDeleteActivity extends AppCompatActivity {

    public static final String EXTRA_CLEANER_TYPE = "cleaner_type";
    public static final int TYPE_WATCHED = 1;
    public static final int TYPE_LARGE = 2;
    public static final int TYPE_DUPLICATE = 3;

    private int cleanerType = TYPE_LARGE;
    private final List<VideoItem> videoList = new ArrayList<>();
    private CleanerAdapter adapter;

    private CheckBox cbSelectAll;
    private Button btnCleanExecute;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cleaner_delete);

        cleanerType = getIntent().getIntExtra(EXTRA_CLEANER_TYPE, TYPE_LARGE);

        ImageView btnBack = findViewById(R.id.btn_clean_delete_back);
        TextView tvTitle = findViewById(R.id.tv_clean_delete_title);
        cbSelectAll = findViewById(R.id.cb_clean_select_all);
        RecyclerView rvList = findViewById(R.id.rv_clean_delete_list);
        layoutEmpty = findViewById(R.id.layout_clean_empty);
        btnCleanExecute = findViewById(R.id.btn_clean_execute);

        btnBack.setOnClickListener(v -> finish());

        switch (cleanerType) {
            case TYPE_WATCHED:
                tvTitle.setText("Watched Videos");
                break;
            case TYPE_LARGE:
                tvTitle.setText("Large Files");
                break;
            case TYPE_DUPLICATE:
                tvTitle.setText("Duplicate Files");
                break;
        }

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CleanerAdapter(this, videoList);
        adapter.setOnSelectionChangeListener((count, size) -> {
            updateCleanButton(count, size);
            cbSelectAll.setChecked(adapter.isAllSelected());
        });
        rvList.setAdapter(adapter);

        cbSelectAll.setOnClickListener(v -> adapter.selectAll(cbSelectAll.isChecked()));

        btnCleanExecute.setOnClickListener(v -> {
            List<VideoItem> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select files to clean", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!PermissionHelper.hasFullStorageAccess(this)) {
                PermissionHelper.showFullStorageAccessDialog(this, null);
                return;
            }
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Clean Files")
                    .setMessage("Move " + selected.size() + " file(s) to Trash? You can restore them anytime within 30 days.")
                    .setPositiveButton("Delete", (dialog, which) -> executeDelete(selected))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        loadFiles();
    }

    private void loadFiles() {
        videoList.clear();
        PlaybackPreferences prefs = new PlaybackPreferences(this);
        List<VideoItem> allVideos = MediaStoreRepository.getAllVideos(this, MediaStoreRepository.SORT_DATE_DESC);

        if (cleanerType == TYPE_WATCHED) {
            for (VideoItem v : allVideos) {
                if (prefs.getPlaybackPosition(v.getId()) > 0) {
                    videoList.add(v);
                }
            }
        } else if (cleanerType == TYPE_LARGE) {
            for (VideoItem v : allVideos) {
                if (v.getSize() >= 50L * 1024 * 1024) { // >= 50 MB
                    videoList.add(v);
                }
            }
            // If none >= 50MB, show top 10 largest
            if (videoList.isEmpty()) {
                List<VideoItem> sortedBySize = new ArrayList<>(allVideos);
                sortedBySize.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));
                for (int i = 0; i < Math.min(10, sortedBySize.size()); i++) {
                    videoList.add(sortedBySize.get(i));
                }
            }
        } else if (cleanerType == TYPE_DUPLICATE) {
            Map<Long, List<VideoItem>> sizeMap = new HashMap<>();
            for (VideoItem v : allVideos) {
                if (!sizeMap.containsKey(v.getSize())) {
                    sizeMap.put(v.getSize(), new ArrayList<>());
                }
                sizeMap.get(v.getSize()).add(v);
            }
            for (List<VideoItem> group : sizeMap.values()) {
                if (group.size() > 1) {
                    videoList.addAll(group);
                }
            }
        }

        layoutEmpty.setVisibility(videoList.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
        cbSelectAll.setChecked(false);
        updateCleanButton(0, 0);
    }

    private void updateCleanButton(int count, long size) {
        if (count == 0) {
            btnCleanExecute.setText("DELETE (0 B)");
            btnCleanExecute.setEnabled(false);
            btnCleanExecute.setAlpha(0.5f);
        } else {
            String sizeStr = Formatter.formatFileSize(this, size);
            btnCleanExecute.setText("DELETE " + count + " (" + sizeStr + ")");
            btnCleanExecute.setEnabled(true);
            btnCleanExecute.setAlpha(1.0f);
        }
    }

    private void executeDelete(List<VideoItem> items) {
        TrashManager trashManager = TrashManager.getInstance(this);
        int deleted = 0;
        for (VideoItem item : items) {
            boolean success = trashManager.moveToTrash(item);
            if (success) deleted++;
        }

        Toast.makeText(this, "Cleaned " + deleted + " files to Trash", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        loadFiles();
    }
}

