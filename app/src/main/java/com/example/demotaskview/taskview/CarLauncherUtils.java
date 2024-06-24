/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import timber.log.Timber;

/**
 * Utils for CarLauncher package.
 */
public class CarLauncherUtils {

    private static final String ACTION_APP_GRID = "com.android.car.carlauncher.ACTION_APP_GRID";

    private CarLauncherUtils() {
    }

    public static Intent getAppsGridIntent() {
        return new Intent(ACTION_APP_GRID);
    }

    /**
     * Intent used to find/launch the maps activity to run in the relevant DisplayArea.
     */
    public static Intent getMapsIntent() {
        Intent defaultIntent = new Intent();
        defaultIntent.setComponent(new ComponentName("",
            ""));
        defaultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return defaultIntent;
    }

    /**
     * Returns {@code true} if a proper limited map intent is configured via
     * {@code config_smallCanvasOptimizedMapIntent} string resource.
     */
    public static boolean isSmallCanvasOptimizedMapIntentConfigured(Context context) {
        // It is always false due to empty config_smallCanvasOptimizedMapIntent in the resources.
        return false;
    }

    /**
     * Returns an intent to trigger a map with a limited functionality (e.g., one to be used when
     * there's not much screen real estate).
     */
    public static Intent getSmallCanvasOptimizedMapIntent() {
        return getMapsIntent();
    }

    static boolean isCustomDisplayPolicyDefined(Context context) {
        Resources resources = context.getResources();
        String customPolicyName = null;
        try {
            customPolicyName = resources.getString(com.android.internal.R.string.config_deviceSpecificDisplayAreaPolicyProvider);
        } catch (Resources.NotFoundException ex) {
            Timber.w("custom policy provider not defined");
        }
        return customPolicyName != null && !customPolicyName.isEmpty();
    }
}
