/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.example.demotaskview.task;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW;
import static android.content.ContentValues.TAG;
import static com.android.wm.shell.ShellTaskOrganizer.TASK_LISTENER_TYPE_FULLSCREEN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.TaskInfo;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.view.WindowManagerGlobal;
import android.window.TaskAppearedInfo;
import android.window.WindowContainerToken;
import android.window.WindowContainerTransaction;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.launcher3.icons.IconProvider;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.DisplayController;
import com.android.wm.shell.common.HandlerExecutor;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.common.TransactionPool;
import com.android.wm.shell.common.annotations.ShellMainThread;
import com.android.wm.shell.fullscreen.FullscreenTaskListener;
import com.android.wm.shell.startingsurface.StartingWindowController;
import com.android.wm.shell.startingsurface.phone.PhoneStartingWindowTypeAlgorithm;
import com.android.wm.shell.sysui.ShellCommandHandler;
import com.android.wm.shell.sysui.ShellController;
import com.android.wm.shell.sysui.ShellInit;
import com.android.wm.shell.taskview.TaskViewTransitions;
import com.android.wm.shell.transition.Transitions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;


/**
 * A manager for creating {@link ControlledCarTaskView}
 */
public final class TaskViewManager {
    public static final boolean DBG = true;
    private static final String SCHEME_PACKAGE = "package";

    private final AtomicReference<ComponentActivity> mCarActivityManagerRef =
        new AtomicReference<>();
    @ShellMainThread
    private final HandlerExecutor mShellExecutor;
    private final SyncTransactionQueue mSyncQueue;
    private final Transitions mTransitions;
    private final TaskViewTransitions mTaskViewTransitions;
    private final ShellTaskOrganizer mTaskOrganizer;
    private final int mHostTaskId;

    // All TaskView are bound to the Host Activity if it exists.
    @ShellMainThread
    private final List<ControlledCarTaskView> mControlledTaskViews = new ArrayList<>();
    private TaskViewInputInterceptor mTaskViewInputInterceptor;
    private Activity mContext;
    private boolean mReleased = false;

    private final TaskStackListener mTaskStackListener = new TaskStackListener() {
        @SuppressLint("TimberArgCount")
        @Override
        public void onTaskFocusChanged(int taskId, boolean focused) {
            boolean hostFocused = taskId == mHostTaskId && focused;
            if (DBG) {
                Timber.d(TAG, "onTaskFocusChanged: taskId=" + taskId
                    + ", hostFocused=" + hostFocused);
            }
            if (!hostFocused) {
                return;
            }

            for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                ControlledCarTaskView taskView = mControlledTaskViews.get(i);
                if (taskView.getTaskId() == INVALID_TASK_ID) {
                    // If the task in TaskView is crashed when host is in background,
                    // We'd like to restart it when host becomes foreground and focused.
                    taskView.startActivity();
                }
            }
        }

