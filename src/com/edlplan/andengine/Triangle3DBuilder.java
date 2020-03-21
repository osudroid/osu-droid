package com.edlplan.andengine;

import com.edlplan.framework.math.Vec3;

import java.util.Arrays;

public class Triangle3DBuilder {

    private float[] ver = new float[9];

    private int offset;

    public void add(Vec3 p1, Vec3 p2, Vec3 p3) {
        if (offset + 9 > ver.length) {
            ver = Arrays.copyOf(ver, ver.length * 3 / 2 + 9);
        }
        ver[offset++] = p1.x;
        ver[offset++] = p1.y;
        ver[offset++] = p1.z;
        ver[offset++] = p2.x;
        ver[offset++] = p2.y;
        ver[offset++] = p2.z;
        ver[offset++] = p3.x;
        ver[offset++] = p3.y;
        ver[offset++] = p3.z;
    }

    public float[] getVertex() {
        if (offset != ver.length) {
            ver = Arrays.copyOf(ver, offset);
        }
        return ver;
    }

    public int getOffset() {
        return offset;
    }

}
