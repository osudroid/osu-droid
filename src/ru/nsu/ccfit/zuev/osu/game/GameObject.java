package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import android.os.Handler;
import android.os.Looper;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;

public abstract class GameObject {

    // All Value Animators needs to run on a thread that has its Looper, since update thread doesn't have one, using a Handler
    // will allow us to run animations with the main looper. I'm leaving this here to use it in more inheritors if it's needed.
    protected static final Handler mAnimationHandler = new Handler(Looper.getMainLooper());

    protected boolean endsCombo;
    protected boolean autoPlay = false;
    protected float hitTime = 0;
    protected int id = -1;
    protected Replay.ReplayObjectData replayObjectData = null;
    protected boolean startHit = false;
    protected PointF pos = new PointF();

    public Replay.ReplayObjectData getReplayData() {
        return replayObjectData;
    }

    public void setReplayData(Replay.ReplayObjectData replayObjectData) {
        this.replayObjectData = replayObjectData;
    }

    public void setEndsCombo(final boolean endsCombo) {
        this.endsCombo = endsCombo;
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

    public PointF getPos() {return pos;}
}
