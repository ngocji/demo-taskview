package com.hyundai.taskview.extension

import android.app.TaskInfo
import android.window.WindowContainerToken
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

fun TaskInfo.getToken(): WindowContainerToken? = try {
    val token = HiddenApiBypass.invoke(
        TaskInfo::class.java,
        this,
        "getToken"
    ) as? WindowContainerToken
    Timber.d("TaskInfoExt getToken: $token")
    token
} catch (e: Exception) {
    Timber.e(e)
    null
}

fun TaskInfo.hasParentTask(): Boolean = try {
    val hasParentTask = HiddenApiBypass.invoke(
        TaskInfo::class.java,
        this,
        "hasParentTask"
    ) as? Boolean ?: false
    Timber.d("TaskInfoExt hasParentTask: $hasParentTask")
    hasParentTask
} catch (e: Exception) {
    Timber.e(e)
    false
}

fun TaskInfo.getWindowingMode(): Int = try {
    val windowMode = HiddenApiBypass.invoke(
        TaskInfo::class.java,
        this,
        "getWindowingMode"
    ) as? Int ?: 0
    Timber.d("TaskInfoExt getWindowingMode: $windowMode")
    windowMode
} catch (e: Exception) {
    Timber.e(e)
    0
}
