package ru.nsu.ccfit.zuev.osu.game;

/**
 * Notelock shake curve modelled from osu!lazer's {@code DrawableExtensions.Shake}
 * <p>
 * Keyframes (d = {@link #SHAKE_DURATION}, m = magnitude):
 * <pre>
 *    0 -> +m  over d/2  (OutSine)
 *   +m -> -m  over d    (InOutSine)
 *   -m -> +m  over d    (InOutSine)
 *   +m -> -m  over d    (InOutSine)
 *   -m ->  0  over d/2  (InSine)
 * </pre>
 * Total length is 4 * d.
 */
public final class ShakeAnimation {

    /** Length of a single shake in seconds */
    public static final float SHAKE_DURATION = 0.03f;

    public static final float TOTAL_DURATION = SHAKE_DURATION * 4;

    /**
     * Shake magnitude relative to the rendered circle diameter
     */
    public static final float MAGNITUDE_RATIO = 8f / 128f;

    private static final float[] KEY_TIMES = {
            0f,
            SHAKE_DURATION * 0.5f,
            SHAKE_DURATION * 1.5f,
            SHAKE_DURATION * 2.5f,
            SHAKE_DURATION * 3.5f,
            SHAKE_DURATION * 4f
    };

    // Offset multipliers of the magnitude at each keyframe.
    private static final float[] KEY_VALUES = {0f, 1f, -1f, 1f, -1f, 0f};

    private ShakeAnimation() {
    }

    public static boolean isFinished(float elapsed) {
        return elapsed >= TOTAL_DURATION;
    }

    /**
     * @param elapsed   Seconds since the shake started
     * @param magnitude Displacement in screen pixels (see {@link #MAGNITUDE_RATIO})
     * @return The horizontal offset to add to the entity's base x position
     */
    public static float offset(float elapsed, float magnitude) {
        if (elapsed <= 0 || elapsed >= TOTAL_DURATION) {
            return 0f;
        }

        for (int i = 0; i < KEY_TIMES.length - 1; i++) {
            float start = KEY_TIMES[i];
            float end = KEY_TIMES[i + 1];

            if (elapsed < end) {
                float t = (elapsed - start) / (end - start);
                float eased;

                if (i == 0) {
                    eased = outSine(t);
                } else if (i == KEY_TIMES.length - 2) {
                    eased = inSine(t);
                } else {
                    eased = inOutSine(t);
                }

                return (KEY_VALUES[i] + (KEY_VALUES[i + 1] - KEY_VALUES[i]) * eased) * magnitude;
            }
        }

        return 0f;
    }

    private static float outSine(float t) {
        return (float) Math.sin(t * Math.PI / 2);
    }

    private static float inSine(float t) {
        return (float) (1 - Math.cos(t * Math.PI / 2));
    }

    private static float inOutSine(float t) {
        return (float) (0.5 - 0.5 * Math.cos(Math.PI * t));
    }
}
