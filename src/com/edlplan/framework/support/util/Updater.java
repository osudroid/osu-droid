package com.edlplan.framework.support.util;

public abstract class Updater {

    private final Object lock = new Object();

    private int updateId;

    private Event runningEvent;

    public void update() {
        synchronized (lock) {
            updateId++;
            if (runningEvent == null) {
                Event event = new Event();
                event.updateId = updateId;
                event.runnable = createEventRunnable();
                runningEvent = event;
                postEvent(event);
            }
        }
    }

    public abstract Runnable createEventRunnable();

    public abstract void postEvent(Runnable r);

    public class Event implements Runnable {
        int updateId;
        Runnable runnable;

        @Override
        public void run() {
            runnable.run();
            synchronized (lock) {
                if (this.updateId > Updater.this.updateId) {
                    //表示之后又发生了更新，应该再次更新
                    Event event = new Event();
                    event.updateId = Updater.this.updateId;
                    event.runnable = createEventRunnable();
                    runningEvent = event;
                    postEvent(event);
                } else {
                    //表示没有新的更新了
                    runningEvent = null;
                }
            }
        }
    }

}
