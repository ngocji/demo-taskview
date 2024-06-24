/**
 * @file TaskViewComponent.kt
 * @author Nguyen Xuan Truong
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
package com.example.demotaskview.taskview

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.android.wm.shell.common.HandlerExecutor
import com.example.demotaskview.taskview.extension.PRIVATE_FLAG_TRUSTED_OVERLAY
import com.example.demotaskview.taskview.extension.addPrivateFlags
import com.hyundai.taskview.CarTaskView
import timber.log.Timber


class TaskViewComponent constructor(
    private val context: Context
) {
    private var mTaskViewManager: TaskViewManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var mTaskView1: CarTaskView? = null
    private var mTaskView2: CarTaskView? = null

    private val mActivityManager: ActivityManager by lazy {
        context.getSystemService(ActivityManager::class.java)
    }
    private var mCarLauncherTaskId = ActivityTaskManager.INVALID_TASK_ID

    var activity: Activity? = null
    var viewContainer1: ViewGroup? = null
    var viewContainer2: ViewGroup? = null


    private val mTaskStackListener: TaskStackListener = object : TaskStackListener() {
        override fun onActivityRestartAttempt(
            task: ActivityManager.RunningTaskInfo?,
            homeTaskVisible: Boolean,
            clearedTask: Boolean,
            wasVisible: Boolean
        ) {
            super.onActivityRestartAttempt(task, homeTaskVisible, clearedTask, wasVisible)
            Timber.d(
                "onActivityRestartAttempt: taskId=" + task?.taskId
                        + ", homeTaskVisible=" + homeTaskVisible + ", wasVisible=" + wasVisible + "" +
                        " taskViewId1: ${mTaskView1?.taskId} taskViewId2: ${mTaskView2?.taskId}"
            )
//            if (!homeTaskVisible && mTaskView1?.taskId == task?.taskId) {
//            if (!homeTaskVisible) {
//                // The embedded map component received an intent, therefore forcibly bringing the
//                // launcher to the foreground.
//                mTaskView1?.notifyInitialized()
//                mTaskView2?.notifyInitialized()
//                return
//            }
        }
    }

    fun showEmbeddedTask1() {
        Timber.d("showEmbeddedMap")
        mTaskView1?.showEmbeddedTask()
    }


    fun showEmbeddedTask2() {
        Timber.d("showEmbeddedMap")
        mTaskView2?.showEmbeddedTask()
    }

    fun attachToViewGroup1(viewGroup: ViewGroup) {
        Timber.d("attachToViewGroup1: $viewGroup")
        viewContainer1 = viewGroup
        mTaskView1?.let {
            (it.parent as ViewGroup?)?.removeAllViews()
            viewGroup.removeAllViews()
            viewGroup.addView(it)
        }
    }

    fun attachToViewGroup2(viewGroup: ViewGroup) {
        Timber.d("attachToViewGroup2: $viewGroup")
        viewContainer2 = viewGroup
        mTaskView2?.let {
            (it.parent as ViewGroup?)?.removeAllViews()
            viewGroup.removeAllViews()
            viewGroup.addView(it)
        }
    }

    fun initTask(activity: Activity) {
        mTaskViewManager = TaskViewManager(activity, HandlerExecutor(handler))
        TaskStackChangeListeners.getInstance().registerTaskStackListener(mTaskStackListener)
        Timber.d("create: activity=$activity")
        this.activity = activity
        mCarLauncherTaskId = activity.taskId
        activity.window.apply {
            // Setting as trusted overlay to let touches pass through.
            addPrivateFlags(PRIVATE_FLAG_TRUSTED_OVERLAY)
            // To pass touches to the underneath task.
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        }
    }

    fun create1(activity: Activity, intent: Intent) {
//        if (!UserHelperLite.isHeadlessSystemUser(activity.getUserId())) {
        setUpTaskView1(activity, intent)
//        }
    }

    fun create2(activity: Activity, intent: Intent) {
//        if (!UserHelperLite.isHeadlessSystemUser(activity.getUserId())) {
        setUpTaskView2(activity, intent)
//        }
    }

    fun destroy() {
        Timber.d("destroy")
        TaskStackChangeListeners.getInstance().unregisterTaskStackListener(mTaskStackListener)
        mTaskView1 = null
        mTaskView2 = null
    }


    private fun setUpTaskView1(activity: Activity, intent: Intent) {
        Timber.d("setUpTaskView")
        val taskViewPackages = emptySet<String>()
//        val mapIntent = CarLauncherUtils.getMapsIntent().apply {
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//        }
        mTaskViewManager?.createControlledCarTaskView(
            ContextCompat.getMainExecutor(activity),
            intent, false, object : ControlledCarTaskViewCallbacks {
                override fun onTaskViewCreated(taskView: CarTaskView?) {
                    Timber.d("onTaskViewCreated")
                    viewContainer1?.apply {
                        removeAllViews()
                        addView(taskView)
                    }
                    mTaskView1 = taskView
                }

                override fun onTaskViewReady() {
                    Timber.d("onTaskViewReady")
                }

                override fun getDependingPackageNames(): MutableSet<String> {
                    return taskViewPackages.toMutableSet()
                }
            })
    }

    private fun setUpTaskView2(activity: Activity, intent: Intent) {
        Timber.d("setUpTaskView2")
        val taskViewPackages = emptySet<String>()
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        mTaskViewManager?.createControlledCarTaskView(
            ContextCompat.getMainExecutor(activity),
            intent, false, object : ControlledCarTaskViewCallbacks {
                override fun onTaskViewCreated(taskView: CarTaskView?) {
                    Timber.d("onTaskViewCreated")
                    viewContainer2?.apply {
                        removeAllViews()
                        addView(taskView)
                    }
                    mTaskView2 = taskView
                }

                override fun onTaskViewReady() {
                    Timber.d("onTaskViewReady")
                }

                override fun getDependingPackageNames(): MutableSet<String> {
                    return taskViewPackages.toMutableSet()
                }
            })
    }

    private fun bringToForeground() {
        Timber.d("bringToForeground $mCarLauncherTaskId")
        if (mCarLauncherTaskId != ActivityTaskManager.INVALID_TASK_ID) {
            mActivityManager.moveTaskToFront(mCarLauncherTaskId,  /* flags= */0)
        }
    }
}
