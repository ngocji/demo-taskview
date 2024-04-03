/**
 * @file CarTaskViewCallbacks.kt
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

import com.android.wm.shell.taskview.TaskView
import com.example.demotaskview.task.CarTaskView

/**
 * A callback interface for the host activity that uses [CarTaskView] and its derivatives.
 */
interface CarTaskViewCallbacks {
    /**
     * Called when the underlying [CarTaskView] instance is created.
     *
     * @param taskView the new newly created [CarTaskView] instance.
     */
    fun onTaskViewCreated(taskView: TaskView)

    /**
     * Called when the underlying [CarTaskView] is ready. A [CarTaskView] can be
     * considered ready when it has completed all the set up that is required.
     * This callback is only triggered once.
     *
     *
     * For [LaunchRootCarTaskView], this is called once the launch root task has been
     * fully set up.
     * For [SemiControlledCarTaskView] & [ControlledCarTaskView] this is called when
     * the surface is created.
     */
    fun onTaskViewReady()
}
