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
package com.hyundai.taskview

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.content.Context
import android.graphics.Rect
import android.util.SparseArray
import android.view.SurfaceControl
import android.window.WindowContainerToken
import android.window.WindowContainerTransaction
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.TaskView
import com.android.wm.shell.common.SyncTransactionQueue
import com.hyundai.taskview.extension.getToken
import timber.log.Timber

/**
 * CarLauncher version of [TaskView] which solves some CarLauncher specific issues:
 *
 *  * b/228092608: Clears the hidden flag to make it TopFocusedRootTask.
 *  * b/225388469: Moves the embedded task to the top to make it resumed.
 *
 */
open class CarTaskView(
    context: Context?, organizer: ShellTaskOrganizer?,
    private val mSyncQueue: SyncTransactionQueue
) : TaskView(context, organizer,  /* taskViewTransitions= */null, mSyncQueue) {
    private var mTaskToken: WindowContainerToken? = null
    private val mInsets: SparseArray<Rect>? = SparseArray()
    private var mTaskViewReadySent = false
    override fun onTaskAppeared(taskInfo: ActivityManager.RunningTaskInfo, leash: SurfaceControl) {
        mTaskToken = taskInfo.getToken()
        super.onTaskAppeared(taskInfo, leash)
        Timber.i("onTaskAppeared: taskInfo=%s", taskInfo)
        applyInsets()
    }

    override fun onTaskVanished(taskInfo: ActivityManager.RunningTaskInfo) {
        super.onTaskVanished(taskInfo)
        Timber.i("onTaskVanished: taskInfo=%s", taskInfo)
    }

    public override fun notifyInitialized() {
        super.notifyInitialized()
        if (mTaskViewReadySent) {
            Timber.i("car task view ready already sent")
            showEmbeddedTask()
            return
        }
        onCarTaskViewInitialized()
        mTaskViewReadySent = true
    }

    /**
     * Called only once when the [CarTaskView] is ready.
     */
    protected open fun onCarTaskViewInitialized() {

    }

    /**
     * Moves the embedded task over the embedding task to make it shown.
     */
    fun showEmbeddedTask() {
        if (mTaskToken == null) {
            return
        }
        val wct = WindowContainerTransaction()
        // Clears the hidden flag to make it TopFocusedRootTask: b/228092608
        wct.setHidden(mTaskToken,  /* hidden= */false)
        // Moves the embedded task to the top to make it resumed: b/225388469
        wct.reorder(mTaskToken,  /* onTop= */true)
        mSyncQueue.queue(wct)
    }
    // TODO(b/238473897): Consider taking insets one by one instead of taking all insets.
    /**
     * Set & apply the given `insets` on the Task.
     */
    fun setInsets(insets: SparseArray<Rect>) {
        mInsets!!.clear()
        for (i in insets.size() - 1 downTo 0) {
            mInsets.append(insets.keyAt(i), insets.valueAt(i))
        }
        applyInsets()
    }

    private fun applyInsets() {
        if (mInsets == null || mInsets.size() == 0) {
            Timber.w("Cannot apply null or empty insets")
            return
        }
        if (mTaskToken == null) {
            Timber.w("Cannot apply insets as the task token is not present.")
            return
        }
        val wct = WindowContainerTransaction()
        for (i in 0 until mInsets.size()) {
            wct.addRectInsetsProvider(mTaskToken, mInsets.valueAt(i), intArrayOf(mInsets.keyAt(i)))
        }
        mSyncQueue.queue(wct)
    }

    val taskId: Int
        /**
         * @return the taskId of the currently running task.
         */
        get() = if (mTaskInfo == null) {
            ActivityTaskManager.INVALID_TASK_ID
        } else mTaskInfo.taskId
}
