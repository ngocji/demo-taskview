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
import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.Application
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.UserManager
import android.window.WindowContainerTransaction
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.common.HandlerExecutor
import com.android.wm.shell.common.SyncTransactionQueue
import com.android.wm.shell.common.TransactionPool
import com.android.wm.shell.common.annotations.ShellMainThread
import timber.log.Timber
import java.util.concurrent.Executor

class TaskViewManager(private val context: Activity, private val handler: Handler) {
    @ShellMainThread
    private val mShellExecutor = HandlerExecutor(handler)
    private val mSyncQueue =
        SyncTransactionQueue(TransactionPool(), mShellExecutor)
    private val mTaskOrganizer = ShellTaskOrganizer(mShellExecutor)
    val isHostVisible: Boolean
        get() = context.isVisibleForAutofill()
    var isPendingRestartActivity = false

    // All TaskView are bound to the Host Activity if it exists.
    @ShellMainThread
    val controlledTaskViews = mutableListOf<ControlledCarTaskView>()
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

            if (!hostFocused || isPendingRestartActivity) {
                return
            }

            // todo try open again taskview
            for (i in controlledTaskViews.indices.reversed()) {
                val taskView = controlledTaskViews[i]
                if (taskView.taskId == ActivityTaskManager.INVALID_TASK_ID) {
                    // If the task in TaskView is crashed when host is in background,
                    // We'd like to restart it when host becomes foreground and focused.
                    taskView.startActivity()
                }
            }
        }

        override fun onActivityRestartAttempt(
            task: ActivityManager.RunningTaskInfo,
            homeTaskVisible: Boolean,
            clearedTask: Boolean,
            wasVisible: Boolean,
        ) {
            Timber.d(
                "$TAG onActivityRestartAttempt: taskId=" + task.taskId
                        + ", homeTaskVisible=" + homeTaskVisible + ", wasVisible=" + wasVisible,
            )

            if (mHostTaskId != task.taskId || isPendingRestartActivity) {
                return
            }

            showEmbeddedTasks()
        }
    }

    private val packageBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("$TAG onReceive: intent=%s", intent)
            if (!isHostVisible) {
                return
            }
            val packageName = intent.data?.schemeSpecificPart.orEmpty()
            for (i in controlledTaskViews.indices.reversed()) {
                val taskView = controlledTaskViews[i]
                if (taskView.taskId == ActivityTaskManager.INVALID_TASK_ID
                    && taskView.getDependingPackageNames().contains(packageName)
                ) {
                    taskView.startActivity()
                }
            }
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
        val packageIntentFilter = IntentFilter(Intent.ACTION_PACKAGE_REPLACED)
        packageIntentFilter.addDataScheme(SCHEME_PACKAGE)
        context.registerReceiver(packageBroadcastReceiver, packageIntentFilter)
    }

    /**
     * Releases [TaskViewManager] and unregisters the underlying [ShellTaskOrganizer].
     * It also removes all TaskViews which are created by this [TaskViewManager].
     */
    fun release() {
        mShellExecutor.execute {
            Timber.d("$TAG TaskViewManager.release")
            TaskStackChangeListeners.instance.unregisterTaskStackListener(mTaskStackListener)
            context.unregisterReceiver(packageBroadcastReceiver)

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

    /**
     * Shows all the embedded tasks. If the tasks are
     */
    private fun showEmbeddedTasks() {
        val wct = WindowContainerTransaction()
        for (i in controlledTaskViews.indices.reversed()) {
            // showEmbeddedTasks() will restart the crashed tasks too.
            controlledTaskViews[i].showEmbeddedTask(wct)
        }
        mSyncQueue.queue(wct)
    }

    companion object {
        private val TAG = TaskViewManager::class.java.simpleName
        private const val SCHEME_PACKAGE = "package"
    }
}