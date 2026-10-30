package com.app.zevix.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zevix.R;
import com.app.zevix.adapter.VideoAdapter;
import com.app.zevix.data.MediaStoreRepository;
import com.app.zevix.data.PlaybackPreferences;
import com.app.zevix.data.PrivateVaultManager;
import com.app.zevix.model.VideoItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class FileProtectionActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private View layoutPin;
    private View layoutVault;

    // PIN Views
    private TextView tvPinPrompt;
    private TextView tvPinHint;
    private ImageView dot1, dot2, dot3, dot4;
    private ImageView btnPinBack;
    private View btnBiometricTrigger;

    // Vault Normal Views
    private View toolbarVaultNormal;
    private ImageView btnVaultBack;
    private TextView tvVaultTitle;
    private ImageView btnVaultCast;
    private ImageView btnVaultStyle;
    private ImageView btnVaultMore;
    private FloatingActionButton fabImport;

    // Vault Selection Views
    private View toolbarVaultSelect;
    private ImageView btnSelectClose;
    private TextView tvSelectCount;
    private ImageView btnActionRestore;
    private ImageView btnActionDelete;
    private ImageView btnSelectAll;

    // Vault List Views
    private RecyclerView rvVault;
    private View layoutVaultEmpty;
    private VideoAdapter adapter;

    // Preferences and State
    private PlaybackPreferences preferences;
    private final StringBuilder currentInputPin = new StringBuilder();
    private String tempSetupPin = null;
    private boolean isSetupMode = false;
    private boolean isGridMode = true;
    private boolean isVaultUnlocked = false;
    private boolean hasPromptedBiometrics = false;
    private List<VideoItem> vaultVideos = new ArrayList<>();

    // Biometrics
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private boolean isBiometricAvailable = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_protection);

        preferences = new PlaybackPreferences(this);
        isGridMode = preferences.isFolderGridMode();

        initViews();
        setupKeypad();
        setupBiometrics();

        isSetupMode = !preferences.hasPrivatePin();
        if (isSetupMode) {
            tvPinPrompt.setText("Set a 4-digit PIN");
            tvPinHint.setText("Create a PIN to protect your private videos");
            if (btnBiometricTrigger != null) {
                btnBiometricTrigger.setVisibility(View.INVISIBLE);
            }
        } else {
            tvPinPrompt.setText("Enter your 4-digit PIN");
            tvPinHint.setText("Files here are hidden and protected");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isSetupMode && !isVaultUnlocked && isBiometricAvailable && !hasPromptedBiometrics) {
            hasPromptedBiometrics = true;
            rvVault.post(this::showBiometricPrompt);
        }
    }

    private void initViews() {
        layoutPin = findViewById(R.id.layout_pin);
        layoutVault = findViewById(R.id.layout_vault);

        // PIN Section
        btnPinBack = findViewById(R.id.btn_pin_back);
        tvPinPrompt = findViewById(R.id.tv_pin_prompt);
        tvPinHint = findViewById(R.id.tv_pin_hint);
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        dot4 = findViewById(R.id.dot_4);
        btnBiometricTrigger = findViewById(R.id.btn_biometric_trigger);

        btnPinBack.setOnClickListener(v -> finish());
        View btnCancel = findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        // Vault Toolbar Normal
        toolbarVaultNormal = findViewById(R.id.toolbar_vault_normal);
        btnVaultBack = findViewById(R.id.btn_vault_back);
        tvVaultTitle = findViewById(R.id.tv_vault_title);
        btnVaultCast = findViewById(R.id.btn_vault_cast);
        btnVaultStyle = findViewById(R.id.btn_vault_style);
        btnVaultMore = findViewById(R.id.btn_vault_more);
        fabImport = findViewById(R.id.fab_import);

        btnVaultBack.setOnClickListener(v -> finish());
        btnVaultCast.setOnClickListener(v -> Toast.makeText(this, "Scanning for Cast displays...", Toast.LENGTH_SHORT).show());
        btnVaultStyle.setOnClickListener(v -> toggleLayoutStyle());
        btnVaultMore.setOnClickListener(this::showVaultMenu);
        fabImport.setOnClickListener(v -> openImportPicker());

        // Vault Toolbar Selection
        toolbarVaultSelect = findViewById(R.id.toolbar_vault_select);
        btnSelectClose = findViewById(R.id.btn_select_close);
        tvSelectCount = findViewById(R.id.tv_select_count);
        btnActionRestore = findViewById(R.id.btn_action_restore);
        btnActionDelete = findViewById(R.id.btn_action_delete);
        btnSelectAll = findViewById(R.id.btn_select_all);

        btnSelectClose.setOnClickListener(v -> exitMultiSelectMode());
        btnSelectAll.setOnClickListener(v -> {
            if (adapter.getSelectedCount() == vaultVideos.size()) {
                adapter.clearSelection();
            } else {
                adapter.selectAll();
            }
        });
        btnActionRestore.setOnClickListener(v -> restoreSelectedVideos());
        btnActionDelete.setOnClickListener(v -> deleteSelectedVideos());

        // Vault List & Empty State
        rvVault = findViewById(R.id.rv_vault);
        layoutVaultEmpty = findViewById(R.id.layout_vault_empty);

        adapter = new VideoAdapter(this, new ArrayList<>(), this);
        adapter.setOnVideoLongClickListener((video, position) -> {
            if (!adapter.isMultiSelectMode()) {
                enterMultiSelectMode(video);
                return true;
            }
            return false;
        });

        adapter.setOnSelectionChangeListener(count -> tvSelectCount.setText(count + " selected"));

        rvVault.setAdapter(adapter);
        updateLayoutMode();
    }

    private void setupBiometrics() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);

        isBiometricAvailable = (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS);

        if (isBiometricAvailable) {
            if (btnBiometricTrigger != null) {
                btnBiometricTrigger.setVisibility(View.VISIBLE);
                btnBiometricTrigger.setOnClickListener(v -> showBiometricPrompt());
            }

            Executor executor = ContextCompat.getMainExecutor(this);
            biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    unlockVault();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Private Videos")
                    .setSubtitle("Use your fingerprint to access private videos")
                    .setNegativeButtonText("Use PIN")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
        } else {
            if (btnBiometricTrigger != null) {
                btnBiometricTrigger.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showBiometricPrompt() {
        if (biometricPrompt != null && promptInfo != null && !isVaultUnlocked) {
            biometricPrompt.authenticate(promptInfo);
        }
    }

    private void updateLayoutMode() {
        if (isGridMode) {
            btnVaultStyle.setImageResource(R.drawable.ic_layout_grid_menu);
            rvVault.setLayoutManager(new GridLayoutManager(this, 3));
            adapter.setGridMode(true);
        } else {
            btnVaultStyle.setImageResource(R.drawable.ic_layout_list_menu);
            rvVault.setLayoutManager(new LinearLayoutManager(this));
            adapter.setGridMode(false);
        }
    }

    private void toggleLayoutStyle() {
        isGridMode = !isGridMode;
        preferences.setFolderGridMode(isGridMode);
        updateLayoutMode();
    }

    private void showVaultMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Select");
        popup.getMenu().add(0, 2, 1, "Sort by");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                enterMultiSelectMode(null);
                return true;
            } else if (item.getItemId() == 2) {
                SortDialogHelper.showSortDialog(this, false, preferences.getSortOrder(), (newOrder, label) -> {
                    preferences.setSortOrder(newOrder);
                    applySorting();
                    adapter.updateData(vaultVideos);
                });
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void enterMultiSelectMode(@Nullable VideoItem initialVideo) {
        adapter.setMultiSelectMode(true);
        if (initialVideo != null) {
            adapter.selectItem(initialVideo);
        }
        toolbarVaultNormal.setVisibility(View.GONE);
        toolbarVaultSelect.setVisibility(View.VISIBLE);
        if (fabImport != null) {
            fabImport.setVisibility(View.GONE);
        }
        tvSelectCount.setText(adapter.getSelectedCount() + " selected");
    }

    private void exitMultiSelectMode() {
        adapter.setMultiSelectMode(false);
        toolbarVaultSelect.setVisibility(View.GONE);
        toolbarVaultNormal.setVisibility(View.VISIBLE);
        if (fabImport != null) {
            fabImport.setVisibility(View.VISIBLE);
        }
    }

    private void restoreSelectedVideos() {
        List<VideoItem> selected = adapter.getSelectedVideos();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No videos selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Move out of folder")
                .setMessage("Move " + selected.size() + " video(s) back to public gallery?")
                .setPositiveButton("Move Out", (d, which) -> {
                    int count = 0;
                    for (VideoItem video : selected) {
                        if (PrivateVaultManager.restoreFromVault(this, video)) {
                            count++;
                        }
                    }
                    Toast.makeText(this, count + " video(s) moved out of private folder", Toast.LENGTH_SHORT).show();
                    exitMultiSelectMode();
                    loadVaultVideos();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelectedVideos() {
        List<VideoItem> selected = adapter.getSelectedVideos();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No videos selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                .setTitle("Delete Videos")
                .setMessage("Permanently delete " + selected.size() + " private video(s)?")
                .setPositiveButton("Delete", (d, which) -> {
                    for (VideoItem video : selected) {
                        PrivateVaultManager.deleteFromVault(this, video);
                    }
                    Toast.makeText(this, selected.size() + " video(s) deleted permanently", Toast.LENGTH_SHORT).show();
                    exitMultiSelectMode();
                    loadVaultVideos();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.isMultiSelectMode()) {
            exitMultiSelectMode();
            return;
        }
        super.onBackPressed();
    }

    private void setupKeypad() {
        int[] numKeyIds = new int[]{
                R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3,
                R.id.key_4, R.id.key_5, R.id.key_6, R.id.key_7,
                R.id.key_8, R.id.key_9
        };

        for (int id : numKeyIds) {
            TextView key = findViewById(id);
            if (key != null) {
                key.setOnClickListener(v -> onDigitPressed(key.getText().toString()));
            }
        }

        View btnBack = findViewById(R.id.key_backspace);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackspacePressed());
        }
    }

    private void onDigitPressed(String digit) {
        if (currentInputPin.length() < 4) {
            currentInputPin.append(digit);
            updatePinDots();

            if (currentInputPin.length() == 4) {
                handlePinEntered(currentInputPin.toString());
            }
        }
    }

    private void onBackspacePressed() {
        if (currentInputPin.length() > 0) {
            currentInputPin.deleteCharAt(currentInputPin.length() - 1);
            updatePinDots();
        }
    }

    private void updatePinDots() {
        int len = currentInputPin.length();
        dot1.setImageResource(len >= 1 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
        dot2.setImageResource(len >= 2 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
        dot3.setImageResource(len >= 3 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
        dot4.setImageResource(len >= 4 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
    }

    private void handlePinEntered(String pin) {
        if (isSetupMode) {
            if (tempSetupPin == null) {
                tempSetupPin = pin;
                currentInputPin.setLength(0);
                updatePinDots();
                tvPinPrompt.setText("Confirm your 4-digit PIN");
                tvPinHint.setText("Re-enter the PIN to confirm");
            } else {
                if (tempSetupPin.equals(pin)) {
                    preferences.setPrivatePin(pin);
                    Toast.makeText(this, "PIN successfully set!", Toast.LENGTH_SHORT).show();
                    unlockVault();
                } else {
                    Toast.makeText(this, "PINs did not match, try again", Toast.LENGTH_SHORT).show();
                    tempSetupPin = null;
                    currentInputPin.setLength(0);
                    updatePinDots();
                    tvPinPrompt.setText("Set a 4-digit PIN");
                }
            }
        } else {
            if (preferences.checkPrivatePin(pin)) {
                unlockVault();
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                currentInputPin.setLength(0);
                updatePinDots();
            }
        }
    }

    private void unlockVault() {
        isVaultUnlocked = true;
        layoutPin.setVisibility(View.GONE);
        layoutVault.setVisibility(View.VISIBLE);
        updateLayoutMode();
        loadVaultVideos();
    }

    private void loadVaultVideos() {
        vaultVideos = PrivateVaultManager.getVaultVideos(this);
        applySorting();
        adapter.updateData(vaultVideos);

        if (vaultVideos.isEmpty()) {
            layoutVaultEmpty.setVisibility(View.VISIBLE);
            rvVault.setVisibility(View.GONE);
        } else {
            layoutVaultEmpty.setVisibility(View.GONE);
            rvVault.setVisibility(View.VISIBLE);
        }
    }

    private void applySorting() {
        MediaStoreRepository.sortVideos(vaultVideos, preferences.getSortOrder());
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
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.Theme_ZevixPro_Dialog);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_video, null);
        dialog.setContentView(sheet);

        TextView title = sheet.findViewById(R.id.sheet_title);
        title.setText(video.getTitle());

        TextView actionLock = sheet.findViewById(R.id.action_lock);
        actionLock.setText("Move out of Private Folder");
        actionLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_unlock, 0, 0, 0);

        actionLock.setOnClickListener(v -> {
            dialog.dismiss();
            boolean restored = PrivateVaultManager.restoreFromVault(this, video);
            if (restored) {
                Toast.makeText(this, "Moved out to Movies folder", Toast.LENGTH_SHORT).show();
                loadVaultVideos();
            } else {
                Toast.makeText(this, "Failed to restore video", Toast.LENGTH_SHORT).show();
            }
        });

        sheet.findViewById(R.id.action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Delete Video")
                    .setMessage("Permanently delete this private video?")
                    .setPositiveButton("Delete", (d, which) -> {
                        PrivateVaultManager.deleteFromVault(this, video);
                        loadVaultVideos();
                        Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        sheet.findViewById(R.id.action_share).setOnClickListener(v -> {
            dialog.dismiss();
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("video/*");
            share.putExtra(Intent.EXTRA_STREAM, video.getUri());
            startActivity(Intent.createChooser(share, "Share Private Video"));
        });

        sheet.findViewById(R.id.action_convert_mp3).setOnClickListener(v -> {
            dialog.dismiss();
            onVideoClick(video, vaultVideos.indexOf(video), vaultVideos);
        });

        sheet.findViewById(R.id.action_add_to_playlist).setVisibility(View.GONE);
        sheet.findViewById(R.id.action_rename).setVisibility(View.GONE);
        sheet.findViewById(R.id.action_info).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, video.getTitle() + "\n" + video.getPath(), Toast.LENGTH_LONG).show();
        });

        dialog.show();
    }

    private final ActivityResultLauncher<String[]> importPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importUriToVault(uri);
                }
            });

    private void openImportPicker() {
        try {
            importPickerLauncher.launch(new String[]{"video/*"});
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void importUriToVault(Uri uri) {
        new Thread(() -> {
            try {
                String fileName = "Private_" + System.currentTimeMillis() + ".mp4";
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            String realName = cursor.getString(nameIndex);
                            if (realName != null && !realName.isEmpty()) {
                                fileName = realName;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                File dest = new File(PrivateVaultManager.getVaultDir(this), fileName);
                if (dest.exists()) {
                    dest = new File(PrivateVaultManager.getVaultDir(this), System.currentTimeMillis() + "_" + fileName);
                }

                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Video imported to Private Vault", Toast.LENGTH_SHORT).show();
                    loadVaultVideos();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to import video", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
