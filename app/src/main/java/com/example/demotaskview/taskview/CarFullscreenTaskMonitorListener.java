/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.demotaskview.taskview;

import android.app.ActivityManager;
import android.car.app.CarActivityManager;
import android.view.SurfaceControl;

import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.fullscreen.FullscreenTaskListener;

import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

/**
 * The Car version of FullscreenTaskListener, which reports Task lifecycle to CarService.
 */
public class CarFullscreenTaskMonitorListener extends FullscreenTaskListener {
    private final AtomicReference<CarActivityManager> mCarActivityManagerRef;

    public CarFullscreenTaskMonitorListener(
        AtomicReference<CarActivityManager> carActivityManagerRef,
        SyncTransactionQueue syncQueue) {
        super(syncQueue);
        mCarActivityManagerRef = carActivityManagerRef;
    }

    @Override
    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo,
                               SurfaceControl leash) {
        super.onTaskAppeared(taskInfo, leash);
        Timber.d("onTaskAppeared: taskInfo=%s", taskInfo);
        CarActivityManager carAM = mCarActivityManagerRef.get();
        if (carAM != null) {
            carAM.onTaskAppeared(taskInfo);
        } else {
            Timber.w("CarActivityManager is null, skip onTaskAppeared: taskInfo=%s", taskInfo);
        }
    }

    @Override
    public void onTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        super.onTaskInfoChanged(taskInfo);
        Timber.d("onTaskInfoChanged: taskInfo=%s", taskInfo);
        CarActivityManager carAM = mCarActivityManagerRef.get();
        if (carAM != null) {
            carAM.onTaskInfoChanged(taskInfo);
        } else {
            Timber.w("CarActivityManager is null, skip onTaskInfoChanged: taskInfo=%s", taskInfo);
        }
    }

    @Override
    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        super.onTaskVanished(taskInfo);
        Timber.d("onTaskVanished: taskInfo=%s", taskInfo);
        CarActivityManager carAM = mCarActivityManagerRef.get();
        if (carAM != null) {
            carAM.onTaskVanished(taskInfo);
        } else {
            Timber.w("CarActivityManager is null, skip onTaskVanished: taskInfo=%s", taskInfo);
        }
    }
}
