package com.edlplan.framework.support.timing;

import com.edlplan.framework.support.Framework;

/**
 * 记载处理帧时间的类
 */
public class FrameClock {

    private double startTime = -1;

    private double frameTime;

    private double deltaTime;

    private boolean running = false;

    public void offset(double o) {
        frameTime += o;
    }

    public void start() {
        if (startTime == -1) {
            startTime = Framework.frameworkTime();
            running = true;
        }
    }

    /**
     * 缓存的帧时间
     *
     * @return
     */
    public double getFrameTime() {
        return frameTime;
    }

    /**
     * 当前的Clock是否是有效更新的
     *
     * @return
     */
    public boolean isRunninng() {
        return running;
    }

    public double toClockTime(double frameworkTime) {
        if (running) {
            return frameworkTime - startTime;
        } else {
            return frameTime;
        }
    }

    /**
     * 更新Clock的缓存时间
     */
    public void update() {
        if (running) {
            double t = Framework.frameworkTime() - startTime;
            deltaTime = t - frameTime;
            frameTime = t;
        }
    }

    /**
     * 让暂停的Clock运行
     */
    public void run() {
        if (!running) {
            running = true;
            double dt = Framework.frameworkTime() - frameTime;
            startTime += dt;
        }
    }

    /**
     * 暂停Clock，暂停之后调用update时不再更新时间
     */
    public void pause() {
        if (running) {
            running = false;
            frameTime = Framework.frameworkTime();
        }
    }
}
