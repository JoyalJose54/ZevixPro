package com.app.zevix.ui;

import android.content.Intent;
import android.os.Bundle;
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

import com.app.zevix.R;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.data.PlaylistManager;
import com.app.zevix.model.VideoItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlaylistMainFragment extends Fragment {

    private View btnCreatePlaylist;
    private TextView tvTotalPlaylists;
    private View itemRecentlyPlayed;
    private TextView tvHistoryCount;
    private View itemFavorites;
    private TextView tvFavCount;
    private TextView tvCustomPlaylistsHeader;
    private RecyclerView rvCustomPlaylists;

    private PlaylistAdapter adapter;
    private final List<String> playlistNames = new ArrayList<>();
    private PlaybackPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_main, container, false);
        preferences = new PlaybackPreferences(requireContext());

        btnCreatePlaylist = view.findViewById(R.id.btn_create_playlist);
        tvTotalPlaylists = view.findViewById(R.id.tv_total_playlists);
        itemRecentlyPlayed = view.findViewById(R.id.item_recently_played);
        tvHistoryCount = view.findViewById(R.id.tv_history_count);
        itemFavorites = view.findViewById(R.id.item_favorites);
        tvFavCount = view.findViewById(R.id.tv_fav_count);
        tvCustomPlaylistsHeader = view.findViewById(R.id.tv_custom_playlists_header);
        rvCustomPlaylists = view.findViewById(R.id.rv_custom_playlists);

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        itemRecentlyPlayed.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_TYPE, PlaylistDetailActivity.TYPE_HISTORY);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, "History");
            startActivity(intent);
        });

        itemFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_TYPE, PlaylistDetailActivity.TYPE_FAVORITES);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, "My Favorites");
            startActivity(intent);
        });

        rvCustomPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistAdapter();
        rvCustomPlaylists.setAdapter(adapter);

        loadPlaylists();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void loadPlaylists() {
        if (getContext() == null) return;
        List<String> names = PlaylistManager.getInstance(requireContext()).getPlaylistNames();
        playlistNames.clear();
        playlistNames.addAll(names);
        adapter.notifyDataSetChanged();

        int customCount = names.size();
        int total = 2 + customCount; // History + Favorites + Custom
        tvTotalPlaylists.setText(total + (total == 1 ? " PLAYLIST" : " PLAYLISTS"));

        tvCustomPlaylistsHeader.setVisibility(customCount > 0 ? View.VISIBLE : View.GONE);
        rvCustomPlaylists.setVisibility(customCount > 0 ? View.VISIBLE : View.GONE);

        // Count favorites
        List<VideoItem> videos = MediaStoreRepository.getAllVideos(requireContext(), preferences.getSortOrder());
        int favCount = 0;
        int recentCount = 0;
        for (VideoItem v : videos) {
            if (preferences.isFavorite(v.getId())) favCount++;
            if (preferences.getPlaybackPosition(v.getId()) > 0) recentCount++;
        }
        tvFavCount.setText(favCount + (favCount == 1 ? " video" : " videos"));
        tvHistoryCount.setText(recentCount > 0 ? (recentCount + (recentCount == 1 ? " video" : " videos")) : "Recently played items");
    }

    private void showCreatePlaylistDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_playlist);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        EditText etName = dialog.findViewById(R.id.et_playlist_name);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnCreate = dialog.findViewById(R.id.btn_create);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                boolean created = PlaylistManager.getInstance(requireContext()).createPlaylist(name);
                if (created) {
                    Toast.makeText(getContext(), "Created playlist: " + name, Toast.LENGTH_SHORT).show();
                    loadPlaylists();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Playlist already exists or name invalid", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void showPlaylistMenu(String name, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Play All");
        popup.getMenu().add(0, 2, 1, "Rename");
        popup.getMenu().add(0, 3, 2, "Delete");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    playCustomPlaylist(name);
                    return true;
                case 2:
                    showRenameDialog(name);
                    return true;
                case 3:
                    showDeleteDialog(name);
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void playCustomPlaylist(String name) {
        Set<Long> ids = PlaylistManager.getInstance(requireContext()).getVideoIdsForPlaylist(name);
        List<VideoItem> all = MediaStoreRepository.getAllVideos(requireContext(), preferences.getSortOrder());
        List<VideoItem> plVideos = new ArrayList<>();
        for (VideoItem v : all) {
            if (ids.contains(v.getId())) plVideos.add(v);
        }
        if (plVideos.isEmpty()) {
            Toast.makeText(getContext(), "Playlist is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_PLAYLIST, (Serializable) plVideos);
        intent.putExtra(PlayerActivity.EXTRA_CURRENT_INDEX, 0);
        startActivity(intent);
    }

    private void showRenameDialog(String oldName) {
        EditText et = new EditText(requireContext());
        et.setText(oldName);
        et.setSelectAllOnFocus(true);
        et.setTextColor(getResources().getColor(R.color.white, requireContext().getTheme()));
        et.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                .setTitle("Rename Playlist")
                .setView(et)
                .setPositiveButton("RENAME", (dialog, which) -> {
                    String newName = et.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(oldName)) {
                        PlaylistManager.getInstance(requireContext()).renamePlaylist(oldName, newName);
                        loadPlaylists();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void showDeleteDialog(String name) {
        new AlertDialog.Builder(requireContext(), R.style.Theme_ZevixPro_Dialog)
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete playlist '" + name + "'?")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    PlaylistManager.getInstance(requireContext()).deletePlaylist(name);
                    loadPlaylists();
                    Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

        @NonNull
        @Override
        public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false);
            return new PlaylistViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
            String name = playlistNames.get(position);
            holder.tvName.setText(name);

            int count = PlaylistManager.getInstance(requireContext()).getVideoCount(name);
            holder.tvCount.setText(count + (count == 1 ? " video" : " videos"));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
                intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_TYPE, PlaylistDetailActivity.TYPE_CUSTOM);
                intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, name);
                startActivity(intent);
            });

            holder.btnMore.setOnClickListener(v -> showPlaylistMenu(name, holder.btnMore));
        }

        @Override
        public int getItemCount() {
            return playlistNames.size();
        }

        class PlaylistViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvCount;
            ImageView btnMore;

            PlaylistViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_playlist_name);
                tvCount = itemView.findViewById(R.id.tv_playlist_count);
                btnMore = itemView.findViewById(R.id.btn_playlist_more);
            }
        }
    }
}
