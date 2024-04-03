/**
 * @file CarTaskView.kt
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
import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Binder
import android.util.SparseArray
import android.view.InsetsSource
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.window.WindowContainerToken
import android.window.WindowContainerTransaction
import androidx.core.util.isEmpty
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.common.SyncTransactionQueue
import com.android.wm.shell.taskview.TaskView
import com.android.wm.shell.taskview.TaskViewTaskController
import com.android.wm.shell.taskview.TaskViewTransitions
import timber.log.Timber

/**
 * CarLauncher version of {@link TaskView} which solves some CarLauncher specific issues:
 * <ul>
 * <li>b/228092608: Clears the hidden flag to make it TopFocusedRootTask.</li>
 * <li>b/225388469: Moves the embedded task to the top to make it resumed.</li>
 * </ul>
 * @noinspection ALL, BlockingMethodInNonBlockingContext
 */
@SuppressLint("ViewConstructor")
@Suppress("unused")
open class CarTaskView(
    context: Context,
    organizer: ShellTaskOrganizer,
    taskViewTransitions: TaskViewTransitions?,
    syncQueue: SyncTransactionQueue,
    shouldHideTask: Boolean,
    taskViewTaskController: TaskViewTaskController = TaskViewTaskController(
        context,
        organizer,
        taskViewTransitions,
        syncQueue
    )
) : TaskView(context, taskViewTaskController) {
    private var mTaskToken: WindowContainerToken? = null
    private var mSyncQueue: SyncTransactionQueue? = null
    private var mInsetsOwner = Binder()
    private var mInsets = SparseArray<Rect>()
    private var mTaskViewReadySent = false
    private var mTaskViewTaskController: TaskViewTaskController? = null
    private var path: Path? = null
    private var cornerRadius = 100f

    init {
        mTaskViewTaskController = taskViewTaskController
        mTaskViewTaskController?.setHideTaskWithSurface(shouldHideTask)
        mSyncQueue = syncQueue

        setZOrderOnTop(true)
        getHolder().setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun onTaskAppeared(
        taskInfo: ActivityManager.RunningTaskInfo?,
        leash: SurfaceControl?
    ) {
        mTaskToken = taskInfo?.getToken()
        super.onTaskAppeared(taskInfo, leash)
        applyAllInsets()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.path = Path()
        this.path?.addRoundRect(
            RectF(0f, 0f, w.toFloat(), h.toFloat()),
            cornerRadius,
            cornerRadius,
            Path.Direction.CW
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        clipRound(canvas)
        super.dispatchDraw(canvas)
    }

    override fun draw(canvas: Canvas) {

        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

    }

    /**
     * Triggers the change in the WM bounds as per the `newBounds` received.
     *
     *
     * Should be called when the surface has changed. Can also be called before an animation if
     * the final bounds are already known.
     */
    fun setWindowBounds(newBounds: Rect) {
        mTaskViewTaskController?.setWindowBounds(newBounds)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        if (mTaskViewReadySent) {
            Timber.i("$TAG car task view ready already sent");
            return
        }
        onCarTaskViewInitialized()
        mTaskViewReadySent = true
    }

    /**
     * Called only once when the [CarTaskView] is ready.
     */
    open fun onCarTaskViewInitialized() {}

    /**
     * Moves the embedded task over the embedding task to make it shown.
     */
    open fun showEmbeddedTask(wct: WindowContainerTransaction) {
        if (mTaskToken == null) {
            return
        }
        // Clears the hidden flag to make it TopFocusedRootTask: b/228092608
        wct.setHidden(mTaskToken,  /* hidden= */false)
        // Moves the embedded task to the top to make it resumed: b/225388469
        wct.reorder(mTaskToken,  /* onTop= */true)
    }

    // TODO(b/238473897): Consider taking insets one by one instead of taking all insets.
    /**
     * Adds & applies the given insets on the Task.
     *
     *
     *
     * The insets that were specified in an earlier call but not specified later, will remain
     * applied to the task. Clients should explicitly call
     * [.removeInsets] to remove the insets from the underlying task.
     *
     *
     * @param index The caller might add multiple insets sources with the same type.
     * This identifies them.
     * @param type  The insets type of the insets source.
     * @param frame The rectangle area of the insets source.
     */
    fun addInsets(index: Int, type: Int, frame: Rect) {
        mInsets.append(InsetsSource.createId(mInsetsOwner, index, type), frame)
        if (mTaskToken == null) {
            // The insets will be applied later as part of onTaskAppeared.
//            Timber.w(TAG, "Cannot apply insets as the task token is not present.");
            return
        }
        val wct = WindowContainerTransaction()
        wct.addInsetsSource(mTaskToken, mInsetsOwner, index, type, frame)
        mSyncQueue?.queue(wct)
    }

    /**
     * Removes the given insets from the Task.
     *
     * @param index The caller might add multiple insets sources with the same type.
     * This identifies them.
     * @param type  The insets type of the insets source.
     */
    fun removeInsets(index: Int, type: Int) {
        if (mInsets.size() == 0) {
            Timber.w("$TAG No insets set.")
            return
        }
        val id = InsetsSource.createId(mInsetsOwner, index, type)
        if (!mInsets.contains(id)) {
            Timber.w(
                "$TAG Insets type: " + type + " can't be removed as it was not "
                        + "applied as part of the last addInsets()"
            )
            return
        }
        mInsets.remove(id)
        if (mTaskToken == null) {
            Timber.w("$TAG Cannot remove insets as the task token is not present.")
            return
        }
        val wct = WindowContainerTransaction()
        wct.removeInsetsSource(mTaskToken, mInsetsOwner, index, type)
        mSyncQueue?.queue(wct)
    }

    private fun applyAllInsets() {
        if (mInsets.isEmpty()) {
            Timber.w("$TAG Cannot apply null or empty insets")
            return
        }
        if (mTaskToken == null) {
            Timber.w("$TAG Cannot apply insets as the task token is not present.")
            return
        }
        val wct = WindowContainerTransaction()
        for (i in 0 until mInsets.size()) {
            val id = mInsets.keyAt(i)
            val frame = mInsets.valueAt(i)
            wct.addInsetsSource(
                mTaskToken,
                mInsetsOwner,
                InsetsSource.getIndex(id),
                InsetsSource.getType(id),
                frame
            )
        }
        mSyncQueue?.queue(wct)
    }

    /**
     * @return the taskId of the currently running task.
     */
    val taskId
        get() = mTaskViewTaskController?.taskInfo?.taskId ?: ActivityTaskManager.INVALID_TASK_ID


    private fun clipRound(canvas: Canvas) {
        path?.run {
            Timber.e("$TAG start cliping: $this")
            canvas.clipPath(this)
        }
    }


    companion object {
        private val TAG = CarTaskView::class.java.simpleName
    }
}
