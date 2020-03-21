package com.edlplan.framework.support.timing;

import com.edlplan.framework.timing.IntervalSchedule;
import com.edlplan.framework.timing.TimeUpdateable;
import com.edlplan.framework.utils.annotation.NotThreadSafe;

public interface IHasIntervalSchedule {
    IntervalSchedule getIntervalSchedule();

    @NotThreadSafe
    default void addIntervalTask(double start, double end, TimeUpdateable updateable) {
        getIntervalSchedule().addTask(start, end, false, updateable);
    }

    @NotThreadSafe
    default void addAnimTask(double start, double duration, TimeUpdateable anim) {
        getIntervalSchedule().addAnimTask(start, duration, anim);
    }

    @NotThreadSafe
    default void addTask(double time, Runnable runnable) {
        getIntervalSchedule().addTask(time, time, true, time1 -> runnable.run());
    }
}
