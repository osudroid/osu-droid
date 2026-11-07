package com.edlplan.andengine;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.utils.FloatArraySlice;

import org.andengine.graphics.batch.MeshRenderer;

import java.util.Arrays;

public class TriangleBuilder extends FloatArraySlice {

    /**
     * Floats per vertex: x, y and the weight of {@link MeshRenderer#drawGradient}.
     */
    public static final int FLOATS_PER_VERTEX = MeshRenderer.GRADIENT_FLOATS;

    private static final int FLOATS_PER_TRIANGLE = FLOATS_PER_VERTEX * 3;


    public float maxX = 0f;

    public float maxY = 0f;


    public TriangleBuilder() {
        this(1);
    }

    public TriangleBuilder(int size) {
        ary = new float[FLOATS_PER_TRIANGLE * size];
    }


    /**
     * Adds a triangle fanning out from {@code center}: the center has the weight 1, the other two
     * corners the weight 0.
     */
    public void add(Vec2 center, Vec2 p2, Vec2 p3) {
        add(center, 1, p2, 0, p3, 0);
    }

    /**
     * Adds a triangle with a weight per corner, see {@link #FLOATS_PER_VERTEX}.
     */
    public void add(Vec2 p1, float w1, Vec2 p2, float w2, Vec2 p3, float w3) {
        if (length + FLOATS_PER_TRIANGLE > ary.length) {
            ary = Arrays.copyOf(ary, ary.length * 3 / 2 + FLOATS_PER_TRIANGLE);
        }
        ary[length++] = p1.x;
        ary[length++] = p1.y;
        ary[length++] = w1;
        ary[length++] = p2.x;
        ary[length++] = p2.y;
        ary[length++] = w2;
        ary[length++] = p3.x;
        ary[length++] = p3.y;
        ary[length++] = w3;

        if (p1.x > maxX) maxX = p1.x;
        if (p2.x > maxX) maxX = p2.x;
        if (p3.x > maxX) maxX = p3.x;

        if (p1.y > maxY) maxY = p1.y;
        if (p2.y > maxY) maxY = p2.y;
        if (p3.y > maxY) maxY = p3.y;
    }

    public void applyVertices(FloatArraySlice slice) {
        slice.offset = 0;
        if (slice.ary.length < length) {
            slice.ary = new float[length];
        }
        slice.length = length;
        System.arraycopy(ary, 0, slice.ary, 0, length);
    }

    public void reset() {
        length = 0;
        maxX = 0f;
        maxY = 0f;
    }


}
