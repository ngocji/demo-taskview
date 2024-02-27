package com.example.demotaskview.task;

import android.app.ActivityManager;
import android.view.SurfaceControl;

import androidx.activity.ComponentActivity;

import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.fullscreen.FullscreenTaskListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The Car version of FullscreenTaskListener, which reports Task lifecycle to CarService.
 */
public class CarFullscreenTaskMonitorListener extends FullscreenTaskListener {
    private static final String TAG = CarFullscreenTaskMonitorListener.class.getSimpleName();
    private final AtomicReference<ComponentActivity> mCarActivityManagerRef;

    public CarFullscreenTaskMonitorListener(
        AtomicReference<ComponentActivity> carActivityManagerRef,
        SyncTransactionQueue syncQueue) {
        super(syncQueue);
        mCarActivityManagerRef = carActivityManagerRef;
    }

    @Override
    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo,
                               SurfaceControl leash) {
        super.onTaskAppeared(taskInfo, leash);
//        CarActivityManager carAM = mCarActivityManagerRef.get();
//        if (carAM != null) {
//            carAM.onTaskAppeared(taskInfo, leash);
//        } else {
//            Log.w(TAG, "CarActivityManager is null, skip onTaskAppeared: taskInfo=" + taskInfo);
//        }
    }

    @Override
    public void onTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        super.onTaskInfoChanged(taskInfo);
//        CarActivityManager carAM = mCarActivityManagerRef.get();
//        if (carAM != null) {
//            carAM.onTaskInfoChanged(taskInfo);
//        } else {
//            Log.w(TAG, "CarActivityManager is null, skip onTaskInfoChanged: taskInfo=" + taskInfo);
//        }
    }

    @Override
    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        super.onTaskVanished(taskInfo);
//        CarActivityManager carAM = mCarActivityManagerRef.get();
//        if (carAM != null) {
//            carAM.onTaskVanished(taskInfo);
//        } else {
//            Log.w(TAG, "CarActivityManager is null, skip onTaskVanished: taskInfo=" + taskInfo);
//        }
    }
}
