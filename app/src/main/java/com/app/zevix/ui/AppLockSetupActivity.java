package com.app.zevix.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.app.zevix.R;
import com.app.zevix.data.AppLockManager;
import com.app.zevix.widget.PatternLockView;

public class AppLockSetupActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tabPin;
    private TextView tabPattern;
    private SwitchCompat switchBiometric;
    private TextView tvSetupPrompt;
    private TextView tvSetupHint;

    private View layoutPinDots;
    private ImageView dot1, dot2, dot3, dot4;
    private View layoutKeypad;

    private View layoutPatternContainer;
    private PatternLockView patternLockView;

    private AppLockManager lockManager;
    private String currentSelectedType = AppLockManager.LOCK_TYPE_PIN;

    // PIN Setup State
    private final StringBuilder currentInputPin = new StringBuilder();
    private String tempSetupPin = null;

    // Pattern Setup State
    private String tempSetupPattern = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock_setup);

        lockManager = AppLockManager.getInstance(this);

        initViews();
        setupKeypad();
        setupPattern();

        // Default to current lock type or PIN
        currentSelectedType = lockManager.getLockType();
        selectTab(currentSelectedType);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tabPin = findViewById(R.id.tab_pin);
        tabPattern = findViewById(R.id.tab_pattern);
        switchBiometric = findViewById(R.id.switch_biometric);
        tvSetupPrompt = findViewById(R.id.tv_setup_prompt);
        tvSetupHint = findViewById(R.id.tv_setup_hint);

        layoutPinDots = findViewById(R.id.layout_pin_dots);
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        dot4 = findViewById(R.id.dot_4);
        layoutKeypad = findViewById(R.id.layout_keypad);

        layoutPatternContainer = findViewById(R.id.layout_pattern_container);
        patternLockView = findViewById(R.id.pattern_lock_view);

        btnBack.setOnClickListener(v -> finish());
        View btnCancel = findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        tabPin.setOnClickListener(v -> selectTab(AppLockManager.LOCK_TYPE_PIN));
        tabPattern.setOnClickListener(v -> selectTab(AppLockManager.LOCK_TYPE_PATTERN));

        switchBiometric.setChecked(lockManager.isBiometricEnabled());
        switchBiometric.setOnCheckedChangeListener((btn, isChecked) -> lockManager.setBiometricEnabled(isChecked));

        if (!lockManager.isBiometricSupported(this)) {
            findViewById(R.id.row_biometric).setVisibility(View.GONE);
        }
    }

    private void selectTab(String type) {
        currentSelectedType = type;
        resetState();

        if (AppLockManager.LOCK_TYPE_PIN.equals(type)) {
            tabPin.setBackgroundResource(R.drawable.bg_chip_promoter_active);
            tabPin.setTextColor(0xffffffff);
            tabPattern.setBackgroundColor(0x00000000);
            tabPattern.setTextColor(0xff9aa0b0);

            layoutPinDots.setVisibility(View.VISIBLE);
            layoutKeypad.setVisibility(View.VISIBLE);
            layoutPatternContainer.setVisibility(View.GONE);

            tvSetupPrompt.setText("Set a 4-digit PIN");
            tvSetupHint.setText("Enter 4 numbers for your lock code");
        } else {
            tabPattern.setBackgroundResource(R.drawable.bg_chip_promoter_active);
            tabPattern.setTextColor(0xffffffff);
            tabPin.setBackgroundColor(0x00000000);
            tabPin.setTextColor(0xff9aa0b0);

            layoutPinDots.setVisibility(View.GONE);
            layoutKeypad.setVisibility(View.GONE);
            layoutPatternContainer.setVisibility(View.VISIBLE);

            tvSetupPrompt.setText("Draw an unlock pattern");
            tvSetupHint.setText("Connect at least 4 dots");
        }
    }

    private void resetState() {
        tempSetupPin = null;
        currentInputPin.setLength(0);
        updatePinDots();

        tempSetupPattern = null;
        if (patternLockView != null) {
            patternLockView.clearPattern();
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

        View btnBackspace = findViewById(R.id.key_backspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> onBackspacePressed());
        }
    }

    private void onDigitPressed(String digit) {
        if (currentInputPin.length() < 4) {
            currentInputPin.append(digit);
            updatePinDots();

            if (currentInputPin.length() == 4) {
                handlePinInput(currentInputPin.toString());
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

    private void handlePinInput(String pin) {
        if (tempSetupPin == null) {
            // First step
            tempSetupPin = pin;
            currentInputPin.setLength(0);
            updatePinDots();
            tvSetupPrompt.setText("Confirm your 4-digit PIN");
            tvSetupHint.setText("Re-enter your PIN to confirm");
        } else {
            // Second step
            if (tempSetupPin.equals(pin)) {
                lockManager.setLockType(AppLockManager.LOCK_TYPE_PIN);
                lockManager.setPin(pin);
                lockManager.setAppLockEnabled(true);
                lockManager.setAppUnlocked(true);
                Toast.makeText(this, "App Lock enabled with PIN!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "PINs did not match, try again", Toast.LENGTH_SHORT).show();
                tempSetupPin = null;
                currentInputPin.setLength(0);
                updatePinDots();
                tvSetupPrompt.setText("Set a 4-digit PIN");
                tvSetupHint.setText("Enter 4 numbers for your lock code");
            }
        }
    }

    private void setupPattern() {
        patternLockView.setOnPatternListener(new PatternLockView.OnPatternListener() {
            @Override
            public void onPatternComplete(String patternString) {
                String[] dots = patternString.split(",");
                if (dots.length < 4) {
                    Toast.makeText(AppLockSetupActivity.this, "Connect at least 4 dots", Toast.LENGTH_SHORT).show();
                    patternLockView.setErrorState(true);
                    return;
                }

                if (tempSetupPattern == null) {
                    tempSetupPattern = patternString;
                    tvSetupPrompt.setText("Draw pattern again to confirm");
                    tvSetupHint.setText("Draw your unlock pattern once more");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> patternLockView.clearPattern(), 300);
                } else {
                    if (tempSetupPattern.equals(patternString)) {
                        lockManager.setLockType(AppLockManager.LOCK_TYPE_PATTERN);
                        lockManager.setPattern(patternString);
                        lockManager.setAppLockEnabled(true);
                        lockManager.setAppUnlocked(true);
                        Toast.makeText(AppLockSetupActivity.this, "App Lock enabled with Pattern!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AppLockSetupActivity.this, "Patterns did not match, try again", Toast.LENGTH_SHORT).show();
                        patternLockView.setErrorState(true);
                        tempSetupPattern = null;
                        tvSetupPrompt.setText("Draw an unlock pattern");
                        tvSetupHint.setText("Connect at least 4 dots");
                    }
                }
            }

            @Override
            public void onPatternStarted() {
            }
        });
    }
}

