package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.annotation.Nullable;

import com.edlplan.framework.math.FMath;
import com.osudroid.game.CursorEvent;
import com.rian.osu.beatmap.HitWindow;
import com.rian.osu.beatmap.hitobject.HitObject;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.util.modifier.IModifier;

import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;

public abstract class GameObject {
    protected boolean endsCombo;
    protected boolean autoPlay = false;
    protected float hitTime = 0;
    protected int id = -1;
    protected Replay.ReplayObjectData replayObjectData = null;
    protected boolean startHit = false;
    protected PointF position = new PointF();
    private float lifetimeEnd;

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

    /**
     * Updates this {@link GameObject} in the same frame after it has been initialized. This is used to account for the
     * time difference between the current elapsed time (time at which this {@link GameObject} is initialized) and its
     * lifetime start (time at which this {@link GameObject} <b>should have</b> been initialized).
     *
     * @param dt The time difference, in seconds.
     */
    public void updateAfterInit(float dt) {}

    public float getHitTime() {
        return hitTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PointF getPosition() {
        return position;
    }

    public boolean isStartHit() {
        return startHit;
    }

    public void stopLoopingSamples() {}

    /**
     * Calls {@link IEntity#onUpdate} on an {@link IEntity} if {@link IEntity#hasParent()} is {@code true}.
     *
     * @param entity The {@link IEntity} to update.
     * @param dt The time difference in seconds.
     */
    protected void updateAfterInit(IEntity entity, float dt) {
        if (entity.hasParent()) {
            entity.onUpdate(dt);
        }
    }

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

                    if (canHit(event, hitObject.hitWindow)) {
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

                if (size > 0) {
                    while (cursor.latestProcessedEventIndex < size) {
                        var event = events.get(cursor.latestProcessedEventIndex++);

                        if (event.isActionUp()) {
                            continue;
                        }

                        boolean isHit = isHit(hitObject, event);

                        // Case 1
                        if (event.isActionDown() && isHit && canHit(event, hitObject.hitWindow)) {
                            return event;
                        }

                        // Case 2
                        if (objectElapsedTime >= 0 && isHit) {
                            return event;
                        }
                    }
                } else {
                    // If there are no new events, check the latest event.
                    // Not passing ACTION_DOWN or ACTION_MOVE here to avoid array allocation.
                    var event = cursor.getLatestEvent();

                    // Only consider case 2 in this scenario, as the event should logically be marked as a move event
                    // even if it's a down event (no new events mean the user keeps pressing on the same spot).
                    if (event != null && !event.isActionUp() && objectElapsedTime >= 0 && isHit(hitObject, event)) {
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

            // Consume down events that have not been processed yet, even those that are already past the hit threshold.
            // This ensures that start time ordered hit policy (aka "notelock") is enforced correctly.
            while (cursor.latestProcessedDownEventIndex < downEventsSize) {
                var downEvent = downEvents.get(cursor.latestProcessedDownEventIndex++);

                if (!canHit(downEvent, hitObject.hitWindow)) {
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
     * @param hitWindow The {@link HitWindow} of this {@link GameObject}.
     * @return {@code true} if the {@link CursorEvent} can hit this {@link GameObject}, {@code false} otherwise.
     */
    private boolean canHit(CursorEvent cursorEvent, HitWindow hitWindow) {
        if (cursorEvent.isActionUp()) {
            return false;
        }

        float hittableRange = GameHelper.isAutopilot()
            ? (float) FMath.clamp(hitWindow.getMehWindow() + 100, 200, HitWindow.MISS_WINDOW) / 1000
            : (float) HitWindow.MISS_WINDOW / 1000;

        return cursorEvent.getHitTime() / 1000 >= hitTime - hittableRange;
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

    //region Lifetime management

    /**
     * Whether the underlying {@link HitObject} of this {@link GameObject} has been judged in its entirety, including
     * nested {@link HitObject}s.
     */
    public boolean isJudged() {
        return false;
    }

    /**
     * Called when this {@link GameObject}'s lifetime expires.
     */
    public void onExpire() {}

    /**
     * Obtains the time in seconds since the beatmap started at which this {@link GameObject}'s lifetime ends.
     */
    public float getLifetimeEnd() {
        return lifetimeEnd;
    }

    protected void setLifetimeEnd(float lifetimeEnd) {
        this.lifetimeEnd = lifetimeEnd;
    }

    /**
     * Extends the lifetime of this {@link GameObject} to allow an {@link IModifier} to finish.
     *
     * @param elapsedTime Elapsed time since the start of the beatmap, in seconds.
     * @param modifier The {@link IModifier} to extend this {@link GameObject}'s lifetime with.
     */
    protected void extendLifetime(float elapsedTime, IModifier<?> modifier) {
        lifetimeEnd = Math.max(lifetimeEnd, elapsedTime + modifier.getDuration());
    }

    //endregion
}
