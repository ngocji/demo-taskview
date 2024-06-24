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

package com.example.demotaskview.taskview;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW;
import static android.car.user.CarUserManager.USER_LIFECYCLE_EVENT_TYPE_SWITCHING;
import static android.car.user.CarUserManager.USER_LIFECYCLE_EVENT_TYPE_UNLOCKED;
import static com.android.wm.shell.ShellTaskOrganizer.TASK_LISTENER_TYPE_FULLSCREEN;
import static com.hyundai.taskview.extension.ActivityExtKt.getUserId;
import static com.hyundai.taskview.extension.ActivityExtKt.isVisibleForAutofill;
import static com.hyundai.taskview.extension.TaskInfoExtKt.getWindowingMode;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.TaskInfo;
import android.app.TaskStackListener;
import android.car.Car;
import android.car.app.CarActivityManager;
import android.car.user.CarUserManager;
import android.car.user.UserLifecycleEventFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.window.TaskAppearedInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.HandlerExecutor;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.common.TransactionPool;
import com.android.wm.shell.common.annotations.ShellMainThread;
import com.android.wm.shell.fullscreen.FullscreenTaskListener;
import com.android.wm.shell.startingsurface.StartingWindowController;
import com.android.wm.shell.startingsurface.phone.PhoneStartingWindowTypeAlgorithm;
import com.android.wm.shell.sysui.ShellInit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;


/**
 * A manager for creating {@link ControlledCarTaskView}.
 */
public final class TaskViewManager {
    private static final String SCHEME_PACKAGE = "package";

    private final AtomicReference<CarActivityManager> mCarActivityManagerRef =
            new AtomicReference<>();
    @ShellMainThread
    private final HandlerExecutor mShellExecutor;
    private final SyncTransactionQueue mSyncQueue;
    private final ShellTaskOrganizer mTaskOrganizer;
    private final int mHostTaskId;

    // All TaskView are bound to the Host Activity if it exists.
    @ShellMainThread
    private final List<ControlledCarTaskView> mControlledTaskViews = new ArrayList<>();
    private final TaskStackListener mTaskStackListener = new TaskStackListener() {
        @Override
        public void onTaskFocusChanged(int taskId, boolean focused) {
            boolean hostFocused = taskId == mHostTaskId && focused;
            Timber.d("onTaskFocusChanged: taskId=" + taskId + ", hostFocused=" + hostFocused);
            if (!hostFocused) {
                return;
            }

            for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                ControlledCarTaskView taskView = mControlledTaskViews.get(i);
                if (taskView.getTaskId() == INVALID_TASK_ID) {
                    // If the task in TaskView is crashed when host is in background,
                    // We'd like to restart it when host becomes foreground and focused.
                    Timber.d("onTaskFocusChanged: Restarting task in TaskView");
                    taskView.startActivity();
                }
            }
        }

