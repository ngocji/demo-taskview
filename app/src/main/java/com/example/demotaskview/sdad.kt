//package com.example.demotaskview
//
//import android.annotation.SuppressLint
//import android.app.ActivityOptions
//import android.app.PendingIntent
//import android.content.ActivityNotFoundException
//import android.content.ComponentName
//import android.content.Intent
//import android.graphics.Rect
//import android.os.Bundle
//import android.os.Handler
//import android.provider.Settings
//import android.util.Log
//import android.view.Display
//import android.view.ViewGroup
//import androidx.appcompat.app.AppCompatActivity
//import androidx.cardview.widget.CardView
//import com.android.wm.shell.common.HandlerExecutor
//import com.android.wm.shell.taskview.TaskView
//
//
//class MainActivity : AppCompatActivity() {
//    private var mTaskViewReady: Boolean = false
//    private var mTaskView: TaskView? = null
//    private lateinit var mTaskViewManager: TaskViewManager;
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        val view = findViewById<CardView>(R.id.card)
//
//        setUpTaskView(view)
//
//    }
//
//
//
//    private fun setUpTaskView(parent: ViewGroup) {
//        mTaskViewManager = TaskViewManager(
//            this,
//            HandlerExecutor(Handler(mainLooper))
//        )
//
//        mTaskViewManager.createTaskView { taskView ->
//            taskView.setListener(mainExecutor, mTaskViewListener)
//
//            parent.addView(taskView)
//            mTaskView = taskView
//        }
//    }
//
//    @SuppressLint("NewApi")
//    private fun startMapsInTaskView() {
//        if (mTaskView == null || !mTaskViewReady) {
//            return
//        }
//        if (isInMultiWindowMode || isInPictureInPictureMode) {
//            return
//        }
//        // 当ActivityVisibilityTests的显示器关闭时，不要启动地图。
//        if (display!!.state != Display.STATE_ON) {
//            return
//        }
//        try {
//            val options = ActivityOptions.makeCustomAnimation(
//                this,  /* enterResId= */
//                0,  /* exitResId= */0
//            )
//
//            val launchBounds = Rect()
//            mTaskView?.getBoundsOnScreen(launchBounds)
//
//            val intent = Intent(Intent.ACTION_VIEW)
//            intent.type = "image/*"
//            val config = ControlledCarTaskViewConfig.builder()
//                .setActivityIntent(Intent(android.provider.Settings.ACTION_SETTINGS))
//                .setAutoRestartOnCrash(false)
//                .build()
//
//
//            val fillInIntent =
//                if (config.mActivityIntent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS != 0) {
//                    Intent().addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//                } else {
//                    null
//                }
//
//
//            mTaskView!!.startActivity(
//                PendingIntent.getActivity(
//                    this,
//                    0,
//                    config.mActivityIntent,
//                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//                ),
//                fillInIntent,
//                options,
//                launchBounds
//            )
//        } catch (e: ActivityNotFoundException) {
//            Log.w("TAG", "Maps activity not found", e)
//        }
//    }
//
//
//    private val mTaskViewListener: TaskView.Listener = object : TaskView.Listener {
//        override fun onInitialized() {
////            if (com.android.car.carlauncher.CarLauncher.DEBUG) Log.d(
////                com.android.car.carlauncher.CarLauncher.TAG,
////                "onInitialized(" + getUserId() + ")"
////            )
//            mTaskViewReady = true
//            startMapsInTaskView()
////            maybeLogReady()
//        }
//
//        override fun onReleased() {
////            if (com.android.car.carlauncher.CarLauncher.DEBUG) Log.d(
////                com.android.car.carlauncher.CarLauncher.TAG,
////                "onReleased(" + getUserId() + ")"
////            )
//            mTaskViewReady = false
//        }
//
//        override fun onTaskCreated(taskId: Int, name: ComponentName) {
////            if (com.android.car.carlauncher.CarLauncher.DEBUG) Log.d(
////                com.android.car.carlauncher.CarLauncher.TAG,
////                "onTaskCreated: taskId=$taskId"
////            )
////            mTaskViewTaskId = taskId
//        }
//
//        override fun onTaskRemovalStarted(taskId: Int) {
////            if (com.android.car.carlauncher.CarLauncher.DEBUG) Log.d(
////                com.android.car.carlauncher.CarLauncher.TAG,
////                "onTaskRemovalStarted: taskId=$taskId"
////            )
////            mTaskViewTaskId = INVALID_TASK_ID
//        }
//    }
//}