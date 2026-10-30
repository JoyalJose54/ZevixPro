package com.app.zevix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.app.zevix.R;
import com.app.zevix.data.AppLockManager;
import com.app.zevix.widget.PatternLockView;

import java.util.concurrent.Executor;

public class AppLockVerifyActivity extends AppCompatActivity {

    private TextView tvLockPrompt;
    private View layoutPinDots;
    private ImageView dot1, dot2, dot3, dot4;
    private View layoutKeypad;
    private View layoutPatternContainer;
    private PatternLockView patternLockView;
    private View btnBiometricTrigger;

    private AppLockManager lockManager;
    private final StringBuilder currentInputPin = new StringBuilder();
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private boolean isBiometricPromptShowing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock_verify);

        lockManager = AppLockManager.getInstance(this);

        initViews();
        setupLockMode();
        setupBiometrics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lockManager.isAppLockEnabled() && !lockManager.isAppUnlocked()) {
            if (lockManager.isBiometricEnabled() && lockManager.isBiometricSupported(this)) {
                new Handler(Looper.getMainLooper()).postDelayed(this::triggerBiometricPrompt, 200);
            }
        }
    }

    private void initViews() {
        tvLockPrompt = findViewById(R.id.tv_lock_prompt);
        layoutPinDots = findViewById(R.id.layout_pin_dots);
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        dot4 = findViewById(R.id.dot_4);
        layoutKeypad = findViewById(R.id.layout_keypad);
        layoutPatternContainer = findViewById(R.id.layout_pattern_container);
        patternLockView = findViewById(R.id.pattern_lock_view);
        btnBiometricTrigger = findViewById(R.id.btn_biometric_trigger);

        View btnCancel = findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finishAffinity());
        }

        setupKeypad();

        patternLockView.setOnPatternListener(new PatternLockView.OnPatternListener() {
            @Override
            public void onPatternComplete(String patternString) {
                if (lockManager.checkPattern(patternString)) {
                    onUnlockSuccess();
                } else {
                    patternLockView.setErrorState(true);
                    tvLockPrompt.setText("Incorrect pattern, try again");
                    tvLockPrompt.setTextColor(0xffff4444);
                }
            }

            @Override
            public void onPatternStarted() {
                tvLockPrompt.setText("Draw pattern to unlock");
                tvLockPrompt.setTextColor(0xff9e9e9e);
            }
        });

        if (btnBiometricTrigger != null) {
            btnBiometricTrigger.setOnClickListener(v -> triggerBiometricPrompt());
        }
    }

    private void setupLockMode() {
        String lockType = lockManager.getLockType();
        if (AppLockManager.LOCK_TYPE_PATTERN.equals(lockType) && lockManager.hasPattern()) {
            layoutPinDots.setVisibility(View.GONE);
            layoutKeypad.setVisibility(View.GONE);
            layoutPatternContainer.setVisibility(View.VISIBLE);
            tvLockPrompt.setText("Draw pattern to unlock");
        } else {
            layoutPatternContainer.setVisibility(View.GONE);
            layoutPinDots.setVisibility(View.VISIBLE);
            layoutKeypad.setVisibility(View.VISIBLE);
            tvLockPrompt.setText("Enter PIN to unlock");
        }
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
                if (lockManager.checkPin(currentInputPin.toString())) {
                    onUnlockSuccess();
                } else {
                    tvLockPrompt.setText("Incorrect PIN, try again");
                    tvLockPrompt.setTextColor(0xffff4444);
                    shakePinDots();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        currentInputPin.setLength(0);
                        updatePinDots();
                    }, 400);
                }
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

    private void shakePinDots() {
        Animation shake = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        layoutPinDots.startAnimation(shake);
    }

    private void setupBiometrics() {
        if (!lockManager.isBiometricEnabled() || !lockManager.isBiometricSupported(this)) {
            if (btnBiometricTrigger != null) btnBiometricTrigger.setVisibility(View.INVISIBLE);
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isBiometricPromptShowing = false;
                onUnlockSuccess();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                isBiometricPromptShowing = false;
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Zevix Pro")
                .setSubtitle("Use your biometric credential to access the app")
                .setNegativeButtonText("Use " + (AppLockManager.LOCK_TYPE_PATTERN.equals(lockManager.getLockType()) ? "Pattern" : "PIN"))
                .build();
    }

    private void triggerBiometricPrompt() {
        if (isBiometricPromptShowing) return;
        if (biometricPrompt != null && promptInfo != null) {
            try {
                isBiometricPromptShowing = true;
                biometricPrompt.authenticate(promptInfo);
            } catch (Exception ignored) {
                isBiometricPromptShowing = false;
            }
        }
    }

    private void onUnlockSuccess() {
        lockManager.setAppUnlocked(true);
        if (getIntent().getBooleanExtra("from_splash", false)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Prevent bypassing the lock screen
        moveTaskToBack(true);
    }
}
