package com.example.demotaskview

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.ArraySet
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.android.wm.shell.taskview.TaskView
import com.example.demotaskview.task.CarTaskView
import com.example.demotaskview.task.ControlledCarTaskViewCallbacks
import com.example.demotaskview.task.ControlledCarTaskViewConfig
import com.example.demotaskview.task.TaskViewManager


class MainActivity : AppCompatActivity() {
    private var mTaskView: TaskView? = null
    private lateinit var mTaskViewManager: TaskViewManager;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = findViewById<CardView>(R.id.card)

        setUpTaskView(view)

    }

    override fun onStart() {
        super.onStart()
        if (this::mTaskViewManager.isInitialized && mTaskViewManager.isReleased) {
            setUpTaskView(findViewById<CardView>(R.id.card))
        }
    }


    private fun setUpTaskView(parent: ViewGroup) {
        val taskViewPackages: Set<String> = ArraySet()
        mTaskViewManager = TaskViewManager(this, Handler(mainLooper))
        mTaskViewManager.createControlledCarTaskView(
            mainExecutor,
            ControlledCarTaskViewConfig.builder()
                .setActivityIntent(getMapsIntent()) // TODO(b/263876526): Enable auto restart after ensuring no CTS failure.
                .setAutoRestartOnCrash(false)
                .build(),
            object : ControlledCarTaskViewCallbacks {
                override fun onTaskViewCreated(taskView: CarTaskView) {
                    parent.addView(taskView)
                    mTaskView = taskView
                }

                override fun onTaskViewReady() {
//                        maybeLogReady();
                }

                override fun getDependingPackageNames(): Set<String> {
                    return taskViewPackages
                }
            })
    }

    private fun getMapsIntent(): Intent {
        val mapIntent = Intent(Settings.ACTION_SETTINGS)
        // Don't want to show this Activity in Recents.
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        return mapIntent
    }


}