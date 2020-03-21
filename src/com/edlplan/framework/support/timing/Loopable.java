package com.edlplan.framework.support.timing;

public abstract class Loopable {

    private Flag flag = Flag.Run;
    private ILooper looper;

    public Flag getFlag() {
        return flag;
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    public ILooper getLooper() {
        return looper;
    }

    public void setLooper(ILooper lp) {
        this.looper = lp;
    }

    public void onRemove() {

    }

    public abstract void onLoop(double deltaTime);

    public enum Flag {
        Run, Skip, Stop
    }
}
