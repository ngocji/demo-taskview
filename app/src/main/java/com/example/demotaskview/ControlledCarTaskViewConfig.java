//package com.example.demotaskview;
//
//import android.content.Intent;
///** This class provides the required configuration to create a {@link ControlledCarTaskView}. */
//public class ControlledCarTaskViewConfig {
//    final Intent mActivityIntent;
//    // TODO(b/242861717): When mAutoRestartOnCrash is enabled, mPackagesThatCanRestart doesn't make
//    // a lot of sense. Consider removing it when there is more confidence with mAutoRestartOnCrash.
//    final boolean mAutoRestartOnCrash;
//    final boolean mCaptureGestures;
//    final boolean mCaptureLongPress;
//    private ControlledCarTaskViewConfig(
//            Intent activityIntent,
//            boolean autoRestartOnCrash,
//            boolean captureGestures,
//            boolean captureLongPress) {
//        mActivityIntent = activityIntent;
//        mAutoRestartOnCrash = autoRestartOnCrash;
//        mCaptureGestures = captureGestures;
//        mCaptureLongPress = captureLongPress;
//    }
//    /**
//     * Creates a {@link Builder} object that is used to create instances of {@link
//     * ControlledCarTaskViewConfig}.
//     */
//    public static Builder builder() {
//        return new Builder();
//    }
//    /** A builder class for {@link ControlledCarTaskViewConfig}. */
//    public static class Builder {
//        private Intent mActivityIntent;
//        private boolean mAutoRestartOnCrash;
//        private boolean mCaptureGestures;
//        private boolean mCaptureLongPress;
//        private Builder() {}
//        /**
//         * The intent of the activity that is meant to be started in this {@link
//         * ControlledCarTaskView}.
//         */
//        public Builder setActivityIntent(Intent activityIntent) {
//            mActivityIntent = activityIntent;
//            return this;
//        }
//        /**
//         * Sets the auto restart functionality. If set, the {@link ControlledCarTaskView} will
//         * restart the task by re-launching the intent set via {@link #setActivityIntent(Intent)}
//         * when the task crashes.
//         */
//        public Builder setAutoRestartOnCrash(boolean autoRestartOnCrash) {
//            mAutoRestartOnCrash = autoRestartOnCrash;
//            return this;
//        }
//        /**
//         * Enables the swipe gesture capturing over {@link ControlledCarTaskView}. When enabled, the
//         * swipe gestures won't be sent to the embedded app and will instead be forwarded to the
//         * host activity.
//         */
//        public Builder setCaptureGestures(boolean captureGestures) {
//            mCaptureGestures = captureGestures;
//            return this;
//        }
//        /**
//         * Enables the long press capturing over {@link ControlledCarTaskView}. When enabled, the
//         * long press won't be sent to the embedded app and will instead be sent to the listener
//         * specified via {@link
//         * ControlledCarTaskView#setOnLongClickListener(View.OnLongClickListener)}.
//         *
//         * <p>If disabled, the listener supplied via {@link
//         * ControlledCarTaskView#setOnLongClickListener(View.OnLongClickListener)} won't be called.
//         */
//        public Builder setCaptureLongPress(boolean captureLongPress) {
//            mCaptureLongPress = captureLongPress;
//            return this;
//        }
//        /** Creates the {@link ControlledCarTaskViewConfig} object. */
//        public ControlledCarTaskViewConfig build() {
//            if (mActivityIntent == null) {
//                throw new IllegalArgumentException("mActivityIntent can't be null");
//            }
//            return new ControlledCarTaskViewConfig(
//                    mActivityIntent, mAutoRestartOnCrash, mCaptureGestures, mCaptureLongPress);
//        }
//    }
//}