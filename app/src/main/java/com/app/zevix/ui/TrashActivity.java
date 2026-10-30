package com.app.zevix.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.data.TrashManager;
import com.app.zevix.utils.FormatUtils;
import com.app.zevix.utils.PermissionHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrashActivity extends AppCompatActivity {

    private View toolbarNormal;
    private ImageView btnBack;
    private TextView btnEmptyTrash;

    private View toolbarSelect;
    private ImageView btnSelectClose;
    private TextView tvSelectCount;
    private ImageView btnActionRestoreSelected;
    private ImageView btnActionDeletePerm;
    private ImageView btnActionSelectAll;

    private RecyclerView rvTrash;
    private View layoutEmpty;
    private TrashAdapter adapter;
    private List<TrashManager.TrashItem> trashList = new ArrayList<>();

    private boolean isSelectMode = false;
    private final Set<Long> selectedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        toolbarNormal = findViewById(R.id.toolbar);
        btnBack = findViewById(R.id.btn_back);
        btnEmptyTrash = findViewById(R.id.btn_empty_trash);

        toolbarSelect = findViewById(R.id.toolbar_trash_select);
        btnSelectClose = findViewById(R.id.btn_select_close);
        tvSelectCount = findViewById(R.id.tv_select_count);
        btnActionRestoreSelected = findViewById(R.id.btn_action_restore_selected);
        btnActionDeletePerm = findViewById(R.id.btn_action_delete_perm);
        btnActionSelectAll = findViewById(R.id.btn_action_select_all);

        rvTrash = findViewById(R.id.rv_trash);
        layoutEmpty = findViewById(R.id.layout_empty);

        btnBack.setOnClickListener(v -> finish());
        btnSelectClose.setOnClickListener(v -> exitSelectMode());

        rvTrash.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrashAdapter();
        rvTrash.setAdapter(adapter);

        btnEmptyTrash.setOnClickListener(v -> {
            if (trashList.isEmpty()) {
                Toast.makeText(this, "Trash is already empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!PermissionHelper.hasFullStorageAccess(this)) {
                PermissionHelper.showFullStorageAccessDialog(this, null);
                return;
            }
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Empty Trash")
                    .setMessage("Are you sure you want to permanently delete all " + trashList.size() + " files in Trash? This cannot be undone.")
                    .setPositiveButton("Empty", (dialog, which) -> {
                        TrashManager.getInstance(this).emptyTrash();
                        loadTrash();
                        Toast.makeText(this, "Trash emptied permanently", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnActionDeletePerm.setOnClickListener(v -> confirmDeletePermanentlySelected());
        btnActionRestoreSelected.setOnClickListener(v -> confirmRestoreSelected());
        btnActionSelectAll.setOnClickListener(v -> toggleSelectAll());

        loadTrash();
    }

    private void loadTrash() {
        TrashManager tm = TrashManager.getInstance(this);
        tm.cleanupResidualPermDeletedFiles();
        trashList = tm.getTrashItems();
        adapter.notifyDataSetChanged();
        layoutEmpty.setVisibility(trashList.isEmpty() ? View.VISIBLE : View.GONE);
        btnEmptyTrash.setVisibility(trashList.isEmpty() ? View.GONE : View.VISIBLE);
        if (isSelectMode && trashList.isEmpty()) {
            exitSelectMode();
        }
    }

    private void enterSelectMode(TrashManager.TrashItem item) {
        if (!isSelectMode) {
            isSelectMode = true;
            toolbarNormal.setVisibility(View.GONE);
            toolbarSelect.setVisibility(View.VISIBLE);
            selectedIds.clear();
            if (item != null) {
                selectedIds.add(item.getId());
            }
            updateSelectCount();
            adapter.notifyDataSetChanged();
        }
    }

    private void exitSelectMode() {
        isSelectMode = false;
        selectedIds.clear();
        toolbarSelect.setVisibility(View.GONE);
        toolbarNormal.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void toggleItemSelection(TrashManager.TrashItem item) {
        if (selectedIds.contains(item.getId())) {
            selectedIds.remove(item.getId());
            if (selectedIds.isEmpty()) {
                exitSelectMode();
                return;
            }
        } else {
            selectedIds.add(item.getId());
        }
        updateSelectCount();
        adapter.notifyDataSetChanged();
    }

    private void toggleSelectAll() {
        if (selectedIds.size() == trashList.size()) {
            selectedIds.clear();
            exitSelectMode();
        } else {
            for (TrashManager.TrashItem item : trashList) {
                selectedIds.add(item.getId());
            }
            updateSelectCount();
            adapter.notifyDataSetChanged();
        }
    }

    private void updateSelectCount() {
        tvSelectCount.setText(selectedIds.size() + " selected");
    }

    private void confirmDeletePermanentlySelected() {
        int count = selectedIds.size();
        if (count == 0) return;

        if (!PermissionHelper.hasFullStorageAccess(this)) {
            PermissionHelper.showFullStorageAccessDialog(this, null);
            return;
        }

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Delete Permanently")
                .setMessage("Permanently delete " + count + " selected video(s)? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    List<TrashManager.TrashItem> toDelete = new ArrayList<>();
                    for (TrashManager.TrashItem item : trashList) {
                        if (selectedIds.contains(item.getId())) {
                            toDelete.add(item);
                        }
                    }
                    for (TrashManager.TrashItem item : toDelete) {
                        TrashManager.getInstance(this).deletePermanently(item);
                    }
                    exitSelectMode();
                    loadTrash();
                    Toast.makeText(this, "Deleted " + toDelete.size() + " video(s) permanently", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRestoreSelected() {
        int count = selectedIds.size();
        if (count == 0) return;

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Restore Videos")
                .setMessage("Restore " + count + " selected video(s) to original location?")
                .setPositiveButton("Restore", (dialog, which) -> {
                    int restoredCount = 0;
                    List<TrashManager.TrashItem> toRestore = new ArrayList<>();
                    for (TrashManager.TrashItem item : trashList) {
                        if (selectedIds.contains(item.getId())) {
                            toRestore.add(item);
                        }
                    }
                    for (TrashManager.TrashItem item : toRestore) {
                        if (TrashManager.getInstance(this).restoreItem(item)) {
                            restoredCount++;
                        }
                    }
                    exitSelectMode();
                    loadTrash();
                    Toast.makeText(this, "Restored " + restoredCount + " video(s)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTrashItemMenu(TrashManager.TrashItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_trash, null);
        dialog.setContentView(sheet);

        TextView title = sheet.findViewById(R.id.sheet_title);
        title.setText(item.getTitle());

        sheet.findViewById(R.id.restore).setOnClickListener(v -> {
            dialog.dismiss();
            boolean restored = TrashManager.getInstance(this).restoreItem(item);
            if (restored) {
                Toast.makeText(this, "Restored to " + item.getOriginalPath(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to restore file", Toast.LENGTH_SHORT).show();
            }
            loadTrash();
        });

        sheet.findViewById(R.id.delete).setOnClickListener(v -> {
            dialog.dismiss();
            if (!PermissionHelper.hasFullStorageAccess(this)) {
                PermissionHelper.showFullStorageAccessDialog(this, null);
                return;
            }
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Delete Permanently")
                    .setMessage("Permanently delete '" + item.getTitle() + "'? This action cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        TrashManager.getInstance(this).deletePermanently(item);
                        loadTrash();
                        Toast.makeText(this, "Deleted permanently", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (isSelectMode) {
            exitSelectMode();
        } else {
            super.onBackPressed();
        }
    }

    class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashViewHolder> {

        @NonNull
        @Override
        public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(TrashActivity.this).inflate(R.layout.item_trash, parent, false);
            return new TrashViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
            TrashManager.TrashItem item = trashList.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvRemaining.setText(item.getDaysRemaining() + " days left");
            holder.tvSize.setText(FormatUtils.formatFileSize(item.getSize()));
            holder.tvDuration.setText(FormatUtils.formatDuration(item.getDuration()));

            Glide.with(TrashActivity.this)
                    .load(new File(item.getTrashPath()))
                    .placeholder(R.drawable.play_png)
                    .error(R.drawable.play_png)
                    .into(holder.ivThumb);

            boolean isSelected = selectedIds.contains(item.getId());
            if (isSelectMode) {
                holder.btnMore.setVisibility(View.GONE);
                holder.cbSelect.setVisibility(View.VISIBLE);
                holder.cbSelect.setChecked(isSelected);
                holder.itemView.setSelected(isSelected);
            } else {
                holder.btnMore.setVisibility(View.VISIBLE);
                holder.cbSelect.setVisibility(View.GONE);
                holder.itemView.setSelected(false);
            }

            holder.itemView.setOnLongClickListener(v -> {
                if (!isSelectMode) {
                    enterSelectMode(item);
                } else {
                    toggleItemSelection(item);
                }
                return true;
            });

            holder.itemView.setOnClickListener(v -> {
                if (isSelectMode) {
                    toggleItemSelection(item);
                } else {
                    showTrashItemMenu(item);
                }
            });

            holder.btnMore.setOnClickListener(v -> {
                if (!isSelectMode) {
                    showTrashItemMenu(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return trashList.size();
        }

        class TrashViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView tvDuration;
            TextView tvTitle;
            TextView tvRemaining;
            TextView tvSize;
            ImageView btnMore;
            CheckBox cbSelect;

            TrashViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumb = itemView.findViewById(R.id.iv_trash_thumb);
                tvDuration = itemView.findViewById(R.id.tv_trash_duration);
                tvTitle = itemView.findViewById(R.id.tv_trash_title);
                tvRemaining = itemView.findViewById(R.id.tv_trash_remaining);
                tvSize = itemView.findViewById(R.id.tv_trash_size);
                btnMore = itemView.findViewById(R.id.btn_trash_more);
                cbSelect = itemView.findViewById(R.id.cb_trash_select);
            }
        }
    }
}
