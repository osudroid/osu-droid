package com.rian.difficultycalculator.beatmap.timings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A manager for a type of control point.
 */
public abstract class ControlPointManager<T extends ControlPoint> {
    /**
     * The default control point for this type.
     */
    public final T defaultControlPoint;

    /**
     * The control points in this manager.
     */
    protected final ArrayList<T> controlPoints = new ArrayList<>();

    /**
     * @param defaultControlPoint The default control point for this type.
     */
    public ControlPointManager(T defaultControlPoint) {
        this.defaultControlPoint = defaultControlPoint;
    }

    /**
     * Finds the control point that is active at a given time.
     *
     * @param time The time, in milliseconds.
     * @return The active control point at the given time.
     */
    public abstract T controlPointAt(double time);

    /**
     * Adds a new control point.
     * <br><br>
     * Note that the provided control point may not be added if the correct state is already present at the control point's time.
     * <br><br>
     * Additionally, any control point that exists in the same time will be removed.
     *
     * @param controlPoint The control point to add.
     * @return Whether the control point was added.
     */
    public boolean add(T controlPoint) {
        T existing = controlPointAt(controlPoint.time);

        if (controlPoint.isRedundant(existing)) {
            return false;
        }

        // Remove the existing control point if the new control point overrides it at the same time.
        while (controlPoint.time == existing.time) {
            if (!remove(existing)) {
                break;
            }

            existing = controlPointAt(controlPoint.time);
        }

        controlPoints.add(findInsertionIndex(controlPoint.time), controlPoint);

        return true;
    }

    /**
     * Removes a control point.
     * <br><br>
     * This method will remove the earliest control point in the array that is equal to the given control point.
     *
     * @param controlPoint The control point to remove.
     * @return Whether the control point was removed.
     */
    public boolean remove(T controlPoint) {
        return controlPoints.remove(controlPoint);
    }

    /**
     * Removes a control point at an index.
     *
     * @param index The index of the control point to remove.
     * @return The control point that was removed, <code>null</code> if no control points were removed.
     */
    public T remove(int index) {
        if (index < 0 || index > controlPoints.size() - 1) {
            return null;
        }

        return controlPoints.remove(index);
    }

    /**
     * Gets an immutable list of control points in this manager.
     */
    public List<T> getControlPoints() {
        return Collections.unmodifiableList(controlPoints);
    }

    /**
     * Clears all control points in this manager.
     */
    public void clear() {
        controlPoints.clear();
    }

    /**
     * Deep clones this manager.
     *
     * @return The deep cloned manager.
     */
    public ControlPointManager<T> deepClone() {
        return null;
    }

    /**
     * Binary searches one of the control point lists to find the active control point at the given time.
     * <br><br>
     * Includes logic for returning the default control point when no matching point is found.
     *
     * @param time The time to find the control point at, in milliseconds.
     * @return The active control point at the given time, or the default control point if none found.
     */
    protected T binarySearchWithFallback(double time) {
        return binarySearchWithFallback(time, defaultControlPoint);
    }

    /**
     * Binary searches the control point list to find the active control point at the given time.
     * <br><br>
     * Includes logic for returning a fallback control point when no matching point is found.
     *
     * @param time The time to find the control point at, in milliseconds.
     * @param fallback The control point to fallback to when no control points were found.
     * @return The active control point at the given time, or the fallback control point if none found.
     */
    protected T binarySearchWithFallback(double time, T fallback) {
        T controlPoint = binarySearch(time);

        return controlPoint != null ? controlPoint : fallback;
    }

    /**
     * Binary searches the control point list to find the active control point at the given time.
     *
     * @param time The time to find the control point at, in milliseconds.
     * @return The active control point at the given time, `null` if none found.
     */
    protected T binarySearch(double time) {
        if (controlPoints.size() == 0 || time < controlPoints.get(0).time) {
            return null;
        }

        if (time >= controlPoints.get(controlPoints.size() - 1).time) {
            return controlPoints.get(controlPoints.size() - 1);
        }

        int l = 0;
        int r = controlPoints.size() - 2;

        while (l <= r) {
            int pivot = l + ((r - l) >> 1);
            T controlPoint = controlPoints.get(pivot);

            if (controlPoint.time < time) {
                l = pivot + 1;
            } else if (controlPoint.time > time) {
                r = pivot - 1;
            } else {
                return controlPoint;
            }
        }

        // l will be the first control point with time > controlPoint.time, but we want the one before it
        return controlPoints.get(l - 1);
    }

    /**
     * Finds the insertion index of a control point in a given time.
     *
     * @param time The start time of the control point, in milliseconds.
     */
    private int findInsertionIndex(int time) {
        if (controlPoints.size() == 0 || time < controlPoints.get(0).time) {
            return 0;
        }

        if (time >= controlPoints.get(controlPoints.size() - 1).time) {
            return controlPoints.size();
        }

        int l = 0;
        int r = controlPoints.size() - 2;

        while (l <= r) {
            int pivot = l + ((r - l) >> 1);
            T controlPoint = controlPoints.get(pivot);

            if (controlPoint.time < time) {
                l = pivot + 1;
            } else if (controlPoint.time > time) {
                r = pivot - 1;
            } else {
                return pivot;
            }
        }

        return l;
    }
}
