package com.example.demotaskview.task;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;

import static com.example.demotaskview.task.TaskViewManager.DBG;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Binder;
import android.util.SparseArray;
import android.view.InsetsSource;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.window.WindowContainerToken;
import android.window.WindowContainerTransaction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.taskview.TaskView;
import com.android.wm.shell.taskview.TaskViewTaskController;
import com.android.wm.shell.taskview.TaskViewTransitions;

import timber.log.Timber;

/**
 * CarLauncher version of {@link TaskView} which solves some CarLauncher specific issues:
 * <ul>
 * <li>b/228092608: Clears the hidden flag to make it TopFocusedRootTask.</li>
 * <li>b/225388469: Moves the embedded task to the top to make it resumed.</li>
 * </ul>
 */
public class CarTaskView extends TaskView {
    private static final String TAG = CarTaskView.class.getSimpleName();
    @Nullable
    private WindowContainerToken mTaskToken;
    private final SyncTransactionQueue mSyncQueue;
    private final Binder mInsetsOwner = new Binder();
    private final SparseArray<Rect> mInsets = new SparseArray<>();
    private boolean mTaskViewReadySent;
    private TaskViewTaskController mTaskViewTaskController;

    public CarTaskView(Context context, ShellTaskOrganizer organizer,
                       TaskViewTransitions taskViewTransitions, SyncTransactionQueue syncQueue,
                       boolean shouldHideTask) {
        this(context, syncQueue, shouldHideTask,
            new TaskViewTaskController(context, organizer, taskViewTransitions, syncQueue));
    }

    public CarTaskView(Context context, SyncTransactionQueue syncQueue, boolean shouldHideTask,
                       TaskViewTaskController taskViewTaskController) {
        super(context, taskViewTaskController);
        mTaskViewTaskController = taskViewTaskController;
        mTaskViewTaskController.setHideTaskWithSurface(shouldHideTask);
        mSyncQueue = syncQueue;
    }

    /**
     * Calls {@link TaskViewTaskController#onTaskAppeared(ActivityManager.RunningTaskInfo,
     * SurfaceControl)}.
     */
    public void dispatchTaskAppeared(ActivityManager.RunningTaskInfo taskInfo,
                                     SurfaceControl leash) {
        mTaskViewTaskController.onTaskAppeared(taskInfo, leash);
    }

    /**
     * Calls {@link TaskViewTaskController#onTaskVanished(ActivityManager.RunningTaskInfo)}.
     */
    public void dispatchTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        mTaskViewTaskController.onTaskVanished(taskInfo);
    }

    /**
     * Calls {@link TaskViewTaskController#onTaskInfoChanged(ActivityManager.RunningTaskInfo)}.
     */
    public void dispatchTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        mTaskViewTaskController.onTaskInfoChanged(taskInfo);
    }

    @Override
    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
        mTaskToken = taskInfo.getToken();
        super.onTaskAppeared(taskInfo, leash);

        applyAllInsets();
    }

    /**
     * Triggers the change in the WM bounds as per the {@code newBounds} received.
     * <p>
     * Should be called when the surface has changed. Can also be called before an animation if
     * the final bounds are already known.
     */
    public void setWindowBounds(Rect newBounds) {
        mTaskViewTaskController.setWindowBounds(newBounds);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        if (mTaskViewReadySent) {
//            if (DBG) Timber.i(TAG, "car task view ready already sent");
            return;
        }
        onCarTaskViewInitialized();
        mTaskViewReadySent = true;
    }

    /**
     * Called only once when the {@link CarTaskView} is ready.
     */
    protected void onCarTaskViewInitialized() {
    }

    /**
     * Moves the embedded task over the embedding task to make it shown.
     */
    void showEmbeddedTask(WindowContainerTransaction wct) {
        if (mTaskToken == null) {
            return;
        }
        // Clears the hidden flag to make it TopFocusedRootTask: b/228092608
        wct.setHidden(mTaskToken, /* hidden= */ false);
        // Moves the embedded task to the top to make it resumed: b/225388469
        wct.reorder(mTaskToken, /* onTop= */ true);
    }

    // TODO(b/238473897): Consider taking insets one by one instead of taking all insets.

    /**
     * Adds & applies the given insets on the Task.
     *
     * <p>
     * The insets that were specified in an earlier call but not specified later, will remain
     * applied to the task. Clients should explicitly call
     * {@link #removeInsets(int, int)} to remove the insets from the underlying task.
     * </p>
     *
     * @param index The caller might add multiple insets sources with the same type.
     *              This identifies them.
     * @param type  The insets type of the insets source.
     * @param frame The rectangle area of the insets source.
     */
    public void addInsets(int index, int type, @NonNull Rect frame) {
        mInsets.append(InsetsSource.createId(mInsetsOwner, index, type), frame);

        if (mTaskToken == null) {
            // The insets will be applied later as part of onTaskAppeared.
//            Timber.w(TAG, "Cannot apply insets as the task token is not present.");
            return;
        }
        WindowContainerTransaction wct = new WindowContainerTransaction();
        wct.addInsetsSource(mTaskToken, mInsetsOwner, index, type, frame);
        mSyncQueue.queue(wct);
    }

    /**
     * Removes the given insets from the Task.
     *
     * @param index The caller might add multiple insets sources with the same type.
     *              This identifies them.
     * @param type  The insets type of the insets source.
     */
    public void removeInsets(int index, int type) {
        if (mInsets.size() == 0) {
            Timber.w(TAG, "No insets set.");
            return;
        }
        int id = InsetsSource.createId(mInsetsOwner, index, type);
        if (!mInsets.contains(id)) {
            Timber.w(TAG, "Insets type: " + type + " can't be removed as it was not "
                + "applied as part of the last addInsets()");
            return;
        }
        mInsets.remove(id);

        if (mTaskToken == null) {
            Timber.w(TAG, "Cannot remove insets as the task token is not present.");
            return;
        }
        WindowContainerTransaction wct = new WindowContainerTransaction();
        wct.removeInsetsSource(mTaskToken, mInsetsOwner, index, type);
        mSyncQueue.queue(wct);
    }

    private void applyAllInsets() {
        if (mInsets.size() == 0) {
            Timber.w(TAG, "Cannot apply null or empty insets");
            return;
        }
        if (mTaskToken == null) {
            Timber.w(TAG, "Cannot apply insets as the task token is not present.");
            return;
        }
        WindowContainerTransaction wct = new WindowContainerTransaction();
        for (int i = 0; i < mInsets.size(); i++) {
            final int id = mInsets.keyAt(i);
            final Rect frame = mInsets.valueAt(i);
            wct.addInsetsSource(mTaskToken, mInsetsOwner, InsetsSource.getIndex(id),
                InsetsSource.getType(id), frame);
        }
        mSyncQueue.queue(wct);
    }

    /**
     * @return the taskId of the currently running task.
     */
    public int getTaskId() {
        if (mTaskViewTaskController.getTaskInfo() == null) {
            return INVALID_TASK_ID;
        }
        return mTaskViewTaskController.getTaskInfo().taskId;
    }
}
