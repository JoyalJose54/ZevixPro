package com.app.zevix.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.airbnb.lottie.LottieAnimationView;
import com.app.zevix.R;

import java.util.concurrent.atomic.AtomicBoolean;

public class SplashActivity extends AppCompatActivity {

    private LottieAnimationView lottieSplash;
    private final AtomicBoolean isNavigated = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.BLACK);
            getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        }
        setContentView(R.layout.activity_splash);

        lottieSplash = findViewById(R.id.lottie_splash);
        View splashRoot = findViewById(R.id.splash_root);

        View.OnClickListener skipListener = v -> navigateToMain();

        if (splashRoot != null) {
            splashRoot.setOnClickListener(skipListener);
        }

        if (lottieSplash != null) {
            lottieSplash.setOnClickListener(skipListener);
            lottieSplash.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    navigateToMain();
                }
            });
        }
    }

    private void navigateToMain() {
        if (isNavigated.compareAndSet(false, true)) {
            if (lottieSplash != null) {
                try {
                    lottieSplash.cancelAnimation();
                } catch (Exception ignored) {}
            }
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }
}
