package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.annotation.Nullable;

import com.edlplan.framework.math.FMath;
import com.osudroid.beatmaps.HitWindow;
import com.osudroid.beatmaps.hitobjects.HitObject;
import com.osudroid.game.CursorEvent;
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
                        if (objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit) {
                            return event;
                        }
                    }
                } else {
                    // If there are no new events, check the latest event.
                    var event = cursor.getLatestEvent();

                    // Only consider case 2 in this scenario, as the event should logically be marked as a move event
                    // even if it's a down event (no new events mean the user keeps pressing on the same spot).
                    if (event != null && !event.isActionUp() && objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit(hitObject, event)) {
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
                    // The press is too early for this object. It is dropped, but osu! shakes the object for it, so
                    // remember that it landed on the object (read via consumeEarlyPress by the owner).
                    // The press is also claimed: only this object (the earliest one under the cursor) may shake for
                    // it, not the overlapping objects locked behind it (see pollNotelockedPress).
                    if (!isPressClaimed(downEvent) && isPressOnObject(listener, hitObject, i, downEvent, cursorCount)) {
                        claimPress(downEvent);
                        earlyPressOnObject = true;
                    }

                    continue;
                }

                for (int j = 0; j < cursorCount; j++) {
                    if (i == j) {
                        // For the cursor that generated the down event, check if the event position is on the object.
                        if (isHit(hitObject, downEvent)) {
                            return claimPress(downEvent);
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
                            return claimPress(downEvent);
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

    //region Notelock shake

    // A press ("claimed" through CursorEvent.isClaimed) belongs to
    // the earliest object under cursor, which either gets hit or shakes.
    // Later overlapping objects must not shake for an already claimed press.
    // Objects are updated in start time order, so the earliest object claims the press first.
    // The flag lives on the event itself (and is reset when the event is obtained from the pool), because
    // CursorEvents are pooled and recycled: remembering them by identity elsewhere would go stale.
    private boolean notelockFirstPollSkipped;
    private boolean earlyPressOnObject;
    private float shakeElapsed = -1f;
    private float appliedShakeOffset;

    private static CursorEvent claimPress(CursorEvent event) {
        event.isClaimed = true;
        return event;
    }

    private static boolean isPressClaimed(CursorEvent event) {
        return event.isClaimed;
    }

    /**
     * Must be called from {@code init} of pooled objects so the notelock state of a previous use does not leak
     * into the next one.
     */
    protected final void resetNotelockState() {
        notelockFirstPollSkipped = false;
        earlyPressOnObject = false;
    }

    /**
     * Returns whether the last {@link #getHittingCursor} calls dropped a press that landed on this object but was too
     * early to hit it, and clears that state. Call it right after {@link #getHittingCursor} returned {@code null}
     * to shake the object like osu! does.
     */
    protected final boolean consumeEarlyPress() {
        boolean result = earlyPressOnObject;
        earlyPressOnObject = false;
        return result;
    }

    /**
     * Checks, WITHOUT consuming anything, whether a new press landed on this object while it is displayed and
     * locked behind an earlier object (regardless of whether the press is already inside its hit window). It never
     * touches the shared (consuming) indices owned by {@link #getHittingCursor}.
     * <p>
     * Only call this while the object is not hittable (see {@link GameObjectListener#isObjectHittable}).
     */
    protected final boolean pollNotelockedPress(GameObjectListener listener, HitObject hitObject) {
        if (autoPlay || GameHelper.isAutopilot() || GameHelper.isRelax() || hitObject.hitWindow == null) {
            return false;
        }

        if (!notelockFirstPollSkipped) {
            // The first call happens right when the object spawns, before the earlier objects have processed this
            // frame's presses, so it must not react to anything yet.
            notelockFirstPollSkipped = true;
            return false;
        }

        int cursorCount = listener.getCursorsCount();
        boolean pressed = false;

        for (int i = 0; i < cursorCount; i++) {
            // Cursor.downEvents is cleared every frame (Cursor.reset), so the whole list is new for this frame. Presses
            // that were already handled are skipped through the claim flag.
            var downEvents = listener.getCursor(i).downEvents;

            for (int index = 0, size = downEvents.size(); index < size; index++) {
                var downEvent = downEvents.get(index);

                // A locked object shakes for any tap on it while it is displayed (no hit window check).
                // The tap is claimed so the objects locked behind this one, which update later, skip it,
                // and only the earliest object under the tap shakes.
                if (!isPressClaimed(downEvent) && isPressOnObject(listener, hitObject, i, downEvent, cursorCount)) {
                    claimPress(downEvent);
                    pressed = true;
                }
            }
        }

        return pressed;
    }

    // Mirrors the position checks of the regular gameplay branch in getHittingCursor.
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
     * Starts (or restarts) the notelock shake of this object.
     */
    protected final void startShake() {
        shakeElapsed = 0f;
    }

    /**
     * Subclasses move every entity that should shake by {@code dx} pixels on the x axis.
     * Hit testing never reads entity positions (it uses {@link #position}), so this does not move the hitbox.
     */
    protected void shiftShake(float dx) {}

    /**
     * Call at the very beginning of {@code update}: removes last frame's offset so that the object's regular
     * logic, which may reposition entities absolutely, always works on canonical positions.
     */
    protected final void preUpdateShake() {
        if (appliedShakeOffset != 0f) {
            shiftShake(-appliedShakeOffset);
            appliedShakeOffset = 0f;
        }
    }

    /**
     * Call at the very end of {@code update}: advances the shake and applies the current offset.
     *
     * @param canShake Whether the object may still shake (false once it is hit or removed).
     */
    protected final void postUpdateShake(float dt, HitObject hitObject, boolean canShake) {
        if (!canShake) {
            shakeElapsed = -1f;
            return;
        }

        if (shakeElapsed < 0f) {
            return;
        }

        shakeElapsed += dt;

        if (ShakeAnimation.isFinished(shakeElapsed)) {
            shakeElapsed = -1f;
            return;
        }

        float magnitude = (float) (2 * hitObject.getScreenSpaceGameplayRadius()) * ShakeAnimation.MAGNITUDE_RATIO;
        float offset = ShakeAnimation.offset(shakeElapsed, magnitude);

        if (offset != 0f) {
            shiftShake(offset);
            appliedShakeOffset = offset;
        }
    }

    /**
     * Stops the shake and puts every entity back. Call from {@code onExpire}.
     */
    protected final void resetShake() {
        preUpdateShake();
        shakeElapsed = -1f;
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
