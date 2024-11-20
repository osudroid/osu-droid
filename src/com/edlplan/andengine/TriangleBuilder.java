package com.edlplan.andengine;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.utils.FloatArraySlice;

import java.util.Arrays;

public class TriangleBuilder extends FloatArraySlice {


    public float maxX = 0f;

    public float maxY = 0f;


    public TriangleBuilder() {
        this(1);
    }

    public TriangleBuilder(int size) {
        ary = new float[6 * size];
    }


    public void add(Vec2 p1, Vec2 p2, Vec2 p3) {
        if (length + 6 > ary.length) {
            ary = Arrays.copyOf(ary, ary.length * 3 / 2 + 6);
        }
        ary[length++] = p1.x;
        ary[length++] = p1.y;
        ary[length++] = p2.x;
        ary[length++] = p2.y;
        ary[length++] = p3.x;
        ary[length++] = p3.y;

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
