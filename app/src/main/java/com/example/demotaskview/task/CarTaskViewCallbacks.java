package com.example.demotaskview.task;

/**
 * A callback interface for the host activity that uses {@link CarTaskView} and its derivatives.
 */
interface CarTaskViewCallbacks {
    /**
     * Called when the underlying {@link CarTaskView} instance is created.
     *
     * @param taskView the new newly created {@link CarTaskView} instance.
     */
    void onTaskViewCreated(CarTaskView taskView);

    /**
     * Called when the underlying {@link CarTaskView} is ready. A {@link CarTaskView} can be
     * considered ready when it has completed all the set up that is required.
     * This callback is only triggered once.
     * <p>
     * For {@link LaunchRootCarTaskView}, this is called once the launch root task has been
     * fully set up.
     * For {@link SemiControlledCarTaskView} & {@link ControlledCarTaskView} this is called when
     * the surface is created.
     */
    void onTaskViewReady();
}