        @SuppressLint("TimberArgCount")
        @Override
        public void onActivityRestartAttempt(ActivityManager.RunningTaskInfo task,
                                             boolean homeTaskVisible, boolean clearedTask, boolean wasVisible) {
            if (DBG) {
                Timber.d(TAG, "onActivityRestartAttempt: taskId=" + task.taskId
                    + ", homeTaskVisible=" + homeTaskVisible + ", wasVisible=" + wasVisible);
            }
            if (mHostTaskId != task.taskId) {
                return;
            }
            showEmbeddedTasks();
        }
    };

    private final BroadcastReceiver mPackageBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("TimberArgCount")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DBG) Timber.d(TAG, "onReceive: intent=%s", intent);

            if (!isHostVisible()) {
                return;
            }

            String packageName = Objects.requireNonNull(intent.getData()).getSchemeSpecificPart();
            for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                ControlledCarTaskView taskView = mControlledTaskViews.get(i);
                if (taskView.getTaskId() == INVALID_TASK_ID
                    && taskView.getDependingPackageNames().contains(packageName)) {
                    taskView.startActivity();
                }
            }
        }
    };

    public TaskViewManager(Activity context, Handler mainHandler) {
        this(context, mainHandler, new HandlerExecutor(mainHandler));
    }

    private TaskViewManager(Activity context, Handler mainHandler,
                            HandlerExecutor handlerExecutor) {
        this(context, mainHandler, handlerExecutor, new ShellTaskOrganizer(handlerExecutor),
            new TransactionPool(), new ShellCommandHandler(), new ShellInit(handlerExecutor));
    }

    private TaskViewManager(Activity context, Handler mainHandler, HandlerExecutor handlerExecutor,
                            ShellTaskOrganizer taskOrganizer, TransactionPool transactionPool,
                            ShellCommandHandler shellCommandHandler, ShellInit shellinit) {
        this(context, mainHandler, handlerExecutor, taskOrganizer,
            transactionPool,
            shellinit,
            new ShellController(context, shellinit, shellCommandHandler, handlerExecutor),
            new DisplayController(context,
                WindowManagerGlobal.getWindowManagerService(), shellinit, handlerExecutor)
        );
    }

    private TaskViewManager(Activity context, Handler mainHandler, HandlerExecutor handlerExecutor,
                            ShellTaskOrganizer taskOrganizer, TransactionPool transactionPool, ShellInit shellinit,
                            ShellController shellController, DisplayController dc) {
        this(context, handlerExecutor, taskOrganizer,
            new SyncTransactionQueue(transactionPool, handlerExecutor),
            new Transitions(context, shellinit, shellController, taskOrganizer,
                transactionPool, dc, handlerExecutor, mainHandler, handlerExecutor),
            shellinit,
            shellController,
            new StartingWindowController(context, shellinit,
                shellController,
                taskOrganizer,
                handlerExecutor,
                new PhoneStartingWindowTypeAlgorithm(),
                new IconProvider(context),
                transactionPool));
    }

    @VisibleForTesting
    TaskViewManager(Activity context, HandlerExecutor handlerExecutor,
                    ShellTaskOrganizer shellTaskOrganizer, SyncTransactionQueue syncQueue,
                    Transitions transitions, ShellInit shellInit, ShellController shellController,
                    StartingWindowController startingWindowController) {
        mContext = context;
        mShellExecutor = handlerExecutor;
        mTaskOrganizer = shellTaskOrganizer;
        mHostTaskId = mContext.getTaskId();
        mSyncQueue = syncQueue;
        mTransitions = transitions;
        mTaskViewTransitions = new TaskViewTransitions(mTransitions);
        mTaskViewInputInterceptor = new TaskViewInputInterceptor(context, this);

        initCar();
        shellInit.init();
        initTaskOrganizer(mCarActivityManagerRef);
        mContext.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    private void initCar() {
        TaskStackChangeListeners.getInstance().registerTaskStackListener(mTaskStackListener);

        IntentFilter packageIntentFilter = new IntentFilter(Intent.ACTION_PACKAGE_REPLACED);
        packageIntentFilter.addDataScheme(SCHEME_PACKAGE);
        mContext.registerReceiver(mPackageBroadcastReceiver, packageIntentFilter);
    }

    private Transitions initTransitions(ShellInit shellInit, TransactionPool txPool,
                                        ShellController shellController, Handler mainHandler) {
        DisplayController dc = new DisplayController(mContext,
            WindowManagerGlobal.getWindowManagerService(), shellInit, mShellExecutor);
        return new Transitions(mContext, shellInit, shellController, mTaskOrganizer,
            txPool, dc, mShellExecutor, mainHandler, mShellExecutor);
    }

    private void initTaskOrganizer(AtomicReference<ComponentActivity> carActivityManagerRef) {
        FullscreenTaskListener fullscreenTaskListener = new CarFullscreenTaskMonitorListener(
            carActivityManagerRef, mSyncQueue);
        mTaskOrganizer.addListenerForType(fullscreenTaskListener, TASK_LISTENER_TYPE_FULLSCREEN);
        List<TaskAppearedInfo> taskAppearedInfos = mTaskOrganizer.registerOrganizer();
        cleanUpExistingTaskViewTasks(taskAppearedInfos);
    }

    /**
     * Creates a {@link ControlledCarTaskView}.
     *
     * @param callbackExecutor            the executor which the {@link ControlledCarTaskViewCallbacks} will
     *                                    be executed on.
     * @param controlledCarTaskViewConfig the configuration for the underlying
     *                                    {@link ControlledCarTaskView}.
     * @param taskViewCallbacks           the callbacks for the underlying TaskView.
     */
    public void createControlledCarTaskView(
        Executor callbackExecutor,
        ControlledCarTaskViewConfig controlledCarTaskViewConfig,
        ControlledCarTaskViewCallbacks taskViewCallbacks) {
        mShellExecutor.execute(() -> {
            ControlledCarTaskView taskView = new ControlledCarTaskView(mContext, mTaskOrganizer,
                mTaskViewTransitions, mSyncQueue, callbackExecutor, controlledCarTaskViewConfig,
                taskViewCallbacks, mContext.getSystemService(UserManager.class), this);
            mControlledTaskViews.add(taskView);

            if (controlledCarTaskViewConfig.mCaptureGestures
                || controlledCarTaskViewConfig.mCaptureLongPress) {
                mTaskViewInputInterceptor.init();
            }
        });

    }

    /**
     * updates the window visibility associated with {@link WindowContainerToken}
     *
     * @param token      {@link WindowContainerToken} of the window that needs to be hidden
     * @param visibility {true} if window needs to be displayed {false} otherwise
     */
    public void updateTaskVisibility(WindowContainerToken token, boolean visibility) {
        WindowContainerTransaction wct = new WindowContainerTransaction();
        wct.setHidden(token, !visibility);
        mSyncQueue.queue(wct);
    }

    /**
     * Releases {@link TaskViewManager} and unregisters the underlying {@link ShellTaskOrganizer}.
     * It also removes all TaskViews which are created by this {@link TaskViewManager}.
     */
    void release() {
        mShellExecutor.execute(() -> {
            if (DBG) Timber.d("%sTaskViewManager.release", TAG);

            TaskStackChangeListeners.getInstance().unregisterTaskStackListener(mTaskStackListener);
            mContext.unregisterReceiver(mPackageBroadcastReceiver);

            ComponentActivity carAM = mCarActivityManagerRef.get();
            if (carAM != null) {
                mCarActivityManagerRef.set(null);
            }

            for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                mControlledTaskViews.get(i).release();
            }
            mControlledTaskViews.clear();


            mContext.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
            mTaskOrganizer.unregisterOrganizer();
            mTaskViewInputInterceptor.release();

            mReleased = true;
        });
    }

    /**
     * Shows all the embedded tasks. If the tasks are
     */
    public void showEmbeddedTasks() {
        WindowContainerTransaction wct = new WindowContainerTransaction();
        for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
            // showEmbeddedTasks() will restart the crashed tasks too.
            mControlledTaskViews.get(i).showEmbeddedTask(wct);
        }

        mSyncQueue.queue(wct);
    }

    /**
     * @return {@code true} if the host activity is in resumed or started state, {@code false}
     * otherwise.
     */
    boolean isHostVisible() {
        // This code relies on Activity#isVisibleForAutofill() instead of maintaining a custom
        // activity state.

        return mContext.isVisibleForAutofill();
    }

    private final ActivityLifecycleCallbacks mActivityLifecycleCallbacks =
        new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity,
                                          @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity,
                                                    @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                release();
            }
        };

    @SuppressLint("TimberArgCount")
    private static void cleanUpExistingTaskViewTasks(List<TaskAppearedInfo> taskAppearedInfos) {
        ActivityTaskManager atm = ActivityTaskManager.getInstance();
        for (TaskAppearedInfo taskAppearedInfo : taskAppearedInfos) {
            TaskInfo taskInfo = taskAppearedInfo.getTaskInfo();
            // Only TaskView tasks have WINDOWING_MODE_MULTI_WINDOW.
            if (taskInfo.getWindowingMode() == WINDOWING_MODE_MULTI_WINDOW) {
                if (DBG) Timber.d(TAG, "Found the dangling task, removing: %s", taskInfo.taskId);
                atm.removeTask(taskInfo.taskId);
            }
        }
    }

    @VisibleForTesting
    List<ControlledCarTaskView> getControlledTaskViews() {
        return mControlledTaskViews;
    }

    @VisibleForTesting
    BroadcastReceiver getPackageBroadcastReceiver() {
        return mPackageBroadcastReceiver;
    }

    @VisibleForTesting
    /** Only meant for testing, should not be used by real code. */
    void setTaskViewInputInterceptor(TaskViewInputInterceptor taskViewInputInterceptor) {
        mTaskViewInputInterceptor = taskViewInputInterceptor;
    }

    boolean isReleased() {
        return mReleased;
    }
}
