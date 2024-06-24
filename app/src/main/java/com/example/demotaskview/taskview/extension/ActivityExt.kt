package com.hyundai.taskview.extension

import android.app.Activity
import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

fun Activity.isVisibleForAutofill() = try {
    val isVisibleForAutofill = HiddenApiBypass.invoke(
        Activity::class.java,
        this,
        "isVisibleForAutofill"
    ) as? Boolean ?: false
    Timber.d("ActivityExt isVisibleForAutofill: $isVisibleForAutofill")
    isVisibleForAutofill
} catch (e: Exception) {
    Timber.e(e)
    false
}

fun Activity.getUserId() = try {
    val userHandle: UserHandle = android.os.Process.myUserHandle()
    val userManager = this.getSystemService(Context.USER_SERVICE) as UserManager?
    val userId = userManager?.getSerialNumberForUser(userHandle) ?: -1
    Timber.d("ActivityExt getUserId: $userId")
    userId.toInt()
} catch (e: Exception) {
    Timber.e(e)
    -1
}
