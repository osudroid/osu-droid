package com.rian.difficultycalculator.hitobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.timings.TimingControlPoint;

/**
 * Represents a hit object.
 */
public abstract class HitObject {
    /**
     * The radius of hit objects (i.e. the radius of a circle) relative to osu!standard.
     */
    public static final float OBJECT_RADIUS = 64;

    /**
     * The time at which this hit object starts, in milliseconds.
     */
    public final int startTime;

    /**
     * The position of this hit object.
     */
    public final Vector2 position;

    /**
     * The stack height of this hit object.
     */
    public int stackHeight;

    /**
     * The scale of this hit object with respect to osu!standard scale metric.
     */
    public float scale;

    /**
     * The timing control point this hit object is under effect on.
     */
    public final TimingControlPoint timingControlPoint;

    /**
     * The difficulty control point this hit object is under effect on.
     */
    public final DifficultyControlPoint difficultyControlPoint;

    /**
     * @param startTime              The time at which this hit object starts, in milliseconds.
     * @param position               The position of the hit object relative to the play field.
     * @param timingControlPoint     The timing control point this hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit object is under effect on.
     */
    public HitObject(int startTime, Vector2 position,
                     TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        this.startTime = startTime;
        this.position = position;
        this.timingControlPoint = timingControlPoint;
        this.difficultyControlPoint = difficultyControlPoint;
    }

    /**
     * @param startTime              The time at which this hit object starts, in milliseconds.
     * @param x                      The X position of the hit object relative to the play field.
     * @param y                      The Y position of the hit object relative to the play field.
     * @param timingControlPoint     The timing control point this hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit object is under effect on.
     */
    public HitObject(int startTime, int x, int y,
                     TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        this(startTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint);
    }

    /**
     * Gets the radius of this hit object.
     */
    public double getRadius() {
        return 64 * OBJECT_RADIUS;
    }

    /**
     * Gets the stack offset vector of this hit object.
     */
    public Vector2 getStackOffset() {
        return new Vector2(
            stackHeight * scale * -6.4f,
            stackHeight * scale * -6.4f
        );
    }

    /**
     * Gets the stacked position of this hit object.
     */
    public Vector2 getStackedPosition() {
        return evaluateStackedPosition(position);
    }

    /**
     * Evaluates a stacked position relative to this hit object.
     *
     * @param position The position to evaluate.
     * @return The evaluated stacked position.
     */
    protected Vector2 evaluateStackedPosition(Vector2 position) {
        return position.add(getStackOffset());
    }
}
