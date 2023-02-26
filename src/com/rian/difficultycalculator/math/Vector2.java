package com.rian.difficultycalculator.math;

import android.graphics.PointF;

/**
 * Represents a two-dimensional vector.
 */
public class Vector2 extends PointF {
    public Vector2(float x, float y) {
        super(x, y);
    }

    /**
     * Multiplies this vector with another vector.
     *
     * @param vec The other vector.
     * @return The multiplied vector.
     */
    public Vector2 multiply(Vector2 vec) {
        return new Vector2(x * vec.x, y * vec.y);
    }

    /**
     * Divides this vector with a scalar.
     *
     * Attempting to divide by 0 will throw an error.
     *
     * @param divideFactor The factor to divide the vector by.
     * @return The divided vector.
     */
    public Vector2 divide(float divideFactor) {
        if (divideFactor == 0) {
            throw new Error("Division by 0");
        }

        return new Vector2(x / divideFactor, y / divideFactor);
    }

    /**
     * Adds this vector with another vector.
     *
     * @param vec The other vector.
     * @return The added vector.
     */
    public Vector2 add(Vector2 vec) {
        return new Vector2(x + vec.x, y + vec.y);
    }

    /**
     * Subtracts this vector with another vector.
     *
     * @param vec The other vector.
     * @return The subtracted vector.
     */
    public Vector2 subtract(Vector2 vec) {
        return new Vector2(x - vec.x, y - vec.y);
    }

    /**
     * The length of this vector.
     */
    public double getLength() {
        return Math.hypot(x, y);
    }

    /**
     * Performs a dot multiplication with another vector.
     *
     * @param vec The other vector.
     * @return The dot product of both vectors.
     */
    public double dot(Vector2 vec) {
        return x * vec.x + y * vec.y;
    }

    /**
     * Scales this vector.
     *
     * @param scaleFactor The factor to scale the vector by.
     * @return The scaled vector.
     */
    public Vector2 scale(float scaleFactor) {
        return new Vector2(x * scaleFactor, y * scaleFactor);
    }

    /**
     * Gets the distance between this vector and another vector.
     *
     * @param vec The other vector.
     * @return The distance between this vector and the other vector.
     */
    public double getDistance(Vector2 vec) {
        return Math.hypot(x - vec.x, y - vec.y);
    }
    
    /**
     * Normalizes the vector.
     */
    public void normalize() {
        final double length = getLength();

        x /= length;
        y /= length;
    }
}
