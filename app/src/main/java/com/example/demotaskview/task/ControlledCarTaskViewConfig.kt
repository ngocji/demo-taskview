/**
 * @file ControlledCarTaskViewConfig.kt
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

import android.content.Intent

/**
 * This class provides the required configuration to create a [ControlledCarTaskView].
 */
class ControlledCarTaskViewConfig(
    @JvmField val mActivityIntent: Intent,
    // TODO(b/242861717): When mAutoRestartOnCrash is enabled, mPackagesThatCanRestart doesn't make
    // a lot of sense. Consider removing it when there is more confidence with mAutoRestartOnCrash.
    @JvmField val mAutoRestartOnCrash: Boolean,
    @JvmField val mCaptureGestures: Boolean,
    @JvmField val mCaptureLongPress: Boolean,
) {
    /**
     * A builder class for [ControlledCarTaskViewConfig].
     */
    class Builder {
        private var mActivityIntent: Intent? = null
        private var mAutoRestartOnCrash = false
        private var mCaptureGestures = false
        private var mCaptureLongPress = false

        /**
         * The intent of the activity that is meant to be started in this [ ].
         */
        fun setActivityIntent(activityIntent: Intent?): Builder {
            mActivityIntent = activityIntent
            return this
        }

        /**
         * Sets the auto restart functionality. If set, the [ControlledCarTaskView] will
         * restart the task by re-launching the intent set via [.setActivityIntent]
         * when the task crashes.
         */
        fun setAutoRestartOnCrash(autoRestartOnCrash: Boolean): Builder {
            mAutoRestartOnCrash = autoRestartOnCrash
            return this
        }

        /**
         * Enables the swipe gesture capturing over [ControlledCarTaskView]. When enabled, the
         * swipe gestures won't be sent to the embedded app and will instead be forwarded to the
         * host activity.
         */
        fun setCaptureGestures(captureGestures: Boolean): Builder {
            mCaptureGestures = captureGestures
            return this
        }

        /**
         * Enables the long press capturing over [ControlledCarTaskView]. When enabled, the
         * long press won't be sent to the embedded app and will instead be sent to the listener
         * specified via [ ][ControlledCarTaskView.setOnLongClickListener].
         *
         *
         * If disabled, the listener supplied via [ ][ControlledCarTaskView.setOnLongClickListener] won't be called.
         */
        fun setCaptureLongPress(captureLongPress: Boolean): Builder {
            mCaptureLongPress = captureLongPress
            return this
        }

        /**
         * Creates the [ControlledCarTaskViewConfig] object.
         */
        fun build(): ControlledCarTaskViewConfig {
            requireNotNull(mActivityIntent) { "mActivityIntent can't be null" }
            return ControlledCarTaskViewConfig(
                mActivityIntent!!, mAutoRestartOnCrash, mCaptureGestures, mCaptureLongPress,
            )
        }
    }

    companion object {
        /**
         * Creates a [Builder] object that is used to create instances of [ ].
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
