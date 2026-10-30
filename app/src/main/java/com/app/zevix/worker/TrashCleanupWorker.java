package com.app.zevix.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.app.zevix.data.TrashManager;

import java.util.concurrent.TimeUnit;

public class TrashCleanupWorker extends Worker {

    public static final String WORK_NAME = "zevix_trash_auto_cleanup";

    public TrashCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            TrashManager trashManager = TrashManager.getInstance(getApplicationContext());
            trashManager.cleanExpiredTrash();
            trashManager.cleanupResidualPermDeletedFiles();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /**
     * Schedules a recurring 24-hour background task that automatically
     * permanently deletes files that have exceeded the 30-day retention period.
     */
    public static void schedulePeriodic(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build();

            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    TrashCleanupWorker.class,
                    24, TimeUnit.HOURS,
                    4, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            );
        } catch (Exception ignored) {}
    }
}

