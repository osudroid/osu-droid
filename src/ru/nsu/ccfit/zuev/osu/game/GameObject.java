package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import ru.nsu.ccfit.zuev.osu.scoring.Replay;

public abstract class GameObject {
    /**
     * The maximum allowable time difference from the start time of an object
     * to its hit time to be considered a hit, in seconds.
     */
    protected static final float objectHittableRange = 0.4f;

    protected boolean endsCombo;
    protected boolean autoPlay = false;
    protected float hitTime = 0;
    protected int id = -1;
    protected Replay.ReplayObjectData replayObjectData = null;
    protected boolean startHit = false;
    protected PointF position = new PointF();

    public Replay.ReplayObjectData getReplayData() {
        return replayObjectData;
    }

    public void setReplayData(Replay.ReplayObjectData replayObjectData) {
        this.replayObjectData = replayObjectData;
    }

    public void setAutoPlay() {
        autoPlay = true;
    }

    public abstract void update(float dt);

    public float getHitTime() {
        return hitTime;
    }

    public void setHitTime(final float hitTime) {
        this.hitTime = hitTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isStartHit(){
        return startHit;
    }

    public void tryHit(float dt) {}

    public PointF getPosition() {
        return position;
    }

    public void stopLoopingSamples() {}
}
