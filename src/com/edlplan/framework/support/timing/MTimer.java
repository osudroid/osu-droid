package com.edlplan.framework.support.timing;

import com.edlplan.framework.support.Framework;

public class MTimer {
    public boolean hasInitial;

    public double startTime;

    public double nowTime;

    public double deltaTime;

    public double runnedTime;

    public MTimer() {
        hasInitial = false;
    }

    public boolean hasInitial() {
        return hasInitial;
    }

    public void initial() {
        initial(Framework.relativePreciseTimeMillion());
    }

    public void initial(double s) {
        hasInitial = true;
        startTime = s;
        nowTime = s;
        deltaTime = 0;
        runnedTime = 0;
    }

    public double nowTime() {
        return nowTime;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public void refresh(double _deltaTime) {
        deltaTime = _deltaTime;
        nowTime += _deltaTime;
        runnedTime += _deltaTime;
    }

    public void refresh() {
        refresh(Framework.relativePreciseTimeMillion() - nowTime);
    }
}
