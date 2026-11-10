package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.annotation.NonNull;

import com.edlplan.framework.math.FMath;
import com.osudroid.beatmaps.HitWindow;
import com.osudroid.beatmaps.hitobjects.HitObject;
import com.osudroid.game.Cursor;
import com.osudroid.game.CursorEvent;
import com.osudroid.game.PressResult;
import com.osudroid.mods.ModRelax;
import com.rian.andengine.modifier.UniversalModifier;
import com.rian.andengine.modifier.UniversalModifierSequence;

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

    public void playLoopingSamples() {}

    public void stopLoopingSamples() {}

    /**
     * The {@link PressResult.Hit} returned by {@link #checkPresses}, reused to avoid allocations.
     */
    private final PressResult.Hit hitResult = new PressResult.Hit();

    /**
     * Checks the presses of the current frame against this {@link GameObject}.
     * <p>
     * While this {@link GameObject} is hittable (see {@link GameObjectListener#isObjectHittable}), unprocessed presses
     * are consumed, even those that do not hit, which enforces the start time ordered hit policy (aka "notelock").
     * Otherwise, nothing is consumed, and only presses that landed on this {@link GameObject} are considered.
     * <p>
     * A press that hits this {@link GameObject} or makes it shake is claimed (see {@link Cursor#claimDownEvent}), so
     * that the overlapping {@link GameObject}s locked behind this one, which are updated later, do not shake for it.
     *
     * @param listener The {@link GameObjectListener} to get cursors from.
     * @param hitObject The {@link HitObject} associated with this {@link GameObject}.
     * @param objectElapsedTime The elapsed time since the start of this {@link GameObject}, in seconds.
     * @return The {@link PressResult}. A {@link PressResult.Hit} is only valid until the next call.
     */
    @NonNull
    protected final PressResult checkPresses(GameObjectListener listener, HitObject hitObject, double objectElapsedTime) {
        if (!listener.isObjectHittable(this)) {
            return checkNotelockedPresses(listener, hitObject);
        }

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
                        return hitResult.withEvent(event);
                    }
                }
            }

            return PressResult.Ignore.INSTANCE;
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
                            return hitResult.withEvent(event);
                        }

                        // Case 2
                        if (objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit) {
                            return hitResult.withEvent(event);
                        }
                    }
                } else {
                    // If there are no new events, check the latest event.
                    var event = cursor.getLatestEvent();

                    // Only consider case 2 in this scenario, as the event should logically be marked as a move event
                    // even if it's a down event (no new events mean the user keeps pressing on the same spot).
                    if (event != null && !event.isActionUp() && objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit(hitObject, event)) {
                        return hitResult.withEvent(event);
                    }
                }
            }

            return PressResult.Ignore.INSTANCE;
        }

        // In regular gameplay, we need to iteratively check whether one or more cursors is on the object while a cursor
        // is being pressed.
        boolean pressedEarly = false;

        for (int i = 0; i < cursorCount; i++) {
            var cursor = listener.getCursor(i);
            var downEvents = cursor.downEvents;
            int downEventsSize = downEvents.size();

            // Consume down events that have not been processed yet, even those that are already past the hit threshold.
            // This ensures that start time ordered hit policy (aka "notelock") is enforced correctly.
            while (cursor.latestProcessedDownEventIndex < downEventsSize) {
                int index = cursor.latestProcessedDownEventIndex++;
                var downEvent = downEvents.get(index);

                if (!canHit(downEvent, hitObject.hitWindow)) {
                    // The press is too early to hit this object, but it still shakes the object if it landed on it.
                    if (!cursor.isDownEventClaimed(index) && isPressOnObject(listener, hitObject, i, downEvent, cursorCount)) {
                        cursor.claimDownEvent(index);
                        pressedEarly = true;
                    }

                    continue;
                }

                if (isPressOnObject(listener, hitObject, i, downEvent, cursorCount)) {
                    cursor.claimDownEvent(index);
                    return hitResult.withEvent(downEvent);
                }
            }
        }

        return pressedEarly ? PressResult.Shake.INSTANCE : PressResult.Ignore.INSTANCE;
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

    //region Notelock shake

    /**
     * Checks whether a press of the current frame landed on this {@link GameObject} while it is locked behind an
     * earlier {@link GameObject}, regardless of whether the press is inside its hit window.
     * <p>
     * This does not consume presses ({@link Cursor#latestProcessedDownEventIndex} is left untouched), but it still
     * claims the presses that landed on this {@link GameObject}.
     */
    @NonNull
    private PressResult checkNotelockedPresses(GameObjectListener listener, HitObject hitObject) {
        if (autoPlay || GameHelper.isAutopilot() || GameHelper.isRelax() || hitObject.hitWindow == null) {
            return PressResult.Ignore.INSTANCE;
        }

        int cursorCount = listener.getCursorsCount();
        boolean pressed = false;

        for (int i = 0; i < cursorCount; i++) {
            // Cursor.downEvents is cleared every frame (Cursor.reset), so the whole list is new for this frame.
            var cursor = listener.getCursor(i);
            var downEvents = cursor.downEvents;

            for (int index = 0, size = downEvents.size(); index < size; index++) {
                if (!cursor.isDownEventClaimed(index) &&
                    isPressOnObject(listener, hitObject, i, downEvents.get(index), cursorCount)) {
                    cursor.claimDownEvent(index);
                    pressed = true;
                }
            }
        }

        return pressed ? PressResult.Shake.INSTANCE : PressResult.Ignore.INSTANCE;
    }

    /**
     * Determines whether a down {@link CursorEvent} landed on this {@link GameObject}, either by the {@link Cursor} that
     * generated it or by any other {@link Cursor} that was held down at the time of the press.
     *
     * @param listener The {@link GameObjectListener} to get {@link Cursor}s from.
     * @param hitObject The {@link HitObject} associated with this {@link GameObject}.
     * @param cursorIndex The index of the {@link Cursor} that generated {@code downEvent}.
     * @param downEvent The down {@link CursorEvent}.
     * @param cursorCount The number of {@link Cursor}s.
     * @return {@code true} if {@code downEvent} landed on this {@link GameObject}, {@code false} otherwise.
     */
    private boolean isPressOnObject(GameObjectListener listener, HitObject hitObject, int cursorIndex,
                                    CursorEvent downEvent, int cursorCount) {
        for (int j = 0; j < cursorCount; j++) {
            if (cursorIndex == j) {
                if (isHit(hitObject, downEvent)) {
                    return true;
                }
            } else {
                var closestEvent = listener.getCursor(j).getClosestEventBefore(downEvent.systemTime);

                if (closestEvent != null && !closestEvent.isActionUp() && isHit(hitObject, closestEvent)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Shake circle by 8 units inside the circle's local space, in which the circle is 128 units wide.
     */
    private static final float SHAKE_MAGNITUDE_RATIO = 8f / 128f;

    /**
     * Obtains the shake magnitude for a {@link HitObject}, in screen pixels.
     *
     * @param hitObject The {@link HitObject} associated with this {@link GameObject}.
     */
    protected static float getShakeMagnitude(HitObject hitObject) {
        return (float) (2 * hitObject.getScreenSpaceGameplayRadius()) * SHAKE_MAGNITUDE_RATIO;
    }

    //endregion

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
     * Extends the lifetime of this {@link GameObject} to allow a {@link UniversalModifier} to finish.
     *
     * @param modifier The {@link UniversalModifier} to extend this {@link GameObject}'s lifetime with.
     */
    protected void extendLifetime(UniversalModifier modifier) {
        lifetimeEnd = Math.max(lifetimeEnd, modifier.getEndTime());
    }

    /**
     * Extends the lifetime of this {@link GameObject} to allow a {@link UniversalModifierSequence} to finish.
     *
     * @param sequence The {@link UniversalModifierSequence} to extend this {@link GameObject}'s lifetime with.
     */
    protected void extendLifetime(UniversalModifierSequence sequence) {
        lifetimeEnd = Math.max(lifetimeEnd, sequence.getEndTime());
    }

    //endregion
}
