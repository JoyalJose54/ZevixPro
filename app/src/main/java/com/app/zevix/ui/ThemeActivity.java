package com.app.zevix.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.app.zevix.R;
import com.app.zevix.data.PlaybackPreferences;

public class ThemeActivity extends AppCompatActivity {

    public static class ThemeItem {
        final String name;
        final int primaryColor;
        final int accentColor;
        final int bgColor;

        ThemeItem(String name, int primaryColor, int accentColor, int bgColor) {
            this.name = name;
            this.primaryColor = primaryColor;
            this.accentColor = accentColor;
            this.bgColor = bgColor;
        }
    }

    public static final ThemeItem[] THEMES = new ThemeItem[]{
            new ThemeItem("Dark OLED (Default)", Color.parseColor("#1b212d"), Color.parseColor("#FF9800"), Color.parseColor("#121620")),
            new ThemeItem("Midnight Blue", Color.parseColor("#152238"), Color.parseColor("#2196F3"), Color.parseColor("#0e1626")),
            new ThemeItem("Emerald Green", Color.parseColor("#1b3323"), Color.parseColor("#4CAF50"), Color.parseColor("#102117")),
            new ThemeItem("Crimson Red", Color.parseColor("#331818"), Color.parseColor("#F44336"), Color.parseColor("#210f0f")),
            new ThemeItem("Royal Purple", Color.parseColor("#291b33"), Color.parseColor("#9C27B0"), Color.parseColor("#1a1021")),
            new ThemeItem("Classic Carbon", Color.parseColor("#262626"), Color.parseColor("#757575"), Color.parseColor("#191919"))
    };

    private ImageView btnBack;
    private TextView tvTitle;
    private CardView cardPreview;
    private View previewToolbar;
    private ImageView previewAccentDot;
    private TextView previewThemeName;
    private ProgressBar previewSeek;
    private View previewBottomNav;
    private TextView previewTabActive;
    private LinearLayout layoutThemeSwatches;
    private Button btnUseTheme;

    private PlaybackPreferences preferences;
    private int currentSelectedThemeIndex = 0;
    private int appliedThemeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme);

        preferences = new PlaybackPreferences(this);
        appliedThemeIndex = preferences.getThemeIndex();
        if (appliedThemeIndex < 0 || appliedThemeIndex >= THEMES.length) appliedThemeIndex = 0;
        currentSelectedThemeIndex = appliedThemeIndex;

        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        cardPreview = findViewById(R.id.card_preview);
        previewToolbar = findViewById(R.id.preview_toolbar);
        previewAccentDot = findViewById(R.id.preview_accent_dot);
        previewThemeName = findViewById(R.id.preview_theme_name);
        previewSeek = findViewById(R.id.preview_seek);
        previewBottomNav = findViewById(R.id.preview_bottom_nav);
        previewTabActive = findViewById(R.id.preview_tab_active);
        layoutThemeSwatches = findViewById(R.id.layout_theme_swatches);
        btnUseTheme = findViewById(R.id.btn_use_theme);

        btnBack.setOnClickListener(v -> finish());

        setupSwatchesUI();
        updatePreviewUI(currentSelectedThemeIndex);

        btnUseTheme.setOnClickListener(v -> {
            appliedThemeIndex = currentSelectedThemeIndex;
            preferences.setThemeIndex(appliedThemeIndex);
            updatePreviewUI(currentSelectedThemeIndex);
            setupSwatchesUI();
            Toast.makeText(this, "Theme applied: " + THEMES[appliedThemeIndex].name, Toast.LENGTH_SHORT).show();
        });
    }

    private void updatePreviewUI(int index) {
        ThemeItem item = THEMES[index];
        tvTitle.setText(item.name);
        previewThemeName.setText(item.name);

        cardPreview.setCardBackgroundColor(item.bgColor);
        previewToolbar.setBackgroundColor(item.primaryColor);
        previewBottomNav.setBackgroundColor(item.primaryColor);

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(item.accentColor);
        previewAccentDot.setBackground(dot);

        previewSeek.setProgressTintList(ColorStateList.valueOf(item.accentColor));
        previewTabActive.setTextColor(item.accentColor);

        if (index == appliedThemeIndex) {
            btnUseTheme.setText("In use");
            btnUseTheme.setEnabled(false);
            btnUseTheme.setAlpha(0.6f);
        } else {
            btnUseTheme.setText("Apply Theme");
            btnUseTheme.setEnabled(true);
            btnUseTheme.setAlpha(1.0f);
        }
    }

    private void setupSwatchesUI() {
        layoutThemeSwatches.removeAllViews();
        for (int i = 0; i < THEMES.length; i++) {
            final int index = i;
            ThemeItem item = THEMES[i];

            View swatch = new View(this);
            int sizeDp = (i == currentSelectedThemeIndex) ? 48 : 40;
            int px = (int) (sizeDp * getResources().getDisplayMetrics().density);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px, px);
            lp.setMargins(16, 0, 16, 0);
            lp.gravity = Gravity.CENTER_VERTICAL;
            swatch.setLayoutParams(lp);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(item.accentColor);
            if (i == currentSelectedThemeIndex) {
                drawable.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.WHITE);
            }
            swatch.setBackground(drawable);

            swatch.setOnClickListener(v -> {
                currentSelectedThemeIndex = index;
                updatePreviewUI(index);
                setupSwatchesUI();
            });

            layoutThemeSwatches.addView(swatch);
        }
    }
}

