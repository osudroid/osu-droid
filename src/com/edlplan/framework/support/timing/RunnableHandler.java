package com.edlplan.framework.support.timing;

import com.edlplan.framework.utils.SafeList;

import java.util.Iterator;

public class RunnableHandler extends Loopable implements IRunnableHandler {
    private ILooper looper;

    private SafeList<DelayedRunnable> bufferedRunnables;

    private Loopable.Flag flag = Loopable.Flag.Run;

    public RunnableHandler() {
        bufferedRunnables = new SafeList<DelayedRunnable>();
    }

    @Override
    public void post(Runnable r, double delayMS) {
        bufferedRunnables.add(new DelayedRunnable(r, delayMS));
    }

    @Override
    public void post(Runnable r) {
        post(r, 0);
    }

    public void stop() {
        flag = Loopable.Flag.Stop;
    }

    public void block() {
        flag = Loopable.Flag.Skip;
    }

    @Override
    public void setLooper(ILooper lp) {

        this.looper = lp;
    }

    @Override
    public void onRemove() {

        bufferedRunnables.clear();
        flag = Loopable.Flag.Stop;
    }

    @Override
    public void onLoop(double deltaTime) {

        bufferedRunnables.startIterate();
        Iterator<DelayedRunnable> iter = bufferedRunnables.iterator();
        DelayedRunnable tmp;
        while (iter.hasNext()) {
            tmp = iter.next();
            tmp.delay -= deltaTime;
            if (tmp.delay <= 0) {
                tmp.r.run();
                iter.remove();
            }
        }
        bufferedRunnables.endIterate();
    }

    @Override
    public Loopable.Flag getFlag() {

        return flag;
    }

    private class DelayedRunnable {
        public Runnable r;
        public double delay;

        public DelayedRunnable(Runnable r, double delay) {
            this.r = r;
            this.delay = delay;
        }
    }

}
