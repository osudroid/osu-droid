package com.edlplan.framework.support.timing;

public interface IRunnableHandler {
    void post(Runnable r);

    void post(Runnable r, double delayMS);
}
