package com.example.demotaskview.taskview.extension

import android.view.Window
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

const val PRIVATE_FLAG_TRUSTED_OVERLAY = 0x20000000
fun Window.addPrivateFlags(flags: Int) = try {
    HiddenApiBypass.invoke(Window::class.java, this, "addPrivateFlags", flags)
    Timber.d("WindowExt Added private flags: $flags")
} catch (e: Exception) {
    Timber.e(e)
}
