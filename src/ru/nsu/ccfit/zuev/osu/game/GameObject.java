package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.annotation.Nullable;

import com.osudroid.game.CursorEvent;
import com.rian.osu.beatmap.HitWindow;
import com.rian.osu.beatmap.hitobject.HitObject;

import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;

public abstract class GameObject {
    /**
     * The maximum allowable time difference from the start time of an object
     * to its hit time to be considered a hit, in seconds.
     */
    protected static final float objectHittableRange = HitWindow.MISS_WINDOW / 1000;

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

    /**
     * Obtains the {@link CursorEvent} that hits this {@link GameObject}, if any.
     *
     * @param listener The {@link GameObjectListener} to get cursors from.
     * @param hitObject The {@link HitObject} associated with this {@link GameObject}.
     * @param objectElapsedTime The elapsed time since the start of this {@link GameObject}, in seconds.
     * @return The {@link CursorEvent} that hits this {@link GameObject}, or {@code null} if none.
     */
    @Nullable
    protected final CursorEvent getHittingCursor(GameObjectListener listener, HitObject hitObject,
                                                 double objectElapsedTime) {
        int cursorCount = listener.getCursorsCount();

        // For Autopilot, we only need to check if a cursor is pressed.
        if (GameHelper.isAutopilot()) {
            for (int i = 0; i < cursorCount; i++) {
                var cursor = listener.getCursor(i);
                var events = cursor.downEvents;
                int size = events.size();

                while (cursor.latestProcessedDownEventIndex < size) {
                    var event = events.get(cursor.latestProcessedDownEventIndex++);

                    if (canHit(event)) {
                        return event;
                    }
                }
            }

            return null;
        }

        // For Relax, we need to check two cases:
        // 1. If a cursor flows over the object when it is hittable.
        // 2. If a cursor is pressed while it is over the object when it is hittable.
        if (GameHelper.isRelax()) {
            for (int i = 0; i < cursorCount; i++) {
                var cursor = listener.getCursor(i);
                var events = cursor.events;
                int size = events.size();

                while (cursor.latestProcessedEventIndex < size) {
                    var event = events.get(cursor.latestProcessedEventIndex++);

                    if (event.isActionUp()) {
                        continue;
                    }

                    boolean isHit = isHit(hitObject, event);

                    // Case 1
                    if (event.isActionDown() && isHit && canHit(event)) {
                        return event;
                    }

                    // Case 2
                    if (objectElapsedTime >= 0 && isHit) {
                        return event;
                    }
                }
            }

            return null;
        }

        // In regular gameplay, we need to iteratively check whether one or more cursors is on the object while a cursor
        // is being pressed.
        for (int i = 0; i < cursorCount; i++) {
            var cursor = listener.getCursor(i);
            var downEvents = cursor.downEvents;
            int downEventsSize = downEvents.size();

            while (cursor.latestProcessedDownEventIndex < downEventsSize) {
                var downEvent = downEvents.get(cursor.latestProcessedDownEventIndex++);

                if (!canHit(downEvent)) {
                    continue;
                }

                for (int j = 0; j < cursorCount; j++) {
                    if (i == j) {
                        // For the cursor that generated the down event, check if the event position is on the object.
                        if (isHit(hitObject, downEvent)) {
                            return downEvent;
                        }
                    } else {
                        // For the other case, we need to check the event closest to the down event and check if it is
                        // on the object.
                        var otherCursor = listener.getCursor(j);
                        var closestEvent = otherCursor.getClosestEventBefore(downEvent.systemTime);

                        if (closestEvent == null || closestEvent.isActionUp()) {
                            continue;
                        }

                        if (isHit(hitObject, closestEvent)) {
                            return downEvent;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Determines whether a {@link CursorEvent} can hit this {@link GameObject}.
     *
     * @param cursorEvent The {@link CursorEvent} to check.
     * @return {@code true} if the {@link CursorEvent} can hit this {@link GameObject}, {@code false} otherwise.
     */
    private boolean canHit(CursorEvent cursorEvent) {
        if (cursorEvent.isActionUp()) {
            return false;
        }

        return (cursorEvent.trackTime + cursorEvent.offset) / 1000 >= hitTime - objectHittableRange;
    }

    /**
     * Determines whether a {@link CursorEvent} hits this {@link GameObject}. The determination is solely based on
     * distance, not considering timing.
     *
     * @param hitObject The {@link HitObject} associated with this {@link GameObject}.
     * @param cursorEvent The {@link CursorEvent} to check.
     * @return {@code true} if the {@link CursorEvent} hits this {@link GameObject}, {@code false} otherwise.
     */
    private boolean isHit(HitObject hitObject, CursorEvent cursorEvent) {
        if (cursorEvent.isActionUp()) {
            return false;
        }

        return Utils.squaredDistance(position, cursorEvent.position) <= Utils.sqr((float) hitObject.getScreenSpaceGameplayRadius());
    }
}
