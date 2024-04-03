/**
 * @file CarFullscreenTaskMonitorListener.kt
 * @author Doan Van Ngoc/대리/메가존_MZ HMI
 *
 * © 2024 Hyundai Motor Company. All Rights Reserved.
 *
 * This software is copyright protected and proprietary to Hyundai Motor Company.
 * Do not copy without prior permission. Any copy of this software or of any
 * derivative work must include the above copyright notice, this paragraph and
 * the one after it.
 *
 * This software is made available on an "AS IS" condition, and Hyundai Motor Company
 * disclaims all warranties of any kind, whether express or implied, statutory or
 * otherwise, including without limitation any warranties of merchantability or
 * fitness for a particular purpose, absence of errors, accuracy, completeness of
 * results or the validity, scope, or non-infringement of any intellectual property.
 */

package com.example.demotaskview.task

import android.app.ActivityManager
import android.view.SurfaceControl
import com.android.wm.shell.common.SyncTransactionQueue
import com.android.wm.shell.fullscreen.FullscreenTaskListener

/**
 * The Car version of FullscreenTaskListener, which reports Task lifecycle to CarService.
 */
class CarFullscreenTaskMonitorListener(
    syncQueue: SyncTransactionQueue?
) : FullscreenTaskListener(syncQueue) {
    override fun onTaskAppeared(
        taskInfo: ActivityManager.RunningTaskInfo,
        leash: SurfaceControl
    ) {
        super.onTaskAppeared(taskInfo, leash)
        //        CarActivityManager carAM = mCarActivityManagerRef.get();
//        if (carAM != null) {
//            carAM.onTaskAppeared(taskInfo, leash);
//        } else {
//            Log.w(TAG, "CarActivityManager is null, skip onTaskAppeared: taskInfo=" + taskInfo);
//        }
    }

    override fun onTaskInfoChanged(taskInfo: ActivityManager.RunningTaskInfo) {
        super.onTaskInfoChanged(taskInfo)
        //        CarActivityManager carAM = mCarActivityManagerRef.get();
//        if (carAM != null) {
//            carAM.onTaskInfoChanged(taskInfo);
//        } else {
//            Log.w(TAG, "CarActivityManager is null, skip onTaskInfoChanged: taskInfo=" + taskInfo);
//        }
    }

    override fun onTaskVanished(taskInfo: ActivityManager.RunningTaskInfo) {
        super.onTaskVanished(taskInfo)
        //        CarActivityManager carAM = mCarActivityManagerRef.get();
//        if (carAM != null) {
//            carAM.onTaskVanished(taskInfo);
//        } else {
//            Log.w(TAG, "CarActivityManager is null, skip onTaskVanished: taskInfo=" + taskInfo);
//        }
    }
}
