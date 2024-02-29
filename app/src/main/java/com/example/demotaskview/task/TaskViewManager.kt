/**
 * @file TaskViewManager2.kt
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

import android.app.Activity
import android.app.ActivityOptions
import android.app.Application
import android.app.PendingIntent
import android.app.TaskStackListener
import android.app.WindowConfiguration
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.UserManager
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.common.HandlerExecutor
import com.android.wm.shell.common.SyncTransactionQueue
import com.android.wm.shell.common.TransactionPool
import com.android.wm.shell.common.annotations.ShellMainThread
import com.android.wm.shell.taskview.TaskView
import timber.log.Timber
import java.util.concurrent.Executor

class TaskViewManager(private val context: Activity, private val handler: Handler) :
    ITaskViewManager {
    @ShellMainThread
    private val mShellExecutor = HandlerExecutor(handler)
    private val mSyncQueue =
        SyncTransactionQueue(TransactionPool(), mShellExecutor)
    private val mTaskOrganizer = ShellTaskOrganizer(mShellExecutor)
    override val isHostVisible: Boolean
        get() = context.isVisibleForAutofill()

    // All TaskView are bound to the Host Activity if it exists.
    @ShellMainThread
    override val controlledTaskViews = mutableListOf<ControlledCarTaskView>()
    private val mTaskViewInputInterceptor = TaskViewInputInterceptor(context, this)
    private var isReleased = false
    private var mHostTaskId = 0


    private val mActivityLifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle,
            ) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                release()
            }
        }

    private val mTaskStackListener: TaskStackListener = object : TaskStackListener() {
        override fun onTaskFocusChanged(taskId: Int, focused: Boolean) {
            val hostFocused = taskId == mHostTaskId && focused
            Timber.d(
                "$TAG onTaskFocusChanged: taskId=" + taskId
                        + ", hostFocused=" + hostFocused,
            )

            if (!hostFocused) {
                return
            }

            // todo try open again taskview
//            for (i in controlledTaskViews.indices.reversed()) {
//                val taskView = controlledTaskViews[i]
//                if (taskView.taskId == ActivityTaskManager.INVALID_TASK_ID) {
//                    // If the task in TaskView is crashed when host is in background,
//                    // We'd like to restart it when host becomes foreground and focused.
//                    taskView.startActivity()
//                }
//            }
        }
    }

    init {
        mHostTaskId = context.taskId
        initTaskStackChanged()
        context.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
    }

    fun createControlledCarTaskView(
        callbackExecutor: Executor,
        controlledCarTaskViewConfig: ControlledCarTaskViewConfig,
        taskViewCallbacks: ControlledCarTaskViewCallbacks,
    ) {
        mShellExecutor.execute {
            val taskView = ControlledCarTaskView(
                context,
                mTaskOrganizer,
                null,
                mSyncQueue,
                callbackExecutor,
                controlledCarTaskViewConfig,
                taskViewCallbacks,
                context.getSystemService(UserManager::class.java),
                this,
            )

            controlledTaskViews.add(taskView)
            if (controlledCarTaskViewConfig.mCaptureGestures
                || controlledCarTaskViewConfig.mCaptureLongPress
            ) {
                mTaskViewInputInterceptor.init()
            }
        }
    }

    private fun initTaskStackChanged() {
        TaskStackChangeListeners.instance.registerTaskStackListener(mTaskStackListener)
        // todo visible for testing
//        val packageIntentFilter = IntentFilter(Intent.ACTION_PACKAGE_REPLACED)
//        packageIntentFilter.addDataScheme(SCHEME_PACKAGE)
//        context.registerReceiver(packageBroadcastReceiver, packageIntentFilter)
    }

    /**
     * Releases [TaskViewManager] and unregisters the underlying [ShellTaskOrganizer].
     * It also removes all TaskViews which are created by this [TaskViewManager].
     */
    fun release() {
        mShellExecutor.execute {
            Timber.d("$TAG TaskViewManager.release")
            TaskStackChangeListeners.instance.unregisterTaskStackListener(mTaskStackListener)
//            context.unregisterReceiver(packageBroadcastReceiver) // todo visible for testing

            for (i in controlledTaskViews.indices.reversed()) {
                controlledTaskViews[i].release()
            }
            controlledTaskViews.clear()
            context.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
            mTaskOrganizer.unregisterOrganizer()
            mTaskViewInputInterceptor.release()
            isReleased = true
        }
    }

    private fun startActivityInternal(taskView: TaskView, config: ControlledCarTaskViewConfig) {
//        if (mUserManager?.isUserUnlocked == false) {
//            Timber.d(
//                "${ControlledCarTaskView.TAG} Can't start activity due to user is isn't unlocked"
//            )
//            return
//        }
//
//        // Don't start activity when the display is off. This can happen when the taskView is not
//        // attached to a window.
//        if (display == null) {
//            Timber.w(
//                "${ControlledCarTaskView.TAG} Can't start activity because display is not available in "
//                        + "taskView yet."
//            )
//            return
//        }
//        // Don't start activity when the display is off for ActivityVisibilityTests.
//        if (display.state != Display.STATE_ON) {
//            Timber.w("${ControlledCarTaskView.TAG} Can't start activity due to the display is off")
//            return
//        }
        val launchBounds = Rect()
        taskView.getBoundsOnScreen(launchBounds)
//        taskView.setWindowBounds(launchBounds)
        taskView.setObscuredTouchRect(launchBounds)


        val options = ActivityOptions.makeCustomAnimation(
            context,
            /* enterResId= */
            0,
            /* exitResId= */0
        )
            .setLaunchBounds(launchBounds)

        options.setLaunchWindowingMode(WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW)
        options.setRemoveWithTaskOrganizer(true)

        Timber.d(
            "${ControlledCarTaskView.TAG} Starting (" + config.mActivityIntent.component + ") on "
                    + launchBounds
        )
        var fillInIntent: Intent? = null
        if (config.mActivityIntent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS != 0) {
            fillInIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        taskView.startActivity(
            PendingIntent.getActivity(
                context,  /* requestCode= */0,
                config.mActivityIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ),
            fillInIntent, options, launchBounds
        )
    }

    companion object {
        private val TAG = TaskViewManager::class.java.simpleName
        private const val SCHEME_PACKAGE = "package"
    }
}