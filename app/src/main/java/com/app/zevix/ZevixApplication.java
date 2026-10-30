package com.app.zevix;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.zevix.data.AppLockManager;
import com.app.zevix.data.TrashManager;
import com.app.zevix.ui.AppLockSetupActivity;
import com.app.zevix.ui.AppLockVerifyActivity;
import com.app.zevix.worker.TrashCleanupWorker;

import java.util.concurrent.Executors;

public class ZevixApplication extends Application {

    private static ZevixApplication instance;
    private int runningActivityCount = 0;
    private boolean isChangingConfig = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Schedule periodic 24-hour background auto-cleanup for trash expired after 30 days
        TrashCleanupWorker.schedulePeriodic(this);

        // Run immediate background check for expired trash and lingering deleted files
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                TrashManager tm = TrashManager.getInstance(this);
                tm.cleanExpiredTrash();
                tm.cleanupResidualPermDeletedFiles();
            } catch (Exception ignored) {}
        });

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (runningActivityCount == 0 && !isChangingConfig) {
                    // App came to foreground
                    AppLockManager lockManager = AppLockManager.getInstance(activity);
                    if (lockManager.isAppLockEnabled() && !lockManager.isAppUnlocked()) {
                        if (!(activity instanceof AppLockVerifyActivity) && !(activity instanceof AppLockSetupActivity)) {
                            Intent intent = new Intent(activity, AppLockVerifyActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            activity.startActivity(intent);
                        }
                    }
                }
                runningActivityCount++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                isChangingConfig = activity.isChangingConfigurations();
                runningActivityCount--;
                if (runningActivityCount <= 0 && !isChangingConfig) {
                    runningActivityCount = 0;
                    // App entered background - relock
                    AppLockManager.getInstance(activity).lockApp();
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    public static ZevixApplication getInstance() {
        return instance;
    }
}
