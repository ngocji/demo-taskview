/**
 * @file RunnerWithBackoff.kt
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

import android.os.Handler
import android.os.Looper
import timber.log.Timber

/**
 * A wrapper class for [Runnable] which retries in an exponential backoff manner.
 */
internal class RunnerWithBackoff(private val mAction: Runnable) {
    private val mHandler = Handler(Looper.getMainLooper())
    private var mBackoffTimeMs = 0
    private var mAttempts = 0
    private val mRetryRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mAttempts >= MAXIMUM_ATTEMPTS) {
                Timber.e("$TAG Failed to perform action, even after $mAttempts attempts")
                return
            }
            Timber.d("$TAG Executing the action. Attempt number $mAttempts")
            mAction.run()
            mHandler.postDelayed(this, mBackoffTimeMs.toLong())
            increaseBackoff()
            mAttempts++
        }
    }

    private fun increaseBackoff() {
        mBackoffTimeMs *= 2
        if (mBackoffTimeMs > MAXIMUM_BACKOFF_TIME_MS) {
            mBackoffTimeMs = MAXIMUM_BACKOFF_TIME_MS
        }
    }

    /**
     * Starts the retrying. The first try happens synchronously.
     */
    fun start() {
        Timber.d("$TAG start backoff runner")
        // Stop the existing retrying as a safeguard to prevent multiple starts.
        stopInternal()
        mBackoffTimeMs = FIRST_BACKOFF_TIME_MS
        mAttempts = 0
        // Call .run() instead of posting to handler so that first try can happen synchronously.
        mRetryRunnable.run()
    }

    /**
     * Stops the retrying.
     */
    fun stop() {
        Timber.d("$TAG stop backoff runner")
        stopInternal()
    }

    private fun stopInternal() {
        mHandler.removeCallbacks(mRetryRunnable)
    }

    companion object {
        private val TAG = RunnerWithBackoff::class.java.simpleName
        private const val MAXIMUM_ATTEMPTS = 5
        private const val FIRST_BACKOFF_TIME_MS = 1000 // 1 second
        private const val MAXIMUM_BACKOFF_TIME_MS = 8000 // 8 seconds
    }
}
