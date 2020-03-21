package ru.nsu.ccfit.zuev.osu.game;

import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData;

public abstract class GameObject {
    protected boolean endsCombo;
    protected boolean autoPlay = false;
    protected float hitTime = 0;
    protected int id = -1;
    protected ReplayObjectData replayData = null;

    public ReplayObjectData getReplayData() {
        return replayData;
    }

    public void setReplayData(ReplayObjectData replayData) {
        this.replayData = replayData;
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
}
