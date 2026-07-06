package ru.nsu.ccfit.zuev.osuplusplus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.Debug;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Custom RenderSurfaceView that intercepts MotionEvents at the lowest possible level
 * via dispatchTouchEvent() override, providing near-zero input latency.
 *
 * Key features (ported from osu!droid+):
 * 1. Raw pointer data is updated IMMEDIATELY on the UI thread, bypassing 1-frame queue latency.
 * 2. All historical MotionEvent samples are processed for smooth slider tracking.
 * 3. Thread-safe atomic versioning for consistent reads from the UpdateThread.
 * 4. Engine signalTouchInterrupt() to process input outside the normal render cycle.
 * 5. Never crashes on input path (wrapped in try-catch).
 *
 * Combined with Choreographer-driven frame callbacks in MainActivity,
 * this decouples touch sampling from the game's update-render cycle.
 */
public class DirectInputSurfaceView extends RenderSurfaceView {

    private Engine attachedEngine;

    /**
     * Maximum number of simultaneous touch pointers.
     */
    private static final int MAX_POINTERS = 10;

    /**
     * Atomic version counter for thread-safe raw pointer reads.
     * Even = stable, Odd = being written (on UI thread).
     */
    private final AtomicIntegerArray mPointerVersions = new AtomicIntegerArray(MAX_POINTERS);

    /**
     * Latest X position for each pointer (surface coordinates).
     */
    private final float[] mPointerX = new float[MAX_POINTERS];

    /**
     * Latest Y position for each pointer (surface coordinates).
     */
    private final float[] mPointerY = new float[MAX_POINTERS];

    /**
     * Whether each pointer is currently down (touching).
     */
    private final boolean[] mPointerDown = new boolean[MAX_POINTERS];

    /**
     * Event time for each pointer (uptime millis).
     */
    private final long[] mPointerEventTime = new long[MAX_POINTERS];

    public DirectInputSurfaceView(final Context context) {
        super(context);
    }

    public DirectInputSurfaceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setRenderer(final Engine pEngine) {
        super.setRenderer(pEngine);
        this.attachedEngine = pEngine;
    }

    /**
     * Returns the Engine attached to this view.
     */
    public Engine getAttachedEngine() {
        return attachedEngine;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event == null || attachedEngine == null) {
            return super.dispatchTouchEvent(event);
        }

        try {
            // Step 1: IMMEDIATELY update raw pointer data (before Engine processing)
            updateRawPointersFromEvent(event);

            // Step 2: Signal the Engine's UpdateThread to wake up and process touch NOW
            attachedEngine.signalTouchInterrupt();

            // Step 3: Let the Engine process the event normally through the queue
            return super.dispatchTouchEvent(event);
        } catch (Exception e) {
            // Never crash in the input path
            Debug.e("DirectInputSurfaceView: dispatchTouchEvent error", e);
            return super.dispatchTouchEvent(event);
        }
    }

    /**
     * Immediately extracts ALL touch data from a MotionEvent (including historical
     * samples) and writes it to the thread-safe raw pointer arrays.
     */
    private void updateRawPointersFromEvent(MotionEvent event) {
        try {
            int action = event.getActionMasked();
            int pointerCount = Math.min(event.getPointerCount(), MAX_POINTERS);
            int historySize = event.getHistorySize();

            // Process historical samples in chronological order
            for (int h = 0; h < historySize; h++) {
                for (int i = 0; i < pointerCount; i++) {
                    int pointerId = event.getPointerId(i);
                    if (pointerId < 0 || pointerId >= MAX_POINTERS) continue;

                    long histTime = event.getHistoricalEventTime(h);
                    float x = event.getHistoricalX(i, h);
                    float y = event.getHistoricalY(i, h);

                    boolean isDown = action == MotionEvent.ACTION_CANCEL
                        ? false
                        : mPointerDown[pointerId] || isDownAction(action, i, event.getActionIndex());

                    // Atomic write with version guard
                    mPointerVersions.incrementAndGet(pointerId);
                    mPointerX[pointerId] = x;
                    mPointerY[pointerId] = y;
                    mPointerDown[pointerId] = isDown;
                    mPointerEventTime[pointerId] = histTime;
                    mPointerVersions.incrementAndGet(pointerId);
                }
            }

            // Current (latest) sample — always written last
            long eventTime = event.getEventTime();
            for (int i = 0; i < pointerCount; i++) {
                int pointerId = event.getPointerId(i);
                if (pointerId < 0 || pointerId >= MAX_POINTERS) continue;

                float x = event.getX(i);
                float y = event.getY(i);
                boolean isDown = action == MotionEvent.ACTION_CANCEL
                    ? false
                    : isDownAction(action, i, event.getActionIndex())
                        || (action != MotionEvent.ACTION_UP
                            && action != MotionEvent.ACTION_POINTER_UP
                            && mPointerDown[pointerId]);

                // Atomic write with version guard
                mPointerVersions.incrementAndGet(pointerId);
                mPointerX[pointerId] = x;
                mPointerY[pointerId] = y;
                mPointerDown[pointerId] = isDown;
                mPointerEventTime[pointerId] = eventTime;
                mPointerVersions.incrementAndGet(pointerId);
            }
        } catch (Exception ignored) {
            // Never crash in the input path
        }
    }

    private static boolean isDownAction(int action, int pointerIndex, int actionIndex) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                return pointerIndex == actionIndex;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                return pointerIndex != actionIndex;
            case MotionEvent.ACTION_MOVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Reads a consistent snapshot of a pointer's position with thread-safe versioning.
     *
     * @param pointerId The pointer ID to read.
     * @param outCoords Array of length 3+ to receive [x, y, isDown(0/1)].
     * @return true if a consistent snapshot was read, false if the pointer is unstable.
     */
    public boolean readPointerSnapshot(int pointerId, float[] outCoords) {
        for (int attempt = 0; attempt < 3; attempt++) {
            int verBefore = mPointerVersions.get(pointerId);
            if ((verBefore & 1) != 0) continue; // being written, retry

            float x = mPointerX[pointerId];
            float y = mPointerY[pointerId];
            boolean down = mPointerDown[pointerId];
            int verAfter = mPointerVersions.get(pointerId);

            if (verBefore == verAfter && (verAfter & 1) == 0) {
                outCoords[0] = x;
                outCoords[1] = y;
                outCoords[2] = down ? 1f : 0f;
                return true;
            }
        }
        return false;
    }

    // Direct array access for the update thread (used by fast path)
    public AtomicIntegerArray getPointerVersions() { return mPointerVersions; }
    public float[] getPointerX() { return mPointerX; }
    public float[] getPointerY() { return mPointerY; }
    public boolean[] getPointerDown() { return mPointerDown; }
    public long[] getPointerEventTime() { return mPointerEventTime; }
    public int getMaxPointers() { return MAX_POINTERS; }
}
