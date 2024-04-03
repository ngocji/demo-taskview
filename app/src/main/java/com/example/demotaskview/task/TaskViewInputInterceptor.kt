/**
 * @file TaskViewInputInterceptor.kt
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

import android.annotation.MainThread
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.android.wm.shell.taskview.TaskView
import timber.log.Timber

/**
 * This class is responsible to intercept the swipe gestures & long press over [ ].
 *
 *
 *  * The gesture interception will only occur when the corresponding [       ][ControlledCarTaskViewConfig.mCaptureGestures] is set.
 *  * The long press interception will only occur when the corresponding [       ][ControlledCarTaskViewConfig.mCaptureLongPress] is set.
 *
 */
class TaskViewInputInterceptor internal constructor(
    private val mHostActivity: Activity,
    private val mTaskViewManager: TaskViewManager
) {
    private val mInputManager: InputManager
    private val mWm: WindowManager
    private val mGestureDetector: GestureDetector
    private val mActivityLifecycleCallbacks: Application.ActivityLifecycleCallbacks =
        ActivityLifecycleHandler()
    private var mSpyWindow: View? = null
    private var mInitialized = false

    init {
        mGestureDetector = GestureDetector(mHostActivity, TaskViewGestureListener())
        mInputManager = mHostActivity.getSystemService(InputManager::class.java)
        mWm = mHostActivity.getSystemService(WindowManager::class.java)
    }

    /**
     * Initializes & starts intercepting gestures. Does nothing if already initialized.
     */
    @MainThread
    fun init() {
        if (mInitialized) {
            Timber.e(TAG, "Already initialized")
            return
        }
        mInitialized = true
        mHostActivity.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
        startInterceptingGestures()
    }

    /**
     * Releases the held resources and stops intercepting gestures. Does nothing if already
     * released.
     */
    @MainThread
    fun release() {
        if (!mInitialized) {
            Timber.e(TAG, "Failed to release as it is not initialized")
            return
        }
        mInitialized = false
        mHostActivity.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
        stopInterceptingGestures()
    }

    private fun startInterceptingGestures() {
        Timber.d(TAG, "Start intercepting gestures")
        if (mSpyWindow != null) {
            Timber.d(TAG, "Already intercepting gestures")
            return
        }
        createAndAddSpyWindow()
    }

    private fun stopInterceptingGestures() {
        Timber.d(TAG, "Stop intercepting gestures")
        if (mSpyWindow == null) {
            Timber.d(TAG, "Already not intercepting gestures")
            return
        }
        removeSpyWindow()
    }

    private fun createAndAddSpyWindow() {
        mSpyWindow = GestureSpyView(mHostActivity)
        val p = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,  // LAYOUT_IN_SCREEN required so that event coordinate system matches the
            // taskview.getBoundsOnScreen coordinate system
            PixelFormat.TRANSLUCENT
        )
        p.inputFeatures = INPUT_FEATURE_SPY
        p.privateFlags = p.privateFlags or WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY
        mWm.addView(mSpyWindow, p)
    }

    private fun removeSpyWindow() {
        if (mSpyWindow == null) {
            Timber.e(TAG, "Spy window is not present")
            return
        }
        mWm.removeView(mSpyWindow)
        mSpyWindow = null
    }

    private inner class GestureSpyView constructor(context: Context?) : View(context) {
        private var mConsumingCurrentEventStream = false
        private var mActionDownInsideTaskView = false
        private var mTouchDownX = 0f
        private var mTouchDownY = 0f
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            var justToggled = false
            mGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                mActionDownInsideTaskView = false
                val taskViewList = mTaskViewManager.controlledTaskViews
                for (tv in taskViewList) {
                    if (tv.getConfig().mCaptureGestures && isIn(event, tv)) {
                        mTouchDownX = event.x
                        mTouchDownY = event.y
                        mActionDownInsideTaskView = true
                        break
                    }
                }

                // Stop consuming immediately on ACTION_DOWN
                mConsumingCurrentEventStream = false
            }
            if (event.action == MotionEvent.ACTION_MOVE) {
                if (!mConsumingCurrentEventStream && mActionDownInsideTaskView && java.lang.Float.compare(
                        mTouchDownX,
                        event.x
                    ) != 0 && java.lang.Float.compare(mTouchDownY, event.y) != 0
                ) {
                    // Start consuming on ACTION_MOVE when ACTION_DOWN happened inside TaskView
                    mConsumingCurrentEventStream = true
                    justToggled = true
                }

                // Handling the events
                if (mConsumingCurrentEventStream) {
                    // Disable the propagation when consuming events.
                    mInputManager.pilferPointers(getViewRootImpl().getInputToken())
                    if (justToggled) {
                        // When just toggled from DOWN to MOVE, dispatch a DOWN event as DOWN event
                        // is meant to be the first event in an event stream.
                        val cloneEvent = MotionEvent.obtain(event)
                        cloneEvent.action = MotionEvent.ACTION_DOWN
                        mHostActivity.dispatchTouchEvent(cloneEvent)
                        cloneEvent.recycle()
                    }
                    mHostActivity.dispatchTouchEvent(event)
                }
            }
            if (event.action == MotionEvent.ACTION_UP) {
                // Handling the events
                if (mConsumingCurrentEventStream) {
                    // Disable the propagation when handling manually.
                    mInputManager.pilferPointers(getViewRootImpl().getInputToken())
                    mHostActivity.dispatchTouchEvent(event)
                }
                mConsumingCurrentEventStream = false
            }
            return false
        }
    }

    private inner class TaskViewGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            val taskViewList = mTaskViewManager.controlledTaskViews
            for (tv in taskViewList) {
                if (tv.getConfig().mCaptureLongPress && isIn(e, tv)) {
                    Timber.d(TAG, "Long press captured for taskView: $tv")
                    mInputManager.pilferPointers(mSpyWindow?.getViewRootImpl()?.getInputToken())
                    if (tv.getOnLongClickListener() != null) {
                        tv.getOnLongClickListener().onLongClick(tv)
                    }
                    return
                }
            }
            Timber.d(TAG, "Long press not captured")
        }
    }

    private inner class ActivityLifecycleHandler : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(
            activity: Activity, savedInstanceState: Bundle?
        ) {
        }

        override fun onActivityStarted(activity: Activity) {
            if (!mInitialized) {
                return
            }
            startInterceptingGestures()
        }

        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {
            if (!mInitialized) {
                return
            }
            stopInterceptingGestures()
        }

        override fun onActivitySaveInstanceState(
            activity: Activity, outState: Bundle
        ) {
        }

        override fun onActivityDestroyed(activity: Activity) {}
    }

    companion object {
        const val INPUT_FEATURE_SPY = 1 shl 2
        private val TAG = TaskViewInputInterceptor::class.java.simpleName
        private val sTmpBounds = Rect()
        private fun isIn(event: MotionEvent, taskView: TaskView): Boolean {
            taskView.getBoundsOnScreen(sTmpBounds)
            return sTmpBounds.contains(event.x.toInt(), event.y.toInt())
        }
    }
}
