/**
 * @file TaskStackChangeListeners.kt
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
import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import com.android.wm.shell.common.HandlerExecutor
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * Organizer of many task stack listeners for the car launcher application.
 */
@Suppress("unused")
class TaskStackChangeListeners private constructor(executor: Executor) {
    private val mImpl: Impl by lazy { Impl(executor) }

    /**
     * Registers a task stack listener with the system.
     * This should be called on the main thread.
     */
    fun registerTaskStackListener(listener: TaskStackListener) {
        synchronized(mImpl) { mImpl.addListener(listener) }
        if (DEBUG) {
            Timber.d("$TAG registerTaskStackListener: $listener")
        }
    }

    /**
     * Unregisters a task stack listener with the system.
     * This should be called on the main thread.
     */
    fun unregisterTaskStackListener(listener: TaskStackListener) {
        synchronized(mImpl) { mImpl.removeListener(listener) }
        if (DEBUG) {
            Timber.d("$TAG unregisterTaskStackListener: $listener")
        }
    }

    private class Impl(private val mExecutor: Executor) : TaskStackListener() {
        private val mTaskStackListeners: MutableList<TaskStackListener> = ArrayList()
        private var mRegistered = false
        fun addListener(listener: TaskStackListener) {
            synchronized(mTaskStackListeners) { mTaskStackListeners.add(listener) }
            if (!mRegistered) {
                // Register mTaskStackListener to IActivityManager only once if needed.
                try {
                    ActivityTaskManager.getService().registerTaskStackListener(this)
                    mRegistered = true
                } catch (e: Exception) {
                    Timber.w("$TAG Failed to call registerTaskStackListener", e)
                }
            }
        }

        fun removeListener(listener: TaskStackListener) {
            var isEmpty: Boolean
            synchronized(mTaskStackListeners) {
                mTaskStackListeners.remove(listener)
                isEmpty = mTaskStackListeners.isEmpty()
            }
            if (isEmpty && mRegistered) {
                // Unregister mTaskStackListener once we have no more listeners
                try {
                    ActivityTaskManager.getService().unregisterTaskStackListener(this)
                    mRegistered = false
                } catch (e: Exception) {
                    Timber.w("$TAG Failed to call unregisterTaskStackListener", e)
                }
            }
        }

        @Throws(RemoteException::class)
        override fun onTaskStackChanged() {
            mExecutor.execute {
                synchronized(mTaskStackListeners) {
                    for (i in mTaskStackListeners.indices) {
                        try {
                            mTaskStackListeners[i].onTaskStackChanged()
                        } catch (e: RemoteException) {
                            Timber.e("$TAG onTaskStackChanged failed", e)
                        }
                    }
                }
            }
        }

        override fun onTaskCreated(taskId: Int, componentName: ComponentName?) {
            mExecutor.execute {
                synchronized(mTaskStackListeners) {
                    for (i in mTaskStackListeners.indices) {
                        try {
                            mTaskStackListeners[i].onTaskCreated(taskId, componentName)
                        } catch (e: RemoteException) {
                            Timber.e("$TAG onTaskCreated failed", e)
                        }
                    }
                }
            }
        }

        @Throws(RemoteException::class)
        override fun onTaskRemoved(taskId: Int) {
            mExecutor.execute {
                synchronized(mTaskStackListeners) {
                    for (i in mTaskStackListeners.indices) {
                        try {
                            mTaskStackListeners[i].onTaskRemoved(taskId)
                        } catch (e: RemoteException) {
                            Timber.e("$TAG onTaskRemoved failed", e)
                        }
                    }
                }
            }
        }

        @Throws(RemoteException::class)
        override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo) {
            mExecutor.execute {
                synchronized(mTaskStackListeners) {
                    for (i in mTaskStackListeners.indices) {
                        try {
                            mTaskStackListeners[i].onTaskMovedToFront(taskInfo)
                        } catch (e: RemoteException) {
                            Timber.e("$TAG onTaskMovedToFront failed", e)
                        }
                    }
                }
            }
        }

        override fun onTaskFocusChanged(taskId: Int, focused: Boolean) {
            mExecutor.execute {
                synchronized(mTaskStackListeners) {
                    for (i in mTaskStackListeners.indices) {
                        mTaskStackListeners[i].onTaskFocusChanged(taskId, focused)
                    }
                }
            }
        }

        override fun onActivityRestartAttempt(
            task: ActivityManager.RunningTaskInfo,
            homeTaskVisible: Boolean, clearedTask: Boolean, wasVisible: Boolean
        ) {
            mExecutor.execute {
                synchronized(mTaskStackListeners) {
                    for (listener in mTaskStackListeners) {
                        try {
                            listener.onActivityRestartAttempt(
                                task, homeTaskVisible, clearedTask, wasVisible
                            )
                        } catch (e: RemoteException) {
                            Timber.e("$TAG onActivityRestartAttempt failed", e)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = TaskStackChangeListeners::class.java.simpleName
        private const val DEBUG = true

        /**
         * Returns a singleton instance of the [TaskStackChangeListeners].
         */
        val instance = TaskStackChangeListeners(
            HandlerExecutor(Handler(Looper.getMainLooper()))
        )
    }
}
