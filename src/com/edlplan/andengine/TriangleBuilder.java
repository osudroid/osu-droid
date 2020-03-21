package com.edlplan.andengine;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.Vec3;
import com.edlplan.framework.utils.FloatArraySlice;

import java.util.Arrays;

public class TriangleBuilder extends FloatArraySlice {

    public TriangleBuilder(float[] cache) {
        this.ary = cache;
    }

    public TriangleBuilder() {
        this(1);
    }

    public TriangleBuilder(int size) {
        ary = new float[6 * size];
    }

    public void add(Vec3 p1, Vec3 p2, Vec3 p3) {
        if (length + 6 > ary.length) {
            ary = Arrays.copyOf(ary, ary.length * 3 / 2 + 6);
        }
        ary[length++] = p1.x;
        ary[length++] = p1.y;
        ary[length++] = p2.x;
        ary[length++] = p2.y;
        ary[length++] = p3.x;
        ary[length++] = p3.y;
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
    }

    public float[] copyVertex() {
        float[] c = Arrays.copyOf(ary, offset);
        return c;
    }

    public FloatArraySlice getVertex(FloatArraySlice slice) {
        slice.offset = 0;
        if (slice.ary.length < length) {
            slice.ary = new float[length];
        }
        slice.length = length;
        System.arraycopy(ary, 0, slice.ary, 0, length);
        return slice;
    }

}
