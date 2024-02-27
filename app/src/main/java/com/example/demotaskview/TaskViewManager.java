//package com.example.demotaskview;
//
//
//import static android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW;
//import static com.android.wm.shell.ShellTaskOrganizer.TASK_LISTENER_TYPE_FULLSCREEN;
//
//import android.app.ActivityTaskManager;
//import android.app.TaskInfo;
//import android.content.Context;
//import android.window.TaskAppearedInfo;
//
//import androidx.annotation.UiContext;
//
//import com.android.launcher3.icons.IconProvider;
//import com.android.wm.shell.ShellTaskOrganizer;
//import com.android.wm.shell.common.HandlerExecutor;
//import com.android.wm.shell.common.SyncTransactionQueue;
//import com.android.wm.shell.common.TransactionPool;
//import com.android.wm.shell.fullscreen.FullscreenTaskListener;
//import com.android.wm.shell.startingsurface.StartingWindowController;
//import com.android.wm.shell.startingsurface.phone.PhoneStartingWindowTypeAlgorithm;
//import com.android.wm.shell.sysui.ShellCommandHandler;
//import com.android.wm.shell.sysui.ShellController;
//import com.android.wm.shell.sysui.ShellInit;
//import com.android.wm.shell.taskview.TaskView;
//import com.android.wm.shell.taskview.TaskViewFactory;
//import com.android.wm.shell.taskview.TaskViewFactoryController;
//
//import java.util.List;
//import java.util.function.Consumer;
//
//public final class TaskViewManager {
//    private static final boolean DBG = false;
//
//    private final Context mContext;
//    private final HandlerExecutor mExecutor;
//    private final TaskViewFactory mTaskViewFactory;
//
//    public TaskViewManager(@UiContext Context context, HandlerExecutor handlerExecutor) {
//        mContext = context;
//        mExecutor = handlerExecutor;
//        mTaskViewFactory = initWmShell();
//    }
//
//    private TaskViewFactory initWmShell() {
//        ShellTaskOrganizer taskOrganizer = new ShellTaskOrganizer(mExecutor);
//        TransactionPool transactionPool = new TransactionPool();
//        FullscreenTaskListener fullscreenTaskListener =
//                new FullscreenTaskListener(new SyncTransactionQueue(transactionPool, mExecutor));
//        taskOrganizer.addListenerForType(fullscreenTaskListener, TASK_LISTENER_TYPE_FULLSCREEN);
//        ShellInit shellinit = new ShellInit(mExecutor);
//        ShellController shellController = new ShellController(mContext, shellinit, new ShellCommandHandler(), mExecutor);
//        StartingWindowController startingController =
//                new StartingWindowController(mContext, shellinit, shellController, taskOrganizer, mExecutor,
//                        new PhoneStartingWindowTypeAlgorithm(), new IconProvider(mContext), transactionPool);
//        taskOrganizer.initStartingWindow(startingController);
//        List<TaskAppearedInfo> taskAppearedInfos = taskOrganizer.registerOrganizer();
//        cleanUpExistingTaskViewTasks(taskAppearedInfos);
//
//        return new TaskViewFactoryController(taskOrganizer, mExecutor, new SyncTransactionQueue(transactionPool, mExecutor)).asTaskViewFactory();
//    }
//
//
//    void createTaskView(Consumer<TaskView> onCreate) {
//        mTaskViewFactory.create(mContext, mExecutor, onCreate);
//    }
//
//    private static void cleanUpExistingTaskViewTasks(List<TaskAppearedInfo> taskAppearedInfos) {
//        ActivityTaskManager atm = ActivityTaskManager.getInstance();
//        for (TaskAppearedInfo taskAppearedInfo : taskAppearedInfos) {
//            TaskInfo taskInfo = taskAppearedInfo.getTaskInfo();
//            // 只有TaskView任务具有WINDOWING_MODE_MULTI_INDOW。
//            if (taskInfo.getWindowingMode() == WINDOWING_MODE_MULTI_WINDOW) {
////                if (DBG) Slog.d(TAG, "Found the dangling task, removing: " + taskInfo.taskId);
//                atm.removeTask(taskInfo.taskId);
//            }
//        }
//    }
//}