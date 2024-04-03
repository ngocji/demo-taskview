/**
 * @file NavigationWidgets.kt
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
package com.example.demotaskview

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.wm.shell.taskview.TaskView
import com.example.demotaskview.task.ControlledCarTaskViewCallbacks
import com.example.demotaskview.task.ControlledCarTaskViewConfig
import com.example.demotaskview.task.TaskViewManager
import timber.log.Timber

@Composable
fun NavigationWidgets(modifier: Modifier) {
    val activity = LocalContext.current as Activity

    val taskViewManager = remember {
        TaskViewManager(activity, Handler(Looper.getMainLooper()))
    }

    var viewGroup by remember {
        mutableStateOf<ViewGroup?>(null)
    }

    SideEffect {
        // Setting as trusted overlay to let touches pass through.
        activity.window.addPrivateFlags(WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY)
        // To pass touches to the underneath task.
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
    }

    DisposableEffect(key1 = Unit) {
        Timber.e("Navigation: onStartDispose")
        taskViewManager.isPendingRestartActivity = false

        onDispose {
            Timber.e("Navigation: onDispose ---->")
            taskViewManager.isPendingRestartActivity = true
        }
    }

    LaunchedEffect(
        key1 = viewGroup,
        block = {
            viewGroup?.run {
                Timber.d("Run Create taskView")
                taskViewManager.createControlledCarTaskView(
                    activity.mainExecutor,
                    ControlledCarTaskViewConfig.builder()
                        .setActivityIntent(getMapsIntent(activity)) // TODO(b/263876526): Enable auto restart after ensuring no CTS failure.
                        .setAutoRestartOnCrash(false)
                        .build(),
                    object : ControlledCarTaskViewCallbacks {
                        override fun onTaskViewCreated(taskView: TaskView) {
                            Timber.d("OnTasViewCreated")
                            addView(taskView)
                        }

                        override fun onTaskViewReady() {
                            Timber.d("OnTasViewReady")
                        }
                    },
                )
            }
        },
    )

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                clipToOutline = true
                viewGroup = this
            }
        },
    )
}

private fun getMapsIntent(activity: Activity): Intent {
    val mapIntent = Intent(Settings.ACTION_SETTINGS)
    // Don't want to show this Activity in Recents.
    mapIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    return mapIntent
}
