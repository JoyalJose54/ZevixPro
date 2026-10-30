package com.app.zevix.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.model.FolderItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(FolderItem folder);
        void onFolderMore(FolderItem folder, View anchor);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private final Context context;
    private List<FolderItem> folderList;
    private final OnFolderClickListener listener;
    private OnSelectionChangeListener selectionChangeListener;

    private boolean isMultiSelectMode = false;
    private final Set<String> selectedPaths = new HashSet<>();

    public FolderAdapter(Context context, List<FolderItem> folderList, OnFolderClickListener listener) {
        this.context = context;
        this.folderList = folderList != null ? folderList : new ArrayList<>();
        this.listener = listener;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public void updateData(List<FolderItem> newList) {
        this.folderList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setMultiSelectMode(boolean enabled) {
        this.isMultiSelectMode = enabled;
        if (!enabled) {
            selectedPaths.clear();
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedPaths.size());
        }
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void selectAll() {
        selectedPaths.clear();
        for (FolderItem folder : folderList) {
            selectedPaths.add(folder.getFolderPath());
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedPaths.size());
        }
    }

    public void clearSelection() {
        selectedPaths.clear();
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(0);
        }
    }

    public List<FolderItem> getSelectedFolders() {
        List<FolderItem> selected = new ArrayList<>();
        for (FolderItem folder : folderList) {
            if (selectedPaths.contains(folder.getFolderPath())) {
                selected.add(folder);
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedPaths.size();
    }

    private void toggleFolderSelection(FolderItem folder) {
        if (selectedPaths.contains(folder.getFolderPath())) {
            selectedPaths.remove(folder.getFolderPath());
        } else {
            selectedPaths.add(folder.getFolderPath());
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedPaths.size());
        }
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        FolderItem folder = folderList.get(position);

        holder.tvFolderName.setText(folder.getFolderName());
        int count = folder.getVideoCount();
        holder.tvVideoCountBadge.setText(String.valueOf(count));

        // Display NEW badge if folder has recent videos
        if (folder.getVideos() != null && !folder.getVideos().isEmpty()) {
            long now = System.currentTimeMillis();
            long day7 = 7L * 24 * 60 * 60 * 1000;
            boolean hasNew = false;
            for (com.app.zevix.model.VideoItem vi : folder.getVideos()) {
                if (now - vi.getDateAdded() < day7) {
                    hasNew = true;
                    break;
                }
            }
            holder.tvFolderNewBadge.setVisibility(hasNew ? View.VISIBLE : View.GONE);
        } else {
            holder.tvFolderNewBadge.setVisibility(View.GONE);
        }

        if (folder.getFolderName().equalsIgnoreCase("Recent Added") || folder.getFolderName().equalsIgnoreCase("Recently Added")) {
            holder.ivFolderIcon.setImageResource(R.drawable.ic_folder_recent);
        } else {
            holder.ivFolderIcon.setImageResource(R.drawable.ic_folder);
        }

        if (isMultiSelectMode) {
            holder.btnFolderMore.setVisibility(View.GONE);
            holder.tvVideoCountBadge.setVisibility(View.GONE);
            holder.cbFolderSelect.setVisibility(View.VISIBLE);
            holder.cbFolderSelect.setChecked(selectedPaths.contains(folder.getFolderPath()));

            holder.itemView.setOnClickListener(v -> toggleFolderSelection(folder));
        } else {
            holder.btnFolderMore.setVisibility(View.VISIBLE);
            holder.tvVideoCountBadge.setVisibility(View.VISIBLE);
            holder.cbFolderSelect.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFolderClick(folder);
                }
            });

            holder.btnFolderMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFolderMore(folder, holder.btnFolderMore);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFolderIcon;
        TextView tvFolderName;
        TextView tvVideoCountBadge;
        TextView tvFolderNewBadge;
        ImageView btnFolderMore;
        CheckBox cbFolderSelect;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFolderIcon = itemView.findViewById(R.id.iv_folder_icon);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            tvVideoCountBadge = itemView.findViewById(R.id.tv_video_count_badge);
            tvFolderNewBadge = itemView.findViewById(R.id.tv_folder_new_badge);
            btnFolderMore = itemView.findViewById(R.id.btn_folder_more);
            cbFolderSelect = itemView.findViewById(R.id.cb_folder_select);
        }
    }
}
