package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.Utils;

public class LinearSongProgress extends GameObject {
    private final Rectangle progressRect;
    private final Rectangle bgRect;
    private float time;
    private float startTime;
    private float passedTime;
    private float initialPassedTime;

    public LinearSongProgress(final GameObjectListener listener,
                              final Scene scene, final float time, final float startTime, final PointF pos) {
        this(listener, scene, time, startTime, pos, Utils.toRes(300), Utils.toRes(7));
    }

    public LinearSongProgress(final GameObjectListener listener,
                              final Scene scene, final float time, final float startTime, final PointF pos, float width, float height) {
        this.time = time;
        this.startTime = startTime;
        if (listener != null)
            listener.addPassiveObject(this);

        bgRect = new Rectangle(pos.x, pos.y, width, height);
        bgRect.setColor(0, 0, 0, 0.3f);
        scene.attachChild(bgRect);

        progressRect = new Rectangle(bgRect.getX(), bgRect.getY(), 0,
                bgRect.getHeight());
        progressRect.setColor(153f / 255f, 204f / 255f, 51f / 255f);
        scene.attachChild(progressRect);
    }


    @Override
    public void update(final float dt) {
        if (passedTime >= startTime) {
            passedTime = Math.min(time, passedTime + dt);
            progressRect.setWidth(bgRect.getWidth() * (passedTime - startTime)
                    / (time - startTime));
        } else {
            passedTime = Math.min(startTime, passedTime + dt);
            progressRect.setWidth(bgRect.getWidth() * (passedTime - initialPassedTime) / (startTime - initialPassedTime));
            if (passedTime >= startTime) {
                progressRect.setColor(1, 1, 150f / 255f);
            }
        }
    }

    public void setTime(float time) {
        this.time = time;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public void setPassedTime(float passedTime) {
        this.passedTime = passedTime;
    }

    public void setInitialPassedTime(float initialPassedTime) {
        this.initialPassedTime = initialPassedTime;
    }

    public void setProgressRectColor(RGBColor color) {
        this.progressRect.setColor(color.r(), color.g(), color.b());
    }

    public void setProgressRectAlpha(float alpha) {
        this.progressRect.setAlpha(alpha);
    }
}
