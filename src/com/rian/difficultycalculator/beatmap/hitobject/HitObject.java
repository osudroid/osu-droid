package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.math.Vector2;

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
    protected double startTime;

    /**
     * The position of this hit object.
     */
    protected Vector2 position;

    /**
     * The end position of this hit object.
     */
    protected Vector2 endPosition;

    /**
     * The stack height of this hit object.
     */
    private int stackHeight;

    /**
     * The osu!standard scale of this hit object.
     */
    private float scale;

    /**
     * @param startTime The time at which this hit object starts, in milliseconds.
     * @param position  The position of the hit object relative to the play field.
     */
    public HitObject(double startTime, Vector2 position) {
        this.startTime = startTime;
        this.position = position;
        endPosition = position;
    }

    /**
     * @param startTime The time at which this hit object starts, in milliseconds.
     * @param x         The X position of the hit object relative to the play field.
     * @param y         The Y position of the hit object relative to the play field.
     */
    public HitObject(double startTime, float x, float y) {
        this(startTime, new Vector2(x, y));
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    protected HitObject(HitObject source) {
        startTime = source.startTime;
        position = new Vector2(source.position.x, source.position.y);
        endPosition = new Vector2(source.endPosition.x, source.endPosition.y);
        stackHeight = source.stackHeight;
        scale = source.scale;
    }

    /**
     * Gets the osu!standard scale of this hit object.
     */
    public float getScale() {
        return scale;
    }

    /**
     * Sets the osu!standard scale of this hit object.
     *
     * @param scale The new osu!standard scale.
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * Gets the time at which this hit object starts, in milliseconds.
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Gets the osu!standard stack height of this hit object.
     */
    public int getStackHeight() {
        return stackHeight;
    }

    /**
     * Sets the osu!standard stack height of this hit object.
     *
     * @param stackHeight The new stack height.
     */
    public void setStackHeight(int stackHeight) {
        this.stackHeight = stackHeight;
    }

    /**
     * Gets the position of this hit object.
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Gets the end position of this hit object.
     */
    public Vector2 getEndPosition() {
        return endPosition;
    }

    /**
     * Gets the radius of this hit object.
     */
    public double getRadius() {
        return OBJECT_RADIUS * scale;
    }

    /**
     * Gets the stack offset vector of this hit object.
     */
    public Vector2 getStackOffset() {
        return new Vector2(stackHeight * scale * -6.4f);
    }

    /**
     * Gets the stacked position of this hit object.
     */
    public Vector2 getStackedPosition() {
        return evaluateStackedPosition(position);
    }

    /**
     * Gets the stacked end position of this slider.
     */
    public Vector2 getStackedEndPosition() {
        return evaluateStackedPosition(endPosition);
    }

    /**
     * Evaluates a stacked position relative to this hit object.
     *
     * @return The evaluated stacked position.
     */
    protected Vector2 evaluateStackedPosition(Vector2 position) {
        return position.add(getStackOffset());
    }

    /**
     * Deep clones this hit object.
     * <br><br>
     * This method only deep clones fields that may be changed during difficulty calculation.
     *
     * @return The deep cloned hit object.
     */
    public HitObject deepClone() {
        return null;
    }
}
