package com.example.demotaskview.task;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Rect;
import android.os.UserManager;
import android.view.Display;
import android.view.SurfaceControl;
import android.window.WindowContainerTransaction;

import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.taskview.TaskViewTransitions;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.Executor;

import timber.log.Timber;

/**
 * A controlled {@link CarTaskView} is fully managed by the {@link TaskViewManager}.
 * The underlying task will be restarted if it is crashed.
 * <p>
 * It should be used when:
 * <ul>
 *     <li>The underlying task is meant to be started by the host and be there forever.</li>
 * </ul>
 */
@SuppressLint("ViewConstructor")
public class ControlledCarTaskView extends CarTaskView {
    private static final String TAG = ControlledCarTaskView.class.getSimpleName();

    private final Executor mCallbackExecutor;
    private final ControlledCarTaskViewCallbacks mCallbacks;
    private final UserManager mUserManager;
    private final TaskViewManager mTaskViewManager;
    private final ControlledCarTaskViewConfig mConfig;
    @Nullable
    private RunnerWithBackoff mStartActivityWithBackoff;

    ControlledCarTaskView(
        Activity context,
        ShellTaskOrganizer organizer,
        TaskViewTransitions taskViewTransitions,
        SyncTransactionQueue syncQueue,
        Executor callbackExecutor,
        ControlledCarTaskViewConfig controlledCarTaskViewConfig,
        ControlledCarTaskViewCallbacks callbacks,
        UserManager userManager,
        TaskViewManager taskViewManager) {
        super(context, organizer, taskViewTransitions, syncQueue, true);
        mCallbackExecutor = callbackExecutor;
        mConfig = controlledCarTaskViewConfig;
        mCallbacks = callbacks;
        mUserManager = userManager;
        mTaskViewManager = taskViewManager;

        mCallbackExecutor.execute(() -> mCallbacks.onTaskViewCreated(this));
        if (mConfig.mAutoRestartOnCrash) {
            mStartActivityWithBackoff = new RunnerWithBackoff(this::startActivityInternal);
        }
    }

    @Override
    protected void onCarTaskViewInitialized() {
        super.onCarTaskViewInitialized();
        startActivity();
        mCallbackExecutor.execute(mCallbacks::onTaskViewReady);
    }

    /**
     * Starts the underlying activity.
     */
    public void startActivity() {
        if (mStartActivityWithBackoff == null) {
            startActivityInternal();
            return;
        }
        mStartActivityWithBackoff.stop();
        mStartActivityWithBackoff.start();
    }

    private void stopTheStartActivityBackoffIfExists() {
        if (mStartActivityWithBackoff == null) {
            if (TaskViewManager.DBG) {
                Timber.d(TAG, "mStartActivityWithBackoff is not present.");
            }
            return;
        }
        mStartActivityWithBackoff.stop();
    }

    private void startActivityInternal() {
        if (!mUserManager.isUserUnlocked()) {
            if (TaskViewManager.DBG) Timber.d(TAG, "Can't start activity due to user is isn't unlocked");
            return;
        }

        // Don't start activity when the display is off. This can happen when the taskview is not
        // attached to a window.
        if (getDisplay() == null) {
            Timber.w(TAG, "Can't start activity because display is not available in "
                + "taskview yet.");
            return;
        }
        // Don't start activity when the display is off for ActivityVisibilityTests.
        if (getDisplay().getState() != Display.STATE_ON) {
            Timber.w(TAG, "Can't start activity due to the display is off");
            return;
        }

        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(),
            /* enterResId= */ 0, /* exitResId= */ 0);
        Rect launchBounds = new Rect();
        getBoundsOnScreen(launchBounds);
        if (TaskViewManager.DBG) {
            Timber.d(TAG, "Starting (" + mConfig.mActivityIntent.getComponent() + ") on "
                + launchBounds);
        }
        Intent fillInIntent = null;
        if ((mConfig.mActivityIntent.getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0) {
            fillInIntent = new Intent().addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
        startActivity(
            PendingIntent.getActivity(getContext(), /* requestCode= */ 0,
                mConfig.mActivityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT),
            fillInIntent, options, launchBounds);
    }

    /**
     * Gets the config used to build this controlled car task view.
     */
    ControlledCarTaskViewConfig getConfig() {
        return mConfig;
    }

    /**
     * See {@link ControlledCarTaskViewCallbacks#getDependingPackageNames()}.
     */
    Set<String> getDependingPackageNames() {
        return mCallbacks.getDependingPackageNames();
    }

    @Override
    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
        super.onTaskAppeared(taskInfo, leash);
        // Stop the start activity backoff because a task has already appeared.
        stopTheStartActivityBackoffIfExists();
    }

    @Override
    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        super.onTaskVanished(taskInfo);
        if (mConfig.mAutoRestartOnCrash && mTaskViewManager.isHostVisible()) {
            // onTaskVanished can be called when the host is in the background. In this case
            // embedded activity should not be started.
            Timber.i(TAG, "Restarting task " + taskInfo.baseActivity
                + " in ControlledCarTaskView");
            startActivity();
        }
    }

    @Override
    void showEmbeddedTask(WindowContainerTransaction wct) {
        if (getTaskInfo() == null) {
            if (TaskViewManager.DBG) {
                Timber.d(TAG, "Embedded task not available, starting it now.");
            }
            startActivity();
            return;
        }
        super.showEmbeddedTask(wct);
    }

    @Override
    public void release() {
        super.release();
        stopTheStartActivityBackoffIfExists();
    }
}
