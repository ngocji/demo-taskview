/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.demotaskview.taskview;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.UserManager;
import android.view.Display;
import android.view.View;

import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.hyundai.taskview.CarTaskView;

import java.util.Set;
import java.util.concurrent.Executor;

import timber.log.Timber;

/**
 * A controlled {@link CarTaskView} is fully managed by the {@link TaskViewManager}.
 * The underlying task will be restarted if it is crashed.
 * <p>
 * It should be used when:
 * <ul>
 *     <li>The underlying task is meant to be started by the host and be there forever.</li>
 * </ul>
 */
final class ControlledCarTaskView extends CarTaskView {
    private final Executor mCallbackExecutor;
    private final Intent mActivityIntent;
    private final boolean mAutoRestartOnCrash;
    // TODO(b/242861717): When mAutoRestartOnCrash is enabled, mPackagesThatCanRestart doesn't make
    // a lot of sense. Consider removing it when there is more confidence with mAutoRestartOnCrash.
    private final ControlledCarTaskViewCallbacks mCallbacks;
    private final UserManager mUserManager;

    private final Context mContext;

    public ControlledCarTaskView(Activity context,
                                 ShellTaskOrganizer organizer,
                                 SyncTransactionQueue syncQueue,
                                 Executor callbackExecutor,
                                 Intent activityIntent,
                                 Boolean autoRestartOnCrash,
                                 ControlledCarTaskViewCallbacks callbacks,
                                 UserManager userManager) {
        super(context, organizer, syncQueue);
        mCallbackExecutor = callbackExecutor;
        mActivityIntent = activityIntent;
        mAutoRestartOnCrash = autoRestartOnCrash;
        mCallbacks = callbacks;
        mUserManager = userManager;
        mContext = context;

        mCallbackExecutor.execute(() -> mCallbacks.onTaskViewCreated(this));
    }

    private static Rect getBoundsOnScreen(View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        return new Rect(loc[0], loc[1], loc[0] + view.getWidth(), loc[1] + view.getHeight());
    }

    @Override
    protected void onCarTaskViewInitialized() {
        super.onCarTaskViewInitialized();
        startActivity();
        mCallbackExecutor.execute(mCallbacks::onTaskViewReady);
    }

    /**
     * Starts the underlying activity.
     */
    public void startActivity() {
        if (!mUserManager.isUserUnlocked()) {
            Timber.d("Can't start activity due to user is isn't unlocked");
            return;
        }

        // Don't start activity when the display is off. This can happen when the taskview is not
        // attached to a window.
        if (getDisplay() == null) {
            Timber.w("Can't start activity because display is not available in taskview yet.");
            return;
        }
        // Don't start activity when the display is off for ActivityVisibilityTests.
        if (getDisplay().getState() != Display.STATE_ON) {
            Timber.w("Can't start activity due to the display is off");
            return;
        }

        ActivityOptions options = ActivityOptions.makeCustomAnimation(mContext,
            /* enterResId= */ 0, /* exitResId= */ 0);
        Rect launchBounds = getBoundsOnScreen(this);
        Timber.d("Starting (" + mActivityIntent.getComponent() + ") on " + launchBounds);
        startActivity(
            PendingIntent.getActivity(mContext, /* requestCode= */ 0,
                mActivityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT),
            /* fillInIntent= */ null, options, launchBounds);
    }

    /**
     * See {@link ControlledCarTaskViewCallbacks#getDependingPackageNames()}.
     */
    Set<String> getDependingPackageNames() {
        return mCallbacks.getDependingPackageNames();
    }

    @Override
    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        super.onTaskVanished(taskInfo);
        if (mAutoRestartOnCrash) {
            Timber.i("Restarting task " + taskInfo.baseActivity + " in ControlledCarTaskView");
            startActivity();
        }
    }

}
