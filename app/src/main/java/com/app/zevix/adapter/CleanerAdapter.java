package com.app.zevix.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.model.VideoItem;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CleanerAdapter extends RecyclerView.Adapter<CleanerAdapter.ViewHolder> {

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount, long selectedSize);
    }

    private final Context context;
    private final List<VideoItem> items;
    private final Set<Long> selectedIds = new HashSet<>();
    private OnSelectionChangeListener listener;

    public CleanerAdapter(Context context, List<VideoItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_clean_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        String sizeStr = Formatter.formatFileSize(context, item.getSize());
        holder.tvInfo.setText(sizeStr);
        holder.tvPath.setText(item.getPath());

        long sec = item.getDuration() / 1000;
        long min = sec / 60;
        sec = sec % 60;
        long hr = min / 60;
        min = min % 60;
        if (hr > 0) {
            holder.tvDuration.setText(String.format(Locale.US, "%d:%02d:%02d", hr, min, sec));
        } else {
            holder.tvDuration.setText(String.format(Locale.US, "%02d:%02d", min, sec));
        }

        Glide.with(context)
                .load(new File(item.getPath()))
                .centerCrop()
                .placeholder(R.drawable.play_png)
                .error(R.drawable.play_png)
                .into(holder.ivThumb);

        boolean isSelected = selectedIds.contains(item.getId());
        holder.cbSelect.setChecked(isSelected);

        holder.itemView.setOnClickListener(v -> {
            if (selectedIds.contains(item.getId())) {
                selectedIds.remove(item.getId());
            } else {
                selectedIds.add(item.getId());
            }
            notifyItemChanged(position);
            notifyListener();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void selectAll(boolean select) {
        selectedIds.clear();
        if (select) {
            for (VideoItem item : items) {
                selectedIds.add(item.getId());
            }
        }
        notifyDataSetChanged();
        notifyListener();
    }

    public List<VideoItem> getSelectedItems() {
        List<VideoItem> selected = new ArrayList<>();
        for (VideoItem item : items) {
            if (selectedIds.contains(item.getId())) {
                selected.add(item);
            }
        }
        return selected;
    }

    public long getSelectedTotalSize() {
        long total = 0;
        for (VideoItem item : items) {
            if (selectedIds.contains(item.getId())) {
                total += item.getSize();
            }
        }
        return total;
    }

    public boolean isAllSelected() {
        return !items.isEmpty() && selectedIds.size() == items.size();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onSelectionChanged(selectedIds.size(), getSelectedTotalSize());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivThumb;
        TextView tvDuration;
        TextView tvTitle;
        TextView tvInfo;
        TextView tvPath;

        ViewHolder(View view) {
            super(view);
            cbSelect = view.findViewById(R.id.cb_clean_item);
            ivThumb = view.findViewById(R.id.iv_clean_thumb);
            tvDuration = view.findViewById(R.id.tv_clean_duration);
            tvTitle = view.findViewById(R.id.tv_clean_item_title);
            tvInfo = view.findViewById(R.id.tv_clean_item_info);
            tvPath = view.findViewById(R.id.tv_clean_item_path);
        }
    }
}
