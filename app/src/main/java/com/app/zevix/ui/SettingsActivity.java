package com.app.zevix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.app.zevix.R;
import com.app.zevix.data.AppLockManager;
import com.app.zevix.data.PlaybackPreferences;
import com.bumptech.glide.Glide;

import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private SwitchCompat switchHwAccel;
    private SwitchCompat switchResume;
    private SwitchCompat switchMiniplayer;
    private TextView tvSeekInterval;

    // App Lock Views
    private SwitchCompat switchAppLock;
    private TextView tvAppLockStatus;
    private View rowChangeLock;
    private TextView tvCurrentLockType;
    private View rowBiometricToggle;
    private SwitchCompat switchBiometricToggle;

    private PlaybackPreferences preferences;
    private AppLockManager lockManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = new PlaybackPreferences(this);
        lockManager = AppLockManager.getInstance(this);

        btnBack = findViewById(R.id.btn_back);
        switchHwAccel = findViewById(R.id.switch_hw_accel);
        switchResume = findViewById(R.id.switch_resume);
        switchMiniplayer = findViewById(R.id.switch_miniplayer);
        tvSeekInterval = findViewById(R.id.tv_seek_interval);

        switchAppLock = findViewById(R.id.switch_app_lock);
        tvAppLockStatus = findViewById(R.id.tv_app_lock_status);
        rowChangeLock = findViewById(R.id.row_change_lock);
        tvCurrentLockType = findViewById(R.id.tv_current_lock_type);
        rowBiometricToggle = findViewById(R.id.row_biometric_toggle);
        switchBiometricToggle = findViewById(R.id.switch_biometric_toggle);

        btnBack.setOnClickListener(v -> finish());

        setupAppLockSection();

        findViewById(R.id.row_seek_interval).setOnClickListener(v -> {
            String[] intervals = new String[]{"5 seconds", "10 seconds", "15 seconds", "30 seconds"};
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Seek Interval")
                    .setItems(intervals, (d, which) -> {
                        tvSeekInterval.setText(intervals[which]);
                        Toast.makeText(this, "Seek interval set to " + intervals[which], Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        findViewById(R.id.row_equalizer).setOnClickListener(v -> {
            startActivity(new Intent(this, EqualizerActivity.class));
        });

        findViewById(R.id.row_clear_cache).setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                Glide.get(getApplicationContext()).clearDiskCache();
                runOnUiThread(() -> Toast.makeText(this, "Video thumbnail cache cleared", Toast.LENGTH_SHORT).show());
            });
        });

        findViewById(R.id.row_about).setOnClickListener(v -> {
            new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                    .setTitle("Zevix Pro")
                    .setMessage("Zevix Pro - All Format Video Player\nVersion 2.9.3 (Build 293001)\n\nPure Java Implementation for Android.")
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void setupAppLockSection() {
        switchAppLock.setOnClickListener(v -> {
            boolean isChecked = switchAppLock.isChecked();
            if (isChecked) {
                if (lockManager.hasPin() || lockManager.hasPattern()) {
                    lockManager.setAppLockEnabled(true);
                    updateAppLockUI();
                    Toast.makeText(this, "App Lock enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // Open setup flow
                    switchAppLock.setChecked(false);
                    startActivity(new Intent(this, AppLockSetupActivity.class));
                }
            } else {
                new AlertDialog.Builder(this, R.style.Theme_ZevixPro_Dialog)
                        .setTitle("Disable App Lock")
                        .setMessage("Are you sure you want to turn off App Lock?")
                        .setPositiveButton("Turn Off", (dialog, which) -> {
                            lockManager.setAppLockEnabled(false);
                            updateAppLockUI();
                            Toast.makeText(this, "App Lock turned off", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            switchAppLock.setChecked(true);
                        })
                        .show();
            }
        });

        rowChangeLock.setOnClickListener(v -> {
            startActivity(new Intent(this, AppLockSetupActivity.class));
        });

        switchBiometricToggle.setOnCheckedChangeListener((btn, isChecked) -> {
            lockManager.setBiometricEnabled(isChecked);
        });
    }

    private void updateAppLockUI() {
        boolean enabled = lockManager.isAppLockEnabled();
        switchAppLock.setChecked(enabled);

        if (enabled) {
            String lockType = lockManager.getLockType();
            tvAppLockStatus.setText("Locked with " + (AppLockManager.LOCK_TYPE_PATTERN.equals(lockType) ? "Pattern" : "PIN"));
            tvCurrentLockType.setText(AppLockManager.LOCK_TYPE_PATTERN.equals(lockType) ? "Pattern" : "PIN");
            rowChangeLock.setVisibility(View.VISIBLE);

            if (lockManager.isBiometricSupported(this)) {
                rowBiometricToggle.setVisibility(View.VISIBLE);
                switchBiometricToggle.setChecked(lockManager.isBiometricEnabled());
            } else {
                rowBiometricToggle.setVisibility(View.GONE);
            }
        } else {
            tvAppLockStatus.setText("Lock Zevix with PIN, Pattern or Biometrics");
            rowChangeLock.setVisibility(View.GONE);
            rowBiometricToggle.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAppLockUI();
    }
}