        @Override
        public void onActivityRestartAttempt(ActivityManager.RunningTaskInfo task,
                                             boolean homeTaskVisible, boolean clearedTask, boolean wasVisible) {
            Timber.d("onActivityRestartAttempt: taskId=" + task.taskId + ", homeTaskVisible=" + homeTaskVisible + ", wasVisible=" + wasVisible);
            if (homeTaskVisible && mHostTaskId == task.taskId) {
                for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                    ControlledCarTaskView taskView = mControlledTaskViews.get(i);
                    // In the case of CarLauncher, this code handles the case where Home Intent is
                    // sent when CarLauncher is foreground and the task in a ControlledTaskView is
                    // crashed.
                    if (taskView.getTaskId() == INVALID_TASK_ID) {
                        taskView.startActivity();
                    }
                }
            }
        }
    };
    private CarUserManager mCarUserManager;
    private Activity hostActivity;
    private final BroadcastReceiver mPackageBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("onReceive: intent=%s", intent);

            if (isActivityStopped(hostActivity)) {
                return;
            }

            String packageName = intent.getData().getSchemeSpecificPart();
            for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                ControlledCarTaskView taskView = mControlledTaskViews.get(i);
                if (taskView.getTaskId() == INVALID_TASK_ID
                        && taskView.getDependingPackageNames().contains(packageName)) {
                    taskView.startActivity();
                }
            }
        }
    };
    private final CarUserManager.UserLifecycleListener mUserLifecycleListener = event -> {
        Timber.d("UserLifecycleListener.onEvent: For User "
                + getUserId(hostActivity)
                + ", received an event " + event);

        // When user-unlocked, if task isn't launched yet, then try to start it.
        if (event.getEventType() == USER_LIFECYCLE_EVENT_TYPE_UNLOCKED
                && getUserId(hostActivity) == event.getUserId()) {
            for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                ControlledCarTaskView taskView = mControlledTaskViews.get(i);
                if (taskView.getTaskId() == INVALID_TASK_ID) {
                    taskView.startActivity();
                }
            }
        }

        // When user-switching, onDestroy in the previous user's Host app isn't called.
        // So try to release the resource explicitly.
        if (event.getEventType() == USER_LIFECYCLE_EVENT_TYPE_SWITCHING
                && getUserId(hostActivity) == event.getPreviousUserId()) {
            release();
        }
    };

    public TaskViewManager(Activity context, HandlerExecutor handlerExecutor) {
        this(context, handlerExecutor, new ShellTaskOrganizer(handlerExecutor),
                new SyncTransactionQueue(new TransactionPool(), handlerExecutor));
    }

    @VisibleForTesting
    TaskViewManager(Activity context, HandlerExecutor handlerExecutor,
                    ShellTaskOrganizer shellTaskOrganizer, SyncTransactionQueue syncQueue) {
        Timber.d("TaskViewManager(): %s", context);
        hostActivity = context;
        mShellExecutor = handlerExecutor;
        mTaskOrganizer = shellTaskOrganizer;
        mHostTaskId = hostActivity.getTaskId();
        mSyncQueue = syncQueue;

        initCar();
        initTaskOrganizer(mCarActivityManagerRef);
        hostActivity.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    private static boolean isActivityStopped(Activity activity) {
        // This code relies on Activity#isVisibleForAutofill() instead of maintaining a custom
        // activity state.
        return !isVisibleForAutofill(activity);
    }

    private static void cleanUpExistingTaskViewTasks(List<TaskAppearedInfo> taskAppearedInfos) {
        ActivityTaskManager atm = ActivityTaskManager.getInstance();
        for (TaskAppearedInfo taskAppearedInfo : taskAppearedInfos) {
            TaskInfo taskInfo = taskAppearedInfo.getTaskInfo();
            Timber.d("cleanUpExistingTaskViewTasks: taskInfo=%s", taskInfo.topActivity + " " + taskInfo.taskId);
            // Only TaskView tasks have WINDOWING_MODE_MULTI_WINDOW.
            if (getWindowingMode(taskInfo) == WINDOWING_MODE_MULTI_WINDOW) {
                Timber.d("Found the dangling task, removing: %s", taskInfo.taskId);
                atm.removeTask(taskInfo.taskId);
            }
        }
    }

    // TODO(b/239958124A): Remove this method when unit tests for TaskViewManager have been added.

    private void initCar() {
        Car.createCar(/* context= */ hostActivity, /* handler= */ null,
                Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
                (car, ready) -> {
                    if (!ready) {
                        Timber.w("CarService looks crashed");
                        mCarActivityManagerRef.set(null);
                        return;
                    }
                    setCarUserManager((CarUserManager) car.getCarManager("car_user_service"));
                    UserLifecycleEventFilter filter = new UserLifecycleEventFilter.Builder()
                            .addEventType(USER_LIFECYCLE_EVENT_TYPE_UNLOCKED)
                            .addEventType(USER_LIFECYCLE_EVENT_TYPE_SWITCHING).build();
                    mCarUserManager.addListener(hostActivity.getMainExecutor(), filter,
                            mUserLifecycleListener);
                    CarActivityManager carAM = (CarActivityManager) car.getCarManager(
                            "car_activity_service");
                    mCarActivityManagerRef.set(carAM);

                    carAM.registerTaskMonitor();
                });

        TaskStackChangeListeners.getInstance().registerTaskStackListener(mTaskStackListener);

        IntentFilter packageIntentFilter = new IntentFilter(Intent.ACTION_PACKAGE_REPLACED);
        packageIntentFilter.addDataScheme(SCHEME_PACKAGE);
        hostActivity.registerReceiver(mPackageBroadcastReceiver, packageIntentFilter);
    }

    /**
     * This method only exists for the container activity to set mock car user manager in tests.
     */
    void setCarUserManager(CarUserManager carUserManager) {
        mCarUserManager = carUserManager;
    }

    private void initTaskOrganizer(AtomicReference<CarActivityManager> carActivityManagerRef) {
        FullscreenTaskListener fullscreenTaskListener = new CarFullscreenTaskMonitorListener(
                carActivityManagerRef, mSyncQueue);
        mTaskOrganizer.addListenerForType(fullscreenTaskListener, -2);
        ShellInit shellInit = new ShellInit(mShellExecutor);
        // StartingWindowController needs to be initialized so that splash screen is displayed.
        new StartingWindowController(hostActivity, shellInit, mTaskOrganizer, mShellExecutor,
                new PhoneStartingWindowTypeAlgorithm(), null,
                new TransactionPool());
        shellInit.init();
        List<TaskAppearedInfo> taskAppearedInfos = mTaskOrganizer.registerOrganizer();
        cleanUpExistingTaskViewTasks(taskAppearedInfos);
    }

    /**
     * Creates a {@link ControlledCarTaskView}.
     *
     * @param callbackExecutor  the executor which the {@link ControlledCarTaskViewCallbacks} will
     *                          be executed on.
     * @param activityIntent    the intent of the activity that is meant to be started in this
     *                          {@link ControlledCarTaskView}.
     * @param taskViewCallbacks the callbacks for the underlying TaskView.
     */
    public void createControlledCarTaskView(
            Executor callbackExecutor,
            Intent activityIntent,
            boolean autoRestartOnCrash,
            ControlledCarTaskViewCallbacks taskViewCallbacks) {
        mShellExecutor.execute(() -> {
            ControlledCarTaskView taskView = new ControlledCarTaskView(hostActivity, mTaskOrganizer,
                    mSyncQueue, callbackExecutor, activityIntent, autoRestartOnCrash,
                    taskViewCallbacks, hostActivity.getSystemService(UserManager.class));
            mControlledTaskViews.add(taskView);
        });
    }

    /**
     * Releases {@link TaskViewManager} and unregisters the underlying {@link ShellTaskOrganizer}.
     * It also removes all TaskViews which are created by this {@link TaskViewManager}.
     */
    public void release() {
        mShellExecutor.execute(() -> {
            Timber.d("TaskViewManager.release");

            if (mCarUserManager != null) {
                mCarUserManager.removeListener(mUserLifecycleListener);
            }
            TaskStackChangeListeners.getInstance().unregisterTaskStackListener(mTaskStackListener);
            hostActivity.unregisterReceiver(mPackageBroadcastReceiver);

            CarActivityManager carAM = mCarActivityManagerRef.get();
            if (carAM != null) {
                carAM.unregisterTaskMonitor();
                mCarActivityManagerRef.set(null);
            }

            for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                mControlledTaskViews.get(i).release();
            }
            mControlledTaskViews.clear();

            hostActivity.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
            mTaskOrganizer.unregisterOrganizer();
        });
    }

    private final ActivityLifecycleCallbacks mActivityLifecycleCallbacks =
            new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity,
                                              @Nullable Bundle savedInstanceState) {
                    Timber.d("Host activity created");
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                    Timber.d("Host activity started");
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    Timber.d("Host activity resumed");
                    if (activity != hostActivity) {
                        return;
                    }
                    mShellExecutor.execute(() -> {
                        for (int i = mControlledTaskViews.size() - 1; i >= 0; --i) {
                            mControlledTaskViews.get(i).showEmbeddedTask();
                        }
                    });
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                    release();
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
                }
            };


}
