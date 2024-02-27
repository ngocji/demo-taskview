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

package com.example.demotaskview.task;

import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import android.annotation.MainThread;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.wm.shell.taskview.TaskView;

import java.util.List;

import timber.log.Timber;

/**
 * This class is responsible to intercept the swipe gestures & long press over {@link
 * ControlledCarTaskView}.
 *
 * <ul>
 *   <li>The gesture interception will only occur when the corresponding {@link
 *       ControlledCarTaskViewConfig#mCaptureGestures} is set.
 *   <li>The long press interception will only occur when the corresponding {@link
 *       ControlledCarTaskViewConfig#mCaptureLongPress} is set.
 * </ul>
 */
public final class TaskViewInputInterceptor {
    public static final int INPUT_FEATURE_SPY = 1 << 2;

    private static final String TAG = TaskViewInputInterceptor.class.getSimpleName();
    private static final Rect sTmpBounds = new Rect();

    private final Activity mHostActivity;
    private final InputManager mInputManager;
    private final TaskViewManager mTaskViewManager;
    private final WindowManager mWm;
    private GestureDetector mGestureDetector;
    private final Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks =
        new ActivityLifecycleHandler();

    private View mSpyWindow;
    private boolean mInitialized = false;

    TaskViewInputInterceptor(Activity hostActivity, TaskViewManager taskViewManager) {
        mGestureDetector = new GestureDetector(hostActivity, new TaskViewGestureListener());
        mHostActivity = hostActivity;
        mInputManager = hostActivity.getSystemService(InputManager.class);
        mTaskViewManager = taskViewManager;
        mWm = mHostActivity.getSystemService(WindowManager.class);
    }

    private static boolean isIn(MotionEvent event, TaskView taskView) {
        taskView.getBoundsOnScreen(sTmpBounds);
        return sTmpBounds.contains((int) event.getX(), (int) event.getY());
    }

    /**
     * Initializes & starts intercepting gestures. Does nothing if already initialized.
     */
    @MainThread
    void init() {
        if (mInitialized) {
            Timber.e(TAG, "Already initialized");
            return;
        }
        mInitialized = true;
        mHostActivity.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        startInterceptingGestures();
    }

    /**
     * Releases the held resources and stops intercepting gestures. Does nothing if already
     * released.
     */
    @MainThread
    void release() {
        if (!mInitialized) {
            Log.e(TAG, "Failed to release as it is not initialized");
            return;
        }
        mInitialized = false;
        mHostActivity.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        stopInterceptingGestures();
    }

    private void startInterceptingGestures() {
        if (TaskViewManager.DBG) {
            Timber.d(TAG, "Start intercepting gestures");
        }
        if (mSpyWindow != null) {
            Timber.d(TAG, "Already intercepting gestures");
            return;
        }
        createAndAddSpyWindow();
    }

    private void stopInterceptingGestures() {
        if (TaskViewManager.DBG) {
            Timber.d(TAG, "Stop intercepting gestures");
        }
        if (mSpyWindow == null) {
            Timber.d(TAG, "Already not intercepting gestures");
            return;
        }
        removeSpyWindow();
    }

    private void createAndAddSpyWindow() {
        mSpyWindow = new GestureSpyView(mHostActivity);
        WindowManager.LayoutParams p =
            new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | FLAG_NOT_FOCUSABLE
                    | FLAG_LAYOUT_IN_SCREEN,
                // LAYOUT_IN_SCREEN required so that event coordinate system matches the
                // taskview.getBoundsOnScreen coordinate system
                PixelFormat.TRANSLUCENT);
        p.inputFeatures = INPUT_FEATURE_SPY;
        p.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY;
        mWm.addView(mSpyWindow, p);
    }

    private void removeSpyWindow() {
        if (mSpyWindow == null) {
            Timber.e(TAG, "Spy window is not present");
            return;
        }
        mWm.removeView(mSpyWindow);
        mSpyWindow = null;
    }

    private final class GestureSpyView extends View {
        private boolean mConsumingCurrentEventStream = false;
        private boolean mActionDownInsideTaskView = false;
        private float mTouchDownX;
        private float mTouchDownY;

        GestureSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            boolean justToggled = false;
            mGestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mActionDownInsideTaskView = false;

                List<ControlledCarTaskView> taskViewList =
                    mTaskViewManager.getControlledTaskViews();
                for (ControlledCarTaskView tv : taskViewList) {
                    if (tv.getConfig().mCaptureGestures && isIn(event, tv)) {
                        mTouchDownX = event.getX();
                        mTouchDownY = event.getY();
                        mActionDownInsideTaskView = true;
                        break;
                    }
                }

                // Stop consuming immediately on ACTION_DOWN
                mConsumingCurrentEventStream = false;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (!mConsumingCurrentEventStream && mActionDownInsideTaskView
                    && Float.compare(mTouchDownX, event.getX()) != 0
                    && Float.compare(mTouchDownY, event.getY()) != 0) {
                    // Start consuming on ACTION_MOVE when ACTION_DOWN happened inside TaskView
                    mConsumingCurrentEventStream = true;
                    justToggled = true;
                }

                // Handling the events
                if (mConsumingCurrentEventStream) {
                    // Disable the propagation when consuming events.
                    mInputManager.pilferPointers(getViewRootImpl().getInputToken());

                    if (justToggled) {
                        // When just toggled from DOWN to MOVE, dispatch a DOWN event as DOWN event
                        // is meant to be the first event in an event stream.
                        MotionEvent cloneEvent = MotionEvent.obtain(event);
                        cloneEvent.setAction(MotionEvent.ACTION_DOWN);
                        TaskViewInputInterceptor.this.mHostActivity.dispatchTouchEvent(cloneEvent);
                        cloneEvent.recycle();
                    }
                    TaskViewInputInterceptor.this.mHostActivity.dispatchTouchEvent(event);
                }
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Handling the events
                if (mConsumingCurrentEventStream) {
                    // Disable the propagation when handling manually.
                    mInputManager.pilferPointers(getViewRootImpl().getInputToken());
                    TaskViewInputInterceptor.this.mHostActivity.dispatchTouchEvent(event);
                }
                mConsumingCurrentEventStream = false;
            }
            return false;
        }
    }

    private final class TaskViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            List<ControlledCarTaskView> taskViewList = mTaskViewManager.getControlledTaskViews();
            for (ControlledCarTaskView tv : taskViewList) {
                if (tv.getConfig().mCaptureLongPress && isIn(e, tv)) {
                    if (TaskViewManager.DBG) {
                        Timber.d(TAG, "Long press captured for taskView: " + tv);
                    }
                    mInputManager.pilferPointers(mSpyWindow.getViewRootImpl().getInputToken());
                    if (tv.getOnLongClickListener() != null) {
                        tv.getOnLongClickListener().onLongClick(tv);
                    }
                    return;
                }
            }
            if (TaskViewManager.DBG) {
                Timber.d(TAG, "Long press not captured");
            }
        }
    }

    private final class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (!mInitialized) {
                return;
            }
            startInterceptingGestures();
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            if (!mInitialized) {
                return;
            }
            stopInterceptingGestures();
        }

        @Override
        public void onActivitySaveInstanceState(
            @NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    }
}
