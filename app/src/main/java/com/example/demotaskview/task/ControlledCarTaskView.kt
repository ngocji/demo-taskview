/**
 * @file ControlledCarTaskView.kt
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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW
import android.content.Intent
import android.graphics.Rect
import android.os.UserManager
import android.view.Display
import android.view.SurfaceControl
import android.window.WindowContainerTransaction
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.common.SyncTransactionQueue
import com.android.wm.shell.taskview.TaskViewTransitions
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * A controlled {@link CarTaskView} is fully managed by the {@link TaskViewManager}.
 * The underlying task will be restarted if it is crashed.
 * <p>
 * It should be used when:
 * <ul>
 *     <li>The underlying task is meant to be started by the host and be there forever.</li>
 * </ul>
 *
 * @noinspection BlockingMethodInNonBlockingContext
 */
class ControlledCarTaskView(
    context: Activity,
    organizer: ShellTaskOrganizer,
    taskViewTransitions: TaskViewTransitions?,
    syncQueue: SyncTransactionQueue,
    callbackExecutor: Executor,
    controlledCarTaskViewConfig: ControlledCarTaskViewConfig,
    callbacks: ControlledCarTaskViewCallbacks,
    userManager: UserManager,
    taskViewManager: TaskViewManager
) : CarTaskView(context, organizer, taskViewTransitions, syncQueue, true) {
    private var mCallbackExecutor: Executor? = null
    private var mCallbacks: ControlledCarTaskViewCallbacks? = null
    private var mUserManager: UserManager? = null
    private val mTaskViewManager: TaskViewManager
    private val mConfig: ControlledCarTaskViewConfig
    private var mStartActivityWithBackoff: RunnerWithBackoff? = null

    init {
        mCallbackExecutor = callbackExecutor
        mConfig = controlledCarTaskViewConfig
        mCallbacks = callbacks
        mUserManager = userManager
        mTaskViewManager = taskViewManager
        mCallbackExecutor?.execute({ mCallbacks?.onTaskViewCreated(this) })
        if (mConfig.mAutoRestartOnCrash) {
            mStartActivityWithBackoff = RunnerWithBackoff { this.startActivityInternal() }
        }
    }

    override fun onCarTaskViewInitialized() {
        super.onCarTaskViewInitialized()
        startActivity()
        mCallbackExecutor?.execute { mCallbacks?.onTaskViewReady() }
    }

    /**
     * Starts the underlying activity.
     */
    fun startActivity() {
        if (mStartActivityWithBackoff == null) {
            startActivityInternal()
            return
        }
        mStartActivityWithBackoff?.stop()
        mStartActivityWithBackoff?.start()
    }

    private fun stopTheStartActivityBackoffIfExists() {
        if (mStartActivityWithBackoff == null) {
            Timber.d("$TAG mStartActivityWithBackoff is not present.")
            return
        }
        mStartActivityWithBackoff?.stop()
    }

    @SuppressLint("IntentWithNullActionLaunch")
    private fun startActivityInternal() {
        if (mUserManager?.isUserUnlocked == false) {
            Timber.d(
                "$TAG Can't start activity due to user is isn't unlocked"
            )
            return
        }

        // Don't start activity when the display is off. This can happen when the taskView is not
        // attached to a window.
        if (display == null) {
            Timber.w(
                "$TAG Can't start activity because display is not available in "
                        + "taskView yet."
            )
            return
        }
        // Don't start activity when the display is off for ActivityVisibilityTests.
        if (display.state != Display.STATE_ON) {
            Timber.w("$TAG Can't start activity due to the display is off")
            return
        }

        val launchBounds = Rect()
        getBoundsOnScreen(launchBounds)
        setWindowBounds(launchBounds)
        setObscuredTouchRect(launchBounds)


        val options = ActivityOptions.makeCustomAnimation(
            context,
            /* enterResId= */
            0,
            /* exitResId= */0
        )
            .setLaunchBounds(launchBounds)

        options.setLaunchWindowingMode(WINDOWING_MODE_MULTI_WINDOW)
        options.setRemoveWithTaskOrganizer(true)

        Timber.d(
            "$TAG Starting (" + mConfig.mActivityIntent.component + ") on "
                    + launchBounds
        )
        var fillInIntent: Intent? = null
        if (mConfig.mActivityIntent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS != 0) {
            fillInIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        startActivity(
            PendingIntent.getActivity(
                context,  /* requestCode= */0,
                mConfig.mActivityIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ),
            fillInIntent, options, launchBounds
        )
    }

    /**
     * Gets the config used to build this controlled car task view.
     */
    fun getConfig(): ControlledCarTaskViewConfig {
        return mConfig
    }

    /**
     * See [ControlledCarTaskViewCallbacks.getDependingPackageNames].
     */
    fun getDependingPackageNames(): Set<String> {
        return mCallbacks?.getDependingPackageNames() ?: emptySet()
    }


    override fun onTaskAppeared(
        taskInfo: ActivityManager.RunningTaskInfo?,
        leash: SurfaceControl?
    ) {
        super.onTaskAppeared(taskInfo, leash)
        // Stop the start activity backoff because a task has already appeared.
        stopTheStartActivityBackoffIfExists()
    }

    override fun onTaskVanished(taskInfo: ActivityManager.RunningTaskInfo?) {
        super.onTaskVanished(taskInfo)
        if (mConfig.mAutoRestartOnCrash && mTaskViewManager.isHostVisible) {
            // onTaskVanished can be called when the host is in the background. In this case
            // embedded activity should not be started.
            Timber.i(
                "$TAG Restarting task " + taskInfo?.baseActivity
                        + " in ControlledCarTaskView"
            )
            startActivity()
        }
    }

    override fun showEmbeddedTask(wct: WindowContainerTransaction) {
        if (taskInfo == null) {
            Timber.d("$TAG Embedded task not available, starting it now.")
            startActivity()
            return
        }
        super.showEmbeddedTask(wct)
    }

    override fun release() {
        super.release()
        stopTheStartActivityBackoffIfExists()
    }

    companion object {
         val TAG = ControlledCarTaskView::class.java.simpleName
    }
}
